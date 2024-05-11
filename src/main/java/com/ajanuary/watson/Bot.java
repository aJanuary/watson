package com.ajanuary.watson;

import com.ajanuary.watson.alarms.AlarmsModule;
import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseManager;
import com.ajanuary.watson.membership.MembersApiClient;
import com.ajanuary.watson.membership.MembershipChecker;
import com.ajanuary.watson.membership.MembershipModule;
import com.ajanuary.watson.notification.EventDispatcher;
import com.ajanuary.watson.programme.ProgrammeModule;
import java.net.http.HttpClient;
import java.sql.SQLException;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bot {

  public Bot(Config config) throws InterruptedException {
    DatabaseManager databaseManager;
    try {
      databaseManager = new DatabaseManager(config.databasePath());
      databaseManager.init();
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    var builder =
        JDABuilder.createDefault(config.discordBotToken())
            .enableIntents(GatewayIntent.GUILD_MEMBERS)
            .enableIntents(GatewayIntent.MESSAGE_CONTENT)
            .setChunkingFilter(ChunkingFilter.ALL)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .enableCache(CacheFlag.FORUM_TAGS)
            .setActivity(Activity.playing("with time"));
    var jda = builder.build();
    jda.awaitReady();
    try {
      config.validateDiscordConfig(jda);
    } catch (Exception e) {
      Logger logger = LoggerFactory.getLogger(Bot.class);
      logger.error("Error validating configuration", e);
      jda.shutdownNow();
      System.exit(1);
    }

    var eventDispatcher = new EventDispatcher();
    config
        .alarms()
        .ifPresent(
            alarmsConfig -> new AlarmsModule(jda, alarmsConfig, databaseManager, eventDispatcher));
    config
        .membership()
        .ifPresent(
            membershipConfig -> {
              var apiClient = new MembersApiClient(membershipConfig, HttpClient.newHttpClient());
              var membershipChecker =
                  new MembershipChecker(jda, membershipConfig, config, apiClient);
              new MembershipModule(jda, config, membershipChecker);
            });
    config
        .programme()
        .ifPresent(
            programmeConfig ->
                new ProgrammeModule(
                    jda, programmeConfig, config, databaseManager, eventDispatcher));
  }
}
