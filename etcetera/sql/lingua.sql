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

 Date: 24/08/2026 17:50:54
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
-- Function structure for normalize_lemma
-- ----------------------------
DROP FUNCTION IF EXISTS "lingua"."normalize_lemma"("p_text" text);
CREATE OR REPLACE FUNCTION "lingua"."normalize_lemma"("p_text" text)
  RETURNS "pg_catalog"."text" AS $BODY$
    SELECT lower(
        regexp_replace(normalize(trim(p_text), NFKD), '[\u0300-\u036f]', '', 'g')
    );
$BODY$
  LANGUAGE sql IMMUTABLE STRICT
  COST 100;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "lingua"."case_endings_id_seq"
OWNED BY "lingua"."case_endings"."id";
SELECT setval('"lingua"."case_endings_id_seq"', 602, true);

-- ----------------------------
-- Indexes structure for table case_endings
-- ----------------------------
CREATE INDEX "case_endings_case_ending_idx" ON "lingua"."case_endings" USING btree (
  "case_ending" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "case_endings_gender_idx" ON "lingua"."case_endings" USING btree (
  "gender" "pg_catalog"."enum_ops" ASC NULLS LAST
);
CREATE INDEX "case_endings_grammatical_case_idx" ON "lingua"."case_endings" USING btree (
  "grammatical_case" "pg_catalog"."enum_ops" ASC NULLS LAST
);
CREATE INDEX "case_endings_number_idx" ON "lingua"."case_endings" USING btree (
  "number" "pg_catalog"."enum_ops" ASC NULLS LAST
);
CREATE INDEX "case_endings_pos_idx" ON "lingua"."case_endings" USING btree (
  "pos" "pg_catalog"."enum_ops" ASC NULLS LAST
);
CREATE INDEX "case_endings_stem_type_idx" ON "lingua"."case_endings" USING btree (
  "stem_type" "pg_catalog"."enum_ops" ASC NULLS LAST
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

-- ----------------------------
-- Sequence structure for verbal_endings_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "lingua"."verbal_endings_id_seq";
CREATE SEQUENCE "lingua"."verbal_endings_id_seq"
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Table structure for verbal_endings
-- ----------------------------
DROP TABLE IF EXISTS "lingua"."verbal_endings";
CREATE TABLE "lingua"."verbal_endings" (
  "id" int4 NOT NULL DEFAULT nextval('"lingua".verbal_endings_id_seq'::regclass),
  "ending" varchar(32) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_suffix" varchar(8) COLLATE "pg_catalog"."default" NOT NULL DEFAULT '',
  "has_augment" bool NOT NULL DEFAULT false,
  "tense_mood" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "person_number" varchar(16) COLLATE "pg_catalog"."default" NOT NULL,
  "pada" varchar(8) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'any',
  "notes" varchar(255) COLLATE "pg_catalog"."default"
);

-- ----------------------------
-- Indexes structure for table verbal_endings
-- ----------------------------
CREATE INDEX "verbal_endings_ending_idx" ON "lingua"."verbal_endings" USING btree (
  "ending" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "verbal_endings_tense_mood_idx" ON "lingua"."verbal_endings" USING btree (
  "tense_mood" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "verbal_endings_person_number_idx" ON "lingua"."verbal_endings" USING btree (
  "person_number" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "verbal_endings_pada_idx" ON "lingua"."verbal_endings" USING btree (
  "pada" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Unique constraint for table verbal_endings
-- ----------------------------
ALTER TABLE "lingua"."verbal_endings" ADD CONSTRAINT "unq_verbal_ending" UNIQUE ("ending", "lemma_suffix", "has_augment", "tense_mood", "person_number", "pada");

-- ----------------------------
-- Primary Key structure for table verbal_endings
-- ----------------------------
ALTER TABLE "lingua"."verbal_endings" ADD CONSTRAINT "verbal_endings_pkey" PRIMARY KEY ("id");

ALTER SEQUENCE "lingua"."verbal_endings_id_seq"
OWNED BY "lingua"."verbal_endings"."id";

-- ----------------------------
-- Seed data for verbal_endings
-- ----------------------------
INSERT INTO "lingua"."verbal_endings" ("ending", "lemma_suffix", "has_augment", "tense_mood", "person_number", "pada", "notes") VALUES
('ati',     'a',   FALSE, 'Present', '3S', 'P', '1/4/6/10 class thematic: bhavati -> bhava'),
('ataH',    'a',   FALSE, 'Present', '3D', 'P', 'bhavataH -> bhava'),
('anti',    'a',   FALSE, 'Present', '3P', 'P', 'bhavanti -> bhava'),
('asi',     'a',   FALSE, 'Present', '2S', 'P', 'bhavasi -> bhava'),
('athaH',   'a',   FALSE, 'Present', '2D', 'P', 'bhavathaH -> bhava'),
('atha',    'a',   FALSE, 'Present', '2P', 'P', 'bhavatha -> bhava'),
('Ami',     'a',   FALSE, 'Present', '1S', 'P', 'bhavAmi -> bhava'),
('AvaH',    'a',   FALSE, 'Present', '1D', 'P', 'bhavAvaH -> bhava'),
('AmaH',    'a',   FALSE, 'Present', '1P', 'P', 'bhavAmaH -> bhava'),
('ti',      '',    FALSE, 'Present', '3S', 'P', 'atti -> ad, asti -> as, dAti -> dA'),
('taH',     '',    FALSE, 'Present', '3D', 'P', 'sthaH -> as, dattaH -> dA'),
('anti',    '',    FALSE, 'Present', '3P', 'P', 'santi -> as'),
('ati',     '',    FALSE, 'Present', '3P', 'P', 'dadati -> dA (3rd class reduplicated)'),
('si',      '',    FALSE, 'Present', '2S', 'P', 'asi -> as'),
('thaH',    '',    FALSE, 'Present', '2D', 'P', 'sthaH -> as'),
('tha',     '',    FALSE, 'Present', '2P', 'P', 'stha -> as'),
('mi',      '',    FALSE, 'Present', '1S', 'P', 'asmi -> as, kurmi -> kR'),
('vaH',     '',    FALSE, 'Present', '1D', 'P', 'svaH -> as'),
('maH',     '',    FALSE, 'Present', '1P', 'P', 'smaH -> as'),
('te',      '',    FALSE, 'Present', '3S', 'A', 'kurute -> kR, datte -> dA'),
('Ate',     '',    FALSE, 'Present', '3D', 'A', 'kurvAte -> kR'),
('ate',     '',    FALSE, 'Present', '3P', 'A', 'kurvate -> kR'),
('se',      '',    FALSE, 'Present', '2S', 'A', 'kuruSe -> kR'),
('Athe',    '',    FALSE, 'Present', '2D', 'A', 'kurvAthe -> kR'),
('dhve',    '',    FALSE, 'Present', '2P', 'A', 'kurudhve -> kR'),
('e',       '',    FALSE, 'Present', '1S', 'A', 'kurve -> kR'),
('vahe',    '',    FALSE, 'Present', '1D', 'A', 'kurvahe -> kR'),
('mahe',    '',    FALSE, 'Present', '1P', 'A', 'kurmahe -> kR'),
('ate',     'a',   FALSE, 'Present', '3S', 'A', 'labhate -> labh, labhyate -> labh (Passive)'),
('ete',     'a',   FALSE, 'Present', '3D', 'A', 'labhete -> labh'),
('ante',    'a',   FALSE, 'Present', '3P', 'A', 'labhante -> labh'),
('ase',     'a',   FALSE, 'Present', '2S', 'A', 'labhase -> labh'),
('ethe',    'a',   FALSE, 'Present', '2D', 'A', 'labhethE -> labh'),
('adhve',   'a',   FALSE, 'Present', '2P', 'A', 'labhadhve -> labh'),
('e',       'a',   FALSE, 'Present', '1S', 'A', 'labhe -> labh'),
('Avahe',   'a',   FALSE, 'Present', '1D', 'A', 'labhAvahe -> labh'),
('Amahe',   'a',   FALSE, 'Present', '1P', 'A', 'labhAmahe -> labh'),
('at',      'a',   TRUE,  'Imperfect', '3S', 'P', 'abhavat -> bhava'),
('atAm',    'a',   TRUE,  'Imperfect', '3D', 'P', 'abhavatAm -> bhava'),
('an',      'a',   TRUE,  'Imperfect', '3P', 'P', 'abhavan -> bhava'),
('aH',      'a',   TRUE,  'Imperfect', '2S', 'P', 'abhavaH -> bhava'),
('atam',    'a',   TRUE,  'Imperfect', '2D', 'P', 'abhavatam -> bhava'),
('ata',     'a',   TRUE,  'Imperfect', '2P', 'P', 'abhavata -> bhava'),
('am',      'a',   TRUE,  'Imperfect', '1S', 'P', 'abhavam -> bhava'),
('Ava',     'a',   TRUE,  'Imperfect', '1D', 'P', 'abhavAva -> bhava'),
('Ama',     'a',   TRUE,  'Imperfect', '1P', 'P', 'abhavAma -> bhava'),
('t',       '',    TRUE,  'Imperfect', '3S', 'P', 'AsIt -> as, adAt -> dA'),
('tAm',     '',    TRUE,  'Imperfect', '3D', 'P', 'AstAm -> as'),
('an',      '',    TRUE,  'Imperfect', '3P', 'P', 'Asan -> as'),
('uH',      '',    TRUE,  'Imperfect', '3P', 'P', 'adur -> dA'),
('H',       '',    TRUE,  'Imperfect', '2S', 'P', 'AsIH -> as'),
('tam',     '',    TRUE,  'Imperfect', '2D', 'P', 'Astam -> as'),
('ta',      '',    TRUE,  'Imperfect', '2P', 'P', 'Asta -> as'),
('am',      '',    TRUE,  'Imperfect', '1S', 'P', 'Asam -> as'),
('va',      '',    TRUE,  'Imperfect', '1D', 'P', 'Asva -> as'),
('ma',      '',    TRUE,  'Imperfect', '1P', 'P', 'Asma -> as'),
('ata',     'a',   TRUE,  'Imperfect', '3S', 'A', 'alabhata -> labh'),
('etAm',    'a',   TRUE,  'Imperfect', '3D', 'A', 'alabhetAm -> labh'),
('anta',    'a',   TRUE,  'Imperfect', '3P', 'A', 'alabhanta -> labh'),
('athAH',   'a',   TRUE,  'Imperfect', '2S', 'A', 'alabhathAH -> labh'),
('ethAm',   'a',   TRUE,  'Imperfect', '2D', 'A', 'alabhethAm -> labh'),
('adhvam',  'a',   TRUE,  'Imperfect', '2P', 'A', 'alabhadhvam -> labh'),
('e',       'a',   TRUE,  'Imperfect', '1S', 'A', 'alabhe -> labh'),
('Avahi',   'a',   TRUE,  'Imperfect', '1D', 'A', 'alabhAvahi -> labh'),
('Amahi',   'a',   TRUE,  'Imperfect', '1P', 'A', 'alabhAmahi -> labh'),
('ta',      '',    TRUE,  'Imperfect', '3S', 'A', 'akuruta -> kR'),
('AtAm',    '',    TRUE,  'Imperfect', '3D', 'A', 'akurvAtAm -> kR'),
('ata',     '',    TRUE,  'Imperfect', '3P', 'A', 'akurvata -> kR'),
('thAH',    '',    TRUE,  'Imperfect', '2S', 'A', 'akurutH -> kR'),
('AthAm',   '',    TRUE,  'Imperfect', '2D', 'A', 'akurvAthAm -> kR'),
('dhvam',   '',    TRUE,  'Imperfect', '2P', 'A', 'akurudhvam -> kR'),
('i',       '',    TRUE,  'Imperfect', '1S', 'A', 'akurvi -> kR'),
('vahi',    '',    TRUE,  'Imperfect', '1D', 'A', 'akurvahi -> kR'),
('mahi',    '',    TRUE,  'Imperfect', '1P', 'A', 'akurmahi -> kR'),
('atu',     'a',   FALSE, 'Imperative', '3S', 'P', 'bhavatu -> bhava'),
('atAm',    'a',   FALSE, 'Imperative', '3D', 'P', 'bhavatAm -> bhava'),
('antu',    'a',   FALSE, 'Imperative', '3P', 'P', 'bhavantu -> bhava'),
('a',       'a',   FALSE, 'Imperative', '2S', 'P', 'bhava -> bhava'),
('atam',    'a',   FALSE, 'Imperative', '2D', 'P', 'bhavatam -> bhava'),
('ata',     'a',   FALSE, 'Imperative', '2P', 'P', 'bhavata -> bhava'),
('Ani',     'a',   FALSE, 'Imperative', '1S', 'P', 'bhavAni -> bhava'),
('Ava',     'a',   FALSE, 'Imperative', '1D', 'P', 'bhavAva -> bhava'),
('Ama',     'a',   FALSE, 'Imperative', '1P', 'P', 'bhavAma -> bhava'),
('atAm',    'a',   FALSE, 'Imperative', '3S', 'A', 'labhatAm -> labh'),
('etAm',    'a',   FALSE, 'Imperative', '3D', 'A', 'labhetAm -> labh'),
('antAm',   'a',   FALSE, 'Imperative', '3P', 'A', 'labhantAm -> labh'),
('asva',    'a',   FALSE, 'Imperative', '2S', 'A', 'labhasva -> labh'),
('ethAm',   'a',   FALSE, 'Imperative', '2D', 'A', 'labhethAm -> labh'),
('adhvam',  'a',   FALSE, 'Imperative', '2P', 'A', 'labhadhvam -> labh'),
('E',       'a',   FALSE, 'Imperative', '1S', 'A', 'labhE -> labh'),
('AvahE',   'a',   FALSE, 'Imperative', '1D', 'A', 'labhavAhE -> labh'),
('AmahE',   'a',   FALSE, 'Imperative', '1P', 'A', 'labhamAhE -> labh'),
('et',      'a',   FALSE, 'Optative', '3S', 'P', 'bhavet -> bhava'),
('etAm',    'a',   FALSE, 'Optative', '3D', 'P', 'bhavetAm -> bhava'),
('eyuH',    'a',   FALSE, 'Optative', '3P', 'P', 'bhaveyuH -> bhava'),
('eH',      'a',   FALSE, 'Optative', '2S', 'P', 'bhaveH -> bhava'),
('etam',    'a',   FALSE, 'Optative', '2D', 'P', 'bhavetam -> bhava'),
('eta',     'a',   FALSE, 'Optative', '2P', 'P', 'bhaveta -> bhava'),
('eyam',    'a',   FALSE, 'Optative', '1S', 'P', 'bhaveyam -> bhava'),
('eva',     'a',   FALSE, 'Optative', '1D', 'P', 'bhaveva -> bhava'),
('ema',     'a',   FALSE, 'Optative', '1P', 'P', 'bhavema -> bhava'),
('eta',     'a',   FALSE, 'Optative', '3S', 'A', 'labheta -> labh'),
('eyAtAm',  'a',   FALSE, 'Optative', '3D', 'A', 'labheyAtAm -> labh'),
('eran',    'a',   FALSE, 'Optative', '3P', 'A', 'labheran -> labh'),
('ethAH',   'a',   FALSE, 'Optative', '2S', 'A', 'labhethAH -> labh'),
('eyAthAm', 'a',   FALSE, 'Optative', '2D', 'A', 'labheyAthAm -> labh'),
('edhvam',  'a',   FALSE, 'Optative', '2P', 'A', 'labhedhvam -> labh'),
('eya',     'a',   FALSE, 'Optative', '1S', 'A', 'labheya -> labh'),
('evahi',   'a',   FALSE, 'Optative', '1D', 'A', 'labhevahi -> labh'),
('emahi',   'a',   FALSE, 'Optative', '1P', 'A', 'labhemahi -> labh'),
('iSyati',  'a',   FALSE, 'Future', '3S', 'P', 'bhaviSyati -> bhava'),
('iSyataH', 'a',   FALSE, 'Future', '3D', 'P', 'bhaviSyataH -> bhava'),
('iSyanti', 'a',   FALSE, 'Future', '3P', 'P', 'bhaviSyanti -> bhava'),
('iSyasi',  'a',   FALSE, 'Future', '2S', 'P', 'bhaviSyasi -> bhava'),
('iSyathaH','a',   FALSE, 'Future', '2D', 'P', 'bhaviSyathaH -> bhava'),
('iSyatha', 'a',   FALSE, 'Future', '2P', 'P', 'bhaviSyatha -> bhava'),
('iSyAmi',  'a',   FALSE, 'Future', '1S', 'P', 'bhaviSyAmi -> bhava'),
('iSyAvaH', 'a',   FALSE, 'Future', '1D', 'P', 'bhaviSyAvaH -> bhava'),
('iSyAmaH', 'a',   FALSE, 'Future', '1P', 'P', 'bhaviSyAmaH -> bhava'),
('Syati',   '',    FALSE, 'Future', '3S', 'P', 'dAsyati -> dA, nekSyati -> nI'),
('SyataH',  '',    FALSE, 'Future', '3D', 'P', 'dAsyataH -> dA'),
('Syanti',  '',    FALSE, 'Future', '3P', 'P', 'dAsyanti -> dA'),
('Syasi',   '',    FALSE, 'Future', '2S', 'P', 'dAsyasi -> dA'),
('SyAmi',   '',    FALSE, 'Future', '1S', 'P', 'dAsyAmi -> dA'),
('iSyate',  'a',   FALSE, 'Future', '3S', 'A', 'bhaviSyate -> bhava'),
('iSyante', 'a',   FALSE, 'Future', '3P', 'A', 'bhaviSyante -> bhava'),
('Syate',   '',    FALSE, 'Future', '3S', 'A', 'dAsyate -> dA'),
('a',       '',    FALSE, 'Perfect', '3S/1S', 'P', 'babhoo-va -> bhoo, tatAna -> tan'),
('atuH',    '',    FALSE, 'Perfect', '3D', 'P', 'babhoovatuH -> bhoo'),
('uH',      '',    FALSE, 'Perfect', '3P', 'P', 'babhoovuH -> bhoo'),
('itha',    '',    FALSE, 'Perfect', '2S', 'P', 'babhoovitha -> bhoo'),
('athaH',   '',    FALSE, 'Perfect', '2D', 'P', 'babhoovathaH -> bhoo'),
('a',       '',    FALSE, 'Perfect', '2P', 'P', 'babhoova -> bhoo'),
('va',      '',    FALSE, 'Perfect', '1D', 'P', 'babhooviva -> bhoo'),
('ma',      '',    FALSE, 'Perfect', '1P', 'P', 'babhoovima -> bhoo'),
('e',       '',    FALSE, 'Perfect', '3S/1S', 'A', 'tenikre -> kR'),
('Ate',     '',    FALSE, 'Perfect', '3D', 'A', 'cakrAte -> kR'),
('ire',     '',    FALSE, 'Perfect', '3P', 'A', 'cakrire -> kR'),
('se',      '',    FALSE, 'Perfect', '2S', 'A', 'cakriSe -> kR'),
('tum',     '',    FALSE, 'Infinitive', 'Inf', 'any', 'gantum -> gam, dAtum -> dA'),
('itum',    '',    FALSE, 'Infinitive', 'Inf', 'any', 'bhavitum -> bhū, likhitum -> likh'),
('tvA',     '',    FALSE, 'Gerund', 'Abs', 'any', 'kRtvA -> kR, gtvA -> gam'),
('itvA',    '',    FALSE, 'Gerund', 'Abs', 'any', 'likhitvA -> likh, bhavitvA -> bhū'),
('ya',      '',    FALSE, 'Gerund', 'Abs_Pref', 'any', 'Agatya -> A-gam, vijitya -> vi-ji'),
('tya',     '',    FALSE, 'Gerund', 'Abs_Pref', 'any', 'kRtya -> kR, srutya -> śru'),
('taH',     '',    FALSE, 'PPP', 'Masc_NomSg', 'any', 'kRtaH -> kR, gataH -> gam'),
('tam',     '',    FALSE, 'PPP', 'Neut_NomSg', 'any', 'kRtam -> kR, gatam -> gam'),
('tA',      '',    FALSE, 'PPP', 'Fem_NomSg', 'any', 'kRtA -> kR, gatA -> gam'),
('itaH',    '',    FALSE, 'PPP', 'Masc_NomSg', 'any', 'likhitaH -> likh'),
('itam',    '',    FALSE, 'PPP', 'Neut_NomSg', 'any', 'likhitam -> likh'),
('itA',     '',    FALSE, 'PPP', 'Fem_NomSg', 'any', 'likhitA -> likh'),
('naH',     '',    FALSE, 'PPP', 'Masc_NomSg', 'any', 'bhinnaH -> bhid, chinnaH -> chid'),
('nam',     '',    FALSE, 'PPP', 'Neut_NomSg', 'any', 'bhinnam -> bhid'),
('nA',      '',    FALSE, 'PPP', 'Fem_NomSg', 'any', 'bhinnA -> bhid'),
('tavAn',   '',    FALSE, 'PPA', 'Masc_NomSg', 'any', 'kRtavAn -> kR, gatavAn -> gam'),
('tavat',   '',    FALSE, 'PPA', 'Neut_NomSg', 'any', 'kRtavat -> kR'),
('tavatI',  '',    FALSE, 'PPA', 'Fem_NomSg', 'any', 'kRtavatI -> kR'),
('itavAn',  '',    FALSE, 'PPA', 'Masc_NomSg', 'any', 'likhitavAn -> likh'),
('an',      'a',   FALSE, 'PPR_Act', 'Masc_NomSg', 'P', 'gacchan -> gam, bhavan -> bhū'),
('at',      'a',   FALSE, 'PPR_Act', 'Neut_NomSg', 'P', 'gacchat -> gam'),
('antI',    'a',   FALSE, 'PPR_Act', 'Fem_NomSg', 'P', 'gacchantI -> gam'),
('mAnaH',   'a',   FALSE, 'PPR_Mid', 'Masc_NomSg', 'A', 'labhamAnaH -> labh, kurvANaH -> kR'),
('mAnam',   'a',   FALSE, 'PPR_Mid', 'Neut_NomSg', 'A', 'labhamAnam -> labh'),
('mAnA',    'a',   FALSE, 'PPR_Mid', 'Fem_NomSg', 'A', 'labhamAnA -> labh'),
('AnaH',    '',    FALSE, 'PPR_Mid', 'Masc_NomSg', 'A', 'kurvAnaH -> kR, dadAnaH -> dA'),
('tavyaH',  '',    FALSE, 'Gerundive', 'Masc_NomSg', 'any', 'kartavyaH -> kR, bhavitavyaH -> bhū'),
('tavyam',  '',    FALSE, 'Gerundive', 'Neut_NomSg', 'any', 'kartavyam -> kR'),
('tavyA',   '',    FALSE, 'Gerundive', 'Fem_NomSg', 'any', 'kartavyA -> kR'),
('anIyaH',  '',    FALSE, 'Gerundive', 'Masc_NomSg', 'any', 'karaNIyaH -> kR, bhavanIyaH -> bhū'),
('anIyam',  '',    FALSE, 'Gerundive', 'Neut_NomSg', 'any', 'karaNIyam -> kR'),
('anIyA',   '',    FALSE, 'Gerundive', 'Fem_NomSg', 'any', 'karaNIyA -> kR'),
('yaH',     '',    FALSE, 'Gerundive', 'Masc_NomSg', 'any', 'kAryaH -> kR, jñeyaH -> jñā'),
('yam',     '',    FALSE, 'Gerundive', 'Neut_NomSg', 'any', 'kAryam -> kR'),
('yA',      '',    FALSE, 'Gerundive', 'Fem_NomSg', 'any', 'kAryA -> kR')
ON CONFLICT DO NOTHING;

SELECT setval('"lingua"."verbal_endings_id_seq"', 220, true);
