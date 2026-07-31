DROP TABLE IF EXISTS "sangraha"."verse_word_derivation";
DROP TABLE IF EXISTS "sangraha"."verse_word_morphology";
DROP TABLE IF EXISTS "sangraha"."verse_words";
DROP TABLE IF EXISTS "sangraha"."verse_analyses";
DROP TABLE IF EXISTS "sangraha"."verses";
DROP TABLE IF EXISTS "sangraha"."chapters";
DROP TABLE IF EXISTS "sangraha"."works";

-- works
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
    CONSTRAINT "ck_work_slug" CHECK (slug::text ~ '^[a-z0-9][a-z0-9-]*$'::text)
);

-- chapters
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
    CONSTRAINT "ck_chapter_slug" CHECK (slug::text ~ '^[a-z0-9][a-z0-9-]*$'::text),
    CONSTRAINT "chapters_work_id_fkey" FOREIGN KEY ("work_id") REFERENCES "sangraha"."works" ("id")
);

-- verses
CREATE TABLE "sangraha"."verses" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "chapter_id" uuid NOT NULL,
    "order_index" int4 NOT NULL,
    "raw_text" text,
    "text_devanagari" text,
    "text_iast" text,
    "status" varchar(20) NOT NULL DEFAULT 'DRAFT',
    "vocabulary_quiz_slug" varchar(255),
    "vocabulary_quiz_id" uuid,
    "created_at" timestamptz(6) NOT NULL DEFAULT now(),
    "updated_at" timestamptz(6),
    "deleted_at" timestamptz(6),
    CONSTRAINT "pk_verses" PRIMARY KEY ("id"),
    CONSTRAINT "ck_verse_status" CHECK (
        status::text = ANY (ARRAY['DRAFT', 'ANALYZING', 'ANALYZED', 'FAILED'])
    ),
    CONSTRAINT "verses_chapter_id_fkey" FOREIGN KEY ("chapter_id")
        REFERENCES "sangraha"."chapters" ("id")
);

-- verse_analyses (1:1 with verses)
CREATE TABLE "sangraha"."verse_analyses" (
    "verse_id" uuid NOT NULL,
    "translation_ru" text NOT NULL,
    "translation_en" text NOT NULL,
    "sandhi_splits" jsonb NOT NULL,
    "raw_model_response" jsonb,
    "model_name" varchar(100) NOT NULL,
    "analyzed_at" timestamptz(6) NOT NULL DEFAULT now(),
    CONSTRAINT "pk_verse_analyses" PRIMARY KEY ("verse_id"),
    CONSTRAINT "verse_analyses_verse_id_fkey" FOREIGN KEY ("verse_id")
        REFERENCES "sangraha"."verses" ("id")
);

-- verse_words (relational, NOT jsonb)
CREATE TABLE "sangraha"."verse_words" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "verse_id" uuid NOT NULL,
    "position" int4 NOT NULL,
    -- surface form
    "surface_iast" varchar(200) NOT NULL,
    "surface_devanagari" varchar(200) NOT NULL,
    -- lexical analysis
    "lemma_iast" varchar(200) NOT NULL,
    "stem" varchar(200),
    "root" varchar(200),
    "pos" varchar(30),
    -- form type
    "form_type" varchar(30),
    "is_finite" boolean,
    -- dictionary meaning of lemma
    "lemma_gloss_ru" varchar(500),
    "lemma_gloss_en" varchar(500),
    -- contextual meaning of surface form
    "context_gloss_ru" varchar(500) NOT NULL,
    "context_gloss_en" varchar(500) NOT NULL,
    -- internal morphophonemic rules
    "formation_rule_numbers" text,
    -- analysis confidence
    "analysis_confidence" varchar(10),
    "ambiguity_notes" text,
    -- vocabulary link
    "vocabulary_word_id" uuid,
    CONSTRAINT "pk_verse_words" PRIMARY KEY ("id"),
    CONSTRAINT "fk_verse_words_verse" FOREIGN KEY ("verse_id")
        REFERENCES "sangraha"."verses" ("id") ON DELETE CASCADE
);
CREATE INDEX "idx_verse_words_verse_id" ON "sangraha"."verse_words" ("verse_id");
CREATE INDEX "idx_verse_words_vocabulary_word_id" ON "sangraha"."verse_words" ("vocabulary_word_id");

-- verse_word_morphology (1:1 with verse_words)
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
    CONSTRAINT "fk_morphology_word" FOREIGN KEY ("verse_word_id")
        REFERENCES "sangraha"."verse_words" ("id") ON DELETE CASCADE
);

-- verse_word_derivation (1:1 with verse_words)
CREATE TABLE "sangraha"."verse_word_derivation" (
    "verse_word_id" uuid NOT NULL,
    "derivation_type" varchar(30),
    "derivational_suffix" varchar(100),
    "derivational_base" varchar(200),
    "description" text,
    CONSTRAINT "pk_verse_word_derivation" PRIMARY KEY ("verse_word_id"),
    CONSTRAINT "fk_derivation_word" FOREIGN KEY ("verse_word_id")
        REFERENCES "sangraha"."verse_words" ("id") ON DELETE CASCADE
);
