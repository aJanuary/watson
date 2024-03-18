package com.ajanuary.watson.config;

public record Roles(
    String member,
    String unverified,
    String onSite,
    String virtual,
    String programmeParticipant,
    String programmeModerator,
    String artist,
    String dealer
) {
}
