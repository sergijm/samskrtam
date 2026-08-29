/*
 * frisch_get_lemma_json_case.sql
 *
 * Case-sensitive variant of cologne_frisch.get_lemma_json — removes the
 * `UNION ... lemma_ascii = normalize_lemma(p_lemma)` branch that made the
 * look-up case-insensitive.
 *
 * Only the first exact match on lemma_iast is used; the case-insensitive
 * fallback is dropped.
 */

CREATE OR REPLACE FUNCTION lingua.frisch_get_lemma_json_case(p_lemma text)
  RETURNS TABLE("entry_id" int4, "homonym_index" int2, "lemma_iast" text, "is_root" bool, "is_related_form" bool, "parent_entry_id" int4, "parent_lemma" text, "grammar_note" text, "pos" jsonb, "genders" jsonb, "verb_class" int2, "verb_forms" jsonb, "derived_stems" jsonb, "related_forms" jsonb, "cross_references" jsonb, "gloss_ru" text, "gloss_cs" text, "gloss_en" text, "senses" jsonb, "raw_headline" text) AS $BODY$
    WITH matched AS (
        SELECT e.*
        FROM cologne_frisch.dict_entry e
        WHERE e.lemma_iast = p_lemma
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
         FROM cologne_frisch.entry_pos ep
         WHERE ep.entry_id = e.entry_id)                              AS pos,

        (SELECT jsonb_agg(jsonb_build_object(
                    'gender', eg.gender,
                    'stem_suffix', eg.stem_suffix
                ))
         FROM cologne_frisch.entry_gender eg
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
         FROM cologne_frisch.verb_form vf
         WHERE vf.entry_id = e.entry_id)                              AS verb_forms,

        (SELECT jsonb_agg(jsonb_build_object(
                    'lemma_iast', d2.lemma_iast,
                    'stem_text', d2.stem_text,
                    'grammar_note', d2.grammar_note,
                    'pos', (SELECT jsonb_agg(ep3.pos ORDER BY ep3.qualifier)
                            FROM cologne_frisch.entry_pos ep3
                            WHERE ep3.entry_id = d2.entry_id)
                ) ORDER BY d2.stem_id, d2.derived_stem_id)
         FROM cologne_frisch.derived_stem d2
         WHERE d2.entry_id = e.entry_id)                              AS derived_stems,

        (SELECT jsonb_agg(jsonb_build_object(
                    'target_entry_id', r2.related_entry_id,
                    'relation', r2.relation_type,
                    'target_lemma', d3.lemma_iast
                ) ORDER BY r2.relation_type)
         FROM cologne_frisch.related_form r2
         JOIN cologne_frisch.dict_entry d3 ON d3.entry_id = r2.related_entry_id
         WHERE r2.entry_id = e.entry_id)                              AS related_forms,

        (SELECT jsonb_agg(jsonb_build_object(
                    'ref_entry_id', cr.target_entry_id,
                    'ref_kind', cr.ref_kind,
                    'custom_text', cr.custom_text,
                    'target_lemma', d4.lemma_iast
                ) ORDER BY cr.ref_kind)
         FROM cologne_frisch.cross_reference cr
         LEFT JOIN cologne_frisch.dict_entry d4 ON d4.entry_id = cr.target_entry_id
         WHERE cr.entry_id = e.entry_id)                              AS cross_references,

        g.gloss_ru,
        g.gloss_cs,
        g.gloss_en,
        gs.senses,
        e.raw_headline
    FROM matched e
    LEFT JOIN cologne_frisch.lemma_frequency lf ON lf.lemma_iast = e.lemma_iast
    LEFT JOIN LATERAL cologne_frisch.get_lemma_gloss_json(e.entry_id) g ON TRUE
    LEFT JOIN LATERAL cologne_frisch.get_lemma_gloss_sense_json(e.entry_id) gs ON TRUE
    LEFT JOIN cologne_frisch.derived_stem d ON d.entry_id = e.entry_id AND d.is_head = TRUE
    LEFT JOIN cologne_frisch.verb_class vc ON vc.entry_id = e.entry_id
    LEFT JOIN cologne_frisch.dict_entry parent ON parent.entry_id = e.parent_entry_id
    GROUP BY e.entry_id, e.page_no, e.lemma_iast, e.homonym_index, e.is_root, e.is_crossref_only, vc.conj_class, e.parent_entry_id, parent.lemma_iast, lf.frequency, g.gloss_ru, g.gloss_cs, g.gloss_en, gs.senses, e.raw_headline
    ORDER BY e.homonym_index NULLS FIRST, lf.frequency DESC;
$BODY$
  LANGUAGE sql STABLE STRICT
  COST 100
  ROWS 20;

-- SELECT * FROM lingua.frisch_get_lemma_json_case('nārada');