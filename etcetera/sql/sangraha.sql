/*
 Navicat Premium Data Transfer

 Source Server         : localhost-pg17
 Source Server Type    : PostgreSQL
 Source Server Version : 170000 (170000)
 Source Host           : localhost:5436
 Source Catalog        : samskrtam
 Source Schema         : sangraha

 Target Server Type    : PostgreSQL
 Target Server Version : 170000 (170000)
 File Encoding         : 65001

 Date: 23/08/2026 22:09:52
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
-- Table structure for classification_scheme
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."classification_scheme";
CREATE TABLE "sangraha"."classification_scheme" (
  "code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "title_ru" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "is_active" bool NOT NULL DEFAULT true
)
;

-- ----------------------------
-- Table structure for classification_topic
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."classification_topic";
CREATE TABLE "sangraha"."classification_topic" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "classification_type" varchar(30) COLLATE "pg_catalog"."default" NOT NULL,
  "code" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "name_ru" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_code" varchar(60) COLLATE "pg_catalog"."default",
  "learning_level" varchar(10) COLLATE "pg_catalog"."default",
  "sort_order" int4
)
;

-- ----------------------------
-- Table structure for curriculum_semantic_class
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."curriculum_semantic_class";
CREATE TABLE "sangraha"."curriculum_semantic_class" (
  "code" varchar(40) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_code" varchar(40) COLLATE "pg_catalog"."default",
  "label_ru" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "label_en" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default"
)
;
COMMENT ON TABLE "sangraha"."curriculum_semantic_class" IS 'Editable mirror of the curriculum semantic taxonomy (lexical-curriculum.md §3): 9 roots (parent_code NULL) + 33 leaves = 42 rows.';

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
-- Table structure for lemma
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."lemma";
CREATE TABLE "sangraha"."lemma" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "lemma_slp1" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_iast" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_devanagari" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for lemma_classification
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."lemma_classification";
CREATE TABLE "sangraha"."lemma_classification" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "lemma_id" uuid NOT NULL,
  "scheme_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "category_code" varchar(40) COLLATE "pg_catalog"."default",
  "gloss_ru" varchar(500) COLLATE "pg_catalog"."default",
  "gloss_en" varchar(500) COLLATE "pg_catalog"."default",
  "confidence" int2,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'CANDIDATE'::character varying,
  "llm_model" varchar(100) COLLATE "pg_catalog"."default",
  "batch_id" uuid,
  "reviewed_by" varchar(100) COLLATE "pg_catalog"."default",
  "reviewed_at" timestamptz(6),
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "gender" varchar(20) COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for lemma_statistics
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."lemma_statistics";
CREATE TABLE "sangraha"."lemma_statistics" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "lemma_id" uuid NOT NULL,
  "gender" varchar(20) COLLATE "pg_catalog"."default",
  "occurrence_count" int4 NOT NULL,
  "dominant_pos_code" varchar(30) COLLATE "pg_catalog"."default",
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
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
  "analyzer_name" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "raw_prompt" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for verse_statistics
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verse_statistics";
CREATE TABLE "sangraha"."verse_statistics" (
  "verse_id" uuid NOT NULL,
  "word_count" int4 NOT NULL,
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "grammar_info" jsonb NOT NULL DEFAULT '{}'::jsonb
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
  "vocabulary_word_id" uuid,
  "lemma_id" uuid
)
;

-- ----------------------------
-- Table structure for verses
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."verses";
CREATE TABLE "sangraha"."verses" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "chapter_id" uuid,
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
  "translation_ru" text COLLATE "pg_catalog"."default",
  "owner_id" uuid
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
-- Table structure for works_class
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."works_class";
CREATE TABLE "sangraha"."works_class" (
  "id" uuid NOT NULL,
  "parent_id" uuid,
  "classification" text COLLATE "pg_catalog"."default" NOT NULL,
  "code" text COLLATE "pg_catalog"."default" NOT NULL,
  "title_sa_iast" text COLLATE "pg_catalog"."default" NOT NULL,
  "title_sa_deva" text COLLATE "pg_catalog"."default",
  "title_ru" text COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" text COLLATE "pg_catalog"."default" NOT NULL,
  "sort_order" int4 NOT NULL DEFAULT 0
)
;

-- ----------------------------
-- Table structure for works_work_class
-- ----------------------------
DROP TABLE IF EXISTS "sangraha"."works_work_class";
CREATE TABLE "sangraha"."works_work_class" (
  "work_id" uuid NOT NULL,
  "class_id" uuid NOT NULL
)
;

-- ----------------------------
-- Function structure for compute_lemma_statistics
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."compute_lemma_statistics"("p_lemma_ids" _uuid);
CREATE OR REPLACE FUNCTION "sangraha"."compute_lemma_statistics"("p_lemma_ids" _uuid=NULL::uuid[])
  RETURNS TABLE("lemma_id" uuid, "gender" varchar, "occurrence_count" int8, "dominant_pos_code" varchar) AS $BODY$
    WITH per_lemma_gender_pos AS (
        SELECT l.id        AS lemma_id,
               m.gender    AS gender,
               COALESCE(vw.pos, 'OTHER') AS pos,
               COUNT(*)    AS cnt
        FROM sangraha.verse_words vw
        JOIN sangraha.lemma l ON l.lemma_iast = vw.lemma_iast
        LEFT JOIN sangraha.verse_word_morphology m ON m.verse_word_id = vw.id
        JOIN sangraha.verses v ON v.id = vw.verse_id AND v.chapter_id IS NOT NULL AND v.deleted_at IS NULL
        WHERE p_lemma_ids IS NULL OR l.id = ANY (p_lemma_ids)
        GROUP BY l.id, m.gender, COALESCE(vw.pos, 'OTHER')
    ),
    ranked AS (
        SELECT lemma_id,
               gender,
               pos,
               cnt,
               ROW_NUMBER() OVER (
                   PARTITION BY lemma_id, gender
                   ORDER BY cnt DESC, pos ASC
               ) AS rn
        FROM per_lemma_gender_pos
    ),
    totals AS (
        SELECT lemma_id,
               gender,
               SUM(cnt) AS occurrence_count
        FROM per_lemma_gender_pos
        GROUP BY lemma_id, gender
    )
    SELECT t.lemma_id,
           t.gender,
           t.occurrence_count,
           r.pos AS dominant_pos_code
    FROM totals t
    JOIN ranked r
      ON r.lemma_id = t.lemma_id
     AND r.gender IS NOT DISTINCT FROM t.gender
     AND r.rn = 1;
$BODY$
  LANGUAGE sql VOLATILE
  COST 100
  ROWS 1000;
COMMENT ON FUNCTION "sangraha"."compute_lemma_statistics"("p_lemma_ids" _uuid) IS 'Статистика (lemma, gender) по корпусу: occurrence_count и мода POS (оконные функции). p_lemma_ids = NULL — все леммы, иначе только указанные.';

-- ----------------------------
-- Function structure for refresh_verse_statistics
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."refresh_verse_statistics"();
CREATE OR REPLACE FUNCTION "sangraha"."refresh_verse_statistics"()
  RETURNS "pg_catalog"."int4" AS $BODY$
    WITH upsert AS (
        INSERT INTO sangraha.verse_statistics (verse_id, word_count, grammar_info, updated_at)
        SELECT
            v.id,
            COUNT(w.id)::int,
            jsonb_build_object(
                'pos',        COALESCE(jsonb_agg(DISTINCT w.pos) FILTER (WHERE w.pos IS NOT NULL), '[]'::jsonb),
                'formType',   COALESCE(jsonb_agg(DISTINCT w.form_type) FILTER (WHERE w.form_type IS NOT NULL), '[]'::jsonb),
                'numberType', COALESCE(jsonb_agg(DISTINCT m.number_type) FILTER (WHERE m.number_type IS NOT NULL), '[]'::jsonb),
                'caseType',   COALESCE(jsonb_agg(DISTINCT m.case_type) FILTER (WHERE m.case_type IS NOT NULL), '[]'::jsonb),
                'gender',     COALESCE(jsonb_agg(DISTINCT m.gender) FILTER (WHERE m.gender IS NOT NULL), '[]'::jsonb),
                'person',     COALESCE(jsonb_agg(DISTINCT m.person) FILTER (WHERE m.person IS NOT NULL), '[]'::jsonb),
                'tense',      COALESCE(jsonb_agg(DISTINCT m.tense) FILTER (WHERE m.tense IS NOT NULL), '[]'::jsonb),
                'mood',       COALESCE(jsonb_agg(DISTINCT m.mood) FILTER (WHERE m.mood IS NOT NULL), '[]'::jsonb),
                'voice',      COALESCE(jsonb_agg(DISTINCT m.voice) FILTER (WHERE m.voice IS NOT NULL), '[]'::jsonb),
                'tuples',     COALESCE(jsonb_agg(DISTINCT
                    CASE WHEN nl.stem_class IS NOT NULL THEN jsonb_build_array(nl.stem_class) ELSE '[]'::jsonb END
                    || CASE WHEN m.gender IS NOT NULL THEN jsonb_build_array(m.gender) ELSE '[]'::jsonb END
                    || CASE WHEN m.case_type IS NOT NULL THEN jsonb_build_array(m.case_type) ELSE '[]'::jsonb END
                    || CASE WHEN m.number_type IS NOT NULL THEN jsonb_build_array(m.number_type) ELSE '[]'::jsonb END
                    || CASE WHEN m.person IS NOT NULL THEN jsonb_build_array(m.person) ELSE '[]'::jsonb END
                    || CASE WHEN m.tense IS NOT NULL THEN jsonb_build_array(m.tense) ELSE '[]'::jsonb END
                    || CASE WHEN m.mood IS NOT NULL THEN jsonb_build_array(m.mood) ELSE '[]'::jsonb END
                    || CASE WHEN m.voice IS NOT NULL THEN jsonb_build_array(m.voice) ELSE '[]'::jsonb END
                ), '[]'::jsonb)
            ),
            now()
        FROM sangraha.verses v
        LEFT JOIN sangraha.verse_words w ON w.verse_id = v.id
        LEFT JOIN sangraha.verse_word_morphology m ON m.verse_word_id = w.id
        LEFT JOIN sangraha.nominal_lemmas nl ON nl.lemma_iast = w.lemma_iast
        WHERE v.deleted_at IS NULL
        GROUP BY v.id
        ON CONFLICT (verse_id) DO UPDATE SET
            word_count = EXCLUDED.word_count,
            grammar_info = EXCLUDED.grammar_info,
            updated_at = now()
        RETURNING 1
    )
    SELECT COUNT(*)::int FROM upsert;
$BODY$
  LANGUAGE sql VOLATILE
  COST 100;
COMMENT ON FUNCTION "sangraha"."refresh_verse_statistics"() IS 'UPSERT verse_statistics для всех стихов. Возвращает число обновлённых строк.';

-- ----------------------------
-- Function structure for uuid_generate_v1
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_generate_v1"();
CREATE OR REPLACE FUNCTION "sangraha"."uuid_generate_v1"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v1'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_generate_v1mc
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_generate_v1mc"();
CREATE OR REPLACE FUNCTION "sangraha"."uuid_generate_v1mc"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v1mc'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_generate_v3
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_generate_v3"("namespace" uuid, "name" text);
CREATE OR REPLACE FUNCTION "sangraha"."uuid_generate_v3"("namespace" uuid, "name" text)
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v3'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_generate_v4
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_generate_v4"();
CREATE OR REPLACE FUNCTION "sangraha"."uuid_generate_v4"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v4'
  LANGUAGE c VOLATILE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_generate_v5
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_generate_v5"("namespace" uuid, "name" text);
CREATE OR REPLACE FUNCTION "sangraha"."uuid_generate_v5"("namespace" uuid, "name" text)
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_generate_v5'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_nil
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_nil"();
CREATE OR REPLACE FUNCTION "sangraha"."uuid_nil"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_nil'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_ns_dns
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_ns_dns"();
CREATE OR REPLACE FUNCTION "sangraha"."uuid_ns_dns"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_ns_dns'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_ns_oid
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_ns_oid"();
CREATE OR REPLACE FUNCTION "sangraha"."uuid_ns_oid"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_ns_oid'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_ns_url
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_ns_url"();
CREATE OR REPLACE FUNCTION "sangraha"."uuid_ns_url"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_ns_url'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Function structure for uuid_ns_x500
-- ----------------------------
DROP FUNCTION IF EXISTS "sangraha"."uuid_ns_x500"();
CREATE OR REPLACE FUNCTION "sangraha"."uuid_ns_x500"()
  RETURNS "pg_catalog"."uuid" AS '$libdir/uuid-ossp', 'uuid_ns_x500'
  LANGUAGE c IMMUTABLE STRICT
  COST 1;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "sangraha"."nominal_lemmas_id_seq"
OWNED BY "sangraha"."nominal_lemmas"."id";
SELECT setval('"sangraha"."nominal_lemmas_id_seq"', 15655, true);

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
-- Primary Key structure for table classification_scheme
-- ----------------------------
ALTER TABLE "sangraha"."classification_scheme" ADD CONSTRAINT "pk_classification_scheme" PRIMARY KEY ("code");

-- ----------------------------
-- Indexes structure for table classification_topic
-- ----------------------------
CREATE INDEX "idx_classification_topic_type" ON "sangraha"."classification_topic" USING btree (
  "classification_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table classification_topic
-- ----------------------------
ALTER TABLE "sangraha"."classification_topic" ADD CONSTRAINT "uq_classification_topic_type_code" UNIQUE ("classification_type", "code");

-- ----------------------------
-- Primary Key structure for table classification_topic
-- ----------------------------
ALTER TABLE "sangraha"."classification_topic" ADD CONSTRAINT "pk_classification_topic" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table curriculum_semantic_class
-- ----------------------------
ALTER TABLE "sangraha"."curriculum_semantic_class" ADD CONSTRAINT "pk_curriculum_semantic_class" PRIMARY KEY ("code");

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
-- Indexes structure for table lemma
-- ----------------------------
CREATE INDEX "idx_lemma_slp1" ON "sangraha"."lemma" USING btree (
  "lemma_slp1" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table lemma
-- ----------------------------
ALTER TABLE "sangraha"."lemma" ADD CONSTRAINT "uq_lemma_slp1" UNIQUE ("lemma_slp1");
ALTER TABLE "sangraha"."lemma" ADD CONSTRAINT "uq_lemma_iast" UNIQUE ("lemma_iast");

-- ----------------------------
-- Primary Key structure for table lemma
-- ----------------------------
ALTER TABLE "sangraha"."lemma" ADD CONSTRAINT "pk_lemma" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table lemma_classification
-- ----------------------------
CREATE INDEX "idx_lemma_classification_lemma" ON "sangraha"."lemma_classification" USING btree (
  "lemma_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_lemma_classification_scheme" ON "sangraha"."lemma_classification" USING btree (
  "scheme_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "lemma_classification_lemma_id_idx" ON "sangraha"."lemma_classification" USING btree (
  "lemma_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table lemma_classification
-- ----------------------------
ALTER TABLE "sangraha"."lemma_classification" ADD CONSTRAINT "uq_lemma_classification_lemma_gender_scheme" UNIQUE ("lemma_id", "gender", "scheme_code");

-- ----------------------------
-- Checks structure for table lemma_classification
-- ----------------------------
ALTER TABLE "sangraha"."lemma_classification" ADD CONSTRAINT "ck_lemma_classification_status" CHECK (status::text = ANY (ARRAY['CANDIDATE'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table lemma_classification
-- ----------------------------
ALTER TABLE "sangraha"."lemma_classification" ADD CONSTRAINT "pk_lemma_classification" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table lemma_statistics
-- ----------------------------
CREATE INDEX "idx_lemma_statistics_lemma_id" ON "sangraha"."lemma_statistics" USING btree (
  "lemma_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table lemma_statistics
-- ----------------------------
ALTER TABLE "sangraha"."lemma_statistics" ADD CONSTRAINT "uq_lemma_statistics_lemma_gender" UNIQUE ("lemma_id", "gender");

-- ----------------------------
-- Primary Key structure for table lemma_statistics
-- ----------------------------
ALTER TABLE "sangraha"."lemma_statistics" ADD CONSTRAINT "pk_lemma_statistics" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table nominal_lemmas
-- ----------------------------
CREATE INDEX "idx_nominal_lemmas_confidence" ON "sangraha"."nominal_lemmas" USING btree (
  "confidence" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_nominal_lemmas_stem_class" ON "sangraha"."nominal_lemmas" USING btree (
  "stem_class" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "nominal_lemmas_lemma_iast_idx" ON "sangraha"."nominal_lemmas" USING btree (
  "lemma_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
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
CREATE INDEX "idx_verse_statistics_grammar_info" ON "sangraha"."verse_statistics" USING gin (
  "grammar_info" "pg_catalog"."jsonb_ops"
);
CREATE INDEX "idx_verse_statistics_word_count" ON "sangraha"."verse_statistics" USING hash (
  "word_count" "pg_catalog"."int4_ops"
);

-- ----------------------------
-- Primary Key structure for table verse_statistics
-- ----------------------------
ALTER TABLE "sangraha"."verse_statistics" ADD CONSTRAINT "pk_verse_statistics" PRIMARY KEY ("verse_id");

-- ----------------------------
-- Indexes structure for table verse_word_derivation
-- ----------------------------
CREATE INDEX "verse_word_derivation_derivation_type_idx" ON "sangraha"."verse_word_derivation" USING btree (
  "derivation_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table verse_word_derivation
-- ----------------------------
ALTER TABLE "sangraha"."verse_word_derivation" ADD CONSTRAINT "pk_verse_word_derivation" PRIMARY KEY ("verse_word_id");

-- ----------------------------
-- Indexes structure for table verse_word_morphology
-- ----------------------------
CREATE INDEX "verse_word_morphology_case_type_gender_number_type_idx" ON "sangraha"."verse_word_morphology" USING btree (
  "case_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "gender" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "number_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "verse_word_morphology_case_type_idx" ON "sangraha"."verse_word_morphology" USING btree (
  "case_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

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
CREATE INDEX "idx_verse_words_lemma_id" ON "sangraha"."verse_words" USING btree (
  "lemma_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_verse_words_surface_iast" ON "sangraha"."verse_words" USING btree (
  "surface_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_verse_words_verse_id" ON "sangraha"."verse_words" USING btree (
  "verse_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_verse_words_vocabulary_word_id" ON "sangraha"."verse_words" USING btree (
  "vocabulary_word_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "verse_words_pos_idx" ON "sangraha"."verse_words" USING btree (
  "pos" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table verse_words
-- ----------------------------
ALTER TABLE "sangraha"."verse_words" ADD CONSTRAINT "pk_verse_words" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table verses
-- ----------------------------
CREATE INDEX "idx_verses_owner_id" ON "sangraha"."verses" USING btree (
  "owner_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_verses_standalone_owner" ON "sangraha"."verses" USING btree (
  "owner_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
) WHERE chapter_id IS NULL;
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
-- Indexes structure for table works_class
-- ----------------------------
CREATE INDEX "idx_work_class_parent" ON "sangraha"."works_class" USING btree (
  "parent_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table works_class
-- ----------------------------
ALTER TABLE "sangraha"."works_class" ADD CONSTRAINT "works_class_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table works_class
-- ----------------------------
ALTER TABLE "sangraha"."works_class" ADD CONSTRAINT "works_class_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table works_work_class
-- ----------------------------
CREATE INDEX "idx_work_work_class_class" ON "sangraha"."works_work_class" USING btree (
  "class_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table works_work_class
-- ----------------------------
ALTER TABLE "sangraha"."works_work_class" ADD CONSTRAINT "works_work_class_pkey" PRIMARY KEY ("work_id", "class_id");

-- ----------------------------
-- Foreign Keys structure for table chapters
-- ----------------------------
ALTER TABLE "sangraha"."chapters" ADD CONSTRAINT "chapters_work_id_fkey" FOREIGN KEY ("work_id") REFERENCES "sangraha"."works" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table curriculum_semantic_class
-- ----------------------------
ALTER TABLE "sangraha"."curriculum_semantic_class" ADD CONSTRAINT "fk_curriculum_semantic_class_parent" FOREIGN KEY ("parent_code") REFERENCES "sangraha"."curriculum_semantic_class" ("code") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lemma_classification
-- ----------------------------
ALTER TABLE "sangraha"."lemma_classification" ADD CONSTRAINT "fk_lemma_classification_lemma" FOREIGN KEY ("lemma_id") REFERENCES "sangraha"."lemma" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "sangraha"."lemma_classification" ADD CONSTRAINT "fk_lemma_classification_scheme" FOREIGN KEY ("scheme_code") REFERENCES "sangraha"."classification_scheme" ("code") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lemma_statistics
-- ----------------------------
ALTER TABLE "sangraha"."lemma_statistics" ADD CONSTRAINT "fk_lemma_statistics_lemma" FOREIGN KEY ("lemma_id") REFERENCES "sangraha"."lemma" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

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
ALTER TABLE "sangraha"."verse_words" ADD CONSTRAINT "fk_verse_words_lemma" FOREIGN KEY ("lemma_id") REFERENCES "sangraha"."lemma" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "sangraha"."verse_words" ADD CONSTRAINT "fk_verse_words_verse" FOREIGN KEY ("verse_id") REFERENCES "sangraha"."verses" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verses
-- ----------------------------
ALTER TABLE "sangraha"."verses" ADD CONSTRAINT "verses_chapter_id_fkey" FOREIGN KEY ("chapter_id") REFERENCES "sangraha"."chapters" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table works
-- ----------------------------
ALTER TABLE "sangraha"."works" ADD CONSTRAINT "fk_works_source" FOREIGN KEY ("source_id") REFERENCES "sangraha"."sources" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table works_class
-- ----------------------------
ALTER TABLE "sangraha"."works_class" ADD CONSTRAINT "works_class_parent_id_fkey" FOREIGN KEY ("parent_id") REFERENCES "sangraha"."works_class" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table works_work_class
-- ----------------------------
ALTER TABLE "sangraha"."works_work_class" ADD CONSTRAINT "works_work_class_class_id_fkey" FOREIGN KEY ("class_id") REFERENCES "sangraha"."works_class" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "sangraha"."works_work_class" ADD CONSTRAINT "works_work_class_work_id_fkey" FOREIGN KEY ("work_id") REFERENCES "sangraha"."works" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
