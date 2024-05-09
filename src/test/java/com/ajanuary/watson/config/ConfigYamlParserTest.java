package com.ajanuary.watson.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ajanuary.watson.programme.channelnameresolvers.DayChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayTodChannelNameResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import fakes.DotenvFake;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import org.junit.jupiter.api.Test;

public class ConfigYamlParserTest {

  @Test
  void readsDiscordBotTokenFromDotenv() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "the-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertEquals("the-token", config.discordBotToken());
  }

  @Test
  void throwsIfDiscordBotTokenMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake();
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("DISCORD_BOT_TOKEN is required", thrown.getMessage());
  }

  @Test
  void readsCoreConfigFrom() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: the-guild-id
        databasePath: some-db-path
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertEquals("the-guild-id", config.guildId());
  }

  @Test
  void errorsIfGuildIdIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: 10
        databasePath: some-db-path
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("guildId must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfGuildIdIsMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        databasePath: test.db
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("guildId is required", thrown.getMessage());
  }

  @Test
  void readsDatabasePathFromConfig() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: the-db-path
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertEquals("the-db-path", config.databasePath());
  }

  @Test
  void errorsIfDatabasePathIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("databasePath must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfDatabasePathIsMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("databasePath is required", thrown.getMessage());
  }

  @Test
  void alarmsConfigIsOptional() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.alarms().isEmpty(), "no alarms config");
  }

  @Test
  void parsesAlarmsTimezone() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("America/New_York", config.alarms().get().timezone().getId());
  }

  @Test
  void errorsIfAlarmsTimezoneIsMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.timezone is required", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsTimezoneIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: 10
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.timezone must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsTimezoneIsInvalid() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: Invalid/Timezone
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "Malformed value for alarms.timezone: Unknown time-zone ID: Invalid/Timezone",
        thrown.getMessage());
  }

  @Test
  void parsesAlarmsTimeBeforeToNotify() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("PT15M", config.alarms().get().timeBeforeToNotify().toString());
  }

  @Test
  void errorsIfAlarmsTimeBeforeToNotifyIsMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.timeBeforeToNotify is required", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsTimeBeforeToNotifyIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.timeBeforeToNotify must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsTimeBeforeToNotifyIsInvalidDuration() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15x
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "Malformed value for alarms.timeBeforeToNotify: Text cannot be parsed to a Duration",
        thrown.getMessage());
  }

  @Test
  void parsesAlarmsMaxTimeAfterToNotify() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("PT5M", config.alarms().get().maxTimeAfterToNotify().toString());
  }

  @Test
  void errorsIfAlarmsMaxTimeAfterToNotifyIsMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.maxTimeAfterToNotify is required", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsMaxTimeAfterToNotifyIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.maxTimeAfterToNotify must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsMaxTimeAfterToNotifyIsInvalidDuration() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5x
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "Malformed value for alarms.maxTimeAfterToNotify: Text cannot be parsed to a Duration",
        thrown.getMessage());
  }

  @Test
  void parsesAlarmsMinTimeBetweenDMs() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals("PT0.5S", config.alarms().get().minTimeBetweenDMs().toString());
  }

  @Test
  void errorsIfAlarmsMinTimeBetweenDMsIsMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.minTimeBetweenDMs is required", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsMinTimeBetweenDMsIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.minTimeBetweenDMs must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmsMinTimeBetweenDMsIsInvalidDuration() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5x
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "Malformed value for alarms.minTimeBetweenDMs: Text cannot be parsed to a Duration",
        thrown.getMessage());
  }

  @Test
  void parsesAlarmEmoji() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
          alarmEmoji: U+1F514
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals(Emoji.fromUnicode("U+1F514"), config.alarms().get().alarmEmoji());
  }

  @Test
  void defaultsAlarmEmojiToClockEmoji() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.alarms().isPresent(), "alarms config is present");
    assertEquals(Emoji.fromUnicode("U+23F0"), config.alarms().get().alarmEmoji());
  }

  @Test
  void errorsIfAlarmEmojiIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
          alarmEmoji: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("alarms.alarmEmoji must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfAlarmEmojiIsInvalidEmoji() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        alarms:
          timezone: America/New_York
          timeBeforeToNotify: 15m
          maxTimeAfterToNotify: 5m
          minTimeBetweenDMs: 0.5s
          alarmEmoji: ""
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "Malformed value for alarms.alarmEmoji: Unicode may not be empty", thrown.getMessage());
  }

  @Test
  void membershipConfigIsOptional() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assert (config.membership().isEmpty());
  }

  @Test
  void membershipModuleRequiresMembersApiKey() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("MEMBERS_API_KEY is required", thrown.getMessage());
  }

  @Test
  void readsMembersApiKeyFromDotenv() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "the-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/the-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("the-key", config.membership().get().membersApiKey());
  }

  @Test
  void parsesMembershipApiRoot() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/the-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("https://example.com/the-api-root", config.membership().get().membersApiRoot());
  }

  @Test
  void errorsIfMembershipApiRootIsMissing() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership: {}
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("membership.membersApiRoot is required", thrown.getMessage());
  }

  @Test
  void errorsIfMembershipApiRootIsNotString() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("membership.membersApiRoot must be a string", thrown.getMessage());
  }

  @Test
  void parsesMembershipDiscordModsChannel() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          discordModsChannel: the-mods-channel
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("the-mods-channel", config.membership().get().discordModsChannel());
  }

  @Test
  void defaultsMembershipDiscordModsChannelToDiscordMods() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("discord-mods", config.membership().get().discordModsChannel());
  }

  @Test
  void errorsIfMembershipDiscordModsChannelIsNotString() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          discordModsChannel: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("membership.discordModsChannel must be a string", thrown.getMessage());
  }

  @Test
  void parsesMembershipMemberRole() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          memberRole: the-member-role
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("the-member-role", config.membership().get().memberRole());
  }

  @Test
  void defaultsMembershipMemberRoleToMember() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("member", config.membership().get().memberRole());
  }

  @Test
  void errorsIfMembershipMemberRoleIsNotString() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          memberRole: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("membership.memberRole must be a string", thrown.getMessage());
  }

  @Test
  void parsesMembershipUnverifiedRole() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          unverifiedRole: the-unverified-role
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("the-unverified-role", config.membership().get().unverifiedRole());
  }

  @Test
  void defaultsMembershipUnverifiedRoleToUnverified() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals("unverified", config.membership().get().unverifiedRole());
  }

  @Test
  void errorsIfMembershipUnverifiedRoleIsNotString() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          unverifiedRole: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("membership.unverifiedRole must be a string", thrown.getMessage());
  }

  @Test
  void parsesMembershipAdditionalRoles() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          additionalRoles:
            role1: the-role-1
            role2: the-role-2
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertEquals(
        2, config.membership().get().additionalRoles().size(), "additional roles has 2 entries");
    assertEquals("the-role-1", config.membership().get().additionalRoles().get("role1"));
    assertEquals("the-role-2", config.membership().get().additionalRoles().get("role2"));
  }

  @Test
  void errorsIfMembershipAdditionalRolesIsNotAnObject() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          additionalRoles: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("membership.additionalRoles must be an object", thrown.getMessage());
  }

  @Test
  void errorsIfMembershipAdditionalRolesContainsNonStringValues() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
          additionalRoles:
            role1: the-role-1
            role2: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("membership.additionalRoles.role2 must be a string", thrown.getMessage());
  }

  @Test
  void defaultsAdditionalRolesToEmptyMap() throws JsonProcessingException {
    var dotenv =
        new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token").add("MEMBERS_API_KEY", "some-key");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        membership:
          membersApiRoot: https://example.com/some-api-root
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.membership().isPresent(), "membership config is present");
    assertTrue(config.membership().get().additionalRoles().isEmpty(), "additional roles is empty");
  }

  @Test
  void programmeConfigIsOptional() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assert (config.programme().isEmpty());
  }

  @Test
  void parsesProgrammeProgrammeUrl() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/the-programme-url
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertEquals("https://example.com/the-programme-url", config.programme().get().programmeUrl());
  }

  @Test
  void errorsIfProgrammeProgrammeUrlIsMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme: {}
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.programmeUrl is required", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeProgrammeUrlIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.programmeUrl must be a string", thrown.getMessage());
  }

  @Test
  void parsesProgrammeMajorAnnouncementsChannel() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          majorAnnouncementsChannel: the-major-announcements-channel
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertEquals(
        "the-major-announcements-channel", config.programme().get().majorAnnouncementsChannel());
  }

  @Test
  void defaultsProgrammeMajorAnnouncementsChannelToProgrammeAnnouncements()
      throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertEquals("programme-announcements", config.programme().get().majorAnnouncementsChannel());
  }

  @Test
  void errorsIfProgrammeMajorAnnouncementsChannelIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          majorAnnouncementsChannel: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.majorAnnouncementsChannel must be a string", thrown.getMessage());
  }

  @Test
  void parsesProgrammeHasPerformedFirstLoad() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        {
          "guildId": "some-guild-id",
          "databasePath": "some-db-path",
          "programme": {
            "programmeUrl": "https://example.com/some-programme-url",
            "hasPerformedFirstLoad": true
          }
        }
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().hasPerformedFirstLoad(), "hasPerformedFirstLoad is true");
  }

  @Test
  void defaultsProgrammeHasPerformedFirstLoadToTrue() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        {
          "guildId": "some-guild-id",
          "databasePath": "some-db-path",
          "programme": {
            "programmeUrl": "https://example.com/some-programme-url"
          }
        }
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertTrue(config.programme().get().hasPerformedFirstLoad(), "hasPerformedFirstLoad is true");
  }

  @Test
  void errorsIfProgrammeHasPerformedFirstLoadIsNotBoolean() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        {
          "guildId": "some-guild-id",
          "databasePath": "some-db-path",
          "programme": {
            "programmeUrl": "https://example.com/some-programme-url",
            "hasPerformedFirstLoad": 10
          }
        }
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.hasPerformedFirstLoad must be a boolean", thrown.getMessage());
  }

  @Test
  void programmeChannelNameResolverIsOptional() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertInstanceOf(DayChannelNameResolver.class, config.programme().get().channelNameResolver());
  }

  @Test
  void errorsIfProgrammeChannelNameResolverIsNotAnObject() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.channelNameResolver must be an object", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeChannelNameResolverTypeIsMissing() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver: {}
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.channelNameResolver.type is required", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeChannelNameResolverTypeIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.channelNameResolver.type must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeChannelNameResolverTypeIsInvalidType() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: invalid
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "Malformed value for programme.channelNameResolver.type: Unknown resolver invalid",
        thrown.getMessage());
  }

  @Test
  void parsesProgrammeDayChannelNameResolver() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

    assertTrue(config.programme().isPresent(), "programme config is present");
    assertInstanceOf(DayChannelNameResolver.class, config.programme().get().channelNameResolver());
  }

  @Test
  void parsesProgrammeDayTodChannelNameResolver() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
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
        """);

    var parser = new ConfigYamlParser();
    var config = parser.parse(jsonConfig, dotenv);

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
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.channelNameResolver.thresholds is required", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeDayTodChannelNameResolverThresholdsIsNotAList()
      throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.channelNameResolver.thresholds must be a list", thrown.getMessage());
  }

  @Test
  void errorsIfProgrammeDayTodChannelNameResolverThresholdsContainsNonObjectValues()
      throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "programme.channelNameResolver.thresholds[0] must be an object", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdIsMissingLabel() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - start: 00:00
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].label is required", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdLabelIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: 10
                start: 00:00
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].label must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdIsMissingStart() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].start is required", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdStartIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 10
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].start must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdStartIsWrongFormat() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: invalid
                end: 12:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "Malformed value for programme.channelNameResolver.thresholds[0].start: must be in the format hh:mm",
        thrown.getMessage());
  }

  @Test
  void errorsIfThresholdIsMissingEnd() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 00:00
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].end is required", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdEndIsNotString() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 00:00
                end: 10
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "programme.channelNameResolver.thresholds[0].end must be a string", thrown.getMessage());
  }

  @Test
  void errorsIfThresholdEndIsWrongFormat() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
          channelNameResolver:
            type: day-tod
            thresholds:
              - label: morning
                start: 00:00
                end: invalid
        """);

    var parser = new ConfigYamlParser();
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals(
        "Malformed value for programme.channelNameResolver.thresholds[0].end: must be in the format hh:mm",
        thrown.getMessage());
  }

  @Test
  void errorsIfThresholdsOverlap() throws JsonProcessingException {
    var dotenv = new DotenvFake().add("DISCORD_BOT_TOKEN", "some-token");
    var jsonConfig =
        new YAMLMapper()
            .readTree(
                """
        guildId: some-guild-id
        databasePath: some-db-path
        programme:
          programmeUrl: https://example.com/some-programme-url
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
    var thrown = assertThrows(ConfigException.class, () -> parser.parse(jsonConfig, dotenv));

    assertEquals("programme.channelNameResolver.thresholds must not overlap", thrown.getMessage());
  }
}
