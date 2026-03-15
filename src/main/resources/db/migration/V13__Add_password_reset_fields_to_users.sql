ALTER TABLE users ADD COLUMN password_reset_code VARCHAR(6);
ALTER TABLE users ADD COLUMN password_reset_code_expiry TIMESTAMP;
ALTER TABLE users ADD COLUMN password_reset_token VARCHAR(36);
ALTER TABLE users ADD COLUMN password_reset_token_expiry TIMESTAMP;
