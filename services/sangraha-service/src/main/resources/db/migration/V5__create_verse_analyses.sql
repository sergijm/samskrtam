CREATE TABLE sangraha.verse_analyses (
    verse_id            UUID NOT NULL REFERENCES sangraha.verses(id),
    translation_ru      TEXT NOT NULL,
    translation_en      TEXT NOT NULL,
    sandhi_splits       JSONB NOT NULL,
    raw_model_response  JSONB,
    model_name          VARCHAR(100) NOT NULL,
    analyzed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_verse_analyses PRIMARY KEY (verse_id)
);