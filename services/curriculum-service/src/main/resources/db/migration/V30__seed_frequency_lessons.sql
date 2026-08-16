-- V30: five frequency-band vocabulary lessons (lexical-curriculum.md §2).
--
-- Splits the frequency-band cards of the lexicon dashboard into standalone
-- lessons. One curriculum.topic per frequency_band row (code prefix "lex-",
-- like the other lexical topics). Composition with Lexeme is NOT seeded here:
-- lexeme_lexical_topic bindings are (re)filled from lexeme_frequency by rank
-- range on lesson regeneration (LexicalQuizItemGenerator.ensureTopicsExist).

INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order, domain, domain_type)
VALUES
    (gen_random_uuid(), 'lex-frequency-core',          'Ядро (1–100)',                    'Core (1–100)',                    'L0', false, 1, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-frequency-essential',     'Существенный минимум (101–250)',  'Essential (101–250)',             'L1', false, 2, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-frequency-foundational',  'Базовый словарь (251–500)',       'Foundational (251–500)',          'L2', false, 3, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-frequency-intermediate',  'Средний уровень (501–1000)',      'Intermediate (501–1000)',         'L3', false, 4, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-frequency-extended',      'Расширенный словарь (1001–2000)', 'Extended (1001–2000)',            'L4', false, 5, 'LEXICON', 'LEXICON');