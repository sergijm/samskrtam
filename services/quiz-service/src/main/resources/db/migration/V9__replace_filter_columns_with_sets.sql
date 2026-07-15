-- =============================================
-- V9: Replace single filter columns with JSONB sets
-- =============================================
-- See: docs/services/quiz-service/quiz-declension.md §3.4
-- Replaces filter_case_type/filter_number_type/filter_gender with:
--   filter_case_types JSONB    — for CASE_ONLY scope (array of caseType strings)
--   filter_number_types JSONB  — for NUMBER_ONLY scope (array of numberType strings)
--   filter_combinations JSONB  — for CASE_NUMBER_GENDER scope (array of {caseType,numberType,gender})

-- 1. Add new JSONB columns
ALTER TABLE quiz.quiz_session
    ADD COLUMN IF NOT EXISTS filter_case_types JSONB,
    ADD COLUMN IF NOT EXISTS filter_number_types JSONB,
    ADD COLUMN IF NOT EXISTS filter_combinations JSONB;

COMMENT ON COLUMN quiz.quiz_session.filter_case_types IS 'JSON array of caseType strings for CASE_ONLY scope';
COMMENT ON COLUMN quiz.quiz_session.filter_number_types IS 'JSON array of numberType strings for NUMBER_ONLY scope';
COMMENT ON COLUMN quiz.quiz_session.filter_combinations IS 'JSON array of {caseType,numberType,gender} objects for CASE_NUMBER_GENDER scope';

-- 2. Migrate existing data: for CASE_ONLY → wrap filter_case_type into JSON array
UPDATE quiz.quiz_session
SET filter_case_types = ('["' || filter_case_type || '"]')::JSONB
WHERE filter_scope = 'CASE_ONLY' AND filter_case_type IS NOT NULL;

-- 3. Migrate existing data: for CASE_NUMBER_GENDER → wrap triple into JSON array
UPDATE quiz.quiz_session
SET filter_combinations = (
    '[{"caseType":"' || filter_case_type || '","numberType":"' || filter_number_type || '","gender":"' || COALESCE(filter_gender, 'UNSPECIFIED') || '"}]'
)::JSONB
WHERE filter_scope = 'CASE_NUMBER_GENDER' AND filter_case_type IS NOT NULL;

-- 4. Drop old single-value columns
ALTER TABLE quiz.quiz_session
    DROP COLUMN IF EXISTS filter_case_type,
    DROP COLUMN IF EXISTS filter_number_type,
    DROP COLUMN IF EXISTS filter_gender;
