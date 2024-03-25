package com.ajanuary.watson.config;

public record Config(
    String guildId,
    String membersApiRoot,
    String programmeUrl,
    String programmeStoragePath,
    Roles roles,
    Channels channels,
    boolean hasPerformedFirstLoad
) {
}
