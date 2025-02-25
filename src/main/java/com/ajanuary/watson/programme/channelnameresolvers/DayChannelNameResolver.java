package com.ajanuary.watson.programme.channelnameresolvers;

import com.ajanuary.watson.programme.ProgrammeItem;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DayChannelNameResolver implements ChannelNameResolver {

  private final Map<String, String> dayMappings;
  private final ZoneId timezone;

  public DayChannelNameResolver(Map<String, String> dayMappings, ZoneId timezone) {
    this.dayMappings = dayMappings;
    this.timezone = timezone;
  }

  @Override
  public Optional<String> resolveChannelName(ProgrammeItem item) {
    var dayEnglish = item.startTime(timezone).format(DateTimeFormatter.ofPattern("EEEE"));
    var day = dayMappings.get(dayEnglish);
    return Optional.ofNullable(day);
  }

  @Override
  public Set<String> getChannelNames() {
    return new HashSet<>(dayMappings.values());
  }

  @Override
  public boolean nameIncludesDay() {
    return true;
  }
}
