package com.ajanuary.watson.programme;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseConnection;
import com.ajanuary.watson.notification.EventDispatcher;
import com.ajanuary.watson.notification.EventType;
import com.ajanuary.watson.utils.JDAUtils;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.furstenheim.CopyDown;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.forums.ForumTag;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgrammeModule {

  private static final int MAX_THREAD_TITLE_LEN = 100;
  private final Logger logger = LoggerFactory.getLogger(ProgrammeModule.class);
  private final ObjectMapper objectMapper = JsonMapper.builder()
      .addModule(new JavaTimeModule())
      .enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
  .build();

  private final JDA jda;
  private final JDAUtils jdaUtils;
  private final Config config;
  private final EventDispatcher eventDispatcher;

  public ProgrammeModule(JDA jda, Config config, EventDispatcher eventDispatcher) {
    this.jda = jda;
    this.jdaUtils = new JDAUtils(jda, config);
    this.config = config;
    this.eventDispatcher = eventDispatcher;
    Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(this::pollProgramme, 0, 1, TimeUnit.MINUTES);
  }

  private void pollProgramme() {
    try (var db = new DatabaseConnection(config.databasePath())){
      var mdConverter = new CopyDown();
      var guild = jda.getGuildById(config.guildId());
      assert guild != null;

      var announcementChannel = jdaUtils.getTextChannel(config.programme().majorAnnouncementsChannel());
      assert announcementChannel != null;

      var newProgrammeItems = getNewProgrammeItems();

      for (var newItem : newProgrammeItems) {
        var existingThread = db.getDiscordThread(newItem.id());
        var newDiscordItem = new DiscordItem(newItem.id(), newItem.title(), newItem.desc(), newItem.loc(), newItem.time(), newItem.dateTime());
        if (existingThread.isEmpty()) {
          logger.info("Add item [{}] '{}'", newItem.id(), newItem.title());

          var day = newItem.date().format(DateTimeFormatter.ofPattern("EEEE"));
          var channel = guild.getForumChannelsByName(day, true).get(0);
          assert channel != null;

          var title = newItem.time() + " " + newItem.title();
          if (config.programme().hasPerformedFirstLoad()) {
            if (title.length() > MAX_THREAD_TITLE_LEN - 6) {
              title = title.substring(0, MAX_THREAD_TITLE_LEN - 6);
            }
            title += " [NEW]";
          } else {
            if (title.length() > MAX_THREAD_TITLE_LEN) {
              title = title.substring(0, MAX_THREAD_TITLE_LEN);
            }
          }

          var desc = makeDescription(newItem, mdConverter);
          var tags = getTags(newItem, channel);
          var forumPost = channel.createForumPost(title, MessageCreateData.fromContent(desc))
              .setTags(tags).complete();

          forumPost.getMessage().addReaction(Emoji.fromUnicode(config.alarms().alarmEmoji())).complete();

          String discordThreadId = forumPost.getThreadChannel().getId();
          String discordMessageId = forumPost.getMessage().getId();
          db.insertDiscordThread(new DiscordThread(discordThreadId, discordMessageId, Status.SCHEDULED, newDiscordItem));
          eventDispatcher.dispatch(EventType.ITEMS_CHANGED);

          if (config.programme().hasPerformedFirstLoad()) {
            var announcementEmbedBuilder = new EmbedBuilder();
            announcementEmbedBuilder.appendDescription("'" + newItem.title() + "' has been added");
            announcementEmbedBuilder.addField("Time", newItem.time(), false);
            announcementEmbedBuilder.addField("Room", newItem.loc(), false);
            announcementEmbedBuilder.addField("Discussion thread", "<#" + discordThreadId + ">", false);
            announcementChannel.sendMessage(MessageCreateData.fromEmbeds(announcementEmbedBuilder.build())).complete();
          }
        } else if (!existingThread.get().item().equals(newDiscordItem) || existingThread.get().status() == Status.CANCELLED) {
          logger.info("Edit item [{}] '{}'", newItem.id(), newItem.title());

          var day = newItem.date().format(DateTimeFormatter.ofPattern("EEEE"));
          var channel = guild.getForumChannelsByName(day, true).get(0);
          assert channel != null;

          var threadChannel = jda.getThreadChannelById(existingThread.get().discordThreadId());
          assert threadChannel != null;
          var forumChannel = threadChannel.getParentChannel().asForumChannel();
          var newTags = getTags(newItem, forumChannel);
          var existingTags = threadChannel.getAppliedTags();

          boolean timeChanged = !existingThread.get().item().dateTime().equals(newItem.dateTime());
          boolean noLongerCancelled = existingThread.get().status() == Status.CANCELLED;
          boolean roomDifferent = !existingThread.get().item().loc().equals(newItem.loc());
          var tagChanges = getTagChanges(newTags, existingTags);
          var isSignificantUpdate = timeChanged || noLongerCancelled || roomDifferent || !tagChanges.isEmpty();

          var title = newItem.time() + " " + newItem.title();

          if (!config.programme().hasPerformedFirstLoad() && (isSignificantUpdate || existingThread.get().status() == Status.UPDATED)) {
            if (title.length() > MAX_THREAD_TITLE_LEN - 10) {
              title = title.substring(0, MAX_THREAD_TITLE_LEN - 10);
            }
            title += " [UPDATED]";
          } else {
            if (title.length() > MAX_THREAD_TITLE_LEN) {
              title = title.substring(0, MAX_THREAD_TITLE_LEN);
            }
          }

          var desc = makeDescription(newItem, mdConverter);

          threadChannel.getManager().setName(title).setAppliedTags(newTags).complete();
          threadChannel.editMessageById(existingThread.get().discordMessageId(), desc).complete();

          db.updateDiscordThread(new DiscordThread(existingThread.get().discordThreadId(), existingThread.get().discordMessageId(), isSignificantUpdate ? Status.UPDATED : existingThread.get().status(), newDiscordItem));
          eventDispatcher.dispatch(EventType.ITEMS_CHANGED);

          if (!config.programme().hasPerformedFirstLoad() && isSignificantUpdate) {
            var announcementEmbedBuilder = new EmbedBuilder();
            var threadEmbedBuilder = new EmbedBuilder();
            var allEmbedBuilders = List.of(announcementEmbedBuilder, threadEmbedBuilder);
            announcementEmbedBuilder.appendDescription("'" + existingThread.get().item().title() + "' has been changed");
            threadEmbedBuilder.appendDescription("This item has been changed");
            if (noLongerCancelled) {
              allEmbedBuilders.forEach(builder -> builder.addField("Status", "The item is no longer cancelled", false));
            }
            if (timeChanged) {
              allEmbedBuilders.forEach(builder -> builder.addField("New time",  newItem.time(), false));
            }
            if (roomDifferent) {
              allEmbedBuilders.forEach(builder -> builder.addField("New room", newItem.loc(), false));
            }
            for (var tagChange : tagChanges) {
              if (!tagChange.tag().equalsIgnoreCase(newItem.loc()) && !tagChange.tag().equalsIgnoreCase(existingThread.get().item().loc())) {
                allEmbedBuilders.forEach(builder -> builder.addField(tagChange.added() ? "New tag" : "Tag removed", tagChange.tag(), false));
              }
            }
            announcementEmbedBuilder.addField("Discussion thread", "<#" + existingThread.get().discordThreadId() + ">", false);

            announcementChannel.sendMessage(MessageCreateData.fromEmbeds(announcementEmbedBuilder.build())).complete();
            threadChannel.sendMessage(MessageCreateData.fromEmbeds(threadEmbedBuilder.build())).complete();
          }
        }
      }

      for (var oldItemId : db.getAllProgrammeItemIds()) {
        if (newProgrammeItems.stream().noneMatch(newItem -> newItem.id().equals(oldItemId))) {
          var existingThreadM = db.getDiscordThread(oldItemId);
          if (existingThreadM.isEmpty()) {
            logger.error("Existing to find item for {} but not found", oldItemId);
            continue;
          }
          var existingThread = existingThreadM.get();
          if (!config.programme().hasPerformedFirstLoad()) {
            Objects.requireNonNull(jda.getThreadChannelById(existingThread.discordThreadId()))
                .delete().complete();
            db.deleteDiscordThread(oldItemId);
          } else {
            if (existingThread.status() != Status.CANCELLED) {
              logger.info("Cancel item [{}] '{}'", oldItemId, existingThread.item().title());

              var title = existingThread.item().time() + " " + existingThread.item().title();

              if (title.length() > MAX_THREAD_TITLE_LEN - 12) {
                title = title.substring(0, MAX_THREAD_TITLE_LEN - 12);
              }
              title += " [CANCELLED]";

              Objects.requireNonNull(jda.getThreadChannelById(existingThread.discordThreadId()))
                  .getManager().setName(title).complete();

              db.updateDiscordThread(new DiscordThread(existingThread.discordThreadId(),
                  existingThread.discordMessageId(), Status.CANCELLED, existingThread.item()));
              eventDispatcher.dispatch(EventType.ITEMS_CHANGED);

              var announcementEmbedBuilder = new EmbedBuilder();
              announcementEmbedBuilder.appendDescription(
                  "'" + existingThread.item().title() + "' has been cancelled");
              announcementEmbedBuilder.addField("Discussion thread",
                  "<#" + existingThread.discordThreadId() + ">", false);
              announcementChannel.sendMessage(
                  MessageCreateData.fromEmbeds(announcementEmbedBuilder.build())).complete();

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

  private record TagChange(String tag, boolean added) {
  }

  private List<TagChange> getTagChanges(ArrayList<ForumTag> newTags, List<ForumTag> existingTags) {
    var tagChanges = new ArrayList<TagChange>();
    for (var newTag : newTags) {
      if (existingTags.stream().noneMatch(existingTag -> existingTag.getId().equals(newTag.getId()))) {
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

  @NotNull
  private static ArrayList<ForumTag> getTags(ProgrammeItem newItem,
      ForumChannel channel) {
    var tags = new ArrayList<ForumTag>();
    channel.getAvailableTags().forEach(tag -> {
      if (newItem.tags().stream().map(String::toLowerCase).anyMatch(t -> t.equals(tag.getName().toLowerCase()))) {
        tags.add(tag);
      } else if (tag.getName().equals(newItem.loc())) {
        tags.add(tag);
      }
    });
    return tags;
  }

  @NotNull
  private static String makeDescription(ProgrammeItem newItem, CopyDown mdConverter) {
    var descMd = mdConverter.convert(newItem.desc());
    var people = newItem.people() == null ? List.<String>of() : newItem.people();
    return descMd + "\n\n\n" + newItem.mins() + " min, " + newItem.loc() + "\n\n" + String.join(", ", people) + "\n\nReact with :alarm_clock: to be reminded when this item starts";
  }

  private List<ProgrammeItem> getNewProgrammeItems() throws IOException, InterruptedException {
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create(config.programme().programmeUrl()))
        .GET().build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("Error polling programme: " + response.body());
    }
    var items = objectMapper.readValue(response.body(), new TypeReference<List<ProgrammeItem>>() {});
    return items.stream().sorted(Comparator.comparing(ProgrammeItem::dateTime).reversed()).toList();
  }
}
