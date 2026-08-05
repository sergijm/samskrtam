-- Seed of lexicon classifiers: frequency bands (§2), part-of-speech (§4),
-- morphology classes (§5) and the semantic taxonomy tree (9 roots + 40 leaves, §3)
-- from docs/docs/services/lexical-curriculum.md. No Lexeme rows here — word
-- content is seeded separately (lexicon-content-pipeline.md).

-- ----------------------------------------------------------------------------
-- Frequency bands (5 bands, lexical-curriculum.md §2)
-- ----------------------------------------------------------------------------

INSERT INTO curriculum.frequency_band (code, min_rank, max_rank, label_ru, label_en, sort_order) VALUES
    ('CORE', 1, 100, 'Ядро', 'Core', 1),
    ('ESSENTIAL', 101, 250, 'Существенный минимум', 'Essential', 2),
    ('FOUNDATIONAL', 251, 500, 'Базовый словарь', 'Foundational', 3),
    ('INTERMEDIATE', 501, 1000, 'Средний уровень', 'Intermediate', 4),
    ('EXTENDED', 1001, 2000, 'Расширенный словарь', 'Extended', 5);

-- ----------------------------------------------------------------------------
-- Part of speech (15 codes, lexical-curriculum.md §4)
-- ----------------------------------------------------------------------------

INSERT INTO curriculum.part_of_speech (code, "group", name_ru, name_en) VALUES
    ('noun', 'NOMINAL', 'существительное', 'noun'),
    ('adjective', 'NOMINAL', 'прилагательное', 'adjective'),
    ('pronoun', 'NOMINAL', 'местоимение', 'pronoun'),
    ('numeral', 'NOMINAL', 'числительное', 'numeral'),
    ('finite-verb', 'VERBAL', 'личный глагол', 'finite verb'),
    ('participle', 'VERBAL', 'причастие', 'participle'),
    ('infinitive', 'VERBAL', 'инфинитив', 'infinitive'),
    ('absolutive', 'VERBAL', 'абсолютив', 'absolutive'),
    ('gerund', 'VERBAL', 'герундий', 'gerund'),
    ('adverb', 'INDECLINABLE', 'наречие', 'adverb'),
    ('particle', 'INDECLINABLE', 'частица', 'particle'),
    ('conjunction', 'INDECLINABLE', 'союз', 'conjunction'),
    ('preverb', 'INDECLINABLE', 'преверб', 'preverb'),
    ('interjection', 'INDECLINABLE', 'междометие', 'interjection'),
    ('preposition', 'INDECLINABLE', 'предлог/послелог', 'preposition');

-- ----------------------------------------------------------------------------
-- Morphology classes (19 codes, lexical-curriculum.md §5)
-- ----------------------------------------------------------------------------

INSERT INTO curriculum.morphology_class (code, applies_to, name_ru, name_en) VALUES
    ('a-stem-masc', 'NOUN', 'a-основа, муж.р.', 'a-stem masculine'),
    ('a-stem-neut', 'NOUN', 'a-основа, ср.р.', 'a-stem neuter'),
    ('a-stem-fem', 'NOUN', 'ā-основа, жен.р.', 'ā-stem feminine'),
    ('i-stem', 'NOUN', 'i-основа', 'i-stem'),
    ('u-stem', 'NOUN', 'u-основа', 'u-stem'),
    ('r-stem', 'NOUN', 'ṛ-основа', 'ṛ-stem'),
    ('consonant-stem', 'NOUN', 'согласная основа', 'consonant stem'),
    ('irregular-noun', 'NOUN', 'неправильное склонение', 'irregular noun'),
    ('class-1', 'VERB', 'класс 1 (bhū)', 'class 1 (bhū)'),
    ('class-2', 'VERB', 'класс 2 (ad)', 'class 2 (ad)'),
    ('class-3', 'VERB', 'класс 3 (hu)', 'class 3 (hu)'),
    ('class-4', 'VERB', 'класс 4 (div)', 'class 4 (div)'),
    ('class-5', 'VERB', 'класс 5 (su)', 'class 5 (su)'),
    ('class-6', 'VERB', 'класс 6 (tud)', 'class 6 (tud)'),
    ('class-7', 'VERB', 'класс 7 (rudh)', 'class 7 (rudh)'),
    ('class-8', 'VERB', 'класс 8 (tan)', 'class 8 (tan)'),
    ('class-9', 'VERB', 'класс 9 (krī)', 'class 9 (krī)'),
    ('class-10', 'VERB', 'класс 10 (cur)', 'class 10 (cur)'),
    ('irregular-verb', 'VERB', 'неправильный глагол', 'irregular verb');

-- ----------------------------------------------------------------------------
-- Semantic taxonomy: 9 roots + 40 leaves (lexical-curriculum.md §3)
-- ----------------------------------------------------------------------------

