package com.ajanuary.watson.programme.channelnameresolvers;

import com.ajanuary.watson.programme.ProgrammeItem;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DayTodChannelNameResolver implements ChannelNameResolver {

  private final Map<String, String> dayMappings;
  private final List<Threshold> thresholds;
  private final ZoneId timezone;

  public DayTodChannelNameResolver(Map<String, String> dayMappings, List<Threshold> thresholds, ZoneId timezone) {
    this.dayMappings = dayMappings;
    this.thresholds = thresholds;
    this.timezone = timezone;
  }

  public List<Threshold> thresholds() {
    return thresholds;
  }

  @Override
  public Optional<String> resolveChannelName(ProgrammeItem item) {
    var dayEnglish = item.startTime(timezone).format(DateTimeFormatter.ofPattern("EEEE"));
    var day = dayMappings.get(dayEnglish);
    if (day == null) {
      return Optional.empty();
    }

    return Optional.of(
        thresholds.stream()
            .filter(
                threshold -> {
                  var time = item.startTime(timezone).format(DateTimeFormatter.ofPattern("HH:mm"));
                  return time.compareTo(threshold.start()) >= 0
                      && time.compareTo(threshold.end()) < 0;
                })
            .findFirst()
            .map(threshold -> day + "-" + threshold.label())
            .orElse(day));
  }

  @Override
  public Set<String> getChannelNames() {
    return dayMappings.values()
        .stream()
        .flatMap(day -> thresholds.stream().map(threshold -> day + "-" + threshold.label()))
        .collect(Collectors.toSet());
  }

  @Override
  public boolean nameIncludesDay() {
    return true;
  }

  public record Threshold(String label, String start, String end) {}
}
