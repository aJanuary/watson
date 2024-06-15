package com.ajanuary.watson;

import com.ajanuary.watson.config.ConfigYamlParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.nio.file.Paths;

public class RunBot {

  public static void main(String[] args) throws IOException, InterruptedException {
    if (args.length != 2) {
      System.err.println("Usage: RunBot <secrets file> <config file>");
      System.exit(1);
    }

    var objectMapper = new YAMLMapper();
    var jsonSecrets = objectMapper.readTree(Paths.get(args[0]).toFile());
    var jsonConfig = objectMapper.readTree(Paths.get(args[1]).toFile());
    var config = new ConfigYamlParser().parse(jsonSecrets, jsonConfig);
    new Bot(config);
  }
}
