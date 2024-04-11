package com.ajanuary.watson.alarms;

import java.time.LocalDateTime;
import java.util.Optional;

public record ScheduledDM(
    String discordThreadId,
    String discordMessageId,
    String userId,
    LocalDateTime messageTime,
    String title,
    String jumpUrl,
    String contents,
    Optional<String> tags
) {
}
