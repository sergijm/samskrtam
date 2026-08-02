/*
 Navicat Premium Data Transfer

 Source Server         : mdm-dev-vm
 Source Server Type    : PostgreSQL
 Source Server Version : 170009 (170009)
 Source Host           : mdm-dev:5432
 Source Catalog        : samskrtam
 Source Schema         : content

 Target Server Type    : PostgreSQL
 Target Server Version : 170009 (170009)
 File Encoding         : 65001

 Date: 02/08/2026 15:29:18
*/


-- ----------------------------
-- Table structure for case_endings
-- ----------------------------
DROP TABLE IF EXISTS "content"."case_endings";
CREATE TABLE "content"."case_endings" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "vowel_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "gender" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "case_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "number_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "ending_iast" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "ending_devanagari" varchar(20) COLLATE "pg_catalog"."default"
)
;
COMMENT ON TABLE "content"."case_endings" IS 'Эталонные окончания склонений для всех типов основ (ADR-003)';

-- ----------------------------
-- Table structure for declension_example_groups
-- ----------------------------
DROP TABLE IF EXISTS "content"."declension_example_groups";
CREATE TABLE "content"."declension_example_groups" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "vowel_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "gender" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "case_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "number_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "verse_ids" jsonb NOT NULL DEFAULT '[]'::jsonb,
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for declension_forms
-- ----------------------------
DROP TABLE IF EXISTS "content"."declension_forms";
CREATE TABLE "content"."declension_forms" (
  "declension_stem_id" uuid NOT NULL,
  "case_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "number_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "form_iast" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "form_devanagari" varchar(50) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "content"."declension_forms"."declension_stem_id" IS 'Идентификатор основы склонения';
COMMENT ON COLUMN "content"."declension_forms"."case_type" IS 'Тип падежа';
COMMENT ON COLUMN "content"."declension_forms"."number_type" IS 'Тип числа';
COMMENT ON COLUMN "content"."declension_forms"."form_iast" IS 'Форма слова в IAST';
COMMENT ON COLUMN "content"."declension_forms"."form_devanagari" IS 'Форма слова в деванагари';
COMMENT ON TABLE "content"."declension_forms" IS 'Таблица для хранения форм склонений';

-- ----------------------------
-- Table structure for declension_stems
-- ----------------------------
DROP TABLE IF EXISTS "content"."declension_stems";
CREATE TABLE "content"."declension_stems" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "stem_iast" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "vowel_type" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "gender" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "translation_ru" varchar(255) COLLATE "pg_catalog"."default",
  "translation_en" varchar(255) COLLATE "pg_catalog"."default",
  "stem_devanagari" varchar(255) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "content"."declension_stems"."id" IS 'Уникальный идентификатор основы склонения';
COMMENT ON COLUMN "content"."declension_stems"."stem_iast" IS 'Название основы в IAST';
COMMENT ON COLUMN "content"."declension_stems"."vowel_type" IS 'Тип гласной основы';
COMMENT ON COLUMN "content"."declension_stems"."gender" IS 'Грамматический род основы';
COMMENT ON COLUMN "content"."declension_stems"."translation_ru" IS 'Перевод основы на русский язык';
COMMENT ON COLUMN "content"."declension_stems"."translation_en" IS 'Перевод основы на английский язык';
COMMENT ON COLUMN "content"."declension_stems"."stem_devanagari" IS 'Название основы в деванагари';
COMMENT ON TABLE "content"."declension_stems" IS 'Таблица для хранения основ склонений';

-- ----------------------------
-- Table structure for flyway_schema_history
-- ----------------------------
DROP TABLE IF EXISTS "content"."flyway_schema_history";
CREATE TABLE "content"."flyway_schema_history" (
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
-- Table structure for lesson
-- ----------------------------
DROP TABLE IF EXISTS "content"."lesson";
CREATE TABLE "content"."lesson" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "slug" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "title_ru" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "title_en" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "description_ru" varchar COLLATE "pg_catalog"."default",
  "description_en" varchar COLLATE "pg_catalog"."default",
  "lesson_type" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "difficulty" varchar COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'BEGINNER'::character varying,
  "questions_per_session" int4 NOT NULL DEFAULT 10,
  "created_at" timestamptz(6) NOT NULL DEFAULT now(),
  "deleted_at" timestamptz(6)
)
;
COMMENT ON COLUMN "content"."lesson"."id" IS 'Уникальный идентификатор квиза';
COMMENT ON COLUMN "content"."lesson"."slug" IS 'Уникальный читаемый идентификатор квиза для URL';
COMMENT ON COLUMN "content"."lesson"."title_ru" IS 'Название квиза на русском языке';
COMMENT ON COLUMN "content"."lesson"."title_en" IS 'Название квиза на английском языке';
COMMENT ON COLUMN "content"."lesson"."description_ru" IS 'Описание квиза на русском языке';
COMMENT ON COLUMN "content"."lesson"."description_en" IS 'Описание квиза на английском языке';
COMMENT ON COLUMN "content"."lesson"."lesson_type" IS 'Тип квиза (например, склонения, спряжения, лексика)';
COMMENT ON COLUMN "content"."lesson"."difficulty" IS 'Уровень сложности квиза';
COMMENT ON COLUMN "content"."lesson"."questions_per_session" IS 'Количество вопросов в одной сессии квиза';
COMMENT ON COLUMN "content"."lesson"."created_at" IS 'Дата и время создания записи';
COMMENT ON COLUMN "content"."lesson"."deleted_at" IS 'Дата и время удаления записи (для мягкого удаления)';
COMMENT ON TABLE "content"."lesson" IS 'Таблица для хранения информации о квизах';

-- ----------------------------
-- Table structure for vocabulary_categories
-- ----------------------------
DROP TABLE IF EXISTS "content"."vocabulary_categories";
CREATE TABLE "content"."vocabulary_categories" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "code" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "parent_id" uuid,
  "name_ru" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "name_en" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "description_ru" text COLLATE "pg_catalog"."default",
  "description_en" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for vocabulary_word_categories
-- ----------------------------
DROP TABLE IF EXISTS "content"."vocabulary_word_categories";
CREATE TABLE "content"."vocabulary_word_categories" (
  "vocabulary_word_id" uuid NOT NULL,
  "category_id" uuid NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Table structure for vocabulary_words
-- ----------------------------
DROP TABLE IF EXISTS "content"."vocabulary_words";
CREATE TABLE "content"."vocabulary_words" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "word_iast" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "word_devanagari" varchar COLLATE "pg_catalog"."default",
  "translation_ru" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "translation_en" varchar COLLATE "pg_catalog"."default" NOT NULL,
  "gender" varchar COLLATE "pg_catalog"."default",
  "stem" varchar COLLATE "pg_catalog"."default",
  "root" varchar COLLATE "pg_catalog"."default",
  "explanation_ru" varchar COLLATE "pg_catalog"."default",
  "explanation_en" varchar COLLATE "pg_catalog"."default",
  "created_at" timestamptz(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamptz(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "content"."vocabulary_words"."id" IS 'Уникальный идентификатор словарного слова';
COMMENT ON COLUMN "content"."vocabulary_words"."word_iast" IS 'Слово в IAST';
COMMENT ON COLUMN "content"."vocabulary_words"."word_devanagari" IS 'Слово в деванагари';
COMMENT ON COLUMN "content"."vocabulary_words"."translation_ru" IS 'Перевод слова на русский язык';
COMMENT ON COLUMN "content"."vocabulary_words"."translation_en" IS 'Перевод слова на английский язык';
COMMENT ON COLUMN "content"."vocabulary_words"."gender" IS 'Грамматический род слова';
COMMENT ON COLUMN "content"."vocabulary_words"."stem" IS 'Основа слова';
COMMENT ON COLUMN "content"."vocabulary_words"."root" IS 'Корень слова';
COMMENT ON COLUMN "content"."vocabulary_words"."created_at" IS 'Дата и время создания записи';
COMMENT ON COLUMN "content"."vocabulary_words"."updated_at" IS 'Дата и время последнего обновления записи';
COMMENT ON TABLE "content"."vocabulary_words" IS 'Таблица для хранения словарных слов';

-- ----------------------------
-- Uniques structure for table case_endings
-- ----------------------------
ALTER TABLE "content"."case_endings" ADD CONSTRAINT "uq_case_endings" UNIQUE ("vowel_type", "gender", "case_type", "number_type");

-- ----------------------------
-- Checks structure for table case_endings
-- ----------------------------
ALTER TABLE "content"."case_endings" ADD CONSTRAINT "ck_vowel_type" CHECK (vowel_type::text = ANY (ARRAY['A_STEM'::character varying, 'AA_STEM'::character varying, 'I_STEM'::character varying, 'II_STEM'::character varying, 'U_STEM'::character varying, 'UU_STEM'::character varying, 'R_STEM'::character varying]::text[]));
ALTER TABLE "content"."case_endings" ADD CONSTRAINT "ck_gender" CHECK (gender::text = ANY (ARRAY['MASCULINE'::character varying, 'FEMININE'::character varying, 'NEUTER'::character varying, 'UNKNOWN'::character varying, 'UNSPECIFIED'::character varying]::text[]));
ALTER TABLE "content"."case_endings" ADD CONSTRAINT "ck_case_type" CHECK (case_type::text = ANY (ARRAY['NOMINATIVE'::character varying, 'ACCUSATIVE'::character varying, 'INSTRUMENTAL'::character varying, 'DATIVE'::character varying, 'ABLATIVE'::character varying, 'GENITIVE'::character varying, 'LOCATIVE'::character varying, 'VOCATIVE'::character varying]::text[]));
ALTER TABLE "content"."case_endings" ADD CONSTRAINT "ck_number_type" CHECK (number_type::text = ANY (ARRAY['SINGULAR'::character varying, 'DUAL'::character varying, 'PLURAL'::character varying]::text[]));
COMMENT ON CONSTRAINT "ck_vowel_type" ON "content"."case_endings" IS 'Допустимые типы гласных основ (Java enum VowelType)';
COMMENT ON CONSTRAINT "ck_gender" ON "content"."case_endings" IS 'Допустимые роды (Java enum Gender)';
COMMENT ON CONSTRAINT "ck_case_type" ON "content"."case_endings" IS 'Допустимые падежи (Java enum CaseType)';
COMMENT ON CONSTRAINT "ck_number_type" ON "content"."case_endings" IS 'Допустимые числа (Java enum NumberType)';

-- ----------------------------
-- Primary Key structure for table case_endings
-- ----------------------------
ALTER TABLE "content"."case_endings" ADD CONSTRAINT "pk_case_endings" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table declension_example_groups
-- ----------------------------
ALTER TABLE "content"."declension_example_groups" ADD CONSTRAINT "declension_example_groups_vowel_type_gender_case_type_numbe_key" UNIQUE ("vowel_type", "gender", "case_type", "number_type");

-- ----------------------------
-- Primary Key structure for table declension_example_groups
-- ----------------------------
ALTER TABLE "content"."declension_example_groups" ADD CONSTRAINT "declension_example_groups_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Checks structure for table declension_forms
-- ----------------------------
ALTER TABLE "content"."declension_forms" ADD CONSTRAINT "ck_case_type" CHECK (case_type::text = ANY (ARRAY['NOMINATIVE'::character varying::text, 'ACCUSATIVE'::character varying::text, 'INSTRUMENTAL'::character varying::text, 'DATIVE'::character varying::text, 'ABLATIVE'::character varying::text, 'GENITIVE'::character varying::text, 'LOCATIVE'::character varying::text, 'VOCATIVE'::character varying::text]));
ALTER TABLE "content"."declension_forms" ADD CONSTRAINT "ck_number_type" CHECK (number_type::text = ANY (ARRAY['SINGULAR'::character varying::text, 'DUAL'::character varying::text, 'PLURAL'::character varying::text]));

-- ----------------------------
-- Primary Key structure for table declension_forms
-- ----------------------------
ALTER TABLE "content"."declension_forms" ADD CONSTRAINT "declension_forms_pkey" PRIMARY KEY ("declension_stem_id", "case_type", "number_type");

-- ----------------------------
-- Uniques structure for table declension_stems
-- ----------------------------
ALTER TABLE "content"."declension_stems" ADD CONSTRAINT "declension_stems_stem_iast_key" UNIQUE ("stem_iast");

-- ----------------------------
-- Checks structure for table declension_stems
-- ----------------------------
ALTER TABLE "content"."declension_stems" ADD CONSTRAINT "ck_vowel_type" CHECK (vowel_type::text = ANY (ARRAY['A_STEM'::text, 'AA_STEM'::text, 'I_STEM'::text, 'II_STEM'::text, 'U_STEM'::text, 'UU_STEM'::text, 'R_STEM'::text, 'PRON_AHAM'::text, 'PRON_TVAM'::text, 'PRON_TAD'::text, 'PRON_ETAD'::text, 'PRON_IDAM'::text, 'PRON_KIM'::text, 'PRON_YAD'::text, 'PRON_REFLEXIVE'::text]));
ALTER TABLE "content"."declension_stems" ADD CONSTRAINT "ck_gender" CHECK (gender::text = ANY (ARRAY['MASCULINE'::text, 'FEMININE'::text, 'NEUTER'::text, 'UNKNOWN'::text, 'UNSPECIFIED'::text]));

-- ----------------------------
-- Primary Key structure for table declension_stems
-- ----------------------------
ALTER TABLE "content"."declension_stems" ADD CONSTRAINT "declension_stems_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table flyway_schema_history
-- ----------------------------
CREATE INDEX "flyway_schema_history_s_idx" ON "content"."flyway_schema_history" USING btree (
  "success" "pg_catalog"."bool_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table flyway_schema_history
-- ----------------------------
ALTER TABLE "content"."flyway_schema_history" ADD CONSTRAINT "flyway_schema_history_pk" PRIMARY KEY ("installed_rank");

-- ----------------------------
-- Uniques structure for table lesson
-- ----------------------------
ALTER TABLE "content"."lesson" ADD CONSTRAINT "quizzes_slug_key" UNIQUE ("slug");

-- ----------------------------
-- Checks structure for table lesson
-- ----------------------------
ALTER TABLE "content"."lesson" ADD CONSTRAINT "ck_difficulty" CHECK (difficulty::text = ANY (ARRAY['BEGINNER'::character varying::text, 'INTERMEDIATE'::character varying::text, 'ADVANCED'::character varying::text]));
ALTER TABLE "content"."lesson" ADD CONSTRAINT "ck_slug_format" CHECK (slug::text ~ '^[a-z0-9\.:]+(?:-[a-z0-9\.:]+)*$'::text);

-- ----------------------------
-- Primary Key structure for table lesson
-- ----------------------------
ALTER TABLE "content"."lesson" ADD CONSTRAINT "quizzes_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table vocabulary_categories
-- ----------------------------
ALTER TABLE "content"."vocabulary_categories" ADD CONSTRAINT "vocabulary_categories_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table vocabulary_categories
-- ----------------------------
ALTER TABLE "content"."vocabulary_categories" ADD CONSTRAINT "vocabulary_categories_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table vocabulary_word_categories
-- ----------------------------
CREATE INDEX "idx_vwc_category" ON "content"."vocabulary_word_categories" USING btree (
  "category_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);
CREATE INDEX "idx_vwc_word" ON "content"."vocabulary_word_categories" USING btree (
  "vocabulary_word_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table vocabulary_word_categories
-- ----------------------------
ALTER TABLE "content"."vocabulary_word_categories" ADD CONSTRAINT "vocabulary_word_categories_pkey" PRIMARY KEY ("vocabulary_word_id", "category_id");

-- ----------------------------
-- Indexes structure for table vocabulary_words
-- ----------------------------
CREATE INDEX "idx_vocabulary_words_devanagari" ON "content"."vocabulary_words" USING btree (
  "word_devanagari" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_vocabulary_words_iast" ON "content"."vocabulary_words" USING btree (
  "word_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_vocabulary_words_root" ON "content"."vocabulary_words" USING btree (
  "root" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table vocabulary_words
-- ----------------------------
ALTER TABLE "content"."vocabulary_words" ADD CONSTRAINT "ck_vocabulary_gender" CHECK (gender::text = ANY (ARRAY['MASCULINE'::text, 'FEMININE'::text, 'NEUTER'::text, 'UNKNOWN'::text, 'UNSPECIFIED'::text]));

-- ----------------------------
-- Primary Key structure for table vocabulary_words
-- ----------------------------
ALTER TABLE "content"."vocabulary_words" ADD CONSTRAINT "vocabulary_words_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table declension_forms
-- ----------------------------
ALTER TABLE "content"."declension_forms" ADD CONSTRAINT "declension_forms_declension_stem_id_fkey" FOREIGN KEY ("declension_stem_id") REFERENCES "content"."declension_stems" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table vocabulary_categories
-- ----------------------------
ALTER TABLE "content"."vocabulary_categories" ADD CONSTRAINT "fk_vocabulary_categories_parent" FOREIGN KEY ("parent_id") REFERENCES "content"."vocabulary_categories" ("id") ON DELETE RESTRICT ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table vocabulary_word_categories
-- ----------------------------
ALTER TABLE "content"."vocabulary_word_categories" ADD CONSTRAINT "fk_vwc_category" FOREIGN KEY ("category_id") REFERENCES "content"."vocabulary_categories" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "content"."vocabulary_word_categories" ADD CONSTRAINT "fk_vwc_word" FOREIGN KEY ("vocabulary_word_id") REFERENCES "content"."vocabulary_words" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
