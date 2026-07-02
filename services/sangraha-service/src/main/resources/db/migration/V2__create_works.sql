CREATE TABLE sangraha.works (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    slug           VARCHAR(80)  UNIQUE NOT NULL,
    title_ru       VARCHAR(255) NOT NULL,
    title_en       VARCHAR(255) NOT NULL,
    description_ru VARCHAR(1000),
    description_en VARCHAR(1000),
    author         VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT pk_works PRIMARY KEY (id),
    CONSTRAINT ck_work_slug CHECK (slug ~ '^[a-z0-9][a-z0-9-]*$')
);