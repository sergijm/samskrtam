-- Drop generated_quiz_data_id column from quiz_session (no longer needed)
-- Content-service no longer persists generated quiz data.
ALTER TABLE "quiz"."quiz_session" DROP COLUMN IF EXISTS "generated_quiz_data_id";

-- Add stem-related columns to session_questions if they don't exist
ALTER TABLE "quiz"."session_questions" ADD COLUMN IF NOT EXISTS "stem" VARCHAR(255);
ALTER TABLE "quiz"."session_questions" ADD COLUMN IF NOT EXISTS "stem_devanagari" VARCHAR(255);
ALTER TABLE "quiz"."session_questions" ADD COLUMN IF NOT EXISTS "stem_translation_ru" VARCHAR(255);
ALTER TABLE "quiz"."session_questions" ADD COLUMN IF NOT EXISTS "stem_translation_en" VARCHAR(255);
ALTER TABLE "quiz"."session_questions" ADD COLUMN IF NOT EXISTS "target_gender" VARCHAR(50);