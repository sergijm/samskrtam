truncate table content.vocabulary_categories cascade;
truncate table content.vocabulary_words cascade;
truncate table content.vocabulary_word_categories cascade;
delete from "content"."lesson" WHERE lesson_type = 'VOCABULARY';

-- ============================================================================
--  Перенос классифицированных данных из raw_data.* в content.*
-- ============================================================================
--  Переносятся только категории, у которых количество связанных слов
--  (по таблице raw_data.dictionary_2500_categories) строго больше 50.
--  Если категория проходит порог, но её предок(и) в иерархии — нет,
--  предки всё равно переносятся (иначе parent_id в content.vocabulary_categories
--  будет ссылаться в никуда). Слова переносятся, только если у них остаётся
--  хотя бы одна связь с категорией, прошедшей порог.
--
--  Все целевые таблицы (content.vocabulary_words, content.vocabulary_categories,
--  content.vocabulary_word_categories) предполагаются ПУСТЫМИ — мерж не делается.
-- ============================================================================

BEGIN;

-- ────────────────────────────────────────────────────────────────────────
-- Шаг 0. Параметр порога (для удобства редактирования в одном месте)
-- ────────────────────────────────────────────────────────────────────────
-- Используем CTE вместо переменной, т.к. чистый SQL/plpgsql-блок ниже
-- зашивает порог напрямую как 50 в нескольких местах — при необходимости
-- поменяйте число 50 во всех местах, помеченных [THRESHOLD].

-- ────────────────────────────────────────────────────────────────────────
-- Шаг 1. Категории, прошедшие порог по количеству слов
-- ────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS tmp_qualifying_categories;
CREATE TEMP TABLE tmp_qualifying_categories AS
SELECT
    dc.category_code,
    count(DISTINCT dc.dictionary_word_id) AS word_count
FROM raw_data.dictionary_2500_categories dc
GROUP BY dc.category_code
HAVING count(DISTINCT dc.dictionary_word_id) > 50;   -- [THRESHOLD]

-- ────────────────────────────────────────────────────────────────────────
-- Шаг 2. Протягиваем предков для прошедших порог категорий,
--         чтобы иерархия в content.vocabulary_categories была целостной
--         (FK parent_id NOT NULL-совместимый обход дерева вверх)
-- ────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS tmp_categories_to_migrate;
CREATE TEMP TABLE tmp_categories_to_migrate AS
WITH RECURSIVE ancestors AS (
    -- стартуем с категорий, прошедших порог
    SELECT c.category_code, c.parent_code
    FROM raw_data.categories_claude c
             JOIN tmp_qualifying_categories q ON q.category_code = c.category_code

    UNION

    -- поднимаемся вверх по дереву к родителям
    SELECT c.category_code, c.parent_code
    FROM raw_data.categories_claude c
             JOIN ancestors a ON c.category_code = a.parent_code
)
SELECT DISTINCT a.category_code
FROM ancestors a;

-- ────────────────────────────────────────────────────────────────────────
-- Шаг 3. Перенос категорий в content.vocabulary_categories
--         Сначала генерируем id (uuid) для каждой переносимой категории,
--         затем вставляем с резолвингом parent_id через self-join по code.
--         Код категории нормализуется через regexp_replace: нижний регистр,
--         все символы кроме [a-z0-9] заменяются на '-' (например
--         "SOC.ROLE.ROYAL" -> "soc-role-royal").
--
--         ВНИМАНИЕ: content.vocabulary_categories.code имеет UNIQUE
--         constraint. Если после нормализации два разных category_code
--         схлопнутся в одинаковый slug, INSERT ниже упадёт по
--         vocabulary_categories_code_key. Проверка на коллизии вынесена
--         отдельным запросом — выполните его перед запуском основного
--         скрипта, если есть сомнения в исходных кодах категорий:
--
--   SELECT regexp_replace(lower(category_code), '[^a-z0-9]', '-', 'g') AS slug,
--          array_agg(category_code) AS original_codes,
--          count(*) AS cnt
--   FROM raw_data.categories_claude
--   GROUP BY 1
--   HAVING count(*) > 1;
-- ────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS tmp_category_ids;
CREATE TEMP TABLE tmp_category_ids AS
SELECT
    m.category_code,
    gen_random_uuid() AS new_id
FROM tmp_categories_to_migrate m;

