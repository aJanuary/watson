package com.ajanuary.watson.membership;

import com.ajanuary.watson.utils.JDAUtils;
import java.util.Map;

public record MembershipConfig(
    String membersApiUrl,
    String membersApiKey,
    String discordModsChannel,
    String memberRole,
    String unverifiedRole,
    Map<String, String> additionalRoles) {

  public void validateDiscordConfig(JDAUtils jdaUtils) {
    jdaUtils.getTextChannel(discordModsChannel());
    jdaUtils.getRole(memberRole());
    jdaUtils.getRole(unverifiedRole());

    for (var entry : additionalRoles.entrySet()) {
      jdaUtils.getRole(entry.getValue());
    }
  }
}
