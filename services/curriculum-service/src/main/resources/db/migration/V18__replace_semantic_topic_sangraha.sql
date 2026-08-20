-- V18: replace semantic_topic with sangraha taxonomy (9 roots + 38 leaves)
-- The codes now follow sangraha convention: roots use domain prefixes,
-- leaves use short names without parent prefix.

-- Detach lexical topics from semantic topics so FK constraint doesn't block delete
UPDATE curriculum.topic SET semantic_topic_id = NULL WHERE domain = 'LEXICON';

-- Clean join table
DELETE FROM curriculum.lexeme_semantic_topic;

-- Remove old taxonomy
DELETE FROM curriculum.semantic_topic;

-- Insert roots (parent_id NULL)
INSERT INTO curriculum.semantic_topic (id, code, name_ru, name_en, parent_id) VALUES
(gen_random_uuid(), 'nature',           'Природа',                'Nature',                NULL),
(gen_random_uuid(), 'people-body',      'Люди и тело',            'People and body',       NULL),
(gen_random_uuid(), 'everyday',         'Быт',                    'Everyday life',         NULL),
(gen_random_uuid(), 'movement-action',  'Движение и действие',    'Movement and action',   NULL),
(gen_random_uuid(), 'speech-comm',      'Речь и общение',         'Speech and communication', NULL),
(gen_random_uuid(), 'perception-cog',   'Восприятие и познание',  'Perception and cognition', NULL),
(gen_random_uuid(), 'emotion-char',     'Эмоции и характер',      'Emotion and character',  NULL),
(gen_random_uuid(), 'abstract',         'Абстрактное',            'Abstract',               NULL),
(gen_random_uuid(), 'society-ritual',   'Общество и ритуал',      'Society and ritual',     NULL);

-- Insert leaves with parent references
INSERT INTO curriculum.semantic_topic (id, code, name_ru, name_en, parent_id)
SELECT gen_random_uuid(), v.code, v.name_ru, v.name_en, p.id
FROM (VALUES
    -- nature
    ('animals',           'nature', 'Животные',             'Animals'),
    ('plants-trees',      'nature', 'Растения и деревья',   'Plants and trees'),
    ('water',             'nature', 'Вода и реки',           'Waters and rivers'),
    ('landscape',         'nature', 'Ландшафт и рельеф',     'Landscape and terrain'),
    ('sky-weather',       'nature', 'Погода и небо',         'Weather and sky'),
    -- people-body
    ('body-parts',        'people-body', 'Части тела',              'Body parts'),
    ('family-kin',        'people-body', 'Семья и родня',           'Family and relatives'),
    ('professions',       'people-body', 'Занятия и профессии',     'Occupations and professions'),
    ('social-relations',  'people-body', 'Социальные связи',        'Social relations'),
    -- everyday
    ('food-drink',        'everyday', 'Еда и напитки',              'Food and drink'),
    ('house-dwelling',    'everyday', 'Жилище',                     'House and dwelling'),
    ('garments',          'everyday', 'Одежда и украшения',         'Garments and ornaments'),
    ('travel-vehicles',   'everyday', 'Путешествия и транспорт',    'Travel and vehicles'),
    ('tools-materials',   'everyday', 'Инструменты и материалы',    'Tools and materials'),
    -- movement-action
    ('motion-verbs',      'movement-action', 'Глаголы движения',    'Motion verbs'),
    ('physical-action',   'movement-action', 'Физическое действие',  'Physical action'),
    ('rest-stillness',    'movement-action', 'Покой и неподвижность', 'Rest and stillness'),
    -- speech-comm
    ('speech-acts',       'speech-comm', 'Речевые акты',            'Speech acts'),
    ('naming-address',    'speech-comm', 'Именование и обращение',  'Naming and address'),
    ('question-answer',   'speech-comm', 'Вопрос и ответ',          'Question and answer'),
    -- perception-cog
    ('senses',            'perception-cog', 'Органы чувств',        'Senses'),
    ('thought-memory',    'perception-cog', 'Мысль и память',       'Thought and memory'),
    ('learning',          'perception-cog', 'Знание и учение',      'Knowledge and learning'),
    -- emotion-char
    ('emotions-positive',  'emotion-char', 'Положительные эмоции',  'Positive emotions'),
    ('emotions-negative',  'emotion-char', 'Отрицательные эмоции',  'Negative emotions'),
    ('desire-will',        'emotion-char', 'Желание и воля',        'Desire and will'),
    -- abstract
    ('time-seasons',       'abstract', 'Время: части суток и сезоны','Time and seasons'),
    ('quantity-number',    'abstract', 'Количество и числа',         'Quantity and number'),
    ('space-direction',    'abstract', 'Пространство и направление', 'Space and direction'),
    -- society-ritual
    ('ritual-worship',     'society-ritual', 'Ритуал и поклонение',     'Ritual and worship'),
    ('law-rule',           'society-ritual', 'Право и управление',      'Law and governance'),
    ('phi-moksha',         'society-ritual', 'Философия и мокша',       'Philosophy and liberation'),
    ('war-conflict',       'society-ritual', 'Война и конфликт',        'War and conflict')
) AS v(code, parent_code, name_ru, name_en)
JOIN curriculum.semantic_topic p ON p.code = v.parent_code;