-- V19: remove curriculum.source and curriculum.source_occurrence —
-- text-source data will later live in curriculum.topic with a dedicated domain.

-- Detach vocabulary_quiz_definition from source
ALTER TABLE curriculum.vocabulary_quiz_definition DROP CONSTRAINT IF EXISTS vocabulary_quiz_definition_source_id_fkey;
ALTER TABLE curriculum.vocabulary_quiz_definition DROP COLUMN IF EXISTS source_id;
ALTER TABLE curriculum.vocabulary_quiz_definition DROP COLUMN IF EXISTS source_location_prefix;

-- Detach word_form from source_occurrence
ALTER TABLE curriculum.word_form DROP CONSTRAINT IF EXISTS fk_word_form_source_occurrence;
ALTER TABLE curriculum.word_form DROP COLUMN IF EXISTS source_occurrence_id;

-- Drop occurrence table (cascades from lexeme_sourced_occurrence FKs already dropped above)
DROP TABLE IF EXISTS curriculum.source_occurrence;

-- Drop source table
DROP TABLE IF EXISTS curriculum.source;