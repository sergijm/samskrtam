CREATE TABLE sangraha.verse_words (
    id                  UUID NOT NULL DEFAULT gen_random_uuid(),
    verse_id            UUID NOT NULL REFERENCES sangraha.verses(id),
    position            INT  NOT NULL,
    surface_iast        VARCHAR(200) NOT NULL,
    surface_devanagari  VARCHAR(200) NOT NULL,
    lemma_iast          VARCHAR(200) NOT NULL,
    stem                VARCHAR(200) NOT NULL,
    root                VARCHAR(200),
    pos                 VARCHAR(30),
    gender              VARCHAR(20),
    case_type           VARCHAR(20),
    number_type         VARCHAR(20),
    person              VARCHAR(20),
    tense               VARCHAR(20),
    mood                VARCHAR(20),
    voice               VARCHAR(20),
    gloss_ru            VARCHAR(500) NOT NULL,
    gloss_en            VARCHAR(500) NOT NULL,
    CONSTRAINT pk_verse_words PRIMARY KEY (id)
);

CREATE INDEX idx_verse_words_verse_id ON sangraha.verse_words (verse_id);