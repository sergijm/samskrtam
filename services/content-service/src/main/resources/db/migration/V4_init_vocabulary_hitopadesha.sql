-- Вставляем корневую категорию
WITH root AS (
    INSERT INTO "content"."vocabulary_categories" (
                                                   "code",
                                                   "parent_id",
                                                   "name_ru",
                                                   "name_en",
                                                   "description_ru",
                                                   "description_en"
        ) VALUES (
                     'hitopadesha',
                     NULL,
                     'Хитопадеша',
                     'Hitopadesha',
                     'Словарь санскритских слов из текста Хитопадеша',
                     'Sanskrit vocabulary from the text Hitopadesha'
                 )
        ON CONFLICT ("code") DO UPDATE SET
            "name_ru" = EXCLUDED."name_ru",
            "name_en" = EXCLUDED."name_en",
            "description_ru" = EXCLUDED."description_ru",
            "description_en" = EXCLUDED."description_en"
        RETURNING "id"
)
-- Вставляем подкатегорию Book Four - Peace
   , book_four AS (
    INSERT INTO "content"."vocabulary_categories" (
                                                   "code",
                                                   "parent_id",
                                                   "name_ru",
                                                   "name_en",
                                                   "description_ru",
                                                   "description_en"
        )
        SELECT
            'hitopadesha-book-4',
            root."id",
            'Книга Четвертая — Мир',
            'Book Four — Peace',
            'Словарь слов из четвёртой книги Хитопадеши (Шанти)',
            'Vocabulary from the fourth book of Hitopadesha (Peace)'
        FROM root
        ON CONFLICT ("code") DO UPDATE SET
            "parent_id" = EXCLUDED."parent_id",
            "name_ru" = EXCLUDED."name_ru",
            "name_en" = EXCLUDED."name_en",
            "description_ru" = EXCLUDED."description_ru",
            "description_en" = EXCLUDED."description_en"
        RETURNING "id"
)
-- Вставляем подкатегорию Chapter 6 - The Story of the Recluse and the Mouse
INSERT INTO "content"."vocabulary_categories" (
    "code",
    "parent_id",
    "name_ru",
    "name_en",
    "description_ru",
    "description_en"
)
SELECT
    'hitopadesha-book-4-chapter-6',
    book_four."id",
    'Глава 6 — История отшельника и мыши',
    'Chapter 6 — The Story of the Recluse and the Mouse',
    'Словарь слов из истории об отшельнике, превратившем мышь в тигра (Хитопадеша, книга IV, глава 6)',
    'Vocabulary from the story of the recluse who turned a mouse into a tiger (Hitopadesha, Book IV, Chapter 6)'
FROM book_four
ON CONFLICT ("code") DO UPDATE SET
                                   "parent_id" = EXCLUDED."parent_id",
                                   "name_ru" = EXCLUDED."name_ru",
                                   "name_en" = EXCLUDED."name_en",
                                   "description_ru" = EXCLUDED."description_ru",
                                   "description_en" = EXCLUDED."description_en";


