-- V15: drop lexeme.status — the DRAFT/CANDIDATE/APPROVED/REJECTED moderation
-- workflow is removed (no moderation filter anywhere in the code).

ALTER TABLE curriculum.lexeme DROP CONSTRAINT IF EXISTS chk_lexeme_status;
DROP INDEX IF EXISTS curriculum.idx_lexeme_status;
ALTER TABLE curriculum.lexeme DROP COLUMN IF EXISTS status;
