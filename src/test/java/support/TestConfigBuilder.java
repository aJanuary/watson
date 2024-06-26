package support;

import com.ajanuary.watson.alarms.AlarmsConfig;
import com.ajanuary.watson.api.ApiConfig;
import com.ajanuary.watson.config.Config;
import com.ajanuary.watson.membership.MembershipConfig;
import com.ajanuary.watson.programme.ProgrammeConfig;
import com.ajanuary.watson.programme.ProgrammeConfig.Link;
import com.ajanuary.watson.programme.ProgrammeConfig.Location;
import com.ajanuary.watson.programme.ProgrammeConfig.NowOnConfig;
import com.ajanuary.watson.programme.channelnameresolvers.ChannelNameResolver;
import com.ajanuary.watson.programme.channelnameresolvers.DayChannelNameResolver;
import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.dv8tion.jda.api.entities.emoji.Emoji;

public class TestConfigBuilder {
  private String discordBotToken = "some-discord-bot-token";
  private String guildId = "some-guild-id";
  private String databasePath = "some-database-path";
  private String portalApiKey = "some-portal-api-key";
  private ZoneId timezone = ZoneId.of("UTC");
  private TestAlarmsConfigBuilder alarmsConfigBuilder = null;
  private TestApiConfigBuilder apiConfigBuilder = null;
  private TestMembershipConfigBuilder membershipConfigBuilder = null;
  private TestProgrammeConfigBuilder programmeConfigBuilder = null;

  public Config build() {
    return new Config(
        discordBotToken,
        guildId,
        databasePath,
        portalApiKey,
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

  public TestConfigBuilder withPortalApiKey(String portalApiKey) {
    this.portalApiKey = portalApiKey;
    return this;
  }

  public TestConfigBuilder withAlarmsConfig(Consumer<TestAlarmsConfigBuilder> configure) {
    if (alarmsConfigBuilder == null) {
      alarmsConfigBuilder = new TestAlarmsConfigBuilder();
    }
    configure.accept(alarmsConfigBuilder);
    return this;
  }

  public TestConfigBuilder withNoAlarmsConfig() {
    alarmsConfigBuilder = null;
    return this;
  }

  public TestConfigBuilder withApiConfig(Consumer<TestApiConfigBuilder> configure) {
    if (apiConfigBuilder == null) {
      apiConfigBuilder = new TestApiConfigBuilder();
    }
    configure.accept(apiConfigBuilder);
    return this;
  }

  public TestConfigBuilder withNoApiConfig() {
    apiConfigBuilder = null;
    return this;
  }

  public TestConfigBuilder withMembershipConfig(Consumer<TestMembershipConfigBuilder> configure) {
    if (membershipConfigBuilder == null) {
      membershipConfigBuilder = new TestMembershipConfigBuilder();
    }
    configure.accept(membershipConfigBuilder);
    return this;
  }

  public TestConfigBuilder withNoMembershipConfig() {
    membershipConfigBuilder = null;
    return this;
  }

  public TestConfigBuilder withProgrammeConfig(Consumer<TestProgrammeConfigBuilder> configure) {
    if (programmeConfigBuilder == null) {
      programmeConfigBuilder = new TestProgrammeConfigBuilder();
    }
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
    private URI membersApiUrl = URI.create("https://example.com/some-api-root");
    private String helpDeskChannel = "some-help-desk-channel";
    private String discordModsChannel = "some-discord-mods-channel";
    private String memberRole = "some-member-role";
    private String unverifiedRole = "some-unverified-role";
    private final Map<String, String> additionalRoles = new HashMap<>();

    public MembershipConfig build() {
      return new MembershipConfig(
          membersApiUrl,
          helpDeskChannel,
          discordModsChannel,
          memberRole,
          unverifiedRole,
          additionalRoles);
    }

    public TestMembershipConfigBuilder withMembersApiUrl(URI membersApiUrl) {
      this.membersApiUrl = membersApiUrl;
      return this;
    }

    public TestMembershipConfigBuilder withHelpDeskChannel(String helpDeskChannel) {
      this.helpDeskChannel = helpDeskChannel;
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
    private URI programmeApiRoot = URI.create("https://example.com/some-api-root");
    private URI assignDiscordPostsApiUrl = URI.create("https://example.com/some-api-root");
    private String majorAnnouncementChannel = "some-major-announcement-channel";
    private Optional<NowOnConfig> nowOnConfig = Optional.empty();
    private ChannelNameResolver channelNameResolver = new DayChannelNameResolver();
    private List<Link> links = new ArrayList<>();
    private List<Location> locations = new ArrayList<>();
    private boolean hasPerformedFirstLoad = true;

    public ProgrammeConfig build() {
      return new ProgrammeConfig(
          programmeApiRoot,
          assignDiscordPostsApiUrl,
          majorAnnouncementChannel,
          nowOnConfig,
          channelNameResolver,
          links,
          locations,
          hasPerformedFirstLoad);
    }

    public TestProgrammeConfigBuilder withProgrammeApiRoot(URI programmeApiRoot) {
      this.programmeApiRoot = programmeApiRoot;
      return this;
    }

    public TestProgrammeConfigBuilder withAssignDiscordPostsApiUrl(URI assignDiscordPostsApiUrl) {
      this.assignDiscordPostsApiUrl = assignDiscordPostsApiUrl;
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

    public TestProgrammeConfigBuilder withLink(String name, String url) {
      links.add(new Link(name, url));
      return this;
    }

    public TestProgrammeConfigBuilder withLocation(String name, String url) {
      locations.add(new Location(name, url));
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
