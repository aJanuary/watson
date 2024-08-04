package com.ajanuary.watson.programme.channelnameresolvers;

import com.ajanuary.watson.programme.ProgrammeItem;
import java.util.Optional;
import java.util.Set;

public interface ChannelNameResolver {

  Optional<String> resolveChannelName(ProgrammeItem item);

  Set<String> getPossibleNames();
}
