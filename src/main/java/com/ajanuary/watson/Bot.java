package com.ajanuary.watson;

import com.ajanuary.watson.alarms.AlarmsModule;
import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseConnection;
import com.ajanuary.watson.membership.MembershipModule;
import com.ajanuary.watson.notification.EventDispatcher;
import com.ajanuary.watson.programme.ProgrammeModule;
import java.io.IOException;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

public class Bot {
  public Bot(Config config, Secrets secrets) throws InterruptedException {
    try (var db = new DatabaseConnection(config.programmeStoragePath())) {
      db.init();
    } catch (SQLException | IOException e) {
      throw new RuntimeException(e);
    }

    var builder = JDABuilder.createDefault(secrets.discordBotToken())
        .enableIntents(GatewayIntent.GUILD_MEMBERS)
        .enableIntents(GatewayIntent.MESSAGE_CONTENT)
        .setChunkingFilter(ChunkingFilter.ALL)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .enableCache(CacheFlag.FORUM_TAGS)
        .setActivity(Activity.playing("with time"));
    var jda = builder.build();
    jda.awaitReady();
    var eventDispatcher = new EventDispatcher();
    new MembershipModule(jda, config, secrets);
    new ProgrammeModule(jda, config, eventDispatcher);
    new AlarmsModule(jda, config, eventDispatcher);
  }
}
