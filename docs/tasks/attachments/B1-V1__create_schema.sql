-- ============================================================
-- sangraha schema — единая исходная миграция (проект без прод-данных,
-- поэтому история миграций сведена к одному файлу V1).
-- ============================================================

BEGIN;

-- ----------------------------
-- Table: works
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."works" CASCADE;
CREATE TABLE "sangraha"."works" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "slug" varchar(80) NOT NULL,
    "title_ru" varchar(255) NOT NULL,
    "title_en" varchar(255) NOT NULL,
    "title_sa_iast" varchar(255),
    "title_sa_devanagari" varchar(255),
    "description_ru" varchar(1000),
    "description_en" varchar(1000),
    "author" varchar(255),
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    "deleted_at" timestamptz(6),
    CONSTRAINT "pk_works" PRIMARY KEY ("id"),
    CONSTRAINT "works_slug_key" UNIQUE ("slug"),
    CONSTRAINT "ck_work_slug" CHECK (slug ~ '^[a-z0-9][a-z0-9-]*$')
);

-- ----------------------------
-- Table: chapters
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."chapters" CASCADE;
CREATE TABLE "sangraha"."chapters" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "work_id" uuid NOT NULL,
    "slug" varchar(80) NOT NULL,
    "order_index" int4,
    "title_ru" varchar(255) NOT NULL,
    "title_en" varchar(255) NOT NULL,
    "title_sa_iast" varchar(255),
    "title_sa_devanagari" varchar(255),
    "deleted_at" timestamptz(6),
    CONSTRAINT "pk_chapters" PRIMARY KEY ("id"),
    CONSTRAINT "uq_chapter_slug" UNIQUE ("work_id", "slug"),
    CONSTRAINT "ck_chapter_slug" CHECK (slug ~ '^[a-z0-9][a-z0-9-]*$')
);

-- ----------------------------
-- Table: verses
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verses" CASCADE;
CREATE TABLE "sangraha"."verses" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "chapter_id" uuid NOT NULL,
    "order_index" int4 NOT NULL,
    "text_devanagari" text,
    "text_iast" text,
    "raw_text" varchar,
    "status" varchar(20) NOT NULL DEFAULT 'DRAFT',
    "vocabulary_quiz_slug" varchar(255),
    "vocabulary_quiz_id" uuid,
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    "updated_at" timestamptz(6),
    "deleted_at" timestamptz(6),
    CONSTRAINT "pk_verses" PRIMARY KEY ("id"),
    CONSTRAINT "ck_verse_status" CHECK (status IN ('DRAFT', 'ANALYZING', 'ANALYZED', 'FAILED'))
);

-- ----------------------------
-- Table: verse_analyses
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verse_analyses" CASCADE;
CREATE TABLE "sangraha"."verse_analyses" (
    "verse_id" uuid NOT NULL,
    "translation_ru" text NOT NULL,
    "translation_en" text NOT NULL,
    "sandhi_splits" jsonb NOT NULL,
    "raw_model_response" jsonb,
    "model_name" varchar(100) NOT NULL,
    "analyzed_at" timestamptz(6) NOT NULL DEFAULT now(),
    CONSTRAINT "pk_verse_analyses" PRIMARY KEY ("verse_id")
);

-- ----------------------------
-- Table: verse_words
-- ----------------------------
-- Разбор конкретной словоформы (surface form), встретившейся в стихе.
-- Морфология и деривация вынесены в отдельные таблицы 1:1
-- (verse_word_morphology, verse_word_derivation) — разграничение
-- ответственности, поля там могут отсутствовать (не для всех форм применимы).
DROP TABLE IF EXISTS "sangraha"."verse_words" CASCADE;
CREATE TABLE "sangraha"."verse_words" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "verse_id" uuid NOT NULL,
    "position" int4 NOT NULL,

    -- A. surface form
    "surface_iast" varchar(200) NOT NULL,
    "surface_devanagari" varchar(200) NOT NULL,

    -- B. lexical analysis
    "lemma_iast" varchar(200) NOT NULL,
    "stem" varchar(200),
    "root" varchar(200),
    "pos" varchar(30),

    -- C. form type
    "form_type" varchar(40),
    "is_finite" boolean,

    -- F. lemma-level dictionary gloss (переезд в vocabulary_words — в будущем)
    "lemma_gloss_ru" varchar(500),
    "lemma_gloss_en" varchar(500),

    -- G. contextual meaning of the actual surface form
    -- (бывшие gloss_ru/gloss_en — переименованы, чтобы не путать с lemma_gloss_*)
    "context_gloss_ru" varchar(500) NOT NULL,
    "context_gloss_en" varchar(500) NOT NULL,

    -- H. internal morphophonemic rules (emenau-sandhi-rules.json, rules 1-40)
    "formation_rule_numbers" TEXT,

    -- I. analysis confidence
    "analysis_confidence" varchar(10),
    "ambiguity_notes" text,

    -- ссылка на словарную статью в vocabulary-service (квизы)
    "vocabulary_word_id" uuid,

    CONSTRAINT "pk_verse_words" PRIMARY KEY ("id"),
    CONSTRAINT "ck_verse_words_confidence" CHECK (
        "analysis_confidence" IS NULL OR "analysis_confidence" IN ('HIGH', 'MEDIUM', 'LOW')
    )
);

CREATE INDEX "idx_verse_words_verse_id" ON "sangraha"."verse_words" USING btree ("verse_id");
CREATE INDEX "idx_verse_words_lemma_iast" ON "sangraha"."verse_words" USING btree ("lemma_iast");
CREATE INDEX "idx_verse_words_root" ON "sangraha"."verse_words" USING btree ("root");
CREATE INDEX "idx_verse_words_form_type" ON "sangraha"."verse_words" USING btree ("form_type");

-- ----------------------------
-- Table: verse_word_morphology (1:1 с verse_words)
-- ----------------------------
-- Инфлективная морфология конкретной словоформы. Строка создаётся, только
-- если для слова вообще есть что сохранять; отдельные поля внутри строки
-- тоже могут быть NULL (не применимы к данной части речи/форме).
DROP TABLE IF EXISTS "sangraha"."verse_word_morphology" CASCADE;
CREATE TABLE "sangraha"."verse_word_morphology" (
    "verse_word_id" uuid NOT NULL,
    "case_type" varchar(20),
    "gender" varchar(20),
    "number_type" varchar(20),
    "person" varchar(20),
    "tense" varchar(20),
    "mood" varchar(20),
    "voice" varchar(20),
    CONSTRAINT "pk_verse_word_morphology" PRIMARY KEY ("verse_word_id"),
    CONSTRAINT "fk_verse_word_morphology_word" FOREIGN KEY ("verse_word_id")
        REFERENCES "sangraha"."verse_words" ("id") ON DELETE CASCADE
);

-- ----------------------------
-- Table: verse_word_derivation (1:1 с verse_words)
-- ----------------------------
-- Деривационная/словообразовательная информация конкретной словоформы.
DROP TABLE IF EXISTS "sangraha"."verse_word_derivation" CASCADE;
CREATE TABLE "sangraha"."verse_word_derivation" (
    "verse_word_id" uuid NOT NULL,
    "derivation_type" varchar(50),
    "derivational_suffix" varchar(100),
    "derivational_base" varchar(200),
    "description" text,
    CONSTRAINT "pk_verse_word_derivation" PRIMARY KEY ("verse_word_id"),
    CONSTRAINT "fk_verse_word_derivation_word" FOREIGN KEY ("verse_word_id")
        REFERENCES "sangraha"."verse_words" ("id") ON DELETE CASCADE
);

COMMIT;
