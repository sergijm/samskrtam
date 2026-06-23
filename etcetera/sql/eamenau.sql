/*
 Navicat Premium Data Transfer

 Source Server         : mdm-dev-vm
 Source Server Type    : PostgreSQL
 Source Server Version : 170009 (170009)
 Source Host           : mdm-dev:5432
 Source Catalog        : samskrtam
 Source Schema         : eamenau

 Target Server Type    : PostgreSQL
 Target Server Version : 170009 (170009)
 File Encoding         : 65001

 Date: 23/06/2026 06:10:40
*/


-- ----------------------------
-- Sequence structure for answers_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."answers_id_seq";
CREATE SEQUENCE "eamenau"."answers_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for aspiration_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."aspiration_id_seq";
CREATE SEQUENCE "eamenau"."aspiration_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for exercises_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."exercises_id_seq";
CREATE SEQUENCE "eamenau"."exercises_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for manner_of_articulation_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."manner_of_articulation_id_seq";
CREATE SEQUENCE "eamenau"."manner_of_articulation_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for phonemes_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."phonemes_id_seq";
CREATE SEQUENCE "eamenau"."phonemes_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for place_of_articulation_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."place_of_articulation_id_seq";
CREATE SEQUENCE "eamenau"."place_of_articulation_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for sandhi_rules_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."sandhi_rules_id_seq";
CREATE SEQUENCE "eamenau"."sandhi_rules_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for solution_sandhi_rules_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."solution_sandhi_rules_id_seq";
CREATE SEQUENCE "eamenau"."solution_sandhi_rules_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for solutions_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."solutions_id_seq";
CREATE SEQUENCE "eamenau"."solutions_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for tasks_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."tasks_id_seq";
CREATE SEQUENCE "eamenau"."tasks_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for varga_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."varga_id_seq";
CREATE SEQUENCE "eamenau"."varga_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for voicing_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "eamenau"."voicing_id_seq";
CREATE SEQUENCE "eamenau"."voicing_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Table structure for answers
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."answers";
CREATE TABLE "eamenau"."answers" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".answers_id_seq'::regclass),
  "task_id" int4 NOT NULL,
  "answer_text" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for aspiration
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."aspiration";
CREATE TABLE "eamenau"."aspiration" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".aspiration_id_seq'::regclass),
  "latin_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "latin_abbr" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "english_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "russian_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for exercises
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."exercises";
CREATE TABLE "eamenau"."exercises" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".exercises_id_seq'::regclass),
  "exercise_number" int4 NOT NULL,
  "exercise_letter" varchar(10) COLLATE "pg_catalog"."default",
  "instruction_text" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for manner_of_articulation
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."manner_of_articulation";
CREATE TABLE "eamenau"."manner_of_articulation" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".manner_of_articulation_id_seq'::regclass),
  "latin_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "latin_abbr" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "english_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "russian_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for phonemes
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."phonemes";
CREATE TABLE "eamenau"."phonemes" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".phonemes_id_seq'::regclass),
  "iast_symbol" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "harvard_kyoto_symbol" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "devanagari_symbol" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "name" varchar(50) COLLATE "pg_catalog"."default",
  "place_id" int4,
  "manner_id" int4,
  "voicing_id" int4,
  "aspiration_id" int4,
  "varga_id" int4,
  "is_nasal" bool DEFAULT false
)
;

