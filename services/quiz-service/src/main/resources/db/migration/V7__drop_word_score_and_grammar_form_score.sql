-- =============================================
-- V7: Drop old word_score and grammar_form_score tables
-- =============================================
-- These tables are replaced by quiz.quiz_item_score (V6, ADR-007).
-- Data is not migrated because there is no data (per spec).
-- 
-- See: docs/adr.md (ADR-007), docs/quizzes/quiz-generator-spec.md §2.2
-- =============================================

DROP TABLE IF EXISTS quiz.word_score CASCADE;
DROP TABLE IF EXISTS quiz.grammar_form_score CASCADE;
