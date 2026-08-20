-- V27: rebind long-vowel (-ī/-ū) lexemes to their own morphology classes.
--
-- Before the fix to LexiconImportService.mapMorphologyCode, II_STEM/UU_STEM
-- lemmas were bound to the short-vowel classes i-stem/u-stem (a legacy of the
-- collapsed i/u mapping). The new `ii-uu-stems` lesson (V26) selects lexemes by
-- the dedicated ii-stem/uu-stem classes, so existing lexemes must be re-bound.
-- The paradigm form table (curriculum.declension_form) carries the LLM-assigned
-- vowel_type and is the authoritative source here: a lemma with II_STEM forms is
-- an -ī stem, one with UU_STEM forms is an -ū stem.

INSERT INTO curriculum.lexeme_morphology (lexeme_id, morphology_class_code)
SELECT DISTINCT l.id, 'ii-stem'
FROM curriculum.declension_form df
JOIN curriculum.lexeme l ON l.lemma_iast = df.lemma_iast
WHERE df.vowel_type = 'II_STEM'
ON CONFLICT (lexeme_id, morphology_class_code) DO NOTHING;

INSERT INTO curriculum.lexeme_morphology (lexeme_id, morphology_class_code)
SELECT DISTINCT l.id, 'uu-stem'
FROM curriculum.declension_form df
JOIN curriculum.lexeme l ON l.lemma_iast = df.lemma_iast
WHERE df.vowel_type = 'UU_STEM'
ON CONFLICT (lexeme_id, morphology_class_code) DO NOTHING;
