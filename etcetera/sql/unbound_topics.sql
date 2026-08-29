-- Топики без лексем по обоим механизмам привязки (lexical-curriculum.md §1):
--   1) классифицированные: тема -> semantic_class_topic -> lexeme_semantic_class
--   2) явные:             тема -> lexeme_lexical_topic (VERSE / неклассифицированные)
-- Кандидаты на ревизию: LEXICON/VERSE-уроки без наполнения.
-- Расшифровка domain: GRAMMAR (уроки грамматики — лексем не связывают),
-- LEXICON (лексические уроки), VERSE (уроки пачки стихов).

SELECT t.id,
       t.code,
       t.title_ru,
       t.title_en,
       t.domain,
       t.domain_type,
       t.learning_level,
       t.is_evergreen,
       count(DISTINCT lsc.lexeme_id) AS lexemes_via_classes,
       count(DISTINCT lt.lexeme_id)  AS lexemes_explicit
  FROM curriculum.topic t
  LEFT JOIN curriculum.semantic_class_topic sct ON sct.topic_id = t.id
  LEFT JOIN curriculum.lexeme_semantic_class lsc ON lsc.semantic_class_id = sct.semantic_class_id
  LEFT JOIN curriculum.lexeme_lexical_topic lt   ON lt.lexical_topic_id = t.id
 GROUP BY t.id, t.code, t.title_ru, t.title_en, t.domain, t.domain_type,
          t.learning_level, t.is_evergreen
HAVING count(DISTINCT lsc.lexeme_id) = 0 AND count(DISTINCT lt.lexeme_id) = 0
 ORDER BY t.domain, t.learning_level NULLS LAST, t.code;