-- Staging table for grammar tokens that pass 2 could not match against
-- grammar_abbreviations. Run this once against the apte_dictionary_schema.sql
-- database before running parse_grammar.py.
--
-- Workflow: run pass 2 -> inspect unmatched_tokens grouped by token,
-- add missing rows to grammar_abbreviations -> mark resolved -> re-run
-- pass 2 for affected entries only.

CREATE TABLE unmatched_tokens (
    id            BIGSERIAL PRIMARY KEY,
    entry_id      BIGINT NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
    token_raw     TEXT NOT NULL,
    token_normalized TEXT NOT NULL,
    context_span  TEXT,          -- surrounding text for a human to judge
    resolved      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_unmatched_tokens_norm ON unmatched_tokens (token_normalized);
CREATE INDEX idx_unmatched_tokens_resolved ON unmatched_tokens (resolved);
