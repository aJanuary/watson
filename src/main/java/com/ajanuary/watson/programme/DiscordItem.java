package com.ajanuary.watson.programme;

import java.time.ZonedDateTime;

public record DiscordItem(
    String id,
    String title,
    String body,
    String loc,
    ZonedDateTime startTime,
    ZonedDateTime endTime) {}
