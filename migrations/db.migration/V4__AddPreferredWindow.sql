ALTER TABLE events ADD COLUMN IF NOT EXISTS preferred_window_start TIME;

ALTER TABLE events ADD COLUMN IF NOT EXISTS preferred_window_end TIME;
