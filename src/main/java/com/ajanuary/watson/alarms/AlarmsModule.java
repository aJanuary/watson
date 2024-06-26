package com.ajanuary.watson.alarms;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseManager;
import com.ajanuary.watson.notification.EventDispatcher;
import com.ajanuary.watson.privatethreads.PrivateThreadManager;
import com.ajanuary.watson.programme.DiscordThread;
import com.ajanuary.watson.programme.ItemChangedEvent;
import com.ajanuary.watson.utils.JDAUtils;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmsModule {

  private final Logger logger = LoggerFactory.getLogger(Scheduler.class);

  private final JDA jda;
  private final JDAUtils jdaUtils;
  private final AlarmsConfig alarmsConfig;
  private final Config config;
  private final DatabaseManager databaseManager;
  private final Scheduler<WithId<ScheduledDM>> dmScheduler;
  private final PrivateThreadManager privateThreadManager;

  public AlarmsModule(
      JDA jda,
      AlarmsConfig alarmsConfig,
      Config config,
      DatabaseManager databaseManager,
      EventDispatcher eventDispatcher) {
    this.jda = jda;
    this.jdaUtils = new JDAUtils(jda, config);
    this.alarmsConfig = alarmsConfig;
    this.config = config;
    this.databaseManager = databaseManager;

    var itemScheduler =
        new Scheduler<>(
            "alarm item",
            Duration.ZERO,
            this::getNextItemTime,
            this::getItemsBefore,
            this::handleItem);
    eventDispatcher.register(ItemChangedEvent.class, e -> itemScheduler.notifyOfDbChange());
    this.dmScheduler =
        new Scheduler<>(
            "dm",
            alarmsConfig.minTimeBetweenDMs(),
            this::getNextScheduledDMTime,
            this::getScheduledDMsBefore,
            this::sendDM);

    this.privateThreadManager = new PrivateThreadManager(jda, "alarms");
  }

  private Optional<ZonedDateTime> getNextItemTime() throws SQLException {
    try (var conn = databaseManager.getConnection()) {
      return conn.getNextItemTime().map(t -> t.minus(alarmsConfig.timeBeforeToNotify()));
    }
  }

  private List<DiscordThread> getItemsBefore(ZonedDateTime time) throws SQLException {
    try (var conn = databaseManager.getConnection()) {
      return conn.getItemsBefore(time.plus(alarmsConfig.timeBeforeToNotify()));
    }
  }

  private void handleItem(DiscordThread discordThread) {
    try (var conn = databaseManager.getConnection()) {
      conn.markThreadAsProcessed(discordThread.item().id());
    } catch (SQLException e) {
      logger.error("Error marking thread as processed", e);
      return;
    }

    var threadChannel = jda.getThreadChannelById(discordThread.discordThreadId());
    assert threadChannel != null;
    threadChannel
        .retrieveMessageById(discordThread.discordMessageId())
        .queue(
            message -> {
              var reaction = message.getReaction(alarmsConfig.alarmEmoji());
              if (reaction == null) {
                logger.warn(
                    "Couldn't get the reaction on the message. This is usually because the time was set too soon.");
                return;
              }

              Optional<String> tags;
              if (!threadChannel.getAppliedTags().isEmpty()) {
                tags =
                    Optional.of(
                        threadChannel.getAppliedTags().stream()
                            .map(this::formatTag)
                            .collect(Collectors.joining(", ")));
              } else {
                tags = Optional.empty();
              }

              reaction
                  .retrieveUsers()
                  .queue(
                      users -> {
                        try (var conn = databaseManager.getConnection()) {
                          for (var user : users) {
                            if (user.isBot()) {
                              continue;
                            }

                            var scheduledDM =
                                new ScheduledDM(
                                    discordThread.discordThreadId(),
                                    discordThread.discordMessageId(),
                                    user.getId(),
                                    discordThread
                                        .item()
                                        .startTime()
                                        .minus(alarmsConfig.timeBeforeToNotify()),
                                    threadChannel.getName(),
                                    threadChannel.getJumpUrl(),
                                    message.getContentRaw(),
                                    tags);
                            conn.addScheduledDM(scheduledDM);
                          }
                        } catch (SQLException e) {
                          logger.error("Error adding DM for item {}", discordThread.item().id(), e);
                        }

                        dmScheduler.notifyOfDbChange();
                      },
                      error ->
                          logger.error(
                              "Error getting users who reacted to the message {}",
                              discordThread.discordMessageId(),
                              error));
            },
            error ->
                logger.error(
                    "Error getting message for thread {}", discordThread.discordThreadId(), error));
  }

  private Optional<ZonedDateTime> getNextScheduledDMTime() throws SQLException, IOException {
    try (var conn = databaseManager.getConnection()) {
      return conn.getNextScheduledDMTime();
    }
  }

  private List<WithId<ScheduledDM>> getScheduledDMsBefore(ZonedDateTime localDateTime)
      throws SQLException, IOException {
    try (var conn = databaseManager.getConnection()) {
      return conn.getScheduledDMsBefore(localDateTime);
    }
  }

  private void sendDM(WithId<ScheduledDM> dmWithId) {
    try (var conn = databaseManager.getConnection()) {
      try {
        conn.deleteScheduledDM(dmWithId.id());
      } catch (SQLException e) {
        logger.error("Error deleting scheduled DM {}", dmWithId.id(), e);
        return;
      }

      var dm = dmWithId.value();
      if (!dm.messageTime()
          .plus(alarmsConfig.timeBeforeToNotify())
          .plus(alarmsConfig.maxTimeAfterToNotify())
          .isAfter(ZonedDateTime.now())) {
        logger.warn(
            "DM {} is being processed too late after it's scheduled time of {}. Ignoring",
            dmWithId.id(),
            dm.messageTime());
        return;
      }

      ThreadChannel thread;
      try {
        thread =
            privateThreadManager
                .createThread(
                    conn,
                    dm.userId(),
                    () -> {
                      var nowNextChannel = jdaUtils.getTextChannel(alarmsConfig.alarmsChannel());
                      return nowNextChannel.createThreadChannel("reminders", true).complete();
                    })
                .thread();
      } catch (SQLException e) {
        logger.error("Error recording private thread for user {}", dm.userId(), e);
        return;
      }

      var embedBuilder =
          new EmbedBuilder()
              .setTitle(dm.title(), dm.jumpUrl())
              .addField("Description", dm.contents(), false);
      dm.tags().ifPresent(tags -> embedBuilder.addField("Tags", tags, false));

      thread
          .sendMessage(
              new MessageCreateBuilder()
                  .addContent("<@" + dm.userId() + "> You asked me to remind you about this event:")
                  .addEmbeds(embedBuilder.build())
                  .build())
          .queue(
              success -> {},
              error ->
                  logger.error(
                      "Error sending message to user {} message {}",
                      dm.userId(),
                      dmWithId.id(),
                      error));
    } catch (SQLException e) {
      logger.error("Error sending DM {}", dmWithId.id(), e);
    }
  }

  private String formatTag(ForumTag tag) {
    if (tag.getEmoji() != null) {
      return tag.getEmoji().getFormatted() + " " + tag.getName();
    } else {
      return tag.getName();
    }
  }
}
