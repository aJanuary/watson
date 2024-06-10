package com.ajanuary.watson.api;

import com.ajanuary.watson.config.ConfigParser.ObjectConfigParserWithValue;

public class ApiConfigYamlParser {

  private ApiConfigYamlParser() {}

  public static ApiConfig parse(ObjectConfigParserWithValue configParser) {
    var channel = configParser.get("channel").string().defaultingTo("api-messages").value();
    return new ApiConfig(channel);
  }
}
