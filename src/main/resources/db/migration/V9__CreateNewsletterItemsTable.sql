CREATE TABLE newsletter_items (
  id VARCHAR NOT NULL PRIMARY KEY,
  discord_message_id VARCHAR NOT NULL,
  content_checksum VARCHAR NOT NULL
);
