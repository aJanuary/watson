package com.ajanuary.watson.utils;

import com.ajanuary.watson.config.ConfigYamlParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class ClearChannels {

  public static void main(String[] args)
      throws IOException, InterruptedException, ExecutionException {
    var dotenv = Dotenv.configure().directory(args[0]).load();

    var objectMapper = new YAMLMapper();
    var jsonConfig = objectMapper.readTree(Paths.get(args[0], "config.yaml").toFile());
    var config = new ConfigYamlParser().parse(jsonConfig, dotenv);

    var possibleNames = config.programme().channelNameResolver().getPossibleNames().stream()
        .map(String::toLowerCase).collect(Collectors.toSet());

    var builder = JDABuilder.createDefault(config.discordBotToken())
        .enableIntents(GatewayIntent.GUILD_MEMBERS);
    var jda = builder.build();
    jda.awaitReady();

    var guild = jda.getGuildById(config.guildId());
    assert guild != null;

    guild.getForumChannels().forEach(channel -> {
      if (possibleNames.contains(channel.getName().toLowerCase())) {
        channel.getThreadChannels().forEach(threadChannel -> threadChannel.delete().complete());
      }
    });

    jda.shutdown();
  }
}
