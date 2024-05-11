package com.ajanuary.watson.membership;

import com.ajanuary.watson.config.Config;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
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
  private final Lock lock = new ReentrantLock();
  private final Condition hasMembersToCheck = lock.newCondition();

  private Set<String> membersToCheck = new HashSet<>();

  public MembershipModule(JDA jda, Config config, MembershipChecker membershipChecker) {
    this.jda = jda;
    this.config = config;
    this.membershipChecker = membershipChecker;

    var pollingThread =
        new Thread(
            () -> {
              while (true) {
                try {
                  lock.lock();
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
                  lock.unlock();
                }
              }
            });
    pollingThread.setName("MembersPollingThread");
    pollingThread.start();

    jda.addEventListener(this);
  }

  @Override
  public void onEvent(@NotNull GenericEvent event) {
    if (event instanceof ReadyEvent || event instanceof SessionResumeEvent) {
      var guild = jda.getGuildById(config.guildId());
      if (guild == null) {
        logger.error("Could not find guild with ID {}", config.guildId());
        return;
      }

      guild
          .loadMembers()
          .onSuccess(
              members -> {
                lock.lock();
                try {
                  members.stream()
                      .filter(membershipChecker::shouldCheckMember)
                      .map(Member::getId)
                      .forEach(membersToCheck::add);
                  hasMembersToCheck.signal();
                } finally {
                  lock.unlock();
                }
              })
          .onError(e -> logger.error("Error loading members", e));
    } else if (event instanceof GuildMemberJoinEvent guildMemberJoinEvent) {
      var member = guildMemberJoinEvent.getMember();
      lock.lock();
      try {
        membersToCheck.add(member.getId());
      } finally {
        lock.unlock();
      }
    }
  }
}