INSERT INTO content.vocabulary_categories
(id, code, parent_id, name_ru, name_en, description_ru, description_en)
SELECT
    ti.new_id,
    regexp_replace(lower(c.category_code), '[^a-z0-9]', '-', 'g') AS code,
    parent_ti.new_id                       AS parent_id,   -- NULL если родитель не переносится / корень
    c.name_ru,
    c.name_en,
    c.description_ru,
    c.description_en
FROM raw_data.categories_claude c
         JOIN tmp_category_ids ti        ON ti.category_code = c.category_code
         LEFT JOIN tmp_category_ids parent_ti
                   ON parent_ti.category_code = c.parent_code;

-- ────────────────────────────────────────────────────────────────────────
-- Шаг 4. Слова, у которых остаётся хотя бы одна связь
--         с категорией, прошедшей порог.
-- ────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS tmp_words_to_migrate;
CREATE TEMP TABLE tmp_words_to_migrate AS
SELECT DISTINCT dc.dictionary_word_id
FROM raw_data.dictionary_2500_categories dc
         JOIN tmp_qualifying_categories q ON q.category_code = dc.category_code;

-- ────────────────────────────────────────────────────────────────────────
-- Шаг 5. Перенос слов в content.vocabulary_words
--         word_iast <- raw_data.dictionary_2500.word
--         translation_ru / translation_en <- одноимённые колонки
--         gender обязателен по CHECK, но колонки-источника нет -> 'UNKNOWN'
-- ────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS tmp_word_ids;
CREATE TEMP TABLE tmp_word_ids AS
SELECT
    d.id                AS src_word_id,
    gen_random_uuid()   AS new_id
FROM raw_data.dictionary_2500 d
         JOIN tmp_words_to_migrate w ON w.dictionary_word_id = d.id;

INSERT INTO content.vocabulary_words
(id, word_iast, translation_ru, translation_en, gender)
SELECT
    wi.new_id,
    d.word,
    COALESCE(d.fri_ru, d.translation_ru, ''),
    COALESCE(d.fri_en, d.translation_en, ''),
    'UNKNOWN'
FROM raw_data.dictionary_2500 d
         JOIN tmp_word_ids wi ON wi.src_word_id = d.id;

-- ────────────────────────────────────────────────────────────────────────
-- Шаг 6. Перенос связей слово <-> категория
--         в content.vocabulary_word_categories.
--
--         ВАЖНО: связь распространяется по ВСЕЙ цепочке предков.
--         То есть если слово привязано к листовой категории
--         SOC.ROLE.ROYAL (прошедшей порог), оно дополнительно
--         получает связи с SOC.ROLE и SOC (корневая категория домена),
--         даже если сами SOC.ROLE / SOC не накопили >50 слов напрямую
--         по dictionary_2500_categories. Это нужно, чтобы запрос
--         "все слова в корневой категории SOC" включал слова всех
--         её потомков.
--
--         Реализация: рекурсивно поднимаемся от каждой qualifying-связи
--         вверх по parent_code до корня, на каждом шаге сохраняя пару
--         (dictionary_word_id, category_code_по_пути).
-- ────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS tmp_word_category_closure;
CREATE TEMP TABLE tmp_word_category_closure AS
WITH RECURSIVE closure AS (
    -- базовый уровень: прямые связи слово -> категория, прошедшая порог
    SELECT
        dc.dictionary_word_id,
        dc.category_code,
        cc.parent_code
    FROM raw_data.dictionary_2500_categories dc
             JOIN tmp_qualifying_categories q  ON q.category_code = dc.category_code
             JOIN raw_data.categories_claude cc ON cc.category_code = dc.category_code

    UNION

    -- подъём по дереву: для каждой пары (слово, категория) добавляем родителя
    SELECT
        cl.dictionary_word_id,
        cc.category_code,
        cc.parent_code
    FROM closure cl
             JOIN raw_data.categories_claude cc ON cc.category_code = cl.parent_code
)
SELECT DISTINCT dictionary_word_id, category_code
FROM closure;

INSERT INTO content.vocabulary_word_categories
(vocabulary_word_id, category_id)
SELECT DISTINCT
    wi.new_id,
    ci.new_id
FROM tmp_word_category_closure wcc
         JOIN tmp_word_ids     wi ON wi.src_word_id   = wcc.dictionary_word_id
         JOIN tmp_category_ids ci ON ci.category_code = wcc.category_code;

