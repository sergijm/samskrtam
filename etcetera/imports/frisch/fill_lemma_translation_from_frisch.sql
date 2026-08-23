-- ============================================================================
-- fill_lemma_translation_from_frisch.sql
--
-- Fills curriculum.lemma_translation from the Friš dictionary (frisch schema),
-- taking the 2500 most frequent lemmas according to lingua.lemma_frequency.
-- Translations come from frisch.gloss_sense (one row per sense, ru + en).
--
-- One row per (lemma_iast, pos, language) — only the first sense (seq = 1).
-- freq_order is sequential (no gaps) and is the same for both ru and en
-- rows of the same lemma+pos.
--
-- Prerequisites (same PostgreSQL database):
--   * frisch schema loaded        (see etcetera/imports/frisch/import_frisch.py)
--   * lingua.lemma_frequency populated (lemma_iast, pos, occurrence_count)
--   * curriculum.lemma_translation exists (with freq_order column)
--
-- Idempotent: existing translation rows for the chosen lemmas are removed
-- first, then re-inserted, so the script can be re-run safely.
--
-- Run with:  psql "$DATABASE_URL" -f fill_lemma_translation_from_frisch.sql
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. Rank all lemma+pos pairs by corpus frequency, pick one frisch entry per pair.
-- ----------------------------------------------------------------------------
WITH candidate AS (
    SELECT
        lf.lemma_iast,
        lf.pos,
        MAX(lf.occurrence_count) AS freq,
        MIN(e.entry_id)          AS entry_id
    FROM lingua.lemma_frequency lf
    JOIN frisch.dict_entry e
      ON e.lemma_iast = lf.lemma_iast
    JOIN frisch.entry_pos ep
      ON ep.entry_id = e.entry_id
     AND ep.pos::text = lf.pos
    WHERE e.is_related_form = FALSE
    GROUP BY lf.lemma_iast, lf.pos
),

-- ----------------------------------------------------------------------------
-- 2. Keep only the top 2500 by frequency.
-- ----------------------------------------------------------------------------
top_candidate AS (
    SELECT lemma_iast, pos, entry_id
    FROM candidate
    ORDER BY freq DESC
    LIMIT 2500
),

-- ----------------------------------------------------------------------------
-- 3. Assign sequential freq_order only to candidates that actually have
--    a gloss_sense with seq = 1 (i.e. will be inserted).
-- ----------------------------------------------------------------------------
has_gloss AS (
    SELECT DISTINCT tc.lemma_iast, tc.pos, tc.entry_id
    FROM top_candidate tc
    JOIN frisch.gloss_sense gs
      ON gs.entry_id = tc.entry_id
     AND gs.lang_code IN ('ru', 'en')
     AND gs.seq = 1
),
final AS (
    SELECT
        hg.lemma_iast,
        hg.pos,
        hg.entry_id,
        ROW_NUMBER() OVER (ORDER BY c.freq DESC) AS freq_order
    FROM has_gloss hg
    JOIN candidate c USING (lemma_iast, pos)
),

-- ----------------------------------------------------------------------------
-- 4. Clear any previously generated rows for those lemmas (cascades to
--    lemma_semantic_class via ON DELETE CASCADE).
-- ----------------------------------------------------------------------------
clear_old AS (
    DELETE FROM curriculum.lemma_translation lt
    WHERE (lt.lemma_iast, lt.pos) IN (SELECT lemma_iast, pos FROM final)
)

-- ----------------------------------------------------------------------------
-- 5. Insert one row per (lemma, pos, language) — only the first sense.
-- ----------------------------------------------------------------------------
INSERT INTO curriculum.lemma_translation
    (id, lemma_iast, language, gloss, pos, gender, is_main, freq_order)
SELECT
    gen_random_uuid(),
    f.lemma_iast,
    gs.lang_code,
    LEFT(gs.sense_text, 300),
    f.pos,
    (gs.genders)[1]::text,
    TRUE,
    f.freq_order
FROM final f
JOIN frisch.gloss_sense gs
  ON gs.entry_id = f.entry_id
 AND gs.lang_code IN ('ru', 'en')
 AND gs.seq = 1
ON CONFLICT ON CONSTRAINT uq_lemma_translation DO NOTHING;

-- ----------------------------------------------------------------------------
-- 6. Report what was written.
-- ----------------------------------------------------------------------------
SELECT
    COUNT(*)                                   AS total_rows,
    COUNT(*) FILTER (WHERE language = 'ru')    AS ru_rows,
    COUNT(*) FILTER (WHERE language = 'en')    AS en_rows,
    COUNT(DISTINCT (lemma_iast, pos))          AS distinct_lemma_pos
FROM curriculum.lemma_translation
WHERE (lemma_iast, pos) IN (
    SELECT lemma_iast, pos FROM final
);

COMMIT;