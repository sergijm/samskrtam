-- =============================================
-- V12: Add case_ending to session_questions
-- =============================================
-- case_ending: stores the endingIast string (e.g. "aḥ", "am", "ena")
--   from GeneratedQuizQuestionDto.caseEnding at session start.
--   Used by quiz-service for ENDING_MATCH / CASE_BY_FORM rendering.

ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS case_ending VARCHAR(50);

COMMENT ON COLUMN quiz.session_questions.case_ending IS
    'Case ending IAST string (e.g. aḥ, am, ena). Copied from content.case_endings.ending_iast at session start. Used for ENDING_MATCH and CASE_BY_FORM display.';
