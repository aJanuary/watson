package com.ajanuary.watson.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ajanuary.watson.api.ApiConfig;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.junit.jupiter.api.Test;
import support.TestConfigBuilder;

public class ConfigTest {

  @Test
  void validateErrorsIfGuildNotFound() {
    var jda = mock(JDA.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(null);

    var config = new TestConfigBuilder().withGuildId("the-guild-id").build();
    var thrown = assertThrows(IllegalStateException.class, () -> config.validateDiscordConfig(jda));

    assertEquals(
        "Guild not found: the-guild-id. Is the ID correct, and has the bot been invited to the server?",
        thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfGuildFound() {
    var jda = mock(JDA.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(mock(Guild.class));
    var config = new TestConfigBuilder().withGuildId("the-guild-id").build();

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfCommsChannelNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-mods-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withApiConfig(apiConfig -> apiConfig.withChannel("the-channel-id"))
            .build();
    new Config(
        "some-token",
        "the-guild-id",
        "some-database-path",
        "some-portal-api-key",
        ZoneId.of("UTC"),
        Optional.empty(),
        Optional.of(new ApiConfig("the-channel-id")),
        Optional.empty(),
        Optional.empty());
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Channel not found: the-channel-id", thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfApiChannelFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withApiConfig(apiConfig -> apiConfig.withChannel("the-channel-id"))
            .build();

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfMultipleCommsChannelsFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class), mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withApiConfig(apiConfig -> apiConfig.withChannel("the-channel-id"))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Multiple channels found with the name: the-channel-id", thrown.getMessage());
  }

  @Test
  void validateErrorsIfHelpDeskChannelNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    when(guild.getTextChannelsByName(eq("the-help-desk-channel-id"), anyBoolean()))
        .thenReturn(List.of());
    when(guild.getTextChannelsByName(eq("the-mods-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig
                        .withHelpDeskChannel("the-help-desk-channel-id")
                        .withDiscordModsChannel("the-mods-channel-id"))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Channel not found: the-help-desk-channel-id", thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfHelpDeskChannelFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-help-desk-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getTextChannelsByName(eq("the-mods-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig
                        .withHelpDeskChannel("the-help-desk-channel-id")
                        .withDiscordModsChannel("the-mods-channel-id"))
            .build();

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfMultipleHelpDeskChannelsFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-help-desk-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class), mock(TextChannel.class)));
    when(guild.getTextChannelsByName(eq("the-mods-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig
                        .withHelpDeskChannel("the-help-desk-channel-id")
                        .withDiscordModsChannel("the-mods-channel-id"))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals(
        "Multiple channels found with the name: the-help-desk-channel-id", thrown.getMessage());
  }

  @Test
  void validateErrorsIfDiscordModsChannelNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    when(guild.getTextChannelsByName(eq("the-help-desk-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getTextChannelsByName(eq("the-mods-channel-id"), anyBoolean()))
        .thenReturn(List.of());

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig
                        .withHelpDeskChannel("the-help-desk-channel-id")
                        .withDiscordModsChannel("the-mods-channel-id"))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Channel not found: the-mods-channel-id", thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfDiscordModsChannelFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-help-desk-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getTextChannelsByName(eq("the-mods-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig
                        .withHelpDeskChannel("the-help-desk-channel-id")
                        .withDiscordModsChannel("the-mods-channel-id"))
            .build();

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfMultipleDiscordModsChannelsFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-help-desk-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getTextChannelsByName(eq("the-mods-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class), mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig
                        .withHelpDeskChannel("the-help-desk-channel-id")
                        .withDiscordModsChannel("the-mods-channel-id"))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Multiple channels found with the name: the-mods-channel-id", thrown.getMessage());
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
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig
                        .withMemberRole("the-member-role")
                        .withUnverifiedRole("the-unverified-role")
                        .withAdditionalRole("some-role-label", "the-role-id"))
            .build();

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
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> membershipConfig.withMemberRole("the-member-role"))
            .build();
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
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> membershipConfig.withMemberRole("the-member-role"))
            .build();
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
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> membershipConfig.withUnverifiedRole("the-unverified-role"))
            .build();
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
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> membershipConfig.withUnverifiedRole("the-unverified-role"))
            .build();
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
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig.withAdditionalRole("some-role-label", "the-role-id"))
            .build();
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
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig ->
                    membershipConfig.withAdditionalRole("some-role-label", "the-role-id"))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Multiple roles found with label: the-role-id", thrown.getMessage());
  }

  @Test
  void validateErrorsIfMajorAnnouncementsChannelNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    when(guild.getTextChannelById("the-major-announcements-channel-id")).thenReturn(null);
    when(guild.getTextChannelsByName(eq("the-now-next-channel"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withProgrammeConfig(
                programmeConfig ->
                    programmeConfig.withMajorAnnouncementChannel(
                        "the-major-announcements-channel-id"))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Channel not found: the-major-announcements-channel-id", thrown.getMessage());
  }

  @Test
  void validateDoesNotErrorIfAllChannelsFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-major-announcements-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getTextChannelsByName(eq("the-now-next-channel"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withProgrammeConfig(
                programmeConfig ->
                    programmeConfig.withMajorAnnouncementChannel(
                        "the-major-announcements-channel-id"))
            .build();

    config.validateDiscordConfig(jda);
  }

  @Test
  void validateErrorsIfMultipleMajorAnnouncementsChannelsFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-major-announcements-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class), mock(TextChannel.class)));
    when(guild.getTextChannelsByName(eq("the-now-next-channel"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withProgrammeConfig(
                programmeConfig ->
                    programmeConfig.withMajorAnnouncementChannel(
                        "the-major-announcements-channel-id"))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals(
        "Multiple channels found with the name: the-major-announcements-channel-id",
        thrown.getMessage());
  }

  @Test
  void validateErrorsINowNextChannelNotFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    when(guild.getTextChannelsByName(eq("the-major-announcements-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getTextChannelById("the-now-next-channel")).thenReturn(null);

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withProgrammeConfig(
                programmeConfig ->
                    programmeConfig
                        .withMajorAnnouncementChannel("the-major-announcements-channel-id")
                        .withNowOn(nowOn -> nowOn.withChannel("the-now-next-channel")))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals("Channel not found: the-now-next-channel", thrown.getMessage());
  }

  @Test
  void validateErrorsIfMultipleNowNextChannelsFound() {
    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    when(jda.getGuildById("the-guild-id")).thenReturn(guild);
    // Just mock any roles, as we don't care about them for this test
    when(guild.getRolesByName(any(), anyBoolean())).thenReturn(List.of(mock(Role.class)));
    when(guild.getTextChannelsByName(eq("the-major-announcements-channel-id"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class)));
    when(guild.getTextChannelsByName(eq("the-now-next-channel"), anyBoolean()))
        .thenReturn(List.of(mock(TextChannel.class), mock(TextChannel.class)));

    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withProgrammeConfig(
                programmeConfig ->
                    programmeConfig
                        .withMajorAnnouncementChannel("the-major-announcements-channel-id")
                        .withNowOn(nowOn -> nowOn.withChannel("the-now-next-channel")))
            .build();
    var thrown =
        assertThrows(IllegalArgumentException.class, () -> config.validateDiscordConfig(jda));

    assertEquals(
        "Multiple channels found with the name: the-now-next-channel", thrown.getMessage());
  }
}
