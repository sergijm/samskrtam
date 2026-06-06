DROP TABLE IF EXISTS content.vocabulary_words;
CREATE TABLE content.vocabulary_words
(
    id                UUID PRIMARY KEY,
    word_iast         VARCHAR(255) NOT NULL,
    word_devanagari   VARCHAR(255) NOT NULL,
    translation_en    VARCHAR(500) NOT NULL,
    translation_ru    VARCHAR(500) NOT NULL,
    gender            VARCHAR(20)  NOT NULL, -- MASCULINE, FEMININE, NEUTER
    stem              VARCHAR(255) NOT NULL,
    root              VARCHAR(255),
    dictionary_entry  TEXT,
    created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vocabulary_words_word_iast ON content.vocabulary_words (word_iast);
CREATE INDEX idx_vocabulary_words_stem ON content.vocabulary_words (stem);
CREATE INDEX idx_vocabulary_words_gender ON content.vocabulary_words (gender);
