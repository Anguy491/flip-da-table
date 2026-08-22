CREATE TABLE IF NOT EXISTS games (
  id TEXT PRIMARY KEY,
  session_id TEXT NOT NULL,
  round_index INT NOT NULL,
  game_type TEXT NOT NULL,
  state TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_games_session ON games(session_id);