-- ============================================================
-- 1. Вставка слов в vocabulary_words
-- ============================================================
WITH inserted_words AS (
    INSERT INTO "content"."vocabulary_words" (
                                              "word_iast",
                                              "word_devanagari",
                                              "translation_ru",
                                              "translation_en",
                                              "gender",
                                              "stem",
                                              "root",
                                              "explanation_ru",
                                              "explanation_en"
        )
        SELECT
            "word_iast",
            "word_devanagary" AS "word_devanagari",
            "translation_ru",
            "translation_en",
            -- Извлекаем род из grammar
            CASE
                WHEN "grammar" ~ 'masculine|masc\.|m\.|мужской' THEN upper('masculine')
                WHEN "grammar" ~ 'feminine|fem\.|f\.|женский' THEN upper('feminine')
                WHEN "grammar" ~ 'neuter|neut\.|n\.|средний' THEN upper('neuter')
                ELSE NULL
                END AS "gender",
            -- Извлекаем корень/основу из grammar
            CASE
                WHEN "grammar" ~ '√[a-zA-Zāīūṛṝṃḥśṣṭḍñṅ+]+'
                    THEN SUBSTRING("grammar" FROM '√([a-zA-Zāīūṛṝṃḥśṣṭḍñṅ+]+)')
                WHEN "part_of_speech" = 'noun' THEN "word_iast"
                WHEN "part_of_speech" = 'adjective' THEN "word_iast"
                ELSE NULL
                END AS "stem",
            -- В root помещаем корень (для глаголов)
            CASE
                WHEN "part_of_speech" = 'verb' AND "grammar" ~ '√[a-zA-Zāīūṛṝṃḥśṣṭḍñṅ+]+'
                    THEN SUBSTRING("grammar" FROM '√([a-zA-Zāīūṛṝṃḥśṣṭḍñṅ+]+)')
                ELSE NULL
                END AS "root",
            -- explanation_ru
            CASE
                WHEN "part_of_speech" = 'noun' THEN 'Существительное. ' || "grammar"
                WHEN "part_of_speech" = 'verb' THEN 'Глагол. ' || "grammar"
                WHEN "part_of_speech" = 'adjective' THEN 'Прилагательное. ' || "grammar"
                WHEN "part_of_speech" = 'adverb' THEN 'Наречие. ' || "grammar"
                WHEN "part_of_speech" = 'pronoun' THEN 'Местоимение. ' || "grammar"
                WHEN "part_of_speech" = 'particle' THEN 'Частица. ' || "grammar"
                WHEN "part_of_speech" = 'conjunction' THEN 'Союз. ' || "grammar"
                ELSE "grammar"
                END AS "explanation_ru",
            -- explanation_en
            CASE
                WHEN "part_of_speech" = 'noun' THEN 'Noun. ' || "grammar"
                WHEN "part_of_speech" = 'verb' THEN 'Verb. ' || "grammar"
                WHEN "part_of_speech" = 'adjective' THEN 'Adjective. ' || "grammar"
                WHEN "part_of_speech" = 'adverb' THEN 'Adverb. ' || "grammar"
                WHEN "part_of_speech" = 'pronoun' THEN 'Pronoun. ' || "grammar"
                WHEN "part_of_speech" = 'particle' THEN 'Particle. ' || "grammar"
                WHEN "part_of_speech" = 'conjunction' THEN 'Conjunction. ' || "grammar"
                ELSE "grammar"
                END AS "explanation_en"
        FROM "raw_data"."hitopadesha_dictionary"
        WHERE "word_iast" IS NOT NULL
        RETURNING "id", "word_iast"
)

-- ============================================================
-- 2. Связывание слов с категорией hitopadesha-book-4-chapter-6
-- ============================================================
   , category AS (
    SELECT "id"
    FROM "content"."vocabulary_categories"
    WHERE "code" = 'hitopadesha-book-4-chapter-6'
)

-- Вставляем связи слово-категория
INSERT INTO "content"."vocabulary_word_categories" (
    "vocabulary_word_id",
    "category_id"
)
SELECT
    inserted_words."id",
    category."id"
FROM inserted_words
         CROSS JOIN category;

INSERT INTO "content"."lesson"
("id", "slug", "title_ru", "title_en",
 "description_ru", "description_en",
 "lesson_type", "difficulty", "questions_per_session",
 "created_at", "deleted_at")
SELECT
    gen_random_uuid()                                        AS id,
    lower(regexp_replace(vc.code, '[._]+', '-', 'g'))             AS slug,
    vc.name_ru                                               AS title_ru,
    vc.name_en                                               AS title_en,
    COALESCE(vc.description_ru,
             'Квиз по лексике на тему «' || vc.name_ru || '».')  AS description_ru,
    COALESCE(vc.description_en,
             'Vocabulary quiz on the topic of ' || vc.name_en || '.')  AS description_en,
    'VOCABULARY_TEXTS'                                             AS quiz_type,
    CASE WHEN vc.parent_id IS NULL THEN 'BEGINNER'
         ELSE 'INTERMEDIATE'
        END                                                      AS difficulty,
    10                                                       AS questions_per_session,
    now()                                                    AS created_at,
    NULL                                                     AS deleted_at
FROM content.vocabulary_categories vc
WHERE vc.code like 'hitopad%'


