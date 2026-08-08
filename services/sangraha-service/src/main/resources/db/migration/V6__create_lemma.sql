-- Модуль классификации лексем (lemma-classification.md §1).
-- Агрегат по всему корпусу: одна строка на (lemmaSlp1, gender) — тот же ключ, что у
-- curriculum.lexeme, см. §1.1. Денормализованная ссылка verse_words.lemma_id (nullable,
-- заполняется refresh-процессом, §1.3).
BEGIN;

CREATE TABLE "sangraha"."lemma" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "lemma_slp1" varchar(100) NOT NULL,
    "lemma_iast" varchar(100) NOT NULL,
    "lemma_devanagari" varchar(100) NOT NULL,
    "gender" varchar(20),
    "dominant_pos_code" varchar(30),
    "occurrence_count" int4 NOT NULL,
    "frequency_rank" int4,
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
    CONSTRAINT "pk_lemma" PRIMARY KEY ("id"),
    CONSTRAINT "uq_lemma_slp1_gender" UNIQUE ("lemma_slp1", "gender")
);

CREATE INDEX "idx_lemma_slp1" ON "sangraha"."lemma" ("lemma_slp1");
CREATE INDEX "idx_lemma_frequency_rank" ON "sangraha"."lemma" ("frequency_rank");

ALTER TABLE "sangraha"."verse_words"
    ADD COLUMN "lemma_id" uuid;

ALTER TABLE "sangraha"."verse_words"
    ADD CONSTRAINT "fk_verse_words_lemma" FOREIGN KEY ("lemma_id")
        REFERENCES "sangraha"."lemma" ("id");

CREATE INDEX "idx_verse_words_lemma_id" ON "sangraha"."verse_words" ("lemma_id");

COMMIT;