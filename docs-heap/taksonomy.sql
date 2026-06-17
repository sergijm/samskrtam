CREATE TABLE content.vocabulary_categories (
                                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                               code VARCHAR(100) NOT NULL UNIQUE,
                                               parent_code VARCHAR(100),
                                               name_ru VARCHAR(255) NOT NULL,
                                               name_en VARCHAR(255) NOT NULL,
                                               description_ru TEXT,
                                               description_en TEXT
);

INSERT INTO content.vocabulary_categories (
    code,
    parent_code,
    name_ru,
    name_en,
    description_ru,
    description_en
)
VALUES

-- ROOT

('NATURE',NULL,'Природа','Nature',
 'Природный мир',
 'Natural world'),

('HUMAN',NULL,'Человек','Human',
 'Человек и общество',
 'Human and society'),

('HOME',NULL,'Дом и быт','Home',
 'Бытовая лексика',
 'Domestic vocabulary'),

('FOOD',NULL,'Еда','Food',
 'Пища и напитки',
 'Food and beverages'),

('TIME',NULL,'Время','Time',
 'Время и календарь',
 'Time and calendar'),

('SPACE',NULL,'Пространство','Space',
 'Пространственные понятия',
 'Spatial concepts'),

('SOCIETY',NULL,'Общество','Society',
 'Социальная лексика',
 'Social vocabulary'),

('ECONOMY',NULL,'Экономика','Economy',
 'Торговля и деньги',
 'Trade and money'),

('EDUCATION',NULL,'Образование','Education',
 'Обучение и знания',
 'Learning and knowledge'),

('WAR',NULL,'Война','War',
 'Военное дело',
 'Military affairs'),

('RELIGION',NULL,'Религия','Religion',
 'Религиозная лексика',
 'Religious vocabulary'),

('PHILOSOPHY',NULL,'Философия','Philosophy',
 'Философские понятия',
 'Philosophical concepts'),

('GRAMMAR',NULL,'Грамматика','Grammar',
 'Грамматическая терминология',
 'Grammar terminology'),

('ABSTRACT',NULL,'Абстрактные понятия','Abstract concepts',
 'Абстрактная лексика',
 'Abstract vocabulary'),

-- NATURE

('ANIMALS','NATURE','Животные','Animals',
 'Млекопитающие',
 'Mammals'),

('BIRDS','NATURE','Птицы','Birds',
 'Птицы',
 'Birds'),

('INSECTS','NATURE','Насекомые','Insects',
 'Насекомые',
 'Insects'),

('REPTILES','NATURE','Пресмыкающиеся','Reptiles',
 'Пресмыкающиеся',
 'Reptiles'),

('FISH','NATURE','Рыбы','Fish',
 'Рыбы',
 'Fish'),

('TREES','NATURE','Деревья','Trees',
 'Деревья',
 'Trees'),

('FLOWERS','NATURE','Цветы','Flowers',
 'Цветы',
 'Flowers'),

('PLANTS','NATURE','Растения','Plants',
 'Растения',
 'Plants'),

('WEATHER','NATURE','Погода','Weather',
 'Погодные явления',
 'Weather'),

('GEOGRAPHY','NATURE','География','Geography',
 'Географические объекты',
 'Geography'),

('CELESTIAL','NATURE','Небесные тела','Celestial',
 'Астрономические объекты',
 'Celestial objects'),

('ELEMENTS','NATURE','Стихии','Elements',
 'Пять элементов',
 'Elements'),

-- HUMAN

('FAMILY','HUMAN','Семья','Family',
 'Родственные отношения',
 'Family relations'),

('BODY','HUMAN','Части тела','Body',
 'Тело человека',
 'Human body'),

('HEALTH','HUMAN','Здоровье','Health',
 'Медицина',
 'Health'),

('EMOTIONS','HUMAN','Эмоции','Emotions',
 'Чувства и эмоции',
 'Feelings and emotions'),

('SENSES','HUMAN','Чувства восприятия','Senses',
 'Органы чувств',
 'Senses'),

-- HOME

('HOUSE','HOME','Дом','House',
 'Части дома',
 'House'),

('FURNITURE','HOME','Мебель','Furniture',
 'Предметы мебели',
 'Furniture'),

('CLOTHING','HOME','Одежда','Clothing',
 'Одежда и украшения',
 'Clothing'),

('TOOLS','HOME','Инструменты','Tools',
 'Инструменты',
 'Tools'),

-- FOOD

('FRUITS','FOOD','Фрукты','Fruits',
 'Фрукты',
 'Fruits'),

('GRAINS','FOOD','Зерновые','Grains',
 'Злаки',
 'Grains'),

('SPICES','FOOD','Специи','Spices',
 'Специи',
 'Spices'),

('DRINKS','FOOD','Напитки','Drinks',
 'Напитки',
 'Drinks'),

-- TIME

('CALENDAR','TIME','Календарь','Calendar',
 'Календарные понятия',
 'Calendar'),

('SEASONS','TIME','Времена года','Seasons',
 'Сезоны',
 'Seasons'),

-- SOCIETY

('PROFESSIONS','SOCIETY','Профессии','Professions',
 'Профессии',
 'Professions'),

('GOVERNMENT','SOCIETY','Государство','Government',
 'Политика и власть',
 'Politics and government'),

('LAW','SOCIETY','Право','Law',
 'Юридические термины',
 'Legal concepts'),

-- RELIGION

('DEITIES','RELIGION','Божества','Deities',
 'Боги и богини',
 'Deities'),

('RITUALS','RELIGION','Ритуалы','Rituals',
 'Обряды',
 'Rituals'),

('SCRIPTURES','RELIGION','Священные тексты','Scriptures',
 'Священные книги',
 'Scriptures'),

('MANTRAS','RELIGION','Мантры','Mantras',
 'Мантры и молитвы',
 'Mantras'),

-- PHILOSOPHY

('VEDANTA','PHILOSOPHY','Веданта','Vedanta',
 'Термины веданты',
 'Vedanta concepts'),

('SAMKHYA','PHILOSOPHY','Санкхья','Samkhya',
 'Термины санкхьи',
 'Samkhya concepts'),

('YOGA','PHILOSOPHY','Йога','Yoga',
 'Термины йоги',
 'Yoga concepts'),

('BUDDHISM','PHILOSOPHY','Буддизм','Buddhism',
 'Буддийские термины',
 'Buddhist concepts'),

-- ABSTRACT

('VIRTUES','ABSTRACT','Добродетели','Virtues',
 'Положительные качества',
 'Virtues'),

('KNOWLEDGE','ABSTRACT','Знание','Knowledge',
 'Познание',
 'Knowledge'),

('VALUES','ABSTRACT','Ценности','Values',
 'Абстрактные ценности',
 'Values');