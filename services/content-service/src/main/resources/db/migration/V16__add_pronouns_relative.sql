-- V16: pronouns-relative lesson + yad stems and forms (ADR-008 step 4/5)

-- ============================================================
-- 1. Lesson: pronouns-relative
-- ============================================================
INSERT INTO CONTENT.lesson (id, slug, title_ru, title_en, description_ru, description_en, lesson_type, difficulty, questions_per_session)
VALUES
    ('20000000-0000-0000-0000-00000000000f', 'pronouns-relative',
     'Местоимения: относительные',
     'Pronouns: relative',
     'Квиз по склонению относительного местоимения yad (который).',
     'Quiz on declension of the relative pronoun yad (which, who).',
     'DECLENSIONS', 'INTERMEDIATE', 10)
ON CONFLICT (slug) DO NOTHING;

-- ============================================================
-- 2. Stems: yad (3 genders)
-- ============================================================
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('yad-m', 'PRON_YAD', 'MASCULINE') ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('yad-f', 'PRON_YAD', 'FEMININE')  ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('yad-n', 'PRON_YAD', 'NEUTER')    ON CONFLICT (stem_iast) DO NOTHING;

-- 2a. yad masculine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','yah'),('ACCUSATIVE','yam'),('INSTRUMENTAL','yena'),('DATIVE','yasmai'),
       ('ABLATIVE','yasmat'),('GENITIVE','yasya'),('LOCATIVE','yasmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2b. yad masculine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','yau'),('ACCUSATIVE','yau'),('INSTRUMENTAL','yabhyam'),('DATIVE','yabhyam'),
       ('ABLATIVE','yabhyam'),('GENITIVE','yayoh'),('LOCATIVE','yayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2c. yad masculine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ye'),('ACCUSATIVE','yan'),('INSTRUMENTAL','yaih'),('DATIVE','yebhyah'),
       ('ABLATIVE','yebhyah'),('GENITIVE','yesam'),('LOCATIVE','yesu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2d. yad feminine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ya'),('ACCUSATIVE','yam'),('INSTRUMENTAL','yaya'),('DATIVE','yasyai'),
       ('ABLATIVE','yasyah'),('GENITIVE','yasyah'),('LOCATIVE','yasyam'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2e. yad feminine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ye'),('ACCUSATIVE','ye'),('INSTRUMENTAL','yabhyam'),('DATIVE','yabhyam'),
       ('ABLATIVE','yabhyam'),('GENITIVE','yayoh'),('LOCATIVE','yayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2f. yad feminine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','yah'),('ACCUSATIVE','yah'),('INSTRUMENTAL','yabhih'),('DATIVE','yabhyah'),
       ('ABLATIVE','yabhyah'),('GENITIVE','yasam'),('LOCATIVE','yasu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2g. yad neuter singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','yat'),('ACCUSATIVE','yat'),('INSTRUMENTAL','yena'),('DATIVE','yasmai'),
       ('ABLATIVE','yasmat'),('GENITIVE','yasya'),('LOCATIVE','yasmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2h. yad neuter dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ye'),('ACCUSATIVE','ye'),('INSTRUMENTAL','yabhyam'),('DATIVE','yabhyam'),
       ('ABLATIVE','yabhyam'),('GENITIVE','yayoh'),('LOCATIVE','yayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2i. yad neuter plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'yad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','yani'),('ACCUSATIVE','yani'),('INSTRUMENTAL','yaih'),('DATIVE','yebhyah'),
       ('ABLATIVE','yebhyah'),('GENITIVE','yesam'),('LOCATIVE','yesu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;
