-- =============================================
-- V10: Add status_filter column to quiz_session
-- =============================================
-- See: docs/services/quiz-service/quiz-generator-spec.md §3 (statusFilter)
-- Used to find in-progress sessions for resume by status filter.

ALTER TABLE quiz.quiz_session
    ADD COLUMN IF NOT EXISTS status_filter VARCHAR(20);

COMMENT ON COLUMN quiz.quiz_session.status_filter IS 'Status filter (NEW|LEARNING|REVIEW) for bucket-based quiz sessions. Null when no status filter applied.';