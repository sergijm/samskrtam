/*
 Navicat Premium Data Transfer

 Source Server         : localhost-pg17
 Source Server Type    : PostgreSQL
 Source Server Version : 170000 (170000)
 Source Host           : localhost:5436
 Source Catalog        : samskrtam
 Source Schema         : cologne_apte

 Target Server Type    : PostgreSQL
 Target Server Version : 170000 (170000)
 File Encoding         : 65001

 Date: 24/08/2026 17:49:55
*/


-- ----------------------------
-- Type structure for derivation_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."derivation_type";
CREATE TYPE "cologne_apte"."derivation_type" AS ENUM (
  'SIMPLE_INFLECTION',
  'ABSOLUTIVE',
  'PARTICIPLE',
  'GERUNDIVE',
  'INFINITIVE',
  'CAUSATIVE',
  'DESIDERATIVE',
  'DENOMINATIVE',
  'COMPOUND_VERB',
  'OTHER'
);
ALTER TYPE "cologne_apte"."derivation_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for form_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."form_type";
CREATE TYPE "cologne_apte"."form_type" AS ENUM (
  'FINITE',
  'INFINITIVE',
  'ABSOLUTIVE',
  'PARTICIPLE',
  'GERUNDIVE',
  'OTHER_NONFINITE',
  'NOMINAL',
  'ADJECTIVAL',
  'PRONOMINAL',
  'INDECLINABLE'
);
ALTER TYPE "cologne_apte"."form_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for gender_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."gender_type";
CREATE TYPE "cologne_apte"."gender_type" AS ENUM (
  'MASCULINE',
  'FEMININE',
  'NEUTER',
  'UNSPECIFIED'
);
ALTER TYPE "cologne_apte"."gender_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for grammatical_case
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."grammatical_case";
CREATE TYPE "cologne_apte"."grammatical_case" AS ENUM (
  'NOMINATIVE',
  'ACCUSATIVE',
  'INSTRUMENTAL',
  'DATIVE',
  'ABLATIVE',
  'GENITIVE',
  'LOCATIVE',
  'VOCATIVE'
);
ALTER TYPE "cologne_apte"."grammatical_case" OWNER TO "postgres";

