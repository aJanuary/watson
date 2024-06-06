create table private_threads (
  user_id string not null,
  purpose string not null,
  discord_thread_id string not null,
  primary key (user_id, purpose)
);