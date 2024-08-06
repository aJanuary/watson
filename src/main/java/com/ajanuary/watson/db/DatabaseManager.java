package com.ajanuary.watson.db;

import com.ajanuary.watson.alarms.ScheduledDM;
import com.ajanuary.watson.alarms.WithId;
import com.ajanuary.watson.programme.DiscordItem;
import com.ajanuary.watson.programme.DiscordThread;
import com.ajanuary.watson.programme.Status;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jetbrains.annotations.NotNull;
import org.sqlite.SQLiteDataSource;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

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

    private static @NotNull String toDbDateTimeString(ZonedDateTime endTime) {
      return endTime.withZoneSameInstant(ZoneOffset.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
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
            start_time,
            end_time,
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
        Optional<String> threadId;
        var threadIdBytes = rs.getBytes(1);
        if (threadIdBytes != null) {
          threadId = Optional.of(new String(threadIdBytes));
        } else {
          threadId = Optional.empty();
        }
        Optional<String> messageId;
        var messageIdBytes = rs.getBytes(2);
        if (messageIdBytes != null) {
          messageId = Optional.of(new String(messageIdBytes));
        } else {
          messageId = Optional.empty();
        }
        var title = rs.getString(3);
        String desc;
        var descBytes = rs.getBytes(4);
        if (descBytes == null) {
          desc = null;
        } else {
          desc = new String(descBytes);
        }
        var loc = rs.getString(5);
        var time = rs.getString(6);
        var startTime = ZonedDateTime.parse(rs.getString(7));
        var endTime = ZonedDateTime.parse(rs.getString(8));
        var status = Status.valueOf(rs.getString(9));
        return Optional.of(
            new DiscordThread(
                threadId,
                messageId,
                status,
                new DiscordItem(programmeItemId, title, desc, loc, time, startTime, endTime)));
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
            start_time,
            end_time,
            status
          )
          values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
          """)) {
        statement.setString(1, discordThread.item().id());
        if (discordThread.discordThreadId().isPresent()) {
          statement.setString(2, discordThread.discordThreadId().get());
        } else {
          statement.setNull(2, Types.VARCHAR);
        }
        if (discordThread.discordMessageId().isPresent()) {
          statement.setString(3, discordThread.discordMessageId().get());
        } else {
          statement.setNull(3, Types.VARCHAR);
        }
        statement.setString(4, discordThread.item().title());
        if (discordThread.item().body() == null) {
          statement.setNull(5, Types.BLOB);
        } else {
          statement.setBytes(5, discordThread.item().body().getBytes());
        }
        statement.setString(6, discordThread.item().loc());
        statement.setString(7, discordThread.item().time());
        statement.setString(8, toDbDateTimeString(discordThread.item().startTime()));
        statement.setString(9, toDbDateTimeString(discordThread.item().endTime()));
        statement.setString(10, discordThread.status().toString());

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
            start_time = ?,
            end_time = ?,
            status = ?
          where
            programme_item_id = ?
          """)) {
        if (discordThread.discordThreadId().isPresent()) {
          statement.setString(1, discordThread.discordThreadId().get());
        } else {
          statement.setNull(1, Types.VARCHAR);
        }
        if (discordThread.discordMessageId().isPresent()) {
          statement.setString(2, discordThread.discordMessageId().get());
        } else {
          statement.setNull(2, Types.VARCHAR);
        }
        statement.setString(3, discordThread.item().title());
        if (discordThread.item().body() == null) {
          statement.setNull(4, Types.BLOB);
        } else {
          statement.setBytes(4, discordThread.item().body().getBytes());
        }
        statement.setString(5, discordThread.item().loc());
        statement.setString(6, discordThread.item().time());
        statement.setString(7, toDbDateTimeString(discordThread.item().startTime()));
        statement.setString(8, toDbDateTimeString(discordThread.item().endTime()));
        statement.setString(9, discordThread.status().toString());
        statement.setString(10, discordThread.item().id());

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

    public List<DiscordThread> getItemsBefore(ZonedDateTime maxTime) throws SQLException {
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
            start_time,
            end_time,
            status
          from
            discord_threads
          where
            start_time <= ?
            and processed_alarms = 0
          """)) {
        statement.setString(1, toDbDateTimeString(maxTime));
        var rs = statement.executeQuery();
        var results = new ArrayList<DiscordThread>();
        while (rs.next()) {
          var programmeItemId = rs.getString(1);
          Optional<String> threadId;
          var threadIdBytes = rs.getBytes(2);
          if (threadIdBytes != null) {
            threadId = Optional.of(new String(threadIdBytes));
          } else {
            threadId = Optional.empty();
          }
          Optional<String> messageId;
          var messageIdBytes = rs.getBytes(3);
          if (messageIdBytes != null) {
            messageId = Optional.of(new String(messageIdBytes));
          } else {
            messageId = Optional.empty();
          }
          var title = rs.getString(4);
          String desc;
          var descBytes = rs.getBytes(5);
          if (descBytes == null) {
            desc = null;
          } else {
            desc = new String(descBytes);
          }
          var loc = rs.getString(6);
          var time = rs.getString(7);
          var startTime = ZonedDateTime.parse(rs.getString(8));
          var endTime = ZonedDateTime.parse(rs.getString(9));
          var status = Status.valueOf(rs.getString(10));
          results.add(
              new DiscordThread(
                  threadId,
                  messageId,
                  status,
                  new DiscordItem(programmeItemId, title, desc, loc, time, startTime, endTime)));
        }
        return results;
      }
    }

    public Optional<ZonedDateTime> getNextItemTime() throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            min(start_time)
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
        return Optional.of(ZonedDateTime.parse(time));
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
        statement.setString(4, toDbDateTimeString(scheduledDM.messageTime()));
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

    public Optional<ZonedDateTime> getNextScheduledDMTime() throws SQLException {
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
        return Optional.of(ZonedDateTime.parse(time));
      }
    }

    public List<WithId<ScheduledDM>> getScheduledDMsBefore(ZonedDateTime dateTime)
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
        statement.setString(1, toDbDateTimeString(dateTime));
        var rs = statement.executeQuery();
        var results = new ArrayList<WithId<ScheduledDM>>();
        while (rs.next()) {
          var id = rs.getInt(1);
          var discordThreadId = rs.getString(2);
          var discordMessageId = rs.getString(3);
          var userId = rs.getString(4);
          var messageTime = ZonedDateTime.parse(rs.getString(5));
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

    public Optional<ZonedDateTime> getNextNowOn(ZonedDateTime now, TemporalAmount timeAfterToKeep)
        throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            min(start_time)
          from
            discord_threads
          where
            end_time > ? and not exists (
              select
                1
              from
                now_on
              where
                discord_threads.programme_item_id = now_on.programme_item_id
            )
          """)) {
        statement.setString(1, toDbDateTimeString(now.minus(timeAfterToKeep)));
        var rs = statement.executeQuery();
        if (!rs.next()) {
          return Optional.empty();
        }
        var time = rs.getString(1);
        if (time == null) {
          return Optional.empty();
        }
        return Optional.of(ZonedDateTime.parse(time));
      }
    }

    public Optional<ZonedDateTime> getNextNowOnEnd() throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            min(end_time)
          from
            now_on
          """)) {
        var rs = statement.executeQuery();
        if (!rs.next()) {
          return Optional.empty();
        }
        var time = rs.getString(1);
        if (time == null) {
          return Optional.empty();
        }
        return Optional.of(ZonedDateTime.parse(time));
      }
    }

    public List<DiscordThread> getNowOn(
        ZonedDateTime now, TemporalAmount timeBeforeToAdd, TemporalAmount timeAfterToKeep)
        throws SQLException {
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
            start_time,
            end_time,
            status
          from
            discord_threads
          where
            start_time <= ? and end_time > ? and not exists (
              select
                1
              from
                now_on
              where
                discord_threads.programme_item_id = now_on.programme_item_id
            )
            order by
              start_time, loc
          """)) {
        statement.setString(1, toDbDateTimeString(now.plus(timeBeforeToAdd)));
        statement.setString(2, toDbDateTimeString(now.minus(timeAfterToKeep)));
        var rs = statement.executeQuery();
        var results = new ArrayList<DiscordThread>();
        while (rs.next()) {
          var programmeItemId = rs.getString(1);
          Optional<String> threadId;
          var threadIdBytes = rs.getBytes(2);
          if (threadIdBytes != null) {
            threadId = Optional.of(new String(threadIdBytes));
          } else {
            threadId = Optional.empty();
          }
          Optional<String> messageId;
          var messageIdBytes = rs.getBytes(3);
          if (messageIdBytes != null) {
            messageId = Optional.of(new String(messageIdBytes));
          } else {
            messageId = Optional.empty();
          }
          var title = rs.getString(4);
          String desc;
          var descBytes = rs.getBytes(5);
          if (descBytes == null) {
            desc = null;
          } else {
            desc = new String(descBytes);
          }
          var loc = rs.getString(6);
          var time = rs.getString(7);
          var startTime = ZonedDateTime.parse(rs.getString(8));
          var endTime = ZonedDateTime.parse(rs.getString(9));
          var status = Status.valueOf(rs.getString(10));
          results.add(
              new DiscordThread(
                  threadId,
                  messageId,
                  status,
                  new DiscordItem(programmeItemId, title, desc, loc, time, startTime, endTime)));
        }
        return results;
      }
    }

    public List<String> getExpiredNowOnMessages(ZonedDateTime time) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          select
            discord_message_id
          from
            now_on
          where
            end_time <= ?
          """)) {
        statement.setString(1, toDbDateTimeString(time));
        var rs = statement.executeQuery();
        var results = new ArrayList<String>();
        while (rs.next()) {
          results.add(rs.getString(1));
        }
        return results;
      }
    }

    public void insertNowOnMessage(
        String programmeItemId, String discordMessageId, ZonedDateTime endTime)
        throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          insert into now_on(
            programme_item_id,
            discord_message_id,
            end_time
          )
          values (?, ?, ?)
          """)) {
        statement.setString(1, programmeItemId);
        statement.setString(2, discordMessageId);
        statement.setString(3, toDbDateTimeString(endTime));
        statement.executeUpdate();
      }
    }

    public void deleteNowOnMessage(String messageId) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          delete from now_on
          where discord_message_id = ?
          """)) {
        statement.setString(1, messageId);
        statement.executeUpdate();
      }
    }

    public boolean markCommsLogAsProcessed(String messageId) throws SQLException {
      try (var connection = dataSource.getConnection();
          var statement =
              connection.prepareStatement(
                  """
          insert into comms_log(discord_message_id) values (?)
          """)) {
        statement.setString(1, messageId);
        statement.executeUpdate();
        return true;
      } catch (SQLiteException e) {
        if (e.getResultCode() == SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY) {
          return false;
        }
        throw e;
      }
    }
  }
}
