-- V9: Declension-paradigm mirror for suppletive (pronoun) stems.
-- Curriculum now serves the v2 declension-paradigm page itself (content-service is
-- removed from this flow). Regular noun paradigms are drawn at runtime from the
-- generated quest_item pool; suppletive pronouns (aham/tvam/tad/etad/idam/kim/yad/
-- atman) cannot be composed from endings and are copied here verbatim from
-- content.declension_stems/declension_forms. Both live in the same physical
-- PostgreSQL instance, so the copy is a direct SELECT (see ADR-008).

-- ----------------------------------------------------------------------------
-- DeclensionStem (mirror)
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.declension_stem (
    id                UUID PRIMARY KEY,
    stem_iast         VARCHAR(120) NOT NULL,
    stem_devanagari   VARCHAR(120),
    translation_ru    VARCHAR(200),
    translation_en    VARCHAR(200),
    vowel_type        VARCHAR(40)  NOT NULL,
    gender            VARCHAR(40)  NOT NULL,
    CONSTRAINT uq_paradigm_stem_iast UNIQUE (stem_iast)
);

COMMENT ON TABLE curriculum.declension_stem IS
    'Suppletive declension stem (mirror of content.declension_stems for PRON_* shaped paradigms).';

-- ----------------------------------------------------------------------------
-- DeclensionForm (mirror)
-- ----------------------------------------------------------------------------

CREATE TABLE curriculum.declension_form (
    declension_stem_id UUID        NOT NULL REFERENCES curriculum.declension_stem (id) ON DELETE CASCADE,
    case_type          VARCHAR(40) NOT NULL,
    number_type        VARCHAR(40) NOT NULL,
    form_iast          VARCHAR(120),
    form_devanagari    VARCHAR(120),
    CONSTRAINT pk_declension_form PRIMARY KEY (declension_stem_id, case_type, number_type)
);

COMMENT ON TABLE curriculum.declension_form IS
    'Suppletive pronoun paradigm cell, attached to curriculum.declension_stem.';

-- ----------------------------------------------------------------------------
-- Copy pronoun stems + forms from content (idempotent)
-- ----------------------------------------------------------------------------

INSERT INTO curriculum.declension_stem (id, stem_iast, stem_devanagari, translation_ru, translation_en, vowel_type, gender)
SELECT s.id, s.stem_iast, s.stem_devanagari, s.translation_ru, s.translation_en,
       s.vowel_type::text, s.gender::text
FROM content.declension_stems s
WHERE s.vowel_type::text LIKE 'PRON_%'
ORDER BY s.id
ON CONFLICT (id) DO NOTHING;

INSERT INTO curriculum.declension_form (declension_stem_id, case_type, number_type, form_iast, form_devanagari)
SELECT f.declension_stem_id, f.case_type::text, f.number_type::text, f.form_iast, f.form_devanagari
FROM content.declension_forms f
JOIN curriculum.declension_stem cs ON cs.id = f.declension_stem_id
ON CONFLICT (declension_stem_id, case_type, number_type) DO NOTHING;