package com.ajanuary.watson.alarms;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.ZoneId;

public class AlarmsConfigYamlParser {
  public AlarmsConfig parse(JsonNode jsonConfig) {
    var timezoneNode = jsonConfig.get("timezone");
    if (timezoneNode == null) {
      throw new IllegalArgumentException("timezone must be specified");
    } else if (!timezoneNode.isTextual()) {
      throw new IllegalArgumentException("timezone must be a string");
    }
    var timezone = ZoneId.of(timezoneNode.asText());

    var alarmEmojiNode = jsonConfig.get("alarmEmoji");
    String alarmEmoji;
    if (alarmEmojiNode == null) {
      alarmEmoji = "U+23F0";
    } else if (!alarmEmojiNode.isTextual()) {
      throw new IllegalArgumentException("alarmEmoji must be a string");
    } else {
      alarmEmoji = alarmEmojiNode.asText();
    }

    var timeBeforeToNotifyNode = jsonConfig.get("timeBeforeToNotify");
    if (timeBeforeToNotifyNode == null) {
      throw new IllegalArgumentException("timeBeforeToNotify must be specified");
    } else if (!timeBeforeToNotifyNode.isTextual()) {
      throw new IllegalArgumentException("timeBeforeToNotify must be a string");
    }
    var timeBeforeToNotify = java.time.Duration.parse("PT" + timeBeforeToNotifyNode.asText());

    var maxTimeAfterToNotifyNode = jsonConfig.get("maxTimeAfterToNotify");
    if (maxTimeAfterToNotifyNode == null) {
      throw new IllegalArgumentException("maxTimeAfterToNotify must be specified");
    } else if (!maxTimeAfterToNotifyNode.isTextual()) {
      throw new IllegalArgumentException("maxTimeAfterToNotify must be a string");
    }
    var maxTimeAfterToNotify = java.time.Duration.parse("PT" + maxTimeAfterToNotifyNode.asText());

    var minTimeBetweenDMsNode = jsonConfig.get("minTimeBetweenDMs");
    if (minTimeBetweenDMsNode == null) {
      throw new IllegalArgumentException("minTimeBetweenDMs must be specified");
    } else if (!minTimeBetweenDMsNode.isTextual()) {
      throw new IllegalArgumentException("minTimeBetweenDMs must be a string");
    }
    var minTimeBetweenDMs = java.time.Duration.parse("PT" + minTimeBetweenDMsNode.asText());

    return new AlarmsConfig(timezone, alarmEmoji, timeBeforeToNotify, maxTimeAfterToNotify, minTimeBetweenDMs);
  }
}
