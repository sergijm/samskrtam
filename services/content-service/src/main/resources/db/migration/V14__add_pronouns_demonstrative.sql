-- V14: pronouns-demonstrative lesson + tad/etad/idam stems and forms (ADR-008 step 2/5)

-- ============================================================
-- 1. Lesson: pronouns-demonstrative
-- ============================================================
INSERT INTO CONTENT.lesson (id, slug, title_ru, title_en, description_ru, description_en, lesson_type, difficulty, questions_per_session)
VALUES
    ('20000000-0000-0000-0000-00000000000d', 'pronouns-demonstrative',
     'Местоимения: указательные',
     'Pronouns: demonstrative',
     'Квиз по склонению указательных местоимений: tad (тот), etad (этот), idam (это).',
     'Quiz on declension of demonstrative pronouns: tad (that), etad (this), idam (this).',
     'DECLENSIONS', 'BEGINNER', 10)
ON CONFLICT (slug) DO NOTHING;

-- ============================================================
-- 2. Stems: tad (3 genders)
-- ============================================================
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('tad-m', 'PRON_TAD', 'MASCULINE') ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('tad-f', 'PRON_TAD', 'FEMININE')  ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('tad-n', 'PRON_TAD', 'NEUTER')    ON CONFLICT (stem_iast) DO NOTHING;

