-- V23: meaning_number on lexeme + VERSE-domain lessons + lexical_topic_binding.
-- 1. meaning_number: value identity per lexicon.md §1 — one spelling may carry
--    several meanings as separate rows; unique within a spelling by meaning number
--    (not by gender). Incremental verse batches assign max+1 (lexicon-content-pipeline.md §7).
-- 2. VERSE lessons: per-chapter lexical lesson created from a verse lemma batch
--    (domain/domain_type = VERSE).
-- 3. lexical_topic_binding re-created: it was dropped by V16 (semantic-topic
--    composition replaced it for LEXICON lessons); VERSE lessons bind lexemes
--    explicitly per batch.

ALTER TABLE curriculum.lexeme ADD COLUMN meaning_number INTEGER NOT NULL DEFAULT 1;

ALTER TABLE curriculum.lexeme DROP CONSTRAINT IF EXISTS uq_lexeme_slp1_gender;
ALTER TABLE curriculum.lexeme
    ADD CONSTRAINT uq_lexeme_slp1_meaning UNIQUE (lemma_slp1, meaning_number);

ALTER TABLE curriculum.topic DROP CONSTRAINT IF EXISTS chk_topic_domain;
ALTER TABLE curriculum.topic
    ADD CONSTRAINT chk_topic_domain CHECK (domain IN (
        'GRAMMAR', 'LEXICON', 'CONJUNCTION',
        'PHONOLOGY_SCRIPT', 'SANDHI', 'GRAMMAR_FOUNDATIONS',
        'NOMINAL_MORPHOLOGY', 'PRONOUNS', 'VERBAL_MORPHOLOGY',
        'NONFINITE_FORMS', 'NUMERALS', 'CASE_SYNTAX',
        'SYNTAX', 'WORD_FORMATION', 'ADVANCED_READING', 'VERSE'));

ALTER TABLE curriculum.topic DROP CONSTRAINT IF EXISTS chk_topic_domain_type;
ALTER TABLE curriculum.topic
    ADD CONSTRAINT chk_topic_domain_type CHECK (domain_type IN ('GRAMMAR', 'LEXICON', 'VERSE'));

CREATE TABLE curriculum.lexical_topic_binding (
    lexical_topic_id   UUID NOT NULL REFERENCES curriculum.topic (id) ON DELETE CASCADE,
    lexeme_id          UUID NOT NULL REFERENCES curriculum.lexeme (id) ON DELETE CASCADE,
    PRIMARY KEY (lexical_topic_id, lexeme_id)
);

CREATE INDEX idx_lexical_topic_binding_lexeme_id ON curriculum.lexical_topic_binding (lexeme_id);
