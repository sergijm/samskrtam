-- curriculum-service — independent schema, no FK/dependency on content/quiz schemas.
-- See docs/services/curriculum-service.md §2 for field-by-field rationale.

CREATE SCHEMA IF NOT EXISTS curriculum;

CREATE TABLE curriculum.topic (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code                  VARCHAR(80) NOT NULL,
    title_ru              VARCHAR(200) NOT NULL,
    title_en              VARCHAR(200) NOT NULL,
    learning_level        VARCHAR(2) NOT NULL,
    is_evergreen          BOOLEAN NOT NULL DEFAULT false,
    display_order         SMALLINT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_topic_code UNIQUE (code),
    CONSTRAINT chk_topic_learning_level CHECK (learning_level IN ('L0','L1','L2','L3','L4','L5','L6'))
);

COMMENT ON TABLE curriculum.topic IS 'Curriculum topic ("урок") — structure only, no content/quizzes. See curriculum-service.md.';
COMMENT ON COLUMN curriculum.topic.learning_level IS 'Authored first-introduction level L0..L6. Independent from the prerequisite DAG (see curriculum-service.md §2/§6) — not derived, not a computed graph layer.';
COMMENT ON COLUMN curriculum.topic.is_evergreen IS 'True for topics outside the layered graph (Mixed review, Error correction) — always available.';

CREATE TABLE curriculum.topic_prerequisite (
    topic_id               UUID NOT NULL,
    prerequisite_topic_id  UUID NOT NULL,
    strength                VARCHAR(20) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (topic_id, prerequisite_topic_id),
    CONSTRAINT fk_topic_prerequisite_topic
        FOREIGN KEY (topic_id) REFERENCES curriculum.topic (id) ON DELETE CASCADE,
    CONSTRAINT fk_topic_prerequisite_prerequisite_topic
        FOREIGN KEY (prerequisite_topic_id) REFERENCES curriculum.topic (id) ON DELETE CASCADE,
    CONSTRAINT chk_topic_prerequisite_no_self_loop
        CHECK (topic_id <> prerequisite_topic_id),
    CONSTRAINT chk_topic_prerequisite_strength
        CHECK (strength IN ('RECOMMENDED', 'HELPFUL'))
);

COMMENT ON TABLE curriculum.topic_prerequisite IS 'Soft (non-blocking) dependency edges between topics. Direction: prerequisite_topic_id -> topic_id.';

-- Reverse-lookup index: "what depends on this topic" (used by cascade-aware admin UI, not required by API v2 itself).
CREATE INDEX idx_topic_prerequisite_prerequisite_topic_id
    ON curriculum.topic_prerequisite (prerequisite_topic_id);

CREATE INDEX idx_topic_is_evergreen
    ON curriculum.topic (is_evergreen);

CREATE INDEX idx_topic_learning_level
    ON curriculum.topic (learning_level);

-- ----------------------------------------------------------------------------
-- ComplexQuiz — Mixed Practice (2-4 topics) / Level Assessment (5-7 topics).
-- Composition only: which Topic take part and how many. No actual questions
-- are stored here — see curriculum-service.md §4/§8.
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.complex_quiz (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type                  VARCHAR(20) NOT NULL,
    learning_level        VARCHAR(2) NOT NULL,
    title_ru              VARCHAR(200) NOT NULL,
    title_en              VARCHAR(200) NOT NULL,
    question_count_hint   SMALLINT NULL,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_complex_quiz_type CHECK (type IN ('MIXED_PRACTICE', 'LEVEL_ASSESSMENT')),
    CONSTRAINT chk_complex_quiz_learning_level CHECK (learning_level IN ('L0','L1','L2','L3','L4','L5','L6'))
);

COMMENT ON TABLE curriculum.complex_quiz IS 'Curated multi-topic practice/assessment composition. Topic count per type (2-4 / 5-7) is validated in the service layer, not the DB.';
COMMENT ON COLUMN curriculum.complex_quiz.question_count_hint IS 'Decorative UI hint only, not a real generated question count.';

CREATE TABLE curriculum.complex_quiz_topic (
    complex_quiz_id       UUID NOT NULL,
    topic_id              UUID NOT NULL,
    PRIMARY KEY (complex_quiz_id, topic_id),
    CONSTRAINT fk_complex_quiz_topic_quiz
        FOREIGN KEY (complex_quiz_id) REFERENCES curriculum.complex_quiz (id) ON DELETE CASCADE,
    CONSTRAINT fk_complex_quiz_topic_topic
        FOREIGN KEY (topic_id) REFERENCES curriculum.topic (id) ON DELETE CASCADE
);

CREATE INDEX idx_complex_quiz_topic_topic_id
    ON curriculum.complex_quiz_topic (topic_id);

CREATE INDEX idx_complex_quiz_level_type
    ON curriculum.complex_quiz (learning_level, type);
