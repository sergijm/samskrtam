-- refresh_verse_statistics(): UPSERT verse_statistics с person, tense, mood, voice
-- Перенос логики из VerseStatisticsRepository.java в БД-функцию.
-- tuple теперь [stem_class, gender, case_type, number_type, person, tense, mood, voice];
-- null-поля не включаются (CASE WHEN, а не FILTER).
BEGIN;

CREATE OR REPLACE FUNCTION sangraha.refresh_verse_statistics()
RETURNS int
LANGUAGE sql
AS $$
    WITH upsert AS (
        INSERT INTO sangraha.verse_statistics (verse_id, word_count, grammar_info, updated_at)
        SELECT
            v.id,
            COUNT(w.id)::int,
            jsonb_build_object(
                'pos',        COALESCE(jsonb_agg(DISTINCT w.pos) FILTER (WHERE w.pos IS NOT NULL), '[]'::jsonb),
                'formType',   COALESCE(jsonb_agg(DISTINCT w.form_type) FILTER (WHERE w.form_type IS NOT NULL), '[]'::jsonb),
                'numberType', COALESCE(jsonb_agg(DISTINCT m.number_type) FILTER (WHERE m.number_type IS NOT NULL), '[]'::jsonb),
                'caseType',   COALESCE(jsonb_agg(DISTINCT m.case_type) FILTER (WHERE m.case_type IS NOT NULL), '[]'::jsonb),
                'gender',     COALESCE(jsonb_agg(DISTINCT m.gender) FILTER (WHERE m.gender IS NOT NULL), '[]'::jsonb),
                'person',     COALESCE(jsonb_agg(DISTINCT m.person) FILTER (WHERE m.person IS NOT NULL), '[]'::jsonb),
                'tense',      COALESCE(jsonb_agg(DISTINCT m.tense) FILTER (WHERE m.tense IS NOT NULL), '[]'::jsonb),
                'mood',       COALESCE(jsonb_agg(DISTINCT m.mood) FILTER (WHERE m.mood IS NOT NULL), '[]'::jsonb),
                'voice',      COALESCE(jsonb_agg(DISTINCT m.voice) FILTER (WHERE m.voice IS NOT NULL), '[]'::jsonb),
                'tuples',     COALESCE(jsonb_agg(DISTINCT
                    CASE WHEN nl.stem_class IS NOT NULL THEN jsonb_build_array(nl.stem_class) ELSE '[]'::jsonb END
                    || CASE WHEN m.gender IS NOT NULL THEN jsonb_build_array(m.gender) ELSE '[]'::jsonb END
                    || CASE WHEN m.case_type IS NOT NULL THEN jsonb_build_array(m.case_type) ELSE '[]'::jsonb END
                    || CASE WHEN m.number_type IS NOT NULL THEN jsonb_build_array(m.number_type) ELSE '[]'::jsonb END
                    || CASE WHEN m.person IS NOT NULL THEN jsonb_build_array(m.person) ELSE '[]'::jsonb END
                    || CASE WHEN m.tense IS NOT NULL THEN jsonb_build_array(m.tense) ELSE '[]'::jsonb END
                    || CASE WHEN m.mood IS NOT NULL THEN jsonb_build_array(m.mood) ELSE '[]'::jsonb END
                    || CASE WHEN m.voice IS NOT NULL THEN jsonb_build_array(m.voice) ELSE '[]'::jsonb END
                ), '[]'::jsonb)
            ),
            now()
        FROM sangraha.verses v
        LEFT JOIN sangraha.verse_words w ON w.verse_id = v.id
        LEFT JOIN sangraha.verse_word_morphology m ON m.verse_word_id = w.id
        LEFT JOIN sangraha.nominal_lemmas nl ON nl.lemma_iast = w.lemma_iast
        WHERE v.deleted_at IS NULL
        GROUP BY v.id
        ON CONFLICT (verse_id) DO UPDATE SET
            word_count = EXCLUDED.word_count,
            grammar_info = EXCLUDED.grammar_info,
            updated_at = now()
        RETURNING 1
    )
    SELECT COUNT(*)::int FROM upsert;
$$;

COMMENT ON FUNCTION sangraha.refresh_verse_statistics() IS
    'UPSERT verse_statistics для всех стихов. Возвращает число обновлённых строк.';

COMMIT;