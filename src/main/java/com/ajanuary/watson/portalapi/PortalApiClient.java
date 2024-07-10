package com.ajanuary.watson.portalapi;

import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalApiClient {
  private final Logger logger = LoggerFactory.getLogger(PortalApiClient.class);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String apiKey;
  private final HttpClient httpClient;

  public PortalApiClient(String apiKey, HttpClient httpClient) {
    this.apiKey = apiKey;
    this.httpClient = httpClient;
  }

  public JsonNode send(URI apiUri, Object requestData) throws IOException, InterruptedException {
    var postDataStr = objectMapper.writeValueAsString(requestData);
    var now =
        ZonedDateTime.now(java.time.ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

    var dataToSign =
        "POST\n"
            + apiUri.getPath()
            + "\n"
            + now
            + "\n"
            + Base64.getEncoder().encodeToString(postDataStr.getBytes());
    var signature = calculateSignature(dataToSign);

    var request =
        HttpRequest.newBuilder(apiUri)
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

    logger.info("Got response {}", response.body());

    return objectMapper.readTree(response.body());
  }

  private String calculateSignature(String dataToSign) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      var secretKey = new SecretKeySpec(apiKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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
}
