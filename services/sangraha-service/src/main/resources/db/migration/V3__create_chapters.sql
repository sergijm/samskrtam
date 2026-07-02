CREATE TABLE sangraha.chapters (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    work_id     UUID        NOT NULL REFERENCES sangraha.works(id),
    slug        VARCHAR(80) NOT NULL,
    order_index INT         NOT NULL,
    title_ru    VARCHAR(255) NOT NULL,
    title_en    VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT pk_chapters PRIMARY KEY (id),
    CONSTRAINT uq_chapter_slug UNIQUE (work_id, slug),
    CONSTRAINT ck_chapter_slug CHECK (slug ~ '^[a-z0-9][a-z0-9-]*$')
);