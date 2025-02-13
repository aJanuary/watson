package com.ajanuary.watson.programme.channelnameresolvers;

import com.ajanuary.watson.programme.ProgrammeItem;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class LocChannelNameResolver implements ChannelNameResolver {

  private final Map<String, String> locMappings;

  public LocChannelNameResolver(Map<String, String> locMappings) {
    this.locMappings = locMappings;
  }

  @Override
  public Optional<String> resolveChannelName(ProgrammeItem item) {
    return Optional.ofNullable(locMappings.get(item.loc()));
  }

  @Override
  public Set<String> getPossibleNames() {
    return new HashSet<>(locMappings.values());
  }

  @Override
  public boolean nameIncludesDay() {
    return false;
  }
}