-- 2a. tad masculine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','sah'),('ACCUSATIVE','tam'),('INSTRUMENTAL','tena'),('DATIVE','tasmai'),
       ('ABLATIVE','tasmat'),('GENITIVE','tasya'),('LOCATIVE','tasmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2b. tad masculine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','tau'),('ACCUSATIVE','tau'),('INSTRUMENTAL','tabhyam'),('DATIVE','tabhyam'),
       ('ABLATIVE','tabhyam'),('GENITIVE','tayoh'),('LOCATIVE','tayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2c. tad masculine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','te'),('ACCUSATIVE','tan'),('INSTRUMENTAL','taih'),('DATIVE','tebhyah'),
       ('ABLATIVE','tebhyah'),('GENITIVE','tesam'),('LOCATIVE','tesu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2d. tad feminine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','sa'),('ACCUSATIVE','tam'),('INSTRUMENTAL','taya'),('DATIVE','tasyai'),
       ('ABLATIVE','tasyah'),('GENITIVE','tasyah'),('LOCATIVE','tasyam'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2e. tad feminine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','te'),('ACCUSATIVE','te'),('INSTRUMENTAL','tabhyam'),('DATIVE','tabhyam'),
       ('ABLATIVE','tabhyam'),('GENITIVE','tayoh'),('LOCATIVE','tayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2f. tad feminine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','tah'),('ACCUSATIVE','tah'),('INSTRUMENTAL','tabhih'),('DATIVE','tabhyah'),
       ('ABLATIVE','tabhyah'),('GENITIVE','tasam'),('LOCATIVE','tasu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2g. tad neuter singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','tat'),('ACCUSATIVE','tat'),('INSTRUMENTAL','tena'),('DATIVE','tasmai'),
       ('ABLATIVE','tasmat'),('GENITIVE','tasya'),('LOCATIVE','tasmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2h. tad neuter dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','te'),('ACCUSATIVE','te'),('INSTRUMENTAL','tabhyam'),('DATIVE','tabhyam'),
       ('ABLATIVE','tabhyam'),('GENITIVE','tayoh'),('LOCATIVE','tayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 2i. tad neuter plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','tani'),('ACCUSATIVE','tani'),('INSTRUMENTAL','taih'),('DATIVE','tebhyah'),
       ('ABLATIVE','tebhyah'),('GENITIVE','tesam'),('LOCATIVE','tesu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- ============================================================
-- 3. Stems: etad (3 genders)
-- ============================================================
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('etad-m', 'PRON_ETAD', 'MASCULINE') ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('etad-f', 'PRON_ETAD', 'FEMININE')  ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('etad-n', 'PRON_ETAD', 'NEUTER')    ON CONFLICT (stem_iast) DO NOTHING;

-- 3a. etad masculine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','esah'),('ACCUSATIVE','etam'),('INSTRUMENTAL','etena'),('DATIVE','etasmai'),
       ('ABLATIVE','etasmat'),('GENITIVE','etasya'),('LOCATIVE','etasmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 3b. etad masculine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','etau'),('ACCUSATIVE','etau'),('INSTRUMENTAL','etabhyam'),('DATIVE','etabhyam'),
       ('ABLATIVE','etabhyam'),('GENITIVE','etayoh'),('LOCATIVE','etayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 3c. etad masculine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ete'),('ACCUSATIVE','etan'),('INSTRUMENTAL','etaih'),('DATIVE','etebhyah'),
       ('ABLATIVE','etebhyah'),('GENITIVE','etesam'),('LOCATIVE','etesu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 3d. etad feminine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','esa'),('ACCUSATIVE','etam'),('INSTRUMENTAL','etaya'),('DATIVE','etasyai'),
       ('ABLATIVE','etasyah'),('GENITIVE','etasyah'),('LOCATIVE','etasyam'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 3e. etad feminine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ete'),('ACCUSATIVE','ete'),('INSTRUMENTAL','etabhyam'),('DATIVE','etabhyam'),
       ('ABLATIVE','etabhyam'),('GENITIVE','etayoh'),('LOCATIVE','etayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 3f. etad feminine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','etah'),('ACCUSATIVE','etah'),('INSTRUMENTAL','etabhih'),('DATIVE','etabhyah'),
       ('ABLATIVE','etabhyah'),('GENITIVE','etasam'),('LOCATIVE','etasu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 3g. etad neuter singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','etat'),('ACCUSATIVE','etat'),('INSTRUMENTAL','etena'),('DATIVE','etasmai'),
       ('ABLATIVE','etasmat'),('GENITIVE','etasya'),('LOCATIVE','etasmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 3h. etad neuter dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ete'),('ACCUSATIVE','ete'),('INSTRUMENTAL','etabhyam'),('DATIVE','etabhyam'),
       ('ABLATIVE','etabhyam'),('GENITIVE','etayoh'),('LOCATIVE','etayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 3i. etad neuter plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'etad-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','etani'),('ACCUSATIVE','etani'),('INSTRUMENTAL','etaih'),('DATIVE','etebhyah'),
       ('ABLATIVE','etebhyah'),('GENITIVE','etesam'),('LOCATIVE','etesu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- ============================================================
-- 4. Stems: idam (3 genders)
-- ============================================================
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('idam-m', 'PRON_IDAM', 'MASCULINE') ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('idam-f', 'PRON_IDAM', 'FEMININE')  ON CONFLICT (stem_iast) DO NOTHING;
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender) VALUES ('idam-n', 'PRON_IDAM', 'NEUTER')    ON CONFLICT (stem_iast) DO NOTHING;

-- 4a. idam masculine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ayam'),('ACCUSATIVE','imam'),('INSTRUMENTAL','anena'),('DATIVE','asmai'),
       ('ABLATIVE','asmat'),('GENITIVE','asya'),('LOCATIVE','asmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 4b. idam masculine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','imau'),('ACCUSATIVE','imau'),('INSTRUMENTAL','abhyam'),('DATIVE','abhyam'),
       ('ABLATIVE','abhyam'),('GENITIVE','anayoh'),('LOCATIVE','anayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 4c. idam masculine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-m') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ime'),('ACCUSATIVE','iman'),('INSTRUMENTAL','ebhih'),('DATIVE','ebhyah'),
       ('ABLATIVE','ebhyah'),('GENITIVE','esam'),('LOCATIVE','esu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 4d. idam feminine singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','iyam'),('ACCUSATIVE','imam'),('INSTRUMENTAL','anaya'),('DATIVE','asyai'),
       ('ABLATIVE','asyah'),('GENITIVE','asyah'),('LOCATIVE','asyam'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 4e. idam feminine dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ime'),('ACCUSATIVE','ime'),('INSTRUMENTAL','abhyam'),('DATIVE','abhyam'),
       ('ABLATIVE','abhyam'),('GENITIVE','anayoh'),('LOCATIVE','anayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 4f. idam feminine plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-f') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','imah'),('ACCUSATIVE','imah'),('INSTRUMENTAL','abhih'),('DATIVE','abhyah'),
       ('ABLATIVE','abhyah'),('GENITIVE','asam'),('LOCATIVE','asu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 4g. idam neuter singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','idam'),('ACCUSATIVE','idam'),('INSTRUMENTAL','anena'),('DATIVE','asmai'),
       ('ABLATIVE','asmat'),('GENITIVE','asya'),('LOCATIVE','asmin'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 4h. idam neuter dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','ime'),('ACCUSATIVE','ime'),('INSTRUMENTAL','abhyam'),('DATIVE','abhyam'),
       ('ABLATIVE','abhyam'),('GENITIVE','anayoh'),('LOCATIVE','anayoh'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;

-- 4i. idam neuter plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'idam-n') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE','imani'),('ACCUSATIVE','imani'),('INSTRUMENTAL','ebhih'),('DATIVE','ebhyah'),
       ('ABLATIVE','ebhyah'),('GENITIVE','esam'),('LOCATIVE','esu'),('VOCATIVE','')
   ) AS f(case_type,form_iast) ON CONFLICT DO NOTHING;
