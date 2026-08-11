-- =============================================
-- V18: Replace filter/status columns with progress_tag_set_id
-- =============================================
-- See: docs/services/quest-engine.md §2.4, docs/services/quiz-service/quiz-declension.md §3.4,
--       docs/services/quiz-service/quiz-generator-spec.md §3
-- The old filter columns (filter_scope, filter_case_types, filter_number_types,
-- filter_combinations, status_filter, filter_vowel_types, filter_genders) are replaced
-- by a single stable progress_tag_set_id (NEW/LEARNING/MASTERED/DIFFICULT +
-- SINGULAR/DUAL/PLURAL/ACC_LOC/INS_ABL/GEN_LOC/DAT_ACC), used for session resume by equality.

ALTER TABLE quiz.quiz_session
    ADD COLUMN IF NOT EXISTS progress_tag_set_id VARCHAR(50);

COMMENT ON COLUMN quiz.quiz_session.progress_tag_set_id IS 'Stable ProgressTagSet id (NEW/LEARNING/MASTERED/DIFFICULT/SINGULAR/DUAL/PLURAL/ACC_LOC/INS_ABL/GEN_LOC/DAT_ACC). Null — session over the whole lesson without a slice. Participates in IN_PROGRESS resume lookup by equality.';

-- Drop legacy filter/status columns (superseded by progress_tag_set_id).
ALTER TABLE quiz.quiz_session
    DROP COLUMN IF EXISTS filter_scope,
    DROP COLUMN IF EXISTS filter_case_types,
    DROP COLUMN IF EXISTS filter_number_types,
    DROP COLUMN IF EXISTS filter_combinations,
    DROP COLUMN IF EXISTS status_filter,
    DROP COLUMN IF EXISTS filter_vowel_types,
    DROP COLUMN IF EXISTS filter_genders;
