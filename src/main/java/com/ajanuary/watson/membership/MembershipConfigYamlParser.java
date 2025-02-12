package com.ajanuary.watson.membership;

import com.ajanuary.watson.config.ConfigParser.ObjectConfigParserWithValue;
import java.net.URI;
import java.util.Map;

public class MembershipConfigYamlParser {

  private MembershipConfigYamlParser() {}

  public static MembershipConfig parse(ObjectConfigParserWithValue configParser) {
    var membersApiUrl = configParser.get("membersApiUrl").string().required().map(URI::create);
    var helpDeskChannel =
        configParser.get("helpDeskChannel").string().defaultingTo("help-desk").value();
    var memberRole = configParser.get("memberRole").string().defaultingTo("member").value();
    var unverifiedRole =
        configParser.get("unverifiedRole").string().defaultingTo("unverified").value();
    var memberHelpRole =
        configParser.get("memberHelpRole").string().defaultingTo("Discord Mod").value();
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
        helpDeskChannel,
        memberRole,
        unverifiedRole,
        additionalRoles,
        memberHelpRole);
  }
}
