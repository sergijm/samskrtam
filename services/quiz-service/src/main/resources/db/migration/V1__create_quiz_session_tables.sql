CREATE SCHEMA IF NOT EXISTS quiz;

CREATE TABLE quiz.quiz_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- Added DEFAULT gen_random_uuid()
    user_id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    quiz_type VARCHAR(50) NOT NULL,
    total_questions INT NOT NULL,
    answered_questions INT NOT NULL,
    score INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE quiz.quiz_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- Added DEFAULT gen_random_uuid()
    session_id UUID NOT NULL,
    question_id UUID NOT NULL,
    selected_option_id UUID,
    correct_form_iast VARCHAR(255) NOT NULL,
    correct BOOLEAN NOT NULL,
    response_time_ms INT NOT NULL,
    answered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (session_id) REFERENCES quiz.quiz_sessions(id)
);

CREATE TABLE quiz.session_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- Added DEFAULT gen_random_uuid()
    session_id UUID NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    quiz_type VARCHAR(50) NOT NULL,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    duration_ms BIGINT NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE quiz.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- Added DEFAULT gen_random_uuid()
    aggregate_id VARCHAR(36) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(), -- Added DEFAULT now()
    processed_at TIMESTAMP WITH TIME ZONE,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX idx_quiz_outbox_pending ON quiz.outbox_events (status, created_at) WHERE status = 'PENDING';
