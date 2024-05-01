package com.ajanuary.watson.membership;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class MembershipConfigYamlParser {
  public MembershipConfig parse(JsonNode jsonConfig, Dotenv dotenv) {
    var membersApiKey = dotenv.get("MEMBERS_API_KEY");
    if (membersApiKey == null) {
      throw new IllegalArgumentException("MEMBERS_API_KEY is required");
    }

    var membersApiRootNode = jsonConfig.get("membersApiRoot");
    if (membersApiRootNode == null) {
      throw new IllegalArgumentException("membersApiRoot is required");
    }
    if (!membersApiRootNode.isTextual()) {
      throw new IllegalArgumentException("membersApiRoot must be a string");
    }

    var discordModsChannelNode = jsonConfig.get("discordModsChannel");
    String discordModsChannel;
    if (discordModsChannelNode == null) {
      discordModsChannel = "discord-mods";
    } else if (!discordModsChannelNode.isTextual()) {
      throw new IllegalArgumentException("discordModsChannel must be a string");
    } else {
      discordModsChannel = discordModsChannelNode.asText();
    }

    var memberRoleNode = jsonConfig.get("memberRole");
    String memberRole;
    if (memberRoleNode == null) {
      memberRole = "member";
    } else if (!memberRoleNode.isTextual()) {
      throw new IllegalArgumentException("memberRole must be a string");
    } else {
      memberRole = memberRoleNode.asText();
    }

    var unverifiedRoleNode = jsonConfig.get("unverifiedRole");
    String unverifiedRole;
    if (unverifiedRoleNode == null) {
      unverifiedRole = "unverified";
    } else if (!unverifiedRoleNode.isTextual()) {
      throw new IllegalArgumentException("unverifiedRole must be a string");
    } else {
      unverifiedRole = unverifiedRoleNode.asText();
    }

    var additionalRolesNode = jsonConfig.get("additionalRoles");
    Map<String, String> additionalRoles = new HashMap<>();
    if (additionalRolesNode == null) {
      throw new IllegalArgumentException("additionalRoles is required");
    }

    for (Iterator<Entry<String, JsonNode>> it = additionalRolesNode.fields(); it.hasNext(); ) {
      var field = it.next();
      if (!field.getValue().isTextual()) {
        throw new IllegalArgumentException("additionalRoles values must be strings");
      }
      additionalRoles.put(field.getKey(), field.getValue().asText());
    }

    return new MembershipConfig(
        membersApiRootNode.asText(),
        membersApiKey,
        discordModsChannel,
        memberRole,
        unverifiedRole,
        additionalRoles
    );
  }
}
