-- target_item_count on topic: документированная цель числа существительных /
-- вопросов, которые должен засеять bootstrapper для данной темы склонения.
-- default=0 — тема не требующая наполнения по target (bootstrapper не трогает).

ALTER TABLE curriculum.topic
    ADD COLUMN target_item_count INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN curriculum.topic.target_item_count IS
    'Target noun/lexeme count for this declension topic, filled by the declension bootstrapper from sangraha; 0 = not a bootstrap target.';

-- Цели наполнения для noun-тем склонений. Код темы должен совпадать с
-- morphology_class.code, по которому генератор находит лексемы
-- (см. DeclensionQuestItemBatchGenerator.bindMorphologyClass).
UPDATE curriculum.topic SET target_item_count = 12 WHERE code IN (
    'a-stem-masc',
    'a-stem-neut',
    'a-stem-fem'
);