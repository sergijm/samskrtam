CREATE SCHEMA IF NOT EXISTS statistics;

CREATE TABLE statistics.user_quiz_session_statistics (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    quiz_type VARCHAR(50) NOT NULL,
    total_sessions INT NOT NULL,
    total_questions_answered INT NOT NULL,
    total_correct_answers INT NOT NULL,
    total_score INT NOT NULL,
    average_score DOUBLE PRECISION NOT NULL,
    last_completed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    UNIQUE (user_id, quiz_id)
);

CREATE INDEX idx_user_quiz_session_statistics_user_id ON statistics.user_quiz_session_statistics (user_id);
