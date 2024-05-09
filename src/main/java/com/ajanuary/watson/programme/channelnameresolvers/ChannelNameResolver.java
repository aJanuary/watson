package com.ajanuary.watson.programme.channelnameresolvers;

import com.ajanuary.watson.programme.ProgrammeItem;
import java.util.Set;

public interface ChannelNameResolver {

  String resolveChannelName(ProgrammeItem item);

  Set<String> getPossibleNames();
}
