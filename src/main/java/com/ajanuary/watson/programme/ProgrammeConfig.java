package com.ajanuary.watson.programme;

import com.ajanuary.watson.programme.channelnameresolvers.ChannelNameResolver;
import com.ajanuary.watson.utils.JDAUtils;
import java.net.URI;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

public record ProgrammeConfig(
    URI programmeUrl,
    URI assignDiscordPostsApiUrl,
    String majorAnnouncementsChannel,
    Optional<NowOnConfig> nowOn,
    ChannelNameResolver channelNameResolver,
    boolean hasPerformedFirstLoad) {

  public void validateDiscordConfig(JDAUtils jdaUtils) {
    jdaUtils.getTextChannel(majorAnnouncementsChannel());
    nowOn().ifPresent(nowOn -> jdaUtils.getTextChannel(nowOn.channel()));
  }

  public record NowOnConfig(
      String channel, TemporalAmount timeBeforeToAdd, TemporalAmount timeAfterToKeep) {}
}
