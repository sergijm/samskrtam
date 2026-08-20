-- V22: add coarse top-level topic classifier domain_type (GRAMMAR | LEXICON).
-- Fine-grained grammar domains (SANDHI, NOMINAL_MORPHOLOGY, ...) all map to
-- GRAMMAR; only LEXICON maps to LEXICON.

ALTER TABLE curriculum.topic ADD COLUMN domain_type VARCHAR(16);

UPDATE curriculum.topic SET domain_type = 'LEXICON' WHERE domain IN ('LEXICON');
UPDATE curriculum.topic SET domain_type = 'GRAMMAR' WHERE domain_type IS NULL;

ALTER TABLE curriculum.topic ALTER COLUMN domain_type SET NOT NULL;

ALTER TABLE curriculum.topic
    ADD CONSTRAINT chk_topic_domain_type CHECK (domain_type IN ('GRAMMAR', 'LEXICON'));