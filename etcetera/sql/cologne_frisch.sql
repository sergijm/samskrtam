/*
 Navicat Premium Data Transfer

 Source Server         : localhost-pg17
 Source Server Type    : PostgreSQL
 Source Server Version : 170000 (170000)
 Source Host           : localhost:5436
 Source Catalog        : samskrtam
 Source Schema         : cologne_frisch

 Target Server Type    : PostgreSQL
 Target Server Version : 170000 (170000)
 File Encoding         : 65001

 Date: 24/08/2026 17:49:41
*/


-- ----------------------------
-- Type structure for derivation_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."derivation_type";
CREATE TYPE "cologne_frisch"."derivation_type" AS ENUM (
  'SIMPLE_INFLECTION',
  'ABSOLUTIVE',
  'PARTICIPLE',
  'GERUNDIVE',
  'INFINITIVE',
  'CAUSATIVE',
  'DESIDERATIVE',
  'DENOMINATIVE',
  'COMPOUND_VERB',
  'OTHER'
);
ALTER TYPE "cologne_frisch"."derivation_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for form_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."form_type";
CREATE TYPE "cologne_frisch"."form_type" AS ENUM (
  'FINITE',
  'INFINITIVE',
  'ABSOLUTIVE',
  'PARTICIPLE',
  'GERUNDIVE',
  'OTHER_NONFINITE',
  'NOMINAL',
  'ADJECTIVAL',
  'PRONOMINAL',
  'INDECLINABLE'
);
ALTER TYPE "cologne_frisch"."form_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for gender
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."gender";
CREATE TYPE "cologne_frisch"."gender" AS ENUM (
  'MASCULINE',
  'FEMININE',
  'NEUTER',
  'UNSPECIFIED'
);
ALTER TYPE "cologne_frisch"."gender" OWNER TO "postgres";

-- ----------------------------
-- Type structure for grammatical_case
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."grammatical_case";
CREATE TYPE "cologne_frisch"."grammatical_case" AS ENUM (
  'NOMINATIVE',
  'ACCUSATIVE',
  'INSTRUMENTAL',
  'DATIVE',
  'ABLATIVE',
  'GENITIVE',
  'LOCATIVE',
  'VOCATIVE'
);
ALTER TYPE "cologne_frisch"."grammatical_case" OWNER TO "postgres";

-- ----------------------------
-- Type structure for mood
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."mood";
CREATE TYPE "cologne_frisch"."mood" AS ENUM (
  'INDICATIVE',
  'OPTATIVE',
  'IMPERATIVE',
  'CONDITIONAL',
  'BENEDICTIVE',
  'INJUNCTIVE'
);
ALTER TYPE "cologne_frisch"."mood" OWNER TO "postgres";

-- ----------------------------
-- Type structure for number_type
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."number_type";
CREATE TYPE "cologne_frisch"."number_type" AS ENUM (
  'SINGULAR',
  'DUAL',
  'PLURAL'
);
ALTER TYPE "cologne_frisch"."number_type" OWNER TO "postgres";

-- ----------------------------
-- Type structure for part_of_speech
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."part_of_speech";
CREATE TYPE "cologne_frisch"."part_of_speech" AS ENUM (
  'NOUN',
  'VERB',
  'ADJECTIVE',
  'PRONOUN',
  'ADVERB',
  'PARTICLE',
  'INDECLINABLE',
  'NUMERAL',
  'CONJUNCTION',
  'INTERJECTION',
  'OTHER'
);
ALTER TYPE "cologne_frisch"."part_of_speech" OWNER TO "postgres";

-- ----------------------------
-- Type structure for person
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."person";
CREATE TYPE "cologne_frisch"."person" AS ENUM (
  'FIRST',
  'SECOND',
  'THIRD'
);
ALTER TYPE "cologne_frisch"."person" OWNER TO "postgres";

-- ----------------------------
-- Type structure for tense
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."tense";
CREATE TYPE "cologne_frisch"."tense" AS ENUM (
  'PRESENT',
  'IMPERFECT',
  'PERFECT',
  'AORIST',
  'FUTURE',
  'PERIPHRASTIC_FUTURE',
  'CONDITIONAL',
  'BENEDICTIVE'
);
ALTER TYPE "cologne_frisch"."tense" OWNER TO "postgres";

