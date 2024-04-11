package com.ajanuary.watson.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;

public record Config(
    String guildId,
    String membersApiRoot,
    String programmeUrl,
    String programmeStoragePath,
    String alarmEmoji,
    @JsonDeserialize(converter = ZoneIdConverter.class)
    ZoneId timezone,
    @JsonDeserialize(converter = TemporalAmountConverter.class)
    TemporalAmount timeBeforeToNotify,
    @JsonDeserialize(converter = TemporalAmountConverter.class)
    TemporalAmount minTimeBetweenDMs,
    @JsonDeserialize(converter = TemporalAmountConverter.class)
    TemporalAmount maxTimeAfterToNotify,
    Roles roles,
    Channels channels,
    boolean hasPerformedFirstLoad
) {
  private static class ZoneIdConverter extends StdConverter<String, ZoneId> {
    @Override
    public ZoneId convert(String s) {
      return ZoneId.of(s);
    }
  }

  private static class TemporalAmountConverter extends StdConverter<String, TemporalAmount> {
    @Override
    public TemporalAmount convert(String s) {
      return Duration.parse("PT" + s);
    }
  }

}
