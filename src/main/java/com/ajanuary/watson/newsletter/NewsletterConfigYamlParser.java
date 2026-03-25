package com.ajanuary.watson.newsletter;

import com.ajanuary.watson.config.ConfigParser.ObjectConfigParserWithValue;
import java.net.URI;
import java.time.Duration;

public class NewsletterConfigYamlParser {

  private NewsletterConfigYamlParser() {}

  public static NewsletterConfig parse(ObjectConfigParserWithValue configParser) {
    var channel = configParser.get("channel").string().required().value();
    var feedUrl = configParser.get("feedUrl").string().required().map(URI::create);
    var pollInterval =
        configParser.get("pollInterval").string().required().map(v -> Duration.parse("PT" + v));
    return new NewsletterConfig(channel, feedUrl, pollInterval);
  }
}
