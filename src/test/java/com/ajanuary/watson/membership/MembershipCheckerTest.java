package com.ajanuary.watson.membership;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ajanuary.watson.db.DatabaseManager;
import com.ajanuary.watson.membership.MembersApiClient.MemberDetails;
import com.ajanuary.watson.membership.MembersApiClient.MembershipStatus;
import com.ajanuary.watson.membership.MembershipChecker.DiscordUser;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import org.junit.jupiter.api.Test;
import support.MonitorableRestAction;
import support.TestConfigBuilder;

public class MembershipCheckerTest {
  @Test
  void doesntCheckBots() {
    var config = new TestConfigBuilder().withMembershipConfig(membershipConfig -> {}).build();
    var member = mock(Member.class);
    var user = mock(User.class);
    when(member.getUser()).thenReturn(user);
    when(user.isBot()).thenReturn(true);

    var membershipChecker =
        new MembershipChecker(
            mock(JDA.class),
            config.membership().get(),
            config,
            mock(MembersApiClient.class),
            mock(DatabaseManager.class));

    assertFalse(membershipChecker.shouldCheckMember(member), "member should not be checked");
  }

  @Test
  void doesntCheckUsersWithUnverifiedRole() {
    var config =
        new TestConfigBuilder()
            .withMembershipConfig(
                membershipConfig -> membershipConfig.withUnverifiedRole("the-unverified-role"))
            .build();

    var unverifiedRole = mock(Role.class);
    when(unverifiedRole.getName()).thenReturn("the-unverified-role");
    var member = mock(Member.class);
    var user = mock(User.class);
    when(member.getUser()).thenReturn(user);
    when(member.getRoles()).thenReturn(List.of(unverifiedRole));
    when(user.isBot()).thenReturn(false);

    var membershipChecker =
        new MembershipChecker(
            mock(JDA.class),
            config.membership().get(),
            config,
            mock(MembersApiClient.class),
            mock(DatabaseManager.class));

    assertFalse(membershipChecker.shouldCheckMember(member), "member should not be checked");
  }

  @Test
  void doesntCheckUsersWithMemberRole() {
    var config =
        new TestConfigBuilder()
            .withMembershipConfig(
                membershipConfig -> membershipConfig.withMemberRole("the-member-role"))
            .build();

    var memberRole = mock(Role.class);
    when(memberRole.getName()).thenReturn("the-member-role");
    var member = mock(Member.class);
    var user = mock(User.class);
    when(member.getUser()).thenReturn(user);
    when(member.getRoles()).thenReturn(List.of(memberRole));
    when(user.isBot()).thenReturn(false);

    var membershipChecker =
        new MembershipChecker(
            mock(JDA.class),
            config.membership().get(),
            config,
            mock(MembersApiClient.class),
            mock(DatabaseManager.class));

    assertFalse(membershipChecker.shouldCheckMember(member), "member should not be checked");
  }

  @Test
  void doesCheckMembersWithNoRoles() {
    var config = new TestConfigBuilder().withMembershipConfig(membershipConfig -> {}).build();

    var member = mock(Member.class);
    var user = mock(User.class);
    when(member.getUser()).thenReturn(user);
    when(member.getRoles()).thenReturn(List.of());
    when(user.isBot()).thenReturn(false);

    var membershipChecker =
        new MembershipChecker(
            mock(JDA.class),
            config.membership().get(),
            config,
            mock(MembersApiClient.class),
            mock(DatabaseManager.class));

    assertTrue(membershipChecker.shouldCheckMember(member), "member should be checked");
  }

