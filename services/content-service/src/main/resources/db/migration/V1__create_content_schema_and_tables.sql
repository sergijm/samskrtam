-- V1__create_content_schema_and_tables.sql
-- Создание схемы content и всех таблиц для content-service

-- 1. Создание схемы
CREATE SCHEMA IF NOT EXISTS content;

-- 2. Создание таблицы quizzes
CREATE TABLE content.quizzes (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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
    CONSTRAINT ck_quiz_type    CHECK (quiz_type IN ('DECLENSIONS','CONJUGATIONS','VOCABULARY')),
    CONSTRAINT ck_difficulty   CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED'))
);

-- 3. Создание таблицы questions (без FK к declension_stems и question_options пока)
CREATE TABLE content.questions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id           UUID NOT NULL REFERENCES content.quizzes(id),
    text_ru           TEXT NOT NULL,
    text_en           TEXT NOT NULL,
    explanation_ru    TEXT NOT NULL,
    explanation_en    TEXT NOT NULL,
    correct_option_id UUID, -- FK будет добавлен позже
    declension_stem_id UUID, -- FK будет добавлен позже
    target_case       VARCHAR(20),
    target_number     VARCHAR(20),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ
);

-- 4. Создание таблицы question_options
CREATE TABLE content.question_options (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES content.questions(id),
    form_iast   VARCHAR(255) NOT NULL, -- Переименовано из text_ru
    form_devanagari VARCHAR(255) -- Переименовано из text_en, сделано nullable
);

-- 5. Создание таблицы vocabulary_words
CREATE TABLE content.vocabulary_words (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_id          UUID NOT NULL REFERENCES content.quizzes(id),
    word             VARCHAR(200) NOT NULL,
    word_devanagari  VARCHAR(200),
    translation_ru   VARCHAR(500) NOT NULL,
    translation_en   VARCHAR(500) NOT NULL,
    part_of_speech   VARCHAR(50),
    example          TEXT
);
CREATE INDEX idx_vocabulary_words_quiz_id ON content.vocabulary_words (quiz_id);

-- 6. Создание таблицы declension_stems
CREATE TABLE content.declension_stems (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stem_name_iast          VARCHAR(50) NOT NULL UNIQUE,
    stem_name_devanagari    VARCHAR(50),
    vowel_type              VARCHAR(20) NOT NULL,
    gender                  VARCHAR(20) NOT NULL,
    CONSTRAINT ck_vowel_type CHECK (vowel_type IN ('A_STEM', 'AA_STEM', 'I_STEM', 'II_STEM', 'U_STEM', 'UU_STEM', 'R_STEM')),
    CONSTRAINT ck_gender CHECK (gender IN ('MASCULINE', 'FEMININE', 'NEUTER'))
);

-- 7. Создание таблицы declension_forms
CREATE TABLE content.declension_forms (
    declension_stem_id      UUID NOT NULL REFERENCES content.declension_stems(id),
    case_type               VARCHAR(20) NOT NULL,
    number_type             VARCHAR(20) NOT NULL,
    form_iast               VARCHAR(50) NOT NULL,
    form_devanagari         VARCHAR(50),
    PRIMARY KEY (declension_stem_id, case_type, number_type),
    CONSTRAINT ck_case_type CHECK (case_type IN ('NOMINATIVE', 'ACCUSATIVE', 'INSTRUMENTAL', 'DATIVE', 'ABLATIVE', 'GENITIVE', 'LOCATIVE', 'VOCATIVE')),
    CONSTRAINT ck_number_type CHECK (number_type IN ('SINGULAR', 'DUAL', 'PLURAL'))
);

-- 8. Добавление оставшихся внешних ключей и ограничений для таблицы questions
ALTER TABLE content.questions
    ADD CONSTRAINT fk_declension_stem
        FOREIGN KEY (declension_stem_id) REFERENCES content.declension_stems(id),
    ADD CONSTRAINT ck_question_target_case CHECK (target_case IN ('NOMINATIVE', 'ACCUSATIVE', 'INSTRUMENTAL', 'DATIVE', 'ABLATIVE', 'GENITIVE', 'LOCATIVE', 'VOCATIVE')),
    ADD CONSTRAINT ck_question_target_number CHECK (target_number IN ('SINGULAR', 'DUAL', 'PLURAL'));

ALTER TABLE content.questions
    ADD CONSTRAINT fk_correct_option
    FOREIGN KEY (correct_option_id) REFERENCES content.question_options(id);
