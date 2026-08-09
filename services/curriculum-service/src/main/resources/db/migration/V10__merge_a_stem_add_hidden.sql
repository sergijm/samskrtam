-- =============================================
-- V10: Merge a-stem-masc + a-stem-neut -> a-stem, add hidden flag
-- =============================================
-- Combines masculine and neuter a-stem topics into a single 'a-stem' lesson.
-- Old topics are hidden (not shown in listings, but still accessible by direct code).

ALTER TABLE curriculum.topic
    ADD COLUMN IF NOT EXISTS hidden BOOLEAN NOT NULL DEFAULT false;

-- Create the merged topic
INSERT INTO curriculum.topic (id, code, title_ru, title_en, learning_level, is_evergreen, display_order, domain, target_item_count)
VALUES (gen_random_uuid(), 'a-stem', 'a-основы', 'a-stems', 'L1', false, 5, 'GRAMMAR', 24);

-- Hide old topics
UPDATE curriculum.topic SET hidden = true, display_order = NULL WHERE code IN ('a-stem-masc', 'a-stem-neut');

-- Move all quest items from old topics to new a-stem
UPDATE curriculum.quest_item qi
   SET topic_id = t_new.id
  FROM curriculum.topic t_old
  JOIN curriculum.topic t_new ON t_new.code = 'a-stem'
 WHERE qi.topic_id = t_old.id
   AND t_old.code IN ('a-stem-masc', 'a-stem-neut');

-- Update prerequisite edges: all edges through old topics now through a-stem
-- 1. Remove old edges referencing a-stem-masc / a-stem-neut
DELETE FROM curriculum.topic_prerequisite
 WHERE topic_id IN (SELECT id FROM curriculum.topic WHERE code IN ('a-stem-masc', 'a-stem-neut'))
    OR prerequisite_topic_id IN (SELECT id FROM curriculum.topic WHERE code IN ('a-stem-masc', 'a-stem-neut'));

-- 2. Add new edges through a-stem
INSERT INTO curriculum.topic_prerequisite (topic_id, prerequisite_topic_id, strength)
SELECT t.id, p.id, 'RECOMMENDED'
FROM (VALUES
    ('a-stem', 'stem-case-concept'),
    ('a-stem-fem', 'a-stem'),
    ('case-meanings-basic', 'a-stem'),
    ('personal-pronouns', 'a-stem'),
    ('i-u-stems', 'a-stem')
) AS e(topic_code, prerequisite_code)
JOIN curriculum.topic t ON t.code = e.topic_code
JOIN curriculum.topic p ON p.code = e.prerequisite_code;