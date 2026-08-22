-- ============================================================================
-- Schema for import of the Friš (Fríš / Zbavitel-Fríš) Sanskrit dictionary
-- Source format observed in fri_03.txt: see comments in import_frisch.py.
--
-- Grammatical categories are modelled as PostgreSQL ENUM types rather than
-- free text, matching the following fixed vocabularies:
--
--   PartOfSpeech     NOUN, VERB, ADJECTIVE, PRONOUN, ADVERB, PARTICLE,
--                     INDECLINABLE, NUMERAL, CONJUNCTION, INTERJECTION, OTHER
--   Gender            MASCULINE, FEMININE, NEUTER, UNSPECIFIED
--   GrammaticalCase   NOMINATIVE, ACCUSATIVE, INSTRUMENTAL, DATIVE, ABLATIVE,
--                     GENITIVE, LOCATIVE, VOCATIVE
--   Mood              INDICATIVE, OPTATIVE, IMPERATIVE, CONDITIONAL,
--                     BENEDICTIVE, INJUNCTIVE
--   NumberType        SINGULAR, DUAL, PLURAL
--   Person            FIRST, SECOND, THIRD
--   Tense             PRESENT, IMPERFECT, PERFECT, AORIST, FUTURE,
--                     PERIPHRASTIC_FUTURE, CONDITIONAL, BENEDICTIVE
--   Voice             ACTIVE, MIDDLE, PASSIVE
--   DerivationType    SIMPLE_INFLECTION, ABSOLUTIVE, PARTICIPLE, GERUNDIVE,
--                     INFINITIVE, CAUSATIVE, DESIDERATIVE, DENOMINATIVE,
--                     COMPOUND_VERB, OTHER
--   FormType          FINITE, INFINITIVE, ABSOLUTIVE, PARTICIPLE, GERUNDIVE,
--                     OTHER_NONFINITE, NOMINAL, ADJECTIVAL, PRONOMINAL,
--                     INDECLINABLE
--
-- The same label spellings are used as Python constants in import_frisch.py
-- (classes PartOfSpeech, Gender, GrammaticalCase, Mood, NumberType, Person,
-- Tense, Voice, DerivationType, FormType) so values round-trip unchanged.
--
-- The Frisch dictionary itself is terse (2-3 word grammatical tags), so most
-- rows will only populate a few of the available enum columns; the rest are
-- left NULL. raw_tag / raw_text / raw_headline columns are kept everywhere
-- for audit and manual re-annotation.
-- ============================================================================

CREATE SCHEMA IF NOT EXISTS frisch;
SET search_path TO frisch, public;

CREATE EXTENSION IF NOT EXISTS pg_trgm;   -- fuzzy/substring search on lemmas
CREATE EXTENSION IF NOT EXISTS unaccent;  -- diacritic-insensitive search

-- ----------------------------------------------------------------------------
-- Grammar enums
-- ----------------------------------------------------------------------------

CREATE TYPE frisch.part_of_speech AS ENUM (
    'NOUN', 'VERB', 'ADJECTIVE', 'PRONOUN', 'ADVERB', 'PARTICLE',
    'INDECLINABLE', 'NUMERAL', 'CONJUNCTION', 'INTERJECTION', 'OTHER'
    );

CREATE TYPE frisch.gender AS ENUM (
    'MASCULINE', 'FEMININE', 'NEUTER', 'UNSPECIFIED'
    );

CREATE TYPE frisch.grammatical_case AS ENUM (
    'NOMINATIVE', 'ACCUSATIVE', 'INSTRUMENTAL', 'DATIVE', 'ABLATIVE',
    'GENITIVE', 'LOCATIVE', 'VOCATIVE'
    );

CREATE TYPE frisch.mood AS ENUM (
    'INDICATIVE', 'OPTATIVE', 'IMPERATIVE', 'CONDITIONAL', 'BENEDICTIVE', 'INJUNCTIVE'
    );

CREATE TYPE frisch.number_type AS ENUM (
    'SINGULAR', 'DUAL', 'PLURAL'
    );

CREATE TYPE frisch.person AS ENUM (
    'FIRST', 'SECOND', 'THIRD'
    );

CREATE TYPE frisch.tense AS ENUM (
    'PRESENT', 'IMPERFECT', 'PERFECT', 'AORIST', 'FUTURE',
    'PERIPHRASTIC_FUTURE', 'CONDITIONAL', 'BENEDICTIVE'
    );

CREATE TYPE frisch.voice AS ENUM (
    'ACTIVE', 'MIDDLE', 'PASSIVE'
    );

