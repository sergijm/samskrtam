-- Lexicon domain: Lexeme + taxonomies + Source/occurrences + UserCollection +
-- UserLexemeProgress + LexicalTopic composition. Same schema `curriculum` as
-- Topic/TopicPrerequisite/ComplexQuiz (module inside curriculum-service, not a
-- separate service, see lexicon.md §0).

-- ----------------------------------------------------------------------------
-- Lexeme / WordForm
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.lexeme (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lemma_iast             VARCHAR(100) NOT NULL,
    lemma_devanagari       VARCHAR(100) NOT NULL,
    lemma_slp1             VARCHAR(100) NOT NULL,
    gloss_ru               VARCHAR(300) NOT NULL,
    gloss_en               VARCHAR(300) NOT NULL,
    long_definition_ru     TEXT NULL,
    long_definition_en     TEXT NULL,
    gender                 VARCHAR(20) NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_lexeme_slp1_gender UNIQUE (lemma_slp1, gender),
    CONSTRAINT chk_lexeme_gender CHECK (gender IS NULL OR gender IN ('MASCULINE','FEMININE','NEUTER','UNSPECIFIED'))
);

COMMENT ON TABLE curriculum.lexeme IS 'A dictionary lemma, not a specific word form. See lexicon.md §1.';

CREATE INDEX idx_lexeme_slp1 ON curriculum.lexeme (lemma_slp1);

