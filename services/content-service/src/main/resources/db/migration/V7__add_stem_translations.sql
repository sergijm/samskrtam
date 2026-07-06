-- =============================================
-- V7: Add translation columns to declension_stems
-- =============================================

ALTER TABLE content.declension_stems
ADD COLUMN IF NOT EXISTS translation_ru VARCHAR(255),
ADD COLUMN IF NOT EXISTS translation_en VARCHAR(255);

COMMENT ON COLUMN content.declension_stems.translation_ru IS 'Перевод основы на русский язык';
COMMENT ON COLUMN content.declension_stems.translation_en IS 'Перевод основы на английский язык';

ALTER TABLE content.generated_questions
ADD COLUMN IF NOT EXISTS stem_translation_ru VARCHAR(255),
ADD COLUMN IF NOT EXISTS stem_translation_en VARCHAR(255),
ADD COLUMN IF NOT EXISTS stem_devanagari VARCHAR(255);
