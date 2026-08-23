-- =====================================================================
-- Apte "Practical Sanskrit-English Dictionary" (CDSL v02/ap) import schema
-- PostgreSQL 14+
--
-- Design goals:
--   1. Preserve raw digitized text losslessly (raw_text, raw_markup)
--   2. Store structural identifiers from the CDSL markup (<L>,<pc>,<k1>,<k2>,<hom>)
--   3. Parse grammatical abbreviations (P., A., Caus., m., 10 U., etc.)
--      into normalized enum-based facts, via an editable abbreviation
--      lookup table rather than hardcoded regex-to-enum logic.
--   4. Allow multiple grammatical analyses per entry (an entry may cover
--      several senses / verb classes / homonyms).
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0. ENUM TYPES
-- ---------------------------------------------------------------------

CREATE TYPE part_of_speech AS ENUM (
    'NOUN','VERB','ADJECTIVE','PRONOUN','ADVERB','PARTICLE',
    'INDECLINABLE','NUMERAL','CONJUNCTION','INTERJECTION','OTHER'
);

CREATE TYPE derivation_type AS ENUM (
    'SIMPLE_INFLECTION','ABSOLUTIVE','PARTICIPLE','GERUNDIVE',
    'INFINITIVE','CAUSATIVE','DESIDERATIVE','DENOMINATIVE',
    'COMPOUND_VERB','OTHER'
);

CREATE TYPE form_type AS ENUM (
    'FINITE','INFINITIVE','ABSOLUTIVE','PARTICIPLE','GERUNDIVE',
    'OTHER_NONFINITE','NOMINAL','ADJECTIVAL','PRONOMINAL','INDECLINABLE'
);

CREATE TYPE gender_type AS ENUM (
    'MASCULINE','FEMININE','NEUTER','UNSPECIFIED'
);

CREATE TYPE grammatical_case AS ENUM (
    'NOMINATIVE','ACCUSATIVE','INSTRUMENTAL','DATIVE','ABLATIVE',
    'GENITIVE','LOCATIVE','VOCATIVE'
);

CREATE TYPE mood_type AS ENUM (
    'INDICATIVE','OPTATIVE','IMPERATIVE','CONDITIONAL',
    'BENEDICTIVE','INJUNCTIVE'
);

CREATE TYPE number_type AS ENUM (
    'SINGULAR','DUAL','PLURAL'
);

CREATE TYPE person_type AS ENUM (
    'FIRST','SECOND','THIRD'
);

CREATE TYPE tense_type AS ENUM (
    'PRESENT','IMPERFECT','PERFECT','AORIST','FUTURE',
    'PERIPHRASTIC_FUTURE','CONDITIONAL','BENEDICTIVE'
);

CREATE TYPE voice_type AS ENUM (
    'ACTIVE','MIDDLE','PASSIVE'
);

-- Sanskrit verb classes (gaṇa) — needed because Apte marks these as
-- "1 P.", "4 Ā.", "10 U." etc. right after the headword. Not in your
-- original enum list, but essential to keep alongside derivation_type;
-- drop this type if you don't want it.
CREATE TYPE verb_class AS ENUM (
    'CLASS_1','CLASS_2','CLASS_3','CLASS_4','CLASS_5',
    'CLASS_6','CLASS_7','CLASS_8','CLASS_9','CLASS_10'
);

-- Traditional pada classification, distinct from voice_type (voice_type
-- describes a specific finite form; padatype describes the *conjugational
-- class* of the root as lexicalized: Parasmaipada / Ātmanepada / Ubhayapada)
CREATE TYPE pada_type AS ENUM (
    'PARASMAIPADA','ATMANEPADA','UBHAYAPADA'
);


-- ---------------------------------------------------------------------
-- 1. SOURCE / DICTIONARY METADATA
-- ---------------------------------------------------------------------

CREATE TABLE dictionaries (
    id              SMALLSERIAL PRIMARY KEY,
    code            TEXT UNIQUE NOT NULL,       -- 'ap', 'ap90', 'mw', ...
    title           TEXT NOT NULL,
    author          TEXT,
    edition_year    INTEGER,
    source_repo_url TEXT
);

