-- =============================================
-- V6: Create unified quiz_item_score table (ADR-007)
-- =============================================
-- Replaces quiz.word_score and quiz.grammar_form_score
-- Single table for all item types (VOCABULARY_WORD, DECLENSION_FORM, etc.)
-- No physical FK to content-service tables (per ADR-007 §2.2)
-- 
-- See: docs/adr.md (ADR-007), docs/quizzes/quiz-generator-spec.md §2.2
-- =============================================

CREATE TABLE IF NOT EXISTS quiz.quiz_item_score (
    id                UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID        NOT NULL,
    item_type         VARCHAR(50) NOT NULL,   -- VOCABULARY_WORD, DECLENSION_FORM, etc.
    external_ref_id   UUID        NOT NULL,   -- references content-service entity (no FK)
    score             INT         NOT NULL DEFAULT 0 CHECK (score >= 0 AND score <= 100),
    stability         INT         NOT NULL DEFAULT 1 CHECK (stability >= 1),
    last_answered_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_mistake_at   TIMESTAMPTZ,            -- NULL if no mistakes yet
    consecutive_mistakes INT     NOT NULL DEFAULT 0 CHECK (consecutive_mistakes >= 0),
    next_review_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_quiz_item_score PRIMARY KEY (id),
    CONSTRAINT uq_quiz_item_score UNIQUE (user_id, item_type, external_ref_id)
);

COMMENT ON TABLE quiz.quiz_item_score IS
    'Единая таблица прогресса для всех типов квизов. Абстракция QuizItem = (itemType, externalRefId). '
    'Нет строки = NEW (score не хранится, статус вычисляется лениво из score). '
    'Физические FK на content-service отсутствуют — целостность эвентуальная (ADR-007 §2.2).';

COMMENT ON COLUMN quiz.quiz_item_score.item_type IS
    'VOCABULARY_WORD — content.vocabulary_words.id; '
    'DECLENSION_FORM — content.case_endings.id (прогресс общий для всех основ с одинаковым vowel_type+gender+case_type+number_type)';
COMMENT ON COLUMN quiz.quiz_item_score.external_ref_id IS
    'UUID сущности в content-service. Конкретный смысл зависит от item_type';
COMMENT ON COLUMN quiz.quiz_item_score.score IS
    'Текущее значение 0-100. Расчёт по формуле §2.5 quiz-generator-spec.md';
COMMENT ON COLUMN quiz.quiz_item_score.stability IS
    'Устойчивость к ошибке. Растёт при успехах, падает при ошибках. Влияет на penalty при error';
COMMENT ON COLUMN quiz.quiz_item_score.consecutive_mistakes IS
    'Счётчик последовательных ошибок. Сбрасывается при правильном ответе. При >= consecutiveMistakesThreshold stability сбрасывается в 1';
COMMENT ON COLUMN quiz.quiz_item_score.next_review_at IS
    'Время следующего показа. Временная заглушка — фиксированный интервал. Полноценная SRS-формула — открытый вопрос §6';

-- Index for efficient lookup by (user_id, item_type, external_ref_id)
CREATE INDEX IF NOT EXISTS idx_quiz_item_score_user_item_ref
    ON quiz.quiz_item_score (user_id, item_type, external_ref_id);

-- Index for finding due items
CREATE INDEX IF NOT EXISTS idx_quiz_item_score_next_review
    ON quiz.quiz_item_score (user_id, item_type, next_review_at);

-- Index for filtering by score range (bucket queries)
CREATE INDEX IF NOT EXISTS idx_quiz_item_score_score
    ON quiz.quiz_item_score (user_id, item_type, score);