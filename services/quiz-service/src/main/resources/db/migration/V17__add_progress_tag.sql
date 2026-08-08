-- =============================================
-- V17: Add progress_tag to session_questions, replace external_ref_id in quiz_item_score
-- =============================================
-- Progress is now keyed by progressTag (String) instead of externalRefId (UUID).
-- progressTag = caseType|numberType|gender for declensions, formIast for vocabulary.
--
-- Changes:
--   1. quiz.session_questions: ADD COLUMN progress_tag VARCHAR(255)
--   2. quiz.quiz_item_score:   RENAME external_ref_id → progress_tag, change type to VARCHAR(255)

ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS progress_tag VARCHAR(255);

COMMENT ON COLUMN quiz.session_questions.progress_tag IS
    'Progress grouping tag: caseType|numberType|gender for declensions, formIast/lemma for vocabulary. Key into quiz_item_score.';

-- =============================================
-- quiz_item_score: replace external_ref_id (UUID) with progress_tag (VARCHAR)
-- =============================================

ALTER TABLE quiz.quiz_item_score
    DROP CONSTRAINT IF EXISTS uq_quiz_item_score;

DROP INDEX IF EXISTS idx_quiz_item_score_user_item_ref;

ALTER TABLE quiz.quiz_item_score
    RENAME COLUMN external_ref_id TO progress_tag;

ALTER TABLE quiz.quiz_item_score
    ALTER COLUMN progress_tag TYPE VARCHAR(255);

ALTER TABLE quiz.quiz_item_score
    ALTER COLUMN progress_tag SET NOT NULL;

COMMENT ON COLUMN quiz.quiz_item_score.progress_tag IS
    'Progress grouping tag: caseType|numberType|gender for declensions, formIast/lemma for vocabulary. Replaces external_ref_id.';

CREATE INDEX IF NOT EXISTS idx_quiz_item_score_user_item_tag
    ON quiz.quiz_item_score (user_id, item_type, progress_tag);

ALTER TABLE quiz.quiz_item_score
    ADD CONSTRAINT uq_quiz_item_score UNIQUE (user_id, item_type, progress_tag);