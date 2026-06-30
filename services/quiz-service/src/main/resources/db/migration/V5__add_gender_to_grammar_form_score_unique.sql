-- ================================================================
-- V5: Добавляем gender в unique constraint grammar_form_score
-- По ADR-005: для уроков -i, -u, -r прогресс считается раздельно по роду,
-- так как это разные основы/слова. Ключ агрегации (gender, caseType, numberType).
-- ================================================================

-- 1. Удаляем старый unique constraint (без gender)
ALTER TABLE quiz.grammar_form_score
    DROP CONSTRAINT IF EXISTS uq_grammar_form_score;

-- 2. Добавляем новый unique constraint с gender
ALTER TABLE quiz.grammar_form_score
    ADD CONSTRAINT uq_grammar_form_score
        UNIQUE (user_id, lesson_id, gender, case_type, number_type);

-- 3. Обновляем индекс
DROP INDEX IF EXISTS idx_gfs_user_lesson;
CREATE INDEX idx_gfs_user_lesson_gender ON quiz.grammar_form_score (user_id, lesson_id, gender);