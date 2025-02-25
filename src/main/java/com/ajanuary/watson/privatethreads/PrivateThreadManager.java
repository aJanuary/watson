package com.ajanuary.watson.privatethreads;

import com.ajanuary.watson.db.DatabaseManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

public class PrivateThreadManager {
  private final Map<String, Object> locks = new HashMap<>();
  private final JDA jda;
  private final String purpose;

  public PrivateThreadManager(JDA jda, String purpose) {
    this.jda = jda;
    this.purpose = purpose;
  }

  public CreateThreadResult createThread(
      DatabaseManager.DatabaseConnection conn, String userId, Supplier<ThreadChannel> createChannel)
      throws SQLException {
    synchronized (locks.computeIfAbsent(userId, k -> new Object())) {
      var privateThreadId = conn.getPrivateThread(userId, purpose);

      if (privateThreadId.isPresent()) {
        var privateThread = jda.getThreadChannelById(privateThreadId.get());
        // Someone could have deleted the private thread. If they have, we need to create a new one.
        if (privateThread != null && !privateThread.isArchived()) {
          return new CreateThreadResult(privateThread, false);
        }
      }

      var thread = createChannel.get();
      conn.insertPrivateThread(userId, purpose, thread.getId());
      return new CreateThreadResult(thread, true);
    }
  }

  public Optional<ThreadChannel> getThread(DatabaseManager.DatabaseConnection conn, String userId)
      throws SQLException {
    synchronized (locks.computeIfAbsent(userId, k -> new Object())) {
      var privateThreadId = conn.getPrivateThread(userId, purpose);
      if (privateThreadId.isEmpty()) {
        return Optional.empty();
      }

      var privateThread = jda.getThreadChannelById(privateThreadId.get());
      if (privateThread == null || privateThread.isArchived()) {
        return Optional.empty();
      }

      return Optional.of(privateThread);
    }
  }

  public record CreateThreadResult(ThreadChannel thread, boolean created) {}
}
