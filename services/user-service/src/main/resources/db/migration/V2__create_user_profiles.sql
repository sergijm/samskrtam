-- V2__create_user_profiles.sql
CREATE TABLE users.user_profiles (
    id          UUID         NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    avatar_url  VARCHAR(500),
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    blocked     BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_profiles   PRIMARY KEY (id),
    CONSTRAINT uq_username        UNIQUE (username),
    CONSTRAINT uq_email           UNIQUE (email),
    CONSTRAINT ck_role            CHECK (role IN ('STUDENT', 'ADMIN'))
);

CREATE INDEX idx_user_profiles_username ON users.user_profiles (username);
CREATE INDEX idx_user_profiles_email    ON users.user_profiles (email);
CREATE INDEX idx_user_profiles_blocked  ON users.user_profiles (blocked);
CREATE INDEX idx_user_profiles_role     ON users.user_profiles (role);
