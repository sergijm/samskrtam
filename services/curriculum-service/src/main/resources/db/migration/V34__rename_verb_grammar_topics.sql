-- V34: three verb conjugation lessons renamed to Latin namespace + releveled.
--
-- The curriculum used English/kebab codes for the three basic verb lessons;
-- the course now labels them by their classical Latin names and introduces
-- them earlier:
--
--   present-parasmaipada-formation (L2) -> presence-indicativus (L1)  «Настоящее время изъявительного наклонения»
--   imperfect                        (L3) -> imperfectum            (L1)  «Прошедшее незавершенное время изъявительного наклонения»
--   optative                         (L4) -> optativus              (L2)  «Желательное наклонение»
--
-- Prerequisite edges and quest bindings reference topic_id (UUID), not code,
-- so renaming a code does not break them. display_order is appended to the
-- end of the target learning level (tie-break within a level, curriculum.md §2).

UPDATE curriculum.topic
SET code = 'presence-indicativus',
    title_ru = 'Настоящее время изъявительного наклонения',
    title_en = 'Present indicative',
    learning_level = 'L1',
    display_order = (SELECT COALESCE(MAX(display_order), 0) + 1
                     FROM curriculum.topic WHERE learning_level = 'L1')
WHERE code = 'present-parasmaipada-formation';

UPDATE curriculum.topic
SET code = 'imperfectum',
    title_ru = 'Прошедшее незавершенное время изъявительного наклонения',
    title_en = 'Imperfect indicative',
    learning_level = 'L1',
    display_order = (SELECT COALESCE(MAX(display_order), 0) + 1
                     FROM curriculum.topic WHERE learning_level = 'L1')
WHERE code = 'imperfect';

UPDATE curriculum.topic
SET code = 'optativus',
    title_ru = 'Желательное наклонение',
    title_en = 'Optative mood',
    learning_level = 'L2',
    display_order = (SELECT COALESCE(MAX(display_order), 0) + 1
                     FROM curriculum.topic WHERE learning_level = 'L2')
WHERE code = 'optative';