CREATE TABLE curriculum.word_form (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lexeme_id              UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    form_iast              VARCHAR(100) NOT NULL,
    form_devanagari        VARCHAR(100) NOT NULL,
    grammatical_note       VARCHAR(200) NULL,
    source_occurrence_id   UUID NULL,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_word_form_lexeme_id ON curriculum.word_form (lexeme_id);

-- ----------------------------------------------------------------------------
-- Frequency
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.frequency_band (
    code        VARCHAR(20) PRIMARY KEY,
    min_rank    INTEGER NOT NULL,
    max_rank    INTEGER NOT NULL,
    label_ru    VARCHAR(60) NOT NULL,
    label_en    VARCHAR(60) NOT NULL,
    sort_order  SMALLINT NOT NULL
);

CREATE TABLE curriculum.lexeme_frequency (
    lexeme_id   UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    source      VARCHAR(50) NOT NULL,
    rank        INTEGER NOT NULL,
    PRIMARY KEY (lexeme_id, source)
);

CREATE INDEX idx_lexeme_frequency_rank ON curriculum.lexeme_frequency (source, rank);

-- ----------------------------------------------------------------------------
-- Semantic taxonomy
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.semantic_topic (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(60) NOT NULL UNIQUE,
    name_ru     VARCHAR(100) NOT NULL,
    name_en     VARCHAR(100) NOT NULL,
    parent_id   UUID NULL REFERENCES curriculum.semantic_topic (id)
);

CREATE TABLE curriculum.lexeme_semantic_topic (
    lexeme_id           UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    semantic_topic_id   UUID NOT NULL REFERENCES curriculum.semantic_topic (id) ON DELETE CASCADE,
    PRIMARY KEY (lexeme_id, semantic_topic_id)
);

CREATE INDEX idx_lexeme_semantic_topic_topic ON curriculum.lexeme_semantic_topic (semantic_topic_id);

-- ----------------------------------------------------------------------------
-- Part of speech
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.part_of_speech (
    code        VARCHAR(20) PRIMARY KEY,
    "group"     VARCHAR(20) NOT NULL,
    name_ru     VARCHAR(60) NOT NULL,
    name_en     VARCHAR(60) NOT NULL,
    CONSTRAINT chk_pos_group CHECK ("group" IN ('NOMINAL','VERBAL','INDECLINABLE'))
);

CREATE TABLE curriculum.lexeme_pos (
    lexeme_id   UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    pos_code    VARCHAR(20) NOT NULL REFERENCES curriculum.part_of_speech (code),
    PRIMARY KEY (lexeme_id, pos_code)
);

CREATE INDEX idx_lexeme_pos_code ON curriculum.lexeme_pos (pos_code);

-- ----------------------------------------------------------------------------
-- Morphology taxonomy
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.morphology_class (
    code        VARCHAR(20) PRIMARY KEY,
    applies_to  VARCHAR(10) NOT NULL,
    name_ru     VARCHAR(60) NOT NULL,
    name_en     VARCHAR(60) NOT NULL,
    CONSTRAINT chk_morphology_applies_to CHECK (applies_to IN ('NOUN','VERB'))
);

CREATE TABLE curriculum.lexeme_morphology (
    lexeme_id               UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    morphology_class_code   VARCHAR(20) NOT NULL REFERENCES curriculum.morphology_class (code),
    PRIMARY KEY (lexeme_id, morphology_class_code)
);

-- ----------------------------------------------------------------------------
-- Source / SourceOccurrence
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.source (
    id                              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                            VARCHAR(60) NOT NULL UNIQUE,
    title_ru                        VARCHAR(200) NOT NULL,
    title_en                        VARCHAR(200) NOT NULL,
    kind                            VARCHAR(20) NOT NULL,
    total_occurrences_cache         INTEGER NOT NULL DEFAULT 0,
    unique_lemma_count_cache        INTEGER NOT NULL DEFAULT 0,
    external_sangraha_work_slug     VARCHAR(100) NULL,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_source_kind CHECK (kind IN ('EPIC','PHILOSOPHICAL','FABLE','OTHER'))
);

CREATE TABLE curriculum.source_occurrence (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id           UUID NOT NULL REFERENCES curriculum.source (id) ON DELETE CASCADE,
    lexeme_id           UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    location_ref        VARCHAR(100) NOT NULL,
    surface_form_iast   VARCHAR(100) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_source_occurrence_source_id ON curriculum.source_occurrence (source_id);
CREATE INDEX idx_source_occurrence_lexeme_id ON curriculum.source_occurrence (lexeme_id);
CREATE INDEX idx_source_occurrence_location_ref ON curriculum.source_occurrence (source_id, location_ref);

ALTER TABLE curriculum.word_form
    ADD CONSTRAINT fk_word_form_source_occurrence
    FOREIGN KEY (source_occurrence_id) REFERENCES curriculum.source_occurrence (id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- UserCollection
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.user_collection (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id      UUID NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   TEXT NULL,
    visibility    VARCHAR(10) NOT NULL DEFAULT 'PRIVATE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_user_collection_visibility CHECK (visibility IN ('PRIVATE','SHARED'))
);

CREATE INDEX idx_user_collection_owner_id ON curriculum.user_collection (owner_id);

CREATE TABLE curriculum.user_collection_item (
    collection_id   UUID NOT NULL REFERENCES curriculum.user_collection (id) ON DELETE CASCADE,
    lexeme_id       UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    added_via       VARCHAR(20) NOT NULL,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (collection_id, lexeme_id),
    CONSTRAINT chk_user_collection_item_added_via CHECK (added_via IN ('MANUAL','DICTIONARY_SEARCH','TEXT_READING','QUIZ_RESULT','LEARNING_RESULT'))
);

-- ----------------------------------------------------------------------------
-- UserLexemeProgress
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.user_lexeme_progress (
    user_id           UUID NOT NULL,
    lexeme_id         UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    mastery_score     SMALLINT NOT NULL DEFAULT 0,
    exposure_count    INTEGER NOT NULL DEFAULT 0,
    correct_count     INTEGER NOT NULL DEFAULT 0,
    incorrect_count   INTEGER NOT NULL DEFAULT 0,
    last_seen_at      TIMESTAMPTZ NULL,
    next_review_at    TIMESTAMPTZ NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, lexeme_id)
);

CREATE INDEX idx_user_lexeme_progress_next_review ON curriculum.user_lexeme_progress (user_id, next_review_at);

-- ----------------------------------------------------------------------------
-- LexicalTopic binding (curriculum.topic with domain=LEXICON <-> Lexeme)
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.lexical_topic_binding (
    lexical_topic_id   UUID NOT NULL REFERENCES curriculum.topic (id) ON DELETE CASCADE,
    lexeme_id          UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    PRIMARY KEY (lexical_topic_id, lexeme_id)
);

CREATE INDEX idx_lexical_topic_binding_lexeme_id ON curriculum.lexical_topic_binding (lexeme_id);

-- ----------------------------------------------------------------------------
-- VocabularyQuizDefinition (curated Frequency/Source/Mixed-Topic quizzes)
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.vocabulary_quiz_definition (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    kind                      VARCHAR(20) NOT NULL,
    title_ru                  VARCHAR(200) NOT NULL,
    title_en                  VARCHAR(200) NOT NULL,
    topic_id                  UUID NULL REFERENCES curriculum.topic (id) ON DELETE CASCADE,
    complex_quiz_id           UUID NULL REFERENCES curriculum.complex_quiz (id) ON DELETE CASCADE,
    frequency_rank_max        INTEGER NULL,
    source_id                 UUID NULL REFERENCES curriculum.source (id) ON DELETE CASCADE,
    source_location_prefix    VARCHAR(100) NULL,
    created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_vocab_quiz_def_kind CHECK (kind IN ('TOPIC','MIXED_TOPIC','FREQUENCY_BAND','SOURCE'))
);
