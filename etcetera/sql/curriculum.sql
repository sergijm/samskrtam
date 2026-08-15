/*
 Navicat Premium Data Transfer

 Source Server         : localhost-pg17
 Source Server Type    : PostgreSQL
 Source Server Version : 170000 (170000)
 Source Host           : localhost:5436
 Source Catalog        : samskrtam
 Source Schema         : curriculum

 Target Server Type    : PostgreSQL
 Target Server Version : 170000 (170000)
 File Encoding         : 65001

 Date: 15/08/2026 14:46:37
*/


-- ----------------------------
-- Table structure for complex_quiz
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."complex_quiz";
CREATE TABLE "curriculum"."complex_quiz" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "learning_level" varchar(2) COLLATE "pg_catalog"."default" NOT NULL,
  "title_ru" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "question_count_hint" int2,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON COLUMN "curriculum"."complex_quiz"."question_count_hint" IS 'Decorative UI hint only, not a real generated question count.';
COMMENT ON TABLE "curriculum"."complex_quiz" IS 'Curated multi-topic practice/assessment composition. Topic count per type (2-4 / 5-7) is validated in the service layer, not the DB.';

-- ----------------------------
-- Table structure for complex_quiz_topic
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."complex_quiz_topic";
CREATE TABLE "curriculum"."complex_quiz_topic" (
  "complex_quiz_id" uuid NOT NULL,
  "topic_id" uuid NOT NULL
)
;

-- ----------------------------
-- Table structure for declension_form
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."declension_form";
CREATE TABLE "curriculum"."declension_form" (
  "lemma_iast" varchar(120) COLLATE "pg_catalog"."default" NOT NULL,
  "vowel_type" varchar(40) COLLATE "pg_catalog"."default" NOT NULL,
  "case_type" varchar(40) COLLATE "pg_catalog"."default" NOT NULL,
  "number_type" varchar(40) COLLATE "pg_catalog"."default" NOT NULL,
  "form_iast" varchar(120) COLLATE "pg_catalog"."default",
  "form_devanagari" varchar(120) COLLATE "pg_catalog"."default"
)
;
COMMENT ON TABLE "curriculum"."declension_form" IS 'Paradigm cell (case+number -> form) of a lemma within one declension class, keyed by (lemma_iast, vowel_type).';

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."flyway_schema_history";
CREATE TABLE "curriculum"."flyway_schema_history" (
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
-- Table structure for frequency_band
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."frequency_band";
CREATE TABLE "curriculum"."frequency_band" (
  "code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "min_rank" int4 NOT NULL,
  "max_rank" int4 NOT NULL,
  "label_ru" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "label_en" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "sort_order" int2 NOT NULL
)
;

-- ----------------------------
-- Table structure for lexeme
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lexeme";
CREATE TABLE "curriculum"."lexeme" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "lemma_iast" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_devanagari" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_slp1" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "gloss_ru" varchar(300) COLLATE "pg_catalog"."default" NOT NULL,
  "gloss_en" varchar(300) COLLATE "pg_catalog"."default" NOT NULL,
  "long_definition_ru" text COLLATE "pg_catalog"."default",
  "long_definition_en" text COLLATE "pg_catalog"."default",
  "gender" varchar(20) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "meaning_number" int4 NOT NULL DEFAULT 1
)
;
COMMENT ON TABLE "curriculum"."lexeme" IS 'A dictionary lemma, not a specific word form. See lexicon.md §1.';

-- ----------------------------
-- Table structure for lexeme_frequency
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lexeme_frequency";
CREATE TABLE "curriculum"."lexeme_frequency" (
  "lexeme_id" uuid NOT NULL,
  "source" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "rank" int4 NOT NULL
)
;

