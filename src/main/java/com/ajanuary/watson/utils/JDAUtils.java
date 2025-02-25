package com.ajanuary.watson.utils;

import com.ajanuary.watson.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.Optional;

public class JDAUtils {

  private final JDA jda;
  private final Config config;

  public JDAUtils(JDA jda, Config config) {
    this.jda = jda;
    this.config = config;
  }

  public Role getRole(String roleName) {
    var guild = jda.getGuildById(config.guildId());
    assert guild != null;

    var roles = guild.getRolesByName(roleName, true);
    if (roles.isEmpty()) {
      throw new IllegalArgumentException("Role not found: " + roleName);
    }
    if (roles.size() > 1) {
      throw new IllegalArgumentException("Multiple roles found with label: " + roleName);
    }
    return roles.get(0);
  }

  public TextChannel getTextChannel(String channelName) {
    return getOptionalTextChannel(channelName).orElseThrow(() -> new IllegalArgumentException("Channel not found: " + channelName));
  }

  public Optional<TextChannel> getOptionalTextChannel(String channelName) {
    var guild = jda.getGuildById(config.guildId());
    assert guild != null;

    var channels = guild.getTextChannelsByName(channelName, true);
    if (channels.isEmpty()) {
      return Optional.empty();
    }
    if (channels.size() > 1) {
      throw new IllegalArgumentException("Multiple channels found with the name: " + channelName);
    }
    return Optional.of(channels.get(0));
  }
}
