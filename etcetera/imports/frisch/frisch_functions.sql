-- ============================================================================
-- Lookup functions for the frisch dictionary schema (enum-based)
-- Apply AFTER frisch_schema.sql (and after the loader has populated data).
-- ============================================================================
SET search_path TO frisch, public;

-- ----------------------------------------------------------------------------
-- Helper: normalize an IAST lemma for accent-insensitive matching.
-- NFKD-decompose, drop Unicode combining diacritical marks (U+0300-U+036F),
-- lower-case. Mirrors strip_accents_lower() in import_frisch.py, so it
-- matches dict_entry.lemma_ascii exactly. Requires PostgreSQL 13+ (normalize()).
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION frisch.normalize_lemma(p_text TEXT)
RETURNS TEXT AS $$
    SELECT lower(
        regexp_replace(normalize(trim(p_text), NFKD), '[\u0300-\u036f]', '', 'g')
    );
$$ LANGUAGE sql IMMUTABLE STRICT PARALLEL SAFE;

-- ----------------------------------------------------------------------------
-- Helper: parse one ';'-delimited gloss segment into (genders, number_note,
-- is_proper_noun, remaining text). Recognizes leading markers such as
-- "m.", "f.", "n.", "m. pl.", "n. pr.", "m., n. pr."
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION frisch._parse_gloss_segment(p_seg TEXT)
RETURNS TABLE (genders frisch.gender[], number_note frisch.number_type, is_proper_noun BOOLEAN, sense_text TEXT)
LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE
    seg     TEXT := btrim(p_seg);
    mtc     TEXT[];
    letters TEXT[];
    result_genders frisch.gender[] := ARRAY[]::frisch.gender[];
    letter  TEXT;
BEGIN
    IF seg = '' THEN
        RETURN;
    END IF;

    mtc := regexp_match(
        seg,
        '^((?:[mfn]\.,?\s*)+)((?:pl|du|sg)\.\s*)?((?:pr)\.\s*)?(.*)$'
    );

    IF mtc IS NULL OR mtc[1] IS NULL THEN
        genders := NULL;
        number_note := NULL;
        is_proper_noun := FALSE;
        sense_text := seg;
        RETURN NEXT;
        RETURN;
    END IF;

    SELECT array_agg(x[1]) INTO letters
    FROM regexp_matches(mtc[1], '[mfn]', 'g') AS x;

    FOREACH letter IN ARRAY letters LOOP
        result_genders := result_genders || CASE letter
            WHEN 'm' THEN 'MASCULINE'::frisch.gender
            WHEN 'f' THEN 'FEMININE'::frisch.gender
            WHEN 'n' THEN 'NEUTER'::frisch.gender
        END;
    END LOOP;
    genders := result_genders;

    number_note := CASE btrim(coalesce(mtc[2], ''), '. ')
        WHEN 'pl' THEN 'PLURAL'::frisch.number_type
        WHEN 'du' THEN 'DUAL'::frisch.number_type
        WHEN 'sg' THEN 'SINGULAR'::frisch.number_type
        ELSE NULL
    END;
    is_proper_noun := mtc[3] IS NOT NULL;
    sense_text := btrim(mtc[4]);

    RETURN NEXT;
END;
$$;

-- ----------------------------------------------------------------------------
-- Rebuild gloss_sense from the current contents of gloss. Safe to re-run,
-- e.g. after editing gloss text directly in the database.
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION frisch.rebuild_gloss_sense()
RETURNS INTEGER
LANGUAGE plpgsql AS $$
DECLARE
    n_inserted INTEGER;
BEGIN
    TRUNCATE frisch.gloss_sense RESTART IDENTITY;

    INSERT INTO frisch.gloss_sense (entry_id, lang_code, seq, genders, number_note, is_proper_noun, sense_text)
    SELECT
        g.entry_id,
        g.lang_code,
        seg.ord::SMALLINT,
        p.genders,
        p.number_note,
        p.is_proper_noun,
        p.sense_text
    FROM frisch.gloss g,
         LATERAL unnest(string_to_array(g.gloss_text, ';')) WITH ORDINALITY AS seg(text_seg, ord),
         LATERAL frisch._parse_gloss_segment(seg.text_seg) p
    WHERE btrim(seg.text_seg) <> '';

    GET DIAGNOSTICS n_inserted = ROW_COUNT;
    RETURN n_inserted;
END;
$$;

