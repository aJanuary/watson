package com.ajanuary.watson.membership;

import com.ajanuary.watson.utils.JDAUtils;
import java.net.URI;
import java.util.Map;

public record MembershipConfig(
    URI membersApiUrl,
    String helpDeskChannel,
    String discordModsChannel,
    String memberRole,
    String unverifiedRole,
    Map<String, String> additionalRoles,
    String memberHelpRole) {

  public void validateDiscordConfig(JDAUtils jdaUtils) {
    jdaUtils.getTextChannel(helpDeskChannel());
    jdaUtils.getTextChannel(discordModsChannel());
    jdaUtils.getRole(memberRole());
    jdaUtils.getRole(unverifiedRole());
    jdaUtils.getRole(memberHelpRole());

    for (var entry : additionalRoles.entrySet()) {
      jdaUtils.getRole(entry.getValue());
    }
  }
}
