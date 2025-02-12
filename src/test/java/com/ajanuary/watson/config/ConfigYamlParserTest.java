package com.ajanuary.watson.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ajanuary.watson.programme.channelnameresolvers.DayChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayTodChannelNameResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.net.URI;
import java.util.Optional;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.junit.jupiter.api.Test;

public class ConfigYamlParserTest {

  @Test
  void readsDiscordBotTokenFromSecrets() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: the-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertEquals("the-token", config.discordBotToken());
  }

  @Test
  void throwsIfDiscordBotTokenMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
    {}
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("discordBotToken is required", thrown.getMessage());
  }

  @Test
  void readsPortalApiKeyFromSecrets() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: the-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertEquals("the-key", config.portalApiKey());
  }

  @Test
  void readsCoreConfigFrom() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: the-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertEquals("the-guild-id", config.guildId());
  }

  @Test
  void errorsIfGuildIdIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: 10
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("guildId must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfGuildIdIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        databasePath: test.db
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("guildId is required", thrown.getMessage());
  }

  @Test
  void readsDatabasePathFromConfig() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: the-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertEquals("the-db-path", config.databasePath());
  }

  @Test
  void errorsIfDatabasePathIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: 10
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("databasePath must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfDatabasePathIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("databasePath is required", thrown.getMessage());
  }

  @Test
  void errorsIfTimezoneIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("timezone is required", thrown.getMessage());
  }

  @Test
  void errorsIfTimezoneIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("timezone must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfTimezoneIsInvalid() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: Invalid/Timezone
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for timezone: Unknown time-zone ID: Invalid/Timezone",
        thrown.getMessage());
  }

  @Test
  void alarmsConfigIsOptional() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.alarms().isEmpty(), "no alarms config");
  }

  @Test
  void parsesAlarmsTimeBeforeToNotify() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("PT15M", config.alarms().get().timeBeforeToNotify().toString());
  }

  @Test
  void errorsIfAlarmsTimeBeforeToNotifyIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("alarms.timeBeforeToNotify is required", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsTimeBeforeToNotifyIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("alarms.timeBeforeToNotify must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsTimeBeforeToNotifyIsInvalidDuration() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15x
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for alarms.timeBeforeToNotify: Text cannot be parsed to a Duration",
        thrown.getMessage());
  }

  @Test
  void parsesAlarmsMaxTimeAfterToNotify() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("PT5M", config.alarms().get().maxTimeAfterToNotify().toString());
  }

  @Test
  void errorsIfAlarmsMaxTimeAfterToNotifyIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("alarms.maxTimeAfterToNotify is required", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsMaxTimeAfterToNotifyIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("alarms.maxTimeAfterToNotify must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsMaxTimeAfterToNotifyIsInvalidDuration() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5x
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for alarms.maxTimeAfterToNotify: Text cannot be parsed to a Duration",
        thrown.getMessage());
  }

  @Test
  void parsesAlarmsMinTimeBetweenDMs() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("PT0.5S", config.alarms().get().minTimeBetweenDMs().toString());
  }

  @Test
  void errorsIfAlarmsMinTimeBetweenDMsIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("alarms.minTimeBetweenDMs is required", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsMinTimeBetweenDMsIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("alarms.minTimeBetweenDMs must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsMinTimeBetweenDMsIsInvalidDuration() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5x
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for alarms.minTimeBetweenDMs: Text cannot be parsed to a Duration",
        thrown.getMessage());
  }

  @Test
  void parsesAlarmEmoji() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
          alarmEmoji: U+1F514
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals(Emoji.fromUnicode("U+1F514"), config.alarms().get().alarmEmoji());
  }

  @Test
  void defaultsAlarmEmojiToClockEmoji() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals(Emoji.fromUnicode("U+23F0"), config.alarms().get().alarmEmoji());
  }

  @Test
  void errorsIfAlarmEmojiIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
          alarmEmoji: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("alarms.alarmEmoji must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmEmojiIsInvalidEmoji() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
          alarmEmoji: ""
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for alarms.alarmEmoji: Unicode may not be empty", thrown.getMessage());
  }

  @Test
  void parsesAlarmsAlarmsChannel() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
          alarmsChannel: the-alarms-channel
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("the-alarms-channel", config.alarms().get().alarmsChannel());
  }

  @Test
  void defaultsAlarmsAlarmsChannelToReminders() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("reminders", config.alarms().get().alarmsChannel());
  }

  @Test
  void errorsIfAlarmsAlarmsChannelIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
          alarmsChannel: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("alarms.alarmsChannel must be a string", thrown.getMessage());
  }

  @Test
  void apiConfigIsOptional() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assert (config.api().isEmpty());
  }

  @Test
  void parsesApiChannel() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
      """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        api:
          channel: the-channel
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.api().isPresent(), "api config is present");
    assertEquals("the-channel", config.api().get().channel());
  }

  @Test
  void defaultsApiChannelToApiMessages() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        api: {}
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.api().isPresent(), "api config is present");
    assertEquals("api-messages", config.api().get().channel());
  }

  @Test
  void errorsIfApiChannelIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        api:
          channel: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("api.channel must be a string", thrown.getMessage());
  }

  @Test
  void membershipConfigIsOptional() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assert (config.membership().isEmpty());
  }

  @Test
  void parsesMembershipApiRoot() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/the-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals(
        URI.create("https://example.com/the-api-root"), config.membership().get().membersApiUrl());
  }

  @Test
  void errorsIfMembershipApiRootIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership: {}
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("membership.membersApiUrl is required", thrown.getMessage());
  }

  @Test
  void errorsIfMembershipApiRootIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("membership.membersApiUrl must be a string", thrown.getMessage());
  }

  @Test
  void parsesMembershipMemberRole() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
          memberRole: the-member-role
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("the-member-role", config.membership().get().memberRole());
  }

  @Test
  void defaultsMembershipMemberRoleToMember() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("member", config.membership().get().memberRole());
  }

  @Test
  void errorsIfMembershipMemberRoleIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
          memberRole: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("membership.memberRole must be a string", thrown.getMessage());
  }

  @Test
  void parsesMembershipUnverifiedRole() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
          unverifiedRole: the-unverified-role
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("the-unverified-role", config.membership().get().unverifiedRole());
  }

  @Test
  void defaultsMembershipUnverifiedRoleToUnverified() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("unverified", config.membership().get().unverifiedRole());
  }

  @Test
  void errorsIfMembershipUnverifiedRoleIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
          unverifiedRole: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("membership.unverifiedRole must be a string", thrown.getMessage());
  }

  @Test
  void parsesMembershipAdditionalRoles() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
          additionalRoles:
            role1: the-role-1
            role2: the-role-2
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals(
        2, config.membership().get().additionalRoles().size(), "additional roles has 2 entries");
    assertEquals("the-role-1", config.membership().get().additionalRoles().get("role1"));
    assertEquals("the-role-2", config.membership().get().additionalRoles().get("role2"));
  }

  @Test
  void errorsIfMembershipAdditionalRolesIsNotAnObject() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
          additionalRoles: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("membership.additionalRoles must be an object", thrown.getMessage());
  }

  @Test
  void errorsIfMembershipAdditionalRolesContainsNonStringValues() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
          additionalRoles:
            role1: the-role-1
            role2: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("membership.additionalRoles.role2 must be a string", thrown.getMessage());
  }

  @Test
  void defaultsAdditionalRolesToEmptyMap() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        membership:
          membersApiUrl: https://example.com/some-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertTrue(config.membership().get().additionalRoles().isEmpty(), "additional roles is empty");
  }

  @Test
  void programmeConfigIsOptional() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assert (config.programme().isEmpty());
  }

  @Test
  void parsesProgrammeProgrammeUrl() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/the-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertEquals(
        URI.create("https://example.com/the-programme-url"),
        config.programme().get().programmeUrl());
  }

  @Test
  void parsesProgrammeAssignDiscordPostsApiUrl() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/the-programme-url
          assignDiscordPostsApiUrl: https://example.com/the-assign-discord-posts-api-url
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertEquals(
        Optional.of(URI.create("https://example.com/the-assign-discord-posts-api-url")),
        config.programme().get().assignDiscordPostsApiUrl());
  }

  @Test
  void errorsIfProgrammeProgrammeUrlIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme: {}
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.programmeUrl is required", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeProgrammeUrlIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.programmeUrl must be a string", thrown.getMessage());
  }

  @Test
  void nowOnConfigIsOptional() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().nowOn().isEmpty(), "nowOn config is not present");
  }

  @Test
  void parsesNoOnChannelId() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            channel: the-now-on-channel-id
            timeBeforeToAdd: 15m
            timeAfterToKeep: 15m
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().nowOn().isPresent(), "nowOn config is present");
    assertEquals("the-now-on-channel-id", config.programme().get().nowOn().get().channel());
  }

  @Test
  void nowOnChannelDefaultsToNowOn() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeBeforeToAdd: 15m
            timeAfterToKeep: 15m
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().nowOn().isPresent(), "nowOn config is present");
    assertEquals("now-on", config.programme().get().nowOn().get().channel());
  }

  @Test
  void errorsIfNowOnChannelIdIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            channel: 10
            timeBeforeToAdd: 15m
            timeAfterToKeep: 15m
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.nowOn.channel must be a string", thrown.getMessage());
  }

  @Test
  void parsesProgrammeNowOnTimeBeforeToAdd() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeBeforeToAdd: 15m
            timeAfterToKeep: 15m
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().nowOn().isPresent(), "nowOn config is present");
    assertEquals("PT15M", config.programme().get().nowOn().get().timeBeforeToAdd().toString());
  }

  @Test
  void errorsIfProgrammeNowOnTimeBeforeToAddIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeAfterToKeep: 15m
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.nowOn.timeBeforeToAdd is required", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeNowOnTimeBeforeToAddIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeBeforeToAdd: 15
            timeAfterToKeep: 15m
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.nowOn.timeBeforeToAdd must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeNowOnTimeBeforeToAddIsInvalidDuration() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeBeforeToAdd: 15x
            timeAfterToKeep: 15m
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for programme.nowOn.timeBeforeToAdd: Text cannot be parsed to a Duration",
        thrown.getMessage());
  }

  @Test
  void parsesProgrammeNowOnTimeAfterToKeep() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeBeforeToAdd: 15m
            timeAfterToKeep: 15m
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().nowOn().isPresent(), "nowOn config is present");
    assertEquals("PT15M", config.programme().get().nowOn().get().timeAfterToKeep().toString());
  }

  @Test
  void errorsIfProgrammeNowOnTimeAfterToKeepIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeBeforeToAdd: 15m
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.nowOn.timeAfterToKeep is required", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeNowOnTimeAfterToKeepIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeBeforeToAdd: 15m
            timeAfterToKeep: 15
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.nowOn.timeAfterToKeep must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeNowOnTimeAfterToKeepIsInvalidDuration() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          nowOn:
            timeBeforeToAdd: 15m
            timeAfterToKeep: 15x
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for programme.nowOn.timeAfterToKeep: Text cannot be parsed to a Duration",
        thrown.getMessage());
  }

  @Test
  void parsesProgrammeMajorAnnouncementsChannel() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          majorAnnouncementsChannel: the-major-announcements-channel
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertEquals(
        "the-major-announcements-channel", config.programme().get().majorAnnouncementsChannel());
  }

  @Test
  void defaultsProgrammeMajorAnnouncementsChannelToProgrammeAnnouncements()
      throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertEquals("programme-announcements", config.programme().get().majorAnnouncementsChannel());
  }

  @Test
  void errorsIfProgrammeMajorAnnouncementsChannelIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          majorAnnouncementsChannel: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.majorAnnouncementsChannel must be a string", thrown.getMessage());
  }

  @Test
  void parsesProgrammeHasPerformedFirstLoad() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
          guildId: some-guild-id
          databasePath: some-db-path
          timezone: America/New_York
          programme:
            programmeUrl: https://example.com/some-programme-url
            assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
            hasPerformedFirstLoad: true
            links:
              - name: some-name
                label: some-label
            locations:
              - id: some-id
                name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().hasPerformedFirstLoad(), "hasPerformedFirstLoad is true");
  }

  @Test
  void defaultsProgrammeHasPerformedFirstLoadToTrue() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
          guildId: some-guild-id,
          databasePath: some-db-path,
          timezone: America/New_York
          programme:
            programmeUrl: https://example.com/some-programme-url
            assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
            links:
              - name: some-name
                label: some-label
            locations:
              - id: some-id
                name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().hasPerformedFirstLoad(), "hasPerformedFirstLoad is true");
  }

  @Test
  void errorsIfProgrammeHasPerformedFirstLoadIsNotBoolean() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
          guildId: some-guild-id,
          databasePath: some-db-path,
          timezone: America/New_York
          programme:
            programmeUrl: https://example.com/some-programme-url
            assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
            hasPerformedFirstLoad: 10
            links:
              - name: some-name
                label: some-label
            locations:
              - id: some-id
                name: some-name
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.hasPerformedFirstLoad must be a boolean", thrown.getMessage());
  }

  @Test
  void programmeChannelNameResolverIsOptional() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertInstanceOf(DayChannelNameResolver.class, config.programme().get().channelNameResolver());
  }

  @Test
  void errorsIfProgrammeChannelNameResolverIsNotAnObject() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.channelNameResolver must be an object", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeChannelNameResolverTypeIsMissing() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver: {}
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.channelNameResolver.type is required", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeChannelNameResolverTypeIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver:
            type: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.channelNameResolver.type must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeChannelNameResolverTypeIsInvalidType() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver:
            type: invalid
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for programme.channelNameResolver.type: Unknown resolver invalid",
        thrown.getMessage());
  }

  @Test
  void parsesProgrammeDayChannelNameResolver() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver:
            type: day
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertInstanceOf(DayChannelNameResolver.class, config.programme().get().channelNameResolver());
  }

  @Test
  void parsesProgrammeDayTodChannelNameResolver() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 00:00
                end: 12:00
              - label: afternoon
                start: 12:00
                end: 18:00
              - label: evening
                start: 18:00
                end: 24:00
          links:
            - name: some-name
              label: some-label
          locations:
            - id: some-id
              name: some-name
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(secretsConfig, jsonConfig);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertInstanceOf(
        DayTodChannelNameResolver.class, config.programme().get().channelNameResolver());

    var resolver = (DayTodChannelNameResolver) config.programme().get().channelNameResolver();
    assertEquals(3, resolver.thresholds().size(), "resolver has 3 thresholds");
    assertEquals("morning", resolver.thresholds().get(0).label());
    assertEquals("00:00", resolver.thresholds().get(0).start());
    assertEquals("12:00", resolver.thresholds().get(0).end());
    assertEquals("afternoon", resolver.thresholds().get(1).label());
    assertEquals("12:00", resolver.thresholds().get(1).start());
    assertEquals("18:00", resolver.thresholds().get(1).end());
    assertEquals("evening", resolver.thresholds().get(2).label());
    assertEquals("18:00", resolver.thresholds().get(2).start());
    assertEquals("24:00", resolver.thresholds().get(2).end());
  }

  @Test
  void errorsIfProgrammeDayTodChannelNameResolverThresholdsAreMissing()
      throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver:
            type: day-tod
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.channelNameResolver.thresholds is required", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeDayTodChannelNameResolverThresholdsIsNotAList()
      throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver:
            type: day-tod
            thresholds: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.channelNameResolver.thresholds must be a list", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeDayTodChannelNameResolverThresholdsContainsNonObjectValues()
      throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "programme.channelNameResolver.thresholds[0] must be an object", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdIsMissingLabel() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - start: 00:00
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].label is required", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdLabelIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: 10
                start: 00:00
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].label must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdIsMissingStart() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].start is required", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdStartIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 10
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].start must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdStartIsWrongFormat() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: invalid
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for programme.channelNameResolver.thresholds[0].start: must be in the format hh:mm",
        thrown.getMessage());
  }

  @Test
  void errorsIfThresholdIsMissingEnd() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 00:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].end is required", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdEndIsNotString() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 00:00
                end: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].end must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdEndIsWrongFormat() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 00:00
                end: invalid
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals(
        "Malformed value for programme.channelNameResolver.thresholds[0].end: must be in the format hh:mm",
        thrown.getMessage());
  }

  @Test
  void errorsIfThresholdsOverlap() throws JsonProcessingException {
    var secretsConfig =
        new YAMLMapper()
            .readTree(
                """
      discordBotToken: some-token
      portalApiKey: some-key`
    """);
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        timezone: America/New_York
        programme:
          programmeUrl: https://example.com/some-programme-url
          assignDiscordPostsApiUrl: https://example.com/some-assign-discord-posts-api-url
          timeBeforeToAddToNowOn: 15m
          timeAfterToKeepInNowOn: 15m
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 00:00
                end: 12:00
              - label: afternoon
                start: 11:00
                end: 18:00
              - label: evening
                start: 18:00
                end: 24:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(secretsConfig, jsonConfig));

    assertEquals("programme.channelNameResolver.thresholds must not overlap", thrown.getMessage());
  }
}
