create table now_on (
  programme_item_id string primary key,
  discord_message_id string not null,
  end_time string not null
);