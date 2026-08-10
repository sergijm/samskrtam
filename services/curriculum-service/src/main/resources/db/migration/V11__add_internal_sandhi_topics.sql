-- V3: Internal sandhi topics (L1), mirroring external sandhi structure.
INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order) VALUES
    (gen_random_uuid(), 'sandhi-vowels-internal', 'Sandhi: внутренние гласные', 'Sandhi: internal vowel sandhi', 'L1', false, 12),
    (gen_random_uuid(), 'sandhi-consonants-internal', 'Sandhi: внутренние согласные', 'Sandhi: internal consonant sandhi', 'L1', false, 13);

INSERT INTO curriculum.topic_prerequisite (topic_id, prerequisite_topic_id, strength)
SELECT t.id, p.id, 'RECOMMENDED'
FROM (VALUES
    ('sandhi-vowels-internal', 'sandhi-vowels-external'),
    ('sandhi-consonants-internal', 'sandhi-vowels-internal')
) AS e(topic_code, prerequisite_code)
JOIN curriculum.topic t ON t.code = e.topic_code
JOIN curriculum.topic p ON p.code = e.prerequisite_code;