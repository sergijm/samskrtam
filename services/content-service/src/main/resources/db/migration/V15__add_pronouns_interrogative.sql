-- V15: pronouns-interrogative lesson + kim stems and forms (ADR-008 step 3/5)

-- ============================================================
-- 1. Lesson: pronouns-interrogative
-- ============================================================
INSERT INTO CONTENT.lesson (id, slug, title_ru, title_en, description_ru, description_en, lesson_type, difficulty, questions_per_session)
VALUES
    ('20000000-0000-0000-0000-00000000000e', 'pronouns-interrogative',
     'Местоимения: вопросительные',
     'Pronouns: interrogative',
     'Квиз по склонению вопросительного местоимения kim (кто? что?).',
     'Quiz on declension of the interrogative pronoun kim (who? what?).',
     'DECLENSIONS', 'INTERMEDIATE', 10)
ON CONFLICT (slug) DO NOTHING;

-- ============================================================
-- 2. Stems: kim (3 genders)
-- ============================================================
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('kim-m', 'PRON_KIM', 'MASCULINE') ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('kim-f', 'PRON_KIM', 'FEMININE')  ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('kim-n', 'PRON_KIM', 'NEUTER')    ON CONFLICT (stem_iast) DO NOTHING;

-- 2a. kim masculine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','kah'),('ACCUSATIVE','kam'),('INSTRUMENTAL','kena'),('DATIVE','kasmai'),
       ('ABLATIVE','kasmat'),('GENITIVE','kasya'),('LOCATIVE','kasmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2b. kim masculine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','kau'),('ACCUSATIVE','kau'),('INSTRUMENTAL','kabhyam'),('DATIVE','kabhyam'),
       ('ABLATIVE','kabhyam'),('GENITIVE','kayoh'),('LOCATIVE','kayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2c. kim masculine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ke'),('ACCUSATIVE','kan'),('INSTRUMENTAL','kaih'),('DATIVE','kebhyah'),
       ('ABLATIVE','kebhyah'),('GENITIVE','kesam'),('LOCATIVE','kesu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2d. kim feminine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ka'),('ACCUSATIVE','kam'),('INSTRUMENTAL','kaya'),('DATIVE','kasyai'),
       ('ABLATIVE','kasyah'),('GENITIVE','kasyah'),('LOCATIVE','kasyam'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2e. kim feminine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ke'),('ACCUSATIVE','ke'),('INSTRUMENTAL','kabhyam'),('DATIVE','kabhyam'),
       ('ABLATIVE','kabhyam'),('GENITIVE','kayoh'),('LOCATIVE','kayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2f. kim feminine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','kah'),('ACCUSATIVE','kah'),('INSTRUMENTAL','kabhih'),('DATIVE','kabhyah'),
       ('ABLATIVE','kabhyah'),('GENITIVE','kasam'),('LOCATIVE','kasu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2g. kim neuter singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','kim'),('ACCUSATIVE','kim'),('INSTRUMENTAL','kena'),('DATIVE','kasmai'),
       ('ABLATIVE','kasmat'),('GENITIVE','kasya'),('LOCATIVE','kasmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2h. kim neuter dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ke'),('ACCUSATIVE','ke'),('INSTRUMENTAL','kabhyam'),('DATIVE','kabhyam'),
       ('ABLATIVE','kabhyam'),('GENITIVE','kayoh'),('LOCATIVE','kayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2i. kim neuter plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'kim-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','kani'),('ACCUSATIVE','kani'),('INSTRUMENTAL','kaih'),('DATIVE','kebhyah'),
       ('ABLATIVE','kebhyah'),('GENITIVE','kesam'),('LOCATIVE','kesu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;
