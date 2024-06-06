package com.ajanuary.watson.db;

import com.ajanuary.watson.alarms.ScheduledDM;
import com.ajanuary.watson.alarms.WithId;
import com.ajanuary.watson.programme.DiscordItem;
import com.ajanuary.watson.programme.DiscordThread;
import com.ajanuary.watson.programme.Status;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.sqlite.SQLiteDataSource;

public class DatabaseManager {

  private final DataSource dataSource;

  public DatabaseManager(String path) throws SQLException {
    var sqliteDataSource = new SQLiteDataSource();
    sqliteDataSource.setUrl("jdbc:sqlite:" + path);
    this.dataSource = sqliteDataSource;
  }

  public void init() throws SQLException {
    var flyway = Flyway.configure().dataSource(dataSource).load();
    flyway.migrate();
  }

  public DatabaseConnection getConnection() throws SQLException {
    return new DatabaseConnection(dataSource.getConnection());
  }

  public class DatabaseConnection implements AutoCloseable {
    private final Connection connection;

    private DatabaseConnection(Connection connection) {
      this.connection = connection;
    }

    @Override
    public void close() throws SQLException {
      connection.close();
    }

    public Optional<DiscordThread> getDiscordThread(String programmeItemId) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
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
        return Optional.of(
            new DiscordThread(
                threadId,
                messageId,
                status,
                new DiscordItem(programmeItemId, title, desc, loc, time, dateTime)));
      }
    }

    public void insertDiscordThread(DiscordThread discordThread) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
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
          throw new SQLException(
              "Error inserting discord thread. Expected to insert 1 row but got " + rowsAffected);
        }
      }
    }

    public void updateDiscordThread(DiscordThread discordThread) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
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
          throw new SQLException(
              "Error inserting discord thread. Expected to insert 1 row but got " + rowsAffected);
        }
      }
    }

    public List<String> getAllProgrammeItemIds() throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            programme_item_id
          from
            discord_threads
          """)) {
        var rs = statement.executeQuery();
        var results = new ArrayList<String>();
        while (rs.next()) {
          results.add(rs.getString(1));
        }
        return results;
      }
    }

    public List<DiscordThread> getItemsBefore(LocalDateTime maxTime) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            programme_item_id,
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
            date_time <= ?
            and processed_alarms = 0
          """)) {
        statement.setString(1, maxTime.toString());
        var rs = statement.executeQuery();
        var results = new ArrayList<DiscordThread>();
        while (rs.next()) {
          var programmeItemId = rs.getString(1);
          var threadId = rs.getString(2);
          var messageId = rs.getString(3);
          var title = rs.getString(4);
          var desc = new String(rs.getBytes(5));
          var loc = rs.getString(6);
          var time = rs.getString(7);
          var dateTime = LocalDateTime.parse(rs.getString(8));
          var status = Status.valueOf(rs.getString(9));
          results.add(
              new DiscordThread(
                  threadId,
                  messageId,
                  status,
                  new DiscordItem(programmeItemId, title, desc, loc, time, dateTime)));
        }
        return results;
      }
    }

    public Optional<LocalDateTime> getNextItemTime() throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            min(date_time)
          from
            discord_threads
          where
            processed_alarms = 0
          """)) {
        var rs = statement.executeQuery();
        if (!rs.next()) {
          return Optional.empty();
        }
        var time = rs.getString(1);
        if (time == null) {
          return Optional.empty();
        }
        return Optional.of(LocalDateTime.parse(time));
      }
    }

    public void markThreadAsProcessed(String programmeItemId) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          update
            discord_threads
          set
            processed_alarms = 1
          where
            programme_item_id = ?
          """)) {
        statement.setString(1, programmeItemId);
        statement.executeUpdate();
      }
    }

    public void addScheduledDM(ScheduledDM scheduledDM) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          insert into scheduled_dm(
            discord_thread_id,
            discord_message_id,
            user_id,
            message_time,
            title,
            jump_url,
            contents,
            tags
          )
          values (?, ?, ?, ?, ?, ?, ?, ?)
          """)) {
        statement.setString(1, scheduledDM.discordThreadId());
        statement.setString(2, scheduledDM.discordMessageId());
        statement.setString(3, scheduledDM.userId());
        statement.setString(4, scheduledDM.messageTime().toString());
        statement.setString(5, scheduledDM.title());
        statement.setString(6, scheduledDM.jumpUrl());
        statement.setString(7, scheduledDM.contents());
        if (scheduledDM.tags().isPresent()) {
          statement.setString(8, scheduledDM.tags().get());
        } else {
          statement.setNull(8, Types.VARCHAR);
        }

        int rowsAffected = statement.executeUpdate();
        if (rowsAffected != 1) {
          throw new SQLException(
              "Error inserting event. Expected to insert 1 row but got " + rowsAffected);
        }
      }
    }

    public Optional<LocalDateTime> getNextScheduledDMTime() throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            min(message_time)
          from
            scheduled_dm
          """)) {
        var rs = statement.executeQuery();
        if (!rs.next()) {
          return Optional.empty();
        }
        var time = rs.getString(1);
        if (time == null) {
          return Optional.empty();
        }
        return Optional.of(LocalDateTime.parse(time));
      }
    }

    public List<WithId<ScheduledDM>> getScheduledDMsBefore(LocalDateTime localDateTime)
        throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            id,
            discord_thread_id,
            discord_message_id,
            user_id,
            message_time,
            title,
            jump_url,
            contents,
            tags
          from
            scheduled_dm
          where
            message_time <= ?
          """)) {
        statement.setString(1, localDateTime.toString());
        var rs = statement.executeQuery();
        var results = new ArrayList<WithId<ScheduledDM>>();
        while (rs.next()) {
          var id = rs.getInt(1);
          var discordThreadId = rs.getString(2);
          var discordMessageId = rs.getString(3);
          var userId = rs.getString(4);
          var messageTime = LocalDateTime.parse(rs.getString(5));
          var title = rs.getString(6);
          var jumpUrl = rs.getString(7);
          var contents = rs.getString(8);
          var tags = Optional.ofNullable(rs.getString(9));
          results.add(
              new WithId<>(
                  id,
                  new ScheduledDM(
                      discordThreadId,
                      discordMessageId,
                      userId,
                      messageTime,
                      title,
                      jumpUrl,
                      contents,
                      tags)));
        }
        return results;
      }
    }

    public void deleteScheduledDM(int id) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          delete from scheduled_dm
          where id = ?
          """)) {
        statement.setInt(1, id);
        statement.executeUpdate();
      }
    }

    public void deleteDiscordThread(String id) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          delete from discord_threads
          where programme_item_id = ?
          """)) {
        statement.setString(1, id);
        statement.executeUpdate();
      }
    }

    public Optional<String> getPrivateThread(String userId, String purpose) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            discord_thread_id
          from
            private_threads
          where
            user_id = ?
            and purpose = ?
          """)) {
        statement.setString(1, userId);
        statement.setString(2, purpose);
        var rs = statement.executeQuery();
        if (!rs.next()) {
          return Optional.empty();
        }
        return Optional.of(rs.getString(1));
      }
    }

    public void insertPrivateThread(String userId, String purpose, String threadId)
        throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          insert or replace into private_threads(
            user_id,
            purpose,
            discord_thread_id
          )
          values (?, ?, ?)
          """)) {
        statement.setString(1, userId);
        statement.setString(2, purpose);
        statement.setString(3, threadId);
        statement.executeUpdate();
      }
    }
  }
}