INSERT INTO dictionaries (code, title, author, edition_year, source_repo_url) VALUES
 ('ap',  'The Practical Sanskrit-English Dictionary', 'Vaman Shivram Apte', 1957,
  'https://github.com/sanskrit-lexicon/csl-orig/tree/main/v02/ap');


-- ---------------------------------------------------------------------
-- 2. RAW ENTRIES  (one row per <L>...<LEND> block)
-- ---------------------------------------------------------------------

CREATE TABLE entries (
    id               BIGSERIAL PRIMARY KEY,
    dictionary_id    SMALLINT NOT NULL REFERENCES dictionaries(id),

    lnum             TEXT NOT NULL,             -- raw <L> value, e.g. '1182' or '1182-1'
    lnum_sort        INTEGER,                   -- numeric part, for ordering
    lnum_suffix      TEXT,                      -- e.g. '1' in '1182-1'

    pc_volume        SMALLINT,                  -- from <pc>
    pc_page          INTEGER,
    pc_column        TEXT,                      -- 'a','b','a1', ...

    k1_slp1          TEXT NOT NULL,             -- headword, SLP1
    k2_original      TEXT,                      -- headword, original/IAST spelling
    homonym_num      SMALLINT,                  -- <hom>

    headword_devanagari TEXT,                   -- derived from k1_slp1 via transliteration

    raw_markup       TEXT NOT NULL,             -- full original block, untouched
    body_text        TEXT,                      -- markup-stripped, human-readable body

    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (dictionary_id, lnum)
);

CREATE INDEX idx_entries_k1 ON entries (k1_slp1);
CREATE INDEX idx_entries_k2 ON entries (k2_original);
CREATE INDEX idx_entries_headword_deva ON entries (headword_devanagari);
CREATE INDEX idx_entries_dict ON entries (dictionary_id);


-- ---------------------------------------------------------------------
-- 3. PAGE / SCAN REFERENCES  ([PageV-PPP-C+ N] markers inside an entry)
-- ---------------------------------------------------------------------

CREATE TABLE page_breaks (
    id            BIGSERIAL PRIMARY KEY,
    entry_id      BIGINT NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
    seq_in_entry  SMALLINT NOT NULL,      -- order of this break within the entry
    volume        SMALLINT,
    page          INTEGER,
    column_label  TEXT,
    line_count    INTEGER,                -- the "N" (lines in following column)
    raw_marker    TEXT NOT NULL,          -- original bracketed text
    UNIQUE (entry_id, seq_in_entry)
);


-- ---------------------------------------------------------------------
-- 4. SENSES  (Ⓐ Ⓑ Ⓒ ... subdivisions, or numbered 1/2/3 subsenses)
-- ---------------------------------------------------------------------

CREATE TABLE senses (
    id            BIGSERIAL PRIMARY KEY,
    entry_id      BIGINT NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
    seq           SMALLINT NOT NULL,          -- ordering within entry
    marker        TEXT,                       -- 'Ⓐ', '1', '(a)', etc. as printed
    gloss_text    TEXT,                       -- English definition text, cleaned
    raw_text      TEXT NOT NULL,              -- untouched slice
    UNIQUE (entry_id, seq)
);


-- ---------------------------------------------------------------------
-- 5. GRAMMATICAL ABBREVIATION LOOKUP
--
-- This is the heart of the parsing strategy: instead of writing ad hoc
-- regexes that bake in every abbreviation variant, every abbreviation
-- token found in the text is matched against this table, which maps it
-- to zero or more normalized grammatical facts. Curate/extend this table
-- as you encounter new abbreviations during import (Apte uses many
-- inconsistently across editions/OCR).
-- ---------------------------------------------------------------------

