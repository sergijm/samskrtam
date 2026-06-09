CREATE SCHEMA IF NOT EXISTS content;

-- =====================================================
-- ENUMS
-- =====================================================

CREATE TYPE content.part_of_speech AS ENUM (
    'NOUN',
    'ADJECTIVE',
    'PRONOUN',
    'NUMERAL',
    'VERB',
    'PARTICIPLE',
    'GERUND',
    'INFINITIVE',
    'ADVERB',
    'PARTICLE',
    'CONJUNCTION',
    'INTERJECTION',
    'PREPOSITION',
    'INDECLINABLE'
    );

CREATE TYPE content.gender_type AS ENUM (
    'MASCULINE',
    'FEMININE',
    'NEUTER',
    'COMMON',
    'UNKNOWN'
    );

CREATE TYPE content.case_type AS ENUM (
    'NOMINATIVE',
    'ACCUSATIVE',
    'INSTRUMENTAL',
    'DATIVE',
    'ABLATIVE',
    'GENITIVE',
    'LOCATIVE',
    'VOCATIVE'
    );

CREATE TYPE content.number_type AS ENUM (
    'SINGULAR',
    'DUAL',
    'PLURAL'
    );

CREATE TYPE content.pada_type AS ENUM (
    'PARASMAIPADA',
    'ATMANEPADA',
    'UBHAYAPADA'
    );

CREATE TYPE content.pronoun_type AS ENUM (
    'PERSONAL',
    'DEMONSTRATIVE',
    'RELATIVE',
    'INTERROGATIVE',
    'REFLEXIVE',
    'INDEFINITE'
    );

CREATE TYPE content.numeral_type AS ENUM (
    'CARDINAL',
    'ORDINAL',
    'MULTIPLICATIVE',
    'FRACTIONAL'
    );

CREATE TYPE content.tense_mood_type AS ENUM (
    'PRESENT',
    'IMPERFECT',
    'IMPERATIVE',
    'OPTATIVE',
    'PERFECT',
    'AORIST',
    'FUTURE',
    'CONDITIONAL',
    'BENEDICTIVE'
    );

-- =====================================================
-- PARADIGMS
-- =====================================================

CREATE TABLE content.paradigms
(
    id             UUID PRIMARY KEY                DEFAULT gen_random_uuid(),

    code           VARCHAR(100)           NOT NULL UNIQUE,

    pos            content.part_of_speech NOT NULL,

    name_ru        VARCHAR(255)           NOT NULL,
    name_en        VARCHAR(255)           NOT NULL,

    description_ru TEXT,
    description_en TEXT,

    created_at     TIMESTAMPTZ            NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- VOCABULARY
-- =====================================================

CREATE TABLE content.vocabulary_words
(
    id                UUID PRIMARY KEY                DEFAULT gen_random_uuid(),

    lemma_iast        VARCHAR(255)           NOT NULL,
    lemma_devanagari  VARCHAR(255)           NOT NULL,

    pos               content.part_of_speech NOT NULL,

    translation_ru    VARCHAR(500)           NOT NULL,
    translation_en    VARCHAR(500)           NOT NULL,

    gender            content.gender_type,

    stem              VARCHAR(255),
    stem_type         VARCHAR(50),

    root              VARCHAR(255),

    paradigm_code     VARCHAR(100),

    verb_class        SMALLINT,
    gana              SMALLINT,

    pada              content.pada_type,

    pronoun_type      content.pronoun_type,

    numeral_type      content.numeral_type,

    indeclinable_type VARCHAR(50),

    is_indeclinable   BOOLEAN                NOT NULL DEFAULT FALSE,

    frequency_rank    INTEGER,

    meaning_group     VARCHAR(100),

    explanation_ru    TEXT                   NOT NULL,
    explanation_en    TEXT                   NOT NULL,

    created_at        TIMESTAMPTZ            NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMPTZ            NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_vocabulary_paradigm
        FOREIGN KEY (paradigm_code)
            REFERENCES content.paradigms (code)
);

-- =====================================================
-- DECLENSION PATTERNS
-- =====================================================

CREATE TABLE content.declension_patterns
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    paradigm_code      VARCHAR(100)        NOT NULL,

    grammatical_case   content.case_type   NOT NULL,

    grammatical_number content.number_type NOT NULL,

    ending_iast        VARCHAR(100)        NOT NULL,
    ending_devanagari  VARCHAR(100),

    CONSTRAINT fk_declension_paradigm
        FOREIGN KEY (paradigm_code)
            REFERENCES content.paradigms (code),

    CONSTRAINT uq_declension_pattern
        UNIQUE (
                paradigm_code,
                grammatical_case,
                grammatical_number
            )
);

-- =====================================================
-- CONJUGATION PATTERNS
-- =====================================================

CREATE TABLE content.conjugation_patterns
(
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    paradigm_code      VARCHAR(100)            NOT NULL,

    tense_mood         content.tense_mood_type NOT NULL,

    person             SMALLINT                NOT NULL CHECK (person BETWEEN 1 AND 3),

    grammatical_number content.number_type     NOT NULL,

    ending_iast        VARCHAR(100)            NOT NULL,
    ending_devanagari  VARCHAR(100),

    CONSTRAINT fk_conjugation_paradigm
        FOREIGN KEY (paradigm_code)
            REFERENCES content.paradigms (code),

    CONSTRAINT uq_conjugation_pattern
        UNIQUE (
                paradigm_code,
                tense_mood,
                person,
                grammatical_number
            )
);

-- =====================================================
-- GENERATED WORD FORMS
-- =====================================================

CREATE TABLE content.word_forms
(
    id                 UUID PRIMARY KEY      DEFAULT gen_random_uuid(),

    vocabulary_word_id UUID         NOT NULL,

    form_iast          VARCHAR(255) NOT NULL,
    form_devanagari    VARCHAR(255) NOT NULL,

    grammatical_case   content.case_type,
    grammatical_number content.number_type,

    tense_mood         content.tense_mood_type,

    person             SMALLINT CHECK (
        person IS NULL
            OR person BETWEEN 1 AND 3
        ),

    generated          BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at         TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_word_form_word
        FOREIGN KEY (vocabulary_word_id)
            REFERENCES content.vocabulary_words (id)
            ON DELETE CASCADE
);

-- =====================================================
-- INDEXES
-- =====================================================

CREATE INDEX idx_vocabulary_lemma_iast
    ON content.vocabulary_words (lemma_iast);

CREATE INDEX idx_vocabulary_root
    ON content.vocabulary_words (root);

CREATE INDEX idx_vocabulary_pos
    ON content.vocabulary_words (pos);

CREATE INDEX idx_word_forms_iast
    ON content.word_forms (form_iast);

CREATE INDEX idx_word_forms_devanagari
    ON content.word_forms (form_devanagari);

CREATE INDEX idx_word_forms_lookup
    ON content.word_forms (
                           grammatical_case,
                           grammatical_number
        );