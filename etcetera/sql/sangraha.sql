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

 Date: 04/07/2026 18:56:49
*/


-- ----------------------------
-- Table structure for chapters
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."chapters";
CREATE TABLE "sangraha"."chapters" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "work_id" uuid NOT NULL,
  "slug" varchar(80) COLLATE "pg_catalog"."default" NOT NULL,
  "order_index" int4 NOT NULL,
  "title_ru" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
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
-- Table structure for outbox_events
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."outbox_events";
CREATE TABLE "sangraha"."outbox_events" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "aggregate_id" uuid NOT NULL,
  "event_type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "payload" jsonb NOT NULL,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'PENDING'::character varying,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "processed_at" timestamptz(6),
  "retry_count" int4 NOT NULL DEFAULT 0,
  "error_message" text COLLATE "pg_catalog"."default"
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
  "analyzed_at" timestamptz(6) NOT NULL DEFAULT now()
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
  "stem" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "root" varchar(200) COLLATE "pg_catalog"."default",
  "pos" varchar(30) COLLATE "pg_catalog"."default",
  "gender" varchar(20) COLLATE "pg_catalog"."default",
  "case_type" varchar(20) COLLATE "pg_catalog"."default",
  "number_type" varchar(20) COLLATE "pg_catalog"."default",
  "person" varchar(20) COLLATE "pg_catalog"."default",
  "tense" varchar(20) COLLATE "pg_catalog"."default",
  "mood" varchar(20) COLLATE "pg_catalog"."default",
  "voice" varchar(20) COLLATE "pg_catalog"."default",
  "gloss_ru" varchar(500) COLLATE "pg_catalog"."default" NOT NULL,
  "gloss_en" varchar(500) COLLATE "pg_catalog"."default" NOT NULL
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
  "text_devanagari" text COLLATE "pg_catalog"."default",
  "text_iast" text COLLATE "pg_catalog"."default",
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'DRAFT'::character varying,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6),
  "deleted_at" timestamptz(6)
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
  "description_ru" varchar(1000) COLLATE "pg_catalog"."default",
  "description_en" varchar(1000) COLLATE "pg_catalog"."default",
  "author" varchar(255) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "deleted_at" timestamptz(6)
)
;

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
-- Indexes structure for table outbox_events
-- ----------------------------
CREATE INDEX "idx_outbox_pending" ON "sangraha"."outbox_events" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
) WHERE status::text = 'PENDING'::text;

-- ----------------------------
-- Checks structure for table outbox_events
-- ----------------------------
ALTER TABLE "sangraha"."outbox_events" ADD CONSTRAINT "ck_event_type" CHECK (event_type::text = 'VERSE_VOCABULARY_EXTRACTED'::text);
ALTER TABLE "sangraha"."outbox_events" ADD CONSTRAINT "ck_status" CHECK (status::text = ANY (ARRAY['PENDING'::character varying, 'PROCESSED'::character varying, 'FAILED'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table outbox_events
-- ----------------------------
ALTER TABLE "sangraha"."outbox_events" ADD CONSTRAINT "pk_outbox_events" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table verse_analyses
-- ----------------------------
ALTER TABLE "sangraha"."verse_analyses" ADD CONSTRAINT "pk_verse_analyses" PRIMARY KEY ("verse_id");

-- ----------------------------
-- Indexes structure for table verse_words
-- ----------------------------
CREATE INDEX "idx_verse_words_verse_id" ON "sangraha"."verse_words" USING btree (
  "verse_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table verse_words
-- ----------------------------
ALTER TABLE "sangraha"."verse_words" ADD CONSTRAINT "pk_verse_words" PRIMARY KEY ("id");

-- ----------------------------
-- Checks structure for table verses
-- ----------------------------
ALTER TABLE "sangraha"."verses" ADD CONSTRAINT "ck_verse_status" CHECK (status::text = ANY (ARRAY['DRAFT'::character varying, 'ANALYZING'::character varying, 'ANALYZED'::character varying, 'FAILED'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table verses
-- ----------------------------
ALTER TABLE "sangraha"."verses" ADD CONSTRAINT "pk_verses" PRIMARY KEY ("id");

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
-- Foreign Keys structure for table verse_words
-- ----------------------------
ALTER TABLE "sangraha"."verse_words" ADD CONSTRAINT "verse_words_verse_id_fkey" FOREIGN KEY ("verse_id") REFERENCES "sangraha"."verses" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verses
-- ----------------------------
ALTER TABLE "sangraha"."verses" ADD CONSTRAINT "verses_chapter_id_fkey" FOREIGN KEY ("chapter_id") REFERENCES "sangraha"."chapters" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
