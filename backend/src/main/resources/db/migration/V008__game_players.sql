CREATE TABLE IF NOT EXISTS game_players (
    id BIGSERIAL PRIMARY KEY,
    game_id TEXT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    player_id TEXT NOT NULL,
    seat_index INT NOT NULL CHECK (seat_index >= 0),
    CONSTRAINT uk_game_players_game_user UNIQUE (game_id, user_id),
    CONSTRAINT uk_game_players_game_player UNIQUE (game_id, player_id),
    CONSTRAINT uk_game_players_game_seat UNIQUE (game_id, seat_index)
);

CREATE INDEX IF NOT EXISTS idx_game_players_user ON game_players(user_id);
