CREATE SCHEMA IF NOT EXISTS content;

CREATE TABLE content.quizzes (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    slug                  VARCHAR(50)  UNIQUE,
    title_ru              VARCHAR(255) NOT NULL,
    title_en              VARCHAR(255) NOT NULL,
    description_ru        VARCHAR(500),
    description_en        VARCHAR(500),
    quiz_type             VARCHAR(20)  NOT NULL,
    difficulty            VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    questions_per_session INT          NOT NULL DEFAULT 10,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ,
    CONSTRAINT pk_quizzes      PRIMARY KEY (id)
);

CREATE TABLE content.questions (
    id                UUID NOT NULL DEFAULT gen_random_uuid(),
    quiz_id           UUID NOT NULL REFERENCES content.quizzes(id),
    text_ru           TEXT NOT NULL,
    text_en           TEXT NOT NULL,
    explanation_ru    TEXT NOT NULL,
    explanation_en    TEXT NOT NULL,
    correct_option_id UUID,
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT pk_questions PRIMARY KEY (id)
);

CREATE TABLE content.question_options (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    question_id UUID         NOT NULL REFERENCES content.questions(id),
    text_ru     VARCHAR(255) NOT NULL,
    text_en     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_options PRIMARY KEY (id)
);

ALTER TABLE content.questions
    ADD CONSTRAINT fk_correct_option
    FOREIGN KEY (correct_option_id) REFERENCES content.question_options(id);

CREATE TABLE content.vocabulary_words (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    quiz_id          UUID         NOT NULL REFERENCES content.quizzes(id),
    word             VARCHAR(200) NOT NULL,
    word_devanagari  VARCHAR(200),
    translation_ru   VARCHAR(500) NOT NULL,
    translation_en   VARCHAR(500) NOT NULL,
    part_of_speech   VARCHAR(50),
    example          TEXT,
    CONSTRAINT pk_vocabulary_words PRIMARY KEY (id)
);
