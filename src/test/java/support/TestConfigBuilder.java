package support;

import com.ajanuary.watson.alarms.AlarmsConfig;
import com.ajanuary.watson.api.ApiConfig;
import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.membership.MembershipConfig;
import com.ajanuary.watson.programme.ProgrammeConfig;
import com.ajanuary.watson.programme.ProgrammeConfig.NowOnConfig;
import com.ajanuary.watson.programme.channelnameresolvers.ChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayChannelNameResolver;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class TestConfigBuilder {
  private String discordBotToken = "some-discord-bot-token";
  private String guildId = "some-guild-id";
  private String databasePath = "some-database-path";
  private ZoneId timezone = ZoneId.of("UTC");
  private TestAlarmsConfigBuilder alarmsConfigBuilder = new TestAlarmsConfigBuilder();
  private TestApiConfigBuilder apiConfigBuilder = new TestApiConfigBuilder();
  private TestMembershipConfigBuilder membershipConfigBuilder = new TestMembershipConfigBuilder();
  private TestProgrammeConfigBuilder programmeConfigBuilder = new TestProgrammeConfigBuilder();

  public Config build() {
    return new Config(
        discordBotToken,
        guildId,
        databasePath,
        timezone,
        Optional.ofNullable(alarmsConfigBuilder).map(TestAlarmsConfigBuilder::build),
        Optional.ofNullable(apiConfigBuilder).map(TestApiConfigBuilder::build),
        Optional.ofNullable(membershipConfigBuilder).map(TestMembershipConfigBuilder::build),
        Optional.ofNullable(programmeConfigBuilder).map(TestProgrammeConfigBuilder::build));
  }

  public TestConfigBuilder withDiscordBotToken(String discordBotToken) {
    this.discordBotToken = discordBotToken;
    return this;
  }

  public TestConfigBuilder withGuildId(String guildId) {
    this.guildId = guildId;
    return this;
  }

  public TestConfigBuilder withDatabasePath(String databasePath) {
    this.databasePath = databasePath;
    return this;
  }

  public TestConfigBuilder withTimezone(ZoneId timezone) {
    this.timezone = timezone;
    return this;
  }

  public TestConfigBuilder withAlarmsConfig(Consumer<TestAlarmsConfigBuilder> configure) {
    configure.accept(alarmsConfigBuilder);
    return this;
  }

  public TestConfigBuilder withNoAlarmsConfig() {
    alarmsConfigBuilder = null;
    return this;
  }

  public TestConfigBuilder withMembershipConfig(Consumer<TestMembershipConfigBuilder> configure) {
    configure.accept(membershipConfigBuilder);
    return this;
  }

  public TestConfigBuilder withNoMembershipConfig() {
    membershipConfigBuilder = null;
    return this;
  }

  public TestConfigBuilder withProgrammeConfig(Consumer<TestProgrammeConfigBuilder> configure) {
    configure.accept(programmeConfigBuilder);
    return this;
  }

  public TestConfigBuilder withNoProgrammeConfig() {
    programmeConfigBuilder = null;
    return this;
  }

  public static class TestAlarmsConfigBuilder {
    private Emoji alarmsEmoji = Emoji.fromUnicode("⏰");
    private String alarmsChannel = "some-alarms-channel";
    private TemporalAmount timeBeforeToNotify = Duration.ofMinutes(5);
    private TemporalAmount maxTimeAfterToNotify = Duration.ofMinutes(15);
    private TemporalAmount minTimeBetweenDMs = Duration.ofSeconds(0, 500000000);

    public AlarmsConfig build() {
      return new AlarmsConfig(
          alarmsEmoji, alarmsChannel, timeBeforeToNotify, maxTimeAfterToNotify, minTimeBetweenDMs);
    }

    public TestAlarmsConfigBuilder withAlarmsEmoji(Emoji alarmsEmoji) {
      this.alarmsEmoji = alarmsEmoji;
      return this;
    }

    public TestAlarmsConfigBuilder withAlarmsChannel(String alarmsChannel) {
      this.alarmsChannel = alarmsChannel;
      return this;
    }

    public TestAlarmsConfigBuilder withTimeBeforeToNotify(TemporalAmount timeBeforeToNotify) {
      this.timeBeforeToNotify = timeBeforeToNotify;
      return this;
    }

    public TestAlarmsConfigBuilder withMaxTimeAfterToNotify(TemporalAmount maxTimeAfterToNotify) {
      this.maxTimeAfterToNotify = maxTimeAfterToNotify;
      return this;
    }

    public TestAlarmsConfigBuilder withMinTimeBetweenDMs(TemporalAmount minTimeBetweenDMs) {
      this.minTimeBetweenDMs = minTimeBetweenDMs;
      return this;
    }
  }

  public static class TestApiConfigBuilder {
    private String channel = "some-channel";

    public ApiConfig build() {
      return new ApiConfig(channel);
    }

    public TestApiConfigBuilder withChannel(String channel) {
      this.channel = channel;
      return this;
    }
  }

  public static class TestMembershipConfigBuilder {
    private String membersApiUrl = "https://example.com/some-api-root";
    private String membersApiKey = "some-api-key";
    private String helpDeskChannel = "some-help-desk-channel";
    private String discordModsChannel = "some-discord-mods-channel";
    private String memberRole = "some-member-role";
    private String unverifiedRole = "some-unverified-role";
    private final Map<String, String> additionalRoles = new HashMap<>();

    public MembershipConfig build() {
      return new MembershipConfig(
          membersApiUrl,
          membersApiKey,
          helpDeskChannel,
          discordModsChannel,
          memberRole,
          unverifiedRole,
          additionalRoles);
    }

    public TestMembershipConfigBuilder withMembersApiUrl(String membersApiUrl) {
      this.membersApiUrl = membersApiUrl;
      return this;
    }

    public TestMembershipConfigBuilder withMembersApiKey(String membersApiKey) {
      this.membersApiKey = membersApiKey;
      return this;
    }

    public TestMembershipConfigBuilder withDiscordModsChannel(String discordModsChannel) {
      this.discordModsChannel = discordModsChannel;
      return this;
    }

    public TestMembershipConfigBuilder withMemberRole(String memberRole) {
      this.memberRole = memberRole;
      return this;
    }

    public TestMembershipConfigBuilder withUnverifiedRole(String unverifiedRole) {
      this.unverifiedRole = unverifiedRole;
      return this;
    }

    public TestMembershipConfigBuilder withAdditionalRole(String roleName, String roleId) {
      additionalRoles.put(roleName, roleId);
      return this;
    }
  }

  public static class TestProgrammeConfigBuilder {
    private String programmeApiRoot = "https://example.com/some-api-root";
    private String majorAnnouncementChannel = "some-major-announcement-channel";
    private Optional<NowOnConfig> nowOnConfig = Optional.empty();
    private ChannelNameResolver channelNameResolver = new DayChannelNameResolver();
    private boolean hasPerformedFirstLoad = true;

    public ProgrammeConfig build() {
      return new ProgrammeConfig(
          programmeApiRoot,
          majorAnnouncementChannel,
          nowOnConfig,
          channelNameResolver,
          hasPerformedFirstLoad);
    }

    public TestProgrammeConfigBuilder withProgrammeApiRoot(String programmeApiRoot) {
      this.programmeApiRoot = programmeApiRoot;
      return this;
    }

    public TestProgrammeConfigBuilder withMajorAnnouncementChannel(
        String majorAnnouncementChannel) {
      this.majorAnnouncementChannel = majorAnnouncementChannel;
      return this;
    }

    public TestProgrammeConfigBuilder withNowOn(Consumer<TestNowOnConfigBuilder> configure) {
      var builder = new TestNowOnConfigBuilder();
      configure.accept(builder);
      this.nowOnConfig = Optional.of(builder.build());
      return this;
    }

    public TestProgrammeConfigBuilder withChannelNameResolver(
        ChannelNameResolver channelNameResolver) {
      this.channelNameResolver = channelNameResolver;
      return this;
    }

    public TestProgrammeConfigBuilder withHasPerformedFirstLoad(boolean hasPerformedFirstLoad) {
      this.hasPerformedFirstLoad = hasPerformedFirstLoad;
      return this;
    }
  }

  public static class TestNowOnConfigBuilder {
    private String channel = "some-channel";
    private TemporalAmount timeBeforeToAdd = Duration.ofMinutes(5);
    private TemporalAmount timeAfterToKeep = Duration.ofMinutes(15);

    public NowOnConfig build() {
      return new NowOnConfig(channel, timeBeforeToAdd, timeAfterToKeep);
    }

    public TestNowOnConfigBuilder withChannel(String channel) {
      this.channel = channel;
      return this;
    }

    public TestNowOnConfigBuilder withTimeBeforeToAdd(TemporalAmount timeBeforeToAdd) {
      this.timeBeforeToAdd = timeBeforeToAdd;
      return this;
    }

    public TestNowOnConfigBuilder withTimeAfterToKeep(TemporalAmount timeAfterToKeep) {
      this.timeAfterToKeep = timeAfterToKeep;
      return this;
    }
  }
}
