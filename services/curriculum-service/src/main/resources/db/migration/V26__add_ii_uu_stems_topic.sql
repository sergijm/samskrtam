-- V26: long-vowel feminine stems lesson (-ī/-ū), parallel to i-u-stems.
--
-- Adds the morphology classes the lesson is built from (ii-stem, uu-stem) and
-- the curriculum topic `ii-uu-stems` (L2, after i-u-stems/r-stems), with a
-- target item count so the declension bootstrapper fills its lexemes from
-- sangraha (DeclensionDataImporter) and the batch generator can seed quest
-- items.

-- ----------------------------------------------------------------------------
-- Morphology classes (parallel to i-stem/u-stem, see V5)
-- ----------------------------------------------------------------------------

INSERT INTO curriculum.morphology_class (code, applies_to, name_ru, name_en) VALUES
    ('ii-stem', 'NOUN', 'ī-основа, жен.р.', 'ī-stem feminine'),
    ('uu-stem', 'NOUN', 'ū-основа, жен.р.', 'ū-stem feminine')
ON CONFLICT (code) DO NOTHING;

-- ----------------------------------------------------------------------------
-- Topic (lesson)
-- ----------------------------------------------------------------------------

INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order, domain, domain_type, target_item_count)
VALUES (gen_random_uuid(), 'ii-uu-stems', 'Склонение на -ī и -ū', 'ī- and ū-stems', 'L2', false, 6, 'NOMINAL_MORPHOLOGY', 'GRAMMAR', 24);

-- ----------------------------------------------------------------------------
-- Prerequisite edges: long-vowel stems build on the short-vowel i/u lesson.
-- ----------------------------------------------------------------------------

INSERT INTO curriculum.topic_prerequisite (topic_id, prerequisite_topic_id, strength)
SELECT t.id, p.id, 'RECOMMENDED'
FROM (VALUES
    ('ii-uu-stems', 'i-u-stems'),
    ('ii-uu-stems', 'a-stem-fem')
) AS e(topic_code, prerequisite_code)
JOIN curriculum.topic t ON t.code = e.topic_code
JOIN curriculum.topic p ON p.code = e.prerequisite_code;
