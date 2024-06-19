package com.ajanuary.watson.config;

import com.ajanuary.watson.alarms.AlarmsConfig;
import com.ajanuary.watson.api.ApiConfig;
import com.ajanuary.watson.membership.MembershipConfig;
import com.ajanuary.watson.programme.ProgrammeConfig;
import com.ajanuary.watson.utils.JDAUtils;
import java.time.ZoneId;
import java.util.Optional;
import net.dv8tion.jda.api.JDA;

public record Config(
    String discordBotToken,
    String guildId,
    String databasePath,
    String portalApiKey,
    ZoneId timezone,
    Optional<AlarmsConfig> alarms,
    Optional<ApiConfig> api,
    Optional<MembershipConfig> membership,
    Optional<ProgrammeConfig> programme) {

  public void validateDiscordConfig(JDA jda) {
    var guild = jda.getGuildById(guildId());
    if (guild == null) {
      throw new IllegalStateException(
          "Guild not found: "
              + guildId()
              + ". Is the ID correct, and has the bot been invited to the server?");
    }

    var jdaUtils = new JDAUtils(jda, this);
    alarms().ifPresent(alarmsConfig -> alarmsConfig.validateDiscordConfig(jdaUtils));
    api().ifPresent(apiConfig -> apiConfig.validateDiscordConfig(jdaUtils));
    membership().ifPresent(membershipConfig -> membershipConfig.validateDiscordConfig(jdaUtils));
    programme().ifPresent(programmeConfig -> programmeConfig.validateDiscordConfig(jdaUtils));
  }
}
