-- progress_tag on quest_item: прогресс-тег, заполняемый batch-генератором при
-- генерации квеста. DECLENSION_*: "caseType|numberType|gender" (для DECLENSION_MATCH
-- берётся первая пара, gender = UNSPECIFIED); VOCABULARY_WORD: lemma в кодировке SLP1.
-- NULL на строках, сгенерированных до этой миграции (бэкфилла нет).
-- Ограничение длины 255: один тег (не массив).

ALTER TABLE curriculum.quest_item
    ADD COLUMN progress_tag VARCHAR(255) NULL;

COMMENT ON COLUMN curriculum.quest_item.progress_tag IS
    'Progress grouping tag populated by the batch generator: DECLENSION_* -> caseType|numberType|gender (MATCH takes first pair, gender=UNSPECIFIED); VOCABULARY_WORD -> lemmaSlp1. NULL for rows generated before V13.';