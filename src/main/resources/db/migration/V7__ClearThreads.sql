-- This will clear all threads from the database
-- This is okay in our case, because we actually need to clear them out anyway, because they have
-- testing data in them.
-- This is not the way to do production code, but we're the only customer, and it's real close to
-- the con so _shrug_

drop table discord_threads;

create table discord_threads (
  programme_item_id string primary key,
  thread_id string null,
  message_id string null,
  title string not null,
  desc blob,
  loc string not null,
  time string not null,
  start_time string not null,
  end_time string not null,
  status string not null,
  processed_alarms tinyint not null default 0
);