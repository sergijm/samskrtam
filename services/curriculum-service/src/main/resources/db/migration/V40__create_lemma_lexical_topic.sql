-- V40: lemma_lexical_topic — binds a lemma (by spelling) to a lexical/verse
-- topic, mirroring lexeme_lexical_topic but keyed by lemma_iast instead of a
-- Lexeme row id. Verse batches (lexicon-content-pipeline.md §7) and any
-- lemma_translation-driven topic membership land here; the lexical quiz
-- generator resolves candidates straight from lemma_translation + this table,
-- without going through the Lexeme entity.

CREATE TABLE curriculum.lemma_lexical_topic (
    topic_code  VARCHAR(60)  NOT NULL,
    lemma_iast  VARCHAR(120) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (topic_code, lemma_iast)
);

COMMENT ON TABLE curriculum.lemma_lexical_topic IS
    'Topic membership of a lemma spelling (verse batches, lemma_translation-driven topics).';

CREATE INDEX ix_lemma_lexical_topic_lemma
    ON curriculum.lemma_lexical_topic (lemma_iast);
