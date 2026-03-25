package com.ajanuary.watson.newsletter;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseManager;
import com.ajanuary.watson.utils.JDAUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.furstenheim.CopyDown;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.FileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewsletterModule {

  private static final int MAX_MESSAGE_LENGTH = 2000;

  private final Logger logger = LoggerFactory.getLogger(NewsletterModule.class);
  private final JDAUtils jdaUtils;
  private final NewsletterConfig newsletterConfig;
  private final DatabaseManager databaseManager;
  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .build();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public NewsletterModule(
      JDA jda,
      NewsletterConfig newsletterConfig,
      Config config,
      DatabaseManager databaseManager) {
    this.jdaUtils = new JDAUtils(jda, config);
    this.newsletterConfig = newsletterConfig;
    this.databaseManager = databaseManager;

    var intervalSeconds = newsletterConfig.pollInterval().getSeconds();
    Executors.newSingleThreadScheduledExecutor()
        .scheduleWithFixedDelay(this::pollNewsletter, 0, intervalSeconds, TimeUnit.SECONDS);
  }

  private void pollNewsletter() {
    var start = System.currentTimeMillis();
    var numAdded = 0;
    var numUpdated = 0;
    var numDeleted = 0;
    try (var conn = databaseManager.getConnection()) {
      var feedItems = fetchFeed();
      var channel = jdaUtils.getTextChannel(newsletterConfig.channel());

      var feedIds = new HashSet<String>();
      for (var item : feedItems) {
        feedIds.add(item.id());
      }

      for (var item : feedItems) {
        var isEmpty = (item.body() == null || item.body().isEmpty())
            && (item.image() == null || item.image().isEmpty());

        var existing = conn.getNewsletterItem(item.id());

        if (isEmpty) {
          if (existing.isPresent()) {
            logger.info("Deleting empty newsletter item [{}]", item.id());
            channel.deleteMessageById(existing.get().discordMessageId()).complete();
            conn.deleteNewsletterItem(item.id());
            numDeleted++;
          }
          continue;
        }

        var checksum = computeChecksum(item);

        if (existing.isEmpty()) {
          var messageContent = formatMessage(item);
          var messageAction = channel.sendMessage(messageContent);
          if (item.image() != null && !item.image().isEmpty()) {
            var imageBytes = downloadImage(item.image());
            var filename = extractFilename(item.image());
            messageAction = messageAction.addFiles(FileUpload.fromData(imageBytes, filename));
          }
          var message = messageAction.complete();
          conn.insertNewsletterItem(item.id(), message.getId(), checksum);
          logger.info("Posted new newsletter item [{}]", item.id());
          numAdded++;
        } else if (!existing.get().contentChecksum().equals(checksum)) {
          var messageContent = formatMessage(item);
          var editAction =
              channel.editMessageById(existing.get().discordMessageId(), messageContent);
          if (item.image() != null && !item.image().isEmpty()) {
            var imageBytes = downloadImage(item.image());
            var filename = extractFilename(item.image());
            editAction = editAction.setFiles(FileUpload.fromData(imageBytes, filename));
          } else {
            editAction = editAction.setFiles();
          }
          editAction.complete();
          conn.updateNewsletterItem(item.id(), checksum);
          logger.info("Updated newsletter item [{}]", item.id());
          numUpdated++;
        }
      }

      for (var existingId : conn.getAllNewsletterIds()) {
        if (!feedIds.contains(existingId)) {
          var existing = conn.getNewsletterItem(existingId);
          if (existing.isPresent()) {
            channel.deleteMessageById(existing.get().discordMessageId()).complete();
            conn.deleteNewsletterItem(existingId);
            logger.info("Deleted newsletter item [{}]", existingId);
            numDeleted++;
          }
        }
      }
    } catch (Exception e) {
      logger.error("Failed to poll newsletter", e);
    } finally {
      long end = System.currentTimeMillis();
      logger.info(
          "Newsletter poll took {}ms. added {} updated {} deleted {}",
          end - start,
          numAdded,
          numUpdated,
          numDeleted);
    }
  }

  private List<NewsletterItem> fetchFeed() throws IOException, InterruptedException {
    var request = HttpRequest.newBuilder().uri(newsletterConfig.feedUrl()).GET().build();
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("Error polling newsletter feed: " + response.body());
    }
    return objectMapper.readValue(response.body(), new TypeReference<>() {});
  }

  private String formatMessage(NewsletterItem item) {
    var prefix = "";
    var mdConverter = new CopyDown();
    if (item.title() != null && !item.title().isEmpty()) {
      prefix = "# " + mdConverter.convert(item.title()).strip() + "\n\n";
    }

    var body = "";
    if (item.body() != null && !item.body().isEmpty()) {
      body = mdConverter.convert(item.body());
    }

    var content = prefix + body;
    if (content.length() > MAX_MESSAGE_LENGTH) {
      content = content.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
    }
    return content;
  }

  private String computeChecksum(NewsletterItem item) {
    try {
      var input =
          Objects.toString(item.title(), "")
              + "\0"
              + Objects.toString(item.body(), "")
              + "\0"
              + Objects.toString(item.image(), "");
      var digest =
          MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 not available", e);
    }
  }

  private byte[] downloadImage(String relativeUrl) throws IOException, InterruptedException {
    var imageUri = newsletterConfig.feedUrl().resolve(relativeUrl);
    var response =
        httpClient.send(
            HttpRequest.newBuilder().uri(imageUri).GET().build(),
            HttpResponse.BodyHandlers.ofByteArray());
    if (response.statusCode() != 200) {
      throw new IOException("Failed to download image: HTTP " + response.statusCode());
    }
    return response.body();
  }

  private String extractFilename(String path) {
    var lastSlash = path.lastIndexOf('/');
    if (lastSlash >= 0) {
      return path.substring(lastSlash + 1);
    }
    return path;
  }
}
