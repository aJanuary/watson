package com.ajanuary.watson.api;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseManager;
import com.ajanuary.watson.membership.MembershipChecker.DiscordUser;
import com.ajanuary.watson.notification.EventDispatcher;
import com.ajanuary.watson.notification.ReadyEvent;
import com.ajanuary.watson.utils.JDAUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiModule implements EventListener {
  private final Logger logger = LoggerFactory.getLogger(ApiModule.class);
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final JDA jda;
  private final JDAUtils jdaUtils;
  private final ApiConfig apiConfig;
  private final Config config;
  private final DatabaseManager databaseManager;
  private final EventDispatcher eventDispatcher;

  public ApiModule(
      JDA jda,
      ApiConfig apiConfig,
      Config config,
      DatabaseManager databaseManager,
      EventDispatcher eventDispatcher) {
    this.jda = jda;
    this.jdaUtils = new JDAUtils(jda, config);
    this.apiConfig = apiConfig;
    this.config = config;
    this.databaseManager = databaseManager;
    this.eventDispatcher = eventDispatcher;

    jda.addEventListener(this);

    eventDispatcher.register(ReadyEvent.class, (e) -> checkCommsLog());
  }

  @Override
  public void onEvent(@NotNull GenericEvent event) {
    if (event instanceof GenericGuildEvent guildEvent) {
      if (!guildEvent.getGuild().getId().equals(config.guildId())) {
        return;
      }
    }

    if (event instanceof SessionResumeEvent) {
      checkCommsLog();
    } else if (event instanceof MessageReceivedEvent guildMessageReceivedEvent) {
      var message = guildMessageReceivedEvent.getMessage();
      if (message.getChannel().getName().equals(apiConfig.channel())) {
        checkCommsLog();
      }
    }
  }

  private void checkCommsLog() {
    var guild = jda.getGuildById(config.guildId());
    if (guild == null) {
      logger.error("Could not find guild with ID {}", config.guildId());
      return;
    }

    var commsChannel = jdaUtils.getTextChannel(apiConfig.channel());

    commsChannel
        .getHistory()
        .retrievePast(100)
        .queue(
            messages -> {
              try (var conn = databaseManager.getConnection()) {
                var membersToCheck = new ArrayList<DiscordUser>();
                // The come in newest first, so reverse them
                Collections.reverse(messages);
                for (var message : messages) {
                  try {
                    if (conn.markCommsLogAsProcessed(message.getId())) {
                      var command = objectMapper.readTree(message.getContentRaw());
                      if (command.has("action")
                          && command.get("action").asText().equals("recheck-user")) {
                        var userId = command.get("user-id").asText();
                        var user = jda.getUserById(userId);
                        if (user != null) {
                          membersToCheck.add(new DiscordUser(userId, user.getName()));
                        }
                      }
                    }
                  } catch (SQLException | JsonProcessingException e) {
                    logger.error("Error processing comms message {}", message.getId(), e);
                  }
                }
                if (!membersToCheck.isEmpty()) {
                  eventDispatcher.dispatch(new CheckUserEvent(membersToCheck));
                }

              } catch (SQLException e) {
                logger.error("Error getting database connection", e);
              }
            },
            e -> logger.error("Error loading comms log", e));
  }
}
