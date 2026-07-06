-- =============================================
-- V4: Add stem column to session_questions
-- =============================================

ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS stem VARCHAR(255);

COMMENT ON COLUMN quiz.session_questions.stem IS 'IAST form of the declension stem, copied from content.declension_stems at session start';