-- Расчёт статистики по корпусу для lemma_statistics (lemma-classification.md §1.3).
-- Лемма определяется JOIN'ом verse_words.lemma_iast = lemma.lemma_iast (lemma_id в
-- verse_words не проставляется). Возвращает одну строку на (lemma_id, gender):
--   occurrence_count  — число слов вхождений группы,
--   dominant_pos_code — мода POS внутри группы (при равенстве частот —
--                       алфавитно меньшее значение, как в старом Java-коде).
-- Выделенная лемма (lemma, gender) с нулевым gender — слова без морфологии.
-- Оконная функция ROW_NUMBER() ранжирует pos внутри (lemma, gender).
-- Аргумент p_lemma_ids (uuid[]): если NULL — статистика для ВСЕХ лемм, иначе
-- только для перечисленных. Мёртвая зона намеренно не трогается: функция
-- только считает, upsert'ит/делетит вызывающий.
BEGIN;

CREATE OR REPLACE FUNCTION sangraha.compute_lemma_statistics(p_lemma_ids uuid[] DEFAULT NULL)
RETURNS TABLE (
    lemma_id          uuid,
    gender            varchar(20),
    occurrence_count  bigint,
    dominant_pos_code varchar(30)
)
LANGUAGE sql
AS $$
    WITH per_lemma_gender_pos AS (
        SELECT l.id        AS lemma_id,
               m.gender    AS gender,
               COALESCE(vw.pos, 'OTHER') AS pos,
               COUNT(*)    AS cnt
        FROM sangraha.verse_words vw
        JOIN sangraha.lemma l ON l.lemma_iast = vw.lemma_iast
        LEFT JOIN sangraha.verse_word_morphology m ON m.verse_word_id = vw.id
        WHERE p_lemma_ids IS NULL OR l.id = ANY (p_lemma_ids)
        GROUP BY l.id, m.gender, COALESCE(vw.pos, 'OTHER')
    ),
    ranked AS (
        SELECT lemma_id,
               gender,
               pos,
               cnt,
               ROW_NUMBER() OVER (
                   PARTITION BY lemma_id, gender
                   ORDER BY cnt DESC, pos ASC
               ) AS rn
        FROM per_lemma_gender_pos
    ),
    totals AS (
        SELECT lemma_id,
               gender,
               SUM(cnt) AS occurrence_count
        FROM per_lemma_gender_pos
        GROUP BY lemma_id, gender
    )
    SELECT t.lemma_id,
           t.gender,
           t.occurrence_count,
           r.pos AS dominant_pos_code
    FROM totals t
    JOIN ranked r
      ON r.lemma_id = t.lemma_id
     AND r.gender IS NOT DISTINCT FROM t.gender
     AND r.rn = 1;
$$;

COMMENT ON FUNCTION sangraha.compute_lemma_statistics(uuid[]) IS
    'Статистика (lemma, gender) по корпусу: occurrence_count и мода POS (оконные функции). '
    'p_lemma_ids = NULL — все леммы, иначе только указанные.';

COMMIT;