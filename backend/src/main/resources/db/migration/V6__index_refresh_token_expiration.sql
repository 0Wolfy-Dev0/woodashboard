-- V6 : speed up the scheduled cleanup of expired refresh tokens

CREATE INDEX idx_refresh_tokens_expiration ON refresh_tokens (expiration_time);
