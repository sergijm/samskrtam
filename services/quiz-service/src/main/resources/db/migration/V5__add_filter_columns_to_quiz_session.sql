-- =============================================
-- V5: Add filter columns to quiz_session
-- =============================================
-- See: docs/quizzes/quiz-declension.md §3.4
-- filterScope: CASE_ONLY | CASE_NUMBER_GENDER
-- filterCaseType: always required (e.g. NOMINATIVE, ACCUSATIVE, etc.)
-- filterNumberType: required only when filterScope = CASE_NUMBER_GENDER (SINGULAR, DUAL, PLURAL)
-- filterGender: required only when filterScope = CASE_NUMBER_GENDER (MASCULINE, FEMININE, NEUTER, UNSPECIFIED)

ALTER TABLE quiz.quiz_session
    ADD COLUMN IF NOT EXISTS filter_scope VARCHAR(50),
    ADD COLUMN IF NOT EXISTS filter_case_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS filter_number_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS filter_gender VARCHAR(50);

COMMENT ON COLUMN quiz.quiz_session.filter_scope IS 'CASE_ONLY — filter by case only (from the "By Case" tab); CASE_NUMBER_GENDER — detailed filter';
COMMENT ON COLUMN quiz.quiz_session.filter_case_type IS 'Case type for filtering (always required)';
COMMENT ON COLUMN quiz.quiz_session.filter_number_type IS 'Number type filter, required only for CASE_NUMBER_GENDER';
COMMENT ON COLUMN quiz.quiz_session.filter_gender IS 'Gender filter, required only for CASE_NUMBER_GENDER; UNSPECIFIED for non-gender-differentiated stems';