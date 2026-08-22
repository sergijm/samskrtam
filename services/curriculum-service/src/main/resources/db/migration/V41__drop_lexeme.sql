-- Drop the legacy `lexeme` aggregate and all its satellite tables.
-- Lexical data now lives in `lemma_translation` (+ `lemma_semantic_class`,
-- `lemma_lexical_topic`); user progress/collections on `lexeme` are dropped by
-- decision (data reset). See task: remove lexeme table entirely.

DROP VIEW IF EXISTS curriculum.semantic_class_lexeme_counts CASCADE;

DROP TABLE IF EXISTS curriculum.word_form CASCADE;
DROP TABLE IF EXISTS curriculum.source_occurrence CASCADE;
DROP TABLE IF EXISTS curriculum.lexeme_frequency CASCADE;
DROP TABLE IF EXISTS curriculum.lexeme_semantic_topic CASCADE;
DROP TABLE IF EXISTS curriculum.lexeme_pos CASCADE;
DROP TABLE IF EXISTS curriculum.lexeme_morphology CASCADE;
DROP TABLE IF EXISTS curriculum.lexical_topic_binding CASCADE;
DROP TABLE IF EXISTS curriculum.user_collection_item CASCADE;
DROP TABLE IF EXISTS curriculum.user_lexeme_progress CASCADE;
DROP TABLE IF EXISTS curriculum.user_collection CASCADE;
DROP TABLE IF EXISTS curriculum.lexeme CASCADE;
