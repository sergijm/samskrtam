-- V10: Merge -i/-u and -ī/-ū lessons into compound lessons
-- Rationale: -i and -u stems share identical declension patterns (same for -ī/-ū).
-- Pedagogically, they form a single lesson each.
--
-- Old slugs (deprecated, kept for backward compatibility):
--   declensions-i, declensions-u, declensions-ii, declensions-uu
-- New compound slugs:
--   declensions-i-u  (I_STEM + U_STEM)
--   declensions-ii-uu (II_STEM + UU_STEM)

-- Insert new compound lessons
INSERT INTO CONTENT.lesson (id, slug, title_ru, title_en, description_ru, description_en, lesson_type, difficulty, questions_per_session)
VALUES
    ('20000000-0000-0000-0000-000000000009', 'declensions-i-u',
     'Склонения: Основы на -i и -u',
     'Declensions: I-stems and U-stems',
     'Квиз по склонению существительных на -i и -u. Оба типа имеют идентичный паттерн словоизменения.',
     'Quiz on declension of nouns ending in -i and -u. Both types share an identical declension pattern.',
     'DECLENSIONS', 'INTERMEDIATE', 10),
    ('20000000-0000-0000-0000-00000000000a', 'declensions-ii-uu',
     'Склонения: Основы на -ī и -ū',
     'Declensions: Ī-stems and Ū-stems',
     'Квиз по склонению существительных на -ī и -ū. Оба типа имеют идентичный паттерн словоизменения.',
     'Quiz on declension of nouns ending in -ī and -ū. Both types share an identical declension pattern.',
     'DECLENSIONS', 'INTERMEDIATE', 10)
ON CONFLICT (slug) DO NOTHING;
