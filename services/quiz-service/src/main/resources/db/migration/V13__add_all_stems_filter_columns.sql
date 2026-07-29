-- =============================================
-- V13: Add filter_vowel_types and filter_genders for ALL_STEMS scope
-- =============================================
-- See: docs/services/quiz-service/quiz-declension.md §5.3
-- filterVowelTypes: JSON array of VowelType values for ALL_STEMS scope
-- filterGenders:    JSON array of Gender values for ALL_STEMS scope
-- Both are stored as JSONB for set-equality comparisons in resume lookup.

ALTER TABLE quiz.quiz_session
    ADD COLUMN IF NOT EXISTS filter_vowel_types JSONB,
    ADD COLUMN IF NOT EXISTS filter_genders JSONB;

COMMENT ON COLUMN quiz.quiz_session.filter_vowel_types IS 'JSON array of VowelType strings for ALL_STEMS scope';
COMMENT ON COLUMN quiz.quiz_session.filter_genders IS 'JSON array of Gender strings for ALL_STEMS scope';