CREATE TABLE grammar_abbreviations (
    id                  SERIAL PRIMARY KEY,
    abbrev              TEXT NOT NULL,          -- as it appears, e.g. 'P.', 'Caus.', 'du.'
    abbrev_normalized   TEXT NOT NULL,           -- lowercased, punctuation-stripped, for matching
    category            TEXT NOT NULL,           -- which enum/dimension this maps to
                                                  -- e.g. 'pada_type','part_of_speech','gender_type',
                                                  --      'derivation_type','verb_class','number_type',
                                                  --      'person_type','tense_type','mood_type',
                                                  --      'voice_type','grammatical_case','form_type'
    mapped_value        TEXT NOT NULL,           -- the enum literal it maps to
    notes               TEXT,
    UNIQUE (abbrev_normalized, category)
);

-- Seed data — the common Apte / CDSL abbreviation set.
-- (Extend after inspecting actual OCR text; abbreviation spelling/spacing
--  varies, e.g. 'P.' vs 'P' vs 'Paras.'.)

INSERT INTO grammar_abbreviations (abbrev, abbrev_normalized, category, mapped_value, notes) VALUES
-- Pada / voice-of-root
('P.',    'p',     'pada_type', 'PARASMAIPADA', 'Parasmaipada'),
('Ā.',    'a',     'pada_type', 'ATMANEPADA',   'Ātmanepada'),
('A.',    'a',     'pada_type', 'ATMANEPADA',   'Ātmanepada (ASCII variant of Ā.)'),
('U.',    'u',     'pada_type', 'UBHAYAPADA',   'Ubhayapada'),
('Ubh.',  'ubh',   'pada_type', 'UBHAYAPADA',   'Ubhayapada, alt spelling'),

-- Verb classes (gaṇa) — appear as leading digit, e.g. "1 P.", "10 U."
('1',  '1',  'verb_class', 'CLASS_1',  'bhvādi'),
('2',  '2',  'verb_class', 'CLASS_2',  'adādi'),
('3',  '3',  'verb_class', 'CLASS_3',  'juhotyādi'),
('4',  '4',  'verb_class', 'CLASS_4',  'divādi'),
('5',  '5',  'verb_class', 'CLASS_5',  'svādi'),
('6',  '6',  'verb_class', 'CLASS_6',  'tudādi'),
('7',  '7',  'verb_class', 'CLASS_7',  'rudhādi'),
('8',  '8',  'verb_class', 'CLASS_8',  'tanādi'),
('9',  '9',  'verb_class', 'CLASS_9',  'kryādi'),
('10', '10', 'verb_class', 'CLASS_10', 'curādi'),

-- Derivation type
('Caus.',  'caus',   'derivation_type', 'CAUSATIVE',    NULL),
('Desid.', 'desid',  'derivation_type', 'DESIDERATIVE', NULL),
('Denom.', 'denom',  'derivation_type', 'DENOMINATIVE', NULL),
('Freq.',  'freq',   'derivation_type', 'OTHER',        'Frequentative/intensive — no dedicated enum value supplied'),

-- Part of speech
('m.',    'm',     'part_of_speech', 'NOUN',         'masculine noun (gender captured separately)'),
('f.',    'f',     'part_of_speech', 'NOUN',         'feminine noun'),
('n.',    'n',     'part_of_speech', 'NOUN',         'neuter noun'),
('a.',    'a-adj', 'part_of_speech', 'ADJECTIVE',    NULL),
('adj.',  'adj',   'part_of_speech', 'ADJECTIVE',    NULL),
('ind.',  'ind',   'part_of_speech', 'INDECLINABLE', NULL),
('adv.',  'adv',   'part_of_speech', 'ADVERB',       NULL),
('pron.', 'pron',  'part_of_speech', 'PRONOUN',      NULL),
('conj.', 'conj',  'part_of_speech', 'CONJUNCTION',  NULL),
('interj.','interj','part_of_speech','INTERJECTION', NULL),
('num.',  'num',   'part_of_speech', 'NUMERAL',      NULL),
('v.t.',  'vt',    'part_of_speech', 'VERB',         'transitive verb'),
('v.i.',  'vi',    'part_of_speech', 'VERB',         'intransitive verb'),
('v.',    'v',     'part_of_speech', 'VERB',         NULL),

-- Gender (as a separate dimension from POS, for noun/adjective agreement)
('m.', 'm', 'gender_type', 'MASCULINE', NULL),
('f.', 'f', 'gender_type', 'FEMININE',  NULL),
('n.', 'n', 'gender_type', 'NEUTER',    NULL),

