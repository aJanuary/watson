package com.ajanuary.watson.newsletter;

import com.ajanuary.watson.utils.JDAUtils;
import java.net.URI;
import java.time.Duration;
import net.dv8tion.jda.api.Permission;

public record NewsletterConfig(String channel, URI feedUrl, Duration pollInterval) {

  public void validateDiscordConfig(JDAUtils jdaUtils) {
    var textChannel = jdaUtils.getTextChannel(channel());
    jdaUtils.checkPermissions(
        textChannel,
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_ATTACH_FILES,
        Permission.MESSAGE_MANAGE);
  }
}
