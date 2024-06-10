package com.ajanuary.watson.membership;

import com.ajanuary.watson.membership.MembershipChecker.DiscordUser;
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
import java.util.function.Consumer;
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

  public Map<String, MembershipStatus> getMemberStatus(Collection<DiscordUser> discordUsers)
      throws IOException, InterruptedException {

    var postData = new HashMap<String, Object>();
    postData.put(
        "discordUsers",
        discordUsers.stream()
            .map(
                discordUser -> {
                  var discordUserMap = new HashMap<String, String>();
                  discordUserMap.put("id", discordUser.userId());
                  discordUserMap.put("username", discordUser.username());
                  return discordUserMap;
                })
            .toList());
    var postDataStr = objectMapper.writeValueAsString(postData);
    var uri = URI.create(config.membersApiUrl());
    var now =
        ZonedDateTime.now(java.time.ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

    var dataToSign =
        "POST\n"
            + uri.getPath()
            + "\n"
            + now
            + "\n"
            + Base64.getEncoder().encodeToString(postDataStr.getBytes());
    var signature = calculateSignature(dataToSign);

    var request =
        HttpRequest.newBuilder(uri)
            .header("accept", "application/json")
            .header("X-Members-RequestTime", now)
            .header("Authorization", "members:1 Watson " + signature)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(postDataStr))
            .build();

    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException(
          "Error checking membership: [" + response.statusCode() + "]" + response.body());
    }
    var memberships = new HashMap<String, MembershipStatus>();

    var responseData = objectMapper.readTree(response.body());
    responseData
        .fields()
        .forEachRemaining(
            entry -> {
              if (entry.getValue().isTextual()) {
                memberships.put(
                    entry.getKey(), MembershipStatus.verification(entry.getValue().asText()));
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
                memberships.put(
                    entry.getKey(), MembershipStatus.member(new MemberDetails(name, roles)));
              }
            });
    return memberships;
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

  public static class MembershipStatus {
    private final MemberDetails details;
    private final String verificationUrl;

    private MembershipStatus(MemberDetails details, String verificationUrl) {
      this.details = details;
      this.verificationUrl = verificationUrl;
    }

    public static MembershipStatus member(MemberDetails details) {
      return new MembershipStatus(details, null);
    }

    public static MembershipStatus verification(String verificationUrl) {
      return new MembershipStatus(null, verificationUrl);
    }

    public void map(Consumer<MemberDetails> ifMember, Consumer<String> ifVerification) {
      if (details != null) {
        ifMember.accept(details);
      } else {
        ifVerification.accept(verificationUrl);
      }
    }
  }

  public record MemberDetails(String name, List<String> roles) {}
}
