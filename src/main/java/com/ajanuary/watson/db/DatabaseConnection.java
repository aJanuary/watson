package com.ajanuary.watson.db;

import com.ajanuary.watson.programme.DiscordItem;
import com.ajanuary.watson.programme.DiscordThread;
import com.ajanuary.watson.programme.Status;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DatabaseConnection implements Closeable {
  private final Connection connection;

  public DatabaseConnection(String path) throws SQLException {
    var newDatabase = !Files.exists(Path.of(path));
    connection = DriverManager.getConnection("jdbc:sqlite:" + path);
    if (newDatabase) {
      createDatabaseSchema();
    } else {
      upgradeDatabaseSchema();
    }
  }

  private void createDatabaseSchema() throws SQLException {
    try (var statement = connection.createStatement()) {
      statement.executeUpdate("""
        create table discord_threads
        (
          programme_item_id string primary key,
          thread_id string not null,
          message_id string not null,
          title string not null,
          desc blob not null,
          loc string not null,
          time string not null,
          date_time string not null,
          status string not null
        )
        """);
    }
  }

  private void upgradeDatabaseSchema() {
  }

  @Override
  public void close() throws IOException {
    try {
      connection.close();
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  public Optional<DiscordThread> getDiscordThread(String programmeItemId) throws SQLException {
    try (var statement = connection.prepareStatement("""
          select
            thread_id,
            message_id,
            title,
            desc,
            loc,
            time,
            date_time,
            status
          from
            discord_threads
          where
            programme_item_id = ?
          """)) {
      statement.setString(1, programmeItemId);
      var rs = statement.executeQuery();
      if (!rs.next()) {
        return Optional.empty();
      }
      var threadId = rs.getString(1);
      var messageId = rs.getString(2);
      var title = rs.getString(3);
      var desc = new String(rs.getBytes(4));
      var loc = rs.getString(5);
      var time = rs.getString(6);
      var dateTime = LocalDateTime.parse(rs.getString(7));
      var status = Status.valueOf(rs.getString(8));
      return Optional.of(new DiscordThread(threadId, messageId, status, new DiscordItem(programmeItemId, title, desc, loc, time, dateTime)));
    }
  }

  public void insertDiscordThread(DiscordThread discordThread) throws SQLException {
    try (var statement = connection.prepareStatement("""
        insert into discord_threads (
          programme_item_id,
          thread_id,
          message_id,
          title,
          desc,
          loc,
          time,
          date_time,
          status
        )
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """)) {
      statement.setString(1, discordThread.item().id());
      statement.setString(2, discordThread.discordThreadId());
      statement.setString(3, discordThread.discordMessageId());
      statement.setString(4, discordThread.item().title());
      statement.setBytes(5, discordThread.item().body().getBytes());
      statement.setString(6, discordThread.item().loc());
      statement.setString(7, discordThread.item().time());
      statement.setString(8, discordThread.item().dateTime().toString());
      statement.setString(9, discordThread.status().toString());

      var rowsAffected = statement.executeUpdate();
      if (rowsAffected != 1) {
        throw new SQLException("Error inserting discord thread. Expected to insert 1 row but got " + rowsAffected);
      }
    }
  }

  public void updateDiscordThread(DiscordThread discordThread) throws SQLException {
    try (var statement = connection.prepareStatement("""
        update discord_threads
          set thread_id = ?,
          message_id = ?,
          title = ?,
          desc = ?,
          loc = ?,
          time = ?,
          date_time = ?,
          status = ?
        where
          programme_item_id = ?
        """)) {
      statement.setString(1, discordThread.discordThreadId());
      statement.setString(2, discordThread.discordMessageId());
      statement.setString(3, discordThread.item().title());
      statement.setBytes(4, discordThread.item().body().getBytes());
      statement.setString(5, discordThread.item().loc());
      statement.setString(6, discordThread.item().time());
      statement.setString(7, discordThread.item().dateTime().toString());
      statement.setString(8, discordThread.status().toString());
      statement.setString(9, discordThread.item().id());

      var rowsAffected = statement.executeUpdate();
      if (rowsAffected != 1) {
        throw new SQLException("Error inserting discord thread. Expected to insert 1 row but got " + rowsAffected);
      }
    }
  }

  public List<String> getAllProgrammeItemIds() throws SQLException {
    try (var statement = connection.prepareStatement("""
        select
          programme_item_id
        from
          discord_threads
        """)) {
      ResultSet rs = statement.executeQuery();
      var results = new ArrayList<String>();
      while (rs.next()) {
        results.add(rs.getString(1));
      }
      return results;
    }
  }
}
