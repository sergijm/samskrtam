-- =============================================
-- V14: Russian-localized variants on quest_item
-- =============================================
-- Английские prompt/correct_answer/distractors остаются каноническими (используются
-- для верификации и истории). Дополнительно храним русские варианты текста, чтобы
-- клиент мог выбрать язык отображения без переписания данных на лету.
--
--   prompt_ru         русский текст вопроса (заполняется batch-генератором для всех
--                     4 типов; NULL на строках до V14 — нужна регенерация)
--   correct_answer_ru русский вариант эталонной метки; только для CASE_RECOGNITION
--                     (русские названия падежа/числа/рода), NULL для остальных
--   distractors_ru    русские варианты дистракторов; только для CASE_RECOGNITION

ALTER TABLE curriculum.quest_item
    ADD COLUMN prompt_ru TEXT NULL,
    ADD COLUMN correct_answer_ru VARCHAR(200) NULL,
    ADD COLUMN distractors_ru JSONB NULL;

COMMENT ON COLUMN curriculum.quest_item.prompt_ru IS
    'Russian prompt text produced by the batch generator for all four DECLENSION types; NULL for rows generated before V14 (requires regeneration).';
COMMENT ON COLUMN curriculum.quest_item.correct_answer_ru IS
    'Russian canonical answer label (CASE_RECOGNITION only); NULL otherwise and for MATCHING.';
COMMENT ON COLUMN curriculum.quest_item.distractors_ru IS
    'Russian distractor labels (CASE_RECOGNITION only); NULL otherwise.';