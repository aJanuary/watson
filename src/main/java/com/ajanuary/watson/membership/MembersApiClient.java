package com.ajanuary.watson.membership;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MembersApiClient {
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final MembershipConfig config;
  private final HttpClient httpClient;

  public MembersApiClient(MembershipConfig config, HttpClient httpClient) {
    this.config = config;
    this.httpClient = httpClient;
  }

  public Map<String, Optional<MemberDetails>> getMemberStatus(Collection<String> discordUserIds)
      throws IOException, InterruptedException {
    var postData = "{\"discordUserIds\": [" + String.join(", ", discordUserIds) + "]}";
    var uri = URI.create(config.membersApiKey());
    var now =
        ZonedDateTime.now(java.time.ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

    var dataToSign =
        "POST\n"
            + uri.getPath()
            + "\n"
            + now
            + "\n"
            + Base64.getEncoder().encodeToString(postData.getBytes());
    var signature = calculateSignature(dataToSign);

    var request =
        HttpRequest.newBuilder(uri)
            .header("accept", "application/json")
            .header("X-Members-RequestTime", now)
            .header("Authorization", "members:1 Watson " + signature)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(postData))
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("Error checking membership: " + response.body());
    } else {
      var memberships = new HashMap<String, Optional<MemberDetails>>();

      var responseData = objectMapper.readTree(response.body());
      responseData
          .fields()
          .forEachRemaining(
              entry -> {
                if (entry.getValue().isNull()) {
                  memberships.put(entry.getKey(), Optional.empty());
                } else {
                  var name = entry.getValue().get("name").asText();
                  var roles = new ArrayList<String>();
                  entry
                      .getValue()
                      .get("roles")
                      .elements()
                      .forEachRemaining(
                          role -> {
                            roles.add(role.asText());
                          });
                  memberships.put(entry.getKey(), Optional.of(new MemberDetails(name, roles)));
                }
              });
      return memberships;
    }
  }

  private String calculateSignature(String dataToSign) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      var secretKey =
          new SecretKeySpec(config.membersApiKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKey);
      var signature = new StringBuilder();
      for (var b : mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8))) {
        signature.append(String.format("%02x", b));
      }
      return signature.toString();
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  public record MemberDetails(String name, List<String> roles) {}
}
