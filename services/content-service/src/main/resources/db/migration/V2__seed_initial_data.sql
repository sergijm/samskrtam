-- V2__seed_initial_data.sql
-- Заполнение начальных данных для content-service

-- 1. Начальные квизы по лексике (из V6__seed_vocabulary_quizzes.sql)
INSERT INTO content.quizzes (id, slug, title_ru, title_en, description_ru, description_en, quiz_type, difficulty, questions_per_session)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'animals',    'Животные',        'Animals',          'Квиз по лексике на тему животных.', 'Quiz on animal vocabulary.', 'VOCABULARY', 'BEGINNER', 10),
    ('10000000-0000-0000-0000-000000000002', 'numbers',    'Числа',           'Numbers',          'Квиз по лексике на тему чисел.', 'Quiz on number vocabulary.', 'VOCABULARY', 'BEGINNER', 10),
    ('10000000-0000-0000-0000-000000000003', 'body-parts', 'Тело',            'Body Parts',       'Квиз по лексике на тему частей тела.', 'Quiz on body parts vocabulary.', 'VOCABULARY', 'BEGINNER', 10),
    ('10000000-0000-0000-0000-000000000004', 'nature',     'Природа',         'Nature',           'Квиз по лексике на тему природы.', 'Quiz on nature vocabulary.', 'VOCABULARY', 'BEGINNER', 10), -- Исправлено description_en
    ('10000000-0000-0000-0000-000000000005', 'basic-vocabulary', 'Базовая лексика', 'Basic Vocabulary', 'Квиз по базовой санскритской лексике.', 'Quiz on basic Sanskrit vocabulary.', 'VOCABULARY', 'BEGINNER', 10), -- Изменено slug
    ('10000000-0000-0000-0000-000000000006', 'intermediate-vocabulary', 'Средний уровень', 'Intermediate',     'Квиз по санскритской лексике среднего уровня.', 'Quiz on intermediate Sanskrit vocabulary.', 'VOCABULARY', 'INTERMEDIATE', 10); -- Изменено slug

-- 2. Начальные квизы по склонениям (из V4__seed_declensions_quizzes.sql)
INSERT INTO content.quizzes (id, slug, title_ru, title_en, description_ru, description_en, quiz_type, difficulty, questions_per_session)
VALUES
    ('20000000-0000-0000-0000-000000000001', 'declensions-a-masc', 'Склонения: Основы на -a (мужской род)', 'Declensions: A-stems (masculine)', 'Квиз по склонению существительных мужского рода на -a. Включает существительные типа "deva-".', 'Quiz on declension of masculine nouns ending in -a. Includes nouns like "deva-".', 'DECLENSIONS', 'BEGINNER', 10),
    ('20000000-0000-0000-0000-000000000002', 'declensions-a-neut', 'Склонения: Основы на -a (средний род)', 'Declensions: A-stems (neuter)', 'Квиз по склонению существительных среднего рода на -a. Включает существительные типа "agāra-".', 'Quiz on declension of neuter nouns ending in -a. Includes nouns like "agāra-".', 'DECLENSIONS', 'BEGINNER', 10),
    ('20000000-0000-0000-0000-000000000003', 'declensions-a-fem', 'Склонения: Основы на -ā (женский род)', 'Declensions: Ā-stems (feminine)', 'Квиз по склонению существительных женского рода на -ā.', 'Quiz on declension of feminine nouns ending in -ā.', 'DECLENSIONS', 'BEGINNER', 10),
    ('20000000-0000-0000-0000-000000000004', 'declensions-i', 'Склонения: Основы на -i', 'Declensions: I-stems', 'Квиз по склонению существительных на -i.', 'Quiz on declension of nouns ending in -i.', 'DECLENSIONS', 'INTERMEDIATE', 10),
    ('20000000-0000-0000-0000-000000000005', 'declensions-ī', 'Declensions: Основы на -ī', 'Declensions: Ī-stems', 'Квиз по склонению существительных на -ī.', 'Quiz on declension of nouns ending in -ī.', 'DECLENSIONS', 'INTERMEDIATE', 10),
    ('20000000-0000-0000-0000-000000000006', 'declensions-u', 'Склонения: Основы на -u', 'Declensions: U-stems', 'Квиз по склонению существительных на -u.', 'Quiz on declension of nouns ending in -u.', 'DECLENSIONS', 'INTERMEDIATE', 10),
    ('20000000-0000-0000-0000-000000000007', 'declensions-ū', 'Склонения: Основы на -ū', 'Declensions: Ū-stems', 'Квиз по склонению существительных на -ū.', 'Quiz on declension of nouns ending in -ū.', 'DECLENSIONS', 'ADVANCED', 10),
    ('20000000-0000-0000-0000-000000000008', 'declensions-ṛ', 'Склонения: Основы на -ṛ', 'Declensions: Ṛ-stems', 'Квиз по склонению существительных на -ṛ.', 'Quiz on declension of nouns ending in -ṛ.', 'DECLENSIONS', 'ADVANCED', 10),
    ('20000000-0000-0000-0000-000000000009', 'declensions-all', 'Склонения: Все основы', 'Declensions: All Stems', 'Квиз по склонению существительных всех типов основ.', 'Quiz on declension of nouns of all stem types.', 'DECLENSIONS', 'ADVANCED', 15);

