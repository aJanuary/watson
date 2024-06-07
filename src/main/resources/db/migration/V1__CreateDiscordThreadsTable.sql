create table discord_threads (
  programme_item_id string primary key,
  thread_id string not null,
  message_id string not null,
  title string not null,
  desc blob not null,
  loc string not null,
  time string not null,
  start_time string not null,
  end_time string not null,
  status string not null,
  processed_alarms tinyint not null default 0
);