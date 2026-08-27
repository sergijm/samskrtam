/*
 * lingua_index_dcs.sql
 *
 * Populates lingua.dcs_surface_forms from the analysed corpus
 * (sangraha.verse_words).  One row per distinct (surface_form, lemma)
 * pair, with occurrence count as frequency.
 *
 * Normalization
 * -------------
 * surface_key / lemma_key are computed via lingua.normalize_lemma
 * (NFKD + strip combining + lower + regexp remove non-alpha), which
 * is the SAME rule used for lingua.lemmas.search_key.  This keeps
 * surface_key comparable with the Java‑side query key, which is
 *
 *     lower(slp1RemoveStress(normalizeToSlp1(query)))
 *
 * NOTE: IAST ś/ṣ/ṃ → normalize_lemma → s / s / m
 *       SLP1 ś/ṣ/ṃ → z / S / M
 * Words containing these phonemes will NOT bridge from DCS surface
 * forms to lemmas via the exact‑match path.  That is acceptable because
 * layers 3/4 (direct exact / direct trigram) still find those entries
 * through lingua.lemmas.  A future Java‑based populator could avoid this
 * by using TransliterationService.
 *
 * Idempotency: uses ON CONFLICT on (surface_key, lemma_key) so a
 * re‑run updates frequencies without creating duplicates.
 * Requires a unique constraint; the first block adds it if missing.
 */

-- ============================================================
-- 0. Ensure unique constraint exists (safe DO block)
-- ============================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'lingua'
          AND t.relname = 'dcs_surface_forms'
          AND c.conname = 'dcs_surface_forms_unique_key'
    ) THEN
        ALTER TABLE lingua.dcs_surface_forms
            ADD CONSTRAINT dcs_surface_forms_unique_key
            UNIQUE (surface_key, lemma_key);
    END IF;
END;
$$;

-- ============================================================
-- 1. Aggregate by NORMALISED key (surface_key, lemma_key)
--    Different raw IAST pairs may collapse to the same key after
--    normalise_lemma; we keep the most frequent raw variant for
--    display and SUM their frequencies.
-- ============================================================
WITH raw AS (
    SELECT
        surface_iast,
        lemma_iast,
        lower(regexp_replace(lingua.normalize_lemma(surface_iast), '[^a-zA-Z]', '', 'g')) AS surface_key,
        lower(regexp_replace(lingua.normalize_lemma(lemma_iast), '[^a-zA-Z]', '', 'g')) AS lemma_key
    FROM sangraha.verse_words
    WHERE surface_iast IS NOT NULL
      AND lemma_iast IS NOT NULL
),
stats AS (
    SELECT
        surface_key,
        lemma_key,
        surface_iast,
        lemma_iast,
        COUNT(*)::int4 AS freq
    FROM raw
    GROUP BY surface_key, lemma_key, surface_iast, lemma_iast
),
merged AS (
    SELECT
        surface_key,
        lemma_key,
        (array_agg(surface_iast ORDER BY freq DESC, surface_iast))[1] AS surface_form,
        (array_agg(lemma_iast   ORDER BY freq DESC, lemma_iast  ))[1] AS lemma,
        SUM(freq)::int4 AS frequency
    FROM stats
    GROUP BY surface_key, lemma_key
)
INSERT INTO lingua.dcs_surface_forms
    (surface_form, surface_key, lemma, lemma_key, frequency)
SELECT
    surface_form,
    surface_key,
    lemma,
    lemma_key,
    frequency
FROM merged
ON CONFLICT (surface_key, lemma_key) DO UPDATE SET
    surface_form = EXCLUDED.surface_form,
    lemma         = EXCLUDED.lemma,
    frequency     = EXCLUDED.frequency;

-- ============================================================
-- 2. (Optional) Check
-- ============================================================
-- SELECT count(*) AS total_rows FROM lingua.dcs_surface_forms;