-- ----------------------------
-- Type structure for voice
-- ----------------------------
DROP TYPE IF EXISTS "cologne_frisch"."voice";
CREATE TYPE "cologne_frisch"."voice" AS ENUM (
  'ACTIVE',
  'MIDDLE',
  'PASSIVE'
);
ALTER TYPE "cologne_frisch"."voice" OWNER TO "postgres";

-- ----------------------------
-- Sequence structure for cross_reference_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_frisch"."cross_reference_id_seq";
CREATE SEQUENCE "cologne_frisch"."cross_reference_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for derived_stem_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_frisch"."derived_stem_id_seq";
CREATE SEQUENCE "cologne_frisch"."derived_stem_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for entry_gender_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_frisch"."entry_gender_id_seq";
CREATE SEQUENCE "cologne_frisch"."entry_gender_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for gloss_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_frisch"."gloss_id_seq";
CREATE SEQUENCE "cologne_frisch"."gloss_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for gloss_sense_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_frisch"."gloss_sense_id_seq";
CREATE SEQUENCE "cologne_frisch"."gloss_sense_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for verb_form_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "cologne_frisch"."verb_form_id_seq";
CREATE SEQUENCE "cologne_frisch"."verb_form_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Table structure for cross_reference
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."cross_reference";
CREATE TABLE "cologne_frisch"."cross_reference" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_frisch".cross_reference_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "ref_kind" text COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'see'::text,
  "target_raw" text COLLATE "pg_catalog"."default" NOT NULL,
  "target_entry_id" int4
)
;

-- ----------------------------
-- Table structure for derived_stem
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."derived_stem";
CREATE TABLE "cologne_frisch"."derived_stem" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_frisch".derived_stem_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "derivation_type" "cologne_frisch"."derivation_type" NOT NULL,
  "surface_form" text COLLATE "pg_catalog"."default",
  "raw_tag" text COLLATE "pg_catalog"."default",
  "seq" int2 NOT NULL DEFAULT 1
)
;

-- ----------------------------
-- Table structure for dict_entry
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."dict_entry";
CREATE TABLE "cologne_frisch"."dict_entry" (
  "entry_id" int4 NOT NULL,
  "page_no" int2 NOT NULL,
  "sort_key1" text COLLATE "pg_catalog"."default" NOT NULL,
  "sort_key2" text COLLATE "pg_catalog"."default" NOT NULL,
  "homonym_index" int2,
  "is_root" bool NOT NULL DEFAULT false,
  "lemma_iast" text COLLATE "pg_catalog"."default" NOT NULL,
  "lemma_ascii" text COLLATE "pg_catalog"."default" NOT NULL,
  "is_related_form" bool NOT NULL DEFAULT false,
  "parent_entry_id" int4,
  "is_crossref_only" bool NOT NULL DEFAULT false,
  "grammar_note" text COLLATE "pg_catalog"."default",
  "raw_headline" text COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamptz(6) NOT NULL DEFAULT now()
)
;

-- ----------------------------
-- Table structure for entry_gender
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."entry_gender";
CREATE TABLE "cologne_frisch"."entry_gender" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_frisch".entry_gender_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "gender" "cologne_frisch"."gender" NOT NULL,
  "stem_suffix" text COLLATE "pg_catalog"."default"
)
;

-- ----------------------------
-- Table structure for entry_pos
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."entry_pos";
CREATE TABLE "cologne_frisch"."entry_pos" (
  "entry_id" int4 NOT NULL,
  "pos" "cologne_frisch"."part_of_speech" NOT NULL,
  "qualifier" text COLLATE "pg_catalog"."default" NOT NULL DEFAULT ''::text
)
;

-- ----------------------------
-- Table structure for gloss
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."gloss";
CREATE TABLE "cologne_frisch"."gloss" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_frisch".gloss_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "lang_code" text COLLATE "pg_catalog"."default" NOT NULL,
  "gloss_text" text COLLATE "pg_catalog"."default" NOT NULL,
  "seq" int2 NOT NULL DEFAULT 1
)
;

