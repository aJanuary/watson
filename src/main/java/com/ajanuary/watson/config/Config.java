package com.ajanuary.watson.config;

import com.ajanuary.watson.alarms.AlarmsConfig;
import com.ajanuary.watson.membership.MembershipConfig;
import com.ajanuary.watson.programme.ProgrammeConfig;
import com.ajanuary.watson.utils.JDAUtils;
import net.dv8tion.jda.api.JDA;

public record Config(
    String discordBotToken,
    String guildId,
    String databasePath,
    AlarmsConfig alarms,
    MembershipConfig membership,
    ProgrammeConfig programme) {

  public void validateDiscordConfig(JDA jda) {
    var guild = jda.getGuildById(guildId());
    if (guild == null) {
      throw new IllegalStateException("Guild not found");
    }

    var jdaUtils = new JDAUtils(jda, this);
    membership().validateDiscordConfig(jdaUtils);
    programme().validateDiscordConfig(jdaUtils);
  }
}
