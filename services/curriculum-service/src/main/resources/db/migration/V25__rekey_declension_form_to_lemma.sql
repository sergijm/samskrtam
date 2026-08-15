-- V25: declension_form rekeyed from lexeme_id to (lemma_iast, vowel_type).
--
-- A paradigm now belongs to a lemma + declension class, not to a lexeme
-- (meaning). The declension_form row key becomes (lemma_iast, vowel_type,
-- case_type, number_type): the class is the vowelType of the paradigm the
-- form belongs to, so the same lemma can later carry paradigms of several
-- classes. Existing rows (if any) referenced the lexeme-keyed schema of V24
-- and are discarded; the external paradigm pipeline fills the table back.
DROP TABLE IF EXISTS curriculum.declension_form;

CREATE TABLE curriculum.declension_form (
    lemma_iast      VARCHAR(120) NOT NULL,
    vowel_type      VARCHAR(40)  NOT NULL,
    case_type       VARCHAR(40)  NOT NULL,
    number_type     VARCHAR(40)  NOT NULL,
    form_iast       VARCHAR(120),
    form_devanagari VARCHAR(120),
    CONSTRAINT pk_declension_form PRIMARY KEY (lemma_iast, vowel_type, case_type, number_type)
);

CREATE INDEX idx_declension_form_vowel_type ON curriculum.declension_form (vowel_type);

COMMENT ON TABLE curriculum.declension_form IS
    'Paradigm cell (case+number -> form) of a lemma within one declension class, keyed by (lemma_iast, vowel_type).';
