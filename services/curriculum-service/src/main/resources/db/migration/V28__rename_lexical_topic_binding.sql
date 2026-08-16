-- V28: lexical_topic_binding -> lexeme_lexical_topic (naming symmetry with
-- lexeme_semantic_topic; entity LexemeLexicalTopic, see lexical-curriculum.md §1).

ALTER TABLE curriculum.lexical_topic_binding RENAME TO lexeme_lexical_topic;

ALTER INDEX curriculum.idx_lexical_topic_binding_lexeme_id
    RENAME TO idx_lexeme_lexical_topic_lexeme_id;

ALTER TABLE curriculum.lexeme_lexical_topic
    RENAME CONSTRAINT lexical_topic_binding_pkey TO lexeme_lexical_topic_pkey;

ALTER TABLE curriculum.lexeme_lexical_topic
    RENAME CONSTRAINT lexical_topic_binding_lexeme_id_fkey TO lexeme_lexical_topic_lexeme_id_fkey;

ALTER TABLE curriculum.lexeme_lexical_topic
    RENAME CONSTRAINT lexical_topic_binding_lexical_topic_id_fkey TO lexeme_lexical_topic_lexical_topic_id_fkey;

COMMENT ON COLUMN curriculum.topic.domain IS 'GRAMMAR = original curriculum.md topics, LEXICON = lexical topics backed by curriculum.lexeme_lexical_topic (see lexical-curriculum.md §1).';