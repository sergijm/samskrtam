CREATE TABLE quiz.grammar_form_score (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    lesson_id       UUID         NOT NULL,  -- lesson UUID (из content.lesson)
    case_type       VARCHAR(20)  NOT NULL,  -- 'NOMINATIVE', 'ACCUSATIVE', ...
    number_type     VARCHAR(20)  NOT NULL,  -- 'SINGULAR', 'DUAL', 'PLURAL'
    score           INT          NOT NULL DEFAULT 0
        CHECK (score BETWEEN 0 AND 100),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_grammar_form_score PRIMARY KEY (id),
    CONSTRAINT uq_grammar_form_score
        UNIQUE (user_id, lesson_id, case_type, number_type)
);

CREATE INDEX idx_gfs_user_lesson ON quiz.grammar_form_score (user_id, lesson_id);