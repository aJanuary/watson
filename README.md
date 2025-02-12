# Watson

A Discord bot for conventions.

The bot consists of several modules, which can be enabled or disabled in the configuration file.
Some of the modules can coordinate with a member portal via the member portal's API.

## Requirements
* Java 21 or above

## Modules

### Alarms
Allows users to set reminders for programme items (it requires the programme module to also be used).
The bot will add an emoji to each programme item message, which users can react to in order to set a reminder.
Shortly before the start time of the programme item the bot will send a message to the user in a private thread.

### API
Allows the bot to listen for messages in a specific channel and respond to them. Useful for providing a mechanism for
external services such as the member portal to interact with the bot without having to directly expose the bot via TCP
or HTTP.

The messages in the channel should contain JSON with a key `action`.

The currently supported actions are:
* `recheck-user`: Requests the bot to recheck the roles of a user. 
  Additional parameters: `user-id` - the ID of the user to recheck.

### Membership
Allows the bot to authenticate users and assign roles based on their membership status in a member portal.

When a user joins, the bot will send a request to the member portal to check their Discord id is associated with a
member. If it isn't, then it sends a private message to the user with a link to the member portal to connect their
Discord id with their membership.

The member portal can also send a list of roles to assign the user in the response.

### Programme
Allows the bot to post programme items from a JSON file to Discord.

The bot will create a forum post in a channel for each programme items. It will monitor the programme JSON for changes
and update the forum post. If the item's start time or room changes it will send a message to a major announcements
channel.

The bot can also send "Now on" messages when an item starts and remove them when the item ends.

The posts will be created in reverse start time order. When the forum channel is configured to show items in created
time order, the items will be ordered according to their start time. A limitation of this approach is that if the start
time changes, it will no longer be in the correct order.

## Usage
`java -jar watson.jar <secrets file> <config file>`

## Secrets file
Contains the secrets for this bot. Should not be shared or stored in SCM.

```yaml
# Token used to authenticate the bot with Discord.
# Taken from the Bot page of the Discord Developer Portal.
# Required.
# e.g. OXWdKdj39EC3Uxh5SDI3OdMsNW.Ld_Oxk.394uDJAnFJSl5zd3O52fIr_dkI38dSJ3djAND3
discordBotToken: <discord bot token>

# Key used to authenticate requests to the portal.
# Required if using the membership module.
# e.g. 12345678-1234-1234-1234-123456789012
portalApiKey: <portal api key>
```

## Configuration file
Contains configuration for the bot. May be stored in SCM.

