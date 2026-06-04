-- V4__update_user_roles_to_set.sql
-- Update user_profiles table to support multiple roles

-- 1. Drop the existing 'role' column from user_profiles
ALTER TABLE users.user_profiles
    DROP COLUMN role;

-- 2. Create a new join table for user roles
CREATE TABLE users.user_roles (
    user_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    CONSTRAINT fk_user_roles_user_id FOREIGN KEY (user_id) REFERENCES users.user_profiles(id) ON DELETE CASCADE,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role),
    CONSTRAINT ck_user_role_type CHECK (role IN ('STUDENT', 'ADMIN'))
);

-- 3. Migrate existing data (if any) - assuming all existing users were 'STUDENT'
-- This step is crucial if you have existing data. If not, it can be skipped or adjusted.
-- For this migration, we assume all existing users were 'STUDENT' by default.
INSERT INTO users.user_roles (user_id, role)
SELECT id, 'STUDENT' FROM users.user_profiles;

-- 4. Drop the old constraint if it still exists (might be implicitly dropped with column)
-- ALTER TABLE users.user_profiles DROP CONSTRAINT ck_role; -- Uncomment if needed
