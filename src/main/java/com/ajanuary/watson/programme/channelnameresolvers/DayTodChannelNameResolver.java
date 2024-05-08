package com.ajanuary.watson.programme.channelnameresolvers;

import com.ajanuary.watson.programme.ProgrammeItem;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DayTodChannelNameResolver implements ChannelNameResolver {
  private final List<Threshold> thresholds;

  public DayTodChannelNameResolver(List<Threshold> thresholds) {
    this.thresholds = thresholds;
  }

  @Override
  public String resolveChannelName(ProgrammeItem item) {
    var day = item.date().format(DateTimeFormatter.ofPattern("EEEE"));
    return thresholds.stream()
        .filter(threshold -> item.time().compareTo(threshold.start()) >= 0 && item.time().compareTo(threshold.end()) < 0)
        .findFirst().map(threshold -> day + "-" + threshold.name()).orElse(day);
  }

  @Override
  public Set<String> getPossibleNames() {
    return Set.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday").stream().flatMap(day ->
        thresholds.stream().map(threshold -> day + "-" + threshold.name())).collect(Collectors.toSet());
  }

  public record Threshold(String name, String start, String end) {
  }
}
