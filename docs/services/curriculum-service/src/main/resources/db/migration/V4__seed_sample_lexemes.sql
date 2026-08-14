-- Illustrative proof-of-concept seed (25 lexemes), NOT the full 2000-word batch.
-- See lexicon-content-pipeline.md §3. All rows hand-checked (common
-- function words / core vocabulary, safe to ship without an AI-enrichment pass).

INSERT INTO curriculum.frequency_band (code, min_rank, max_rank, label_ru, label_en, sort_order) VALUES
    ('CORE', 1, 100, 'Ядро', 'Core', 1),
    ('ESSENTIAL', 101, 250, 'Существенный минимум', 'Essential', 2),
    ('FOUNDATIONAL', 251, 500, 'Базовый словарь', 'Foundational', 3),
    ('INTERMEDIATE', 501, 1000, 'Средний уровень', 'Intermediate', 4),
    ('EXTENDED', 1001, 2000, 'Расширенный словарь', 'Extended', 5);

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
    ('preverb', 'INDECLINABLE', 'превербь', 'preverb'),
    ('interjection', 'INDECLINABLE', 'междометие', 'interjection'),
    ('preposition', 'INDECLINABLE', 'предлог/послелог', 'preposition/indeclinable');

INSERT INTO curriculum.morphology_class (code, applies_to, name_ru, name_en) VALUES
    ('a-stem-masc', 'NOUN', 'a-основа, муж.р.', 'a-stem masculine'),
    ('a-stem-neut', 'NOUN', 'a-основа, ср.р.', 'a-stem neuter'),
    ('a-stem-fem', 'NOUN', 'ā-основа, жен.р.', 'ā-stem feminine'),
    ('i-stem', 'NOUN', 'i-основа', 'i-stem'),
    ('u-stem', 'NOUN', 'u-основа', 'u-stem'),
    ('r-stem', 'NOUN', 'ṛ-основа', 'ṛ-stem'),
    ('consonant-stem', 'NOUN', 'согласная основа', 'consonant stem'),
    ('irregular-noun', 'NOUN', 'неправильное склонение', 'irregular'),
    ('class-1', 'VERB', 'класс 1 (bhū)', 'class 1 (bhū)'),
    ('class-2', 'VERB', 'класс 2 (ad)', 'class 2 (ad)'),
    ('class-4', 'VERB', 'класс 4 (div)', 'class 4 (div)'),
    ('class-6', 'VERB', 'класс 6 (tud)', 'class 6 (tud)'),
    ('class-10', 'VERB', 'класс 10 (cur)', 'class 10 (cur)'),
    ('irregular-verb', 'VERB', 'неправильный глагол', 'irregular verb');

INSERT INTO curriculum.semantic_topic (id, code, name_ru, name_en, parent_id) VALUES
    (gen_random_uuid(), 'people', 'Люди', 'People & Body', NULL),
    (gen_random_uuid(), 'abstract', 'Абстрактное', 'Abstract', NULL);

