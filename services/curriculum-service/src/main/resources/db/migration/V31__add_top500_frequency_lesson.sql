-- V31: cumulative "500 most frequent words" lesson.
--
-- The lexicon dashboard frequency section opens with a compact overview card
-- ("500 самых частотных слов", the top-3 bands combined, ranks 1–500). This
-- mirrors that card as a standalone evergreen lesson. Composition via
-- lexeme_lexical_topic is (re)filled on lesson regeneration
-- (LexicalQuizItemGenerator.ensureTopicsExist), like the V30 band lessons.

INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order, domain, domain_type)
VALUES (gen_random_uuid(), 'lex-frequency-top500', '500 самых частотных слов', '500 most frequent words', NULL, true, NULL, 'LEXICON', 'LEXICON');