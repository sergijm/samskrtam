-- =============================================
-- V14: Add curriculum-driven columns to session_questions
-- =============================================
-- Supports sessions composed from curriculum.quest_item (universal question engine):
-- prompt + correctAnswer + distractors/payload are materialized by curriculum-service
-- and fixed at session start (identical on resume).
--
-- New columns (all NULL for legacy content-based questions):
--   answer_mode     how the answer is checked (FREE_TEXT | SINGLE_CHOICE | MATCHING)
--   correct_answer  canonical/correct answer text (NULL for MATCHING)
--   options         JSONB array of {id, text} rendered options (with deterministic ids)
--   payload         JSONB pass-through of the curriculum item payload
--   topic_code      curriculum.topic.code the question belongs to

ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS answer_mode VARCHAR(20),
    ADD COLUMN IF NOT EXISTS correct_answer VARCHAR(200),
    ADD COLUMN IF NOT EXISTS options JSONB,
    ADD COLUMN IF NOT EXISTS payload JSONB,
    ADD COLUMN IF NOT EXISTS topic_code VARCHAR(100);

COMMENT ON COLUMN quiz.session_questions.answer_mode IS
    'Curriculum answer mode: FREE_TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE, MATCHING. NULL for legacy content-based questions.';
COMMENT ON COLUMN quiz.session_questions.correct_answer IS
    'Canonical answer text for curriculum questions; NULL for MATCHING.';
COMMENT ON COLUMN quiz.session_questions.options IS
    'Rendered option list as JSONB array of {id, text} with the correct option included; fixed at session start.';
COMMENT ON COLUMN quiz.session_questions.payload IS
    'Materialized curriculum quest_item payload, passed through unparsed.';
COMMENT ON COLUMN quiz.session_questions.topic_code IS
    'curriculum.topic.code the question belongs to; NULL for legacy lesson-based questions.';