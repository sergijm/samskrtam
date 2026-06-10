-- V3__add_generated_questions_and_generated_quiz_data_tables.sql

-- Create generated_quiz_data table
CREATE TABLE content.generated_quiz_data
(
    id                      UUID PRIMARY KEY,
    quiz_id                 UUID                        NOT NULL,
    user_locale             VARCHAR(10)                 NOT NULL,
    generated_at            TIMESTAMP WITH TIME ZONE    NOT NULL,
    vocabulary_words_json   TEXT,
    CONSTRAINT fk_generated_quiz_data_quiz_id FOREIGN KEY (quiz_id) REFERENCES content.quizzes (id)
);

-- Create generated_questions table
CREATE TABLE content.generated_questions
(
    id                          UUID PRIMARY KEY,
    generated_quiz_data_id      UUID                        NOT NULL,
    quiz_id                     UUID                        NOT NULL,
    text                        TEXT,
    explanation_ru              TEXT,
    explanation_en              TEXT,
    declension_stem_id          UUID,
    target_case                 VARCHAR(255),
    target_number               VARCHAR(255),
    correct_form_iast           VARCHAR(255),
    correct_form_devanagari     VARCHAR(255),
    vocabulary_word_id          UUID,
    question_source_language    VARCHAR(255),
    question_target_language    VARCHAR(255),
    correct_translation_ru      TEXT,
    correct_translation_en      TEXT,
    user_locale                 VARCHAR(10),
    CONSTRAINT fk_generated_questions_generated_quiz_data_id FOREIGN KEY (generated_quiz_data_id) REFERENCES content.generated_quiz_data (id),
    CONSTRAINT fk_generated_questions_quiz_id FOREIGN KEY (quiz_id) REFERENCES content.quizzes (id),
    CONSTRAINT fk_generated_questions_declension_stem_id FOREIGN KEY (declension_stem_id) REFERENCES content.declension_stems (id),
    CONSTRAINT fk_generated_questions_vocabulary_word_id FOREIGN KEY (vocabulary_word_id) REFERENCES content.vocabulary_words (id)
);
