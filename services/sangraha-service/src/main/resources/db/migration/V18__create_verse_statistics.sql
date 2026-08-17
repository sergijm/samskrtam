-- verse_statistics (1:1 с verses, PK = verse_id): предвычисленная длина стиха
-- в словах (word_count) — для поиска примеров словоформ (sangraha-service.md §9),
-- чтобы не считать COUNT(verse_words) на каждый запрос. Пересчитывается на
-- POST /sangraha/internal/lexicon/lemmas/refresh-statistics (LemmaRefreshService).
BEGIN;

CREATE TABLE "sangraha"."verse_statistics" (
    "verse_id" uuid NOT NULL,
    "word_count" int4 NOT NULL,
    "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
    CONSTRAINT "pk_verse_statistics" PRIMARY KEY ("verse_id"),
    CONSTRAINT "fk_verse_statistics_verse" FOREIGN KEY ("verse_id")
        REFERENCES "sangraha"."verses" ("id") ON DELETE CASCADE
);

-- Фильтр word_count <= maxPhraseWords в findVerseWordCountsByVowelType.
CREATE INDEX "idx_verse_statistics_word_count" ON "sangraha"."verse_statistics" ("word_count");

-- Первичное наполнение — поиск §9 работает сразу после миграции, без ожидания
-- refresh-statistics. Строки soft-удалённых стихов (deleted_at) не считаются.
INSERT INTO "sangraha"."verse_statistics" ("verse_id", "word_count", "updated_at")
SELECT v."id", COUNT(w."id")::int4, now()
FROM "sangraha"."verses" v
LEFT JOIN "sangraha"."verse_words" w ON w."verse_id" = v."id"
WHERE v."deleted_at" IS NULL
GROUP BY v."id"
ON CONFLICT ("verse_id") DO UPDATE SET
    "word_count" = EXCLUDED."word_count",
    "updated_at" = now();

COMMIT;