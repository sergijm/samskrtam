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

 Date: 28/08/2026 18:00:50
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
-- Table structure for conjugation_forms
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."conjugation_forms";
CREATE TABLE "curriculum"."conjugation_forms" (
  "id" uuid NOT NULL,
  "topic_code" varchar(80) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_iast" varchar(120) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_devanagari" varchar(120) COLLATE "pg_catalog"."default",
  "meaning_ru" varchar(200) COLLATE "pg_catalog"."default",
  "voice" varchar(40) COLLATE "pg_catalog"."default" NOT NULL,
  "person" int4 NOT NULL,
  "number_type" varchar(40) COLLATE "pg_catalog"."default" NOT NULL,
  "sentence_iast" varchar(300) COLLATE "pg_catalog"."default" NOT NULL,
  "sentence_devanagari" varchar(300) COLLATE "pg_catalog"."default" NOT NULL,
  "translation_ru" varchar(300) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON TABLE "curriculum"."conjugation_forms" IS 'Present-tense conjugation paradigm cells (example sentences, person x number) for verb lessons.';

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
  "form_devanagari" varchar(120) COLLATE "pg_catalog"."default",
  "confidence" varchar(10) COLLATE "pg_catalog"."default"
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
-- Table structure for lemma
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lemma";
CREATE TABLE "curriculum"."lemma" (
  "id" uuid NOT NULL,
  "lemma_iast" varchar(120) COLLATE "pg_catalog"."default" NOT NULL,
  "pos" varchar(40) COLLATE "pg_catalog"."default",
  "gender" varchar(20) COLLATE "pg_catalog"."default",
  "freq_order" int4
)
;
COMMENT ON TABLE "curriculum"."lemma" IS 'Lemma entries (headword spellings) with POS, gender, and frequency metadata.';

-- ----------------------------
-- Table structure for lemma_lexical_topic
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lemma_lexical_topic";
CREATE TABLE "curriculum"."lemma_lexical_topic" (
  "topic_code" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_iast" varchar(120) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;
COMMENT ON TABLE "curriculum"."lemma_lexical_topic" IS 'Topic membership of a lemma spelling (verse batches, lemma_translation-driven topics).';

-- ----------------------------
-- Table structure for lemma_semantic_class
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lemma_semantic_class";
CREATE TABLE "curriculum"."lemma_semantic_class" (
  "lemma_id" uuid NOT NULL,
  "semantic_class_id" uuid NOT NULL
)
;
COMMENT ON TABLE "curriculum"."lemma_semantic_class" IS 'Semantic-class bindings per lemma_translation row.';

-- ----------------------------
-- Table structure for lemma_translation
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."lemma_translation";
CREATE TABLE "curriculum"."lemma_translation" (
  "id" uuid NOT NULL,
  "language" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "gloss" varchar(300) COLLATE "pg_catalog"."default" NOT NULL,
  "is_main" bool NOT NULL DEFAULT false,
  "lemma_id" uuid NOT NULL
)
;
COMMENT ON TABLE "curriculum"."lemma_translation" IS 'Per-language glosses for lexicon lemmas; one is_main per (lemma_iast, language).';

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
  "item_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "answer_mode" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "prompt" text COLLATE "pg_catalog"."default" NOT NULL,
  "correct_answer" text COLLATE "pg_catalog"."default",
  "distractors" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "payload" jsonb NOT NULL,
  "generator_source" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "progress_tag" varchar COLLATE "pg_catalog"."default",
  "prompt_ru" text COLLATE "pg_catalog"."default",
  "correct_answer_ru" varchar COLLATE "pg_catalog"."default",
  "distractors_ru" jsonb,
  "quest_pattern" varchar COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "curriculum"."quest_item"."progress_tag" IS 'Progress grouping tag populated by the batch generator: DECLENSION_* -> caseType|numberType|gender (MATCH takes first pair, gender=UNSPECIFIED); VOCABULARY_WORD -> lemmaSlp1. NULL for rows generated before V13.';
COMMENT ON COLUMN "curriculum"."quest_item"."prompt_ru" IS 'Russian prompt text produced by the batch generator for all four DECLENSION types; NULL for rows generated before V14 (requires regeneration).';
COMMENT ON COLUMN "curriculum"."quest_item"."correct_answer_ru" IS 'Russian canonical answer label (CASE_RECOGNITION only); NULL otherwise and for MATCHING.';
COMMENT ON COLUMN "curriculum"."quest_item"."distractors_ru" IS 'Russian distractor labels (CASE_RECOGNITION only); NULL otherwise.';
COMMENT ON COLUMN "curriculum"."quest_item"."quest_pattern" IS 'Cognitive-operation label from quest_catalog_2.md (decorative, e.g. nom-form).';
COMMENT ON TABLE "curriculum"."quest_item" IS 'Materialized quest items for all quest types (grammar+lexicon), see curriculum-quest-items.md §1.';

-- ----------------------------
-- Table structure for semantic_class
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."semantic_class";
CREATE TABLE "curriculum"."semantic_class" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(60) COLLATE "pg_catalog"."default" NOT NULL,
  "name_ru" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_id" uuid
)
;

-- ----------------------------
-- Table structure for semantic_class_topic
-- ----------------------------
DROP TABLE IF EXISTS "curriculum"."semantic_class_topic";
CREATE TABLE "curriculum"."semantic_class_topic" (
  "topic_id" uuid NOT NULL,
  "semantic_class_id" uuid NOT NULL
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
  "domain_type" varchar(16) COLLATE "pg_catalog"."default" NOT NULL
)
;
COMMENT ON COLUMN "curriculum"."topic"."learning_level" IS 'Authored first-introduction level L0..L6. Independent from the prerequisite DAG (see curriculum-service.md §2/§6) — not derived, not a computed graph layer.';
COMMENT ON COLUMN "curriculum"."topic"."is_evergreen" IS 'True for topics outside the layered graph (Mixed review, Error correction) — always available.';
COMMENT ON COLUMN "curriculum"."topic"."domain" IS 'GRAMMAR = original curriculum.md topics, LEXICON = lexical topics whose lexemes come from semantic_class_topic (classified, via lexeme_semantic_class) plus lexeme_lexical_topic (unclassified/VERSE explicit bindings), see lexical-curriculum.md §1.';
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
-- Uniques structure for table conjugation_forms
-- ----------------------------
ALTER TABLE "curriculum"."conjugation_forms" ADD CONSTRAINT "uq_conjugation_forms" UNIQUE ("topic_code", "lemma_iast", "voice", "person", "number_type");

-- ----------------------------
-- Checks structure for table conjugation_forms
-- ----------------------------
ALTER TABLE "curriculum"."conjugation_forms" ADD CONSTRAINT "conjugation_forms_person_check" CHECK (person = ANY (ARRAY[1, 2, 3]));

-- ----------------------------
-- Primary Key structure for table conjugation_forms
-- ----------------------------
ALTER TABLE "curriculum"."conjugation_forms" ADD CONSTRAINT "conjugation_forms_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table declension_form
-- ----------------------------
CREATE INDEX "declension_form_lemma_iast_idx" ON "curriculum"."declension_form" USING btree (
  "lemma_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
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
-- Uniques structure for table lemma
-- ----------------------------
ALTER TABLE "curriculum"."lemma" ADD CONSTRAINT "uq_lemma" UNIQUE ("lemma_iast");

-- ----------------------------
-- Primary Key structure for table lemma
-- ----------------------------
ALTER TABLE "curriculum"."lemma" ADD CONSTRAINT "lemma_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table lemma_lexical_topic
-- ----------------------------
CREATE INDEX "ix_lemma_lexical_topic_lemma" ON "curriculum"."lemma_lexical_topic" USING btree (
  "lemma_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table lemma_lexical_topic
-- ----------------------------
ALTER TABLE "curriculum"."lemma_lexical_topic" ADD CONSTRAINT "lemma_lexical_topic_pkey" PRIMARY KEY ("topic_code", "lemma_iast");

-- ----------------------------
-- Indexes structure for table lemma_semantic_class
-- ----------------------------
CREATE INDEX "idx_lemma_semantic_class_class" ON "curriculum"."lemma_semantic_class" USING btree (
  "semantic_class_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Indexes structure for table lemma_translation
-- ----------------------------
CREATE INDEX "ix_lemma_translation_lemma" ON "curriculum"."lemma_translation" USING btree (
  "lemma_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table lemma_translation
-- ----------------------------
ALTER TABLE "curriculum"."lemma_translation" ADD CONSTRAINT "uq_lemma_translation" UNIQUE ("lemma_id", "language", "gloss");

-- ----------------------------
-- Primary Key structure for table lemma_translation
-- ----------------------------
ALTER TABLE "curriculum"."lemma_translation" ADD CONSTRAINT "lemma_translation_pkey" PRIMARY KEY ("id");

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
-- Indexes structure for table semantic_class
-- ----------------------------
CREATE INDEX "semantic_class_code_idx" ON "curriculum"."semantic_class" USING btree (
  "code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table semantic_class
-- ----------------------------
ALTER TABLE "curriculum"."semantic_class" ADD CONSTRAINT "semantic_class_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table semantic_class
-- ----------------------------
ALTER TABLE "curriculum"."semantic_class" ADD CONSTRAINT "semantic_class_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table semantic_class_topic
-- ----------------------------
CREATE INDEX "idx_semantic_class_topic_class" ON "curriculum"."semantic_class_topic" USING btree (
  "semantic_class_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table semantic_class_topic
-- ----------------------------
ALTER TABLE "curriculum"."semantic_class_topic" ADD CONSTRAINT "semantic_class_topic_pkey" PRIMARY KEY ("topic_id", "semantic_class_id");

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
-- Checks structure for table vocabulary_quiz_definition
-- ----------------------------
ALTER TABLE "curriculum"."vocabulary_quiz_definition" ADD CONSTRAINT "chk_vocab_quiz_def_kind" CHECK (kind::text = ANY (ARRAY['TOPIC'::character varying::text, 'MIXED_TOPIC'::character varying::text, 'FREQUENCY_BAND'::character varying::text, 'SOURCE'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table vocabulary_quiz_definition
-- ----------------------------
ALTER TABLE "curriculum"."vocabulary_quiz_definition" ADD CONSTRAINT "vocabulary_quiz_definition_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table complex_quiz_topic
-- ----------------------------
ALTER TABLE "curriculum"."complex_quiz_topic" ADD CONSTRAINT "fk_complex_quiz_topic_quiz" FOREIGN KEY ("complex_quiz_id") REFERENCES "curriculum"."complex_quiz" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."complex_quiz_topic" ADD CONSTRAINT "fk_complex_quiz_topic_topic" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lemma_semantic_class
-- ----------------------------
ALTER TABLE "curriculum"."lemma_semantic_class" ADD CONSTRAINT "fk_lemma_semantic_class_lemma" FOREIGN KEY ("lemma_id") REFERENCES "curriculum"."lemma" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."lemma_semantic_class" ADD CONSTRAINT "lemma_semantic_class_semantic_class_id_fkey" FOREIGN KEY ("semantic_class_id") REFERENCES "curriculum"."semantic_class" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table lemma_translation
-- ----------------------------
ALTER TABLE "curriculum"."lemma_translation" ADD CONSTRAINT "fk_lemma_translation_lemma" FOREIGN KEY ("lemma_id") REFERENCES "curriculum"."lemma" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table quest_item
-- ----------------------------
ALTER TABLE "curriculum"."quest_item" ADD CONSTRAINT "quest_item_topic_id_fkey" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table semantic_class
-- ----------------------------
ALTER TABLE "curriculum"."semantic_class" ADD CONSTRAINT "semantic_class_parent_id_fkey" FOREIGN KEY ("parent_id") REFERENCES "curriculum"."semantic_class" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table semantic_class_topic
-- ----------------------------
ALTER TABLE "curriculum"."semantic_class_topic" ADD CONSTRAINT "semantic_class_topic_semantic_class_id_fkey" FOREIGN KEY ("semantic_class_id") REFERENCES "lingua"."semantic_class" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."semantic_class_topic" ADD CONSTRAINT "semantic_class_topic_topic_id_fkey" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table topic_prerequisite
-- ----------------------------
ALTER TABLE "curriculum"."topic_prerequisite" ADD CONSTRAINT "fk_topic_prerequisite_prerequisite_topic" FOREIGN KEY ("prerequisite_topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."topic_prerequisite" ADD CONSTRAINT "fk_topic_prerequisite_topic" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table vocabulary_quiz_definition
-- ----------------------------
ALTER TABLE "curriculum"."vocabulary_quiz_definition" ADD CONSTRAINT "vocabulary_quiz_definition_complex_quiz_id_fkey" FOREIGN KEY ("complex_quiz_id") REFERENCES "curriculum"."complex_quiz" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "curriculum"."vocabulary_quiz_definition" ADD CONSTRAINT "vocabulary_quiz_definition_topic_id_fkey" FOREIGN KEY ("topic_id") REFERENCES "curriculum"."topic" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