COMMENT ON FUNCTION frisch.rebuild_gloss_sense() IS
    'Repopulates frisch.gloss_sense by splitting frisch.gloss.gloss_text on '';'' '
    'and extracting gender/number/proper-noun markers from each segment. '
    'Returns the number of sense rows inserted. Not needed if the loader already '
    'populates gloss_sense directly; provided for reprocessing after manual edits.';

-- ----------------------------------------------------------------------------
-- frisch.get_lemma_info(p_lemma TEXT)
--
-- Given a lemma in IAST (accents optional, case-insensitive), returns one row
-- per matching dictionary entry (several rows if the lemma is a homonym, e.g.
-- "kar" I/II/III) with every piece of grammatical information extracted from
-- the source (POS, gender, verb class/forms, secondary stems, related/derived
-- forms, cross-references) plus glosses -- both the raw per-language text and
-- gender-split senses ("senses").
--
-- Matching order: (1) exact match on stored IAST lemma, (2) accent-insensitive
-- match via normalize_lemma().
-- ----------------------------------------------------------------------------

DROP FUNCTION IF EXISTS frisch.get_lemma_info(TEXT);

CREATE OR REPLACE FUNCTION frisch.get_lemma_info(p_lemma TEXT)
RETURNS TABLE (
    entry_id          INTEGER,
    homonym_index     SMALLINT,
    lemma_iast        TEXT,
    is_root           BOOLEAN,
    is_related_form   BOOLEAN,
    parent_entry_id   INTEGER,
    parent_lemma      TEXT,
    grammar_note      TEXT,

    pos               JSONB,   -- [{pos, qualifier}, ...]
    genders           JSONB,   -- [{gender, stem_suffix}, ...]; stem_suffix NULL = primary gender

    verb_class        SMALLINT,
    verb_forms        JSONB,   -- [{form_type, tense, mood, voice, person, number, vedic, form, raw_tag}, ...]
    derived_stems     JSONB,   -- [{derivation_type, form, raw_tag}, ...]

    related_forms     JSONB,   -- [{derivation_type, preverb, surface_form, case_government,
                                --   entry_id, lemma_iast}, ...]
    cross_references  JSONB,   -- [{kind, target_raw, target_entry_id, target_lemma}, ...]

    gloss_ru          TEXT,
    gloss_cs          TEXT,
    gloss_en          TEXT,
    senses            JSONB,   -- [{genders, number_note, is_proper_noun, cs, ru, en}, ...]

    raw_headline      TEXT
)
LANGUAGE sql STABLE AS $$
    WITH matched AS (
        SELECT e.*
        FROM frisch.dict_entry e
        WHERE e.lemma_iast = p_lemma
        UNION
        SELECT e.*
        FROM frisch.dict_entry e
        WHERE e.lemma_ascii = frisch.normalize_lemma(p_lemma)
    )
    SELECT
        e.entry_id,
        e.homonym_index,
        e.lemma_iast,
        e.is_root,
        e.is_related_form,
        e.parent_entry_id,
        parent.lemma_iast AS parent_lemma,
        e.grammar_note,

        (SELECT jsonb_agg(jsonb_build_object(
                    'pos', ep.pos,
                    'qualifier', NULLIF(ep.qualifier, '')
                ))
         FROM frisch.entry_pos ep
         WHERE ep.entry_id = e.entry_id)                              AS pos,

        (SELECT jsonb_agg(jsonb_build_object(
                    'gender', eg.gender,
                    'stem_suffix', eg.stem_suffix
                ))
         FROM frisch.entry_gender eg
         WHERE eg.entry_id = e.entry_id)                              AS genders,

        vc.conj_class                                                  AS verb_class,

        (SELECT jsonb_agg(jsonb_build_object(
                    'form_type', vf.form_type,
                    'tense', vf.tense,
                    'mood', vf.mood,
                    'voice', vf.voice,
                    'person', vf.person,
                    'number', vf.number_type,
                    'vedic', vf.is_vedic,
                    'form', vf.form_text,
                    'raw_tag', vf.raw_tag
                ) ORDER BY vf.seq)
         FROM frisch.verb_form vf
         WHERE vf.entry_id = e.entry_id)                              AS verb_forms,

        (SELECT jsonb_agg(jsonb_build_object(
                    'derivation_type', ds.derivation_type,
                    'form', ds.surface_form,
                    'raw_tag', ds.raw_tag
                ) ORDER BY ds.seq)
         FROM frisch.derived_stem ds
         WHERE ds.entry_id = e.entry_id)                              AS derived_stems,

        (SELECT jsonb_agg(jsonb_build_object(
                    'derivation_type', rf.derivation_type,
                    'preverb', rf.preverb,
                    'surface_form', rf.surface_form,
                    'case_government', to_jsonb(rf.case_government),
                    'entry_id', rf.entry_id,
                    'lemma_iast', d2.lemma_iast
                ))
         FROM frisch.related_form rf
         JOIN frisch.dict_entry d2 ON d2.entry_id = rf.entry_id
         WHERE rf.base_entry_id = e.entry_id)                         AS related_forms,

        (SELECT jsonb_agg(jsonb_build_object(
                    'kind', cr.ref_kind,
                    'target_raw', cr.target_raw,
                    'target_entry_id', cr.target_entry_id,
                    'target_lemma', d3.lemma_iast
                ))
         FROM frisch.cross_reference cr
         LEFT JOIN frisch.dict_entry d3 ON d3.entry_id = cr.target_entry_id
         WHERE cr.entry_id = e.entry_id)                              AS cross_references,

        (SELECT g.gloss_text FROM frisch.gloss g
         WHERE g.entry_id = e.entry_id AND g.lang_code = 'ru'
         ORDER BY g.seq LIMIT 1)                                      AS gloss_ru,
        (SELECT g.gloss_text FROM frisch.gloss g
         WHERE g.entry_id = e.entry_id AND g.lang_code = 'cs'
         ORDER BY g.seq LIMIT 1)                                      AS gloss_cs,
        (SELECT g.gloss_text FROM frisch.gloss g
         WHERE g.entry_id = e.entry_id AND g.lang_code = 'en'
         ORDER BY g.seq LIMIT 1)                                      AS gloss_en,

        (SELECT jsonb_agg(jsonb_build_object(
                    'genders', to_jsonb(coalesce(cs.genders, ru.genders, en.genders)),
                    'number_note', coalesce(cs.number_note, ru.number_note, en.number_note),
                    'is_proper_noun', coalesce(cs.is_proper_noun, ru.is_proper_noun, en.is_proper_noun, false),
                    'cs', cs.sense_text,
                    'ru', ru.sense_text,
                    'en', en.sense_text
                ) ORDER BY coalesce(cs.seq, ru.seq, en.seq))
         FROM (SELECT * FROM frisch.gloss_sense WHERE entry_id = e.entry_id AND lang_code = 'cs') cs
         FULL JOIN (SELECT * FROM frisch.gloss_sense WHERE entry_id = e.entry_id AND lang_code = 'ru') ru
                ON ru.seq = cs.seq
         FULL JOIN (SELECT * FROM frisch.gloss_sense WHERE entry_id = e.entry_id AND lang_code = 'en') en
                ON en.seq = coalesce(cs.seq, ru.seq))                 AS senses,

        e.raw_headline
    FROM matched e
    LEFT JOIN frisch.verb_class vc     ON vc.entry_id = e.entry_id
    LEFT JOIN frisch.dict_entry parent ON parent.entry_id = e.parent_entry_id
    ORDER BY e.homonym_index NULLS FIRST, e.entry_id;
