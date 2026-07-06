-- =============================================
-- V8: Rename stem_name_iast -> stem_iast, drop stem_name_devanagari, unify stem_devanagari
-- =============================================

-- 1. Drop old unique constraint on stem_name_iast
ALTER TABLE content.declension_stems
    DROP CONSTRAINT IF EXISTS declension_stems_stem_name_iast_key;

-- 2. Rename stem_name_iast -> stem_iast
ALTER TABLE content.declension_stems
    RENAME COLUMN stem_name_iast TO stem_iast;

-- 3. Add unique constraint on stem_iast
ALTER TABLE content.declension_stems
    ADD CONSTRAINT declension_stems_stem_iast_key UNIQUE (stem_iast);

-- 4. Drop stem_name_devanagari (duplicate of stem_devanagari)
ALTER TABLE content.declension_stems
    DROP COLUMN IF EXISTS stem_name_devanagari;

-- 5. Update comments
COMMENT ON COLUMN content.declension_stems.stem_iast IS 'Название основы в IAST';
COMMENT ON COLUMN content.declension_stems.stem_devanagari IS 'Название основы в деванагари';