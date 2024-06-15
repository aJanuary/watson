package com.ajanuary.watson.config;

import com.ajanuary.watson.alarms.AlarmsConfigYamlParser;
import com.ajanuary.watson.api.ApiConfigYamlParser;
import com.ajanuary.watson.membership.MembershipConfigYamlParser;
import com.ajanuary.watson.programme.ProgrammeConfigYamlParser;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.ZoneId;

public class ConfigYamlParser {

  public Config parse(JsonNode jsonSecrets, JsonNode jsonConfig) {
    var secretsParser = ConfigParser.parse(jsonSecrets);
    var discordBotToken = secretsParser.get("discordBotToken").string().required().value();

    var configParser = ConfigParser.parse(jsonConfig);
    var guildId = configParser.get("guildId").string().required().value();
    var databasePath = configParser.get("databasePath").string().required().value();
    var timezone = configParser.get("timezone").string().required().map(ZoneId::of);
    var alarmsConfig = configParser.get("alarms").object().map(AlarmsConfigYamlParser::parse);
    var apiConfig = configParser.get("api").object().map(ApiConfigYamlParser::parse);
    var membershipConfig =
        configParser
            .get("membership")
            .object()
            .map(
                membershipConfigObj ->
                    MembershipConfigYamlParser.parse(secretsParser, membershipConfigObj));
    var programmeConfig =
        configParser.get("programme").object().map(ProgrammeConfigYamlParser::parse);

    return new Config(
        discordBotToken,
        guildId,
        databasePath,
        timezone,
        alarmsConfig,
        apiConfig,
        membershipConfig,
        programmeConfig);
  }
}