CREATE TYPE frisch.derivation_type AS ENUM (
    'SIMPLE_INFLECTION', 'ABSOLUTIVE', 'PARTICIPLE', 'GERUNDIVE', 'INFINITIVE',
    'CAUSATIVE', 'DESIDERATIVE', 'DENOMINATIVE', 'COMPOUND_VERB', 'OTHER'
    );

CREATE TYPE frisch.form_type AS ENUM (
    'FINITE', 'INFINITIVE', 'ABSOLUTIVE', 'PARTICIPLE', 'GERUNDIVE',
    'OTHER_NONFINITE', 'NOMINAL', 'ADJECTIVAL', 'PRONOMINAL', 'INDECLINABLE'
    );

-- ----------------------------------------------------------------------------
-- Language reference table (not a grammar enum - open vocabulary)
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.language_code (
                                      code TEXT PRIMARY KEY,   -- 'cs', 'ru', 'en'
                                      name TEXT NOT NULL
);
INSERT INTO frisch.language_code VALUES ('cs', 'czech'), ('ru', 'russian'), ('en', 'english');

-- ----------------------------------------------------------------------------
-- Main entry table -- one row per <L> ... <LEND> block
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.dict_entry (
                                   entry_id            INTEGER PRIMARY KEY,          -- value of <L>
                                   page_no             SMALLINT NOT NULL,             -- value of <pc>
                                   sort_key1           TEXT NOT NULL,                  -- <k1>
                                   sort_key2           TEXT NOT NULL,                  -- <k2>

                                   homonym_index       SMALLINT,                       -- I/II/III... parsed to 1/2/3
                                   is_root             BOOLEAN NOT NULL DEFAULT FALSE, -- headword marked with √ / ѵ
                                   lemma_iast          TEXT NOT NULL,                   -- headword, IAST, diacritics kept
                                   lemma_ascii         TEXT NOT NULL,                   -- unaccented lower-case, for search

                                   is_related_form     BOOLEAN NOT NULL DEFAULT FALSE,  -- entry is a " +..." sub-entry
                                   parent_entry_id      INTEGER REFERENCES frisch.dict_entry(entry_id),
    -- FK to the headword this "+..." sub-entry (derived form) belongs to;
    -- resolved during load (= nearest preceding entry with is_related_form = FALSE)

                                   is_crossref_only    BOOLEAN NOT NULL DEFAULT FALSE,  -- e.g. "akārya v. akartavya"

                                   grammar_note         TEXT,        -- unclassified parenthetical remark, e.g. "(ved.)"
                                   raw_headline         TEXT NOT NULL,  -- untouched first content line, for audit/reparse
                                   created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_dict_entry_lemma_iast   ON frisch.dict_entry (lemma_iast);
CREATE INDEX idx_dict_entry_lemma_ascii  ON frisch.dict_entry (lemma_ascii);
CREATE INDEX idx_dict_entry_lemma_trgm   ON frisch.dict_entry USING gin (lemma_ascii gin_trgm_ops);
CREATE INDEX idx_dict_entry_sort_key1    ON frisch.dict_entry (sort_key1);
CREATE INDEX idx_dict_entry_page         ON frisch.dict_entry (page_no);
CREATE INDEX idx_dict_entry_parent       ON frisch.dict_entry (parent_entry_id);

-- ----------------------------------------------------------------------------
-- Part of speech -- an entry can carry several
-- (e.g. "katham adv., pcl. interrog." -> ADVERB + PARTICLE)
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.entry_pos (
                                  entry_id  INTEGER NOT NULL REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                                  pos       frisch.part_of_speech NOT NULL,
                                  qualifier TEXT NOT NULL DEFAULT '',   -- e.g. 'interrog.', 'emph.', 'encl.', 'preposition'
                                  PRIMARY KEY (entry_id, pos, qualifier)
);
CREATE INDEX idx_entry_pos_pos ON frisch.entry_pos (pos);

-- ----------------------------------------------------------------------------
-- Gender: both the primary gender of the headword (stem_suffix NULL) and
-- gender-conditioned stem variants, e.g. "deva (f. -ī)" -> (FEMININE, '-ī')
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.entry_gender (
                                     id          SERIAL PRIMARY KEY,
                                     entry_id    INTEGER NOT NULL REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                                     gender      frisch.gender NOT NULL,
                                     stem_suffix TEXT   -- NULL = this is the headword's own primary gender;
    -- non-NULL = alternate-gender stem suffix, e.g. '-ī', '-ā'
);
CREATE INDEX idx_entry_gender_entry ON frisch.entry_gender (entry_id);

-- ----------------------------------------------------------------------------
-- Verb conjugation class (Pāṇinian class I-X); not part of the given enum
-- set, kept as a plain bounded integer.
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.verb_class (
                                   entry_id    INTEGER PRIMARY KEY REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                                   conj_class  SMALLINT CHECK (conj_class BETWEEN 1 AND 10)
);

-- ----------------------------------------------------------------------------
-- Verb forms: principal parts of the paradigm belonging directly to an
-- entry (present indicative active/middle, infinitive, participle, perfect,
-- future, aorist, passive present, ...). Each row is one attested form.
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.verb_form (
                                  id           SERIAL PRIMARY KEY,
                                  entry_id     INTEGER NOT NULL REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                                  form_type    frisch.form_type NOT NULL,
                                  tense        frisch.tense,
                                  mood         frisch.mood,
                                  voice        frisch.voice,
                                  person       frisch.person,
                                  number_type  frisch.number_type,
                                  is_vedic     BOOLEAN NOT NULL DEFAULT FALSE,
                                  form_text    TEXT NOT NULL,
                                  raw_tag      TEXT,          -- source abbreviation, e.g. 'pp.', 'fut.', 'pass.'; NULL for
    -- the untagged present forms printed right after the lemma
                                  seq          SMALLINT NOT NULL DEFAULT 1
);
CREATE INDEX idx_verb_form_entry ON frisch.verb_form (entry_id);
CREATE INDEX idx_verb_form_text  ON frisch.verb_form (form_text);

-- ----------------------------------------------------------------------------
-- Secondary verbal stems mentioned inline on a root/verb entry
-- (causative, desiderative, ...) that are not separate "+..." sub-entries.
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.derived_stem (
                                     id               SERIAL PRIMARY KEY,
                                     entry_id         INTEGER NOT NULL REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                                     derivation_type  frisch.derivation_type NOT NULL,
                                     surface_form     TEXT,
                                     raw_tag          TEXT,     -- e.g. 'caus.', 'des.', 'intens.'
                                     seq              SMALLINT NOT NULL DEFAULT 1
);
CREATE INDEX idx_derived_stem_entry ON frisch.derived_stem (entry_id);

-- ----------------------------------------------------------------------------
-- Related/derived sub-entries: lines beginning with " +" in the source,
-- each with its own <L>/dict_entry row. Covers prefixed verbs
-- (+ud (udac) -> COMPOUND_VERB), secondary stems cited as their own
-- headword (+caus./+des./+pass.), and non-verbal derivatives (abstract
-- nouns, possessive adjectives, case-government patterns) classified as
-- OTHER with the raw text kept for detail.
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.related_form (
                                     entry_id           INTEGER PRIMARY KEY REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                                     base_entry_id      INTEGER REFERENCES frisch.dict_entry(entry_id),  -- resolved base headword
                                     derivation_type    frisch.derivation_type NOT NULL,
                                     preverb            TEXT,        -- 'ava', 'ā', 'sam', 'vi', 'ud', ... (COMPOUND_VERB only)
                                     surface_form       TEXT,        -- 'avakar', 'kīryate', 'kārayati', ...
                                     case_government    frisch.grammatical_case[],  -- e.g. '{ACCUSATIVE,ABLATIVE}'
                                     raw_text           TEXT NOT NULL   -- original " +..." line, for audit/reparse
);
CREATE INDEX idx_related_form_base ON frisch.related_form (base_entry_id);

-- ----------------------------------------------------------------------------
-- Cross references: "v. akartavya", "v. I dhā", "v. s. v. I kar"
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.cross_reference (
                                        id               SERIAL PRIMARY KEY,
                                        entry_id         INTEGER NOT NULL REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                                        ref_kind         TEXT NOT NULL DEFAULT 'see'
                                            CHECK (ref_kind IN ('see', 'see_sub_verbo', 'confer')),
                                        target_raw       TEXT NOT NULL,   -- raw target text as printed, e.g. 'akartavya', 'I kar'
                                        target_entry_id  INTEGER REFERENCES frisch.dict_entry(entry_id)  -- resolved by 2nd pass
);
CREATE INDEX idx_cross_reference_entry  ON frisch.cross_reference (entry_id);
CREATE INDEX idx_cross_reference_target ON frisch.cross_reference (target_entry_id);

-- ----------------------------------------------------------------------------
-- Glosses / translations (multilingual, whole-sense text): 1=cs, 2=ru, 3=en
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.gloss (
                              id            SERIAL PRIMARY KEY,
                              entry_id      INTEGER NOT NULL REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                              lang_code     TEXT NOT NULL REFERENCES frisch.language_code(code),
                              gloss_text    TEXT NOT NULL,
                              seq           SMALLINT NOT NULL DEFAULT 1
);
CREATE UNIQUE INDEX uq_gloss_entry_lang_seq ON frisch.gloss (entry_id, lang_code, seq);
CREATE INDEX idx_gloss_text_trgm ON frisch.gloss USING gin (gloss_text gin_trgm_ops);

-- ----------------------------------------------------------------------------
-- Gender-split gloss senses: a gloss such as
--   "zářící, nebeský, božský; m. bůh, bráhman, král; f. bohyně, královna"
-- is split on ';' into one row per sense, tagged with the gender(s) and/or
-- proper-noun marker that introduced it (NULL genders = general sense).
-- ----------------------------------------------------------------------------

CREATE TABLE frisch.gloss_sense (
                                    id             SERIAL PRIMARY KEY,
                                    entry_id       INTEGER NOT NULL REFERENCES frisch.dict_entry(entry_id) ON DELETE CASCADE,
                                    lang_code      TEXT NOT NULL REFERENCES frisch.language_code(code),
                                    seq            SMALLINT NOT NULL,          -- position within this language's gloss (1-based)
                                    genders        frisch.gender[],             -- e.g. '{MASCULINE}', '{MASCULINE,NEUTER}'; NULL = general
                                    number_note    frisch.number_type,          -- e.g. PLURAL for "m. pl. ..."
                                    is_proper_noun BOOLEAN NOT NULL DEFAULT FALSE,
                                    sense_text     TEXT NOT NULL
);
CREATE UNIQUE INDEX uq_gloss_sense_entry_lang_seq ON frisch.gloss_sense (entry_id, lang_code, seq);
CREATE INDEX idx_gloss_sense_entry     ON frisch.gloss_sense (entry_id);
CREATE INDEX idx_gloss_sense_genders   ON frisch.gloss_sense USING gin (genders);
CREATE INDEX idx_gloss_sense_text_trgm ON frisch.gloss_sense USING gin (sense_text gin_trgm_ops);

-- ----------------------------------------------------------------------------
-- Convenience views
-- ----------------------------------------------------------------------------

CREATE VIEW frisch.v_entry_full AS
SELECT
    e.entry_id,
    e.page_no,
    e.lemma_iast,
    e.homonym_index,
    e.is_root,
    e.is_crossref_only,
    array_agg(DISTINCT p.pos)                                       AS pos_list,
    (SELECT gloss_text FROM frisch.gloss g WHERE g.entry_id = e.entry_id AND g.lang_code = 'cs' ORDER BY seq LIMIT 1) AS gloss_cs,
    (SELECT gloss_text FROM frisch.gloss g WHERE g.entry_id = e.entry_id AND g.lang_code = 'ru' ORDER BY seq LIMIT 1) AS gloss_ru,
    (SELECT gloss_text FROM frisch.gloss g WHERE g.entry_id = e.entry_id AND g.lang_code = 'en' ORDER BY seq LIMIT 1) AS gloss_en,
    vc.conj_class,
    e.parent_entry_id
FROM frisch.dict_entry e
         LEFT JOIN frisch.entry_pos p  ON p.entry_id = e.entry_id
         LEFT JOIN frisch.verb_class vc ON vc.entry_id = e.entry_id
GROUP BY e.entry_id, e.page_no, e.lemma_iast, e.homonym_index, e.is_root,
         e.is_crossref_only, vc.conj_class, e.parent_entry_id;

CREATE VIEW frisch.v_verb_family AS
SELECT
    root.entry_id   AS root_entry_id,
    root.lemma_iast  AS root_lemma,
    vc.conj_class,
    d.entry_id      AS derivative_entry_id,
    d.lemma_iast     AS derivative_lemma,
    rf.preverb,
    rf.surface_form
FROM frisch.dict_entry root
         JOIN frisch.verb_class vc       ON vc.entry_id = root.entry_id
         LEFT JOIN frisch.related_form rf ON rf.base_entry_id = root.entry_id
    AND rf.derivation_type = 'COMPOUND_VERB'
         LEFT JOIN frisch.dict_entry d    ON d.entry_id = rf.entry_id;

-- ============================================================================
-- End of schema. See frisch_functions.sql for lookup functions
-- (frisch.get_lemma_info, frisch.get_lemma_json, frisch.rebuild_gloss_sense)
-- and import_frisch.py for the loader.
-- ============================================================================