$$;

COMMENT ON FUNCTION frisch.get_lemma_info(TEXT) IS
    'Returns Russian/Czech/English glosses (whole-text and gender-split) plus '
    'full grammatical info (POS, gender, verb class/forms, secondary stems, '
    'related/derived forms, cross-references) for every dictionary entry '
    'matching the given IAST lemma. Several rows are returned for homonyms.';

-- ----------------------------------------------------------------------------
-- Convenience wrapper: single JSONB array, one element per matching entry.
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION frisch.get_lemma_json(p_lemma TEXT)
RETURNS JSONB
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(jsonb_agg(to_jsonb(t)), '[]'::jsonb)
    FROM frisch.get_lemma_info(p_lemma) AS t;
$$;

COMMENT ON FUNCTION frisch.get_lemma_json(TEXT) IS
    'Same as get_lemma_info(), collapsed into a single JSONB array (one object per homonym).';

-- ----------------------------------------------------------------------------
-- Usage examples:
--
--   SELECT * FROM frisch.get_lemma_info('kar');
--   SELECT * FROM frisch.get_lemma_info('kṛ');   -- exact IAST match
--   SELECT * FROM frisch.get_lemma_info('kr');    -- accent-insensitive fallback
--
--   SELECT jsonb_pretty(frisch.get_lemma_json('deva'));
-- ============================================================================
