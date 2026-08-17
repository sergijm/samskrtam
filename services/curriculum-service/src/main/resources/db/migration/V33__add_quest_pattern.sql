-- quest_pattern: decorative cognitive-operation label from quest_catalog_2.md
-- (e.g. "nom-form", "lex-tran"), see docs/handoff/quest-patterns-state.md.
-- Not used for answer checking (AnswerMode), session selection or progress.

ALTER TABLE curriculum.quest_item
    ADD COLUMN quest_pattern VARCHAR(16) NULL;

COMMENT ON COLUMN curriculum.quest_item.quest_pattern
    IS 'Cognitive-operation label from quest_catalog_2.md (decorative, e.g. nom-form).';