package com.ajanuary.watson.membership;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.db.DatabaseManager;
import com.ajanuary.watson.privatethreads.PrivateThreadManager;
import com.ajanuary.watson.utils.JDAUtils;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MembershipChecker {
  private final Logger logger = LoggerFactory.getLogger(MembershipChecker.class);

  private final JDA jda;
  private final JDAUtils jdaUtils;
  private final MembershipConfig membershipConfig;
  private final Config config;
  private final MembersApiClient membersApiClient;
  private final DatabaseManager databaseManager;
  private final PrivateThreadManager privateThreadManager;

  public MembershipChecker(
      JDA jda,
      MembershipConfig membershipConfig,
      Config config,
      MembersApiClient membersApiClient,
      DatabaseManager databaseManager) {
    this.jda = jda;
    this.jdaUtils = new JDAUtils(jda, config);
    this.membershipConfig = membershipConfig;
    this.config = config;
    this.membersApiClient = membersApiClient;
    this.databaseManager = databaseManager;
    this.privateThreadManager = new PrivateThreadManager(jda, "discord-validation-links");
  }

  public void checkMembership(Collection<DiscordUser> discordUsers) {
    if (discordUsers.isEmpty()) {
      return;
    }

    logger.info("Checking memberships for users: {}", discordUsers);
    try {
      var results = membersApiClient.getMemberStatus(discordUsers);

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
          membership.map(
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

                guild
                    .removeRoleFromMember(
                        member, jdaUtils.getRole(membershipConfig.unverifiedRole()))
                    .queue();

                try (var conn = databaseManager.getConnection()) {
                  privateThreadManager
                      .getThread(conn, userId)
                      .ifPresent(
                          thread -> {
                            thread
                                .sendMessage(
                                    "Thank you, we've now associated your membership with this Discord account.")
                                .onSuccess(v -> thread.getManager().setArchived(true).queue())
                                .queue();
                          });
                } catch (SQLException e) {
                  logger.error("Error checking private thread for user {}", userId, e);
                }
              },
              verificationUrl -> {
                logger.info("Missing user {}. Adding unverified role.", userId);
                var member = guild.retrieveMemberById(userId).complete();
                guild
                    .addRoleToMember(member, jdaUtils.getRole(membershipConfig.unverifiedRole()))
                    .queue(
                        (v) -> {},
                        e ->
                            logger.error(
                                "Error adding role {} to user {}",
                                membershipConfig.unverifiedRole(),
                                userId,
                                e));
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

                try (var conn = databaseManager.getConnection()) {
                  var createThreadResult =
                      privateThreadManager.createThread(
                          conn,
                          userId,
                          () -> {
                            var helpDeskChannel =
                                jdaUtils.getTextChannel(membershipConfig.helpDeskChannel());
                            return helpDeskChannel
                                .createThreadChannel("verification", true)
                                .complete();
                          });

                  if (createThreadResult.created()) {
                    var memberHelpRole = jdaUtils.getRole(membershipConfig.memberHelpRole());
                    createThreadResult
                        .thread()
                        .sendMessage(
                            new MessageCreateBuilder()
                                .addContent(
                                    "Hello <@"
                                        + userId
                                        + ">!\n\nTo help us keep our members safe, we need you to click the link below to associate your Discord account with your convention membership.\n[Verify your Discord account](<"
                                        + verificationUrl
                                        + ">)\n\n")
                                .addContent(
                                    "If you have any problems, please reply here and a <@&"
                                        + memberHelpRole.getId()
                                        + "> will be happy to help.")
                                .build())
                        .queue();
                  } else {
                    createThreadResult
                        .thread()
                        .sendMessage(
                            new MessageCreateBuilder()
                                .addContent(
                                    "Sorry, we're still having trouble verifying you. Please wait while a member of the help desk team assists you.")
                                .build())
                        .queue();
                  }
                } catch (SQLException e) {
                  logger.error("Error creating private thread for user {}", userId, e);
                }
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

  public record DiscordUser(String userId, String username) {}
}
