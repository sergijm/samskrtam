-- V39: lemma_translation — normalized per-language glosses for lexicon entries.
--
-- Vocabulary quizzes currently read a single denormalized gloss (gloss_ru/gloss_en)
-- straight off the lexeme row. This table extracts translations into their own
-- relation so a lemma can carry several glosses across languages, with exactly
-- one flagged is_main per (lemma_iast, language). The vocabulary-quiz generator
-- will read the learner's language from here instead of the lexeme columns.

CREATE TABLE curriculum.lemma_translation (
    id           UUID PRIMARY KEY,
    lemma_iast   VARCHAR(120) NOT NULL,
    language     VARCHAR(10)  NOT NULL,
    gloss        VARCHAR(300) NOT NULL,
    pos          VARCHAR(40),
    gender       VARCHAR(20),
    is_main      BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_lemma_translation UNIQUE (lemma_iast, language, gloss)
);

COMMENT ON TABLE curriculum.lemma_translation IS
    'Per-language glosses for lexicon lemmas; one is_main per (lemma_iast, language).';

CREATE INDEX ix_lemma_translation_lemma ON curriculum.lemma_translation (lemma_iast);

-- At most one main translation within a given language for a lemma.
CREATE UNIQUE INDEX uq_lemma_translation_main
    ON curriculum.lemma_translation (lemma_iast, language)
    WHERE is_main;

-- lemma_semantic_class — semantic-class bindings per translation row
-- (so a single lemma spelling can carry different classes per gloss/language).
CREATE TABLE curriculum.lemma_semantic_class (
    lemma_translation_id UUID NOT NULL REFERENCES curriculum.lemma_translation (id) ON DELETE CASCADE,
    semantic_class_id     UUID NOT NULL REFERENCES curriculum.semantic_class (id) ON DELETE CASCADE,
    PRIMARY KEY (lemma_translation_id, semantic_class_id)
);

COMMENT ON TABLE curriculum.lemma_semantic_class IS
    'Semantic-class bindings per lemma_translation row.';

CREATE INDEX idx_lemma_semantic_class_class
    ON curriculum.lemma_semantic_class (semantic_class_id);
