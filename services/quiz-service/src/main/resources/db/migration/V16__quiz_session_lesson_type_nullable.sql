-- =============================================
-- V16: Make quiz_session.lesson_type nullable
-- =============================================
-- Composed sessions (POST /api/v2/quiz/compose) have lessonType == null
-- because they are assembled from questions across all topics/badges,
-- not tied to a single lesson. The lesson_type column must allow NULL.

ALTER TABLE quiz.quiz_session
    ALTER COLUMN lesson_type DROP NOT NULL;

COMMENT ON COLUMN quiz.quiz_session.lesson_type IS 'Lesson type of the associated lesson. Null for composed sessions created via /api/v2/quiz/compose.';