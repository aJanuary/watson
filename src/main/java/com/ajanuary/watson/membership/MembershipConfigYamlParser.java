package com.ajanuary.watson.membership;

import com.ajanuary.watson.config.ConfigParser.ObjectConfigParserWithValue;
import java.util.Map;

public class MembershipConfigYamlParser {

  private MembershipConfigYamlParser() {}

  public static MembershipConfig parse(
      ObjectConfigParserWithValue secretsParser, ObjectConfigParserWithValue configParser) {
    var membersApiKey = secretsParser.get("membersApiKey").string().required().value();

    var membersApiUrl = configParser.get("membersApiUrl").string().required().value();
    var helpDeskChannel =
        configParser.get("helpDeskChannel").string().defaultingTo("help-desk").value();
    var discordModsChannel =
        configParser.get("discordModsChannel").string().defaultingTo("discord-mods").value();
    var memberRole = configParser.get("memberRole").string().defaultingTo("member").value();
    var unverifiedRole =
        configParser.get("unverifiedRole").string().defaultingTo("unverified").value();
    var additionalRoles =
        configParser
            .get("additionalRoles")
            .object()
            .map(
                additionalRolesConfig ->
                    additionalRolesConfig.toMap(
                        roleConfig -> roleConfig.string().required().value()))
            .orElse(Map.of());

    return new MembershipConfig(
        membersApiUrl,
        membersApiKey,
        helpDeskChannel,
        discordModsChannel,
        memberRole,
        unverifiedRole,
        additionalRoles);
  }
}
