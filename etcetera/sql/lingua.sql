/*
 Navicat Premium Data Transfer

 Source Server         : localhost-pg17
 Source Server Type    : PostgreSQL
 Source Server Version : 170000 (170000)
 Source Host           : localhost:5436
 Source Catalog        : samskrtam
 Source Schema         : lingua

 Target Server Type    : PostgreSQL
 Target Server Version : 170000 (170000)
 File Encoding         : 65001

 Date: 24/08/2026 09:06:59
*/


-- ----------------------------
-- Type structure for gender_enum
-- ----------------------------
DROP TYPE IF EXISTS "lingua"."gender_enum";
CREATE TYPE "lingua"."gender_enum" AS ENUM (
  'MASCULINE',
  'FEMININE',
  'NEUTER',
  'UNSPECIFIED'
);
ALTER TYPE "lingua"."gender_enum" OWNER TO "postgres";

-- ----------------------------
-- Type structure for grammatical_case_enum
-- ----------------------------
DROP TYPE IF EXISTS "lingua"."grammatical_case_enum";
CREATE TYPE "lingua"."grammatical_case_enum" AS ENUM (
  'NOMINATIVE',
  'ACCUSATIVE',
  'INSTRUMENTAL',
  'DATIVE',
  'ABLATIVE',
  'GENITIVE',
  'LOCATIVE',
  'VOCATIVE'
);
ALTER TYPE "lingua"."grammatical_case_enum" OWNER TO "postgres";

-- ----------------------------
-- Type structure for number_type_enum
-- ----------------------------
DROP TYPE IF EXISTS "lingua"."number_type_enum";
CREATE TYPE "lingua"."number_type_enum" AS ENUM (
  'SINGULAR',
  'DUAL',
  'PLURAL'
);
ALTER TYPE "lingua"."number_type_enum" OWNER TO "postgres";

-- ----------------------------
-- Type structure for pos_enum
-- ----------------------------
DROP TYPE IF EXISTS "lingua"."pos_enum";
CREATE TYPE "lingua"."pos_enum" AS ENUM (
  'NOUN',
  'VERB',
  'ADJECTIVE',
  'PRONOUN',
  'ADVERB',
  'PARTICLE',
  'INDECLINABLE',
  'NUMERAL',
  'CONJUNCTION',
  'INTERJECTION',
  'OTHER'
);
ALTER TYPE "lingua"."pos_enum" OWNER TO "postgres";

-- ----------------------------
-- Type structure for stem_type_enum
-- ----------------------------
DROP TYPE IF EXISTS "lingua"."stem_type_enum";
CREATE TYPE "lingua"."stem_type_enum" AS ENUM (
  'A_STEM',
  'AA_STEM',
  'I_STEM',
  'II_STEM',
  'U_STEM',
  'UU_STEM',
  'R_STEM',
  'IN_STEM',
  'AN_STEM',
  'AS_STEM',
  'IS_STEM',
  'US_STEM',
  'ANT_STEM',
  'VAT_STEM',
  'ROOT_STEM',
  'O_STEM',
  'AU_STEM',
  'PRON_TAD_MASC',
  'PRON_TAD_NEUT',
  'PRON_TAD_FEM',
  'PRON_IDAM_MASC',
  'PRON_IDAM_NEUT',
  'PRON_IDAM_FEM',
  'PRON_ADAS_MASC',
  'PRON_ADAS_NEUT',
  'PRON_ADAS_FEM',
  'PRON_ASMAD',
  'PRON_YUSMAD',
  'PRON_SARVA_MASC',
  'PRON_SARVA_NEUT',
  'PRON_SARVA_FEM',
  'PRON_PURVA_MASC',
  'PRON_PURVA_NEUT',
  'PRON_PURVA_FEM',
  'PRON_VAT_MASC',
  'PRON_VAT_FEM',
  'PRON_UBHA_MASC',
  'PRON_UBHA_FN',
  'PRON_AN',
  'PRON_KATI'
);
ALTER TYPE "lingua"."stem_type_enum" OWNER TO "postgres";

-- ----------------------------
-- Sequence structure for case_endings_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "lingua"."case_endings_id_seq";
CREATE SEQUENCE "lingua"."case_endings_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Table structure for case_endings
-- ----------------------------
DROP TABLE IF EXISTS "lingua"."case_endings";
CREATE TABLE "lingua"."case_endings" (
  "id" int4 NOT NULL DEFAULT nextval('"lingua".case_endings_id_seq'::regclass),
  "stem_type" "lingua"."stem_type_enum" NOT NULL,
  "pos" "lingua"."pos_enum" NOT NULL,
  "gender" "lingua"."gender_enum" NOT NULL,
  "number" "lingua"."number_type_enum" NOT NULL,
  "grammatical_case" "lingua"."grammatical_case_enum" NOT NULL,
  "case_ending" varchar(64) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for lemma_frequency
-- ----------------------------
DROP TABLE IF EXISTS "lingua"."lemma_frequency";
CREATE TABLE "lingua"."lemma_frequency" (
  "lemma_iast" varchar(200) COLLATE "pg_catalog"."default",
  "pos" varchar COLLATE "pg_catalog"."default",
  "occurrence_count" int8
)
;

-- ----------------------------
-- Table structure for semantic_class
-- ----------------------------
DROP TABLE IF EXISTS "lingua"."semantic_class";
CREATE TABLE "lingua"."semantic_class" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "name_ru" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_id" uuid
)
;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "lingua"."case_endings_id_seq"
OWNED BY "lingua"."case_endings"."id";
SELECT setval('"lingua"."case_endings_id_seq"', 524, true);

-- ----------------------------
-- Indexes structure for table case_endings
-- ----------------------------
CREATE INDEX "idx_case_endings_lookup" ON "lingua"."case_endings" USING btree (
  "stem_type" "pg_catalog"."enum_ops" ASC NULLS LAST,
  "gender" "pg_catalog"."enum_ops" ASC NULLS LAST,
  "number" "pg_catalog"."enum_ops" ASC NULLS LAST,
  "grammatical_case" "pg_catalog"."enum_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table case_endings
-- ----------------------------
ALTER TABLE "lingua"."case_endings" ADD CONSTRAINT "unq_case_ending" UNIQUE ("stem_type", "pos", "gender", "number", "grammatical_case", "case_ending");

-- ----------------------------
-- Primary Key structure for table case_endings
-- ----------------------------
ALTER TABLE "lingua"."case_endings" ADD CONSTRAINT "case_endings_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table semantic_class
-- ----------------------------
CREATE INDEX "semantic_class_code_idx" ON "lingua"."semantic_class" USING btree (
  "code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table semantic_class
-- ----------------------------
ALTER TABLE "lingua"."semantic_class" ADD CONSTRAINT "semantic_class_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table semantic_class
-- ----------------------------
ALTER TABLE "lingua"."semantic_class" ADD CONSTRAINT "semantic_class_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table semantic_class
-- ----------------------------
ALTER TABLE "lingua"."semantic_class" ADD CONSTRAINT "semantic_class_parent_id_fkey" FOREIGN KEY ("parent_id") REFERENCES "curriculum"."semantic_class" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