-- ----------------------------
-- Table structure for lexeme_morphology
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lexeme_morphology";
CREATE TABLE "curriculum"."lexeme_morphology" (
  "lexeme_id" uuid NOT NULL,
  "morphology_class_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for lexeme_pos
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lexeme_pos";
CREATE TABLE "curriculum"."lexeme_pos" (
  "lexeme_id" uuid NOT NULL,
  "pos_code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for lexeme_semantic_topic
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lexeme_semantic_topic";
CREATE TABLE "curriculum"."lexeme_semantic_topic" (
  "lexeme_id" uuid NOT NULL,
  "semantic_topic_id" uuid NOT NULL
)
;

-- ----------------------------
-- Table structure for lexical_topic_binding
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lexical_topic_binding";
CREATE TABLE "curriculum"."lexical_topic_binding" (
  "lexical_topic_id" uuid NOT NULL,
  "lexeme_id" uuid NOT NULL
)
;

-- ----------------------------
-- Table structure for morphology_class
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."morphology_class";
CREATE TABLE "curriculum"."morphology_class" (
  "code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "applies_to" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "name_ru" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(60) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for part_of_speech
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."part_of_speech";
CREATE TABLE "curriculum"."part_of_speech" (
  "code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "group" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "name_ru" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(60) COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for quest_item
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."quest_item";
CREATE TABLE "curriculum"."quest_item" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "topic_id" uuid NOT NULL,
  "item_type" varchar(40) COLLATE "pg_catalog"."default" NOT NULL,
  "answer_mode" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "prompt" text COLLATE "pg_catalog"."default" NOT NULL,
  "correct_answer" text COLLATE "pg_catalog"."default",
  "distractors" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "payload" jsonb NOT NULL,
  "generator_source" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "progress_tag" varchar(255) COLLATE "pg_catalog"."default",
  "prompt_ru" text COLLATE "pg_catalog"."default",
  "correct_answer_ru" varchar(200) COLLATE "pg_catalog"."default",
  "distractors_ru" jsonb
)
;
COMMENT ON COLUMN "curriculum"."quest_item"."progress_tag" IS 'Progress grouping tag populated by the batch generator: DECLENSION_* -> caseType|numberType|gender (MATCH takes first pair, gender=UNSPECIFIED); VOCABULARY_WORD -> lemmaSlp1. NULL for rows generated before V13.';
COMMENT ON COLUMN "curriculum"."quest_item"."prompt_ru" IS 'Russian prompt text produced by the batch generator for all four DECLENSION types; NULL for rows generated before V14 (requires regeneration).';
COMMENT ON COLUMN "curriculum"."quest_item"."correct_answer_ru" IS 'Russian canonical answer label (CASE_RECOGNITION only); NULL otherwise and for MATCHING.';
COMMENT ON COLUMN "curriculum"."quest_item"."distractors_ru" IS 'Russian distractor labels (CASE_RECOGNITION only); NULL otherwise.';
COMMENT ON TABLE "curriculum"."quest_item" IS 'Materialized quest items for all quest types (grammar+lexicon), see curriculum-quest-items.md §1.';

-- ----------------------------
-- Table structure for semantic_topic
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."semantic_topic";
CREATE TABLE "curriculum"."semantic_topic" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "name_ru" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_id" uuid
)
;

-- ----------------------------
-- Table structure for topic
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."topic";
CREATE TABLE "curriculum"."topic" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(80) COLLATE "pg_catalog"."default" NOT NULL,
  "title_ru" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "learning_level" varchar(2) COLLATE "pg_catalog"."default",
  "is_evergreen" bool NOT NULL DEFAULT false,
  "display_order" int2,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  "domain" varchar(25) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'GRAMMAR'::character varying,
  "target_item_count" int4 NOT NULL DEFAULT 0,
  "hidden" bool NOT NULL DEFAULT false,
  "semantic_topic_id" uuid,
  "domain_type" varchar(16) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "curriculum"."topic"."learning_level" IS 'Authored first-introduction level L0..L6. Independent from the prerequisite DAG (see curriculum-service.md §2/§6) — not derived, not a computed graph layer.';
COMMENT ON COLUMN "curriculum"."topic"."is_evergreen" IS 'True for topics outside the layered graph (Mixed review, Error correction) — always available.';
COMMENT ON COLUMN "curriculum"."topic"."domain" IS 'GRAMMAR = original curriculum.md topics, LEXICON = lexical topics backed by curriculum.lexical_topic_binding (see lexical-curriculum.md §1).';
COMMENT ON COLUMN "curriculum"."topic"."target_item_count" IS 'Target noun/lexeme count for this declension topic, filled by the declension bootstrapper from sangraha; 0 = not a bootstrap target.';
COMMENT ON TABLE "curriculum"."topic" IS 'Curriculum topic ("урок") — structure only, no content/quizzes. See curriculum-service.md.';

-- ----------------------------
-- Table structure for topic_prerequisite
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."topic_prerequisite";
CREATE TABLE "curriculum"."topic_prerequisite" (
  "topic_id" uuid NOT NULL,
  "prerequisite_topic_id" uuid NOT NULL,
  "strength" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "curriculum"."topic_prerequisite" IS 'Soft (non-blocking) dependency edges between topics. Direction: prerequisite_topic_id -> topic_id.';

-- ----------------------------
-- Table structure for user_collection
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."user_collection";
CREATE TABLE "curriculum"."user_collection" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "owner_id" uuid NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "description" text COLLATE "pg_catalog"."default",
  "visibility" varchar(10) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'PRIVATE'::character varying,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for user_collection_item
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."user_collection_item";
CREATE TABLE "curriculum"."user_collection_item" (
  "collection_id" uuid NOT NULL,
  "lexeme_id" uuid NOT NULL,
  "added_via" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "added_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for user_lexeme_progress
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."user_lexeme_progress";
CREATE TABLE "curriculum"."user_lexeme_progress" (
  "user_id" uuid NOT NULL,
  "lexeme_id" uuid NOT NULL,
  "mastery_score" int2 NOT NULL DEFAULT 0,
  "exposure_count" int4 NOT NULL DEFAULT 0,
  "correct_count" int4 NOT NULL DEFAULT 0,
  "incorrect_count" int4 NOT NULL DEFAULT 0,
  "last_seen_at" timestamptz(6),
  "next_review_at" timestamptz(6),
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for vocabulary_quiz_definition
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."vocabulary_quiz_definition";
CREATE TABLE "curriculum"."vocabulary_quiz_definition" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "kind" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "title_ru" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "topic_id" uuid,
  "complex_quiz_id" uuid,
  "frequency_rank_max" int4,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "updated_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for word_form
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."word_form";
CREATE TABLE "curriculum"."word_form" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "lexeme_id" uuid NOT NULL,
  "form_iast" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "form_devanagari" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "grammatical_note" varchar(200) COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- View structure for semantic_topic_lexeme_counts
-- ----------------------------
DROP VIEW IF EXISTS "curriculum"."semantic_topic_lexeme_counts";
CREATE VIEW "curriculum"."semantic_topic_lexeme_counts" AS  WITH RECURSIVE tree AS (
         SELECT st_1.id AS root_id,
            st_1.id AS node_id,
            COALESCE(dc.c, 0::bigint) AS direct_count
           FROM curriculum.semantic_topic st_1
             LEFT JOIN ( SELECT lexeme_semantic_topic.semantic_topic_id,
                    count(*) AS c
                   FROM curriculum.lexeme_semantic_topic
                  GROUP BY lexeme_semantic_topic.semantic_topic_id) dc ON dc.semantic_topic_id = st_1.id
        UNION ALL
         SELECT t_1.root_id,
            child.id,
            COALESCE(dc2.c, 0::bigint) AS "coalesce"
           FROM tree t_1
             JOIN curriculum.semantic_topic child ON child.parent_id = t_1.node_id
             LEFT JOIN ( SELECT lexeme_semantic_topic.semantic_topic_id,
                    count(*) AS c
                   FROM curriculum.lexeme_semantic_topic
                  GROUP BY lexeme_semantic_topic.semantic_topic_id) dc2 ON dc2.semantic_topic_id = child.id
        )
 SELECT st.code,
    st.name_ru,
    st.name_en,
    st.parent_id,
    sum(t.direct_count) AS lexeme_count
   FROM tree t
     JOIN curriculum.semantic_topic st ON st.id = t.root_id
  GROUP BY st.id, st.code, st.name_ru, st.name_en, st.parent_id
  ORDER BY st.code;

-- ----------------------------
-- Indexes structure for table complex_quiz
-- ----------------------------
CREATE INDEX "idx_complex_quiz_level_type" ON "curriculum"."complex_quiz" USING btree (
  "learning_level" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table complex_quiz
-- ----------------------------
ALTER TABLE "curriculum"."complex_quiz" ADD CONSTRAINT "chk_complex_quiz_learning_level" CHECK (learning_level::text = ANY (ARRAY['L0'::character varying::text, 'L1'::character varying::text, 'L2'::character varying::text, 'L3'::character varying::text, 'L4'::character varying::text, 'L5'::character varying::text, 'L6'::character varying::text]));
ALTER TABLE "curriculum"."complex_quiz" ADD CONSTRAINT "chk_complex_quiz_type" CHECK (type::text = ANY (ARRAY['MIXED_PRACTICE'::character varying::text, 'LEVEL_ASSESSMENT'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table complex_quiz
-- ----------------------------
ALTER TABLE "curriculum"."complex_quiz" ADD CONSTRAINT "complex_quiz_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table complex_quiz_topic
-- ----------------------------
CREATE INDEX "idx_complex_quiz_topic_topic_id" ON "curriculum"."complex_quiz_topic" USING btree (
  "topic_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table complex_quiz_topic
-- ----------------------------
ALTER TABLE "curriculum"."complex_quiz_topic" ADD CONSTRAINT "complex_quiz_topic_pkey" PRIMARY KEY ("complex_quiz_id", "topic_id");

-- ----------------------------
-- Indexes structure for table declension_form
-- ----------------------------
CREATE INDEX "idx_declension_form_vowel_type" ON "curriculum"."declension_form" USING btree (
  "vowel_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table declension_form
-- ----------------------------
ALTER TABLE "curriculum"."declension_form" ADD CONSTRAINT "pk_declension_form" PRIMARY KEY ("lemma_iast", "vowel_type", "case_type", "number_type");

-- ----------------------------
-- Indexes structure for table flyway_schema_history
-- ----------------------------
CREATE INDEX "flyway_schema_history_s_idx" ON "curriculum"."flyway_schema_history" USING btree (
  "success" "pg_catalog"."bool_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table flyway_schema_history
-- ----------------------------
ALTER TABLE "curriculum"."flyway_schema_history" ADD CONSTRAINT "flyway_schema_history_pk" PRIMARY KEY ("installed_rank");

-- ----------------------------
-- Primary Key structure for table frequency_band
-- ----------------------------
ALTER TABLE "curriculum"."frequency_band" ADD CONSTRAINT "frequency_band_pkey" PRIMARY KEY ("code");

-- ----------------------------
-- Indexes structure for table lexeme
-- ----------------------------
CREATE INDEX "idx_lexeme_slp1" ON "curriculum"."lexeme" USING btree (
  "lemma_slp1" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table lexeme
-- ----------------------------
ALTER TABLE "curriculum"."lexeme" ADD CONSTRAINT "uq_lexeme_slp1_meaning" UNIQUE ("lemma_slp1", "meaning_number");

-- ----------------------------
-- Checks structure for table lexeme
-- ----------------------------
ALTER TABLE "curriculum"."lexeme" ADD CONSTRAINT "chk_lexeme_gender" CHECK (gender IS NULL OR (gender::text = ANY (ARRAY['MASCULINE'::character varying::text, 'FEMININE'::character varying::text, 'NEUTER'::character varying::text, 'UNSPECIFIED'::character varying::text])));

-- ----------------------------
-- Primary Key structure for table lexeme
-- ----------------------------
ALTER TABLE "curriculum"."lexeme" ADD CONSTRAINT "lexeme_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table lexeme_frequency
-- ----------------------------
CREATE INDEX "idx_lexeme_frequency_rank" ON "curriculum"."lexeme_frequency" USING btree (
  "source" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "rank" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table lexeme_frequency
-- ----------------------------
ALTER TABLE "curriculum"."lexeme_frequency" ADD CONSTRAINT "lexeme_frequency_pkey" PRIMARY KEY ("lexeme_id", "source");

-- ----------------------------
-- Primary Key structure for table lexeme_morphology
-- ----------------------------
ALTER TABLE "curriculum"."lexeme_morphology" ADD CONSTRAINT "lexeme_morphology_pkey" PRIMARY KEY ("lexeme_id", "morphology_class_code");

-- ----------------------------
-- Indexes structure for table lexeme_pos
-- ----------------------------
CREATE INDEX "idx_lexeme_pos_code" ON "curriculum"."lexeme_pos" USING btree (
  "pos_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table lexeme_pos
-- ----------------------------
ALTER TABLE "curriculum"."lexeme_pos" ADD CONSTRAINT "lexeme_pos_pkey" PRIMARY KEY ("lexeme_id", "pos_code");

-- ----------------------------
-- Indexes structure for table lexeme_semantic_topic
-- ----------------------------
CREATE INDEX "idx_lexeme_semantic_topic_topic" ON "curriculum"."lexeme_semantic_topic" USING btree (
  "semantic_topic_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table lexeme_semantic_topic
-- ----------------------------
ALTER TABLE "curriculum"."lexeme_semantic_topic" ADD CONSTRAINT "lexeme_semantic_topic_pkey" PRIMARY KEY ("lexeme_id", "semantic_topic_id");

-- ----------------------------
-- Indexes structure for table lexical_topic_binding
-- ----------------------------
CREATE INDEX "idx_lexical_topic_binding_lexeme_id" ON "curriculum"."lexical_topic_binding" USING btree (
  "lexeme_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table lexical_topic_binding
-- ----------------------------
ALTER TABLE "curriculum"."lexical_topic_binding" ADD CONSTRAINT "lexical_topic_binding_pkey" PRIMARY KEY ("lexical_topic_id", "lexeme_id");

-- ----------------------------
-- Checks structure for table morphology_class
-- ----------------------------
ALTER TABLE "curriculum"."morphology_class" ADD CONSTRAINT "chk_morphology_applies_to" CHECK (applies_to::text = ANY (ARRAY['NOUN'::character varying::text, 'VERB'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table morphology_class
-- ----------------------------
ALTER TABLE "curriculum"."morphology_class" ADD CONSTRAINT "morphology_class_pkey" PRIMARY KEY ("code");

-- ----------------------------
-- Checks structure for table part_of_speech
-- ----------------------------
ALTER TABLE "curriculum"."part_of_speech" ADD CONSTRAINT "chk_pos_group" CHECK ("group"::text = ANY (ARRAY['NOMINAL'::character varying::text, 'VERBAL'::character varying::text, 'INDECLINABLE'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table part_of_speech
-- ----------------------------
ALTER TABLE "curriculum"."part_of_speech" ADD CONSTRAINT "part_of_speech_pkey" PRIMARY KEY ("code");

-- ----------------------------
-- Indexes structure for table quest_item
-- ----------------------------
CREATE INDEX "idx_quest_item_topic_type" ON "curriculum"."quest_item" USING btree (
  "topic_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
  "item_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_quest_item_type" ON "curriculum"."quest_item" USING btree (
  "item_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table quest_item
-- ----------------------------
ALTER TABLE "curriculum"."quest_item" ADD CONSTRAINT "chk_quest_item_answer_mode" CHECK (answer_mode::text = ANY (ARRAY['FREE_TEXT'::character varying::text, 'SINGLE_CHOICE'::character varying::text, 'MULTI_SELECT'::character varying::text, 'SPAN_SELECT'::character varying::text, 'MATCHING'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table quest_item
-- ----------------------------
ALTER TABLE "curriculum"."quest_item" ADD CONSTRAINT "quest_item_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table semantic_topic
-- ----------------------------
CREATE INDEX "semantic_topic_code_idx" ON "curriculum"."semantic_topic" USING btree (
  "code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table semantic_topic
-- ----------------------------
ALTER TABLE "curriculum"."semantic_topic" ADD CONSTRAINT "semantic_topic_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table semantic_topic
-- ----------------------------
ALTER TABLE "curriculum"."semantic_topic" ADD CONSTRAINT "semantic_topic_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table topic
-- ----------------------------
CREATE INDEX "idx_topic_domain" ON "curriculum"."topic" USING btree (
  "domain" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_topic_is_evergreen" ON "curriculum"."topic" USING btree (
  "is_evergreen" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_topic_learning_level" ON "curriculum"."topic" USING btree (
  "learning_level" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table topic
-- ----------------------------
ALTER TABLE "curriculum"."topic" ADD CONSTRAINT "uq_topic_code" UNIQUE ("code");

-- ----------------------------
-- Checks structure for table topic
-- ----------------------------
ALTER TABLE "curriculum"."topic" ADD CONSTRAINT "chk_topic_learning_level" CHECK (learning_level::text = ANY (ARRAY['L0'::character varying::text, 'L1'::character varying::text, 'L2'::character varying::text, 'L3'::character varying::text, 'L4'::character varying::text, 'L5'::character varying::text, 'L6'::character varying::text]));
ALTER TABLE "curriculum"."topic" ADD CONSTRAINT "chk_topic_domain" CHECK (domain::text = ANY (ARRAY['GRAMMAR'::character varying, 'LEXICON'::character varying, 'CONJUNCTION'::character varying, 'PHONOLOGY_SCRIPT'::character varying, 'SANDHI'::character varying, 'GRAMMAR_FOUNDATIONS'::character varying, 'NOMINAL_MORPHOLOGY'::character varying, 'PRONOUNS'::character varying, 'VERBAL_MORPHOLOGY'::character varying, 'NONFINITE_FORMS'::character varying, 'NUMERALS'::character varying, 'CASE_SYNTAX'::character varying, 'SYNTAX'::character varying, 'WORD_FORMATION'::character varying, 'ADVANCED_READING'::character varying, 'VERSE'::character varying]::text[]));
ALTER TABLE "curriculum"."topic" ADD CONSTRAINT "chk_topic_domain_type" CHECK (domain_type::text = ANY (ARRAY['GRAMMAR'::character varying, 'LEXICON'::character varying, 'VERSE'::character varying]::text[]));

-- ----------------------------
-- Primary Key structure for table topic
-- ----------------------------
ALTER TABLE "curriculum"."topic" ADD CONSTRAINT "topic_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table topic_prerequisite
-- ----------------------------
CREATE INDEX "idx_topic_prerequisite_prerequisite_topic_id" ON "curriculum"."topic_prerequisite" USING btree (
  "prerequisite_topic_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table topic_prerequisite
-- ----------------------------
ALTER TABLE "curriculum"."topic_prerequisite" ADD CONSTRAINT "chk_topic_prerequisite_no_self_loop" CHECK (topic_id <> prerequisite_topic_id);
ALTER TABLE "curriculum"."topic_prerequisite" ADD CONSTRAINT "chk_topic_prerequisite_strength" CHECK (strength::text = ANY (ARRAY['RECOMMENDED'::character varying::text, 'HELPFUL'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table topic_prerequisite
-- ----------------------------
ALTER TABLE "curriculum"."topic_prerequisite" ADD CONSTRAINT "topic_prerequisite_pkey" PRIMARY KEY ("topic_id", "prerequisite_topic_id");

-- ----------------------------
-- Indexes structure for table user_collection
-- ----------------------------
CREATE INDEX "idx_user_collection_owner_id" ON "curriculum"."user_collection" USING btree (
  "owner_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table user_collection
-- ----------------------------
ALTER TABLE "curriculum"."user_collection" ADD CONSTRAINT "chk_user_collection_visibility" CHECK (visibility::text = ANY (ARRAY['PRIVATE'::character varying::text, 'SHARED'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table user_collection
-- ----------------------------
ALTER TABLE "curriculum"."user_collection" ADD CONSTRAINT "user_collection_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Checks structure for table user_collection_item
-- ----------------------------
ALTER TABLE "curriculum"."user_collection_item" ADD CONSTRAINT "chk_user_collection_item_added_via" CHECK (added_via::text = ANY (ARRAY['MANUAL'::character varying::text, 'DICTIONARY_SEARCH'::character varying::text, 'TEXT_READING'::character varying::text, 'QUIZ_RESULT'::character varying::text, 'LEARNING_RESULT'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table user_collection_item
-- ----------------------------
ALTER TABLE "curriculum"."user_collection_item" ADD CONSTRAINT "user_collection_item_pkey" PRIMARY KEY ("collection_id", "lexeme_id");

-- ----------------------------
-- Indexes structure for table user_lexeme_progress
-- ----------------------------
CREATE INDEX "idx_user_lexeme_progress_next_review" ON "curriculum"."user_lexeme_progress" USING btree (
  "user_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
  "next_review_at" "pg_catalog"."timestamptz_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table user_lexeme_progress
-- ----------------------------
ALTER TABLE "curriculum"."user_lexeme_progress" ADD CONSTRAINT "user_lexeme_progress_pkey" PRIMARY KEY ("user_id", "lexeme_id");

-- ----------------------------
-- Checks structure for table vocabulary_quiz_definition
-- ----------------------------
ALTER TABLE "curriculum"."vocabulary_quiz_definition" ADD CONSTRAINT "chk_vocab_quiz_def_kind" CHECK (kind::text = ANY (ARRAY['TOPIC'::character varying::text, 'MIXED_TOPIC'::character varying::text, 'FREQUENCY_BAND'::character varying::text, 'SOURCE'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table vocabulary_quiz_definition
-- ----------------------------
ALTER TABLE "curriculum"."vocabulary_quiz_definition" ADD CONSTRAINT "vocabulary_quiz_definition_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table word_form
-- ----------------------------
CREATE INDEX "idx_word_form_lexeme_id" ON "curriculum"."word_form" USING btree (
  "lexeme_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table word_form
-- ----------------------------
ALTER TABLE "curriculum"."word_form" ADD CONSTRAINT "word_form_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table complex_quiz_topic
-- ----------------------------
ALTER TABLE "curriculum"."complex_quiz_topic" ADD CONSTRAINT "fk_complex_quiz_topic_quiz" FOREIGN KEY ("complex_quiz_id") REFERENCES "curriculum"."complex_quiz" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."complex_quiz_topic" ADD CONSTRAINT "fk_complex_quiz_topic_topic" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lexeme_frequency
-- ----------------------------
ALTER TABLE "curriculum"."lexeme_frequency" ADD CONSTRAINT "lexeme_frequency_lexeme_id_fkey" FOREIGN KEY ("lexeme_id") REFERENCES "curriculum"."lexeme" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lexeme_morphology
-- ----------------------------
ALTER TABLE "curriculum"."lexeme_morphology" ADD CONSTRAINT "lexeme_morphology_lexeme_id_fkey" FOREIGN KEY ("lexeme_id") REFERENCES "curriculum"."lexeme" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."lexeme_morphology" ADD CONSTRAINT "lexeme_morphology_morphology_class_code_fkey" FOREIGN KEY ("morphology_class_code") REFERENCES "curriculum"."morphology_class" ("code") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lexeme_pos
-- ----------------------------
ALTER TABLE "curriculum"."lexeme_pos" ADD CONSTRAINT "lexeme_pos_lexeme_id_fkey" FOREIGN KEY ("lexeme_id") REFERENCES "curriculum"."lexeme" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."lexeme_pos" ADD CONSTRAINT "lexeme_pos_pos_code_fkey" FOREIGN KEY ("pos_code") REFERENCES "curriculum"."part_of_speech" ("code") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lexeme_semantic_topic
-- ----------------------------
ALTER TABLE "curriculum"."lexeme_semantic_topic" ADD CONSTRAINT "lexeme_semantic_topic_lexeme_id_fkey" FOREIGN KEY ("lexeme_id") REFERENCES "curriculum"."lexeme" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."lexeme_semantic_topic" ADD CONSTRAINT "lexeme_semantic_topic_semantic_topic_id_fkey" FOREIGN KEY ("semantic_topic_id") REFERENCES "curriculum"."semantic_topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lexical_topic_binding
-- ----------------------------
ALTER TABLE "curriculum"."lexical_topic_binding" ADD CONSTRAINT "lexical_topic_binding_lexeme_id_fkey" FOREIGN KEY ("lexeme_id") REFERENCES "curriculum"."lexeme" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."lexical_topic_binding" ADD CONSTRAINT "lexical_topic_binding_lexical_topic_id_fkey" FOREIGN KEY ("lexical_topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table quest_item
-- ----------------------------
ALTER TABLE "curriculum"."quest_item" ADD CONSTRAINT "quest_item_topic_id_fkey" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table semantic_topic
-- ----------------------------
ALTER TABLE "curriculum"."semantic_topic" ADD CONSTRAINT "semantic_topic_parent_id_fkey" FOREIGN KEY ("parent_id") REFERENCES "curriculum"."semantic_topic" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table topic
-- ----------------------------
ALTER TABLE "curriculum"."topic" ADD CONSTRAINT "topic_semantic_topic_id_fkey" FOREIGN KEY ("semantic_topic_id") REFERENCES "curriculum"."semantic_topic" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table topic_prerequisite
-- ----------------------------
ALTER TABLE "curriculum"."topic_prerequisite" ADD CONSTRAINT "fk_topic_prerequisite_prerequisite_topic" FOREIGN KEY ("prerequisite_topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."topic_prerequisite" ADD CONSTRAINT "fk_topic_prerequisite_topic" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table user_collection_item
-- ----------------------------
ALTER TABLE "curriculum"."user_collection_item" ADD CONSTRAINT "user_collection_item_collection_id_fkey" FOREIGN KEY ("collection_id") REFERENCES "curriculum"."user_collection" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."user_collection_item" ADD CONSTRAINT "user_collection_item_lexeme_id_fkey" FOREIGN KEY ("lexeme_id") REFERENCES "curriculum"."lexeme" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table user_lexeme_progress
-- ----------------------------
ALTER TABLE "curriculum"."user_lexeme_progress" ADD CONSTRAINT "user_lexeme_progress_lexeme_id_fkey" FOREIGN KEY ("lexeme_id") REFERENCES "curriculum"."lexeme" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table vocabulary_quiz_definition
-- ----------------------------
ALTER TABLE "curriculum"."vocabulary_quiz_definition" ADD CONSTRAINT "vocabulary_quiz_definition_complex_quiz_id_fkey" FOREIGN KEY ("complex_quiz_id") REFERENCES "curriculum"."complex_quiz" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."vocabulary_quiz_definition" ADD CONSTRAINT "vocabulary_quiz_definition_topic_id_fkey" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table word_form
-- ----------------------------
ALTER TABLE "curriculum"."word_form" ADD CONSTRAINT "word_form_lexeme_id_fkey" FOREIGN KEY ("lexeme_id") REFERENCES "curriculum"."lexeme" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
