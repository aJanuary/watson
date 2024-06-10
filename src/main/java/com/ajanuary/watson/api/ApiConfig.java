package com.ajanuary.watson.api;

import com.ajanuary.watson.utils.JDAUtils;

public record ApiConfig(String channel) {

  public void validateDiscordConfig(JDAUtils jdaUtils) {
    jdaUtils.getTextChannel(channel());
  }
}
