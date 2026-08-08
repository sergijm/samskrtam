-- Справочники классификации (lemma-classification.md §1.4–§1.5).
-- classification_scheme — расширяемая модель схем; CURRICULUM активна, WORDNET зарезервирована.
-- curriculum_semantic_topic — редактируемая копия таксономии lexical-curriculum.md §3:
-- ровно 9 корней (parent_code NULL) + 33 листа (parent_code задан) = 42 строки.
BEGIN;

CREATE TABLE "sangraha"."classification_scheme" (
    "code" varchar(20) NOT NULL,
    "title_ru" varchar(100) NOT NULL,
    "is_active" boolean NOT NULL DEFAULT true,
    CONSTRAINT "pk_classification_scheme" PRIMARY KEY ("code")
);

INSERT INTO "sangraha"."classification_scheme" ("code", "title_ru", "is_active") VALUES
    ('CURRICULUM', 'Семантическая таксономия curriculum', true),
    ('WORDNET',  'Synset-классификация (зарезервирована)', false);

CREATE TABLE "sangraha"."curriculum_semantic_topic" (
    "code" varchar(40) NOT NULL,
    "parent_code" varchar(40),
    "label_ru" varchar(100) NOT NULL,
    "label_en" varchar(100) NOT NULL,
    "description" text,
    CONSTRAINT "pk_curriculum_semantic_topic" PRIMARY KEY ("code"),
    CONSTRAINT "fk_curriculum_semantic_topic_parent"
        FOREIGN KEY ("parent_code") REFERENCES "sangraha"."curriculum_semantic_topic" ("code")
);

-- === 9 корней (parent_code NULL) ===
INSERT INTO "sangraha"."curriculum_semantic_topic"
    ("code", "parent_code", "label_ru", "label_en", "description")
VALUES
    ('nature',          NULL, 'Природа', 'Nature', 'Живая и неживая природа'),
    ('people-body',     NULL, 'Люди и тело', 'People and body', 'Человек и части тела'),
    ('everyday',        NULL, 'Быт', 'Everyday life', 'Еда, жилище, одежда, транспорт'),
    ('movement-action', NULL, 'Движение и действие', 'Movement and action', 'Глаголы движения и деятельности'),
    ('speech-comm',     NULL, 'Речь и общение', 'Speech and communication', 'Речевые акты и общение'),
    ('perception-cog',  NULL, 'Восприятие и познание', 'Perception and cognition', 'Чувства, мысль, знание'),
    ('emotion-char',    NULL, 'Эмоции и характер', 'Emotion and character', 'Эмоции, воля и характер'),
    ('abstract',        NULL, 'Абстрактное', 'Abstract', 'Время, количество, пространство'),
    ('society-ritual',  NULL, 'Общество и ритуал', 'Society and ritual', 'Ритуал, право, философия');

-- === 33 листа (parent_code задан) ===
INSERT INTO "sangraha"."curriculum_semantic_topic"
    ("code", "parent_code", "label_ru", "label_en", "description")
VALUES
    -- nature (5)
    ('animals',          'nature',          'Животные', 'Animals', null),
    ('plants-trees',     'nature',          'Растения и деревья', 'Plants and trees', null),
    ('water',            'nature',          'Вода и реки', 'Waters and rivers', null),
    ('landscape',        'nature',          'Ландшафт и рельеф', 'Landscape and terrain', null),
    ('sky-weather',      'nature',          'Погода и небо', 'Weather and sky', null),
    -- people-body (4)
    ('body-parts',       'people-body',     'Части тела', 'Body parts', null),
    ('family-kin',       'people-body',     'Семья и родня', 'Family and relatives', null),
    ('professions',      'people-body',     'Занятия и профессии', 'Occupations and professions', null),
    ('social-relations', 'people-body',     'Социальные связи', 'Social relations', null),
    -- everyday (5)
    ('food-drink',       'everyday',        'Еда и напитки', 'Food and drink', null),
    ('house-dwelling',   'everyday',        'Жилище', 'House and dwelling', null),
    ('garments',         'everyday',        'Одежда и украшения', 'Garments and ornaments', null),
    ('travel-vehicles',  'everyday',        'Путешествия и транспорт', 'Travel and vehicles', null),
    ('tools-materials',  'everyday',        'Инструменты и материалы', 'Tools and materials', null),
    -- movement-action (3)
    ('motion-verbs',     'movement-action', 'Глаголы движения', 'Motion verbs', null),
    ('physical-action',  'movement-action', 'Физическое действие', 'Physical action', null),
    ('rest-stillness',   'movement-action', 'Покой и неподвижность', 'Rest and stillness', null),
    -- speech-comm (3)
    ('speech-acts',      'speech-comm',     'Речевые акты', 'Speech acts', null),
    ('naming-address',   'speech-comm',     'Именование и обращение', 'Naming and address', null),
    ('question-answer',  'speech-comm',     'Вопрос и ответ', 'Question and answer', null),
    -- perception-cog (3)
    ('senses',           'perception-cog',  'Органы чувств', 'Senses', null),
    ('thought-memory',   'perception-cog',  'Мысль и память', 'Thought and memory', null),
    ('learning',         'perception-cog',  'Знание и учение', 'Knowledge and learning', null),
    -- emotion-char (3)
    ('emotions-positive','emotion-char',    'Положительные эмоции', 'Positive emotions', null),
    ('emotions-negative','emotion-char',    'Отрицательные эмоции', 'Negative emotions', null),
    ('desire-will',      'emotion-char',    'Желание и воля', 'Desire and will', null),
    -- abstract (3)
    ('time-seasons',     'abstract',        'Время: части суток и сезоны', 'Time and seasons', null),
    ('quantity-number',  'abstract',        'Количество и числа', 'Quantity and number', null),
    ('space-direction',  'abstract',        'Пространство и направление', 'Space and direction', null),
    -- society-ritual (3)
    ('ritual-worship',   'society-ritual',  'Ритуал и поклонение', 'Ritual and worship', null),
    ('law-rule',         'society-ritual',  'Право и управление', 'Law and governance', null),
    ('phi-moksha',       'society-ritual',  'Философия и мокша', 'Philosophy and liberation', null),
    ('war-conflict',     'society-ritual',  'Война и конфликт', 'War and conflict', null);

COMMIT;