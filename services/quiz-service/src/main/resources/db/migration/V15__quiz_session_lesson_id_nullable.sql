-- =============================================
-- V15: Make quiz_session.lesson_id nullable
-- =============================================
-- Composed sessions (POST /api/v2/quiz/compose) have lessonId == null
-- because they are assembled from questions across all topics/badges,
-- not tied to a single lesson. The lesson_id column must allow NULL.

ALTER TABLE quiz.quiz_session
    ALTER COLUMN lesson_id DROP NOT NULL;

COMMENT ON COLUMN quiz.quiz_session.lesson_id IS 'Lesson the session belongs to. Null for composed sessions created via /api/v2/quiz/compose.';