-- =============================================
-- V9: Data-fix — fill stem_devanagari, translation_ru, translation_en
-- for existing declension_stems rows
-- =============================================
-- Context:
--   - V2 inserted stems from raw_data.sanskrit_declensions_enriched into
--     content.declension_stems with stem_name_iast/vowel_type/gender only.
--   - V7 added translation_ru, translation_en columns (nullable).
--   - V8 renamed stem_name_iast → stem_iast, dropped stem_name_devanagari.
--   - stem_devanagari column existed since V1 but was never populated by V2 seed.
--   - This data-fix fills stem_devanagari, translation_ru, translation_en
--     for all currently existing stems.
--
-- Data source: user-provided (see content-service.md §9).
-- Each UPDATE block below matches a stem by its stem_iast value.

-- =============================================
-- A-STEMS (masculine, neuter)
-- =============================================

-- Данные будут предоставлены пользователем
-- Пример формата:
-- UPDATE content.declension_stems
--    SET stem_devanagari = 'देव',
--        translation_ru  = 'бог',
--        translation_en  = 'god'
--  WHERE stem_iast = 'deva';

-- =============================================
-- AA-STEMS (feminine)
-- =============================================

-- Данные будут предоставлены пользователем

-- =============================================
-- I-STEMS
-- =============================================

-- Данные будут предоставлены пользователем

-- =============================================
-- II-STEMS
-- =============================================

-- Данные будут предоставлены пользователем

-- =============================================
-- U-STEMS
-- =============================================

-- Данные будут предоставлены пользователем

-- =============================================
-- UU-STEMS
-- =============================================

-- Данные будут предоставлены пользователем

-- =============================================
-- R-STEMS
-- =============================================

-- Данные будут предоставлены пользователем

-- =============================================
-- Verify: after running data-fix, check for NULLs
-- =============================================
-- SELECT stem_iast FROM content.declension_stems
--  WHERE stem_devanagari IS NULL
--     OR translation_ru IS NULL
--     OR translation_en IS NULL;
