package com.ajanuary.watson.programme;

import com.ajanuary.watson.portalapi.PortalApiClient;
import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalProgrammeApiClient {
  private final Logger logger = LoggerFactory.getLogger(PortalProgrammeApiClient.class);

  private final ReentrantLock lock = new ReentrantLock();
  private final Condition hasPostDetails = lock.newCondition();
  private Map<String, PostDetails> queue = new HashMap<>();

  public PortalProgrammeApiClient(URI assignDiscordPostApiUrl, PortalApiClient portalApiClient) {
    new Thread(
            () -> {
              while (true) {
                Collection<PostDetails> toSend;
                lock.lock();
                try {
                  while (queue.isEmpty()) {
                    hasPostDetails.await();
                  }
                  toSend = queue.values();
                  queue = new HashMap<>();
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  break;
                } finally {
                  lock.unlock();
                }

                try {
                  var result = portalApiClient.send(assignDiscordPostApiUrl, toSend);
                  if (!result.isObject()
                      || !result.has("result")
                      || !result.get("result").isTextual()
                      || !result.get("result").asText().equals("success"))
                    if (!result.isBoolean() || !result.asBoolean()) {
                      throw new IOException("Error sending post details");
                    }
                } catch (IOException e) {
                  logger.error("Error sending post details", e);
                  lock.lock();
                  try {
                    // Requeue them, but only if they haven't been updated since
                    toSend.stream()
                        .filter(postDetails -> !queue.containsKey(postDetails.itemId()))
                        .forEach(postDetails -> queue.put(postDetails.itemId(), postDetails));
                  } finally {
                    lock.unlock();
                  }
                  // Prevent spamming the API
                  try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(5));
                  } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                    break;
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  break;
                }
              }
            })
        .start();
  }

  public void addPostDetails(
      String itemId,
      String title,
      LocalDateTime localDateTime,
      int mins,
      String roomId,
      String postUrl) {
    var postDetails =
        new PostDetails(
            itemId,
            title,
            localDateTime.format(DateTimeFormatter.ISO_DATE_TIME),
            mins,
            roomId,
            postUrl);
    lock.lock();
    try {
      queue.put(itemId, postDetails);
      hasPostDetails.signal();
    } finally {
      lock.unlock();
    }
  }

  private record PostDetails(
      String itemId, String title, String start, int mins, String roomId, String postUrl) {}
}
