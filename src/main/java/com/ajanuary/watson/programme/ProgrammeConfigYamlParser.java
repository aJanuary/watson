package com.ajanuary.watson.programme;

import com.ajanuary.watson.config.ConfigException;
import com.ajanuary.watson.config.ConfigParser.ObjectConfigParserWithValue;
import com.ajanuary.watson.programme.channelnameresolvers.ChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayTodChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayTodChannelNameResolver.Threshold;
import com.ajanuary.watson.programme.channelnameresolvers.LocChannelNameResolver;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Optional;

public class ProgrammeConfigYamlParser {

  private ProgrammeConfigYamlParser() {}

  public static ProgrammeConfig parse(ObjectConfigParserWithValue configParser, ZoneId timezone) {
    var programmeUrl = configParser.get("programmeUrl").string().required().map(URI::create);
    var assignDiscordPostsApiUrl =
        configParser.get("assignDiscordPostsApiUrl").string().defaultingTo("").<Optional<URI>>map(v -> {
          if (v.isEmpty()) {
            return Optional.empty();
          } else {
            return Optional.of(URI.create(v));
          }
        });
    var majorAnnouncementsChannel =
        configParser
            .get("majorAnnouncementsChannel")
            .string()
            .defaultingTo("programme-announcements")
            .value();

    var nowOnConfig =
        configParser
            .get("nowOn")
            .object()
            .map(
                p -> {
                  var channel = p.get("channel").string().defaultingTo("now-on").value();
                  var timeBeforeToAdd =
                      p.get("timeBeforeToAdd")
                          .string()
                          .required()
                          .map(ProgrammeConfigYamlParser::parseDuration);
                  var timeAfterToKeep =
                      p.get("timeAfterToKeep")
                          .string()
                          .required()
                          .map(ProgrammeConfigYamlParser::parseDuration);
                  return new ProgrammeConfig.NowOnConfig(channel, timeBeforeToAdd, timeAfterToKeep);
                });

    var channelNameResolver =
        configParser
            .get("channelNameResolver")
            .object()
            .map(
                channelNameResolverConfig ->
                    channelNameResolverConfig
                        .get("type")
                        .string()
                        .required()
                        .map(
                            resolverType ->
                                switch (resolverType) {
                                  case "day" -> new DayChannelNameResolver(timezone);
                                  case "day-tod" ->
                                      parseDayTodChannelNameResolver(channelNameResolverConfig, timezone);
                                  case "loc" ->
                                      parseLocChannelNameResolver(channelNameResolverConfig);
                                  default ->
                                      throw new IllegalArgumentException(
                                          "Unknown resolver " + resolverType);
                                }))
            .orElseGet(() -> new DayChannelNameResolver(timezone));

    var links =
        configParser.get("links").list().required().value().stream()
            .map(
                linkConfig -> {
                  var linkConfigObj = linkConfig.object().required();
                  var name = linkConfigObj.get("name").string().required().value();
                  var label = linkConfigObj.get("label").string().required().value();
                  return new ProgrammeConfig.Link(name, label);
                })
            .toList();

    var locations =
        configParser.get("locations").list().required().value().stream()
            .map(
                locationConfig -> {
                  var locationConfigObj = locationConfig.object().required();
                  var id = locationConfigObj.get("id").string().required().value();
                  var name = locationConfigObj.get("name").string().required().value();
                  return new ProgrammeConfig.Location(id, name);
                })
            .toList();

    var hasPerformedFirstLoadNode =
        configParser.get("hasPerformedFirstLoad").bool().defaultingTo(true).value();

    return new ProgrammeConfig(
        programmeUrl,
        assignDiscordPostsApiUrl,
        majorAnnouncementsChannel,
        nowOnConfig,
        channelNameResolver,
        links,
        locations,
        hasPerformedFirstLoadNode);
  }

  private static ChannelNameResolver parseLocChannelNameResolver(
      ObjectConfigParserWithValue configParser) {
    var roomMapping =
        configParser
            .get("locMappings")
            .object()
            .required()
            .toMap(mapping -> mapping.string().required().value());
    return new LocChannelNameResolver(roomMapping);
  }

  private static ChannelNameResolver parseDayTodChannelNameResolver(
      ObjectConfigParserWithValue configParser, ZoneId timezone) {
    var thresholdsConfig = configParser.get("thresholds").list().required();
    var thresholds =
        thresholdsConfig.value().stream()
            .map(
                thresholdConfig -> {
                  var thresholdConfigObj = thresholdConfig.object().required();
                  var label = thresholdConfigObj.get("label").string().required().value();
                  var start =
                      thresholdConfigObj
                          .get("start")
                          .string()
                          .required()
                          .validate(ProgrammeConfigYamlParser::validateTime)
                          .value();
                  var end =
                      thresholdConfigObj
                          .get("end")
                          .string()
                          .required()
                          .validate(ProgrammeConfigYamlParser::validateTime)
                          .value();
                  return new Threshold(label, start, end);
                })
            .toList();

    for (int i = 0; i < thresholds.size(); i++) {
      for (int j = i + 1; j < thresholds.size(); j++) {
        var threshold1 = thresholds.get(i);
        var threshold2 = thresholds.get(j);
        if (threshold1.start().compareTo(threshold2.end()) < 0
            && threshold1.end().compareTo(threshold2.start()) > 0) {
          throw new ConfigException(thresholdsConfig.path() + " must not overlap");
        }
      }
    }

    return new DayTodChannelNameResolver(thresholds, timezone);
  }

  private static Optional<String> validateTime(String value) {
    if (!value.matches("^\\d{2}:\\d{2}$")) {
      return Optional.of("must be in the format hh:mm");
    } else {
      return Optional.empty();
    }
  }

  private static Duration parseDuration(String value) {
    return Duration.parse("PT" + value);
  }
}
