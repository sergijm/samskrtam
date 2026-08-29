-- =============================================
-- V19: Russian prompt variant on session_questions
-- =============================================
-- Для curriculum-driven вопросов (см. V14) храним и русский текст вопроса, чтобы
-- resume/история могли отрисовать карточку на любом языке по выбору клиента.
-- Английский `text` остаётся каноническим (верификация и фолбэк).

ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS text_ru TEXT;

COMMENT ON COLUMN quiz.session_questions.text_ru IS
    'Russian prompt text for curriculum questions; NULL for legacy content-based questions and rows composed before V19.';
