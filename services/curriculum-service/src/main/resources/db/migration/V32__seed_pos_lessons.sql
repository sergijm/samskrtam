-- V32: part-of-speech vocabulary lessons.
--
-- One curriculum.topic per part_of_speech code (cf. V5). Composition with
-- Lexeme is (re)filled on lesson regeneration (LexicalQuizItemGenerator
-- .ensureTopicsExist → rebindPosLessons) — all lexemes whose partsOfSpeech
-- includes the given code are bound to the corresponding topic.

INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order, domain, domain_type)
VALUES
    (gen_random_uuid(), 'lex-pos-noun',            'Существительное',  'Noun',          NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-adjective',       'Прилагательное',  'Adjective',     NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-pronoun',         'Местоимение',     'Pronoun',       NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-numeral',         'Числительное',    'Numeral',       NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-finite-verb',     'Личный глагол',   'Finite verb',   NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-participle',      'Причастие',       'Participle',    NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-infinitive',      'Инфинитив',       'Infinitive',    NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-absolutive',      'Абсолютив',       'Absolutive',    NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-gerund',          'Герундий',        'Gerund',        NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-adverb',          'Наречие',         'Adverb',        NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-particle',        'Частица',         'Particle',      NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-conjunction',     'Союз',            'Conjunction',   NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-preverb',         'Преверб',         'Preverb',       NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-interjection',    'Междометие',      'Interjection',  NULL, true, NULL, 'LEXICON', 'LEXICON'),
    (gen_random_uuid(), 'lex-pos-preposition',     'Предлог',         'Preposition',   NULL, true, NULL, 'LEXICON', 'LEXICON');