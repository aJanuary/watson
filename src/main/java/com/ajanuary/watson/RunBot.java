package com.ajanuary.watson;

import com.ajanuary.watson.config.ConfigYamlParser;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.nio.file.Paths;

public class RunBot {

  public static void main(String[] args) throws IOException, InterruptedException {
    var dotenv = Dotenv.configure().filename(args[0]).load();

    var objectMapper = new YAMLMapper();
    var jsonConfig = objectMapper.readTree(Paths.get(args[1]).toFile());
    var config = new ConfigYamlParser().parse(jsonConfig, dotenv);
    new Bot(config);
  }
}
