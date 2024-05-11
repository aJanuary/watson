create table scheduled_dm (
  id integer primary key autoincrement,
  discord_thread_id string not null,
  discord_message_id string not null,
  user_id string not null,
  message_time string not null,
  title string not null,
  jump_url string not null,
  contents string not null,
  tags string
);