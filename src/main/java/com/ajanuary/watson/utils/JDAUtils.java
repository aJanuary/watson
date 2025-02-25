package com.ajanuary.watson.utils;

import com.ajanuary.watson.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.ForumChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

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
    return getOptionalTextChannel(channelName).orElseThrow(() -> new IllegalArgumentException("Text channel not found: " + channelName));
  }

  public Optional<TextChannel> getOptionalTextChannel(String channelName) {
    var guild = jda.getGuildById(config.guildId());
    assert guild != null;

    var channels = guild.getTextChannelsByName(channelName, true);
    if (channels.isEmpty()) {
      return Optional.empty();
    }
    if (channels.size() > 1) {
      throw new IllegalArgumentException("Multiple text channels found with the name: " + channelName);
    }
    return Optional.of(channels.get(0));
  }

  public ForumChannel getForumChannel(String channelName) {
    return getOptionalForumChannel(channelName).orElseThrow(() -> new IllegalArgumentException("Forum channel not found: " + channelName));
  }

  public Optional<ForumChannel> getOptionalForumChannel(String channelName) {
    var guild = jda.getGuildById(config.guildId());
    assert guild != null;

    var channels = guild.getForumChannelsByName(channelName, true);
    if (channels.isEmpty()) {
      return Optional.empty();
    }
    if (channels.size() > 1) {
      throw new IllegalArgumentException("Multiple forum channels found with the name: " + channelName);
    }
    return Optional.of(channels.get(0));
  }

  public void checkPermissions(GuildChannel channel, Permission... permissions) {
    var guild = jda.getGuildById(config.guildId());
    assert guild != null;
    for (var permission : permissions) {
      if (!guild.getSelfMember().hasPermission(channel, permission)) {
        throw new IllegalArgumentException("Bot is missing required permission '" + permission.getName() + "' on channel: " + channel.getName());
      }
    }
  }
}
