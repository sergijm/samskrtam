-- =============================================
-- V8: Add case_ending_id and item_type to session_questions
-- =============================================
-- case_ending_id: stores the caseEndingId from GeneratedQuizQuestionDto
--   (externalRefId for DECLENSION_FORM items)
-- item_type: stores the itemType string (VOCABULARY_WORD, DECLENSION_FORM, ...)
--   so quiz-service can resolve ItemType without guessing by null-fields
-- =============================================

ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS case_ending_id UUID;

ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS item_type VARCHAR(50);

COMMENT ON COLUMN quiz.session_questions.case_ending_id IS
    'ID of content.case_endings row. Copied from GeneratedQuizQuestionDto.caseEndingId at session start. Used as externalRefId for DECLENSION_FORM progress.';

COMMENT ON COLUMN quiz.session_questions.item_type IS
    'ItemType enum name (VOCABULARY_WORD, DECLENSION_FORM, ...). Copied from GeneratedQuizQuestionDto.itemType at session start.';