-- 3. Добавление DeclensionStems
INSERT INTO content.declension_stems (id, stem_name_iast, stem_name_devanagari, vowel_type, gender) VALUES
                                                                                                        ('30000000-0000-0000-0000-000000000001', 'deva', 'देव', 'A_STEM', 'MASCULINE'),
                                                                                                        ('30000000-0000-0000-0000-000000000002', 'agāra', 'अगार', 'A_STEM', 'NEUTER'),
                                                                                                        ('30000000-0000-0000-0000-000000000003', 'dhenu', 'धेनु', 'U_STEM', 'FEMININE');

-- 4. Добавление DeclensionForms для 'deva' (мужской род, a-основа)
INSERT INTO content.declension_forms (declension_stem_id, case_type, number_type, form_iast, form_devanagari) VALUES
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'NOMINATIVE', 'SINGULAR', 'devaḥ', 'देवः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'NOMINATIVE', 'DUAL', 'devau', 'देवौ'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'NOMINATIVE', 'PLURAL', 'devāḥ', 'देवाः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'ACCUSATIVE', 'SINGULAR', 'devam', 'देवम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'ACCUSATIVE', 'DUAL', 'devau', 'देवौ'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'ACCUSATIVE', 'PLURAL', 'devān', 'देवान्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'INSTRUMENTAL', 'SINGULAR', 'devena', 'देवेन'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'INSTRUMENTAL', 'DUAL', 'devābhyām', 'देवाभ्याम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'INSTRUMENTAL', 'PLURAL', 'devaiḥ', 'देवैः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'DATIVE', 'SINGULAR', 'devāya', 'देवाय'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'DATIVE', 'DUAL', 'devābhyām', 'देवाभ्याम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'DATIVE', 'PLURAL', 'devebhyaḥ', 'देवेभ्यः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'ABLATIVE', 'SINGULAR', 'devāt', 'देवात्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'ABLATIVE', 'DUAL', 'devābhyām', 'देवाभ्याम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'ABLATIVE', 'PLURAL', 'devebhyaḥ', 'देवेभ्यः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'GENITIVE', 'SINGULAR', 'devasya', 'देवस्य'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'GENITIVE', 'DUAL', 'devayoḥ', 'देवयोः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'GENITIVE', 'PLURAL', 'devānām', 'देवानाम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'LOCATIVE', 'SINGULAR', 'deve', 'देवे'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'LOCATIVE', 'DUAL', 'devayoḥ', 'देवयोः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'LOCATIVE', 'PLURAL', 'deveṣu', 'देवेषु'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'VOCATIVE', 'SINGULAR', 'deva', 'देव'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'VOCATIVE', 'DUAL', 'devau', 'देवौ'),
                                                                                                                  ('30000000-0000-0000-0000-000000000001', 'VOCATIVE', 'PLURAL', 'devāḥ', 'देवाः');

-- 5. Добавление DeclensionForms для 'agāra' (средний род, a-основа)
INSERT INTO content.declension_forms (declension_stem_id, case_type, number_type, form_iast, form_devanagari) VALUES
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'NOMINATIVE', 'SINGULAR', 'agāram', 'अगारम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'NOMINATIVE', 'DUAL', 'agāre', 'अगारे'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'NOMINATIVE', 'PLURAL', 'agārāṇi', 'अगारानि'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'ACCUSATIVE', 'SINGULAR', 'agāram', 'अगारम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'ACCUSATIVE', 'DUAL', 'agāre', 'अगारे'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'ACCUSATIVE', 'PLURAL', 'agārāṇi', 'अगारानि'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'INSTRUMENTAL', 'SINGULAR', 'agāreṇa', 'अगारेण'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'INSTRUMENTAL', 'DUAL', 'agārābhyām', 'अगारभ्याम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'INSTRUMENTAL', 'PLURAL', 'agāraiḥ', 'अगारैः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'DATIVE', 'SINGULAR', 'agārāya', 'अगारय'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'DATIVE', 'DUAL', 'agārābhyām', 'अगारभ्याम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'DATIVE', 'PLURAL', 'agārebhyaḥ', 'अगारभ्यः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'ABLATIVE', 'SINGULAR', 'agārāt', 'अगारत्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'ABLATIVE', 'DUAL', 'agārābhyām', 'अगारभ्याम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'ABLATIVE', 'PLURAL', 'agārebhyaḥ', 'अगारभ्यः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'GENITIVE', 'SINGULAR', 'agārasya', 'अगारस्य'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'GENITIVE', 'DUAL', 'agārayoḥ', 'अगारयोः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'GENITIVE', 'PLURAL', 'agārāṇām', 'अगारणाम्'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'LOCATIVE', 'SINGULAR', 'agāre', 'अगारे'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'LOCATIVE', 'DUAL', 'agārayoḥ', 'अगारयोः'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'LOCATIVE', 'PLURAL', 'agāreṣu', 'अगारेशु'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'VOCATIVE', 'SINGULAR', 'agāra', 'अगार'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'VOCATIVE', 'DUAL', 'agāre', 'अगारे'),
                                                                                                                  ('30000000-0000-0000-0000-000000000002', 'VOCATIVE', 'PLURAL', 'agārāṇi', 'अगारानि');

