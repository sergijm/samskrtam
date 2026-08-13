-- V16: semantic topics become lesson topics.
--
-- * topic gains semantic_topic_id FK (semantic classifier node that the lesson
--   is built from; NULL for standalone/frequency lessons);
-- * all hand-crafted lex-* lessons are removed EXCEPT the frequency-based
--   evergreen review (lex-core-vocabulary-review);
-- * 40 new lessons are generated from the 40 leaf nodes of the semantic tree
--   (code/title come from semantic_topic, level/order assigned explicitly);
-- * lexical_topic_binding is dropped: lesson <-> lexeme composition now comes
--   solely from curriculum.lexeme_semantic_topic via topic.semantic_topic_id.

ALTER TABLE curriculum.topic ADD COLUMN semantic_topic_id UUID NULL
    REFERENCES curriculum.semantic_topic (id);

-- 1. Remove old lexical lessons (keep only the frequency evergreen review).
DELETE FROM curriculum.topic
WHERE domain = 'LEXICON'
  AND code LIKE 'lex-%'
  AND code <> 'lex-core-vocabulary-review';

-- 2. Create one lesson per semantic leaf (display_order restarts per level).
INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order, domain, semantic_topic_id)
SELECT gen_random_uuid(), st.code, st.name_ru, st.name_en, v.level, false, v.ord, 'LEXICON', st.id
FROM curriculum.semantic_topic st
JOIN (VALUES
    -- L0 — базовый бытовой словарь
    ('people-family',            'L0', 1),
    ('people-body-parts',        'L0', 2),
    ('movement-physical-action', 'L0', 3),
    -- L1 — природа и быт
    ('nature-animals',           'L1', 1),
    ('nature-plants',            'L1', 2),
    ('nature-landscape',         'L1', 3),
    ('nature-water',             'L1', 4),
    ('everyday-food-drink',      'L1', 5),
    ('everyday-house-dwelling',  'L1', 6),
    ('everyday-clothing-ornament','L1', 7),
    -- L2 — расширение бытового словаря, движения, речь
    ('nature-weather-sky',       'L2', 1),
    ('nature-agriculture',       'L2', 2),
    ('people-occupations',       'L2', 3),
    ('everyday-travel-vehicles', 'L2', 4),
    ('everyday-objects-tools',   'L2', 5),
    ('everyday-materials',       'L2', 6),
    ('movement-motion-verbs',    'L2', 7),
    ('speech-acts',              'L2', 8),
    ('cognition-senses',         'L2', 9),
    ('abstract-time',            'L2', 10),
    -- L3 — абстракция, эмоции, социум
    ('people-social-relations',  'L3', 1),
    ('people-character',         'L3', 2),
    ('people-royalty-hierarchy', 'L3', 3),
    ('emotion-positive',         'L3', 4),
    ('emotion-negative',         'L3', 5),
    ('emotion-desire-will',      'L3', 6),
    ('abstract-quantity-number', 'L3', 7),
    ('abstract-space-direction', 'L3', 8),
    ('abstract-cause-purpose',   'L3', 9),
    ('abstract-comparison',      'L3', 10),
    -- L4 — познание, ритуал, этика
    ('movement-rest-stillness',  'L4', 1),
    ('speech-naming-address',    'L4', 2),
    ('speech-question-answer',   'L4', 3),
    ('cognition-thought-memory', 'L4', 4),
    ('cognition-knowledge-learning', 'L4', 5),
    ('society-ritual-worship',   'L4', 6),
    ('society-ethics-duty',      'L4', 7),
    ('society-governance-law',   'L4', 8),
    ('society-philosophy-liberation', 'L4', 9),
    -- L5 — широкая лексика
    ('society-war-conflict',     'L5', 1)
) AS v(code, level, ord) ON v.code = st.code
WHERE st.parent_id IS NOT NULL;

-- 3. Lesson composition now lives in lexeme_semantic_topic only.
DROP TABLE IF EXISTS curriculum.lexical_topic_binding;