-- ----------------------------
-- Table structure for place_of_articulation
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."place_of_articulation";
CREATE TABLE "eamenau"."place_of_articulation" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".place_of_articulation_id_seq'::regclass),
  "latin_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "latin_abbr" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "english_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "russian_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for sandhi_rules
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."sandhi_rules";
CREATE TABLE "eamenau"."sandhi_rules" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".sandhi_rules_id_seq'::regclass),
  "rule_number" int4 NOT NULL,
  "rule_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "short_description" text COLLATE "pg_catalog"."default",
  "whitney_number" varchar(20) COLLATE "pg_catalog"."default",
  "iast_example" text COLLATE "pg_catalog"."default",
  "hk_example" text COLLATE "pg_catalog"."default",
  "notes" text COLLATE "pg_catalog"."default",
  "full_text" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for sandhi_rules_group
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."sandhi_rules_group";
CREATE TABLE "eamenau"."sandhi_rules_group" (
  "id" int4 NOT NULL,
  "description" text COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(255) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for sandhi_rules_group_map
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."sandhi_rules_group_map";
CREATE TABLE "eamenau"."sandhi_rules_group_map" (
  "sandhi_rules_id" int4 NOT NULL,
  "sandhi_rules_group_id" int4 NOT NULL
)
;

-- ----------------------------
-- Table structure for solution_sandhi_rules
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."solution_sandhi_rules";
CREATE TABLE "eamenau"."solution_sandhi_rules" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".solution_sandhi_rules_id_seq'::regclass),
  "solution_id" int4 NOT NULL,
  "sandhi_rule_id" int4 NOT NULL,
  "position_order" int4 DEFAULT 0
)
;

-- ----------------------------
-- Table structure for solutions
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."solutions";
CREATE TABLE "eamenau"."solutions" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".solutions_id_seq'::regclass),
  "task_id" int4 NOT NULL,
  "solution_text" text COLLATE "pg_catalog"."default" NOT NULL,
  "step_by_step" text COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT now(),
  "is_correct" bool DEFAULT false
)
;

-- ----------------------------
-- Table structure for tasks
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."tasks";
CREATE TABLE "eamenau"."tasks" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".tasks_id_seq'::regclass),
  "exercise_id" int4 NOT NULL,
  "task_number" int4 NOT NULL,
  "task_text" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for varga
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."varga";
CREATE TABLE "eamenau"."varga" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".varga_id_seq'::regclass),
  "number" int4 NOT NULL,
  "latin_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "latin_abbr" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "english_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "russian_name" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "devanagari_symbol" varchar(10) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for voicing
