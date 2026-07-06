/*
  Migration V2: Declension stems reference table + stem fields in session_questions

  Created: 2025-07-04
  Description: Add quiz.declension_stems table and stem_devanagari / stem_translation_ru / stem_translation_en columns to quiz.session_questions
*/

-- ----------------------------
-- Table structure for declension_stems
-- ----------------------------
DROP TABLE IF EXISTS "quiz"."declension_stems";
CREATE TABLE "quiz"."declension_stems" (
    "id" uuid NOT NULL DEFAULT gen_random_uuid(),
    "stem_iast" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
    "stem_devanagari" varchar(255) COLLATE "pg_catalog"."default",
    "translation_ru" varchar(255) COLLATE "pg_catalog"."default",
    "translation_en" varchar(255) COLLATE "pg_catalog"."default",
    "gender" varchar(50) COLLATE "pg_catalog"."default",
    "vowel_type" varchar(50) COLLATE "pg_catalog"."default"
);

-- ----------------------------
-- Primary Key structure for table declension_stems
-- ----------------------------
ALTER TABLE "quiz"."declension_stems" ADD CONSTRAINT "declension_stems_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- New columns in session_questions
-- ----------------------------
ALTER TABLE "quiz"."session_questions"
    ADD COLUMN IF NOT EXISTS "stem_devanagari" varchar(255) COLLATE "pg_catalog"."default",
    ADD COLUMN IF NOT EXISTS "stem_translation_ru" varchar(255) COLLATE "pg_catalog"."default",
    ADD COLUMN IF NOT EXISTS "stem_translation_en" varchar(255) COLLATE "pg_catalog"."default";