-- ----------------------------
-- Type structure for mood_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."mood_type";
CREATE TYPE "cologne_apte"."mood_type" AS ENUM (
  'INDICATIVE',
  'OPTATIVE',
  'IMPERATIVE',
  'CONDITIONAL',
  'BENEDICTIVE',
  'INJUNCTIVE'
);
ALTER TYPE "cologne_apte"."mood_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for number_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."number_type";
CREATE TYPE "cologne_apte"."number_type" AS ENUM (
  'SINGULAR',
  'DUAL',
  'PLURAL'
);
ALTER TYPE "cologne_apte"."number_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for pada_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."pada_type";
CREATE TYPE "cologne_apte"."pada_type" AS ENUM (
  'PARASMAIPADA',
  'ATMANEPADA',
  'UBHAYAPADA'
);
ALTER TYPE "cologne_apte"."pada_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for part_of_speech
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."part_of_speech";
CREATE TYPE "cologne_apte"."part_of_speech" AS ENUM (
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
ALTER TYPE "cologne_apte"."part_of_speech" OWNER TO "postgres";

-- ----------------------------
-- Type structure for person_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."person_type";
CREATE TYPE "cologne_apte"."person_type" AS ENUM (
  'FIRST',
  'SECOND',
  'THIRD'
);
ALTER TYPE "cologne_apte"."person_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for tense_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."tense_type";
CREATE TYPE "cologne_apte"."tense_type" AS ENUM (
  'PRESENT',
  'IMPERFECT',
  'PERFECT',
  'AORIST',
  'FUTURE',
  'PERIPHRASTIC_FUTURE',
  'CONDITIONAL',
  'BENEDICTIVE'
);
ALTER TYPE "cologne_apte"."tense_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for verb_class
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."verb_class";
CREATE TYPE "cologne_apte"."verb_class" AS ENUM (
  'CLASS_1',
  'CLASS_2',
  'CLASS_3',
  'CLASS_4',
  'CLASS_5',
  'CLASS_6',
  'CLASS_7',
  'CLASS_8',
  'CLASS_9',
  'CLASS_10'
);
ALTER TYPE "cologne_apte"."verb_class" OWNER TO "postgres";

-- ----------------------------
-- Type structure for voice_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_apte"."voice_type";
CREATE TYPE "cologne_apte"."voice_type" AS ENUM (
  'ACTIVE',
  'MIDDLE',
  'PASSIVE'
);
ALTER TYPE "cologne_apte"."voice_type" OWNER TO "postgres";

-- ----------------------------
-- Sequence structure for citations_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."citations_id_seq";
CREATE SEQUENCE "cologne_apte"."citations_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for dictionaries_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."dictionaries_id_seq";
CREATE SEQUENCE "cologne_apte"."dictionaries_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 32767
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for entries_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."entries_id_seq";
CREATE SEQUENCE "cologne_apte"."entries_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for grammar_abbreviations_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."grammar_abbreviations_id_seq";
CREATE SEQUENCE "cologne_apte"."grammar_abbreviations_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for grammar_facts_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."grammar_facts_id_seq";
CREATE SEQUENCE "cologne_apte"."grammar_facts_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for import_log_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."import_log_id_seq";
CREATE SEQUENCE "cologne_apte"."import_log_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for page_breaks_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."page_breaks_id_seq";
CREATE SEQUENCE "cologne_apte"."page_breaks_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for senses_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."senses_id_seq";
CREATE SEQUENCE "cologne_apte"."senses_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for text_source_abbreviations_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."text_source_abbreviations_id_seq";
CREATE SEQUENCE "cologne_apte"."text_source_abbreviations_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for unmatched_tokens_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_apte"."unmatched_tokens_id_seq";
CREATE SEQUENCE "cologne_apte"."unmatched_tokens_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 9223372036854775807
START 1
CACHE 1;

-- ----------------------------
-- Table structure for citations
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."citations";
CREATE TABLE "cologne_apte"."citations" (
  "id" int8 NOT NULL DEFAULT nextval('"cologne_apte".citations_id_seq'::regclass),
  "entry_id" int8 NOT NULL,
  "sense_id" int8,
  "source_abbrev_id" int4,
  "locus" text COLLATE "pg_catalog"."default",
  "raw_text" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for dictionaries
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."dictionaries";
CREATE TABLE "cologne_apte"."dictionaries" (
  "id" int2 NOT NULL DEFAULT nextval('"cologne_apte".dictionaries_id_seq'::regclass),
  "code" text COLLATE "pg_catalog"."default" NOT NULL,
  "title" text COLLATE "pg_catalog"."default" NOT NULL,
  "author" text COLLATE "pg_catalog"."default",
  "edition_year" int4,
  "source_repo_url" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for entries
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."entries";
CREATE TABLE "cologne_apte"."entries" (
  "id" int8 NOT NULL DEFAULT nextval('"cologne_apte".entries_id_seq'::regclass),
  "dictionary_id" int2 NOT NULL,
  "lnum" text COLLATE "pg_catalog"."default" NOT NULL,
  "lnum_sort" int4,
  "lnum_suffix" text COLLATE "pg_catalog"."default",
  "pc_volume" int2,
  "pc_page" int4,
  "pc_column" text COLLATE "pg_catalog"."default",
  "k1_slp1" text COLLATE "pg_catalog"."default" NOT NULL,
  "k2_original" text COLLATE "pg_catalog"."default",
  "homonym_num" int2,
  "headword_devanagari" text COLLATE "pg_catalog"."default",
  "raw_markup" text COLLATE "pg_catalog"."default" NOT NULL,
  "body_text" text COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for grammar_abbreviations
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."grammar_abbreviations";
CREATE TABLE "cologne_apte"."grammar_abbreviations" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_apte".grammar_abbreviations_id_seq'::regclass),
  "abbrev" text COLLATE "pg_catalog"."default" NOT NULL,
  "abbrev_normalized" text COLLATE "pg_catalog"."default" NOT NULL,
  "category" text COLLATE "pg_catalog"."default" NOT NULL,
  "mapped_value" text COLLATE "pg_catalog"."default" NOT NULL,
  "notes" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for grammar_facts
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."grammar_facts";
CREATE TABLE "cologne_apte"."grammar_facts" (
  "id" int8 NOT NULL DEFAULT nextval('"cologne_apte".grammar_facts_id_seq'::regclass),
  "entry_id" int8 NOT NULL,
  "sense_id" int8,
  "part_of_speech" "cologne_apte"."part_of_speech",
  "derivation_type" "cologne_apte"."derivation_type",
  "form_type" "cologne_apte"."form_type",
  "gender" "cologne_apte"."gender_type",
  "grammatical_case" "cologne_apte"."grammatical_case",
  "mood" "cologne_apte"."mood_type",
  "grammatical_number" "cologne_apte"."number_type",
  "person" "cologne_apte"."person_type",
  "tense" "cologne_apte"."tense_type",
  "voice" "cologne_apte"."voice_type",
  "verb_class" "cologne_apte"."verb_class",
  "pada" "cologne_apte"."pada_type",
  "source_abbrev_ids" int4[],
  "raw_grammar_span" text COLLATE "pg_catalog"."default" NOT NULL,
  "confidence" numeric(3,2) DEFAULT 1.00,
  "parse_notes" text COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for import_log
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."import_log";
CREATE TABLE "cologne_apte"."import_log" (
  "id" int8 NOT NULL DEFAULT nextval('"cologne_apte".import_log_id_seq'::regclass),
  "entry_id" int8 NOT NULL,
  "stage" text COLLATE "pg_catalog"."default" NOT NULL,
  "status" text COLLATE "pg_catalog"."default" NOT NULL,
  "message" text COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for page_breaks
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."page_breaks";
CREATE TABLE "cologne_apte"."page_breaks" (
  "id" int8 NOT NULL DEFAULT nextval('"cologne_apte".page_breaks_id_seq'::regclass),
  "entry_id" int8 NOT NULL,
  "seq_in_entry" int2 NOT NULL,
  "volume" int2,
  "page" int4,
  "column_label" text COLLATE "pg_catalog"."default",
  "line_count" int4,
  "raw_marker" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for senses
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."senses";
CREATE TABLE "cologne_apte"."senses" (
  "id" int8 NOT NULL DEFAULT nextval('"cologne_apte".senses_id_seq'::regclass),
  "entry_id" int8 NOT NULL,
  "seq" int2 NOT NULL,
  "marker" text COLLATE "pg_catalog"."default",
  "gloss_text" text COLLATE "pg_catalog"."default",
  "raw_text" text COLLATE "pg_catalog"."default" NOT NULL,
  "parent_seq" int2
)
;

-- ----------------------------
-- Table structure for text_source_abbreviations
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."text_source_abbreviations";
CREATE TABLE "cologne_apte"."text_source_abbreviations" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_apte".text_source_abbreviations_id_seq'::regclass),
  "abbrev" text COLLATE "pg_catalog"."default" NOT NULL,
  "full_title" text COLLATE "pg_catalog"."default",
  "author" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for unmatched_tokens
-- ----------------------------
DROP TABLE IF EXISTS "cologne_apte"."unmatched_tokens";
CREATE TABLE "cologne_apte"."unmatched_tokens" (
  "id" int8 NOT NULL DEFAULT nextval('"cologne_apte".unmatched_tokens_id_seq'::regclass),
  "entry_id" int8 NOT NULL,
  "token_raw" text COLLATE "pg_catalog"."default" NOT NULL,
  "token_normalized" text COLLATE "pg_catalog"."default" NOT NULL,
  "context_span" text COLLATE "pg_catalog"."default",
  "resolved" bool NOT NULL DEFAULT false,
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."citations_id_seq"
OWNED BY "cologne_apte"."citations"."id";
SELECT setval('"cologne_apte"."citations_id_seq"', 67156, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."dictionaries_id_seq"
OWNED BY "cologne_apte"."dictionaries"."id";
SELECT setval('"cologne_apte"."dictionaries_id_seq"', 1, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."entries_id_seq"
OWNED BY "cologne_apte"."entries"."id";
SELECT setval('"cologne_apte"."entries_id_seq"', 635929, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."grammar_abbreviations_id_seq"
OWNED BY "cologne_apte"."grammar_abbreviations"."id";
SELECT setval('"cologne_apte"."grammar_abbreviations_id_seq"', 70, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."grammar_facts_id_seq"
OWNED BY "cologne_apte"."grammar_facts"."id";
SELECT setval('"cologne_apte"."grammar_facts_id_seq"', 113434, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."import_log_id_seq"
OWNED BY "cologne_apte"."import_log"."id";
SELECT setval('"cologne_apte"."import_log_id_seq"', 909470, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."page_breaks_id_seq"
OWNED BY "cologne_apte"."page_breaks"."id";
SELECT setval('"cologne_apte"."page_breaks_id_seq"', 13377, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."senses_id_seq"
OWNED BY "cologne_apte"."senses"."id";
SELECT setval('"cologne_apte"."senses_id_seq"', 148530, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."text_source_abbreviations_id_seq"
OWNED BY "cologne_apte"."text_source_abbreviations"."id";
SELECT setval('"cologne_apte"."text_source_abbreviations_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_apte"."unmatched_tokens_id_seq"
OWNED BY "cologne_apte"."unmatched_tokens"."id";
SELECT setval('"cologne_apte"."unmatched_tokens_id_seq"', 581194, true);

-- ----------------------------
-- Primary Key structure for table citations
-- ----------------------------
ALTER TABLE "cologne_apte"."citations" ADD CONSTRAINT "citations_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table dictionaries
-- ----------------------------
ALTER TABLE "cologne_apte"."dictionaries" ADD CONSTRAINT "dictionaries_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table dictionaries
-- ----------------------------
ALTER TABLE "cologne_apte"."dictionaries" ADD CONSTRAINT "dictionaries_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table entries
-- ----------------------------
CREATE INDEX "idx_entries_dict" ON "cologne_apte"."entries" USING btree (
  "dictionary_id" "pg_catalog"."int2_ops" ASC NULLS LAST
);
CREATE INDEX "idx_entries_headword_deva" ON "cologne_apte"."entries" USING btree (
  "headword_devanagari" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_entries_k1" ON "cologne_apte"."entries" USING btree (
  "k1_slp1" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_entries_k2" ON "cologne_apte"."entries" USING btree (
  "k2_original" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table entries
-- ----------------------------
ALTER TABLE "cologne_apte"."entries" ADD CONSTRAINT "entries_dictionary_id_lnum_key" UNIQUE ("dictionary_id", "lnum");

-- ----------------------------
-- Primary Key structure for table entries
-- ----------------------------
ALTER TABLE "cologne_apte"."entries" ADD CONSTRAINT "entries_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table grammar_abbreviations
-- ----------------------------
ALTER TABLE "cologne_apte"."grammar_abbreviations" ADD CONSTRAINT "grammar_abbreviations_abbrev_normalized_category_key" UNIQUE ("abbrev_normalized", "category");

-- ----------------------------
-- Primary Key structure for table grammar_abbreviations
-- ----------------------------
ALTER TABLE "cologne_apte"."grammar_abbreviations" ADD CONSTRAINT "grammar_abbreviations_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table grammar_facts
-- ----------------------------
CREATE INDEX "idx_grammar_facts_entry" ON "cologne_apte"."grammar_facts" USING btree (
  "entry_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_grammar_facts_pos" ON "cologne_apte"."grammar_facts" USING btree (
  "part_of_speech" "pg_catalog"."enum_ops" ASC NULLS LAST
);
CREATE INDEX "idx_grammar_facts_sense" ON "cologne_apte"."grammar_facts" USING btree (
  "sense_id" "pg_catalog"."int8_ops" ASC NULLS LAST
);
CREATE INDEX "idx_grammar_facts_verbclass" ON "cologne_apte"."grammar_facts" USING btree (
  "verb_class" "pg_catalog"."enum_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table grammar_facts
-- ----------------------------
ALTER TABLE "cologne_apte"."grammar_facts" ADD CONSTRAINT "grammar_facts_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table import_log
-- ----------------------------
CREATE INDEX "idx_import_log_status" ON "cologne_apte"."import_log" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table import_log
-- ----------------------------
ALTER TABLE "cologne_apte"."import_log" ADD CONSTRAINT "import_log_status_check" CHECK (status = ANY (ARRAY['ok'::text, 'partial'::text, 'failed'::text]));

-- ----------------------------
-- Primary Key structure for table import_log
-- ----------------------------
ALTER TABLE "cologne_apte"."import_log" ADD CONSTRAINT "import_log_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table page_breaks
-- ----------------------------
ALTER TABLE "cologne_apte"."page_breaks" ADD CONSTRAINT "page_breaks_entry_id_seq_in_entry_key" UNIQUE ("entry_id", "seq_in_entry");

-- ----------------------------
-- Primary Key structure for table page_breaks
-- ----------------------------
ALTER TABLE "cologne_apte"."page_breaks" ADD CONSTRAINT "page_breaks_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table senses
-- ----------------------------
ALTER TABLE "cologne_apte"."senses" ADD CONSTRAINT "senses_entry_id_seq_key" UNIQUE ("entry_id", "seq");

-- ----------------------------
-- Primary Key structure for table senses
-- ----------------------------
ALTER TABLE "cologne_apte"."senses" ADD CONSTRAINT "senses_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table text_source_abbreviations
-- ----------------------------
ALTER TABLE "cologne_apte"."text_source_abbreviations" ADD CONSTRAINT "text_source_abbreviations_abbrev_key" UNIQUE ("abbrev");

-- ----------------------------
-- Primary Key structure for table text_source_abbreviations
-- ----------------------------
ALTER TABLE "cologne_apte"."text_source_abbreviations" ADD CONSTRAINT "text_source_abbreviations_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table unmatched_tokens
-- ----------------------------
CREATE INDEX "idx_unmatched_tokens_norm" ON "cologne_apte"."unmatched_tokens" USING btree (
  "token_normalized" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_unmatched_tokens_resolved" ON "cologne_apte"."unmatched_tokens" USING btree (
  "resolved" "pg_catalog"."bool_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table unmatched_tokens
-- ----------------------------
ALTER TABLE "cologne_apte"."unmatched_tokens" ADD CONSTRAINT "unmatched_tokens_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table citations
-- ----------------------------
ALTER TABLE "cologne_apte"."citations" ADD CONSTRAINT "citations_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_apte"."entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "cologne_apte"."citations" ADD CONSTRAINT "citations_sense_id_fkey" FOREIGN KEY ("sense_id") REFERENCES "cologne_apte"."senses" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "cologne_apte"."citations" ADD CONSTRAINT "citations_source_abbrev_id_fkey" FOREIGN KEY ("source_abbrev_id") REFERENCES "cologne_apte"."text_source_abbreviations" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table entries
-- ----------------------------
ALTER TABLE "cologne_apte"."entries" ADD CONSTRAINT "entries_dictionary_id_fkey" FOREIGN KEY ("dictionary_id") REFERENCES "cologne_apte"."dictionaries" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table grammar_facts
-- ----------------------------
ALTER TABLE "cologne_apte"."grammar_facts" ADD CONSTRAINT "grammar_facts_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_apte"."entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "cologne_apte"."grammar_facts" ADD CONSTRAINT "grammar_facts_sense_id_fkey" FOREIGN KEY ("sense_id") REFERENCES "cologne_apte"."senses" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table import_log
-- ----------------------------
ALTER TABLE "cologne_apte"."import_log" ADD CONSTRAINT "import_log_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_apte"."entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table page_breaks
-- ----------------------------
ALTER TABLE "cologne_apte"."page_breaks" ADD CONSTRAINT "page_breaks_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_apte"."entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table senses
-- ----------------------------
ALTER TABLE "cologne_apte"."senses" ADD CONSTRAINT "senses_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_apte"."entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table unmatched_tokens
-- ----------------------------
ALTER TABLE "cologne_apte"."unmatched_tokens" ADD CONSTRAINT "unmatched_tokens_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_apte"."entries" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
