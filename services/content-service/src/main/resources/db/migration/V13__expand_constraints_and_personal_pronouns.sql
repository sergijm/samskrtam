-- V13: Expand ck_vowel_type with PRON_* values, add UNSPECIFIED to ck_gender,
--       insert pronouns-personal lesson + aham/tvam stems and forms (ADR-008 step 1/5)

-- ============================================================
-- 1. Expand vowel_type check constraint (7 → 15 values)
--    Includes PRON_REFLEXIVE for atman (pronouns-reflexive lesson, V17)
-- ============================================================
ALTER TABLE content.declension_stems DROP CONSTRAINT IF EXISTS ck_vowel_type;

ALTER TABLE content.declension_stems ADD CONSTRAINT ck_vowel_type
    CHECK (vowel_type::text = ANY (ARRAY[
        'A_STEM', 'AA_STEM', 'I_STEM', 'II_STEM', 'U_STEM', 'UU_STEM', 'R_STEM',
        'PRON_AHAM', 'PRON_TVAM', 'PRON_TAD', 'PRON_ETAD', 'PRON_IDAM',
        'PRON_KIM', 'PRON_YAD', 'PRON_REFLEXIVE'
    ]));

-- ============================================================
-- 2. Add UNSPECIFIED to ck_gender on declension_stems
-- ============================================================
ALTER TABLE content.declension_stems DROP CONSTRAINT IF EXISTS ck_gender;

ALTER TABLE content.declension_stems ADD CONSTRAINT ck_gender
    CHECK (gender::text = ANY (ARRAY[
        'MASCULINE', 'FEMININE', 'NEUTER', 'UNKNOWN', 'UNSPECIFIED'
    ]));

-- ============================================================
-- 3. Lesson: pronouns-personal
-- ============================================================
INSERT INTO CONTENT.lesson (id, slug, title_ru, title_en, description_ru, description_en, lesson_type, difficulty, questions_per_session)
VALUES
    ('20000000-0000-0000-0000-00000000000c', 'pronouns-personal',
     'Местоимения: личные',
     'Pronouns: personal',
     'Квиз по склонению личных местоимений санскрита: aham (я) и tvam (ты).',
     'Quiz on declension of Sanskrit personal pronouns: aham (I) and tvam (you).',
     'DECLENSIONS', 'BEGINNER', 10)
ON CONFLICT (slug) DO NOTHING;

-- ============================================================
-- 4. Stem: aham (personal, I)
-- ============================================================
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender)
VALUES ('aham', 'PRON_AHAM', 'UNSPECIFIED')
ON CONFLICT (stem_iast) DO NOTHING;

-- 4a. aham singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'aham') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'aham'),
       ('ACCUSATIVE',  'mām'),
       ('INSTRUMENTAL','mayā'),
       ('DATIVE',      'mahyam'),
       ('ABLATIVE',    'mat'),
       ('GENITIVE',    'mama'),
       ('LOCATIVE',    'mayi'),
       ('VOCATIVE',    'aham')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;

-- 4b. aham dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'aham') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'āvām'),
       ('ACCUSATIVE',  'āvām'),
       ('INSTRUMENTAL','āvābhyām'),
       ('DATIVE',      'āvābhyām'),
       ('ABLATIVE',    'āvābhyām'),
       ('GENITIVE',    'āvayoḥ'),
       ('LOCATIVE',    'āvayoḥ'),
       ('VOCATIVE',    'āvām')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;

-- 4c. aham plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'aham') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'vayam'),
       ('ACCUSATIVE',  'asmān'),
       ('INSTRUMENTAL','asmābhiḥ'),
       ('DATIVE',      'asmabhyam'),
       ('ABLATIVE',    'asmat'),
       ('GENITIVE',    'asmākam'),
       ('LOCATIVE',    'asmāsu'),
       ('VOCATIVE',    'vayam')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;

-- ============================================================
-- 5. Stem: tvam (personal, you)
-- ============================================================
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender)
VALUES ('tvam', 'PRON_TVAM', 'UNSPECIFIED')
ON CONFLICT (stem_iast) DO NOTHING;

-- 5a. tvam singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tvam') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'tvam'),
       ('ACCUSATIVE',  'tvām'),
       ('INSTRUMENTAL','tvayā'),
       ('DATIVE',      'tubhyam'),
       ('ABLATIVE',    'tvat'),
       ('GENITIVE',    'tava'),
       ('LOCATIVE',    'tvayi'),
       ('VOCATIVE',    'tvam')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;

-- 5b. tvam dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tvam') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'yuvām'),
       ('ACCUSATIVE',  'yuvām'),
       ('INSTRUMENTAL','yuvābhyām'),
       ('DATIVE',      'yuvābhyām'),
       ('ABLATIVE',    'yuvābhyām'),
       ('GENITIVE',    'yuvayoḥ'),
       ('LOCATIVE',    'yuvayoḥ'),
       ('VOCATIVE',    'yuvām')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;

-- 5c. tvam plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'tvam') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'yūyam'),
       ('ACCUSATIVE',  'yuṣmān'),
       ('INSTRUMENTAL','yuṣmābhiḥ'),
       ('DATIVE',      'yuṣmabhyam'),
       ('ABLATIVE',    'yuṣmat'),
       ('GENITIVE',    'yuṣmākam'),
       ('LOCATIVE',    'yuṣmāsu'),
       ('VOCATIVE',    'yūyam')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;
