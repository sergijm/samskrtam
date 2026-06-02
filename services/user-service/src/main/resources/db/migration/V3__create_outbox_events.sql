-- V3__create_outbox_events.sql
CREATE TABLE users.outbox_events (
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
    CONSTRAINT ck_event_type    CHECK (event_type IN (
        'USER_REGISTERED', 'PROFILE_UPDATED', 'USER_BLOCKED', 'USER_UNBLOCKED'
    )),
    CONSTRAINT ck_status        CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_outbox_pending ON users.outbox_events (status, created_at)
    WHERE status = 'PENDING';
