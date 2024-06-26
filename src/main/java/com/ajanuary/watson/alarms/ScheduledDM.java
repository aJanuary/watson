package com.ajanuary.watson.alarms;

import java.time.ZonedDateTime;
import java.util.Optional;

public record ScheduledDM(
    String discordThreadId,
    String discordMessageId,
    String userId,
    ZonedDateTime messageTime,
    String title,
    String jumpUrl,
    String contents,
    Optional<String> tags) {}
