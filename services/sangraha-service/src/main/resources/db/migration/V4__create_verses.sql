CREATE TABLE sangraha.verses (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    chapter_id      UUID NOT NULL REFERENCES sangraha.chapters(id),
    order_index     INT  NOT NULL,
    text_devanagari TEXT,
    text_iast       TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT pk_verses PRIMARY KEY (id),
    CONSTRAINT ck_verse_status CHECK (status IN ('DRAFT','ANALYZING','ANALYZED','FAILED'))
);