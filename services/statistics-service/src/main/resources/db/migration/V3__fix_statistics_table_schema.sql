-- Переименовать quiz_type → lesson_type (соответствие Java-модели)
ALTER TABLE statistics.user_quiz_session_statistics
RENAME COLUMN quiz_type TO lesson_type;
-- Снять NOT NULL с last_completed_at:
-- агрегат создаётся при первом ответе, до завершения сессии поле null
ALTER TABLE statistics.user_quiz_session_statistics
ALTER COLUMN last_completed_at DROP NOT NULL;
-- Добавить индекс по (user_id, quiz_id) для быстрого upsert
-- UNIQUE constraint уже есть из V1, индекс добавляем отдельно для производительности
CREATE INDEX IF NOT EXISTS idx_statistics_user_quiz
ON statistics.user_quiz_session_statistics (user_id, quiz_id);