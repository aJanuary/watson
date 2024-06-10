package com.ajanuary.watson.api;

import com.ajanuary.watson.membership.MembershipChecker.DiscordUser;
import com.ajanuary.watson.notification.Event;
import java.util.List;

public class CheckUserEvent implements Event {
  private final List<DiscordUser> users;

  public CheckUserEvent(List<DiscordUser> users) {
    this.users = users;
  }

  public List<DiscordUser> users() {
    return users;
  }
}
