package com.ajanuary.watson;

public record Config(
    String guildId,
    String membersApiRoot,
    String unverifiedRoleId,
    String discordModsChannelId
) {
}
