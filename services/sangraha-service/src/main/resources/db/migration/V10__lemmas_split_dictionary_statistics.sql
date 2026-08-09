-- Лемма становится словарём (уникальна по lemma_slp1); статистика по
-- (lemma, gender) уходит в отдельную таблицу lemma_statistics
-- (lemma-classification.md §1.1–§1.3, решение от 2026-08-09).
-- verse_words.lemma_id — остаётся FK на lemma; текстовая копия lemma_iast в
-- verse_words сохраняется (внешние скрипты пишут текст напрямую).
BEGIN;

-- 0. Снять зависимости от удаляемой таблицы и очистить ссылки: старые
--    lemma.id исчезнут вместе с таблицей, а FK нельзя восстановить, пока
--    verse_words.lemma_id / lemma_classification.lemma_id указывают на
--    несуществующие строки. lemma_id проставляется заново процессом refresh.
ALTER TABLE "sangraha"."verse_words"
    DROP CONSTRAINT IF EXISTS "fk_verse_words_lemma";

UPDATE "sangraha"."verse_words" SET "lemma_id" = NULL;

-- Классификации физически не могут ссылаться на удалённые леммы — перезапуск
-- batch-классификации перегенерирует их заново после refresh.
ALTER TABLE "sangraha"."lemma_classification"
    DROP CONSTRAINT IF EXISTS "fk_lemma_classification_lemma";
DELETE FROM "sangraha"."lemma_classification";

-- 1. lemma: словарь, уникальна по lemma_slp1.
DROP TABLE IF EXISTS "sangraha"."lemma" CASCADE;

CREATE TABLE "sangraha"."lemma" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "lemma_slp1" varchar(100) NOT NULL,
    "lemma_iast" varchar(100) NOT NULL,
    "lemma_devanagari" varchar(100) NOT NULL,
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
    CONSTRAINT "pk_lemma" PRIMARY KEY ("id"),
    CONSTRAINT "uq_lemma_slp1" UNIQUE ("lemma_slp1")
);

CREATE INDEX "idx_lemma_slp1" ON "sangraha"."lemma" ("lemma_slp1");

-- Verse_words.lemma_id: FK восстанавливается со ссылками уже на пустой словарь.
ALTER TABLE "sangraha"."verse_words"
    ADD CONSTRAINT "fk_verse_words_lemma" FOREIGN KEY ("lemma_id")
        REFERENCES "sangraha"."lemma" ("id");

CREATE INDEX IF NOT EXISTS "idx_verse_words_lemma_id" ON "sangraha"."verse_words" ("lemma_id");

-- 2. lemma_statistics: одна строка на (lemma, gender).
CREATE TABLE "sangraha"."lemma_statistics" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "lemma_id" uuid NOT NULL,
    "gender" varchar(20),
    "occurrence_count" int4 NOT NULL,
    "dominant_pos_code" varchar(30),
    "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
    CONSTRAINT "pk_lemma_statistics" PRIMARY KEY ("id"),
    CONSTRAINT "uq_lemma_statistics_lemma_gender" UNIQUE ("lemma_id", "gender"),
    CONSTRAINT "fk_lemma_statistics_lemma" FOREIGN KEY ("lemma_id")
        REFERENCES "sangraha"."lemma" ("id") ON DELETE CASCADE
);

CREATE INDEX "idx_lemma_statistics_lemma_id" ON "sangraha"."lemma_statistics" ("lemma_id");

-- 3. lemma_classification: добавляется gender; уникальность с учётом gender.
ALTER TABLE "sangraha"."lemma_classification"
    DROP CONSTRAINT IF EXISTS "uq_lemma_classification_lemma_scheme";

ALTER TABLE "sangraha"."lemma_classification"
    ADD COLUMN "gender" varchar(20);

ALTER TABLE "sangraha"."lemma_classification"
    ADD CONSTRAINT "uq_lemma_classification_lemma_gender_scheme"
        UNIQUE ("lemma_id", "gender", "scheme_code");

ALTER TABLE "sangraha"."lemma_classification"
    ADD CONSTRAINT "fk_lemma_classification_lemma" FOREIGN KEY ("lemma_id")
        REFERENCES "sangraha"."lemma" ("id") ON DELETE CASCADE;

COMMIT;