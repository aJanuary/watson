package com.ajanuary.watson.alarms;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseConnection;
import com.ajanuary.watson.notification.EventDispatcher;
import com.ajanuary.watson.notification.EventType;
import com.ajanuary.watson.programme.DiscordThread;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmsModule {
  private final Logger logger = LoggerFactory.getLogger(Scheduler.class);

  private final JDA jda;
  private final AlarmsConfig alarmsConfig;
  private final Config config;
  private final Scheduler<WithId<ScheduledDM>> dmScheduler;

  public AlarmsModule(JDA jda, AlarmsConfig alarmsConfig, Config config, EventDispatcher eventDispatcher) {
    this.jda = jda;
    this.alarmsConfig = alarmsConfig;
    this.config = config;

    var itemScheduler = new Scheduler<>("item", jda, alarmsConfig.timezone(), Duration.ZERO, this::getNextItemTime, this::getItemsBefore, this::handleItem);
    eventDispatcher.register(EventType.ITEMS_CHANGED, itemScheduler::notifyOfDbChange);
    this.dmScheduler = new Scheduler<>("dm", jda, alarmsConfig.timezone(), alarmsConfig.minTimeBetweenDMs(), this::getNextScheduledDMTime, this::getScheduledDMsBefore, this::sendDM);
  }

  private Optional<LocalDateTime> getNextItemTime() throws SQLException, IOException {
    try (var db = new DatabaseConnection(config.databasePath())) {
      return db.getNextItemTime().map(t -> t.minus(alarmsConfig.timeBeforeToNotify()));
    }
  }

  private List<DiscordThread> getItemsBefore(LocalDateTime time) throws SQLException, IOException {
    try (var db = new DatabaseConnection(config.databasePath())) {
      return db.getItemsBefore(time.plus(alarmsConfig.timeBeforeToNotify()));
    }
  }

  private void handleItem(DiscordThread discordThread) {
    try (var db = new DatabaseConnection(config.databasePath())) {
      db.markThreadAsProcessed(discordThread.item().id());
    } catch (SQLException | IOException e) {
      logger.error("Error marking thread as processed", e);
      return;
    }

    var threadChannel = jda.getThreadChannelById(discordThread.discordThreadId());
    assert threadChannel != null;
    threadChannel.retrieveMessageById(discordThread.discordMessageId()).queue(message -> {
      var reaction = message.getReaction(alarmsConfig.alarmEmoji());
      if (reaction == null) {
        logger.warn("Couldn't get the reaction on the message. This is usually because the time was set too soon.");
        return;
      }

      Optional<String> tags;
      if (!threadChannel.getAppliedTags().isEmpty()) {
        tags = Optional.of(threadChannel.getAppliedTags().stream().map(this::formatTag).collect(
            Collectors.joining(", ")));
      } else {
        tags = Optional.empty();
      }

      reaction.retrieveUsers().queue(users -> {
        try (var db = new DatabaseConnection(config.databasePath())) {
          for (var user : users) {
            if (user.isBot()) {
              continue;
            }

            var scheduledDM = new ScheduledDM(discordThread.discordThreadId(),
                discordThread.discordMessageId(), user.getId(),
                discordThread.item().dateTime().minus(alarmsConfig.timeBeforeToNotify()),
                threadChannel.getName(), threadChannel.getJumpUrl(), message.getContentRaw(), tags);
            db.addScheduledDM(scheduledDM);
          }
        } catch (SQLException | IOException e) {
          logger.error("Error adding DM for item {}", discordThread.item().id(), e);
        }

        dmScheduler.notifyOfDbChange();
      }, error -> logger.error("Error getting users who reacted to the message {}",
          discordThread.discordMessageId(), error));
    }, error -> logger.error("Error getting message for thread {}", discordThread.discordThreadId(),
        error));
  }

  private Optional<LocalDateTime> getNextScheduledDMTime() throws SQLException, IOException {
    try (var db = new DatabaseConnection(config.databasePath())) {
      return db.getNextScheduledDMTime();
    }
  }

  private List<WithId<ScheduledDM>> getScheduledDMsBefore(LocalDateTime localDateTime) throws SQLException, IOException {
    try (var db = new DatabaseConnection(config.databasePath())) {
      return db.getScheduledDMsBefore(localDateTime);
    }
  }

  private void sendDM(WithId<ScheduledDM> dmWithId) {
    try (var db = new DatabaseConnection(config.databasePath())) {
      db.deleteScheduledDM(dmWithId.id());
    } catch (SQLException | IOException e) {
      logger.error("Error deleting scheduled DM {}", dmWithId.id(), e);
      return;
    }

    var dm = dmWithId.value();
    if (!dm.messageTime().plus(alarmsConfig.timeBeforeToNotify()).plus(alarmsConfig.maxTimeAfterToNotify()).isAfter(LocalDateTime.now(
        alarmsConfig.timezone()))) {
      logger.warn("DM {} is being processed too late after it's scheduled time of {}. Ignoring",
          dmWithId.id(), dm.messageTime());
      return;
    }

    jda.retrieveUserById(dm.userId()).queue(user -> user.openPrivateChannel().queue(channel -> {
      var embedBuilder = new EmbedBuilder()
          .setTitle(dm.title(), dm.jumpUrl())
          .addField("Description", dm.contents(), false);
      dm.tags().ifPresent(tags -> embedBuilder.addField("Tags", tags, false));

      channel.sendMessage(new MessageCreateBuilder()
          .addContent("You asked me to remind you about this event:")
          .addEmbeds(embedBuilder.build()).build())
          .queue(
              success -> { },
              error -> logger.error("Error sending message to user {} message {}", user.getName(),
                  dmWithId.id(), error));
    }, error -> logger.error("Error opening private channel with user {} for dm {}", dm.userId(),
        dmWithId.id(), error)), error -> logger.error("Error getting user {} for dm {}", dm.userId(), dmWithId.id(), error));
  }


  private String formatTag(ForumTag tag) {
    if (tag.getEmoji() != null) {
      return tag.getEmoji().getFormatted() + " " + tag.getName();
    } else {
      return tag.getName();
    }
  }
}
