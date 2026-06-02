-- V2__create_quizzes_table.sql
CREATE TABLE content.quizzes (
    id                  UUID PRIMARY KEY,
    slug                VARCHAR(255) NOT NULL UNIQUE,
    title_ru            VARCHAR(255) NOT NULL,
    title_en            VARCHAR(255) NOT NULL,
    quiz_type           VARCHAR(50)  NOT NULL,
    difficulty          VARCHAR(50)  NOT NULL,
    questions_per_session INT        NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at          TIMESTAMP WITH TIME ZONE,

    CONSTRAINT ck_quiz_type CHECK (quiz_type IN ('DECLENSIONS', 'CONJUGATIONS', 'VOCABULARY')),
    CONSTRAINT ck_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD'))
);

CREATE INDEX idx_quizzes_slug ON content.quizzes (slug);
CREATE INDEX idx_quizzes_quiz_type ON content.quizzes (quiz_type);
