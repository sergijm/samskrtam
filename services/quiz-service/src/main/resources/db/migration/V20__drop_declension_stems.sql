-- V20: Drop quiz.declension_stems.
--
-- Created by V2 as a mirror of content.declension_stems. The mirror is dead:
-- nothing reads it (the paradigm page and the batch generator now use
-- curriculum.declension_form, keyed by lexeme). session_questions stem columns
-- are left in place.
DROP TABLE IF EXISTS "quiz"."declension_stems";