-- ----------------------------
DROP TABLE IF EXISTS "eamenau"."voicing";
CREATE TABLE "eamenau"."voicing" (
  "id" int4 NOT NULL DEFAULT nextval('"eamenau".voicing_id_seq'::regclass),
  "latin_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "latin_abbr" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "english_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "russian_name" varchar(30) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- View structure for phonemes_flat
-- ----------------------------
DROP VIEW IF EXISTS "eamenau"."phonemes_flat";
CREATE VIEW "eamenau"."phonemes_flat" AS  SELECT p.iast_symbol,
    p.harvard_kyoto_symbol,
    p.devanagari_symbol,
    p.name,
    pla.latin_name AS place_latin,
    pla.latin_abbr AS place_abbr,
    pla.english_name AS place_english,
    pla.russian_name AS place_russian,
    ma.latin_name AS manner_latin,
    ma.latin_abbr AS manner_abbr,
    ma.english_name AS manner_english,
    ma.russian_name AS manner_russian,
    v.latin_name AS voicing_latin,
    v.latin_abbr AS voicing_abbr,
    v.english_name AS voicing_english,
    v.russian_name AS voicing_russian,
    a.latin_name AS aspiration_latin,
    a.latin_abbr AS aspiration_abbr,
    a.english_name AS aspiration_english,
    a.russian_name AS aspiration_russian,
    vg.number AS varga_number,
    vg.latin_name AS varga_latin,
    vg.english_name AS varga_english,
    vg.russian_name AS varga_russian,
    vg.devanagari_symbol AS varga_symbol,
    p.is_nasal
   FROM eamenau.phonemes p
     LEFT JOIN eamenau.place_of_articulation pla ON p.place_id = pla.id
     LEFT JOIN eamenau.manner_of_articulation ma ON p.manner_id = ma.id
     LEFT JOIN eamenau.voicing v ON p.voicing_id = v.id
     LEFT JOIN eamenau.aspiration a ON p.aspiration_id = a.id
     LEFT JOIN eamenau.varga vg ON p.varga_id = vg.id
  ORDER BY (
        CASE
            WHEN p.manner_id = ANY (ARRAY[1, 2]) THEN 0
            ELSE 1
        END), (COALESCE(vg.number, 999)), p.id;

-- ----------------------------
-- View structure for exercises_flat
-- ----------------------------
DROP VIEW IF EXISTS "eamenau"."exercises_flat";
CREATE VIEW "eamenau"."exercises_flat" AS  SELECT e.id AS exercise_id,
    e.exercise_number,
    e.exercise_letter,
    e.instruction_text AS exercise_instruction,
    t.id AS task_id,
    t.task_number,
    t.task_text,
        CASE
            WHEN e.exercise_letter IS NOT NULL AND e.exercise_letter::text <> ''::text THEN (((e.exercise_number || ' '::text) || e.exercise_letter::text) || '.'::text) || t.task_number
            ELSE (e.exercise_number || '.'::text) || t.task_number
        END AS full_task_number
   FROM eamenau.exercises e
     JOIN eamenau.tasks t ON e.id = t.exercise_id
  ORDER BY e.exercise_number, (COALESCE(e.exercise_letter, ' '::character varying)), t.task_number;

-- ----------------------------
-- View structure for answers_flat
-- ----------------------------
DROP VIEW IF EXISTS "eamenau"."answers_flat";
CREATE VIEW "eamenau"."answers_flat" AS  SELECT e.exercise_number,
    e.exercise_letter,
    t.task_number,
    t.task_text,
    a.answer_text,
        CASE
            WHEN e.exercise_letter IS NOT NULL AND e.exercise_letter::text <> ''::text THEN (((e.exercise_number || ' '::text) || e.exercise_letter::text) || '.'::text) || t.task_number
            ELSE (e.exercise_number || '.'::text) || t.task_number
        END AS full_task_number
   FROM eamenau.exercises e
     JOIN eamenau.tasks t ON e.id = t.exercise_id
     JOIN eamenau.answers a ON t.id = a.task_id
  ORDER BY e.exercise_number, (COALESCE(e.exercise_letter, ' '::character varying)), t.task_number;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."answers_id_seq"
OWNED BY "eamenau"."answers"."id";
SELECT setval('"eamenau"."answers_id_seq"', 465, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."aspiration_id_seq"
OWNED BY "eamenau"."aspiration"."id";
SELECT setval('"eamenau"."aspiration_id_seq"', 2, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."exercises_id_seq"
OWNED BY "eamenau"."exercises"."id";
SELECT setval('"eamenau"."exercises_id_seq"', 43, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."manner_of_articulation_id_seq"
OWNED BY "eamenau"."manner_of_articulation"."id";
SELECT setval('"eamenau"."manner_of_articulation_id_seq"', 8, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."phonemes_id_seq"
OWNED BY "eamenau"."phonemes"."id";
SELECT setval('"eamenau"."phonemes_id_seq"', 48, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."place_of_articulation_id_seq"
OWNED BY "eamenau"."place_of_articulation"."id";
SELECT setval('"eamenau"."place_of_articulation_id_seq"', 6, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."sandhi_rules_id_seq"
OWNED BY "eamenau"."sandhi_rules"."id";
SELECT setval('"eamenau"."sandhi_rules_id_seq"', 182, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."solution_sandhi_rules_id_seq"
OWNED BY "eamenau"."solution_sandhi_rules"."id";
SELECT setval('"eamenau"."solution_sandhi_rules_id_seq"', 471, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."solutions_id_seq"
OWNED BY "eamenau"."solutions"."id";
SELECT setval('"eamenau"."solutions_id_seq"', 3262, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."tasks_id_seq"
OWNED BY "eamenau"."tasks"."id";
SELECT setval('"eamenau"."tasks_id_seq"', 466, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."varga_id_seq"
OWNED BY "eamenau"."varga"."id";
SELECT setval('"eamenau"."varga_id_seq"', 5, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "eamenau"."voicing_id_seq"
OWNED BY "eamenau"."voicing"."id";
SELECT setval('"eamenau"."voicing_id_seq"', 3, true);

-- ----------------------------
-- Indexes structure for table answers
-- ----------------------------
CREATE INDEX "idx_answers_task_id" ON "eamenau"."answers" USING btree (
  "task_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table answers
-- ----------------------------
ALTER TABLE "eamenau"."answers" ADD CONSTRAINT "answers_task_id_key" UNIQUE ("task_id");

-- ----------------------------
-- Primary Key structure for table answers
-- ----------------------------
ALTER TABLE "eamenau"."answers" ADD CONSTRAINT "answers_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table aspiration
-- ----------------------------
ALTER TABLE "eamenau"."aspiration" ADD CONSTRAINT "aspiration_latin_abbr_key" UNIQUE ("latin_abbr");

-- ----------------------------
-- Primary Key structure for table aspiration
-- ----------------------------
ALTER TABLE "eamenau"."aspiration" ADD CONSTRAINT "aspiration_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table exercises
-- ----------------------------
ALTER TABLE "eamenau"."exercises" ADD CONSTRAINT "exercises_exercise_number_exercise_letter_key" UNIQUE ("exercise_number", "exercise_letter");

-- ----------------------------
-- Primary Key structure for table exercises
-- ----------------------------
ALTER TABLE "eamenau"."exercises" ADD CONSTRAINT "exercises_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table manner_of_articulation
-- ----------------------------
ALTER TABLE "eamenau"."manner_of_articulation" ADD CONSTRAINT "manner_of_articulation_latin_abbr_key" UNIQUE ("latin_abbr");

-- ----------------------------
-- Primary Key structure for table manner_of_articulation
-- ----------------------------
ALTER TABLE "eamenau"."manner_of_articulation" ADD CONSTRAINT "manner_of_articulation_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table phonemes
-- ----------------------------
ALTER TABLE "eamenau"."phonemes" ADD CONSTRAINT "phonemes_iast_symbol_key" UNIQUE ("iast_symbol");
ALTER TABLE "eamenau"."phonemes" ADD CONSTRAINT "phonemes_harvard_kyoto_symbol_key" UNIQUE ("harvard_kyoto_symbol");

-- ----------------------------
-- Primary Key structure for table phonemes
-- ----------------------------
ALTER TABLE "eamenau"."phonemes" ADD CONSTRAINT "phonemes_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table place_of_articulation
-- ----------------------------
ALTER TABLE "eamenau"."place_of_articulation" ADD CONSTRAINT "place_of_articulation_latin_abbr_key" UNIQUE ("latin_abbr");

-- ----------------------------
-- Primary Key structure for table place_of_articulation
-- ----------------------------
ALTER TABLE "eamenau"."place_of_articulation" ADD CONSTRAINT "place_of_articulation_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table sandhi_rules
-- ----------------------------
CREATE INDEX "idx_rule_number" ON "eamenau"."sandhi_rules" USING btree (
  "rule_number" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_rule_type" ON "eamenau"."sandhi_rules" USING btree (
  "rule_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table sandhi_rules
-- ----------------------------
ALTER TABLE "eamenau"."sandhi_rules" ADD CONSTRAINT "sandhi_rules_rule_number_key" UNIQUE ("rule_number");

-- ----------------------------
-- Primary Key structure for table sandhi_rules
-- ----------------------------
ALTER TABLE "eamenau"."sandhi_rules" ADD CONSTRAINT "sandhi_rules_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table sandhi_rules_group
-- ----------------------------
ALTER TABLE "eamenau"."sandhi_rules_group" ADD CONSTRAINT "sаndhi_rules_group_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table solution_sandhi_rules
-- ----------------------------
CREATE INDEX "idx_solution_rules_rule_id" ON "eamenau"."solution_sandhi_rules" USING btree (
  "sandhi_rule_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_solution_rules_solution_id" ON "eamenau"."solution_sandhi_rules" USING btree (
  "solution_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table solution_sandhi_rules
-- ----------------------------
ALTER TABLE "eamenau"."solution_sandhi_rules" ADD CONSTRAINT "solution_sandhi_rules_solution_id_sandhi_rule_id_key" UNIQUE ("solution_id", "sandhi_rule_id");

-- ----------------------------
-- Primary Key structure for table solution_sandhi_rules
-- ----------------------------
ALTER TABLE "eamenau"."solution_sandhi_rules" ADD CONSTRAINT "solution_sandhi_rules_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table solutions
-- ----------------------------
CREATE INDEX "idx_solutions_is_correct" ON "eamenau"."solutions" USING btree (
  "is_correct" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_solutions_task_id" ON "eamenau"."solutions" USING btree (
  "task_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table solutions
-- ----------------------------
ALTER TABLE "eamenau"."solutions" ADD CONSTRAINT "solutions_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table tasks
-- ----------------------------
ALTER TABLE "eamenau"."tasks" ADD CONSTRAINT "tasks_exercise_id_task_number_key" UNIQUE ("exercise_id", "task_number");

-- ----------------------------
-- Primary Key structure for table tasks
-- ----------------------------
ALTER TABLE "eamenau"."tasks" ADD CONSTRAINT "tasks_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table varga
-- ----------------------------
ALTER TABLE "eamenau"."varga" ADD CONSTRAINT "varga_number_key" UNIQUE ("number");
ALTER TABLE "eamenau"."varga" ADD CONSTRAINT "varga_latin_abbr_key" UNIQUE ("latin_abbr");

-- ----------------------------
-- Primary Key structure for table varga
-- ----------------------------
ALTER TABLE "eamenau"."varga" ADD CONSTRAINT "varga_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table voicing
-- ----------------------------
ALTER TABLE "eamenau"."voicing" ADD CONSTRAINT "voicing_latin_abbr_key" UNIQUE ("latin_abbr");

-- ----------------------------
-- Primary Key structure for table voicing
-- ----------------------------
ALTER TABLE "eamenau"."voicing" ADD CONSTRAINT "voicing_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table answers
-- ----------------------------
ALTER TABLE "eamenau"."answers" ADD CONSTRAINT "answers_task_id_fkey" FOREIGN KEY ("task_id") REFERENCES "eamenau"."tasks" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table phonemes
-- ----------------------------
ALTER TABLE "eamenau"."phonemes" ADD CONSTRAINT "phonemes_aspiration_id_fkey" FOREIGN KEY ("aspiration_id") REFERENCES "eamenau"."aspiration" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "eamenau"."phonemes" ADD CONSTRAINT "phonemes_manner_id_fkey" FOREIGN KEY ("manner_id") REFERENCES "eamenau"."manner_of_articulation" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "eamenau"."phonemes" ADD CONSTRAINT "phonemes_place_id_fkey" FOREIGN KEY ("place_id") REFERENCES "eamenau"."place_of_articulation" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "eamenau"."phonemes" ADD CONSTRAINT "phonemes_varga_id_fkey" FOREIGN KEY ("varga_id") REFERENCES "eamenau"."varga" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "eamenau"."phonemes" ADD CONSTRAINT "phonemes_voicing_id_fkey" FOREIGN KEY ("voicing_id") REFERENCES "eamenau"."voicing" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table sandhi_rules_group_map
-- ----------------------------
ALTER TABLE "eamenau"."sandhi_rules_group_map" ADD CONSTRAINT "gandhi_rules_group_map_sandhi_rules_group_id_fkey" FOREIGN KEY ("sandhi_rules_group_id") REFERENCES "eamenau"."sandhi_rules_group" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "eamenau"."sandhi_rules_group_map" ADD CONSTRAINT "gandhi_rules_group_map_sandhi_rules_id_fkey" FOREIGN KEY ("sandhi_rules_id") REFERENCES "eamenau"."sandhi_rules" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table solution_sandhi_rules
-- ----------------------------
ALTER TABLE "eamenau"."solution_sandhi_rules" ADD CONSTRAINT "solution_sandhi_rules_solution_id_fkey" FOREIGN KEY ("solution_id") REFERENCES "eamenau"."solutions" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table solutions
-- ----------------------------
ALTER TABLE "eamenau"."solutions" ADD CONSTRAINT "solutions_task_id_fkey" FOREIGN KEY ("task_id") REFERENCES "eamenau"."tasks" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table tasks
-- ----------------------------
ALTER TABLE "eamenau"."tasks" ADD CONSTRAINT "tasks_exercise_id_fkey" FOREIGN KEY ("exercise_id") REFERENCES "eamenau"."exercises" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