-- Number
('sg.', 'sg', 'number_type', 'SINGULAR', NULL),
('du.', 'du', 'number_type', 'DUAL',     NULL),
('pl.', 'pl', 'number_type', 'PLURAL',   NULL),

-- Case
('nom.',  'nom',  'grammatical_case', 'NOMINATIVE',   NULL),
('acc.',  'acc',  'grammatical_case', 'ACCUSATIVE',   NULL),
('instr.','instr','grammatical_case', 'INSTRUMENTAL', NULL),
('dat.',  'dat',  'grammatical_case', 'DATIVE',       NULL),
('abl.',  'abl',  'grammatical_case', 'ABLATIVE',     NULL),
('gen.',  'gen',  'grammatical_case', 'GENITIVE',     NULL),
('loc.',  'loc',  'grammatical_case', 'LOCATIVE',     NULL),
('voc.',  'voc',  'grammatical_case', 'VOCATIVE',     NULL),

-- Tense / mood
('pr.',    'pr',    'tense_type', 'PRESENT',              NULL),
('impf.',  'impf',  'tense_type', 'IMPERFECT',            NULL),
('pf.',    'pf',    'tense_type', 'PERFECT',              NULL),
('perf.',  'perf',  'tense_type', 'PERFECT',              'alt spelling'),
('aor.',   'aor',   'tense_type', 'AORIST',               NULL),
('fut.',   'fut',   'tense_type', 'FUTURE',               NULL),
('p.fut.', 'pfut',  'tense_type', 'PERIPHRASTIC_FUTURE',  NULL),
('cond.',  'cond',  'tense_type', 'CONDITIONAL',          NULL),
('opt.',   'opt',   'mood_type',  'OPTATIVE',             NULL),
('imperat.','imperat','mood_type','IMPERATIVE',           NULL),
('inj.',   'inj',   'mood_type',  'INJUNCTIVE',           NULL),
('indic.', 'indic', 'mood_type',  'INDICATIVE',           NULL),
('bene.',  'bene',  'mood_type',  'BENEDICTIVE',          NULL),

-- Non-finite forms
('inf.',  'inf',  'form_type', 'INFINITIVE', NULL),
('ger.',  'ger',  'form_type', 'GERUNDIVE',  NULL),
('pot.',  'pot',  'form_type', 'GERUNDIVE',  'potential participle / gerundive'),
('p.p.',  'pp',   'form_type', 'PARTICIPLE', 'past participle'),
('pr.p.', 'prp',  'form_type', 'PARTICIPLE', 'present participle'),
('abs.',  'abs',  'form_type', 'ABSOLUTIVE', NULL);


