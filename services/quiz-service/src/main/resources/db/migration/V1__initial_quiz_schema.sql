-- Create quiz_sessions table
CREATE TABLE quiz.quiz_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    quiz_type VARCHAR(50) NOT NULL,
    total_questions INT NOT NULL,
    answered_questions INT NOT NULL,
    score INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    vocabulary_words_json TEXT
);

CREATE INDEX idx_quiz_sessions_user_id ON quiz.quiz_sessions (user_id);
CREATE INDEX idx_quiz_sessions_quiz_id ON quiz.quiz_sessions (quiz_id);

-- Create quiz_answers table
CREATE TABLE quiz.quiz_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    session_question_id UUID NOT NULL,
    selected_option_id UUID,
    is_correct BOOLEAN NOT NULL,
    response_time_ms INT NOT NULL,
    answered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    selected_form_iast VARCHAR(255),
    correct_form_iast VARCHAR(255),
    FOREIGN KEY (session_id) REFERENCES quiz.quiz_sessions(id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_answers_session_id ON quiz.quiz_answers (session_id);
CREATE INDEX idx_quiz_answers_session_question_id ON quiz.quiz_answers (session_question_id);

-- Create session_questions table
CREATE TABLE quiz.session_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    question_id UUID,
    text TEXT NOT NULL,
    explanation_ru TEXT,
    explanation_en TEXT,
    declension_stem_id UUID,
    target_case VARCHAR(50),
    target_number VARCHAR(50),
    correct_form_iast VARCHAR(255),
    correct_form_devanagari VARCHAR(255),
    vocabulary_word_id UUID,
    question_source_language VARCHAR(50),
    question_target_language VARCHAR(50),
    correct_translation_ru VARCHAR(255),
    correct_translation_en VARCHAR(255),
    FOREIGN KEY (session_id) REFERENCES quiz.quiz_sessions(id) ON DELETE CASCADE
);

CREATE INDEX idx_session_questions_session_id ON quiz.session_questions (session_id);

-- Create outbox_events table for quiz schema
CREATE TABLE quiz.outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id VARCHAR(255) NOT NULL,
    topic VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    processed_at TIMESTAMP WITH TIME ZONE,
    retry_count INT NOT NULL DEFAULT 0,
    error_message TEXT
);

CREATE INDEX idx_outbox_events_status ON quiz.outbox_events (status);
CREATE INDEX idx_outbox_events_aggregate_id ON quiz.outbox_events (aggregate_id);