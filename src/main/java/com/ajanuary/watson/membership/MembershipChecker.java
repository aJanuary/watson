package com.ajanuary.watson.membership;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.utils.JDAUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MembershipChecker {
  private final Logger logger = LoggerFactory.getLogger(MembershipChecker.class);

  private final JDA jda;
  private final JDAUtils jdaUtils;
  private final MembershipConfig membershipConfig;
  private final Config config;
  private final MembersApiClient membersApiClient;

  public MembershipChecker(
      JDA jda,
      MembershipConfig membershipConfig,
      Config config,
      MembersApiClient membersApiClient) {
    this.jda = jda;
    this.jdaUtils = new JDAUtils(jda, config);
    this.membershipConfig = membershipConfig;
    this.config = config;
    this.membersApiClient = membersApiClient;
  }

  public void checkMembership(Collection<String> discordUserIds) {
    if (discordUserIds.isEmpty()) {
      return;
    }

    logger.info("Checking memberships for users: {}", discordUserIds);
    try {
      var results = membersApiClient.getMemberStatus(discordUserIds);

      var guild = jda.getGuildById(config.guildId());
      if (guild == null) {
        logger.error("Could not find guild with ID {}", config.guildId());
        return;
      }

      var modsChannel = jdaUtils.getTextChannel(membershipConfig.discordModsChannel());

      for (var entry : results.entrySet()) {
        try {
          var userId = entry.getKey();
          var membership = entry.getValue();
          membership.ifPresentOrElse(
              memberDetails -> {
                var member = guild.retrieveMemberById(userId).complete();
                if (member == null) {
                  logger.error("Member {} not found", userId);
                  return;
                }
                logger.info(
                    "User {} is a member. Setting nickname to {}.", userId, memberDetails.name());
                try {
                  guild.modifyNickname(member, memberDetails.name()).complete();
                } catch (Exception e) {
                  logger.error("Error setting nickname for user {}", userId, e);
                }

                var roles =
                    new ArrayList<>(
                        memberDetails.roles().stream()
                            .map(role -> membershipConfig.additionalRoles().get(role))
                            .filter(Objects::nonNull)
                            .toList());
                roles.add(membershipConfig.memberRole());
                for (var roleName : roles) {
                  logger.info("Adding role {} to user {}", roleName, userId);
                  guild
                      .addRoleToMember(member, jdaUtils.getRole(roleName))
                      .queue(
                          (v) -> {},
                          e ->
                              logger.error("Error adding role {} to user {}", roleName, userId, e));
                }
              },
              () -> {
                logger.info("Missing user {}. Adding unverified role.", userId);
                var member = guild.retrieveMemberById(userId).complete();
                guild
                    .addRoleToMember(member, jdaUtils.getRole(membershipConfig.unverifiedRole()))
                    .queue();
                if (!member
                    .getEffectiveName()
                    .endsWith("[" + membershipConfig.unverifiedRole() + "]")) {
                  guild
                      .modifyNickname(
                          member,
                          member.getEffectiveName()
                              + " ["
                              + membershipConfig.unverifiedRole()
                              + "]")
                      .queue();
                }
                modsChannel
                    .sendMessage(
                        "User <@"
                            + userId
                            + "> has joined but we can't find them in our members database. They have been given the Unverified role.")
                    .queue();
              });
        } catch (Exception e) {
          logger.error("Error processing user {}", entry.getKey(), e);
        }
      }
    } catch (Exception e) {
      logger.error("Error checking memberships", e);
    }
  }

  public boolean shouldCheckMember(Member member) {
    if (member.getUser().isBot()) {
      // Bots are never members that need checking
      return false;
    }

    return member.getRoles().stream()
        .noneMatch(
            role ->
                role.getName().equalsIgnoreCase(membershipConfig.unverifiedRole())
                    || role.getName().equalsIgnoreCase(membershipConfig.memberRole()));
  }
}
