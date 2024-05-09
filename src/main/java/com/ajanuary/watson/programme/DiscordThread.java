package com.ajanuary.watson.programme;

public record DiscordThread(
    String discordThreadId, String discordMessageId, Status status, DiscordItem item) {}
