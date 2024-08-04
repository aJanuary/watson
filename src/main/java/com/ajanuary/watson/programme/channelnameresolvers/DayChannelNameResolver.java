package com.ajanuary.watson.programme.channelnameresolvers;

import com.ajanuary.watson.programme.ProgrammeItem;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Set;

public class DayChannelNameResolver implements ChannelNameResolver {

  @Override
  public Optional<String> resolveChannelName(ProgrammeItem item) {
    return Optional.of(item.startTime().format(DateTimeFormatter.ofPattern("EEEE")));
  }

  @Override
  public Set<String> getPossibleNames() {
    return Set.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday");
  }
}
