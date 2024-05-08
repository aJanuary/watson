package com.ajanuary.watson.programme;

import com.ajanuary.watson.programme.channelnameresolvers.ChannelNameResolver;
import com.ajanuary.watson.utils.JDAUtils;

public record ProgrammeConfig(
  String programmeUrl,
  String majorAnnouncementsChannel,
  ChannelNameResolver channelNameResolver,
  boolean hasPerformedFirstLoad
) {
  public void validateDiscordConfig(JDAUtils jdaUtils) {
    jdaUtils.getTextChannel(majorAnnouncementsChannel());
  }
}