-- 6. Добавление вопросов для квиза "declensions-a-masc" (deva-)
INSERT INTO content.questions (id, quiz_id, text_ru, text_en, explanation_ru, explanation_en, declension_stem_id, target_case, target_number)
VALUES
    ('40000000-0000-0000-0000-000000000001', (SELECT id FROM content.quizzes WHERE slug = 'declensions-a-masc'),
     'Какова форма слова "deva-" (бог) в Именительном падеже, Единственном числе?',
     'What is the form of "deva-" (god) in Nominative case, Singular number?',
     'Именительный падеж единственного числа для мужских родов на -a образуется с помощью окончания -aḥ.',
     'The Nominative singular for masculine a-stems is formed with the ending -aḥ.',
     '30000000-0000-0000-0000-000000000001', 'NOMINATIVE', 'SINGULAR');

INSERT INTO content.question_options (id, question_id, form_iast, form_devanagari)
VALUES
    ('50000000-0000-0000-0000-000000000001', (SELECT id FROM content.questions WHERE id = '40000000-0000-0000-0000-000000000001'), 'devaḥ', 'देवः');
UPDATE content.questions SET correct_option_id = '50000000-0000-0000-0000-000000000001' WHERE id = '40000000-0000-0000-0000-000000000001';

-- Вопрос 2: deva-, Винительный, Единственное число
INSERT INTO content.questions (id, quiz_id, text_ru, text_en, explanation_ru, explanation_en, declension_stem_id, target_case, target_number)
VALUES
    ('40000000-0000-0000-0000-000000000002', (SELECT id FROM content.quizzes WHERE slug = 'declensions-a-masc'),
     'Какова форма слова "deva-" (бог) в Винительном падеже, Единственном числе?',
     'What is the form of "deva-" (god) in Accusative case, Singular number?',
     'Винительный падеж единственного числа для мужских родов на -a образуется с помощью окончания -am.',
     'The Accusative singular for masculine a-stems is formed with the ending -am.',
     '30000000-0000-0000-0000-000000000001', 'ACCUSATIVE', 'SINGULAR');

INSERT INTO content.question_options (id, question_id, form_iast, form_devanagari)
VALUES
    ('50000000-0000-0000-0000-000000000002', (SELECT id FROM content.questions WHERE id = '40000000-0000-0000-0000-000000000002'), 'devam', 'देवम्');
UPDATE content.questions SET correct_option_id = '50000000-0000-0000-0000-000000000002' WHERE id = '40000000-0000-0000-0000-000000000002';

-- 7. Добавление вопросов для квиза "declensions-a-neut" (agāra-)
-- Вопрос 1: agāra-, Именительный, Единственное число
INSERT INTO content.questions (id, quiz_id, text_ru, text_en, explanation_ru, explanation_en, declension_stem_id, target_case, target_number)
VALUES
    ('40000000-0000-0000-0000-000000000003', (SELECT id FROM content.quizzes WHERE slug = 'declensions-a-neut'),
     'Какова форма слова "agāra-" (покои) в Именительном падеже, Единственном числе?',
     'What is the form of "agāra-" (abode) in Nominative case, Singular number?',
     'Именительный падеж единственного числа для средних родов на -a образуется с помощью окончания -am.',
     'The Nominative singular for neuter a-stems is formed with the ending -am.',
     '30000000-0000-0000-0000-000000000002', 'NOMINATIVE', 'SINGULAR');

INSERT INTO content.question_options (id, question_id, form_iast, form_devanagari)
VALUES
    ('50000000-0000-0000-0000-000000000003', (SELECT id FROM content.questions WHERE id = '40000000-0000-0000-0000-000000000003'), 'agāram', 'अगारम्');
UPDATE content.questions SET correct_option_id = '50000000-0000-0000-0000-000000000003';