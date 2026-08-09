BEGIN;

-- Standalone-стихи для страницы /analysis (verse.chapter_id = null, не привязаны
-- к произведению/главе). Стихи корпуса не меняются.

-- 1. chapter_id становится nullable — standalone-стихи не привязаны к главе
ALTER TABLE "sangraha"."verses" ALTER COLUMN "chapter_id" DROP NOT NULL;

-- 2. Владелец standalone-стиха (у стихов корпуса — null)
ALTER TABLE "sangraha"."verses" ADD COLUMN IF NOT EXISTS "owner_id" uuid;

CREATE INDEX IF NOT EXISTS "idx_verses_owner_id" ON "sangraha"."verses" ("owner_id");
CREATE INDEX IF NOT EXISTS "idx_verses_standalone_owner"
    ON "sangraha"."verses" ("owner_id", "created_at")
    WHERE "chapter_id" IS NULL;

-- 3. Корпусные агрегации (леммы/статистика/экспорт) считают только стихи корпуса —
-- standalone-стихи пользователей не должны попадать в лемматический словарь.
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
        JOIN sangraha.verses v ON v.id = vw.verse_id AND v.chapter_id IS NOT NULL AND v.deleted_at IS NULL
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

COMMIT;