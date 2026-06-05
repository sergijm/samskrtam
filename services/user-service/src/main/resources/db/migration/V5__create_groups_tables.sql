-- V5__create_groups_tables.sql
CREATE TABLE users.groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    curator_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_groups_curator FOREIGN KEY (curator_id) REFERENCES users.user_profiles(id) ON DELETE SET NULL
);

CREATE TABLE users.group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES users.groups(id) ON DELETE CASCADE,
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users.user_profiles(id) ON DELETE CASCADE,
    UNIQUE (group_id, user_id)
);

CREATE INDEX idx_groups_name ON users.groups (name);
CREATE INDEX idx_group_members_group_id ON users.group_members (group_id);
CREATE INDEX idx_group_members_user_id ON users.group_members (user_id);