-- ----------------------------
-- Table structure for gloss_sense
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."gloss_sense";
CREATE TABLE "cologne_frisch"."gloss_sense" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_frisch".gloss_sense_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "lang_code" text COLLATE "pg_catalog"."default" NOT NULL,
  "seq" int2 NOT NULL,
  "genders" "cologne_frisch"."gender"[],
  "number_note" "cologne_frisch"."number_type",
  "is_proper_noun" bool NOT NULL DEFAULT false,
  "sense_text" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for language_code
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."language_code";
CREATE TABLE "cologne_frisch"."language_code" (
  "code" text COLLATE "pg_catalog"."default" NOT NULL,
  "name" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for lemma_frequency
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."lemma_frequency";
CREATE TABLE "cologne_frisch"."lemma_frequency" (
  "lemma_iast" varchar(200) COLLATE "pg_catalog"."default" NOT NULL,
  "frequency" int8,
  "row_num" int8
)
;

-- ----------------------------
-- Table structure for related_form
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."related_form";
CREATE TABLE "cologne_frisch"."related_form" (
  "entry_id" int4 NOT NULL,
  "base_entry_id" int4,
  "derivation_type" "cologne_frisch"."derivation_type" NOT NULL,
  "preverb" text COLLATE "pg_catalog"."default",
  "surface_form" text COLLATE "pg_catalog"."default",
  "case_government" "cologne_frisch"."grammatical_case"[],
  "raw_text" text COLLATE "pg_catalog"."default" NOT NULL
)
;

-- ----------------------------
-- Table structure for verb_class
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."verb_class";
CREATE TABLE "cologne_frisch"."verb_class" (
  "entry_id" int4 NOT NULL,
  "conj_class" int2
)
;

-- ----------------------------
-- Table structure for verb_form
-- ----------------------------
DROP TABLE IF EXISTS "cologne_frisch"."verb_form";
CREATE TABLE "cologne_frisch"."verb_form" (
  "id" int4 NOT NULL DEFAULT nextval('"cologne_frisch".verb_form_id_seq'::regclass),
  "entry_id" int4 NOT NULL,
  "form_type" "cologne_frisch"."form_type" NOT NULL,
  "tense" "cologne_frisch"."tense",
  "mood" "cologne_frisch"."mood",
  "voice" "cologne_frisch"."voice",
  "person" "cologne_frisch"."person",
  "number_type" "cologne_frisch"."number_type",
  "is_vedic" bool NOT NULL DEFAULT false,
  "form_text" text COLLATE "pg_catalog"."default" NOT NULL,
  "raw_tag" text COLLATE "pg_catalog"."default",
  "seq" int2 NOT NULL DEFAULT 1
)
;

-- ----------------------------
-- Function structure for _parse_gloss_segment
-- ----------------------------
DROP FUNCTION IF EXISTS "cologne_frisch"."_parse_gloss_segment"("p_seg" text);
CREATE OR REPLACE FUNCTION "cologne_frisch"."_parse_gloss_segment"("p_seg" text)
  RETURNS TABLE("genders" "cologne_frisch"."_gender", "number_note" "cologne_frisch"."number_type", "is_proper_noun" bool, "sense_text" text) AS $BODY$
DECLARE
    seg     TEXT := btrim(p_seg);
    mtc     TEXT[];
    letters TEXT[];
    result_genders cologne_frisch.gender[] := ARRAY[]::cologne_frisch.gender[];
    letter  TEXT;
BEGIN
    IF seg = '' THEN
        RETURN;
    END IF;

    mtc := regexp_match(
        seg,
        '^((?:[mfn]\.,?\s*)+)((?:pl|du|sg)\.\s*)?((?:pr)\.\s*)?(.*)$'
    );

    IF mtc IS NULL OR mtc[1] IS NULL THEN
        genders := NULL;
        number_note := NULL;
        is_proper_noun := FALSE;
        sense_text := seg;
        RETURN NEXT;
        RETURN;
    END IF;

    SELECT array_agg(x[1]) INTO letters
    FROM regexp_matches(mtc[1], '[mfn]', 'g') AS x;

    FOREACH letter IN ARRAY letters LOOP
        result_genders := result_genders || CASE letter
            WHEN 'm' THEN 'MASCULINE'::cologne_frisch.gender
            WHEN 'f' THEN 'FEMININE'::cologne_frisch.gender
            WHEN 'n' THEN 'NEUTER'::cologne_frisch.gender
        END;
    END LOOP;
    genders := result_genders;

    number_note := CASE btrim(coalesce(mtc[2], ''), '. ')
        WHEN 'pl' THEN 'PLURAL'::cologne_frisch.number_type
        WHEN 'du' THEN 'DUAL'::cologne_frisch.number_type
        WHEN 'sg' THEN 'SINGULAR'::cologne_frisch.number_type
        ELSE NULL
    END;
    is_proper_noun := mtc[3] IS NOT NULL;
    sense_text := btrim(mtc[4]);

    RETURN NEXT;
END;
$BODY$
  LANGUAGE plpgsql IMMUTABLE
  COST 100
  ROWS 1000;

-- ----------------------------
-- Function structure for get_lemma_info
-- ----------------------------
DROP FUNCTION IF EXISTS "cologne_frisch"."get_lemma_info"("p_lemma" text);
CREATE OR REPLACE FUNCTION "cologne_frisch"."get_lemma_info"("p_lemma" text)
  RETURNS TABLE("entry_id" int4, "homonym_index" int2, "lemma_iast" text, "is_root" bool, "is_related_form" bool, "parent_entry_id" int4, "parent_lemma" text, "grammar_note" text, "pos" jsonb, "genders" jsonb, "verb_class" int2, "verb_forms" jsonb, "derived_stems" jsonb, "related_forms" jsonb, "cross_references" jsonb, "gloss_ru" text, "gloss_cs" text, "gloss_en" text, "senses" jsonb, "raw_headline" text) AS $BODY$
    WITH matched AS (
        SELECT e.*
        FROM cologne_frisch.dict_entry e
        WHERE e.lemma_iast = p_lemma
        UNION
        SELECT e.*
        FROM cologne_frisch.dict_entry e
        WHERE e.lemma_ascii = lingua.normalize_lemma(p_lemma)
    )
    SELECT
        e.entry_id,
        e.homonym_index,
        e.lemma_iast,
        e.is_root,
        e.is_related_form,
        e.parent_entry_id,
        parent.lemma_iast AS parent_lemma,
        e.grammar_note,

        (SELECT jsonb_agg(jsonb_build_object(
                    'pos', ep.pos,
                    'qualifier', NULLIF(ep.qualifier, '')
                ))
         FROM cologne_frisch.entry_pos ep
         WHERE ep.entry_id = e.entry_id)                              AS pos,

        (SELECT jsonb_agg(jsonb_build_object(
                    'gender', eg.gender,
                    'stem_suffix', eg.stem_suffix
                ))
         FROM cologne_frisch.entry_gender eg
         WHERE eg.entry_id = e.entry_id)                              AS genders,

        vc.conj_class                                                  AS verb_class,

        (SELECT jsonb_agg(jsonb_build_object(
                    'form_type', vf.form_type,
                    'tense', vf.tense,
                    'mood', vf.mood,
                    'voice', vf.voice,
                    'person', vf.person,
                    'number', vf.number_type,
                    'vedic', vf.is_vedic,
                    'form', vf.form_text,
                    'raw_tag', vf.raw_tag
                ) ORDER BY vf.seq)
         FROM cologne_frisch.verb_form vf
         WHERE vf.entry_id = e.entry_id)                              AS verb_forms,

        (SELECT jsonb_agg(jsonb_build_object(
                    'derivation_type', ds.derivation_type,
                    'form', ds.surface_form,
                    'raw_tag', ds.raw_tag
                ) ORDER BY ds.seq)
         FROM cologne_frisch.derived_stem ds
         WHERE ds.entry_id = e.entry_id)                              AS derived_stems,

        (SELECT jsonb_agg(jsonb_build_object(
                    'derivation_type', rf.derivation_type,
                    'preverb', rf.preverb,
                    'surface_form', rf.surface_form,
                    'case_government', to_jsonb(rf.case_government),
                    'entry_id', rf.entry_id,
                    'lemma_iast', d2.lemma_iast
                ))
         FROM cologne_frisch.related_form rf
         JOIN cologne_frisch.dict_entry d2 ON d2.entry_id = rf.entry_id
         WHERE rf.base_entry_id = e.entry_id)                         AS related_forms,

        (SELECT jsonb_agg(jsonb_build_object(
                    'kind', cr.ref_kind,
                    'target_raw', cr.target_raw,
                    'target_entry_id', cr.target_entry_id,
                    'target_lemma', d3.lemma_iast
                ))
         FROM cologne_frisch.cross_reference cr
         LEFT JOIN cologne_frisch.dict_entry d3 ON d3.entry_id = cr.target_entry_id
         WHERE cr.entry_id = e.entry_id)                              AS cross_references,

        (SELECT g.gloss_text FROM cologne_frisch.gloss g
         WHERE g.entry_id = e.entry_id AND g.lang_code = 'ru'
         ORDER BY g.seq LIMIT 1)                                      AS gloss_ru,
        (SELECT g.gloss_text FROM cologne_frisch.gloss g
         WHERE g.entry_id = e.entry_id AND g.lang_code = 'cs'
         ORDER BY g.seq LIMIT 1)                                      AS gloss_cs,
        (SELECT g.gloss_text FROM cologne_frisch.gloss g
         WHERE g.entry_id = e.entry_id AND g.lang_code = 'en'
         ORDER BY g.seq LIMIT 1)                                      AS gloss_en,

        (SELECT jsonb_agg(jsonb_build_object(
                    'genders', to_jsonb(coalesce(cs.genders, ru.genders, en.genders)),
                    'number_note', coalesce(cs.number_note, ru.number_note, en.number_note),
                    'is_proper_noun', coalesce(cs.is_proper_noun, ru.is_proper_noun, en.is_proper_noun, false),
                    'cs', cs.sense_text,
                    'ru', ru.sense_text,
                    'en', en.sense_text
                ) ORDER BY coalesce(cs.seq, ru.seq, en.seq))
         FROM (SELECT * FROM cologne_frisch.gloss_sense WHERE entry_id = e.entry_id AND lang_code = 'cs') cs
         FULL JOIN (SELECT * FROM cologne_frisch.gloss_sense WHERE entry_id = e.entry_id AND lang_code = 'ru') ru
                ON ru.seq = cs.seq
         FULL JOIN (SELECT * FROM cologne_frisch.gloss_sense WHERE entry_id = e.entry_id AND lang_code = 'en') en
                ON en.seq = coalesce(cs.seq, ru.seq))                 AS senses,

        e.raw_headline
    FROM matched e
    LEFT JOIN cologne_frisch.verb_class vc     ON vc.entry_id = e.entry_id
    LEFT JOIN cologne_frisch.dict_entry parent ON parent.entry_id = e.parent_entry_id
    ORDER BY e.homonym_index NULLS FIRST, e.entry_id;
$BODY$
  LANGUAGE sql STABLE
  COST 100
  ROWS 1000;
COMMENT ON FUNCTION "cologne_frisch"."get_lemma_info"("p_lemma" text) IS 'Returns Russian/Czech/English glosses (whole-text and gender-split) plus full grammatical info (POS, gender, verb class/forms, secondary stems, related/derived forms, cross-references) for every dictionary entry matching the given IAST lemma. Several rows are returned for homonyms.';

-- ----------------------------
-- Function structure for get_lemma_json
-- ----------------------------
DROP FUNCTION IF EXISTS "cologne_frisch"."get_lemma_json"("p_lemma" text);
CREATE OR REPLACE FUNCTION "cologne_frisch"."get_lemma_json"("p_lemma" text)
  RETURNS "pg_catalog"."jsonb" AS $BODY$
    SELECT COALESCE(jsonb_agg(to_jsonb(t)), '[]'::jsonb)
    FROM cologne_frisch.get_lemma_info(p_lemma) AS t;
$BODY$
  LANGUAGE sql STABLE
  COST 100;
COMMENT ON FUNCTION "cologne_frisch"."get_lemma_json"("p_lemma" text) IS 'Same as get_lemma_info(), collapsed into a single JSONB array (one object per homonym).';

-- ----------------------------
-- Function structure for rebuild_gloss_sense
-- ----------------------------
DROP FUNCTION IF EXISTS "cologne_frisch"."rebuild_gloss_sense"();
CREATE OR REPLACE FUNCTION "cologne_frisch"."rebuild_gloss_sense"()
  RETURNS "pg_catalog"."int4" AS $BODY$
DECLARE
    n_inserted INTEGER;
BEGIN
    TRUNCATE cologne_frisch.gloss_sense RESTART IDENTITY;

    INSERT INTO cologne_frisch.gloss_sense (entry_id, lang_code, seq, genders, number_note, is_proper_noun, sense_text)
    SELECT
        g.entry_id,
        g.lang_code,
        seg.ord::SMALLINT,
        p.genders,
        p.number_note,
        p.is_proper_noun,
        p.sense_text
    FROM cologne_frisch.gloss g,
         LATERAL unnest(string_to_array(g.gloss_text, ';')) WITH ORDINALITY AS seg(text_seg, ord),
         LATERAL cologne_frisch._parse_gloss_segment(seg.text_seg) p
    WHERE btrim(seg.text_seg) <> '';

    GET DIAGNOSTICS n_inserted = ROW_COUNT;
    RETURN n_inserted;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;
COMMENT ON FUNCTION "cologne_frisch"."rebuild_gloss_sense"() IS 'Repopulates frisch.gloss_sense by splitting frisch.gloss.gloss_text on '';'' and extracting gender/number/proper-noun markers from each segment. Returns the number of sense rows inserted. Not needed if the loader already populates gloss_sense directly; provided for reprocessing after manual edits.';

-- ----------------------------
-- View structure for v_entry_full
-- ----------------------------
DROP VIEW IF EXISTS "cologne_frisch"."v_entry_full";
CREATE VIEW "cologne_frisch"."v_entry_full" AS  SELECT e.entry_id,
    e.page_no,
    e.lemma_iast,
    e.homonym_index,
    e.is_root,
    e.is_crossref_only,
    array_agg(DISTINCT p.pos) AS pos_list,
    ( SELECT g.gloss_text
           FROM cologne_frisch.gloss g
          WHERE g.entry_id = e.entry_id AND g.lang_code = 'cs'::text
          ORDER BY g.seq
         LIMIT 1) AS gloss_cs,
    ( SELECT g.gloss_text
           FROM cologne_frisch.gloss g
          WHERE g.entry_id = e.entry_id AND g.lang_code = 'ru'::text
          ORDER BY g.seq
         LIMIT 1) AS gloss_ru,
    ( SELECT g.gloss_text
           FROM cologne_frisch.gloss g
          WHERE g.entry_id = e.entry_id AND g.lang_code = 'en'::text
          ORDER BY g.seq
         LIMIT 1) AS gloss_en,
    vc.conj_class,
    e.parent_entry_id
   FROM cologne_frisch.dict_entry e
     LEFT JOIN cologne_frisch.entry_pos p ON p.entry_id = e.entry_id
     LEFT JOIN cologne_frisch.verb_class vc ON vc.entry_id = e.entry_id
  GROUP BY e.entry_id, e.page_no, e.lemma_iast, e.homonym_index, e.is_root, e.is_crossref_only, vc.conj_class, e.parent_entry_id;

-- ----------------------------
-- View structure for v_verb_family
-- ----------------------------
DROP VIEW IF EXISTS "cologne_frisch"."v_verb_family";
CREATE VIEW "cologne_frisch"."v_verb_family" AS  SELECT root.entry_id AS root_entry_id,
    root.lemma_iast AS root_lemma,
    vc.conj_class,
    d.entry_id AS derivative_entry_id,
    d.lemma_iast AS derivative_lemma,
    rf.preverb,
    rf.surface_form
   FROM cologne_frisch.dict_entry root
     JOIN cologne_frisch.verb_class vc ON vc.entry_id = root.entry_id
     LEFT JOIN cologne_frisch.related_form rf ON rf.base_entry_id = root.entry_id AND rf.derivation_type = 'COMPOUND_VERB'::cologne_frisch.derivation_type
     LEFT JOIN cologne_frisch.dict_entry d ON d.entry_id = rf.entry_id;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_frisch"."cross_reference_id_seq"
OWNED BY "cologne_frisch"."cross_reference"."id";
SELECT setval('"cologne_frisch"."cross_reference_id_seq"', 274, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_frisch"."derived_stem_id_seq"
OWNED BY "cologne_frisch"."derived_stem"."id";
SELECT setval('"cologne_frisch"."derived_stem_id_seq"', 40, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_frisch"."entry_gender_id_seq"
OWNED BY "cologne_frisch"."entry_gender"."id";
SELECT setval('"cologne_frisch"."entry_gender_id_seq"', 3596, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_frisch"."gloss_id_seq"
OWNED BY "cologne_frisch"."gloss"."id";
SELECT setval('"cologne_frisch"."gloss_id_seq"', 23013, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_frisch"."gloss_sense_id_seq"
OWNED BY "cologne_frisch"."gloss_sense"."id";
SELECT setval('"cologne_frisch"."gloss_sense_id_seq"', 28485, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "cologne_frisch"."verb_form_id_seq"
OWNED BY "cologne_frisch"."verb_form"."id";
SELECT setval('"cologne_frisch"."verb_form_id_seq"', 1790, true);

-- ----------------------------
-- Indexes structure for table cross_reference
-- ----------------------------
CREATE INDEX "idx_cross_reference_entry" ON "cologne_frisch"."cross_reference" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_cross_reference_target" ON "cologne_frisch"."cross_reference" USING btree (
  "target_entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Checks structure for table cross_reference
-- ----------------------------
ALTER TABLE "cologne_frisch"."cross_reference" ADD CONSTRAINT "cross_reference_ref_kind_check" CHECK (ref_kind = ANY (ARRAY['see'::text, 'see_sub_verbo'::text, 'confer'::text]));

-- ----------------------------
-- Primary Key structure for table cross_reference
-- ----------------------------
ALTER TABLE "cologne_frisch"."cross_reference" ADD CONSTRAINT "cross_reference_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table derived_stem
-- ----------------------------
CREATE INDEX "idx_derived_stem_entry" ON "cologne_frisch"."derived_stem" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table derived_stem
-- ----------------------------
ALTER TABLE "cologne_frisch"."derived_stem" ADD CONSTRAINT "derived_stem_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table dict_entry
-- ----------------------------
CREATE INDEX "idx_dict_entry_lemma_ascii" ON "cologne_frisch"."dict_entry" USING btree (
  "lemma_ascii" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_dict_entry_lemma_iast" ON "cologne_frisch"."dict_entry" USING btree (
  "lemma_iast" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_dict_entry_lemma_trgm" ON "cologne_frisch"."dict_entry" USING gin (
  "lemma_ascii" COLLATE "pg_catalog"."default" "public"."gin_trgm_ops"
);
CREATE INDEX "idx_dict_entry_page" ON "cologne_frisch"."dict_entry" USING btree (
  "page_no" "pg_catalog"."int2_ops" ASC NULLS LAST
);
CREATE INDEX "idx_dict_entry_parent" ON "cologne_frisch"."dict_entry" USING btree (
  "parent_entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_dict_entry_sort_key1" ON "cologne_frisch"."dict_entry" USING btree (
  "sort_key1" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table dict_entry
-- ----------------------------
ALTER TABLE "cologne_frisch"."dict_entry" ADD CONSTRAINT "dict_entry_pkey" PRIMARY KEY ("entry_id");

-- ----------------------------
-- Indexes structure for table entry_gender
-- ----------------------------
CREATE INDEX "idx_entry_gender_entry" ON "cologne_frisch"."entry_gender" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table entry_gender
-- ----------------------------
ALTER TABLE "cologne_frisch"."entry_gender" ADD CONSTRAINT "entry_gender_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table entry_pos
-- ----------------------------
CREATE INDEX "idx_entry_pos_pos" ON "cologne_frisch"."entry_pos" USING btree (
  "pos" "pg_catalog"."enum_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table entry_pos
-- ----------------------------
ALTER TABLE "cologne_frisch"."entry_pos" ADD CONSTRAINT "entry_pos_pkey" PRIMARY KEY ("entry_id", "pos", "qualifier");

-- ----------------------------
-- Indexes structure for table gloss
-- ----------------------------
CREATE INDEX "idx_gloss_text_trgm" ON "cologne_frisch"."gloss" USING gin (
  "gloss_text" COLLATE "pg_catalog"."default" "public"."gin_trgm_ops"
);
CREATE UNIQUE INDEX "uq_gloss_entry_lang_seq" ON "cologne_frisch"."gloss" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "lang_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "seq" "pg_catalog"."int2_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table gloss
-- ----------------------------
ALTER TABLE "cologne_frisch"."gloss" ADD CONSTRAINT "gloss_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table gloss_sense
-- ----------------------------
CREATE INDEX "idx_gloss_sense_entry" ON "cologne_frisch"."gloss_sense" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_gloss_sense_genders" ON "cologne_frisch"."gloss_sense" USING gin (
  "genders" "pg_catalog"."array_ops"
);
CREATE INDEX "idx_gloss_sense_text_trgm" ON "cologne_frisch"."gloss_sense" USING gin (
  "sense_text" COLLATE "pg_catalog"."default" "public"."gin_trgm_ops"
);
CREATE UNIQUE INDEX "uq_gloss_sense_entry_lang_seq" ON "cologne_frisch"."gloss_sense" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "lang_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "seq" "pg_catalog"."int2_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table gloss_sense
-- ----------------------------
ALTER TABLE "cologne_frisch"."gloss_sense" ADD CONSTRAINT "gloss_sense_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Primary Key structure for table language_code
-- ----------------------------
ALTER TABLE "cologne_frisch"."language_code" ADD CONSTRAINT "language_code_pkey" PRIMARY KEY ("code");

-- ----------------------------
-- Primary Key structure for table lemma_frequency
-- ----------------------------
ALTER TABLE "cologne_frisch"."lemma_frequency" ADD CONSTRAINT "lemma_frequency_pkey" PRIMARY KEY ("lemma_iast");

-- ----------------------------
-- Indexes structure for table related_form
-- ----------------------------
CREATE INDEX "idx_related_form_base" ON "cologne_frisch"."related_form" USING btree (
  "base_entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table related_form
-- ----------------------------
ALTER TABLE "cologne_frisch"."related_form" ADD CONSTRAINT "related_form_pkey" PRIMARY KEY ("entry_id");

-- ----------------------------
-- Checks structure for table verb_class
-- ----------------------------
ALTER TABLE "cologne_frisch"."verb_class" ADD CONSTRAINT "verb_class_conj_class_check" CHECK (conj_class >= 1 AND conj_class <= 10);

-- ----------------------------
-- Primary Key structure for table verb_class
-- ----------------------------
ALTER TABLE "cologne_frisch"."verb_class" ADD CONSTRAINT "verb_class_pkey" PRIMARY KEY ("entry_id");

-- ----------------------------
-- Indexes structure for table verb_form
-- ----------------------------
CREATE INDEX "idx_verb_form_entry" ON "cologne_frisch"."verb_form" USING btree (
  "entry_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_verb_form_text" ON "cologne_frisch"."verb_form" USING btree (
  "form_text" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table verb_form
-- ----------------------------
ALTER TABLE "cologne_frisch"."verb_form" ADD CONSTRAINT "verb_form_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table cross_reference
-- ----------------------------
ALTER TABLE "cologne_frisch"."cross_reference" ADD CONSTRAINT "cross_reference_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "cologne_frisch"."cross_reference" ADD CONSTRAINT "cross_reference_target_entry_id_fkey" FOREIGN KEY ("target_entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table derived_stem
-- ----------------------------
ALTER TABLE "cologne_frisch"."derived_stem" ADD CONSTRAINT "derived_stem_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table dict_entry
-- ----------------------------
ALTER TABLE "cologne_frisch"."dict_entry" ADD CONSTRAINT "dict_entry_parent_entry_id_fkey" FOREIGN KEY ("parent_entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table entry_gender
-- ----------------------------
ALTER TABLE "cologne_frisch"."entry_gender" ADD CONSTRAINT "entry_gender_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table entry_pos
-- ----------------------------
ALTER TABLE "cologne_frisch"."entry_pos" ADD CONSTRAINT "entry_pos_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table gloss
-- ----------------------------
ALTER TABLE "cologne_frisch"."gloss" ADD CONSTRAINT "gloss_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "cologne_frisch"."gloss" ADD CONSTRAINT "gloss_lang_code_fkey" FOREIGN KEY ("lang_code") REFERENCES "cologne_frisch"."language_code" ("code") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table gloss_sense
-- ----------------------------
ALTER TABLE "cologne_frisch"."gloss_sense" ADD CONSTRAINT "gloss_sense_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "cologne_frisch"."gloss_sense" ADD CONSTRAINT "gloss_sense_lang_code_fkey" FOREIGN KEY ("lang_code") REFERENCES "cologne_frisch"."language_code" ("code") ON DELETE NO ACTION ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table related_form
-- ----------------------------
ALTER TABLE "cologne_frisch"."related_form" ADD CONSTRAINT "related_form_base_entry_id_fkey" FOREIGN KEY ("base_entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE NO ACTION ON UPDATE NO ACTION;
ALTER TABLE "cologne_frisch"."related_form" ADD CONSTRAINT "related_form_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verb_class
-- ----------------------------
ALTER TABLE "cologne_frisch"."verb_class" ADD CONSTRAINT "verb_class_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table verb_form
-- ----------------------------
ALTER TABLE "cologne_frisch"."verb_form" ADD CONSTRAINT "verb_form_entry_id_fkey" FOREIGN KEY ("entry_id") REFERENCES "cologne_frisch"."dict_entry" ("entry_id") ON DELETE CASCADE ON UPDATE NO ACTION;
