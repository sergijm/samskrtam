-- V3__insert_initial_quizzes.sql
INSERT INTO content.quizzes (id, slug, title_ru, title_en, quiz_type, difficulty, questions_per_session, created_at) VALUES
('a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11', 'declensions-quiz-1', 'Квиз по склонениям 1', 'Declensions Quiz 1', 'DECLENSIONS', 'EASY', 10, NOW()),
('b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12', 'conjugations-quiz-1', 'Квиз по спряжениям 1', 'Conjugations Quiz 1', 'CONJUGATIONS', 'MEDIUM', 12, NOW()),
('c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a13', 'vocabulary-animals', 'Квиз по лексике: Животные', 'Vocabulary Quiz: Animals', 'VOCABULARY', 'EASY', 15, NOW());
