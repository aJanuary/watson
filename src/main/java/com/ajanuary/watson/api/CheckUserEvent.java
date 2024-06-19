package com.ajanuary.watson.api;

import com.ajanuary.watson.membership.MembershipChecker.DiscordUser;
import com.ajanuary.watson.notification.Event;
import java.util.Collection;

public record CheckUserEvent(Collection<DiscordUser> users) implements Event {}
