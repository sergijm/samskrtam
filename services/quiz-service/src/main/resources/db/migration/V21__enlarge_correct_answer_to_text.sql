-- =============================================
-- V21: Enlarge correct_answer to TEXT
-- =============================================
-- In VOCABULARY_WORD items the English gloss may exceed 200 characters
-- (e.g. multi-word definitions). Changing to TEXT removes the limit.

ALTER TABLE quiz.session_questions
    ALTER COLUMN correct_answer TYPE TEXT;

COMMENT ON COLUMN quiz.session_questions.correct_answer IS
    'Canonical answer text for curriculum questions; NULL for MATCHING. Enlarged to TEXT in V21.';