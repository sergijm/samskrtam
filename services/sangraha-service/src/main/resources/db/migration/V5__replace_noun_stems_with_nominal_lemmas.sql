-- Замена noun_stems на nominal_lemmas (verse-word-grammar.md §1б): классификация
-- на уровне леммы — одна строка на lemma_iast вместо дублирования на каждое вхождение.
-- noun_stems НЕ удаляется — остаётся для отката/сверки.
BEGIN;

CREATE TABLE "sangraha"."nominal_lemmas" (
    "id" BIGSERIAL,
    "lemma_iast" text NOT NULL,
    "stem_iast" text,
    "stem_class" text,
    "confidence" text,
    "model" text,
    "created_at" timestamptz NOT NULL DEFAULT now(),
    "updated_at" timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT "pk_nominal_lemmas" PRIMARY KEY ("id"),
    CONSTRAINT "uq_nominal_lemmas_lemma_iast" UNIQUE ("lemma_iast"),
    CONSTRAINT "ck_nominal_lemma_confidence" CHECK (
        confidence IS NULL OR confidence IN ('HIGH', 'MEDIUM', 'LOW')
    )
);

CREATE INDEX "idx_nominal_lemmas_stem_class" ON "sangraha"."nominal_lemmas" ("stem_class");
CREATE INDEX "idx_nominal_lemmas_confidence" ON "sangraha"."nominal_lemmas" ("confidence");

-- lemma_iast становится ключом join'а при поиске примеров (sangraha-service.md §9) —
-- индекса в V1 не было, без него seq scan по verse_words.
CREATE INDEX "idx_verse_words_lemma_iast" ON "sangraha"."verse_words" ("lemma_iast");

-- Перенос данных из noun_stems: дедупликация по lemma_iast, при нескольких кандидатах
-- на одну лемму — предпочтение строке с наивысшим confidence.
INSERT INTO "sangraha"."nominal_lemmas" ("lemma_iast", "stem_iast", "stem_class", "confidence", "model")
SELECT DISTINCT ON (vw."lemma_iast")
       vw."lemma_iast",
       ns."stem_iast",
       ns."stem_class",
       ns."confidence",
       ns."model"
FROM "sangraha"."noun_stems" ns
JOIN "sangraha"."verse_words" vw ON vw."id" = ns."verse_word_id"
ORDER BY vw."lemma_iast",
         CASE WHEN ns."confidence" = 'HIGH' THEN 1
              WHEN ns."confidence" = 'MEDIUM' THEN 2
              WHEN ns."confidence" = 'LOW' THEN 3
              ELSE 4 END,
         ns."created_at" DESC,
         ns."id";

-- Консистентность: новых строк не должно быть больше, чем различных лемм в источнике
-- (иначе — сигнал дублирования).
DO $$
DECLARE
    v_source_lemmas bigint;
    v_new_rows bigint;
BEGIN
    SELECT COUNT(DISTINCT vw."lemma_iast")
    INTO v_source_lemmas
    FROM "sangraha"."noun_stems" ns
    JOIN "sangraha"."verse_words" vw ON vw."id" = ns."verse_word_id";

    SELECT COUNT(*) INTO v_new_rows FROM "sangraha"."nominal_lemmas";

    IF v_new_rows > v_source_lemmas THEN
        RAISE EXCEPTION 'nominal_lemmas rows (%) exceed distinct lemma_iast in noun_stems (%)',
            v_new_rows, v_source_lemmas;
    END IF;
END
$$;

COMMIT;
