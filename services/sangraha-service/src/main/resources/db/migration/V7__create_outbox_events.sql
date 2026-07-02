CREATE TABLE sangraha.outbox_events (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id  UUID        NOT NULL,
    event_type    VARCHAR(50) NOT NULL,
    payload       JSONB       NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,
    retry_count   INT         NOT NULL DEFAULT 0,
    error_message TEXT,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT ck_event_type CHECK (event_type IN ('VERSE_VOCABULARY_EXTRACTED')),
    CONSTRAINT ck_status     CHECK (status IN ('PENDING','PROCESSED','FAILED'))
);

CREATE INDEX idx_outbox_pending ON sangraha.outbox_events (status, created_at)
    WHERE status = 'PENDING';