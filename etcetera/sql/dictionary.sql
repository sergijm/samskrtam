/*
 Navicat Premium Data Transfer

 Source Server         : mdm-dev-vm
 Source Server Type    : PostgreSQL
 Source Server Version : 170009 (170009)
 Source Host           : mdm-dev:5432
 Source Catalog        : samskrtam
 Source Schema         : dictionary

 Target Server Type    : PostgreSQL
 Target Server Version : 170009 (170009)
 File Encoding         : 65001

 Date: 23/06/2026 06:10:25
*/


-- ----------------------------
-- Sequence structure for mw_bio_terms_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "dictionary"."mw_bio_terms_id_seq";
CREATE SEQUENCE "dictionary"."mw_bio_terms_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for mw_entries_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "dictionary"."mw_entries_id_seq";
CREATE SEQUENCE "dictionary"."mw_entries_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for mw_sanskrit_words_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "dictionary"."mw_sanskrit_words_id_seq";
CREATE SEQUENCE "dictionary"."mw_sanskrit_words_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Table structure for mw_bio_terms
-- ----------------------------
DROP TABLE IF EXISTS "dictionary"."mw_bio_terms";
CREATE TABLE "dictionary"."mw_bio_terms" (
  "id" int4 NOT NULL DEFAULT nextval('"dictionary".mw_bio_terms_id_seq'::regclass),
  "entry_id" int4,
  "term_type" varchar(10) COLLATE "pg_catalog"."default",
  "latin_name" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for mw_entries
-- ----------------------------
DROP TABLE IF EXISTS "dictionary"."mw_entries";
CREATE TABLE "dictionary"."mw_entries" (
  "id" int4 NOT NULL DEFAULT nextval('"dictionary".mw_entries_id_seq'::regclass),
  "record_id" int4 NOT NULL,
  "entry_type" varchar(5) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_record_id" int4,
  "headword_slp1" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "headword_slp1_trailing" varchar(255) COLLATE "pg_catalog"."default",
  "headword_iast" varchar(255) COLLATE "pg_catalog"."default",
  "headword_devanagari" varchar(255) COLLATE "pg_catalog"."default",
  "homonym_number" int4,
  "subentry_type" varchar(2) COLLATE "pg_catalog"."default",
  "gender_text" text COLLATE "pg_catalog"."default",
  "gender_parsed" varchar(20) COLLATE "pg_catalog"."default",
  "gender_standardized" varchar(50) COLLATE "pg_catalog"."default",
  "definition" text COLLATE "pg_catalog"."default",
  "sanskrit_text" text COLLATE "pg_catalog"."default",
  "sanskrit_iast" text COLLATE "pg_catalog"."default",
  "source_reference" varchar(255) COLLATE "pg_catalog"."default",
  "page" int4,
  "column_num" int4,
  "is_revision" bool DEFAULT false,
  "is_supplement" bool DEFAULT false,
  "inheritance_info" varchar(10) COLLATE "pg_catalog"."default",
  "etymology" text COLLATE "pg_catalog"."default",
  "alternate_spellings" text[] COLLATE "pg_catalog"."default",
  "see_also" text[] COLLATE "pg_catalog"."default",
  "raw_xml" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT now(),
  "tsv_definition" tsvector
)
;

-- ----------------------------
-- Table structure for mw_sanskrit_words
-- ----------------------------
DROP TABLE IF EXISTS "dictionary"."mw_sanskrit_words";
CREATE TABLE "dictionary"."mw_sanskrit_words" (
  "id" int4 NOT NULL DEFAULT nextval('"dictionary".mw_sanskrit_words_id_seq'::regclass),
  "entry_id" int4,
  "word_slp1" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "word_iast" varchar(255) COLLATE "pg_catalog"."default",
  "word_devanagari" varchar(255) COLLATE "pg_catalog"."default",
  "word_type" varchar(20) COLLATE "pg_catalog"."default",
  "position" int4
)
;

-- ----------------------------
-- Table structure for mw_verb_info
-- ----------------------------
DROP TABLE IF EXISTS "dictionary"."mw_verb_info";
CREATE TABLE "dictionary"."mw_verb_info" (
  "entry_id" int4 NOT NULL,
  "verb_type" varchar(20) COLLATE "pg_catalog"."default",
  "class_pada" varchar(50) COLLATE "pg_catalog"."default",
  "parse_text" text COLLATE "pg_catalog"."default",
  "westergaard_root" varchar(100) COLLATE "pg_catalog"."default",
  "westergaard_ref" varchar(50) COLLATE "pg_catalog"."default",
  "whitney_root" varchar(100) COLLATE "pg_catalog"."default",
  "whitney_page" int4
)
;

-- ----------------------------
-- Function structure for update_tsv_definition
-- ----------------------------
DROP FUNCTION IF EXISTS "dictionary"."update_tsv_definition"();
CREATE OR REPLACE FUNCTION "dictionary"."update_tsv_definition"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.tsv_definition := to_tsvector('english', COALESCE(NEW.definition, ''));
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "dictionary"."mw_bio_terms_id_seq"
OWNED BY "dictionary"."mw_bio_terms"."id";
SELECT setval('"dictionary"."mw_bio_terms_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "dictionary"."mw_entries_id_seq"
OWNED BY "dictionary"."mw_entries"."id";
SELECT setval('"dictionary"."mw_entries_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "dictionary"."mw_sanskrit_words_id_seq"
OWNED BY "dictionary"."mw_sanskrit_words"."id";
SELECT setval('"dictionary"."mw_sanskrit_words_id_seq"', 1, false);

-- ----------------------------
-- Primary Key structure for table mw_bio_terms
-- ----------------------------
ALTER TABLE "dictionary"."mw_bio_terms" ADD CONSTRAINT "mw_bio_terms_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table mw_entries
-- ----------------------------
CREATE INDEX "idx_mw_entries_gender" ON "dictionary"."mw_entries" USING btree (
  "gender_standardized" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entries_headword_iast" ON "dictionary"."mw_entries" USING btree (
  "headword_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entries_headword_slp1" ON "dictionary"."mw_entries" USING btree (
  "headword_slp1" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entries_parent" ON "dictionary"."mw_entries" USING btree (
  "parent_record_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_entries_record_id" ON "dictionary"."mw_entries" USING btree (
  "record_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mw_tsv_definition" ON "dictionary"."mw_entries" USING gin (
  "tsv_definition" "pg_catalog"."tsvector_ops"
);

-- ----------------------------
-- Triggers structure for table mw_entries
-- ----------------------------
CREATE TRIGGER "tsvector_update" BEFORE INSERT OR UPDATE ON "dictionary"."mw_entries"
FOR EACH ROW
EXECUTE PROCEDURE "dictionary"."update_tsv_definition"();

-- ----------------------------
-- Uniques structure for table mw_entries
-- ----------------------------
ALTER TABLE "dictionary"."mw_entries" ADD CONSTRAINT "mw_entries_record_id_key" UNIQUE ("record_id");

-- ----------------------------
-- Primary Key structure for table mw_entries
-- ----------------------------
ALTER TABLE "dictionary"."mw_entries" ADD CONSTRAINT "mw_entries_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table mw_sanskrit_words
-- ----------------------------
ALTER TABLE "dictionary"."mw_sanskrit_words" ADD CONSTRAINT "mw_sanskrit_words_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table mw_verb_info
-- ----------------------------
ALTER TABLE "dictionary"."mw_verb_info" ADD CONSTRAINT "mw_verb_info_pkey" PRIMARY KEY ("entry_id");

-- ----------------------------
-- Foreign Keys structure for table mw_bio_terms
-- ----------------------------
ALTER TABLE "dictionary"."mw_bio_terms" ADD CONSTRAINT "mw_bio_terms_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "dictionary"."mw_entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table mw_sanskrit_words
-- ----------------------------
ALTER TABLE "dictionary"."mw_sanskrit_words" ADD CONSTRAINT "mw_sanskrit_words_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "dictionary"."mw_entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table mw_verb_info
-- ----------------------------
ALTER TABLE "dictionary"."mw_verb_info" ADD CONSTRAINT "mw_verb_info_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "dictionary"."mw_entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
