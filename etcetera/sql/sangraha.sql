/*
 Navicat Premium Data Transfer

 Source Server         : mdm-dev-vm
 Source Server Type    : PostgreSQL
 Source Server Version : 170009 (170009)
 Source Host           : mdm-dev:5432
 Source Catalog        : samskrtam
 Source Schema         : sangraha

 Target Server Type    : PostgreSQL
 Target Server Version : 170009 (170009)
 File Encoding         : 65001

 Date: 02/08/2026 12:01:33
*/


-- ----------------------------
-- Sequence structure for nominal_lemmas_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "sangraha"."nominal_lemmas_id_seq";
CREATE SEQUENCE "sangraha"."nominal_lemmas_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Table structure for chapters
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."chapters";
CREATE TABLE "sangraha"."chapters" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "work_id" uuid NOT NULL,
  "slug" varchar(80) COLLATE "pg_catalog"."default" NOT NULL,
  "order_index" int4,
  "title_ru" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "title_sa_iast" varchar(255) COLLATE "pg_catalog"."default",
  "title_sa_devanagari" varchar(255) COLLATE "pg_catalog"."default",
  "deleted_at" timestamptz(6)
)
;

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."flyway_schema_history";
CREATE TABLE "sangraha"."flyway_schema_history" (
  "installed_rank" int4 NOT NULL,
  "version" varchar(50) COLLATE "pg_catalog"."default",
  "description" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "script" varchar(1000) COLLATE "pg_catalog"."default" NOT NULL,
  "checksum" int4,
  "installed_by" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "installed_on" timestamp(6) NOT NULL DEFAULT now(),
  "execution_time" int4 NOT NULL,
  "success" bool NOT NULL
)
;

