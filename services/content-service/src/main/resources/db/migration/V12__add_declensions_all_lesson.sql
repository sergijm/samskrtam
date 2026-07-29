-- V12: Add virtual lesson "declensions-all" for cross-lesson quiz
-- See: docs/services/quiz-service/quiz-declension.md §5.2

INSERT INTO CONTENT.lesson (id, slug, title_ru, title_en, description_ru, description_en, lesson_type, difficulty, questions_per_session)
VALUES
    ('20000000-0000-0000-0000-00000000000b', 'declensions-all',
     'Все основы',
     'All stems',
     'Универсальный квиз по склонению всех гласных основ вперемешку.',
     'Universal quiz mixing declension of all vowel stems together.',
     'DECLENSIONS', 'INTERMEDIATE', 10)
ON CONFLICT (slug) DO NOTHING;
