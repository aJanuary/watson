package com.ajanuary.watson.utils;

import com.ajanuary.watson.config.ConfigYamlParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class ClearChannels {

  public static void main(String[] args)
      throws IOException, InterruptedException, ExecutionException {
    if (args.length != 2) {
      System.err.println("Usage: ClearChannels <secrets file> <config file>");
      System.exit(1);
    }

    var objectMapper = new YAMLMapper();
    var jsonSecrets = objectMapper.readTree(Paths.get(args[0]).toFile());
    var jsonConfig = objectMapper.readTree(Paths.get(args[1]).toFile());
    var config = new ConfigYamlParser().parse(jsonSecrets, jsonConfig);

    var possibleNames =
        config.programme().get().channelNameResolver().getPossibleNames().stream()
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

    var builder =
        JDABuilder.createDefault(config.discordBotToken())
            .enableIntents(GatewayIntent.GUILD_MEMBERS);
    var jda = builder.build();
    jda.awaitReady();

    var guild = jda.getGuildById(config.guildId());
    assert guild != null;

    var count = new AtomicInteger();
    guild
        .getForumChannels()
        .forEach(
            channel -> {
              if (possibleNames.contains(channel.getName().toLowerCase())) {
                channel
                    .getThreadChannels()
                    .forEach(
                        threadChannel -> {
                          count.incrementAndGet();
                          threadChannel.delete().complete();
                        });
              }
            });

    var announcementsChannel =
        guild
            .getTextChannelsByName(config.programme().get().majorAnnouncementsChannel(), true)
            .get(0);
    announcementsChannel.getIterableHistory().forEach(m -> m.delete().complete());

    jda.shutdown();
    System.out.println("Deleted " + count.get() + " threads");
  }
}
