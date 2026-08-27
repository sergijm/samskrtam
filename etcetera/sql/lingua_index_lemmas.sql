/*
 * lingua_index_lemmas.sql
 *
 * Fills lingua.lemmas (the unified fuzzy-search index) from the
 * currently-present dictionaries:
 *   - cologne_apte.entries   (headword key in SLP1: k1_slp1)
 *   - cologne_frisch.dict_entry (headword key in IAST: lemma_iast)
 *   - cologne_mw.entries    (headword keys in SLP1: key1/key2;
 *                            part of speech taken from the grammar JSONB
 *                            column, grammar->>'partOfSpeech')
 *
 * Normalized search key
 * ----------------------
 * search_key is computed identically for the query and every dictionary so
 * cross-dictionary matching works:
 *
 *     search_key = lower(regexp_replace(lingua.normalize_lemma(<src>),
 *                                       '[^a-zA-Z]', '', 'g'))
 *
 *   * lingua.normalize_lemma -> NFKD + strip combining marks + lowercase
 *     (turns IAST "rāma" into "rama", leaves SLP1 "rAma" as "rAma")
 *   * regexp_replace '[^a-zA-Z]' -> '' strips SLP1 stress markers (^, _)
 *     and any punctuation, so Apte/MW and Frisch end up on the SAME key.
 *
 * IMPORTANT (Java side): the LemmaSearcher query key must match this rule,
 * i.e. compute  lower(slp1RemoveStress(normalizeToSlp1(query))).
 * (ApteService's existing exact lookup stays as-is — it compares cased SLP1
 *  against k1_slp1 and is a different, exact path.)
 *
 * Idempotent: re-running refreshes rows via ON CONFLICT
 * (dictionary_code, external_entry_id) DO UPDATE.
 */

-- ============================================================
-- 1. Apte  (cologne_apte.entries)
-- ============================================================
INSERT INTO lingua.lemmas
    (dictionary_code, external_entry_id, k1_slp1, k2_original, headword_display, search_key, pos)
SELECT
    'apte',
    e.id,
    e.k1_slp1,
    e.k2_original,
    COALESCE(e.headword_devanagari, e.k2_original, e.k1_slp1),
    lower(regexp_replace(lingua.normalize_lemma(e.k1_slp1), '[^a-zA-Z]', '', 'g')),
    NULL
FROM cologne_apte.entries e
ON CONFLICT (dictionary_code, external_entry_id) DO UPDATE SET
    k1_slp1         = EXCLUDED.k1_slp1,
    k2_original     = EXCLUDED.k2_original,
    headword_display = EXCLUDED.headword_display,
    search_key      = EXCLUDED.search_key;

-- ============================================================
-- 2. Frisch  (cologne_frisch.dict_entry)
-- ============================================================
INSERT INTO lingua.lemmas
    (dictionary_code, external_entry_id, k1_slp1, k2_original, headword_display, search_key, pos)
SELECT
    'frisch',
    d.entry_id,
    NULL,                       -- no SLP1 column in Frisch; key derived below
    d.lemma_iast,
    d.lemma_iast,
    lower(regexp_replace(lingua.normalize_lemma(d.lemma_iast), '[^a-zA-Z]', '', 'g')),
    NULL
FROM cologne_frisch.dict_entry d
ON CONFLICT (dictionary_code, external_entry_id) DO UPDATE SET
    k2_original     = EXCLUDED.k2_original,
    headword_display = EXCLUDED.headword_display,
    search_key      = EXCLUDED.search_key;

-- ============================================================
-- 3. Monier-Williams  (cologne_mw.entries)
-- ============================================================
-- MW headwords are stored in SLP1 (key1/key2). The grammar JSONB column
-- carries the part of speech under grammar->>'partOfSpeech' (e.g. "VERB"),
-- which is projected straight into the unified pos column. Entries without
-- a parsed grammar (or non-verbal entries) get NULL pos, matching the
-- Apte/Frisch behaviour.
INSERT INTO lingua.lemmas
    (dictionary_code, external_entry_id, k1_slp1, k2_original, headword_display, search_key, pos)
SELECT
    'mw',
    e.id,
    e.key1,
    e.key2,
    COALESCE(e.key2, e.key1),
    lower(regexp_replace(lingua.normalize_lemma(e.key1), '[^a-zA-Z]', '', 'g')),
    e.grammar->>'partOfSpeech'
FROM cologne_mw.entries e
ON CONFLICT (dictionary_code, external_entry_id) DO UPDATE SET
    k1_slp1         = EXCLUDED.k1_slp1,
    k2_original     = EXCLUDED.k2_original,
    headword_display = EXCLUDED.headword_display,
    search_key      = EXCLUDED.search_key,
    pos             = EXCLUDED.pos;

-- ============================================================
-- Result check
-- ============================================================
-- SELECT dictionary_code, count(*) FROM lingua.lemmas GROUP BY dictionary_code;
