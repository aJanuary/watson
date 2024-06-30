package com.ajanuary.watson.programme;

import com.ajanuary.watson.alarms.Scheduler;
import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseManager;
import com.ajanuary.watson.notification.EventDispatcher;
import com.ajanuary.watson.portalapi.PortalApiClient;
import com.ajanuary.watson.programme.ProgrammeConfig.Location;
import com.ajanuary.watson.utils.JDAUtils;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.furstenheim.CopyDown;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgrammeModule {

  private static final int MAX_THREAD_TITLE_LEN = 100;
  private final Logger logger = LoggerFactory.getLogger(ProgrammeModule.class);
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("EEE HH:mm");
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
  private final ObjectMapper objectMapper =
      JsonMapper.builder()
          .addModule(new JavaTimeModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
          .build();

  private final JDA jda;
  private final JDAUtils jdaUtils;
  private final ProgrammeConfig programmeConfig;
  private final Config config;
  private final DatabaseManager databaseManager;
  private final EventDispatcher eventDispatcher;
  private final PortalProgrammeApiClient portalProgrammeApiClient;

  private boolean doneFirstOnNowPoll = false;

  public ProgrammeModule(
      JDA jda,
      ProgrammeConfig programmeConfig,
      Config config,
      DatabaseManager databaseManager,
      PortalApiClient portalApiClient,
      EventDispatcher eventDispatcher) {
    this.jda = jda;
    this.jdaUtils = new JDAUtils(jda, config);
    this.programmeConfig = programmeConfig;
    this.config = config;
    this.databaseManager = databaseManager;
    this.portalProgrammeApiClient =
        new PortalProgrammeApiClient(programmeConfig.assignDiscordPostsApiUrl(), portalApiClient);
    this.eventDispatcher = eventDispatcher;
    Executors.newSingleThreadScheduledExecutor()
        .scheduleWithFixedDelay(this::pollProgramme, 0, 1, TimeUnit.MINUTES);

    if (programmeConfig.nowOn().isPresent()) {
      var scheduler =
          new Scheduler<>(
              "now on item",
              Duration.ZERO,
              this::getNextNowOnTime,
              this::getNowOn,
              this::handleNowOn);
      eventDispatcher.register(ItemChangedEvent.class, e -> scheduler.notifyOfDbChange());
    }
  }

  @NotNull
  private static ArrayList<ForumTag> getTags(ProgrammeItem newItem, ForumChannel channel) {
    var tags = new ArrayList<ForumTag>();
    channel
        .getAvailableTags()
        .forEach(
            tag -> {
              if (newItem.tags().stream()
                  .map(String::toLowerCase)
                  .anyMatch(t -> t.equals(tag.getName().toLowerCase()))) {
                tags.add(tag);
              } else if (tag.getName().equals(newItem.loc())) {
                tags.add(tag);
              }
            });
    return tags;
  }

  @NotNull
  private String makeDescription(
      ProgrammeItem newItem, CopyDown mdConverter, boolean hasAlarmsConfigured) {
    var descMd = mdConverter.convert(newItem.desc());
    var people = newItem.people() == null ? List.<String>of() : newItem.people();
    var desc =
        new StringBuilder()
            .append(descMd)
            .append("\n\n\n")
            .append(newItem.mins())
            .append(" min, ")
            .append(newItem.loc());
    if (!people.isEmpty()) {
      desc.append("\n\n").append(String.join(", ", people));
    }

    var links =
        programmeConfig.links().stream()
            .filter(link -> newItem.links().containsKey(link.name()))
            .map(link -> "[" + link.label() + "](<" + newItem.links().get(link.name()) + ">)")
            .toList();
    if (!links.isEmpty()) {
      desc.append("\n\n").append(String.join("\n", links));
    }
    if (hasAlarmsConfigured) {
      desc.append("\n\nReact with :alarm_clock: to be reminded when this item starts");
    }
    return desc.toString();
  }

  private void pollProgramme() {
    try (var conn = databaseManager.getConnection()) {
      var mdConverter = new CopyDown();
      var guild = jda.getGuildById(config.guildId());
      assert guild != null;

      var announcementChannel =
          jdaUtils.getTextChannel(programmeConfig.majorAnnouncementsChannel());
      assert announcementChannel != null;

      var newProgrammeItems = getNewProgrammeItems();

      for (var newItem : newProgrammeItems) {
        var existingThread = conn.getDiscordThread(newItem.id());
        var time =
            newItem.startTime().withZoneSameInstant(config.timezone()).format(TIME_FORMATTER);
        var dateTime =
            newItem.startTime().withZoneSameInstant(config.timezone()).format(DATE_TIME_FORMATTER);
        var newDiscordItem =
            new DiscordItem(
                newItem.id(),
                newItem.title(),
                newItem.desc(),
                newItem.loc(),
                time,
                newItem.startTime(),
                newItem.endTime());
        if (existingThread.isEmpty()) {
          logger.info("Add item [{}] '{}'", newItem.id(), newItem.title());

          var channelName = programmeConfig.channelNameResolver().resolveChannelName(newItem);
          var channel = guild.getForumChannelsByName(channelName, true).get(0);
          assert channel != null;

          var title = time + " " + newItem.title();
          if (programmeConfig.hasPerformedFirstLoad()) {
            if (title.length() > MAX_THREAD_TITLE_LEN - 6) {
              title = title.substring(0, MAX_THREAD_TITLE_LEN - 6);
            }
            title += " [NEW]";
          } else {
            if (title.length() > MAX_THREAD_TITLE_LEN) {
              title = title.substring(0, MAX_THREAD_TITLE_LEN);
            }
          }

          var desc = makeDescription(newItem, mdConverter, config.alarms().isPresent());
          var tags = getTags(newItem, channel);
          var forumPost =
              channel
                  .createForumPost(title, MessageCreateData.fromContent(desc))
                  .setTags(tags)
                  .complete();

          config
              .alarms()
              .ifPresent(
                  alarmsConfig -> {
                    forumPost.getMessage().addReaction(alarmsConfig.alarmEmoji()).complete();
                  });

          String discordThreadId = forumPost.getThreadChannel().getId();
          String discordMessageId = forumPost.getMessage().getId();
          conn.insertDiscordThread(
              new DiscordThread(
                  discordThreadId, discordMessageId, Status.SCHEDULED, newDiscordItem));
          var roomId =
              programmeConfig.locations().stream()
                  .filter(l -> l.name().equals(newItem.loc()))
                  .findFirst()
                  .map(Location::id)
                  .orElse("");
          portalProgrammeApiClient.addPostDetails(
              newItem.id(),
              newItem.title(),
              newItem.startTime(),
              newItem.mins(),
              roomId,
              "https://discord.com/channels/" + config.guildId() + "/" + discordThreadId);
          eventDispatcher.dispatch(new ItemChangedEvent());

          if (programmeConfig.hasPerformedFirstLoad()) {
            var announcementEmbedBuilder = new EmbedBuilder();
            announcementEmbedBuilder.appendDescription("'" + newItem.title() + "' has been added");
            announcementEmbedBuilder.addField("Time", dateTime, false);
            announcementEmbedBuilder.addField("Room", newItem.loc(), false);
            announcementEmbedBuilder.addField(
                "Discussion thread", "<#" + discordThreadId + ">", false);
            announcementChannel
                .sendMessage(MessageCreateData.fromEmbeds(announcementEmbedBuilder.build()))
                .complete();
          }
        } else if (!existingThread.get().item().equals(newDiscordItem)
            || existingThread.get().status() == Status.CANCELLED) {
          logger.info("Edit item [{}] '{}'", newItem.id(), newItem.title());

          var threadChannel = jda.getThreadChannelById(existingThread.get().discordThreadId());
          assert threadChannel != null;
          var forumChannel = threadChannel.getParentChannel().asForumChannel();
          var newTags = getTags(newItem, forumChannel);
          var existingTags = threadChannel.getAppliedTags();

          boolean timeChanged =
              !existingThread.get().item().startTime().equals(newItem.startTime());
          boolean noLongerCancelled = existingThread.get().status() == Status.CANCELLED;
          boolean roomDifferent = !existingThread.get().item().loc().equals(newItem.loc());
          var tagChanges = getTagChanges(newTags, existingTags);
          var isSignificantUpdate =
              timeChanged || noLongerCancelled || roomDifferent || !tagChanges.isEmpty();

          var title = time + " " + newItem.title();

          if (!programmeConfig.hasPerformedFirstLoad()
              && (isSignificantUpdate || existingThread.get().status() == Status.UPDATED)) {
            if (title.length() > MAX_THREAD_TITLE_LEN - 10) {
              title = title.substring(0, MAX_THREAD_TITLE_LEN - 10);
            }
            title += " [UPDATED]";
          } else {
            if (title.length() > MAX_THREAD_TITLE_LEN) {
              title = title.substring(0, MAX_THREAD_TITLE_LEN);
            }
          }

          var desc = makeDescription(newItem, mdConverter, config.alarms().isPresent());

          threadChannel.getManager().setName(title).setAppliedTags(newTags).complete();
          threadChannel.editMessageById(existingThread.get().discordMessageId(), desc).complete();

          conn.updateDiscordThread(
              new DiscordThread(
                  existingThread.get().discordThreadId(),
                  existingThread.get().discordMessageId(),
                  isSignificantUpdate ? Status.UPDATED : existingThread.get().status(),
                  newDiscordItem));
          eventDispatcher.dispatch(new ItemChangedEvent());

          if (!programmeConfig.hasPerformedFirstLoad() && isSignificantUpdate) {
            var announcementEmbedBuilder = new EmbedBuilder();
            var threadEmbedBuilder = new EmbedBuilder();
            var allEmbedBuilders = List.of(announcementEmbedBuilder, threadEmbedBuilder);
            announcementEmbedBuilder.appendDescription(
                "'" + existingThread.get().item().title() + "' has been changed");
            threadEmbedBuilder.appendDescription("This item has been changed");
            if (noLongerCancelled) {
              allEmbedBuilders.forEach(
                  builder -> builder.addField("Status", "The item is no longer cancelled", false));
            }
            if (timeChanged) {
              allEmbedBuilders.forEach(builder -> builder.addField("New time", dateTime, false));
            }
            if (roomDifferent) {
              allEmbedBuilders.forEach(
                  builder -> builder.addField("New room", newItem.loc(), false));
            }
            for (var tagChange : tagChanges) {
              if (!tagChange.tag().equalsIgnoreCase(newItem.loc())
                  && !tagChange.tag().equalsIgnoreCase(existingThread.get().item().loc())) {
                allEmbedBuilders.forEach(
                    builder ->
                        builder.addField(
                            tagChange.added() ? "New tag" : "Tag removed", tagChange.tag(), false));
              }
            }
            announcementEmbedBuilder.addField(
                "Discussion thread", "<#" + existingThread.get().discordThreadId() + ">", false);

            announcementChannel
                .sendMessage(MessageCreateData.fromEmbeds(announcementEmbedBuilder.build()))
                .complete();
            threadChannel
                .sendMessage(MessageCreateData.fromEmbeds(threadEmbedBuilder.build()))
                .complete();
          }
        }
      }

      for (var oldItemId : conn.getAllProgrammeItemIds()) {
        if (newProgrammeItems.stream().noneMatch(newItem -> newItem.id().equals(oldItemId))) {
          var existingThreadM = conn.getDiscordThread(oldItemId);
          if (existingThreadM.isEmpty()) {
            logger.error("Existing to find item for {} but not found", oldItemId);
            continue;
          }
          var existingThread = existingThreadM.get();
          if (!programmeConfig.hasPerformedFirstLoad()) {
            Objects.requireNonNull(jda.getThreadChannelById(existingThread.discordThreadId()))
                .delete()
                .complete();
            conn.deleteDiscordThread(oldItemId);
          } else {
            if (existingThread.status() != Status.CANCELLED) {
              logger.info("Cancel item [{}] '{}'", oldItemId, existingThread.item().title());

              var title = existingThread.item().time() + " " + existingThread.item().title();

              if (title.length() > MAX_THREAD_TITLE_LEN - 12) {
                title = title.substring(0, MAX_THREAD_TITLE_LEN - 12);
              }
              title += " [CANCELLED]";

              Objects.requireNonNull(jda.getThreadChannelById(existingThread.discordThreadId()))
                  .getManager()
                  .setName(title)
                  .complete();

              conn.updateDiscordThread(
                  new DiscordThread(
                      existingThread.discordThreadId(),
                      existingThread.discordMessageId(),
                      Status.CANCELLED,
                      existingThread.item()));
              eventDispatcher.dispatch(new ItemChangedEvent());

              var announcementEmbedBuilder = new EmbedBuilder();
              announcementEmbedBuilder.appendDescription(
                  "'" + existingThread.item().title() + "' has been cancelled");
              announcementEmbedBuilder.addField(
                  "Discussion thread", "<#" + existingThread.discordThreadId() + ">", false);
              announcementChannel
                  .sendMessage(MessageCreateData.fromEmbeds(announcementEmbedBuilder.build()))
                  .complete();

              var threadChannel = jda.getThreadChannelById(existingThread.discordThreadId());
              assert threadChannel != null;
              threadChannel.sendMessage("This item has been cancelled.").complete();
            }
          }
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      logger.error("Failed to poll programme", e);
    }
  }

  private List<TagChange> getTagChanges(ArrayList<ForumTag> newTags, List<ForumTag> existingTags) {
    var tagChanges = new ArrayList<TagChange>();
    for (var newTag : newTags) {
      if (existingTags.stream()
          .noneMatch(existingTag -> existingTag.getId().equals(newTag.getId()))) {
        tagChanges.add(new TagChange(newTag.getName(), true));
      }
    }
    for (var existingTag : existingTags) {
      if (newTags.stream().noneMatch(newTag -> newTag.getId().equals(existingTag.getId()))) {
        tagChanges.add(new TagChange((existingTag.getName()), false));
      }
    }
    return tagChanges;
  }

  private List<ProgrammeItem> getNewProgrammeItems() throws IOException, InterruptedException {
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder().uri(programmeConfig.programmeUrl()).GET().build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("Error polling programme: " + response.body());
    }
    var items =
        objectMapper.readValue(response.body(), new TypeReference<List<ProgrammeItem>>() {});
    return items.stream()
        .sorted(Comparator.comparing(ProgrammeItem::startTime).reversed())
        .toList();
  }

  private Optional<ZonedDateTime> getNextNowOnTime() throws SQLException {
    if (!doneFirstOnNowPoll) {
      // When we start up we want to check what messages we should be posting.
      // It could be that we were down when an event started, so we need to catch up and post it.
      doneFirstOnNowPoll = true;
      return Optional.of(LocalDateTime.MIN.atZone(ZoneId.of("UTC")));
    }

    try (var conn = databaseManager.getConnection()) {
      var nextItemStart =
          conn.getNextNowOn(ZonedDateTime.now(), programmeConfig.nowOn().get().timeAfterToKeep())
              .map(t -> t.minus(programmeConfig.nowOn().get().timeBeforeToAdd()));
      var nextNowOnEnd = conn.getNextNowOnEnd();

      if (nextItemStart.isEmpty()) {
        return nextNowOnEnd;
      }

      return nextNowOnEnd
          .map(dateTime -> nextItemStart.get().isBefore(dateTime) ? nextItemStart.get() : dateTime)
          .or(() -> nextItemStart);
    }
  }

  private static class NowOnAction {
    private final DiscordThread discordThreadToPost;
    private final String discordMessageIdToDelete;

    private NowOnAction(DiscordThread discordThreadToPost, String discordMessageIdToDelete) {
      this.discordThreadToPost = discordThreadToPost;
      this.discordMessageIdToDelete = discordMessageIdToDelete;
    }

    public static NowOnAction post(DiscordThread discordThreadToPost) {
      return new NowOnAction(discordThreadToPost, null);
    }

    public static NowOnAction delete(String discordMessageIdToDelete) {
      return new NowOnAction(null, discordMessageIdToDelete);
    }

    public DiscordThread discordThreadToPost() {
      return discordThreadToPost;
    }

    public String discordMessageIdToDelete() {
      return discordMessageIdToDelete;
    }
  }

  private List<NowOnAction> getNowOn(ZonedDateTime time) throws SQLException {
    try (var conn = databaseManager.getConnection()) {
      return Stream.concat(
              conn
                  .getNowOn(
                      time,
                      programmeConfig.nowOn().get().timeBeforeToAdd(),
                      programmeConfig.nowOn().get().timeAfterToKeep())
                  .stream()
                  .map(NowOnAction::post),
              conn.getExpiredNowOnMessages(time).stream().map(NowOnAction::delete))
          .toList();
    }
  }

  private void handleNowOn(NowOnAction action) {
    if (action.discordThreadToPost() != null) {
      handleNowOnPost(action.discordThreadToPost());
    } else if (action.discordMessageIdToDelete() != null) {
      handleNowOnDelete(action.discordMessageIdToDelete());
    }
  }

  private void handleNowOnPost(DiscordThread discordThread) {
    logger.info("Posting now on message for {}", discordThread.item().id());
    var start =
        discordThread
            .item()
            .startTime()
            .withZoneSameInstant(config.timezone())
            .format(TIME_FORMATTER);
    var end =
        discordThread
            .item()
            .endTime()
            .withZoneSameInstant(config.timezone())
            .format(TIME_FORMATTER);
    var message =
        jdaUtils
            .getTextChannel(programmeConfig.nowOn().get().channel())
            .sendMessage(
                "**"
                    + start
                    + " - "
                    + end
                    + " "
                    + discordThread.item().title()
                    + "**\n"
                    + discordThread.item().loc()
                    + " | [Discuss](https://discord.com/channels/"
                    + config.guildId()
                    + "/"
                    + discordThread.discordThreadId()
                    + ")")
            .complete();
    try (var conn = databaseManager.getConnection()) {
      conn.insertNowOnMessage(
          discordThread.item().id(),
          message.getId(),
          discordThread.item().endTime().plus(programmeConfig.nowOn().get().timeAfterToKeep()));
    } catch (SQLException e) {
      logger.error("Failed to insert now on message", e);
    }
  }

  private void handleNowOnDelete(String messageId) {
    logger.info("Deleting now on message {}", messageId);
    jdaUtils
        .getTextChannel(programmeConfig.nowOn().get().channel())
        .retrieveMessageById(messageId)
        .queue(
            message -> {
              message.delete().queue();
            });

    try (var conn = databaseManager.getConnection()) {
      conn.deleteNowOnMessage(messageId);
    } catch (SQLException e) {
      logger.error("Failed to delete now on message", e);
    }
  }

  private record TagChange(String tag, boolean added) {}
}
