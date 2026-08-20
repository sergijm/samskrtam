-- V29: semantic taxonomy refactor (see lexical-curriculum.md §1/§3):
--   * semantic_topic          -> semantic_class      (entity SemanticClass)
--   * lexeme_semantic_topic   -> lexeme_semantic_class (lexeme <-> semantic_class)
--   * topic.semantic_topic_id (1:1, single FK) replaced by semantic_class_topic
--     (M:N topic <-> semantic_class); backfilled from topic.semantic_topic_id
--   * view semantic_topic_lexeme_counts -> semantic_class_lexeme_counts
--
-- After this migration a LEXICON topic's lexemes come from BOTH sources:
--   semantic_class_topic (classified lexemes via lexeme_semantic_class) and
--   lexeme_lexical_topic (unclassified / VERSE lessons, explicit bindings).

-- ----------------------------------------------------------------------------
-- Rename tables, columns, constraints and indexes
-- ----------------------------------------------------------------------------

ALTER TABLE curriculum.semantic_topic RENAME TO semantic_class;

ALTER INDEX curriculum.semantic_topic_code_idx RENAME TO semantic_class_code_idx;

ALTER TABLE curriculum.semantic_class RENAME CONSTRAINT semantic_topic_pkey TO semantic_class_pkey;
ALTER TABLE curriculum.semantic_class RENAME CONSTRAINT semantic_topic_code_key TO semantic_class_code_key;
ALTER TABLE curriculum.semantic_class RENAME CONSTRAINT semantic_topic_parent_id_fkey TO semantic_class_parent_id_fkey;

ALTER TABLE curriculum.lexeme_semantic_topic RENAME TO lexeme_semantic_class;

ALTER TABLE curriculum.lexeme_semantic_class RENAME COLUMN semantic_topic_id TO semantic_class_id;

ALTER INDEX curriculum.idx_lexeme_semantic_topic_topic RENAME TO idx_lexeme_semantic_class_class;

ALTER TABLE curriculum.lexeme_semantic_class RENAME CONSTRAINT lexeme_semantic_topic_pkey TO lexeme_semantic_class_pkey;
ALTER TABLE curriculum.lexeme_semantic_class RENAME CONSTRAINT lexeme_semantic_topic_lexeme_id_fkey TO lexeme_semantic_class_lexeme_id_fkey;
ALTER TABLE curriculum.lexeme_semantic_class RENAME CONSTRAINT lexeme_semantic_topic_semantic_topic_id_fkey TO lexeme_semantic_class_semantic_class_id_fkey;

-- ----------------------------------------------------------------------------
-- topic.semantic_topic_id (1:1 FK) -> semantic_class_topic (M:N)
-- ----------------------------------------------------------------------------

DROP VIEW IF EXISTS curriculum.semantic_topic_lexeme_counts;

ALTER TABLE curriculum.topic DROP CONSTRAINT IF EXISTS topic_semantic_topic_id_fkey;

CREATE TABLE curriculum.semantic_class_topic (
    topic_id           UUID NOT NULL REFERENCES curriculum.topic (id) ON DELETE CASCADE,
    semantic_class_id  UUID NOT NULL REFERENCES curriculum.semantic_class (id) ON DELETE CASCADE,
    PRIMARY KEY (topic_id, semantic_class_id)
);

CREATE INDEX idx_semantic_class_topic_class
    ON curriculum.semantic_class_topic (semantic_class_id);

-- Backfill the new M:N from the single FK column being removed.
INSERT INTO curriculum.semantic_class_topic (topic_id, semantic_class_id)
SELECT id, semantic_topic_id
FROM curriculum.topic
WHERE semantic_topic_id IS NOT NULL;

ALTER TABLE curriculum.topic DROP COLUMN semantic_topic_id;

COMMENT ON COLUMN curriculum.topic.domain IS 'GRAMMAR = original curriculum.md topics, LEXICON = lexical topics whose lexemes come from semantic_class_topic (classified, via lexeme_semantic_class) plus lexeme_lexical_topic (unclassified/VERSE explicit bindings), see lexical-curriculum.md §1.';

-- ----------------------------------------------------------------------------
-- View: semantic_class_lexeme_counts (renamed recreation of V20)
-- ----------------------------------------------------------------------------

CREATE OR REPLACE VIEW curriculum.semantic_class_lexeme_counts AS
WITH RECURSIVE tree AS (
    SELECT st.id AS root_id,
           st.id AS node_id,
           COALESCE(dc.c, 0::bigint) AS direct_count
    FROM curriculum.semantic_class st
    LEFT JOIN (
        SELECT semantic_class_id, COUNT(*) AS c
        FROM curriculum.lexeme_semantic_class
        GROUP BY semantic_class_id
    ) dc ON dc.semantic_class_id = st.id
    UNION ALL
    SELECT t.root_id,
           child.id,
           COALESCE(dc2.c, 0::bigint) AS direct_count
    FROM tree t
    JOIN curriculum.semantic_class child ON child.parent_id = t.node_id
    LEFT JOIN (
        SELECT semantic_class_id, COUNT(*) AS c
        FROM curriculum.lexeme_semantic_class
        GROUP BY semantic_class_id
    ) dc2 ON dc2.semantic_class_id = child.id
)
SELECT st.code,
       st.name_ru,
       st.name_en,
       st.parent_id,
       SUM(t.direct_count) AS lexeme_count
FROM tree t
JOIN curriculum.semantic_class st ON st.id = t.root_id
GROUP BY st.id, st.code, st.name_ru, st.name_en, st.parent_id
ORDER BY st.code;