package com.ajanuary.watson.programme;

import com.ajanuary.watson.utils.JDAUtils;

public record ProgrammeConfig(
  String programmeUrl,
  String majorAnnouncementsChannel,
  boolean hasPerformedFirstLoad
) {
  public void validateDiscordConfig(JDAUtils jdaUtils) {
    jdaUtils.getTextChannel(majorAnnouncementsChannel());
  }
}