INSERT INTO curriculum.semantic_topic (id, code, name_ru, name_en, parent_id)
SELECT gen_random_uuid(), v.code, v.name_ru, v.name_en, p.id
FROM (VALUES
    -- roots (parent_id NULL)
    ('nature',            NULL,                'Природа',               'Nature'),
    ('people-body',       NULL,                'Люди и тело',           'People & Body'),
    ('everyday-life',     NULL,                'Повседневная жизнь',    'Everyday life'),
    ('movement-action',   NULL,                'Движение и действие',   'Movement & Action'),
    ('speech-communication', NULL,             'Речь и общение',        'Speech & Communication'),
    ('perception-cognition', NULL,             'Восприятие и познание', 'Perception & Cognition'),
    ('emotion-character', NULL,                'Эмоции и характер',     'Emotion & Character'),
    ('abstract',          NULL,                'Абстрактное',           'Abstract'),
    ('society-ritual',    NULL,                'Общество и ритуал',     'Society & Ritual'),
    -- Nature
    ('nature-animals',        'nature',            'Животные',              'Animals'),
    ('nature-plants',         'nature',            'Растения',              'Plants'),
    ('nature-landscape',      'nature',            'Ландшафт',              'Landscape'),
    ('nature-water',          'nature',            'Вода',                  'Water'),
    ('nature-weather-sky',    'nature',            'Погода и небо',         'Weather & Sky'),
    ('nature-agriculture',    'nature',            'Земледелие',            'Agriculture'),
    -- People & Body
    ('people-family',             'people-body',      'Семья',                   'Family'),
    ('people-body-parts',         'people-body',      'Части тела',              'Body parts'),
    ('people-occupations',        'people-body',      'Занятия',                 'Occupations'),
    ('people-social-relations',   'people-body',      'Социальные связи',        'Social relations'),
    ('people-character',          'people-body',      'Характер и личность',     'Character & Personality'),
    ('people-royalty-hierarchy',  'people-body',      'Власть и социальная иерархия', 'Royalty & Social hierarchy'),
    -- Everyday life
    ('everyday-food-drink',           'everyday-life', 'Еда и напитки',          'Food & Drink'),
    ('everyday-house-dwelling',       'everyday-life', 'Дом и жилище',           'House & Dwelling'),
    ('everyday-clothing-ornament',    'everyday-life', 'Одежда и украшения',     'Clothing & Ornament'),
    ('everyday-travel-vehicles',      'everyday-life', 'Путешествия и транспорт','Travel & Vehicles'),
    ('everyday-objects-tools',        'everyday-life', 'Предметы и инструменты', 'Objects & Tools'),
    ('everyday-materials',            'everyday-life', 'Материалы',              'Materials'),
    -- Movement & Action
    ('movement-motion-verbs',    'movement-action', 'Глаголы движения',   'Motion verbs'),
    ('movement-physical-action', 'movement-action', 'Физическое действие','Physical action'),
    ('movement-rest-stillness',  'movement-action', 'Покой и неподвижность','Rest & Stillness'),
    -- Speech & Communication
    ('speech-acts',           'speech-communication', 'Речевые акты',           'Speech acts'),
    ('speech-naming-address', 'speech-communication', 'Именование и обращение', 'Naming & Address'),
    ('speech-question-answer','speech-communication', 'Вопрос и ответ',         'Question & Answer'),
    -- Perception & Cognition
    ('cognition-senses',           'perception-cognition', 'Чувства',          'Senses'),
    ('cognition-thought-memory',   'perception-cognition', 'Мысль и память',   'Thought & Memory'),
    ('cognition-knowledge-learning','perception-cognition','Знание и учение',  'Knowledge & Learning'),
    -- Emotion & Character
    ('emotion-positive',   'emotion-character', 'Позитивные эмоции',   'Positive emotions'),
    ('emotion-negative',   'emotion-character', 'Негативные эмоции',   'Negative emotions'),
    ('emotion-desire-will','emotion-character', 'Желание и воля',      'Desire & Will'),
    -- Abstract
    ('abstract-time',            'abstract', 'Время',            'Time'),
    ('abstract-quantity-number', 'abstract', 'Количество и число','Quantity & Number'),
    ('abstract-space-direction', 'abstract', 'Пространство и направление','Space & Direction'),
    ('abstract-cause-purpose',   'abstract', 'Причина и цель',    'Cause & Purpose'),
    ('abstract-comparison',      'abstract', 'Сравнение',         'Comparison'),
    -- Society & Ritual
    ('society-ritual-worship',      'society-ritual', 'Ритуал и поклонение',      'Ritual & Worship'),
    ('society-ethics-duty',         'society-ritual', 'Этика и долг (дхарма)',    'Ethics & Duty (dharma)'),
    ('society-governance-law',      'society-ritual', 'Управление и закон',       'Governance & Law'),
    ('society-war-conflict',        'society-ritual', 'Война и конфликт',         'War & Conflict'),
    ('society-philosophy-liberation','society-ritual','Философия и освобождение', 'Philosophy & Liberation')
) AS v(code, parent_code, name_ru, name_en)
LEFT JOIN curriculum.semantic_topic p ON p.code = v.parent_code;
