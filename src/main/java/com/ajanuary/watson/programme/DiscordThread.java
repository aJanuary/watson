package com.ajanuary.watson.programme;

import java.util.Optional;

public record DiscordThread(
    Optional<String> discordThreadId,
    Optional<String> discordMessageId,
    Status status,
    DiscordItem item) {}
