package com.ajanuary.watson.membership;

import com.ajanuary.watson.api.CheckUserEvent;
import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.membership.MembershipChecker.DiscordUser;
import com.ajanuary.watson.notification.EventDispatcher;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MembershipModule implements EventListener {

  private final Logger logger = LoggerFactory.getLogger(MembershipModule.class);

  private final JDA jda;
  private final Config config;
  private final MembershipChecker membershipChecker;
  private final Lock membersLock = new ReentrantLock();
  private final Condition hasMembersToCheck = membersLock.newCondition();

  private Set<DiscordUser> membersToCheck = new HashSet<>();

  public MembershipModule(
      JDA jda,
      Config config,
      MembershipChecker membershipChecker,
      EventDispatcher eventDispatcher) {
    this.jda = jda;
    this.config = config;
    this.membershipChecker = membershipChecker;
    eventDispatcher.register(
        CheckUserEvent.class, e -> membershipChecker.checkMembership(e.users()));

    var pollingThread =
        new Thread(
            () -> {
              while (true) {
                try {
                  membersLock.lock();
                  while (membersToCheck.isEmpty()) {
                    hasMembersToCheck.await();
                  }
                  var capturedMembersToCheck = membersToCheck;
                  membersToCheck = new HashSet<>();
                  membershipChecker.checkMembership(capturedMembersToCheck);
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                  break;
                } finally {
                  membersLock.unlock();
                }
              }
            });
    pollingThread.setName("MembersPollingThread");
    pollingThread.start();

    jda.addEventListener(this);
    checkAllMembers();
  }

  @Override
  public void onEvent(@NotNull GenericEvent event) {
    if (event instanceof GenericGuildEvent guildEvent) {
      if (!guildEvent.getGuild().getId().equals(config.guildId())) {
        return;
      }
    }

    if (event instanceof SessionResumeEvent) {
      checkAllMembers();
    } else if (event instanceof GuildMemberJoinEvent guildMemberJoinEvent) {
      var member = guildMemberJoinEvent.getMember();
      membersLock.lock();
      try {
        if (membershipChecker.shouldCheckMember(member)) {
          membersToCheck.add(new DiscordUser(member.getId(), member.getUser().getName()));
          hasMembersToCheck.signal();
        }
      } finally {
        membersLock.unlock();
      }
    }
  }

  private void checkAllMembers() {
    var guild = jda.getGuildById(config.guildId());
    if (guild == null) {
      logger.error("Could not find guild with ID {}", config.guildId());
      return;
    }

    guild
        .loadMembers()
        .onSuccess(
            members -> {
              membersLock.lock();
              try {
                members.stream()
                    .filter(membershipChecker::shouldCheckMember)
                    .map(m -> new DiscordUser(m.getId(), m.getUser().getName()))
                    .forEach(membersToCheck::add);
                hasMembersToCheck.signal();
              } finally {
                membersLock.unlock();
              }
            })
        .onError(e -> logger.error("Error loading members", e));
  }
}
