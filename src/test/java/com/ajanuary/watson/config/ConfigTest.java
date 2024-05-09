package com.ajanuary.watson.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ajanuary.watson.membership.MembershipConfig;
import com.ajanuary.watson.programme.ProgrammeConfig;
import com.ajanuary.watson.programme.channelnameresolvers.DayChannelNameResolver;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.junit.jupiter.api.Test;

public class ConfigTest {

  @Test
  void validateErrorsIfGuildNotFound() {
    var jda = mock(JDA.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(null);

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.empty(),
            Optional.empty());
    var thrown = assertThrows(IllegalStateException.class, () -> config.validateDiscordConfig(jda));

    assertEquals(
        "Guild not found: the-guild-id. Is the ID correct, and has the bot been invited to the server?",
        thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfGuildFound() {
    var jda = mock(JDA.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(mock(Guild.class));

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.empty(),
            Optional.empty());

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfDiscordModsChannelNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    when(guild.getTextChannelById("the-channel-id")).thenReturn(null);

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "the-channel-id",
                    "some-mods-channel",
                    "some-unverified-role",
                    Map.of())),
            Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Channel not found: the-channel-id", thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfDiscordModsChannelFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "the-channel-id",
                    "some-member-role",
                    "some-unverified-role",
                    Map.of())),
            Optional.empty());

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfMultipleDiscordModsChannelsFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class), mock(TextChannel.class)));

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "the-channel-id",
                    "some-member-role",
                    "some-unverified-role",
                    Map.of())),
            Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Multiple channels found with the name: the-channel-id", thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfAllRolesFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any channels, as we don't care about them for this test
    when(guild.getTextChannelsByName(any(), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "some-mods-channel",
                    "the-member-role",
                    "the-unverified-role",
                    Map.of("some-role-label", "the-role-id"))),
            Optional.empty());

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfMemberRoleNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any channels, as we don't care about them for this test
    when(guild.getTextChannelsByName(any(), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getRolesByName(any(), anyBoolean()))
        .thenAnswer(
            i -> {
              if ("the-member-role".equals(i.getArgument(0))) {
                return List.of();
              } else {
                // Just mock any other roles, as we don't care about them for this test
                return List.of(mock(Role.class));
              }
            });

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "some-mods-channel",
                    "the-member-role",
                    "some-unverified-role",
                    Map.of())),
            Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Role not found: the-member-role", thrown.getMessage());
  }

  @Test
  void verifyErrorsIfMultipleMemberRolesFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any channels, as we don't care about them for this test
    when(guild.getTextChannelsByName(any(), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getRolesByName(any(), anyBoolean()))
        .thenAnswer(
            i -> {
              if ("the-member-role".equals(i.getArgument(0))) {
                return List.of(mock(Role.class), mock(Role.class));
              } else {
                // Just mock any other roles, as we don't care about them for this test
                return List.of(mock(Role.class));
              }
            });

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "some-mods-channel",
                    "the-member-role",
                    "some-unverified-role",
                    Map.of())),
            Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Multiple roles found with label: the-member-role", thrown.getMessage());
  }

  @Test
  void validateErrorsIfUnverifiedRoleNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any channels, as we don't care about them for this test
    when(guild.getTextChannelsByName(any(), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getRolesByName(any(), anyBoolean()))
        .thenAnswer(
            i -> {
              if ("the-unverified-role".equals(i.getArgument(0))) {
                return List.of();
              } else {
                // Just mock any other roles, as we don't care about them for this test
                return List.of(mock(Role.class));
              }
            });

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "some-mods-channel",
                    "some-member-role",
                    "the-unverified-role",
                    Map.of())),
            Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Role not found: the-unverified-role", thrown.getMessage());
  }

  @Test
  void validateErrorsIfMultipleUnverifiedRolesFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any channels, as we don't care about them for this test
    when(guild.getTextChannelsByName(any(), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getRolesByName(any(), anyBoolean()))
        .thenAnswer(
            i -> {
              if ("the-unverified-role".equals(i.getArgument(0))) {
                return List.of(mock(Role.class), mock(Role.class));
              } else {
                // Just mock any other roles, as we don't care about them for this test
                return List.of(mock(Role.class));
              }
            });

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "some-mods-channel",
                    "some-member-role",
                    "the-unverified-role",
                    Map.of())),
            Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Multiple roles found with label: the-unverified-role", thrown.getMessage());
  }

  @Test
  void verifyErrorsIfAdditionalRolesNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any channels, as we don't care about them for this test
    when(guild.getTextChannelsByName(any(), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getRolesByName(any(), anyBoolean()))
        .thenAnswer(
            i -> {
              if ("the-role-id".equals(i.getArgument(0))) {
                return List.of();
              } else {
                // Just mock any other roles, as we don't care about them for this test
                return List.of(mock(Role.class));
              }
            });

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "some-mods-channel",
                    "some-member-role",
                    "some-unverified-role",
                    Map.of("some-role-label", "the-role-id"))),
            Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Role not found: the-role-id", thrown.getMessage());
  }

  @Test
  void validateErrorsIfMultipleAdditionalRolesFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any channels, as we don't care about them for this test
    when(guild.getTextChannelsByName(any(), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getRolesByName(any(), anyBoolean()))
        .thenAnswer(
            i -> {
              if ("the-role-id".equals(i.getArgument(0))) {
                return List.of(mock(Role.class), mock(Role.class));
              } else {
                // Just mock any other roles, as we don't care about them for this test
                return List.of(mock(Role.class));
              }
            });

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.of(
                new MembershipConfig(
                    "https://example.com/some-api-root",
                    "some-api-key",
                    "some-mods-channel",
                    "some-member-role",
                    "some-unverified-role",
                    Map.of("some-role-label", "the-role-id"))),
            Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Multiple roles found with label: the-role-id", thrown.getMessage());
  }

  @Test
  void validateErrorsIfMajorAnnouncementsChannelNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    when(guild.getTextChannelById("the-channel-id")).thenReturn(null);

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.empty(),
            Optional.of(
                new ProgrammeConfig(
                    "https://example.com/some-api-root",
                    "the-channel-id",
                    new DayChannelNameResolver(),
                    false)));
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Channel not found: the-channel-id", thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfMajorAnnouncementsChannelFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.empty(),
            Optional.of(
                new ProgrammeConfig(
                    "https://example.com/some-api-root",
                    "the-channel-id",
                    new DayChannelNameResolver(),
                    false)));

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfMultipleMajorAnnouncementsChannelsFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class), mock(TextChannel.class)));

    var config =
        new Config(
            "some-token",
            "the-guild-id",
            "some-database-path",
            Optional.empty(),
            Optional.empty(),
            Optional.of(
                new ProgrammeConfig(
                    "https://example.com/some-api-root",
                    "the-channel-id",
                    new DayChannelNameResolver(),
                    false)));
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Multiple channels found with the name: the-channel-id", thrown.getMessage());
  }
}
