-- Two-factor login with an authenticator app (TOTP).
ALTER TABLE users ADD COLUMN IF NOT EXISTS totp_secret varchar(64);
ALTER TABLE users ADD COLUMN IF NOT EXISTS totp_enabled boolean NOT NULL DEFAULT false;
-- Last accepted 30-second step, so one code cannot be used twice.
ALTER TABLE users ADD COLUMN IF NOT EXISTS totp_last_step bigint NOT NULL DEFAULT 0;
