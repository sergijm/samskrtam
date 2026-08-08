-- Materialized quest items for all quest types (grammar + lexicon).
-- Generic table serving the 4 DECLENSION_FORM-family quest types (and future
-- types) with a uniform structure; type-specific data lives in payload/
-- distractors (jsonb). See curriculum-quest-items.md §1.

-- ----------------------------------------------------------------------------
-- QuestItem
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.quest_item (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id          UUID NOT NULL REFERENCES curriculum.topic (id) ON DELETE CASCADE,
    item_type         VARCHAR(40) NOT NULL,
    answer_mode       VARCHAR(20) NOT NULL,
    prompt            TEXT NOT NULL,
    correct_answer    TEXT NULL,
    distractors       JSONB NOT NULL DEFAULT '[]'::jsonb,
    payload           JSONB NOT NULL,
    generator_source  VARCHAR(60) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_quest_item_answer_mode CHECK (answer_mode IN ('FREE_TEXT','SINGLE_CHOICE','MULTI_SELECT','SPAN_SELECT','MATCHING'))
);

COMMENT ON TABLE curriculum.quest_item IS 'Materialized quest items for all quest types (grammar+lexicon), see curriculum-quest-items.md §1.';

CREATE INDEX idx_quest_item_topic_type ON curriculum.quest_item (topic_id, item_type);
CREATE INDEX idx_quest_item_type ON curriculum.quest_item (item_type);

-- ----------------------------------------------------------------------------
-- Generation idempotency key: prevent batch-generator from re-creating the
-- same quest item for the same (topic, itemType, lexeme, case, number).
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.quest_item_generation_key (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quest_item_id   UUID NOT NULL REFERENCES curriculum.quest_item (id) ON DELETE CASCADE,
    generation_key  VARCHAR(200) NOT NULL,
    CONSTRAINT uq_quest_item_generation_key UNIQUE (generation_key)
);