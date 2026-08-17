-- verse_statistics.grammar_info (sangraha-service.md §9): jsonb cgo distinct-массивами
-- грамматического набора стиха — все части речи (pos), формы части речи (form_type),
-- числа (number_type), падежи (case_type), роды (gender), встречающиеся в стихе.
-- Пересчитывается на POST /sangraha/internal/lexicon/lemmas/refresh-statistics
-- (LemmaRefreshService → VerseStatisticsRepository.refreshStatistics).
BEGIN;

ALTER TABLE "sangraha"."verse_statistics"
    ADD COLUMN "grammar_info" jsonb NOT NULL DEFAULT '{}'::jsonb;

-- Первичное наполнение — поисковый контур (см. refreshStatistics) работает сразу
-- после миграции.  Строки soft-удалённых стихов (deleted_at) не учитываются;
-- NULL-значения грамматических полей никуда не попадают (jsonb_agg их отбрасывает).
UPDATE "sangraha"."verse_statistics" vs
SET "grammar_info" = gi."grammar_info",
    "updated_at" = now()
FROM (
    SELECT
        v."id" AS verse_id,
        jsonb_build_object(
            'pos',        COALESCE(jsonb_agg(DISTINCT w."pos") FILTER (WHERE w."pos" IS NOT NULL), '[]'::jsonb),
            'formType',   COALESCE(jsonb_agg(DISTINCT w."form_type") FILTER (WHERE w."form_type" IS NOT NULL), '[]'::jsonb),
            'numberType', COALESCE(jsonb_agg(DISTINCT m."number_type") FILTER (WHERE m."number_type" IS NOT NULL), '[]'::jsonb),
            'caseType',   COALESCE(jsonb_agg(DISTINCT m."case_type") FILTER (WHERE m."case_type" IS NOT NULL), '[]'::jsonb),
            'gender',     COALESCE(jsonb_agg(DISTINCT m."gender") FILTER (WHERE m."gender" IS NOT NULL), '[]'::jsonb)
        ) AS grammar_info
    FROM "sangraha"."verses" v
    LEFT JOIN "sangraha"."verse_words" w ON w."verse_id" = v."id"
    LEFT JOIN "sangraha"."verse_word_morphology" m ON m."verse_word_id" = w."id"
    WHERE v."deleted_at" IS NULL
    GROUP BY v."id"
) gi
WHERE gi."verse_id" = vs."verse_id";

-- Поиск стихов по грамматике: "найти стихи, где есть ACCUSATIVE" →
-- grammar_info @> '{"caseType": ["ACCUSATIVE"]}' (и аналогично для других полей).
-- Default-класс jsonb_ops покрывает все операторы индекса нижнего уровня
-- (@>, ?, ?|, ?&) — это самый общий "правильный" GIN-индекс для таких запросов.
CREATE INDEX "idx_verse_statistics_grammar_info"
    ON "sangraha"."verse_statistics" USING gin ("grammar_info");

COMMIT;