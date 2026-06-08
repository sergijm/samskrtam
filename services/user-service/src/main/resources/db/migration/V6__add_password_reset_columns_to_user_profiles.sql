ALTER TABLE users.user_profiles
    ADD COLUMN password_reset_token VARCHAR(255) UNIQUE,
    ADD COLUMN password_reset_token_expiry TIMESTAMP WITH TIME ZONE;
