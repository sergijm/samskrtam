-- V12: seed стандартных VocabularyQuizDefinition (Core Vocabulary 1–5)
-- для FREQUENCY_BAND, см. lexical-quizzes.md §2

INSERT INTO curriculum.vocabulary_quiz_definition (id, kind, title_ru, title_en, frequency_rank_max, created_at, updated_at)
SELECT gen_random_uuid(), 'FREQUENCY_BAND', v.title_ru, v.title_en, v.rank_max, now(), now()
FROM (VALUES
    ('Core Vocabulary 1', 'Core Vocabulary 1', 100),
    ('Core Vocabulary 2', 'Core Vocabulary 2', 250),
    ('Core Vocabulary 3', 'Core Vocabulary 3', 500),
    ('Core Vocabulary 4', 'Core Vocabulary 4', 1000),
    ('Core Vocabulary 5', 'Core Vocabulary 5', 2000)
) AS v(title_ru, title_en, rank_max)
WHERE NOT EXISTS (
    SELECT 1 FROM curriculum.vocabulary_quiz_definition
    WHERE kind = 'FREQUENCY_BAND' AND frequency_rank_max = v.rank_max
);
