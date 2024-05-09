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
      throw new ConfigException("DISCORD_BOT_TOKEN is required");
    }

    var configParser = ConfigParser.parse(jsonConfig);
    var guildId = configParser.get("guildId").string().required().value();
    var databasePath = configParser.get("databasePath").string().required().value();
    var alarmsConfig = configParser.get("alarms").object().map(AlarmsConfigYamlParser::parse);
    var membershipConfig =
        configParser
            .get("membership")
            .object()
            .map(
                membershipConfigObj ->
                    MembershipConfigYamlParser.parse(membershipConfigObj, dotenv));
    var programmeConfig =
        configParser.get("programme").object().map(ProgrammeConfigYamlParser::parse);

    return new Config(
        discordBotToken, guildId, databasePath, alarmsConfig, membershipConfig, programmeConfig);
  }
}