-- 25 illustrative core lexemes (function words, basic pronouns/numerals/verbs)
INSERT INTO curriculum.lexeme (id, lemma_iast, lemma_devanagari, lemma_slp1, gloss_ru, gloss_en, gender) VALUES
    (gen_random_uuid(), 'ca', 'च', 'ca', 'и', 'and', NULL),
    (gen_random_uuid(), 'na', 'न', 'na', 'не', 'not', NULL),
    (gen_random_uuid(), 'tu', 'तु', 'tu', 'но, же', 'but, indeed', NULL),
    (gen_random_uuid(), 'iti', 'इति', 'iti', 'так (конец цитаты)', 'thus (quotation marker)', NULL),
    (gen_random_uuid(), 'eva', 'एव', 'eva', 'именно, только', 'indeed, only', NULL),
    (gen_random_uuid(), 'api', 'अपि', 'api', 'также, даже', 'also, even', NULL),
    (gen_random_uuid(), 'yathā', 'यथा', 'yathA', 'как', 'as, just as', NULL),
    (gen_random_uuid(), 'tathā', 'तथा', 'tathA', 'так', 'so, thus', NULL),
    (gen_random_uuid(), 'yadi', 'यदि', 'yadi', 'если', 'if', NULL),
    (gen_random_uuid(), 'sa', 'स', 'sa', 'он, тот', 'he, that (demonstrative pronoun)', 'MASCULINE'),
    (gen_random_uuid(), 'tad', 'तद्', 'tad', 'то', 'that (demonstrative pronoun)', 'NEUTER'),
    (gen_random_uuid(), 'aham', 'अहम्', 'aham', 'я', 'I', 'UNSPECIFIED'),
    (gen_random_uuid(), 'tvam', 'त्वम्', 'tvam', 'ты', 'you (singular)', 'UNSPECIFIED'),
    (gen_random_uuid(), 'ka', 'क', 'ka', 'кто, какой', 'who, which (interrogative)', 'MASCULINE'),
    (gen_random_uuid(), 'sarva', 'सर्व', 'sarva', 'весь, всякий', 'all, every', 'MASCULINE'),
    (gen_random_uuid(), 'eka', 'एक', 'eka', 'один', 'one', 'MASCULINE'),
    (gen_random_uuid(), 'dvi', 'द्वि', 'dvi', 'два', 'two', 'UNSPECIFIED'),
    (gen_random_uuid(), 'tri', 'त्रि', 'tri', 'три', 'three', 'UNSPECIFIED'),
    (gen_random_uuid(), 'bhū', 'भू', 'BU', 'быть, становиться', 'to be, to become', NULL),
    (gen_random_uuid(), 'as', 'अस्', 'as', 'быть', 'to be', NULL),
    (gen_random_uuid(), 'gam', 'गम्', 'gam', 'идти', 'to go', NULL),
    (gen_random_uuid(), 'kṛ', 'कृ', 'kf', 'делать', 'to do, to make', NULL),
    (gen_random_uuid(), 'vad', 'वद्', 'vad', 'говорить', 'to speak', NULL),
    (gen_random_uuid(), 'nara', 'नर', 'nara', 'человек, мужчина', 'man, person', 'MASCULINE'),
    (gen_random_uuid(), 'gaja', 'गज', 'gaja', 'слон', 'elephant', 'MASCULINE');

-- Frequency ranks (1-25, illustrative order only)
INSERT INTO curriculum.lexeme_frequency (lexeme_id, source, rank)
SELECT id, 'CURATED_2000', ROW_NUMBER() OVER (ORDER BY lemma_iast)
FROM curriculum.lexeme;

-- POS tagging for the sample (illustrative subset)
INSERT INTO curriculum.lexeme_pos (lexeme_id, pos_code)
SELECT id, 'particle' FROM curriculum.lexeme WHERE lemma_iast IN ('ca','na','tu','iti','eva','api','yathā','tathā','yadi')
UNION ALL
SELECT id, 'pronoun' FROM curriculum.lexeme WHERE lemma_iast IN ('sa','tad','aham','tvam','ka')
UNION ALL
SELECT id, 'numeral' FROM curriculum.lexeme WHERE lemma_iast IN ('eka','dvi','tri')
UNION ALL
SELECT id, 'adjective' FROM curriculum.lexeme WHERE lemma_iast = 'sarva'
UNION ALL
SELECT id, 'finite-verb' FROM curriculum.lexeme WHERE lemma_iast IN ('bhū','as','gam','kṛ','vad')
UNION ALL
SELECT id, 'noun' FROM curriculum.lexeme WHERE lemma_iast IN ('nara','gaja');

-- Morphology for the two noun samples + two verb-class samples
INSERT INTO curriculum.lexeme_morphology (lexeme_id, morphology_class_code)
SELECT id, 'a-stem-masc' FROM curriculum.lexeme WHERE lemma_iast IN ('nara','gaja')
UNION ALL
SELECT id, 'class-1' FROM curriculum.lexeme WHERE lemma_iast = 'bhū'
UNION ALL
SELECT id, 'class-1' FROM curriculum.lexeme WHERE lemma_iast = 'gam';

-- Semantic tagging for the two noun samples (People root)
INSERT INTO curriculum.lexeme_semantic_topic (lexeme_id, semantic_topic_id)
SELECT l.id, st.id FROM curriculum.lexeme l, curriculum.semantic_topic st
WHERE l.lemma_iast = 'nara' AND st.code = 'people';
