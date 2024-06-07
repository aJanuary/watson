package com.ajanuary.watson.programme;

import java.time.LocalDateTime;

public record DiscordItem(
    String id,
    String title,
    String body,
    String loc,
    String time,
    LocalDateTime startTime,
    LocalDateTime endTime) {}
