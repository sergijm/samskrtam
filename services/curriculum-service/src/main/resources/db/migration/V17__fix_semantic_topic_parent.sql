-- V17: fix semantic_topic parent_id — the V5 seed inserted all rows with
-- NULL parent_id because its INSERT ... SELECT self-referenced the table being
-- inserted (the SELECT does not see its own new rows). This migration
-- reconstructs the parent links for the 40 leaves.

UPDATE curriculum.semantic_topic st
SET parent_id = p.id
FROM (VALUES
    ('nature-animals', 'nature'),
    ('nature-plants', 'nature'),
    ('nature-landscape', 'nature'),
    ('nature-water', 'nature'),
    ('nature-weather-sky', 'nature'),
    ('nature-agriculture', 'nature'),
    ('people-family', 'people-body'),
    ('people-body-parts', 'people-body'),
    ('people-occupations', 'people-body'),
    ('people-social-relations', 'people-body'),
    ('people-character', 'people-body'),
    ('people-royalty-hierarchy', 'people-body'),
    ('everyday-food-drink', 'everyday-life'),
    ('everyday-house-dwelling', 'everyday-life'),
    ('everyday-clothing-ornament', 'everyday-life'),
    ('everyday-travel-vehicles', 'everyday-life'),
    ('everyday-objects-tools', 'everyday-life'),
    ('everyday-materials', 'everyday-life'),
    ('movement-motion-verbs', 'movement-action'),
    ('movement-physical-action', 'movement-action'),
    ('movement-rest-stillness', 'movement-action'),
    ('speech-acts', 'speech-communication'),
    ('speech-naming-address', 'speech-communication'),
    ('speech-question-answer', 'speech-communication'),
    ('cognition-senses', 'perception-cognition'),
    ('cognition-thought-memory', 'perception-cognition'),
    ('cognition-knowledge-learning', 'perception-cognition'),
    ('emotion-positive', 'emotion-character'),
    ('emotion-negative', 'emotion-character'),
    ('emotion-desire-will', 'emotion-character'),
    ('abstract-time', 'abstract'),
    ('abstract-quantity-number', 'abstract'),
    ('abstract-space-direction', 'abstract'),
    ('abstract-cause-purpose', 'abstract'),
    ('abstract-comparison', 'abstract'),
    ('society-ritual-worship', 'society-ritual'),
    ('society-ethics-duty', 'society-ritual'),
    ('society-governance-law', 'society-ritual'),
    ('society-war-conflict', 'society-ritual'),
    ('society-philosophy-liberation', 'society-ritual')
) AS v(code, parent_code)
JOIN curriculum.semantic_topic p ON p.code = v.parent_code
WHERE st.code = v.code;