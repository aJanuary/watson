package com.ajanuary.watson.membership;

import com.ajanuary.watson.config.ConfigException;
import com.ajanuary.watson.config.ConfigParser.ObjectConfigParserWithValue;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Map;

public class MembershipConfigYamlParser {

  private MembershipConfigYamlParser() {}

  public static MembershipConfig parse(ObjectConfigParserWithValue configParser, Dotenv dotenv) {
    var membersApiKey = dotenv.get("MEMBERS_API_KEY");
    if (membersApiKey == null) {
      throw new ConfigException("MEMBERS_API_KEY is required");
    }

    var membersApiUrl = configParser.get("membersApiUrl").string().required().value();
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
        discordModsChannel,
        memberRole,
        unverifiedRole,
        additionalRoles);
  }
}
