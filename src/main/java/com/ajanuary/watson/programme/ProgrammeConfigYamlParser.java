package com.ajanuary.watson.programme;

import com.fasterxml.jackson.databind.JsonNode;

public class ProgrammeConfigYamlParser {
  public ProgrammeConfig parse(JsonNode jsonConfig) {
    var programmeUrlNode = jsonConfig.get("programmeUrl");
    if (programmeUrlNode == null) {
      throw new IllegalArgumentException("programmeUrl is required");
    }
    if (!programmeUrlNode.isTextual()) {
      throw new IllegalArgumentException("programmeUrl must be a string");
    }

    var majorAnnouncementsChannelNode = jsonConfig.get("majorAnnouncementsChannel");
    String majorAnnouncementsChannel;
    if (majorAnnouncementsChannelNode == null) {
      majorAnnouncementsChannel = "programme-announcements";
    } else if (!majorAnnouncementsChannelNode.isTextual()) {
      throw new IllegalArgumentException("majorAnnouncementsChannel must be a string");
    } else {
      majorAnnouncementsChannel = majorAnnouncementsChannelNode.asText();
    }

    var hasPerformedFirstLoadNode = jsonConfig.get("hasPerformedFirstLoad");
    if (hasPerformedFirstLoadNode == null) {
      throw new IllegalArgumentException("hasPerformedFirstLoad is required");
    }
    if (!hasPerformedFirstLoadNode.isBoolean()) {
      throw new IllegalArgumentException("hasPerformedFirstLoad must be a boolean");
    }

    return new ProgrammeConfig(
        programmeUrlNode.asText(),
        majorAnnouncementsChannel,
        hasPerformedFirstLoadNode.asBoolean()
    );
  }
}