package com.ajanuary.watson.alarms;

import com.ajanuary.watson.utils.JDAUtils;
import java.time.temporal.TemporalAmount;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public record AlarmsConfig(
    Emoji alarmEmoji,
    String alarmsChannel,
    TemporalAmount timeBeforeToNotify,
    TemporalAmount maxTimeAfterToNotify,
    TemporalAmount minTimeBetweenDMs) {

  public void validateDiscordConfig(JDAUtils jdaUtils) {
    jdaUtils.getTextChannel(alarmsChannel());
  }
}