-- ---------------------------------------------------------------------
-- 6. PARSED GRAMMATICAL FACTS
--
-- Junction/fact table: each row is ONE grammatical assertion about an
-- entry or a specific sense (e.g. "this entry, as a verb, is class 1
-- Parasmaipada"; "this sense's headword is a feminine noun"). An entry
-- can have many rows here (homonyms, multiple POS, causative alongside
-- simple form, etc).
-- ---------------------------------------------------------------------

CREATE TABLE grammar_facts (
    id                  BIGSERIAL PRIMARY KEY,
    entry_id            BIGINT NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
    sense_id            BIGINT REFERENCES senses(id) ON DELETE CASCADE,  -- NULL = applies to whole entry

    part_of_speech      part_of_speech,
    derivation_type     derivation_type,
    form_type           form_type,
    gender              gender_type,
    grammatical_case    grammatical_case,
    mood                mood_type,
    grammatical_number  number_type,
    person              person_type,
    tense               tense_type,
    voice               voice_type,
    verb_class          verb_class,
    pada                pada_type,

    -- provenance: which raw token(s) produced this fact, for audit/debug
    source_abbrev_ids   INTEGER[],
    raw_grammar_span    TEXT NOT NULL,     -- exact substring parsed, e.g. "10 U." or "f."
    confidence          NUMERIC(3,2) DEFAULT 1.00,  -- <1.00 for heuristic/ambiguous parses
    parse_notes         TEXT,

    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_grammar_facts_entry ON grammar_facts (entry_id);
CREATE INDEX idx_grammar_facts_sense ON grammar_facts (sense_id);
CREATE INDEX idx_grammar_facts_pos   ON grammar_facts (part_of_speech);
CREATE INDEX idx_grammar_facts_verbclass ON grammar_facts (verb_class);


-- ---------------------------------------------------------------------
-- 7. CITATIONS  (source references inside entries, e.g. "K. 179", "Bh.",
--    "Māl. 5.25") — not grammar per se, but usually wanted alongside it
--    and is straightforward to extract with the same abbreviation
--    mechanism. Optional; drop if out of scope.
-- ---------------------------------------------------------------------

CREATE TABLE text_source_abbreviations (
    id           SERIAL PRIMARY KEY,
    abbrev       TEXT UNIQUE NOT NULL,     -- 'K.', 'Bh.', 'Māl.', ...
    full_title   TEXT,                     -- 'Kādambarī', 'Bhartṛhari', 'Mālavikāgnimitra'
    author       TEXT
);

CREATE TABLE citations (
    id            BIGSERIAL PRIMARY KEY,
    entry_id      BIGINT NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
    sense_id      BIGINT REFERENCES senses(id) ON DELETE CASCADE,
    source_abbrev_id INTEGER REFERENCES text_source_abbreviations(id),
    locus         TEXT,             -- '5.25', '179', etc. as printed
    raw_text      TEXT NOT NULL
);


-- ---------------------------------------------------------------------
-- 8. IMPORT LOG  (per-entry parse status, for iterative QA)
-- ---------------------------------------------------------------------

CREATE TABLE import_log (
    id            BIGSERIAL PRIMARY KEY,
    entry_id      BIGINT NOT NULL REFERENCES entries(id) ON DELETE CASCADE,
    stage         TEXT NOT NULL,        -- 'structure_parse','grammar_parse','citation_parse'
    status        TEXT NOT NULL CHECK (status IN ('ok','partial','failed')),
    message       TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_import_log_status ON import_log (status);


-- =====================================================================
-- NOTES ON PARSING STRATEGY (read before writing the import script)
-- =====================================================================
--
-- 1. Two-pass import:
--    Pass 1 — split ap.txt on <L>...<LEND>, populate `entries` with raw
--    fields only (lnum, pc, k1, k2, hom, raw_markup). This is a pure
--    structural parse and should have ~0 failures.
--
--    Pass 2 — run a grammar tokenizer over each entry's raw_markup
--    (specifically the portion between the headword and the first
--    English gloss, typically inside <lex>...</lex> tags or immediately
--    following <ab>...</ab>). Tokenize on whitespace/periods, look each
--    token up in grammar_abbreviations, and emit rows into grammar_facts.
--    Tokens with no match should NOT be silently dropped — log them
--    (e.g. a `unmatched_tokens` staging table) so you can extend the
--    abbreviation table iteratively rather than guessing all variants
--    up front. Apte's OCR is inconsistent (missing periods, spacing,
--    ligature errors), so expect an unmatched_tokens table with a few
--    hundred entries and periodic reclassification.
--
-- 2. Ambiguity: 'm.', 'f.', 'n.' map to BOTH part_of_speech=NOUN and a
--    gender_type in the seed data above (two separate grammar_facts
--    rows, or one row using both columns — pick one approach and be
--    consistent; the schema supports either).
--
-- 3. Verb entries commonly appear as a compact sequence like
--    "10 U." (class 10, ubhayapada) or "1 P." — write a small regex
--    `^(\d{1,2})\s*([PAU]\.|Ubh\.)` as a fast-path before falling back
--    to the generic tokenizer, since this pattern covers a large
--    fraction of verb headword lines.
--
-- 4. Keep raw_grammar_span + source_abbrev_ids on every grammar_facts
--    row. When (not if) you find misparses, you'll want to re-run
--    Pass 2 against raw_markup without re-deriving Pass 1.
-- =====================================================================
