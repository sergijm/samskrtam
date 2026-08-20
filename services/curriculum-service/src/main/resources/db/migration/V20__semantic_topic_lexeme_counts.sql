-- V20: semantic_topic_lexeme_counts view — количество лексем в каждой семантической
-- группе.  Родительские группы агрегируют количество лексем всех своих потомков.

CREATE OR REPLACE VIEW curriculum.semantic_topic_lexeme_counts AS
WITH RECURSIVE tree AS (
    -- Якорь: каждый узел ссылается сам на себя (путь длины 1)
    SELECT st.id                                    AS root_id,
           st.id                                    AS node_id,
           COALESCE(dc.c, 0)                        AS direct_count
    FROM curriculum.semantic_topic st
    LEFT JOIN (
        SELECT semantic_topic_id, COUNT(*) AS c
        FROM curriculum.lexeme_semantic_topic
        GROUP BY semantic_topic_id
    ) dc ON dc.semantic_topic_id = st.id

    UNION ALL

    -- Рекурсия: спускаемся от родителя к потомку; root_id остаётся родителем
    SELECT t.root_id,
           child.id,
           COALESCE(dc2.c, 0)
    FROM tree t
    JOIN curriculum.semantic_topic child ON child.parent_id = t.node_id
    LEFT JOIN (
        SELECT semantic_topic_id, COUNT(*) AS c
        FROM curriculum.lexeme_semantic_topic
        GROUP BY semantic_topic_id
    ) dc2 ON dc2.semantic_topic_id = child.id
)
SELECT
    st.code,
    st.name_ru,
    st.name_en,
    st.parent_id,
    SUM(t.direct_count) AS lexeme_count
FROM tree t
JOIN curriculum.semantic_topic st ON st.id = t.root_id
GROUP BY st.id, st.code, st.name_ru, st.name_en, st.parent_id
ORDER BY st.code;