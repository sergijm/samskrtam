-- ================================================================
-- V6: Добавляем target_gender в session_questions
-- По ADR-005: для уроков -i, -u, -r вопросы различаются по роду
-- (MASCULINE/FEMININE) при одинаковых case+number, поэтому gender
-- необходим для фильтрации истории ответов.
-- ================================================================

-- Добавляем колонку target_gender в таблицу session_questions
ALTER TABLE quiz.session_questions
    ADD COLUMN IF NOT EXISTS target_gender varchar(50) COLLATE "pg_catalog"."default";

-- Обновляем индекс для поиска по gender
DROP INDEX IF EXISTS idx_session_questions_gender;
CREATE INDEX idx_session_questions_gender ON quiz.session_questions (
    session_id, target_gender, target_case, target_number
);