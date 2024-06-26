package com.ajanuary.watson.programme;

import java.time.ZonedDateTime;

public record DiscordItem(
    String id,
    String title,
    String body,
    String loc,
    String time,
    ZonedDateTime startTime,
    ZonedDateTime endTime) {}
