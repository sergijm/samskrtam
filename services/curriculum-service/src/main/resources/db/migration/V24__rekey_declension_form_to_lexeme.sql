-- V24: Paradigms are now keyed by lexeme, the stem mirror is dropped.
--
-- The declension-paradigm page and the batch generator no longer compose
-- paradigms on the fly; both read word forms from curriculum.declension_form,
-- now keyed by curriculum.lexeme.id (previously by curriculum.declension_stem.id).
--
-- 1. Drop the form table first: it references declension_stem via FK
--    (declension_form_declension_stem_id_fkey), so the stem mirror cannot be
--    dropped before it. Existing rows referenced the dropped stem mirror and
--    are discarded; the external paradigm pipeline fills the table back per
--    lexeme.
DROP TABLE IF EXISTS curriculum.declension_form;

-- 2. Drop the stem mirror (V9) — suppletive pronoun lexemes live in
--    curriculum.lexeme like any other lexeme; vowel_type is derived from the
--    morphology class, not stored.
DROP TABLE IF EXISTS curriculum.declension_stem;

-- 3. Rebuild declension_form keyed by lexeme.
CREATE TABLE curriculum.declension_form (
    lexeme_id       UUID        NOT NULL REFERENCES curriculum.lexeme (id),
    case_type       VARCHAR(40) NOT NULL,
    number_type     VARCHAR(40) NOT NULL,
    form_iast       VARCHAR(120),
    form_devanagari VARCHAR(120),
    CONSTRAINT pk_declension_form PRIMARY KEY (lexeme_id, case_type, number_type)
);

COMMENT ON TABLE curriculum.declension_form IS
    'Paradigm cell (case+number -> form) of a lexeme, keyed by curriculum.lexeme.id.';
