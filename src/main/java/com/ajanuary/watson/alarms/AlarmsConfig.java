package com.ajanuary.watson.alarms;

import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public record AlarmsConfig(
    ZoneId timezone,
    Emoji alarmEmoji,
    TemporalAmount timeBeforeToNotify,
    TemporalAmount maxTimeAfterToNotify,
    TemporalAmount minTimeBetweenDMs) {}
