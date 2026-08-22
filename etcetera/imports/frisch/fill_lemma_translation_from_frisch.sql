-- ============================================================================
-- fill_lemma_translation_from_frisch.sql
--
-- Fills curriculum.lemma_translation from the Friš dictionary (frisch schema),
-- taking the 2500 most frequent lemmas according to lingua.lemma_frequency.
-- Translations come from frisch.gloss_sense (one row per sense, ru + en).
--
-- Prerequisites (same PostgreSQL database):
--   * frisch schema loaded        (see etcetera/imports/frisch/import_frisch.py)
--   * lingua.lemma_frequency populated (lemma_iast, frequency, row_num)
--   * curriculum.lemma_translation exists (migration V39)
--
-- Idempotent: existing translation rows for the chosen lemmas are removed
-- first, then re-inserted, so the script can be re-run safely.
--
-- Run with:  psql "$DATABASE_URL" -f fill_lemma_translation_from_frisch.sql
-- ============================================================================

-- Work in a single transaction so a failure leaves the table untouched.
BEGIN;

-- ----------------------------------------------------------------------------
-- 1. Rank Frisch headwords by corpus frequency, keep the top 2500.
--    One representative entry per distinct lemma_iast (the lowest entry_id),
--    so homographs / "+"-sub-entries collapse to a single lemma.
-- ----------------------------------------------------------------------------
WITH ranked AS (
    SELECT
        e.lemma_iast,
        MIN(e.entry_id) AS entry_id,
        MAX(lf.frequency) AS freq          -- most attested frequency for the lemma
    FROM frisch.dict_entry e
    JOIN lingua.lemma_frequency lf
      ON lf.lemma_iast = e.lemma_iast
    WHERE e.is_related_form = FALSE         -- main headwords only
    GROUP BY e.lemma_iast
),
top_lemmas AS (
    SELECT lemma_iast, entry_id
    FROM ranked
    ORDER BY freq DESC NULLS LAST
    LIMIT 2500
),

-- ----------------------------------------------------------------------------
-- 2. Clear any previously generated rows for those lemmas (cascades to
--    lemma_semantic_class via ON DELETE CASCADE).
-- ----------------------------------------------------------------------------
clear_old AS (
    DELETE FROM curriculum.lemma_translation lt
    WHERE lt.lemma_iast IN (SELECT lemma_iast FROM top_lemmas)
)

-- ----------------------------------------------------------------------------
-- 3. Insert one row per gloss sense (frisch.gloss_sense) for ru/en.
--    - gloss      = sense_text (truncated to 300)
--    - gender     = first element of the sense's genders[] array (per-sense)
--    - is_main    = TRUE only for the first sense (seq = 1) within a language,
--                   satisfying the one-is_main-per-(lemma,language) partial index
-- ----------------------------------------------------------------------------
INSERT INTO curriculum.lemma_translation
    (id, lemma_iast, language, gloss, pos, gender, is_main)
SELECT
    gen_random_uuid(),
    tl.lemma_iast,
    gs.lang_code,
    LEFT(gs.sense_text, 300),                                  -- gloss VARCHAR(300)
    (SELECT p.pos::text
       FROM frisch.entry_pos p
      WHERE p.entry_id = tl.entry_id
      ORDER BY p.pos
      LIMIT 1),
    (gs.genders)[1]::text,                                     -- per-sense gender (NULL if none)
    (gs.seq = 1)                                               -- first sense is the main one
FROM top_lemmas tl
JOIN frisch.gloss_sense gs
  ON gs.entry_id = tl.entry_id
 AND gs.lang_code IN ('ru', 'en')                            -- russian + english only
ON CONFLICT ON CONSTRAINT uq_lemma_translation DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. Report what was written.
-- ----------------------------------------------------------------------------
SELECT
    COUNT(*)                                   AS total_rows,
    COUNT(*) FILTER (WHERE language = 'ru')    AS ru_rows,
    COUNT(*) FILTER (WHERE language = 'en')    AS en_rows,
    COUNT(DISTINCT lemma_iast)                 AS distinct_lemmas
FROM curriculum.lemma_translation
WHERE lemma_iast IN (
    SELECT e.lemma_iast
    FROM frisch.dict_entry e
    JOIN lingua.lemma_frequency lf ON lf.lemma_iast = e.lemma_iast
    WHERE e.is_related_form = FALSE
    GROUP BY e.lemma_iast
    ORDER BY MAX(lf.frequency) DESC NULLS LAST
    LIMIT 2500
);

COMMIT;
