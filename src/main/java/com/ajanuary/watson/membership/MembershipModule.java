package com.ajanuary.watson.membership;

import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.utils.JDAUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MembershipModule implements EventListener  {
  private final Logger logger = LoggerFactory.getLogger(MembershipModule.class);

  private final JDA jda;
  private final JDAUtils jdaUtils;
  private final MembershipConfig membershipConfig;
  private final Config config;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public MembershipModule(JDA jda, MembershipConfig membershipConfig, Config config) {
    this.jda = jda;
    this.jdaUtils = new JDAUtils(jda, config);
    this.membershipConfig = membershipConfig;
    this.config = config;

    jda.addEventListener(this);
  }

  @Override
  public void onEvent(@NotNull GenericEvent event) {
    if (event instanceof ReadyEvent || event instanceof SessionResumeEvent) {
      var guild = jda.getGuildById(config.guildId());
      assert guild != null;

      var unverifiedRole = jdaUtils.getRole(membershipConfig.unverifiedRole());
      var membersRole = jdaUtils.getRole(membershipConfig.memberRole());

      guild.loadMembers().onSuccess(members -> checkMembership(
          members.stream()
              .filter(m -> !m.getUser().isBot())
              .filter(m -> m.getRoles().stream().noneMatch(r -> r.getId().equals(unverifiedRole.getId()) || r.getId().equals(membersRole.getId())))
              .map(Member::getId).toList()));
    } else if (event instanceof GuildMemberJoinEvent guildMemberJoinEvent) {
      var member = guildMemberJoinEvent.getMember();
      checkMembership(List.of(member.getId()));
    }
  }

  private void checkMembership(List<String> discordUserIds) {
    if (discordUserIds.isEmpty()) {
      return;
    }

    logger.info("Checking memberships for users: {}", discordUserIds);

    var postData = "{\"discordUserIds\": " + discordUserIds + "}";
    var uri = URI.create(membershipConfig.membersApiRoot() + "/api/check-discord-ids.php");
    var now = ZonedDateTime.now(java.time.ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));

    var dataToSign = "POST\n" + uri.getPath() + "\n" + now + "\n" + Base64.getEncoder().encodeToString(postData.getBytes());
    var signature = calculateSignature(dataToSign);

    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder(uri)
        .header("accept", "application/json")
        .header("X-Members-RequestTime", now)
        .header("Authorization", "members:1 Watson " + signature)
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(postData))
        .build();

    try {
      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        logger.error("Error checking membership: {}", response.body());
      } else {
        var guild = jda.getGuildById(config.guildId());
        assert guild != null;
        var modsChannel = jdaUtils.getTextChannel(membershipConfig.discordModsChannel());
        assert modsChannel != null;

        var responseData = objectMapper.readTree(response.body());
        responseData.fieldNames().forEachRemaining(userId -> {
          try {
            var userDetails = responseData.get(userId);
            if (userDetails.isNull()) {
              logger.info("Missing user {}. Adding unverified role.", userId);
              var member = guild.retrieveMember(UserSnowflake.fromId(userId)).complete();
              guild.addRoleToMember(member, jdaUtils.getRole(membershipConfig.unverifiedRole())).queue();
              if (!member.getEffectiveName().endsWith("[" + membershipConfig.unverifiedRole() + "]")) {
                guild.modifyNickname(member, member.getEffectiveName() + " [" + membershipConfig.unverifiedRole() + "]").queue();
              }
              modsChannel.sendMessage(
                      "User <@" + userId
                          + "> has joined but we can't find them in our members database. They have been given the Unverified role.")
                  .queue();
            } else {
              var name = userDetails.get("name").asText();
              var roles = new ArrayList<String>();
              userDetails.get("roles").elements().forEachRemaining(role -> {
                var roleId = role.asText();
                roles.add(membershipConfig.additionalRoles().get(roleId));
              });
              roles.add(membershipConfig.memberRole());

              var member = guild.retrieveMember(UserSnowflake.fromId(userId)).complete();
              if (member == null) {
                logger.error("Member {} not found", userId);
              } else {
                logger.info("User {} is a member. Setting nickname to {}.", userId, name);
                try {
                  guild.modifyNickname(member, name).complete();
                } catch (Exception e) {
                  logger.error("Error setting nickname for user {}", userId, e);
                }
              }

              for (var roleName : roles) {
                logger.info("Adding role {} to user {}", roleName, userId);
                guild.addRoleToMember(UserSnowflake.fromId(userId),
                    jdaUtils.getRole(roleName)).queue((v) -> {}, e -> logger.error("Error adding role {} to user {}", roleName, userId, e));
              }
            }
          } catch (Exception e) {
            logger.error("Error processing user {}", userId, e);
          }
        });
      }
    } catch (IOException e) {
      logger.error("Error checking membership", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private String calculateSignature(String dataToSign) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      var secretKey = new SecretKeySpec(membershipConfig.membersApiKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKey);
      var signature = new StringBuilder();
      for (var b : mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8))) {
        signature.append(String.format("%02x", b));
      }
      return signature.toString();
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }
}
