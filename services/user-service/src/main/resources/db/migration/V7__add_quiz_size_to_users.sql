-- V7__add_quiz_size_to_users.sql
ALTER TABLE users.user_profiles
ADD COLUMN quiz_size INTEGER DEFAULT 10;
