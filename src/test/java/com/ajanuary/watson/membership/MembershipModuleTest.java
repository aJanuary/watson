package com.ajanuary.watson.membership;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.junit.jupiter.api.Test;
import support.ResolvedTask;
import support.TestConfigBuilder;

public class MembershipModuleTest {
  @Test
  void onStartupVerifiesAllUsers() {
    var config = new TestConfigBuilder().withGuildId("the-guild-id").build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var member1 = mock(Member.class);
    var member2 = mock(Member.class);
    var membershipChecker = mock(MembershipChecker.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    when(member1.getId()).thenReturn("the-member1-id");
    when(member2.getId()).thenReturn("the-member2-id");

    when(guild.loadMembers()).thenReturn(new ResolvedTask<>(List.of(member1, member2)));

    when(membershipChecker.shouldCheckMember(member1)).thenReturn(true);
    when(membershipChecker.shouldCheckMember(member2)).thenReturn(false);

    new MembershipModule(jda, config, membershipChecker);

    verify(membershipChecker, timeout(100)).checkMembership(Set.of("the-member1-id"));
  }

  @Test
  void onSessionResumeVerifiesAllUsers() {
    var config = new TestConfigBuilder().withGuildId("the-guild-id").build();

    var jda = mock(JDA.class);
    var guild = mock(Guild.class);
    var member1 = mock(Member.class);
    var member2 = mock(Member.class);
    var membershipChecker = mock(MembershipChecker.class);

    when(jda.getGuildById("the-guild-id")).thenReturn(guild);

    when(member1.getId()).thenReturn("the-member1-id");
    when(member2.getId()).thenReturn("the-member2-id");

    when(guild.loadMembers())
        .thenReturn(new ResolvedTask<>(List.of()))
        .thenReturn(new ResolvedTask<>(List.of(member1, member2)));

    when(membershipChecker.shouldCheckMember(member1)).thenReturn(true);
    when(membershipChecker.shouldCheckMember(member2)).thenReturn(false);

    // Invoke the listener with a ReadyEvent as soon as it's attached.
    // This is slightly different to the real behaviour, which will invoke the listener on a
    // separate thread, but invoking it directly makes the tests easier to manage.
    doAnswer(
            invocation -> {
              var listener = (EventListener) invocation.getArgument(0);
              listener.onEvent(mock(SessionResumeEvent.class));
              return null;
            })
        .when(jda)
        .addEventListener(any());

    new MembershipModule(jda, config, membershipChecker);

    verify(membershipChecker, timeout(100)).checkMembership(Set.of("the-member1-id"));
  }
}
