package com.ajanuary.watson.programme;

import com.ajanuary.watson.programme.channelnameresolvers.ChannelNameResolver;
import com.ajanuary.watson.utils.JDAUtils;
import net.dv8tion.jda.api.Permission;

import java.net.URI;
import java.time.temporal.TemporalAmount;
import java.util.List;
import java.util.Optional;

public record ProgrammeConfig(
    URI programmeUrl,
    Optional<URI> assignDiscordPostsApiUrl,
    String majorAnnouncementsChannel,
    Optional<NowOnConfig> nowOn,
    ChannelNameResolver channelNameResolver,
    List<Link> links,
    List<Location> locations,
    boolean hasPerformedFirstLoad) {

  public void validateDiscordConfig(JDAUtils jdaUtils) {
    var majorAnnouncementsChannel = jdaUtils.getTextChannel(majorAnnouncementsChannel());
    jdaUtils.checkPermissions(majorAnnouncementsChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS);

    nowOn().ifPresent(nowOn -> {
      var nowOnChannel = jdaUtils.getTextChannel(nowOn.channel());
      jdaUtils.checkPermissions(nowOnChannel, Permission.MESSAGE_SEND);
    });

    var channelNames = channelNameResolver().getChannelNames();
    for (var channelName : channelNames) {
      var forumChannel = jdaUtils.getForumChannel(channelName);
      jdaUtils.checkPermissions(forumChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_SEND_IN_THREADS, Permission.MESSAGE_EMBED_LINKS);
    }
  }

  public record NowOnConfig(
      String channel, TemporalAmount timeBeforeToAdd, TemporalAmount timeAfterToKeep) {}

  public record Link(String name, String label) {}

  public record Location(String id, String name) {}
}
