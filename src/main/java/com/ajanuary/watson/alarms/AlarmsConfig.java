package com.ajanuary.watson.alarms;

import java.time.ZoneId;
import java.time.temporal.TemporalAmount;

public record AlarmsConfig(
    ZoneId timezone,
    String alarmEmoji,
    TemporalAmount timeBeforeToNotify,
    TemporalAmount maxTimeAfterToNotify,
    TemporalAmount minTimeBetweenDMs
) {
}