  @Test
  void doesCheckMembersWithUnrelatedRoles() {
    var config = new TestConfigBuilder().withMembershipConfig(membershipConfig -> {}).build();

    var unrelatedRole = mock(Role.class);
    when(unrelatedRole.getName()).thenReturn("some-unrelated-role");
    var member = mock(Member.class);
    var user = mock(User.class);
    when(member.getUser()).thenReturn(user);
    when(member.getRoles()).thenReturn(List.of(unrelatedRole));
    when(user.isBot()).thenReturn(false);

    var membershipChecker =
        new MembershipChecker(
            mock(JDA.class),
            config.membership().get(),
            config,
            mock(MembersApiClient.class),
            mock(DatabaseManager.class));

    assertTrue(membershipChecker.shouldCheckMember(member), "member should be checked");
  }

  @Test
  void addsUnverifiedRoleToUnverifiedUser() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withUnverifiedRole("the-unverified-role")
                      .withDiscordModsChannel("the-mods-channel");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var unverifiedRole = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.modifyNickname(any(), any())).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-unverified-role", true)).thenReturn(List.of(unverifiedRole));
    var addRoleToMemberAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.addRoleToMember(member, unverifiedRole)).thenReturn(addRoleToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(Map.of("the-member", MembershipStatus.verification("some-verification-url")));

    when(member.getEffectiveName()).thenReturn("the-member-name");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertTrue(
        addRoleToMemberAction.wasInvoked(), "addRoleToMember action was scheduled for execution");
  }

  @Test
  void appendsUnverifiedLabelToUnverifiedUser() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withUnverifiedRole("the-unverified-role")
                      .withDiscordModsChannel("the-mods-channel");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var unverifiedRole = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.modifyNickname(member, "the-member-name [the-unverified-role]"))
        .thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-unverified-role", true)).thenReturn(List.of(unverifiedRole));
    var addRoleToMemberAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.addRoleToMember(member, unverifiedRole)).thenReturn(addRoleToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(Map.of("the-member", MembershipStatus.verification("some-verification-url")));

    when(member.getEffectiveName()).thenReturn("the-member-name");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertTrue(
        modifyNicknameAction.wasInvoked(), "modifyNickname action was scheduled for execution");
  }

  @Test
  void doesNotAppendUnverifiedLabelIfAlreadyExists() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withUnverifiedRole("the-unverified-role")
                      .withDiscordModsChannel("the-mods-channel");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var unverifiedRole = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.modifyNickname(any(), any())).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-unverified-role", true)).thenReturn(List.of(unverifiedRole));
    var addRoleToMemberAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.addRoleToMember(member, unverifiedRole)).thenReturn(addRoleToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(Map.of("the-member", MembershipStatus.verification("some-verification-url")));

    when(member.getEffectiveName()).thenReturn("the-member-name [the-unverified-role]");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertFalse(
        modifyNicknameAction.wasInvoked(),
        "modifyNickname action was never scheduled for execution");
  }

  @Test
  void notifiesModsOfUnverifiedUser() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withUnverifiedRole("the-unverified-role")
                      .withDiscordModsChannel("the-mods-channel");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var unverifiedRole = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.modifyNickname(any(), any())).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-unverified-role", true)).thenReturn(List.of(unverifiedRole));
    var addRoleToMemberAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.addRoleToMember(member, unverifiedRole)).thenReturn(addRoleToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(Map.of("the-member", MembershipStatus.verification("some-verification-url")));

    when(member.getEffectiveName()).thenReturn("the-member-name [the-unverified-role]");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class);
    when(modsChannel.sendMessage(argThat((String message) -> message.contains("<@the-member>"))))
        .thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertTrue(sendMessageAction.wasInvoked(), "sendMessage action was scheduled for execution");
  }

  @Test
  void addsMemberRoleToVerifiedUser() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withMemberRole("the-member-role")
                      .withDiscordModsChannel("the-mods-channel");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var memberRole = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.modifyNickname(any(), any())).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-member-role", true)).thenReturn(List.of(memberRole));
    var addRoleToMemberAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.addRoleToMember(member, memberRole)).thenReturn(addRoleToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(
            Map.of(
                "the-member",
                MembershipStatus.member(new MemberDetails("some-username", List.of()))));

    when(member.getEffectiveName()).thenReturn("the-member-name");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertTrue(
        addRoleToMemberAction.wasInvoked(), "addRoleToMember action was scheduled for execution");
  }

  @Test
  void setsNicknameForVerifiedUser() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withMemberRole("the-member-role")
                      .withDiscordModsChannel("the-mods-channel");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var memberRole = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class, null);
    when(guild.modifyNickname(member, "the-badge-name")).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-member-role", true)).thenReturn(List.of(memberRole));
    var addRoleToMemberAction = MonitorableRestAction.create(AuditableRestAction.class, null);
    when(guild.addRoleToMember(member, memberRole)).thenReturn(addRoleToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(
            Map.of(
                "the-member",
                MembershipStatus.member(new MemberDetails("the-badge-name", List.of()))));

    when(member.getEffectiveName()).thenReturn("the-member-name");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class, null);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertTrue(
        modifyNicknameAction.wasInvoked(), "modifyNickname action was scheduled for execution");
  }

  @Test
  void addsMappedRolesToVerifiedUser() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withMemberRole("the-member-role")
                      .withDiscordModsChannel("the-mods-channel")
                      .withAdditionalRole("role-1-name", "role-1-id")
                      .withAdditionalRole("role-2-name", "role-2-id");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var memberRole = mock(Role.class);
    var role1 = mock(Role.class);
    var role2 = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class, null);
    when(guild.modifyNickname(any(), any())).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-member-role", true)).thenReturn(List.of(memberRole));
    when(guild.getRolesByName("role-1-id", true)).thenReturn(List.of(role1));
    when(guild.getRolesByName("role-2-id", true)).thenReturn(List.of(role2));
    var addRole1ToMemberAction = MonitorableRestAction.create(AuditableRestAction.class, null);
    when(guild.addRoleToMember(member, role1)).thenReturn(addRole1ToMemberAction.action());
    var addRole2ToMemberAction = MonitorableRestAction.create(AuditableRestAction.class, null);
    when(guild.addRoleToMember(member, role2)).thenReturn(addRole2ToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(
            Map.of(
                "the-member",
                MembershipStatus.member(
                    new MemberDetails(
                        "some-username", List.of("role-1-name", "role-2-name", "role-3-name")))));

    when(member.getEffectiveName()).thenReturn("the-member-name");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class, null);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertTrue(
        addRole1ToMemberAction.wasInvoked(),
        "addRoleToMember action for role 1 was scheduled for execution");
    assertTrue(
        addRole2ToMemberAction.wasInvoked(),
        "addRoleToMember action for role 2 was scheduled for execution");
  }

  @Test
  void addsRolesEvenIfSettingNicknameErrors() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withMemberRole("the-member-role")
                      .withDiscordModsChannel("the-mods-channel");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var memberRole = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction =
        MonitorableRestAction.createWithException(
            AuditableRestAction.class, new RuntimeException("some exception"));
    when(guild.modifyNickname(any(), any())).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-member-role", true)).thenReturn(List.of(memberRole));
    var addRoleToMemberAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.addRoleToMember(member, memberRole)).thenReturn(addRoleToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(
            Map.of(
                "the-member",
                MembershipStatus.member(new MemberDetails("some-username", List.of()))));

    when(member.getEffectiveName()).thenReturn("the-member-name");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertTrue(
        addRoleToMemberAction.wasInvoked(), "addRoleToMember action was scheduled for execution");
  }

  @Test
  void addsOtherRolesEvenIfAddingOneFails() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withMemberRole("the-member-role")
                      .withDiscordModsChannel("the-mods-channel")
                      .withAdditionalRole("role-1-name", "role-1-id")
                      .withAdditionalRole("role-2-name", "role-2-id");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var memberRole = mock(Role.class);
    var role1 = mock(Role.class);
    var role2 = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member")).thenReturn(retrieveMemberByIdAction.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.modifyNickname(any(), any())).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-member-role", true)).thenReturn(List.of(memberRole));
    when(guild.getRolesByName("role-1-id", true)).thenReturn(List.of(role1));
    when(guild.getRolesByName("role-2-id", true)).thenReturn(List.of(role2));
    var addRole1ToMemberAction =
        MonitorableRestAction.createWithException(
            AuditableRestAction.class, new RuntimeException("Some exception"));
    when(guild.addRoleToMember(member, role1)).thenReturn(addRole1ToMemberAction.action());
    var addRole2ToMemberAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.addRoleToMember(member, role2)).thenReturn(addRole2ToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(List.of(new DiscordUser("the-id", "the-member"))))
        .thenReturn(
            Map.of(
                "the-member",
                MembershipStatus.member(
                    new MemberDetails(
                        "some-username", List.of("role-1-name", "role-2-name", "role-3-name")))));

    when(member.getEffectiveName()).thenReturn("the-member-name");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(List.of(new DiscordUser("the-id", "the-member")));

    assertTrue(
        addRole2ToMemberAction.wasInvoked(),
        "addRoleToMember action for role 2 was scheduled for execution");
  }

  @Test
  void processesOtherUsersEvenIfOneErrors() throws IOException, InterruptedException {
    var config =
        new TestConfigBuilder()
            .withGuildId("the-guild-id")
            .withMembershipConfig(
                membershipConfig -> {
                  membershipConfig
                      .withMemberRole("the-member-role")
                      .withDiscordModsChannel("the-mods-channel");
                })
            .build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var apiClient = mock(MembersApiClient.class);
    var member = mock(Member.class);
    var memberRole = mock(Role.class);
    var modsChannel = mock(TextChannel.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    var retrieveMemberByIdAction1 =
        MonitorableRestAction.createWithException(
            CacheRestAction.class, new RuntimeException("some-exception"));
    when(guild.retrieveMemberById("the-member-1")).thenReturn(retrieveMemberByIdAction1.action());
    var retrieveMemberByIdAction2 = MonitorableRestAction.create(CacheRestAction.class, member);
    when(guild.retrieveMemberById("the-member-2")).thenReturn(retrieveMemberByIdAction2.action());
    var modifyNicknameAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.modifyNickname(any(), any())).thenReturn(modifyNicknameAction.action());
    when(guild.getRolesByName("the-member-role", true)).thenReturn(List.of(memberRole));
    var addRoleToMemberAction = MonitorableRestAction.create(AuditableRestAction.class);
    when(guild.addRoleToMember(member, memberRole)).thenReturn(addRoleToMemberAction.action());
    when(guild.getTextChannelsByName("the-mods-channel", true)).thenReturn(List.of(modsChannel));

    when(apiClient.getMemberStatus(
            List.of(
                new DiscordUser("the-id-1", "the-member-1"),
                new DiscordUser("the-id-2", "the-member-2"))))
        .thenReturn(
            Map.of(
                "the-member-1",
                MembershipStatus.member(new MemberDetails("some-username", List.of())),
                "the-member-2",
                MembershipStatus.member(new MemberDetails("some-username", List.of()))));

    when(member.getEffectiveName()).thenReturn("the-member-name");

    var sendMessageAction = MonitorableRestAction.create(MessageCreateAction.class);
    when(modsChannel.sendMessage(any(String.class))).thenReturn(sendMessageAction.action());

    var membershipChecker =
        new MembershipChecker(
            jda, config.membership().get(), config, apiClient, mock(DatabaseManager.class));
    membershipChecker.checkMembership(
        List.of(
            new DiscordUser("the-id-1", "the-member-1"),
            new DiscordUser("the-id-2", "the-member-2")));

    assertTrue(
        addRoleToMemberAction.wasInvoked(), "addRoleToMember action was scheduled for execution");
  }
}
