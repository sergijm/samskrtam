-- V17: pronouns-reflexive lesson + atman stem and forms (ADR-008 step 5/5)

-- ============================================================
-- 1. Lesson: pronouns-reflexive
-- ============================================================
INSERT INTO CONTENT.lesson (id, slug, title_ru, title_en, description_ru, description_en, lesson_type, difficulty, questions_per_session)
VALUES
    ('20000000-0000-0000-0000-000000000010', 'pronouns-reflexive',
     'Местоимения: возвратное',
     'Pronouns: reflexive',
     'Квиз по склонению возвратного местоимения ātman (себя).',
     'Quiz on declension of the reflexive pronoun atman (self).',
     'DECLENSIONS', 'BEGINNER', 10)
ON CONFLICT (slug) DO NOTHING;

-- ============================================================
-- 2. Stem: atman (reflexive, masculine, singular-only in practice)
-- ============================================================
INSERT INTO CONTENT.declension_stems (stem_iast, vowel_type, gender)
VALUES ('atman', 'PRON_REFLEXIVE', 'MASCULINE')
ON CONFLICT (stem_iast) DO NOTHING;

-- 2a. atman singular
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'SINGULAR', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'atman') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'ātmā'),
       ('ACCUSATIVE',  'ātmānam'),
       ('INSTRUMENTAL','ātmanā'),
       ('DATIVE',      'ātmane'),
       ('ABLATIVE',    'ātmanaḥ'),
       ('GENITIVE',    'ātmanaḥ'),
       ('LOCATIVE',    'ātmani'),
       ('VOCATIVE',    'ātman')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;

-- 2b. atman dual
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'DUAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'atman') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'ātmānau'),
       ('ACCUSATIVE',  'ātmānau'),
       ('INSTRUMENTAL','ātmabhyām'),
       ('DATIVE',      'ātmabhyām'),
       ('ABLATIVE',    'ātmabhyām'),
       ('GENITIVE',    'ātmanoḥ'),
       ('LOCATIVE',    'ātmanoḥ'),
       ('VOCATIVE',    'ātmānau')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;

-- 2c. atman plural
INSERT INTO CONTENT.declension_forms (declension_stem_id, case_type, number_type, form_iast)
SELECT ds.id, f.case_type, 'PLURAL', f.form_iast
FROM (SELECT id FROM content.declension_stems WHERE stem_iast = 'atman') ds
   CROSS JOIN (VALUES
       ('NOMINATIVE',  'ātmānaḥ'),
       ('ACCUSATIVE',  'ātmanaḥ'),
       ('INSTRUMENTAL','ātmabhiḥ'),
       ('DATIVE',      'ātmabhyaḥ'),
       ('ABLATIVE',    'ātmabhyaḥ'),
       ('GENITIVE',    'ātmanām'),
       ('LOCATIVE',    'ātmasu'),
       ('VOCATIVE',    'ātmānaḥ')
   ) AS f(case_type, form_iast)
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;
