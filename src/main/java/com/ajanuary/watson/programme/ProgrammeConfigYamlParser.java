package com.ajanuary.watson.programme;

import com.ajanuary.watson.programme.channelnameresolvers.ChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayTodChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayTodChannelNameResolver.Threshold;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;

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

    var channelNameResolverNode = jsonConfig.get("channelNameResolver");
    ChannelNameResolver channelNameResolver;
    if (channelNameResolverNode == null) {
      channelNameResolver = new DayChannelNameResolver();
    } else if (!channelNameResolverNode.isObject()) {
      throw new IllegalArgumentException("channelNameResolver must be an object");
    } else {
      var resolverTypeNode = channelNameResolverNode.get("type");
      if (resolverTypeNode == null) {
        throw new IllegalArgumentException("channelNameResolver.type is required");
      }
      if (!resolverTypeNode.isTextual()) {
        throw new IllegalArgumentException("channelNameResolver.type must be a string");
      }
      var resolverType = resolverTypeNode.asText();
      channelNameResolver = switch (resolverType) {
        case "day" -> new DayChannelNameResolver();
        case "day-tod" -> parseDayTodChannelNameResolver(channelNameResolverNode);
        default ->
            throw new IllegalArgumentException("Unknown channelNameResolver type: " + resolverType);
      };
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
        channelNameResolver,
        hasPerformedFirstLoadNode.asBoolean()
    );
  }

  private ChannelNameResolver parseDayTodChannelNameResolver(JsonNode channelNameResolverNode) {
    var thresholdsNode = channelNameResolverNode.get("thresholds");
    if (thresholdsNode == null) {
      throw new IllegalArgumentException("channelNameResolver.thresholds is required for day-tod");
    }

    if (!thresholdsNode.isArray()) {
      throw new IllegalArgumentException("channelNameResolver.thresholds must be an array");
    }

    var thresholdNodes = thresholdsNode.elements();
    var thresholds = new ArrayList<Threshold>();
    thresholdNodes.forEachRemaining(node -> {
      if (!node.isObject()) {
        throw new IllegalArgumentException("channelNameResolver.thresholds must be an array of objects");
      }
      var labelNode = node.get("label");
      if (labelNode == null) {
        throw new IllegalArgumentException("channelNameResolver.thresholds.label is required");
      }
      if (!labelNode.isTextual()) {
        throw new IllegalArgumentException("channelNameResolver.thresholds.label must be a string");
      }

      var startNode = node.get("start");
      if (startNode == null) {
        throw new IllegalArgumentException("channelNameResolver.thresholds.start is required");
      }
      if (!startNode.isTextual()) {
        throw new IllegalArgumentException("channelNameResolver.thresholds.start must be a string");
      }
      if (!startNode.asText().matches("\\d{2}:\\d{2}")) {
        throw new IllegalArgumentException("channelNameResolver.thresholds.start must be in the format HH:mm");
      }

      var endNode = node.get("end");
      if (endNode == null) {
        throw new IllegalArgumentException("channelNameResolver.thresholds.end is required");
      }
      if (!endNode.isTextual()) {
        throw new IllegalArgumentException("channelNameResolver.thresholds.end must be a string");
      }
      if (!endNode.asText().matches("\\d{2}:\\d{2}")) {
        throw new IllegalArgumentException("channelNameResolver.thresholds.end must be in the format HH:mm");
      }

      thresholds.add(new Threshold(labelNode.asText(), startNode.asText(), endNode.asText()));
    });

    for (int i = 0; i < thresholds.size(); i++) {
      for (int j = i + 1; j < thresholds.size(); j++) {
        var threshold1 = thresholds.get(i);
        var threshold2 = thresholds.get(j);
        if (threshold1.start().compareTo(threshold2.end()) < 0 && threshold1.end().compareTo(threshold2.start()) > 0) {
          throw new IllegalArgumentException("channelNameResolver.thresholds must not overlap");
        }
      }
    }

    return new DayTodChannelNameResolver(thresholds);
  }
}