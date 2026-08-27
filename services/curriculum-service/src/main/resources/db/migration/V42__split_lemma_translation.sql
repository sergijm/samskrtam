-- V42: Split lemma_translation into lemma + lemma_translation
--
-- Moves lemma-level properties (pos, gender, freq_order) into a new
-- curriculum.lemma table. The lemma_translation table keeps only the
-- per-language gloss. The lemma_semantic_class join table now references
-- lemma.id instead of lemma_translation.id.

-- 1. Create lemma table
CREATE TABLE curriculum.lemma (
    id         UUID PRIMARY KEY,
    lemma_iast VARCHAR(120) NOT NULL,
    pos        VARCHAR(40),
    gender     VARCHAR(20),
    freq_order INTEGER,
    CONSTRAINT uq_lemma UNIQUE (lemma_iast)
);

COMMENT ON TABLE curriculum.lemma IS
    'Lemma entries (headword spellings) with POS, gender, and frequency metadata.';

-- 2. Populate from existing lemma_translation (one row per distinct lemma_iast)
INSERT INTO curriculum.lemma (id, lemma_iast, pos, gender, freq_order)
SELECT
    gen_random_uuid(),
    lemma_iast,
    MAX(pos) FILTER (WHERE pos IS NOT NULL),
    MAX(gender::text)::VARCHAR(20) FILTER (WHERE gender IS NOT NULL),
    MIN(freq_order)
FROM curriculum.lemma_translation
GROUP BY lemma_iast;

-- 3. Add lemma_id to lemma_translation
ALTER TABLE curriculum.lemma_translation ADD COLUMN lemma_id UUID;

UPDATE curriculum.lemma_translation lt
SET lemma_id = l.id
FROM curriculum.lemma l
WHERE l.lemma_iast = lt.lemma_iast;

ALTER TABLE curriculum.lemma_translation ALTER COLUMN lemma_id SET NOT NULL;

ALTER TABLE curriculum.lemma_translation
    ADD CONSTRAINT fk_lemma_translation_lemma
    FOREIGN KEY (lemma_id) REFERENCES curriculum.lemma (id);

-- 4. Drop old constraints and columns from lemma_translation
DROP INDEX IF EXISTS curriculum.ix_lemma_translation_lemma;
DROP INDEX IF EXISTS curriculum.uq_lemma_translation_main;
ALTER TABLE curriculum.lemma_translation DROP CONSTRAINT uq_lemma_translation;

ALTER TABLE curriculum.lemma_translation DROP COLUMN lemma_iast;
ALTER TABLE curriculum.lemma_translation DROP COLUMN pos;
ALTER TABLE curriculum.lemma_translation DROP COLUMN gender;
ALTER TABLE curriculum.lemma_translation DROP COLUMN freq_order;

-- 5. New constraints on lemma_translation
ALTER TABLE curriculum.lemma_translation
    ADD CONSTRAINT uq_lemma_translation UNIQUE (lemma_id, language, gloss);

CREATE INDEX ix_lemma_translation_lemma ON curriculum.lemma_translation (lemma_id);

CREATE UNIQUE INDEX uq_lemma_translation_main
    ON curriculum.lemma_translation (lemma_id, language)
    WHERE is_main;

-- 6. Rebuild lemma_semantic_class to reference lemma.id instead of lemma_translation.id
ALTER TABLE curriculum.lemma_semantic_class
    DROP CONSTRAINT lemma_semantic_class_lemma_translation_id_fkey;

ALTER TABLE curriculum.lemma_semantic_class
    DROP CONSTRAINT lemma_semantic_class_pkey;

ALTER TABLE curriculum.lemma_semantic_class
    RENAME COLUMN lemma_translation_id TO lemma_id;

ALTER TABLE curriculum.lemma_semantic_class
    ADD PRIMARY KEY (lemma_id, semantic_class_id);

ALTER TABLE curriculum.lemma_semantic_class
    ADD CONSTRAINT fk_lemma_semantic_class_lemma
    FOREIGN KEY (lemma_id) REFERENCES curriculum.lemma (id) ON DELETE CASCADE;