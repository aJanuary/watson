package com.ajanuary.watson.alarms;

import com.ajanuary.watson.config.ConfigParser.ObjectConfigParserWithValue;
import java.time.Duration;
import java.time.ZoneId;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class AlarmsConfigYamlParser {

  private AlarmsConfigYamlParser() {
  }

  public static AlarmsConfig parse(ObjectConfigParserWithValue configParser) {
    var timezone = configParser.get("timezone").string().required().map(ZoneId::of);
    var alarmEmoji = configParser.get("alarmEmoji").string().defaultingTo("U+23F0")
        .map(Emoji::fromUnicode);
    var timeBeforeToNotify = configParser.get("timeBeforeToNotify").string().required()
        .map(AlarmsConfigYamlParser::parseDuration);
    var maxTimeAfterToNotify = configParser.get("maxTimeAfterToNotify").string().required()
        .map(AlarmsConfigYamlParser::parseDuration);
    var minTimeBetweenDMs = configParser.get("minTimeBetweenDMs").string().required()
        .map(AlarmsConfigYamlParser::parseDuration);
    return new AlarmsConfig(timezone, alarmEmoji, timeBeforeToNotify, maxTimeAfterToNotify,
        minTimeBetweenDMs);
  }

  private static Duration parseDuration(String value) {
    return Duration.parse("PT" + value);
  }
}
