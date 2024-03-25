package com.ajanuary.watson.utils;

import com.ajanuary.watson.Secrets;
import com.ajanuary.watson.config.Config;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class ClearChannels {

  private static final Set<String> DAYS_OF_THE_WEEK = Set.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");

  public static void main(String[] args)
      throws IOException, InterruptedException, ExecutionException {
    var dotenv = Dotenv.configure().directory(args[0]).load();
    var secrets = new Secrets(dotenv.get("DISCORD_BOT_TOKEN"), dotenv.get("MEMBERS_API_KEY"));

    var objectMapper = new YAMLMapper();
    var config = objectMapper.readValue(Paths.get(args[0], "config.yaml").toFile(), Config.class);

    var builder = JDABuilder.createDefault(secrets.discordBotToken())
        .enableIntents(GatewayIntent.GUILD_MEMBERS);
    var jda = builder.build();
    jda.awaitReady();

    var guild = jda.getGuildById(config.guildId());
    assert guild != null;

    var futures = new ArrayList<Future<Void>>();
    guild.getForumChannels().forEach(channel -> {
      if (DAYS_OF_THE_WEEK.contains(channel.getName().toLowerCase())) {
        channel.getThreadChannels().forEach(threadChannel -> {
          futures.add(threadChannel.delete().submit());
        });
      }
    });

    for (var future : futures) {
      future.get();
    }
    jda.shutdown();
  }
}