-- ────────────────────────────────────────────────────────────────────────
-- Шаг 7. Контрольная сводка
-- ────────────────────────────────────────────────────────────────────────
DO $$
    DECLARE
        v_categories_total   int;
        v_categories_qualify int;
        v_words_total        int;
        v_links_total        int;
    BEGIN
        SELECT count(*) INTO v_categories_total FROM content.vocabulary_categories;
        SELECT count(*) INTO v_categories_qualify FROM tmp_qualifying_categories;
        SELECT count(*) INTO v_words_total FROM content.vocabulary_words;
        SELECT count(*) INTO v_links_total FROM content.vocabulary_word_categories;

        RAISE NOTICE 'Категорий, прошедших порог (>50 слов) напрямую: %', v_categories_qualify;
        RAISE NOTICE 'Категорий перенесено всего (включая предков для иерархии): %', v_categories_total;
        RAISE NOTICE 'Слов перенесено: %', v_words_total;
        RAISE NOTICE 'Связей слово-категория перенесено (включая связи с предками/корнями): %', v_links_total;
    END $$;

-- Если всё устраивает — COMMIT. Если нужно проверить результат перед фиксацией,
-- замените COMMIT на ROLLBACK и проверьте данные в открытой транзакции
-- (учтите: temp-таблицы видны только в рамках текущей сессии).
COMMIT;

-- ────────────────────────────────────────────────────────────────────────
-- Очистка временных таблиц (опционально, они и так DROP-нутся в конце сессии)
-- ────────────────────────────────────────────────────────────────────────
DROP TABLE IF EXISTS tmp_qualifying_categories;
DROP TABLE IF EXISTS tmp_categories_to_migrate;
DROP TABLE IF EXISTS tmp_category_ids;
DROP TABLE IF EXISTS tmp_words_to_migrate;
DROP TABLE IF EXISTS tmp_word_ids;
DROP TABLE IF EXISTS tmp_word_category_closure;

-- ============================================================================
--  Проверочные запросы (выполнить отдельно после переноса)
-- ============================================================================
-- Сколько слов в каждой перенесённой категории (включая унаследованные
-- от потомков через closure — например корневая категория SOC покажет
-- сумму слов всех своих дочерних веток):
-- SELECT vc.code, vc.name_ru, count(*) AS word_count
-- FROM content.vocabulary_word_categories wc
-- JOIN content.vocabulary_categories vc ON vc.id = wc.category_id
-- GROUP BY vc.code, vc.name_ru
-- ORDER BY word_count DESC;

-- Проверка целостности иерархии (категории без слов вообще, такого
-- быть не должно после добавления closure, но на всякий случай):
-- SELECT vc.code, vc.name_ru
-- FROM content.vocabulary_categories vc
-- WHERE NOT EXISTS (
--     SELECT 1 FROM content.vocabulary_word_categories wc WHERE wc.category_id = vc.id
-- );

-- Пример слова с его категориями после переноса (должны быть видны
-- и листовая категория, и все её предки вплоть до корня домена):
-- SELECT w.word_iast, w.translation_ru, vc.code, vc.name_ru
-- FROM content.vocabulary_words w
-- JOIN content.vocabulary_word_categories wc ON wc.vocabulary_word_id = w.id
-- JOIN content.vocabulary_categories vc ON vc.id = wc.category_id
-- WHERE w.word_iast = 'abda'
-- ORDER BY vc.code;



-----------------------------------------------------------------------------------------------------------------
-- V2__seed_initial_data.sql
-- Заполнение начальных данных для content-service

-- 1. Начальные квизы по лексике (из V6__seed_vocabulary_quizzes.sql)
-- Скрипт генерирует INSERT для таблицы quizzes
-- на основе данных из vocabulary_categories.
--
-- Правила маппинга:
--   code       -> slug (нижний регистр, '_' заменяется на '-')
--   name_en    -> title_en
--   name_ru    -> title_ru
--   description_en / description_ru (если NULL — генерируем базовый текст)
--   quiz_type  = 'VOCABULARY'
--   difficulty = 'BEGINNER' для корневых категорий,
--                'INTERMEDIATE' для дочерних (parent_id IS NOT NULL)
--   questions_per_session = 10
--   id         = генерируем через gen_random_uuid()

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
    'VOCABULARY_BASIC'                                             AS quiz_type,
    CASE WHEN vc.parent_id IS NULL THEN 'BEGINNER'
         ELSE 'INTERMEDIATE'
        END                                                      AS difficulty,
    10                                                       AS questions_per_session,
    now()                                                    AS created_at,
    NULL                                                     AS deleted_at
FROM content.vocabulary_categories vc;
