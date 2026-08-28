-- V11: Add pull model fields for remote runners
ALTER TABLE runners ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP;
ALTER TABLE runners ADD COLUMN IF NOT EXISTS registration_token VARCHAR(255);
ALTER TABLE runners ADD COLUMN IF NOT EXISTS registration_token_expires_at TIMESTAMP;
ALTER TABLE runners ALTER COLUMN host DROP NOT NULL;
ALTER TABLE runners ALTER COLUMN port DROP NOT NULL;
ALTER TABLE runners ALTER COLUMN username DROP NOT NULL;
