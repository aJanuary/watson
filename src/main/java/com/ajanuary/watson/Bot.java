package com.ajanuary.watson;

import com.ajanuary.watson.config.Config;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot {
  public Bot(Config config, Secrets secrets) {
    var builder = JDABuilder.createDefault(secrets.discordBotToken())
        .enableIntents(GatewayIntent.GUILD_MEMBERS);
    var jda = builder.build();
    new MembershipModule(jda, config, secrets);
  }
}
