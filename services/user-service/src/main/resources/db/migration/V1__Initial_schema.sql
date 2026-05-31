CREATE SCHEMA IF NOT EXISTS users;

CREATE TABLE users.users
(
    id         UUID PRIMARY KEY,
    username   VARCHAR(255) NOT NULL UNIQUE,
    email      VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    last_name  VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users.user_profiles
(
    user_id      UUID PRIMARY KEY REFERENCES users.users (id) ON DELETE CASCADE,
    avatar_url   VARCHAR(2048),
    bio          TEXT,
    date_of_birth DATE
);

-- Trigger to update 'updated_at' timestamp on any change
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
   NEW.updated_at = NOW();
   RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
BEFORE UPDATE ON users.users
FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();
