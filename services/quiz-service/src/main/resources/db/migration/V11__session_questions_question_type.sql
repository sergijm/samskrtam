-- =============================================
-- V11: Add question_type column to session_questions
-- =============================================
-- See: docs/services/quiz-service/quiz-declension.md §4.5 (questionType persistence)
-- questionType is assigned once at session start and must not be re-randomized on resume.
-- NULL is treated as FORM_BY_CASE for backward compatibility.

ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS question_type VARCHAR;

COMMENT ON COLUMN quiz.session_questions.question_type IS 'Question type for rendering: FORM_BY_CASE (default/null), CASE_BY_FORM, or ENDING_MATCH. Assigned once at session generation.';
