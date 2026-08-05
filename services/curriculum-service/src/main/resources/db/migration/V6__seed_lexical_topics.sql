-- Seed of 68 LEXICAL topics (learning_level L0-L6) + 1 evergreen review topic.
-- Source list: docs/docs/services/lexical-curriculum.md §6. Rows are ordinary
-- curriculum.topic with domain=LEXICON; composition with Lexeme lives in
-- curriculum.lexical_topic_binding (not seeded here). No prerequisite edges for
-- lexical topics (by design, pending curriculum review).
-- code prefix "lex-" distinguishes lexical topics from grammar ones (same UNIQUE
-- constraint on curriculum.topic.code).

INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order, domain) VALUES
    -- L0 — фундамент (9)
    (gen_random_uuid(), 'lex-basic-function-words',      'Базовые служебные слова',      'Basic function words',              'L0', false, 1, 'LEXICON'),
    (gen_random_uuid(), 'lex-numbers-1-10',              'Числительные 1–10',            'Numbers 1–10',                    'L0', false, 2, 'LEXICON'),
    (gen_random_uuid(), 'lex-personal-pronouns',         'Личные местоимения: лексика',  'Personal pronoun vocabulary',     'L0', false, 3, 'LEXICON'),
    (gen_random_uuid(), 'lex-family-core',                'Семья: базовый словарь',      'Family core',                     'L0', false, 4, 'LEXICON'),
    (gen_random_uuid(), 'lex-body-core',                  'Тело: базовый словарь',        'Body core',                       'L0', false, 5, 'LEXICON'),
    (gen_random_uuid(), 'lex-common-nouns-objects',       'Существительные (предметы)',   'Common nouns (objects)',         'L0', false, 6, 'LEXICON'),
    (gen_random_uuid(), 'lex-common-verbs-actions',       'Глаголы (базовые действия)',   'Common verbs (basic actions)',   'L0', false, 7, 'LEXICON'),
    (gen_random_uuid(), 'lex-greetings-address',          'Приветствия и обращения',      'Greetings & address',            'L0', false, 8, 'LEXICON'),
    (gen_random_uuid(), 'lex-questions-affirmation',      'Да/нет и базовые вопросы',     'Yes/no & basic questions',       'L0', false, 9, 'LEXICON'),

    -- L1 — база (10)
    (gen_random_uuid(), 'lex-animals',                    'Животные',                     'Animals',                        'L1', false, 1, 'LEXICON'),
    (gen_random_uuid(), 'lex-plants-trees',               'Растения и деревья',           'Plants & trees',                 'L1', false, 2, 'LEXICON'),
    (gen_random_uuid(), 'lex-food-drink',                 'Еда и напитки',                'Food & drink',                   'L1', false, 3, 'LEXICON'),
    (gen_random_uuid(), 'lex-house-dwelling',             'Дом и жилище',                 'House & dwelling',               'L1', false, 4, 'LEXICON'),
    (gen_random_uuid(), 'lex-clothing-ornament',          'Одежда и украшения',           'Clothing & ornament',            'L1', false, 5, 'LEXICON'),
    (gen_random_uuid(), 'lex-colors',                     'Цвета',                        'Colors',                         'L1', false, 6, 'LEXICON'),
    (gen_random_uuid(), 'lex-adjectives-quality',         'Прилагательные (качество)',    'Common adjectives (quality)',    'L1', false, 7, 'LEXICON'),
    (gen_random_uuid(), 'lex-adjectives-size',            'Прилагательные (размер/количество)', 'Common adjectives (size/quantity)', 'L1', false, 8, 'LEXICON'),
    (gen_random_uuid(), 'lex-landscape-terrain',          'Ландшафт и рельеф',            'Landscape & terrain',            'L1', false, 9, 'LEXICON'),
    (gen_random_uuid(), 'lex-water-rivers',               'Вода и реки',                  'Water & rivers',                 'L1', false, 10, 'LEXICON'),

    -- L2 — расширение бытовой лексики (10)
    (gen_random_uuid(), 'lex-weather-sky',                'Погода и небо',                'Weather & sky',                  'L2', false, 1, 'LEXICON'),
    (gen_random_uuid(), 'lex-time-seasons',               'Время: части дня и времена года','Time — parts of day & seasons', 'L2', false, 2, 'LEXICON'),
    (gen_random_uuid(), 'lex-occupations-professions',    'Занятия и профессии',          'Occupations & professions',      'L2', false, 3, 'LEXICON'),
    (gen_random_uuid(), 'lex-travel-vehicles',            'Путешествия и транспорт',      'Travel & vehicles',              'L2', false, 4, 'LEXICON'),
    (gen_random_uuid(), 'lex-tools-objects',              'Инструменты и предметы',       'Tools & objects',                'L2', false, 5, 'LEXICON'),
    (gen_random_uuid(), 'lex-motion-verbs-basic',         'Глаголы движения (базовые)',   'Motion verbs (basic)',           'L2', false, 6, 'LEXICON'),
    (gen_random_uuid(), 'lex-speech-verbs',               'Глаголы речи',                 'Speech verbs',                   'L2', false, 7, 'LEXICON'),
    (gen_random_uuid(), 'lex-perception-verbs',           'Глаголы восприятия',           'Perception verbs (see/hear/know)','L2', false, 8, 'LEXICON'),
    (gen_random_uuid(), 'lex-materials-substances',       'Материалы и вещества',         'Materials & substances',         'L2', false, 9, 'LEXICON'),
    (gen_random_uuid(), 'lex-agriculture-village',        'Земледелие и деревенская жизнь','Agriculture & village life',    'L2', false, 10, 'LEXICON'),

    -- L3 — абстракция и социум (10)
    (gen_random_uuid(), 'lex-emotions-positive',          'Эмоции: позитивные',           'Emotions — positive',            'L3', false, 1, 'LEXICON'),
    (gen_random_uuid(), 'lex-emotions-negative',          'Эмоции: негативные',           'Emotions — negative',            'L3', false, 2, 'LEXICON'),
    (gen_random_uuid(), 'lex-desire-will',                'Желание и воля',               'Desire & will',                  'L3', false, 3, 'LEXICON'),
    (gen_random_uuid(), 'lex-character-personality',      'Характер и черты личности',    'Character & personality traits', 'L3', false, 4, 'LEXICON'),
    (gen_random_uuid(), 'lex-social-relations',           'Социальные связи и родство',   'Social relations & kinship',     'L3', false, 5, 'LEXICON'),
    (gen_random_uuid(), 'lex-royalty-hierarchy',          'Власть и социальная иерархия', 'Royalty & social hierarchy',     'L3', false, 6, 'LEXICON'),
    (gen_random_uuid(), 'lex-quantity-measurement',       'Количество и измерение',       'Quantity & measurement',         'L3', false, 7, 'LEXICON'),
    (gen_random_uuid(), 'lex-space-direction',            'Пространство и направление',   'Space & direction',              'L3', false, 8, 'LEXICON'),
    (gen_random_uuid(), 'lex-comparison-degree',          'Сравнение и степень',          'Comparison & degree',            'L3', false, 9, 'LEXICON'),
    (gen_random_uuid(), 'lex-cause-purpose',              'Причина и цель',               'Cause & purpose vocabulary',     'L3', false, 10, 'LEXICON'),

    -- L4 — религия, этика, познание (9)
    (gen_random_uuid(), 'lex-ritual-worship',             'Ритуал и поклонение',          'Ritual & worship',               'L4', false, 1, 'LEXICON'),
    (gen_random_uuid(), 'lex-ethics-duty-dharma',         'Этика и долг (дхарма)',        'Ethics & duty (dharma vocabulary)','L4', false, 2, 'LEXICON'),
    (gen_random_uuid(), 'lex-knowledge-learning',         'Знание и учение',              'Knowledge & learning',           'L4', false, 3, 'LEXICON'),
    (gen_random_uuid(), 'lex-thought-memory',             'Мысль и память',               'Thought & memory',               'L4', false, 4, 'LEXICON'),
    (gen_random_uuid(), 'lex-philosophy-liberation',      'Философия и освобождение (мокша)', 'Philosophy & liberation (mokṣa vocabulary)', 'L4', false, 5, 'LEXICON'),
    (gen_random_uuid(), 'lex-governance-law',             'Управление и закон',           'Governance & law',               'L4', false, 6, 'LEXICON'),
    (gen_random_uuid(), 'lex-naming-address',             'Именование и формы обращения', 'Naming & address forms',         'L4', false, 7, 'LEXICON'),
    (gen_random_uuid(), 'lex-question-answer',            'Вопрос и ответ',               'Question & answer vocabulary',    'L4', false, 8, 'LEXICON'),
    (gen_random_uuid(), 'lex-rest-stillness-verbs',       'Глаголы покоя',                'Rest & stillness verbs',         'L4', false, 9, 'LEXICON'),

    -- L5 — расширенный словарь (12)
    (gen_random_uuid(), 'lex-war-conflict',               'Война и конфликт',             'War & conflict',                 'L5', false, 1, 'LEXICON'),
    (gen_random_uuid(), 'lex-music-arts',                 'Музыка и искусства',           'Music & arts',                   'L5', false, 2, 'LEXICON'),
    (gen_random_uuid(), 'lex-motion-verbs-advanced',      'Глаголы движения: сложные',    'Advanced motion verbs (compound)','L5', false, 3, 'LEXICON'),
    (gen_random_uuid(), 'lex-adjectives-abstract',        'Прилагательные: абстрактное',  'Advanced adjectives (abstract)','L5', false, 4, 'LEXICON'),
    (gen_random_uuid(), 'lex-nature-extended',            'Природа: расширенный словарь', 'Nature — extended (celestial)','L5', false, 5, 'LEXICON'),
    (gen_random_uuid(), 'lex-mythology-epithets',         'Мифология и эпитеты',          'Mythology & epithets',           'L5', false, 6, 'LEXICON'),
    (gen_random_uuid(), 'lex-particles-conjunctions',     'Частицы и союзы',              'Advanced particles & conjunctions','L5', false, 7, 'LEXICON'),
    (gen_random_uuid(), 'lex-verbs-giving-taking',        'Глаголы давать/брать/обмен',   'Verbs of giving/taking/exchange','L5', false, 8, 'LEXICON'),
    (gen_random_uuid(), 'lex-verbs-creation-destruction', 'Глаголы созидания и разрушения','Verbs of creation & destruction','L5', false, 9, 'LEXICON'),
    (gen_random_uuid(), 'lex-body-extended',              'Тело: расширенный словарь',    'Body — extended (internal organs)','L5', false, 10, 'LEXICON'),
    (gen_random_uuid(), 'lex-comparative-superlative',    'Сравнительная и превосходная', 'Comparative & superlative forms','L5', false, 11, 'LEXICON'),
    (gen_random_uuid(), 'lex-idiomatic-collocations',     'Идиоматические глагольно-именные сочетания', 'Idiomatic verb-noun collocations','L5', false, 12, 'LEXICON'),

    -- L6 — по частям речи / морфологии, интеграционные (7)
    (gen_random_uuid(), 'lex-a-stem-nouns',               'a-основы: лексический срез',   'a-stem nouns (vocabulary cross-section)', 'L6', false, 1, 'LEXICON'),
    (gen_random_uuid(), 'lex-a-stem-feminine-nouns',     'ā-основы: лексический срез',   'ā-stem nouns (vocabulary cross-section)', 'L6', false, 2, 'LEXICON'),
    (gen_random_uuid(), 'lex-verb-class-1',              'Глаголы класса 1: лексика',     'Verb class 1 vocabulary',        'L6', false, 3, 'LEXICON'),
    (gen_random_uuid(), 'lex-verb-class-6',              'Глаголы класса 6: лексика',     'Verb class 6 vocabulary',        'L6', false, 4, 'LEXICON'),
    (gen_random_uuid(), 'lex-participle-vocabulary',     'Причастия: лексика',            'Participle vocabulary',          'L6', false, 5, 'LEXICON'),
    (gen_random_uuid(), 'lex-absolutive-infinitive',     'Абсолютивы и инфинитивы',       'Absolutive & infinitive vocabulary','L6', false, 6, 'LEXICON'),
    (gen_random_uuid(), 'lex-numbers-1-100',             'Числительные 1–100',            'Numerals 1–100',                 'L6', false, 7, 'LEXICON'),

    -- Evergreen — доступен всегда, вне уровней и графа (display_order NULL)
    (gen_random_uuid(), 'lex-core-vocabulary-review',     'Основной словарь: повторение', 'Core vocabulary review',      null, true, NULL, 'LEXICON');