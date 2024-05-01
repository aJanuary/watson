package com.ajanuary.watson.config;

import com.ajanuary.watson.alarms.AlarmsConfigYamlParser;
import com.ajanuary.watson.membership.MembershipConfigYamlParser;
import com.ajanuary.watson.programme.ProgrammeConfigYamlParser;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;

public class ConfigYamlParser {
  public Config parse(JsonNode jsonConfig, Dotenv dotenv) {
    var discordBotToken = dotenv.get("DISCORD_BOT_TOKEN");
    if (discordBotToken == null) {
      throw new IllegalArgumentException("DISCORD_BOT_TOKEN is required");
    }

    var guildId = jsonConfig.get("guildId");
    if (guildId == null) {
      throw new IllegalArgumentException("guildId is required");
    }
    var databasePath = jsonConfig.get("databasePath");
    if (databasePath == null) {
      throw new IllegalArgumentException("databasePath is required");
    }

    var alarmsJsonConfig = jsonConfig.get("alarms");
    if (alarmsJsonConfig == null) {
      throw new IllegalArgumentException("alarms config is required");
    }
    var alarmsConfig = new AlarmsConfigYamlParser().parse(alarmsJsonConfig);

    var membersJsonConfig = jsonConfig.get("membership");
    if (membersJsonConfig == null) {
      throw new IllegalArgumentException("membership config is required");
    }
    var membershipConfig = new MembershipConfigYamlParser().parse(membersJsonConfig, dotenv);

    var programmeJsonConfig = jsonConfig.get("programme");
    if (programmeJsonConfig == null) {
      throw new IllegalArgumentException("programme config is required");
    }
    var programmeConfig = new ProgrammeConfigYamlParser().parse(programmeJsonConfig);

    return new Config(discordBotToken, guildId.asText(), databasePath.asText(), alarmsConfig, membershipConfig, programmeConfig);
  }
}