-- ----------------------------
-- Table structure for nominal_lemmas
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."nominal_lemmas";
CREATE TABLE "sangraha"."nominal_lemmas" (
  "id" int8 NOT NULL DEFAULT nextval('"sangraha".nominal_lemmas_id_seq'::regclass),
  "lemma_iast" text COLLATE "pg_catalog"."default" NOT NULL,
  "stem_iast" text COLLATE "pg_catalog"."default",
  "stem_class" text COLLATE "pg_catalog"."default",
  "confidence" text COLLATE "pg_catalog"."default",
  "model" text COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for sources
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."sources";
CREATE TABLE "sangraha"."sources" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "title_ru" varchar(255) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for verse_analyses
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verse_analyses";
CREATE TABLE "sangraha"."verse_analyses" (
  "verse_id" uuid NOT NULL,
  "translation_ru" text COLLATE "pg_catalog"."default" NOT NULL,
  "translation_en" text COLLATE "pg_catalog"."default" NOT NULL,
  "sandhi_splits" jsonb NOT NULL,
  "raw_model_response" jsonb,
  "model_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "analyzed_at" timestamptz(6) NOT NULL DEFAULT now(),
  "analyzer_name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for verse_statistics
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verse_statistics";
CREATE TABLE "sangraha"."verse_statistics" (
  "verse_id" uuid NOT NULL,
  "word_count" int4 NOT NULL,
  "grammar_info" jsonb NOT NULL DEFAULT '{}'::jsonb,
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for verse_word_derivation
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verse_word_derivation";
CREATE TABLE "sangraha"."verse_word_derivation" (
  "verse_word_id" uuid NOT NULL,
  "derivation_type" varchar(30) COLLATE "pg_catalog"."default",
  "derivational_suffix" varchar(100) COLLATE "pg_catalog"."default",
  "derivational_base" varchar(200) COLLATE "pg_catalog"."default",
  "description" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for verse_word_morphology
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verse_word_morphology";
CREATE TABLE "sangraha"."verse_word_morphology" (
  "verse_word_id" uuid NOT NULL,
  "case_type" varchar(20) COLLATE "pg_catalog"."default",
  "gender" varchar(20) COLLATE "pg_catalog"."default",
  "number_type" varchar(20) COLLATE "pg_catalog"."default",
  "person" varchar(20) COLLATE "pg_catalog"."default",
  "tense" varchar(20) COLLATE "pg_catalog"."default",
  "mood" varchar(20) COLLATE "pg_catalog"."default",
  "voice" varchar(20) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for verse_words
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verse_words";
CREATE TABLE "sangraha"."verse_words" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "verse_id" uuid NOT NULL,
  "position" int4 NOT NULL,
  "surface_iast" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "surface_devanagari" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_iast" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "stem" varchar(200) COLLATE "pg_catalog"."default",
  "root" varchar(200) COLLATE "pg_catalog"."default",
  "pos" varchar(30) COLLATE "pg_catalog"."default",
  "form_type" varchar(30) COLLATE "pg_catalog"."default",
  "is_finite" bool,
  "lemma_gloss_ru" varchar(500) COLLATE "pg_catalog"."default",
  "lemma_gloss_en" varchar(500) COLLATE "pg_catalog"."default",
  "context_gloss_ru" varchar(500) COLLATE "pg_catalog"."default" NOT NULL,
  "context_gloss_en" varchar(500) COLLATE "pg_catalog"."default" NOT NULL,
  "formation_rule_numbers" text COLLATE "pg_catalog"."default",
  "analysis_confidence" varchar(10) COLLATE "pg_catalog"."default",
  "ambiguity_notes" text COLLATE "pg_catalog"."default",
  "vocabulary_word_id" uuid
)
;

-- ----------------------------
-- Table structure for verses
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verses";
CREATE TABLE "sangraha"."verses" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "chapter_id" uuid NOT NULL,
  "order_index" int4 NOT NULL,
  "raw_text" text COLLATE "pg_catalog"."default",
  "text_devanagari" text COLLATE "pg_catalog"."default",
  "text_iast" text COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'DRAFT'::character varying,
  "vocabulary_quiz_slug" varchar(255) COLLATE "pg_catalog"."default",
  "vocabulary_quiz_id" uuid,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6),
  "deleted_at" timestamptz(6),
  "translation_ru" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for works
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."works";
CREATE TABLE "sangraha"."works" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "slug" varchar(80) COLLATE "pg_catalog"."default" NOT NULL,
  "title_ru" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "title_sa_iast" varchar(255) COLLATE "pg_catalog"."default",
  "title_sa_devanagari" varchar(255) COLLATE "pg_catalog"."default",
  "description_ru" varchar(1000) COLLATE "pg_catalog"."default",
  "description_en" varchar(1000) COLLATE "pg_catalog"."default",
  "author" varchar(255) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "deleted_at" timestamptz(6),
  "source_id" uuid NOT NULL
)
;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "sangraha"."nominal_lemmas_id_seq"
OWNED BY "sangraha"."nominal_lemmas"."id";
SELECT setval('"sangraha"."nominal_lemmas_id_seq"', 1, false);

-- ----------------------------
-- Uniques structure for table chapters
-- ----------------------------
ALTER TABLE "sangraha"."chapters" ADD CONSTRAINT "uq_chapter_slug" UNIQUE ("work_id", "slug");

-- ----------------------------
-- Checks structure for table chapters
-- ----------------------------
ALTER TABLE "sangraha"."chapters" ADD CONSTRAINT "ck_chapter_slug" CHECK (slug::text ~ '^[a-z0-9][a-z0-9-]*$'::text);

-- ----------------------------
-- Primary Key structure for table chapters
-- ----------------------------
ALTER TABLE "sangraha"."chapters" ADD CONSTRAINT "pk_chapters" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table flyway_schema_history
-- ----------------------------
CREATE INDEX "flyway_schema_history_s_idx" ON "sangraha"."flyway_schema_history" USING btree (
  "success" "pg_catalog"."bool_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table flyway_schema_history
-- ----------------------------
ALTER TABLE "sangraha"."flyway_schema_history" ADD CONSTRAINT "flyway_schema_history_pk" PRIMARY KEY ("installed_rank");

-- ----------------------------
-- Indexes structure for table nominal_lemmas
-- ----------------------------
CREATE INDEX "idx_nominal_lemmas_confidence" ON "sangraha"."nominal_lemmas" USING btree (
  "confidence" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_nominal_lemmas_stem_class" ON "sangraha"."nominal_lemmas" USING btree (
  "stem_class" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table nominal_lemmas
-- ----------------------------
ALTER TABLE "sangraha"."nominal_lemmas" ADD CONSTRAINT "uq_nominal_lemmas_lemma_iast" UNIQUE ("lemma_iast");

-- ----------------------------
-- Checks structure for table nominal_lemmas
-- ----------------------------
ALTER TABLE "sangraha"."nominal_lemmas" ADD CONSTRAINT "ck_nominal_lemma_confidence" CHECK (confidence IS NULL OR (confidence = ANY (ARRAY['HIGH'::text, 'MEDIUM'::text, 'LOW'::text])));

-- ----------------------------
-- Primary Key structure for table nominal_lemmas
-- ----------------------------
ALTER TABLE "sangraha"."nominal_lemmas" ADD CONSTRAINT "pk_nominal_lemmas" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table sources
-- ----------------------------
ALTER TABLE "sangraha"."sources" ADD CONSTRAINT "uq_sources_code" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table sources
-- ----------------------------
ALTER TABLE "sangraha"."sources" ADD CONSTRAINT "pk_sources" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table verse_analyses
-- ----------------------------
ALTER TABLE "sangraha"."verse_analyses" ADD CONSTRAINT "pk_verse_analyses" PRIMARY KEY ("verse_id");

-- ----------------------------
-- Indexes structure for table verse_statistics
-- ----------------------------
CREATE INDEX "idx_verse_statistics_word_count" ON "sangraha"."verse_statistics" USING btree (
  "word_count" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_verse_statistics_grammar_info" ON "sangraha"."verse_statistics" USING gin (
  "grammar_info"
);

-- ----------------------------
-- Primary Key structure for table verse_statistics
-- ----------------------------
ALTER TABLE "sangraha"."verse_statistics" ADD CONSTRAINT "pk_verse_statistics" PRIMARY KEY ("verse_id");

-- ----------------------------
-- Primary Key structure for table verse_word_derivation
-- ----------------------------
ALTER TABLE "sangraha"."verse_word_derivation" ADD CONSTRAINT "pk_verse_word_derivation" PRIMARY KEY ("verse_word_id");

-- ----------------------------
-- Primary Key structure for table verse_word_morphology
-- ----------------------------
ALTER TABLE "sangraha"."verse_word_morphology" ADD CONSTRAINT "pk_verse_word_morphology" PRIMARY KEY ("verse_word_id");

-- ----------------------------
-- Indexes structure for table verse_words
-- ----------------------------
CREATE INDEX "idx_verse_words_lemma_iast" ON "sangraha"."verse_words" USING btree (
  "lemma_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_verse_words_verse_id" ON "sangraha"."verse_words" USING btree (
  "verse_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_verse_words_vocabulary_word_id" ON "sangraha"."verse_words" USING btree (
  "vocabulary_word_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table verse_words
-- ----------------------------
ALTER TABLE "sangraha"."verse_words" ADD CONSTRAINT "pk_verse_words" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table verses
-- ----------------------------
CREATE INDEX "verses_chapter_id_idx" ON "sangraha"."verses" USING btree (
  "chapter_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table verses
-- ----------------------------
ALTER TABLE "sangraha"."verses" ADD CONSTRAINT "ck_verse_status" CHECK (status::text = ANY (ARRAY['DRAFT'::text, 'ANALYZING'::text, 'ANALYZED'::text, 'FAILED'::text]));

-- ----------------------------
-- Primary Key structure for table verses
-- ----------------------------
ALTER TABLE "sangraha"."verses" ADD CONSTRAINT "pk_verses" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table works
-- ----------------------------
CREATE INDEX "idx_works_source_id" ON "sangraha"."works" USING btree (
  "source_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table works
-- ----------------------------
ALTER TABLE "sangraha"."works" ADD CONSTRAINT "works_slug_key" UNIQUE ("slug");

-- ----------------------------
-- Checks structure for table works
-- ----------------------------
ALTER TABLE "sangraha"."works" ADD CONSTRAINT "ck_work_slug" CHECK (slug::text ~ '^[a-z0-9][a-z0-9-]*$'::text);

-- ----------------------------
-- Primary Key structure for table works
-- ----------------------------
ALTER TABLE "sangraha"."works" ADD CONSTRAINT "pk_works" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table chapters
-- ----------------------------
ALTER TABLE "sangraha"."chapters" ADD CONSTRAINT "chapters_work_id_fkey" FOREIGN KEY ("work_id") REFERENCES "sangraha"."works" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verse_analyses
-- ----------------------------
ALTER TABLE "sangraha"."verse_analyses" ADD CONSTRAINT "verse_analyses_verse_id_fkey" FOREIGN KEY ("verse_id") REFERENCES "sangraha"."verses" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verse_statistics
-- ----------------------------
ALTER TABLE "sangraha"."verse_statistics" ADD CONSTRAINT "fk_verse_statistics_verse" FOREIGN KEY ("verse_id") REFERENCES "sangraha"."verses" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verse_word_derivation
-- ----------------------------
ALTER TABLE "sangraha"."verse_word_derivation" ADD CONSTRAINT "fk_derivation_word" FOREIGN KEY ("verse_word_id") REFERENCES "sangraha"."verse_words" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verse_word_morphology
-- ----------------------------
ALTER TABLE "sangraha"."verse_word_morphology" ADD CONSTRAINT "fk_morphology_word" FOREIGN KEY ("verse_word_id") REFERENCES "sangraha"."verse_words" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verse_words
-- ----------------------------
ALTER TABLE "sangraha"."verse_words" ADD CONSTRAINT "fk_verse_words_verse" FOREIGN KEY ("verse_id") REFERENCES "sangraha"."verses" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verses
-- ----------------------------
ALTER TABLE "sangraha"."verses" ADD CONSTRAINT "verses_chapter_id_fkey" FOREIGN KEY ("chapter_id") REFERENCES "sangraha"."chapters" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table works
-- ----------------------------
ALTER TABLE "sangraha"."works" ADD CONSTRAINT "fk_works_source" FOREIGN KEY ("source_id") REFERENCES "sangraha"."sources" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
