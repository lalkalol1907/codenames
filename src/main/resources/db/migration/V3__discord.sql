ALTER TABLE rooms ADD COLUMN discord_instance_id VARCHAR(64);
ALTER TABLE rooms ADD COLUMN discord_channel_id VARCHAR(64);
CREATE UNIQUE INDEX idx_rooms_discord_instance_id ON rooms(discord_instance_id);

ALTER TABLE players ADD COLUMN discord_user_id VARCHAR(64);
ALTER TABLE players ADD COLUMN avatar_url VARCHAR(256);
CREATE UNIQUE INDEX idx_players_room_discord_user ON players(room_id, discord_user_id);
