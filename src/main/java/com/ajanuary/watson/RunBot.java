package com.ajanuary.watson;

import com.ajanuary.watson.config.Config;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;

public class RunBot {
  public static void main(String[] args) throws IOException, InterruptedException {
    var dotenv = Dotenv.configure().directory(args[0]).load();
    var secrets = new Secrets(dotenv.get("DISCORD_BOT_TOKEN"), dotenv.get("MEMBERS_API_KEY"));

    var objectMapper = new YAMLMapper();
    var config = objectMapper.readValue(Paths.get(args[0], "config.yaml").toFile(), Config.class);

    new Bot(config, secrets);
  }
}
