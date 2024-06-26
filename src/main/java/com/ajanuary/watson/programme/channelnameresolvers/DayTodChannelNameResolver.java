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

  public List<Threshold> thresholds() {
    return thresholds;
  }

  @Override
  public String resolveChannelName(ProgrammeItem item) {
    var day = item.startTime().format(DateTimeFormatter.ofPattern("EEEE"));
    return thresholds.stream()
        .filter(
            threshold -> {
              var time = item.startTime().format(DateTimeFormatter.ofPattern("HH:mm"));
              return time.compareTo(threshold.start()) >= 0 && time.compareTo(threshold.end()) < 0;
            })
        .findFirst()
        .map(threshold -> day + "-" + threshold.label())
        .orElse(day);
  }

  @Override
  public Set<String> getPossibleNames() {
    return Set.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        .stream()
        .flatMap(day -> thresholds.stream().map(threshold -> day + "-" + threshold.label()))
        .collect(Collectors.toSet());
  }

  public record Threshold(String label, String start, String end) {}
}
