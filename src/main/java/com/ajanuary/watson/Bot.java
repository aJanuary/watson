package com.ajanuary.watson;

import com.ajanuary.watson.alarms.AlarmsModule;
import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseConnection;
import com.ajanuary.watson.membership.MembershipModule;
import com.ajanuary.watson.notification.EventDispatcher;
import com.ajanuary.watson.programme.ProgrammeModule;
import java.io.IOException;
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
    try (var db = new DatabaseConnection(config.databasePath())) {
      db.init();
    } catch (SQLException | IOException e) {
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
        .ifPresent(alarmsConfig -> new AlarmsModule(jda, alarmsConfig, config, eventDispatcher));
    config
        .membership()
        .ifPresent(membershipConfig -> new MembershipModule(jda, membershipConfig, config));
    config
        .programme()
        .ifPresent(
            programmeConfig -> new ProgrammeModule(jda, programmeConfig, config, eventDispatcher));
  }
}
