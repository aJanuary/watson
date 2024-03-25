package com.ajanuary.watson;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.membership.MembershipModule;
import com.ajanuary.watson.programme.ProgrammeModule;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Bot {
  public Bot(Config config, Secrets secrets) throws InterruptedException {
    var builder = JDABuilder.createDefault(secrets.discordBotToken())
        .enableIntents(GatewayIntent.GUILD_MEMBERS);
    var jda = builder.build();
    jda.awaitReady();
    new MembershipModule(jda, config, secrets);
    new ProgrammeModule(jda, config);
  }
}
