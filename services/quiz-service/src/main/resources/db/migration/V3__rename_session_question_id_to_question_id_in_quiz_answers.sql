-- V3__rename_session_question_id_to_question_id_in_quiz_answers.sql

ALTER TABLE quiz.quiz_answers
    RENAME COLUMN session_question_id TO question_id;
