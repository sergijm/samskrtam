-- V2__add_word_score.sql
-- Добавляет таблицу word_score для кэширования агрегированной статистики слов

CREATE TABLE IF NOT EXISTS "quiz"."word_score" (
  "id" uuid NOT NULL DEFAULT gen_random_uuid(),
  "user_id" uuid NOT NULL,
  "word_id" uuid NOT NULL,
  "lesson_id" uuid NOT NULL,
  "score" int4 NOT NULL DEFAULT 0,
  "updated_at" timestamptz(6) NOT NULL DEFAULT now(),
  CONSTRAINT "uq_word_score" UNIQUE ("user_id", "word_id", "lesson_id")
);

CREATE INDEX IF NOT EXISTS "idx_word_score_user_lesson" ON "quiz"."word_score" USING btree (
  "user_id" "pg_catalog"."uuid_ops" ASC NULLS LAST,
  "lesson_id" "pg_catalog"."uuid_ops" ASC NULLS LAST
);

-- Primary Key
ALTER TABLE "quiz"."word_score" ADD CONSTRAINT "word_score_pkey" PRIMARY KEY ("id");