```yaml
# ID of the guild (aka server) where the bot will operate.
# Can be found in the Discord client by enabling Developer Mode and right-clicking on the guild.
# Required.
# e.g. "123456789012345678"
guildId: <guild id>

# Path to the file to store the bot's persistent state.
# Should be a durable path that survives restarting the server.
# The OS user running the bot will need permission to create and write to this file.
# Required.
# e.g. /var/lib/watson/testserver.db
databasePath: <database path>

# Timezone the programme schedule is expressed in.
# See https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZoneId.html#time-zone-ids-heading for the list of valid values.
# Required.
# e.g. Europe/London
timezone: <timezone>

# Enable the alarms module.
# Optional. If not provided, the module will be disabled.
alarms:
  # Emoji to use when reacting to messages to set an alarm.
  # Should be a single emoji encoded using the rules for a Java string literal.
  # Optional. Defaults to `U+23F0`
  # e.g. 0x23F0
  alarmEmoji: <emoji>

  # Name of the channel to send alarm messages to.
  # The bot must have permissions to create private threads in the channel.
  # Optional. Defaults to `reminders`
  alarmsChannel: <channel name>

  # How long before the event time should the reminder by sent.
  # Expressed as an ISO8601 duration without the leading `PT`.
  # Required
  # e.g. 15m
  timeBeforeToNotify: <duration>

  # If reminders get delayed, how long after the event should it give up on sending the reminder.
  # Expressed as an ISO8601 duration without the leading `PT`.
  # Required
  # e.g. 5m
  maxTimeAfterToNotify: <duration>

  # How long to wait between sending reminder messages.
  # Used to prevent Discord from thinking you're trying to spam the server.
  # Expressed as an ISO8601 duration without the leading `PT`.
  # Required
  # e.g. 2s
  minTimeBetweenDMs: <duration>

# Enable the API module.
# Optional. If not provided, the module will be disabled.
api:
  # Name of the channel to listen for API messages on.
  # The bot must have permissions to read messages in the channel.
  # Optional. Defaults to `api-messages`
  # e.g. api-messages
  channel: <channel name>

# Enable the membership module.
# Optional. If not provided, the module will be disabled.
membership:
  # Url of the member portal's membership API.
  # Required.
  # e.g. https://portal.democon.example.com/api/membership
  membershipApiUrl: <url>

  # Name of the channel to send membership-related messages to.
  # If the member portal doesn't know about the user, the bot will send
  # a message to the user in this channel via a private thread with a link
  # to connect their Discord ID with their membership.
  # The bot must have permissions to create private threads in the channel.
  # Optional. Defaults to `help-desk`
  # e.g. help-desk
  helpDeskChannel: <channel name>

  # Name of the role to add to members.
  # Optional. Defaults to `member`.
  # e.g. member
  memberRole: <role name>

  # Name of the role to add to unverified members.
  # If the member portal doesn't know about the user, the bot will add this role to them.
  # Can be used to prevent access to privileged channels.
  # Optional. Defaults to `unverified`.
  # e.g. unverified
  unverifiedRole: <role name>

  # Name of the role to include in private threads in the help desk channel.
  # Optional. Defaults to `Discord Mod`.
  # e.g. Discord Mod
  memberHelpRole: <role name>

  # Mapping of additional roles to add to members based on their roles in the member portal.
  # Any unknown roles in the member portal will be ignored.
  # Optional.
  # e.g.
  #  Artist: artist
  #  Staff (staff): staff
  #  Staff (committee): staff
  additionalRoles:
    <name in the member portal>: <discord role name>

# Enable the programme module.
# Optional. If not provided, the module will be disabled.
programme:
  # Whether the programme has finished loading or not.
  # If set to true, items not seen before will be treated as new (for example,
  # a message will be sent in the major announcements channel).
  hasPerformedFirstLoad: <true/false>
  
  # Url of the programme JSON.
  # Should be a ConClár compatible JSON. (https://github.com/lostcarpark/conclar)
  # Required.
  # e.g. https://example.com/programme.json
  programmeUrl: <url>

  # Url of the member portal API to submit Discord post ids to.
  # If provided, the `portalApiKey` must be set in the secrets file.
  # Optional. If not provided, no posts will be submitted.
  # e.g. https://portal.democon.example.com/api/discord-posts
  assignDiscordPostsApiUrl: <url>

  # Name of the channel to send major announcements to.
  # A major announcement is any time a programme item's start time or room changes.
  # The bot must have permissions to send messages in the channel.
  # Optional. Defaults to `programme-announcements`
  # e.g. programme-announcements
  majorAnnouncementsChannel: <channel name>

  # Enable sending "Now on" messages.
  # Now on messages get posted when the item starts, and deleted when the item ends.
  # Optional. If not provided, no now on messages are sent.
  nowOn:
    # Name of the channel to send "Now on" messages to.
    # The bot must have permissions to send messages in the channel.
    # Optional. Defaults to `now-on`
    # e.g. now-on
    channel: <channel name>

    # How long before the event time should the "Now on" message be sent.
    # Expressed as an ISO8601 duration without the leading `PT`.
    # Required
    # e.g. 5m
    timeBeforeToAdd: <duration>

    # How long after the event time should the "Now on" message be removed.
    # Expressed as an ISO8601 duration without the leading `PT`.
    # Required
    # e.g. 5m
    timeAfterToKeep: <duration>

  # How to decide which channel to post a programme item in.
  # The channel must be a forum channel.
  # Optional. Defaults to day.
  channelNameResolver:
    # Type of the resolver to use.
    # Must be one of:
    # `day`     - Posts in a channel named after the day of the week the item is on.
    # `day-tod` - Posts in a channel named after the day of the week and part of the day (e.g. morning, afternoon, evening) the item is on.
    # `loc`     - Posts in a channel named after the location the item is in.
    # e.g. day-tod
    type: <type name>

     # How to label the parts of the day.
     # Only used when `type` is `day-tod`
     # A list of threshold configurations. The time for the parts must not overlap.
     # If none of the thresholds are matched, it will just use the day without a time of day part.
     # Required when `type` is `day-tod`    
    thresholds:
      # Label for the part of day.
      # Will be appended to the day name to make the channel name.
      # Required
      # e.g. morning
      - label: <label>
        # Time the part of day starts.
        # Expressed as hh:mm in the configured timezone.
        # Required
        # e.g. 09:00
        start: <start time>
        # Time the part of day ends.
        # Expressed as hh:mm in the configured timezone.
        # Required
        # e.g. 12:00
        end: <end time>

    # How to map locations to channel names.
    # Only used when `type` is `loc`
    # A list of mappings from location names to channel names.
    # Required when `type` is `loc`
    # e.g.
    #  Room 1: room-1
    #  Room 2: room-2
    locMappings:
      <loc name>: <channel name>

  # Which links from the programme JSON to include in the message.
  # List of links to include in the message.
  # The messages will include a link with the given label using the URL from the programme JSON.
  # Required.
  links:
    # Key of the link in the programme JSON.
    # Required
    # e.g. stream
    - name: <link key>
      # Label to use for the link in the message.
      # Required
      # e.g. Watch the stream
      label: <link label>

  # Mapping of location names to member portal location ids.
  # Used to submit the location id to the member portal when a programme item is posted.
  # Required.
  locations:
    # Location id from the member portal.
    # Required
    # e.g. room-1
    - id: <location id>
      # Name of the location in the programme JSON.
      # Required
      # e.g. Room 1
      name: <location name>
```

## Development

### Entry point

The entry point is [`com.ajanuary.watson.RunBot`](src/main/java/com/ajanuary/watson/RunBot.java).

### Clear Channels

The [`com.ajanuary.watson.utils.ClearChannels`](src/main/java/com/ajanuary/watson/utils/ClearChannels.java) entry point
can be used to clear all the programme related messages.