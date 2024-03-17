package com.ajanuary.watson;

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
import java.util.Base64;
import java.util.List;
import java.util.Objects;
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
  private final Config config;
  private final Secrets secrets;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public MembershipModule(JDA jda, Config config, Secrets secrets) {
    this.jda = jda;
    this.config = config;
    this.secrets = secrets;

    jda.addEventListener(this);
  }

  @Override
  public void onEvent(@NotNull GenericEvent event) {
    if (event instanceof ReadyEvent || event instanceof SessionResumeEvent) {
      var guild = jda.getGuildById(config.guildId());
      assert guild != null;
      guild.loadMembers().onSuccess(members -> checkMembership(
          members.stream()
              .filter(m -> !m.getUser().isBot())
              .filter(m -> m.getRoles().stream().noneMatch(r -> r.getId().equals(config.unverifiedRoleId())))
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
    var uri = URI.create(config.membersApiRoot() + "/api/check-discord-ids.php");
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
        var responseData = objectMapper.readTree(response.body());
        responseData.get("missing").forEach(discordUserId -> {
          var userId = discordUserId.asText();
          logger.info("Missing user {}. Adding unverified role.", userId);

          var guild = jda.getGuildById(config.guildId());
          assert guild != null;
          guild.addRoleToMember(UserSnowflake.fromId(userId),
              Objects.requireNonNull(guild.getRoleById(config.unverifiedRoleId()))).queue();

          guild.getTextChannelById(config.discordModsChannelId()).sendMessage(
              "User <@" + userId + "> has joined but we can't find them in our members database. They have been given the Unverified role.").queue();
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
      var secretKey = new SecretKeySpec(secrets.membersApiKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
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
