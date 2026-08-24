-- V42: reference lesson showing the lingua.case_endings table.
--
-- A GRAMMAR topic at learning level L2. It has no quiz items of its own —
-- the lesson page renders the case_endings reference table instead.

INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order, domain, domain_type)
VALUES (gen_random_uuid(), 'case-endings', 'Падежные окончания', 'Case endings', 'L2', false, NULL, 'GRAMMAR', 'GRAMMAR');
