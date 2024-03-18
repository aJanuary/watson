package com.ajanuary.watson.config;

public record Config(
    String guildId,
    String membersApiRoot,
    String discordModsChannelId,
    Roles roles
) {
}
