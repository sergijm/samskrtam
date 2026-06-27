-- 2. Начальные квизы по склонениям (обновлено для HK-слагов и специфичных QuizType)
INSERT INTO CONTENT.lesson ( ID, slug, title_ru, title_en, description_ru, description_en, lesson_type, difficulty, questions_per_session)
VALUES
    ( '20000000-0000-0000-0000-000000000001', 'declensions-a-masc', 'Склонения: Основы на -a (мужской род)', 'Declensions: A-stems (masculine)', 'Квиз по склонению существительных мужского рода на -a. Включает существительные типа "deva-".', 'Quiz on declension of masculine nouns ending in -a. Includes nouns like "deva-".', 'A_STEM_DECLENSIONS', 'BEGINNER', 10 ),
    ( '20000000-0000-0000-0000-000000000002', 'declensions-a-neut', 'Склонения: Основы на -a (средний род)', 'Declensions: A-stems (neuter)', 'Квиз по склонению существительных среднего рода на -a. Включает существительные типа "agāra-".', 'Quiz on declension of neuter nouns ending in -a. Includes nouns like "agāra-".', 'A_STEM_DECLENSIONS', 'BEGINNER', 10),
    ( '20000000-0000-0000-0000-000000000003', 'declensions-aa-fem', 'Склонения: Основы на -ā (женский род)', 'Declensions: Ā-stems (feminine)', 'Квиз по склонению существительных женского рода на -ā.', 'Quiz on declension of feminine nouns ending in -ā.', 'AA_STEM_DECLENSIONS', 'BEGINNER', 10),
    ( '20000000-0000-0000-0000-000000000004', 'declensions-i', 'Склонения: Основы на -i', 'Declensions: I-stems', 'Квиз по склонению существительных на -i.', 'Quiz on declension of nouns ending in -i.', 'I_STEM_DECLENSIONS', 'INTERMEDIATE', 10 ),
    ( '20000000-0000-0000-0000-000000000005', 'declensions-ii', 'Declensions: Основы на -ī', 'Declensions: Ī-stems', 'Квиз по склонению существительных на -ī.', 'Quiz on declension of nouns ending in -ī.', 'II_STEM_DECLENSIONS', 'INTERMEDIATE', 10 ),
    ( '20000000-0000-0000-0000-000000000006', 'declensions-u', 'Склонения: Основы на -u', 'Declensions: U-stems', 'Квиз по склонению существительных на -u.', 'Quiz on declension of nouns ending in -u.', 'U_STEM_DECLENSIONS', 'INTERMEDIATE', 10 ),
    ( '20000000-0000-0000-0000-000000000007', 'declensions-uu', 'Склонения: Основы на -ū', 'Declensions: Ū-stems', 'Квиз по склонению существительных на -ū.', 'Quiz on declension of nouns ending in -ū.', 'UU_STEM_DECLENSIONS', 'ADVANCED', 10),
    ( '20000000-0000-0000-0000-000000000008', 'declensions-r', 'Склонения: Основы на -ṛ', 'Declensions: Ṛ-stems', 'Квиз по склонению существительных на -ṛ.', 'Quiz on declension of nouns ending in -ṛ.', 'R_STEM_DECLENSIONS', 'ADVANCED', 10 ),
    ( '20000000-0000-0000-0000-000000000009', 'declensions-all', 'Склонения: Все основы', 'Declensions: All Stems', 'Квиз по склонению существительных всех типов основ.', 'Quiz on declension of nouns of all stem types.', 'DECLENSIONS', 'ADVANCED', 15 );
-- Шаг 1: Заполнение таблицы основ (content.declension_stems) из уникальных записей в raw_data.
-- Мы используем DISTINCT для выбора каждой уникальной основы только один раз.
-- ON CONFLICT гарантирует, что если основа уже существует, мы просто пропустим ее, избегая ошибок.
INSERT INTO CONTENT.declension_stems ( stem_name_iast, vowel_type, gender ) SELECT DISTINCT
                                                                                rd.основа,
-- Преобразуем 'a', 'ā', 'i' и т.д. в формат 'A_STEM', 'AA_STEM', 'I_STEM'
                                                                                CASE

                                                                                    WHEN rd.тематическая_гласная = 'a' THEN
                                                                                        'A_STEM'
                                                                                    WHEN rd.тематическая_гласная = 'ā' THEN
                                                                                        'AA_STEM'
                                                                                    WHEN rd.тематическая_гласная = 'i' THEN
                                                                                        'I_STEM'
                                                                                    WHEN rd.тематическая_гласная = 'ī' THEN
                                                                                        'II_STEM'
                                                                                    WHEN rd.тематическая_гласная = 'u' THEN
                                                                                        'U_STEM'
                                                                                    WHEN rd.тематическая_гласная = 'ū' THEN
                                                                                        'UU_STEM'
                                                                                    WHEN rd.тематическая_гласная = 'ṛ' THEN
                                                                                        'R_STEM' ELSE'UNKNOWN' -- Обработка непредвиденных значений

                                                                                    END AS vowel_type,
-- Преобразуем 'Masculine', 'Feminine' в формат 'MASCULINE', 'FEMININE'
                                                                                UPPER ( rd.gender_latin ) AS gender
FROM
    raw_data.sanskrit_declensions_enriched rd
WHERE
    rd.основа != 'основа' -- Пропускаем заголовочную строку

  AND UPPER ( rd.gender ) != 'UNKNOWN' ON CONFLICT ( stem_name_iast ) DO
    NOTHING;
-- Шаг 2: Заполнение таблицы форм склонений (content.declension_forms).
-- Мы соединяем raw_data с уже заполненной таблицей declension_stems, чтобы получить UUID для каждой основы.
-- Это позволяет нам установить правильный внешний ключ (declension_stem_id).
INSERT INTO CONTENT.declension_forms ( declension_stem_id, case_type, number_type, form_iast, form_devanagari ) SELECT
                                                                                                                    ds.ID,
-- Преобразуем 'Nominative', 'Accusative' в формат 'NOMINATIVE', 'ACCUSATIVE'
                                                                                                                    UPPER ( rd.case_latin ) AS case_type,
                                                                                                                    UPPER ( rd.number_latin ) AS number_type,
                                                                                                                    rd.declension_resullt,
                                                                                                                    rd.devanagari
FROM
    raw_data.sanskrit_declensions_enriched rd
        JOIN CONTENT.declension_stems ds ON rd.основа = ds.stem_name_iast
WHERE
    rd.основа != 'основа' -- Пропускаем заголовочную строку

  AND UPPER ( rd.gender ) != 'UNKNOWN' ON CONFLICT ( declension_stem_id, case_type, number_type ) DO
    NOTHING;