#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
import_frisch.py
=================

Parses the Frisch Sanskrit dictionary source file (fri_03.txt-style, custom
SGML-ish markup: <L>...<pc>...<k1>...<k2>... ... <LEND>) and loads it into
the PostgreSQL schema defined in frisch_schema.sql (schema "frisch").

This script does NOT create the schema. Run frisch_schema.sql (and, once
data is loaded, frisch_functions.sql) yourself beforehand.

Usage:
    python import_frisch.py

All configuration is via the constants below and the .env file
(DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD).

Two-pass load:
    Pass 1: parse every <L>...<LEND> block, insert dict_entry + all
            per-entry child rows (entry_pos, entry_gender, verb_form,
            verb_class, derived_stem, related_form, cross_reference, gloss,
            gloss_sense). Any reference to another lemma (cross-reference
            target, related-form base root, "+..." parent headword) is
            recorded as raw text only.
    Pass 2: resolve those raw text references to entry_id using an in-memory
            lemma -> entry_id index built during pass 1, and UPDATE the
            relevant FK columns.
"""

import logging
import re
import sys
import unicodedata
from pathlib import Path

import psycopg

# ----------------------------------------------------------------------------
# Configuration
# ----------------------------------------------------------------------------

LOG_FILE = r"C:\MyDev\logs\import_frisch.log"
ENV_FILE_PATH = r"C:\MyDev\samskrtam\.env"
FRISCH_PATH = r"C:\github\sanskrit-lexicon\FRI\fri_01\fri_03.txt"

# Commit every N parsed entries during pass 1 (progress + safety checkpoint).
BATCH_SIZE = 500

# ----------------------------------------------------------------------------
# Grammar constants
#
# These mirror, label for label, the PostgreSQL ENUM types created in
# frisch_schema.sql (frisch.part_of_speech, frisch.gender,
# frisch.grammatical_case, frisch.mood, frisch.number_type, frisch.person,
# frisch.tense, frisch.voice, frisch.derivation_type, frisch.form_type).
# Using these constants (instead of literal strings) throughout the parser
# means a typo turns into a NameError at parse time instead of a silent
# mismatch with the database enum.
# ----------------------------------------------------------------------------


class PartOfSpeech:
    NOUN = "NOUN"
    VERB = "VERB"
    ADJECTIVE = "ADJECTIVE"
    PRONOUN = "PRONOUN"
    ADVERB = "ADVERB"
    PARTICLE = "PARTICLE"
    INDECLINABLE = "INDECLINABLE"
    NUMERAL = "NUMERAL"
    CONJUNCTION = "CONJUNCTION"
    INTERJECTION = "INTERJECTION"
    OTHER = "OTHER"


class Gender:
    MASCULINE = "MASCULINE"
    FEMININE = "FEMININE"
    NEUTER = "NEUTER"
    UNSPECIFIED = "UNSPECIFIED"


class GrammaticalCase:
    NOMINATIVE = "NOMINATIVE"
    ACCUSATIVE = "ACCUSATIVE"
    INSTRUMENTAL = "INSTRUMENTAL"
    DATIVE = "DATIVE"
    ABLATIVE = "ABLATIVE"
    GENITIVE = "GENITIVE"
    LOCATIVE = "LOCATIVE"
    VOCATIVE = "VOCATIVE"


class Mood:
    INDICATIVE = "INDICATIVE"
    OPTATIVE = "OPTATIVE"
    IMPERATIVE = "IMPERATIVE"
    CONDITIONAL = "CONDITIONAL"
    BENEDICTIVE = "BENEDICTIVE"
    INJUNCTIVE = "INJUNCTIVE"


class NumberType:
    SINGULAR = "SINGULAR"
    DUAL = "DUAL"
    PLURAL = "PLURAL"


class Person:
    FIRST = "FIRST"
    SECOND = "SECOND"
    THIRD = "THIRD"


class Tense:
    PRESENT = "PRESENT"
    IMPERFECT = "IMPERFECT"
    PERFECT = "PERFECT"
    AORIST = "AORIST"
    FUTURE = "FUTURE"
    PERIPHRASTIC_FUTURE = "PERIPHRASTIC_FUTURE"
    CONDITIONAL = "CONDITIONAL"
    BENEDICTIVE = "BENEDICTIVE"


class Voice:
    ACTIVE = "ACTIVE"
    MIDDLE = "MIDDLE"
    PASSIVE = "PASSIVE"


class DerivationType:
    SIMPLE_INFLECTION = "SIMPLE_INFLECTION"
    ABSOLUTIVE = "ABSOLUTIVE"
    PARTICIPLE = "PARTICIPLE"
    GERUNDIVE = "GERUNDIVE"
    INFINITIVE = "INFINITIVE"
    CAUSATIVE = "CAUSATIVE"
    DESIDERATIVE = "DESIDERATIVE"
    DENOMINATIVE = "DENOMINATIVE"
    COMPOUND_VERB = "COMPOUND_VERB"
    OTHER = "OTHER"


class FormType:
    FINITE = "FINITE"
    INFINITIVE = "INFINITIVE"
    ABSOLUTIVE = "ABSOLUTIVE"
    PARTICIPLE = "PARTICIPLE"
    GERUNDIVE = "GERUNDIVE"
    OTHER_NONFINITE = "OTHER_NONFINITE"
    NOMINAL = "NOMINAL"
    ADJECTIVAL = "ADJECTIVAL"
    PRONOMINAL = "PRONOMINAL"
    INDECLINABLE = "INDECLINABLE"


# POS abbreviation -> (PartOfSpeech constant, qualifier or None, Gender constant or None)
POS_ABBR_MAP = {
    "m.": (PartOfSpeech.NOUN, None, Gender.MASCULINE),
    "f.": (PartOfSpeech.NOUN, None, Gender.FEMININE),
    "n.": (PartOfSpeech.NOUN, None, Gender.NEUTER),
    "adj.": (PartOfSpeech.ADJECTIVE, None, None),
    "adv.": (PartOfSpeech.ADVERB, None, None),
    "pron.": (PartOfSpeech.PRONOUN, None, None),
    "num.": (PartOfSpeech.NUMERAL, None, None),
    "prep.": (PartOfSpeech.OTHER, "preposition", None),
    "pcl.": (PartOfSpeech.PARTICLE, None, None),
    "conj.": (PartOfSpeech.CONJUNCTION, None, None),
    "interj.": (PartOfSpeech.INTERJECTION, None, None),
    "indecl.": (PartOfSpeech.INDECLINABLE, None, None),
}
POS_ABBR_RE = re.compile(
    r"\b(" + "|".join(re.escape(a) for a in sorted(POS_ABBR_MAP, key=len, reverse=True)) + r")"
)

# Principal-part tag -> ('verb_form' | 'derived_stem', dict of column values)
# Untagged 3rd-sg. present forms right after the lemma are handled separately.
DEFAULT_FINITE_3SG = {
    "mood": Mood.INDICATIVE,
    "person": Person.THIRD,
    "number_type": NumberType.SINGULAR,
}

PRINCIPAL_PART_TAG_MAP = {
    "inf": ("verb_form", {"form_type": FormType.INFINITIVE}),
    "pp": ("verb_form", {"form_type": FormType.PARTICIPLE, "voice": Voice.PASSIVE}),
    "pf": ("verb_form", {"form_type": FormType.FINITE, "tense": Tense.PERFECT, **DEFAULT_FINITE_3SG}),
    "fut": ("verb_form", {"form_type": FormType.FINITE, "tense": Tense.FUTURE, "voice": Voice.ACTIVE, **DEFAULT_FINITE_3SG}),
    "aor": ("verb_form", {"form_type": FormType.FINITE, "tense": Tense.AORIST, "voice": Voice.ACTIVE, **DEFAULT_FINITE_3SG}),
    "pass": ("verb_form", {"form_type": FormType.FINITE, "tense": Tense.PRESENT, "voice": Voice.PASSIVE, **DEFAULT_FINITE_3SG}),
    "caus": ("derived_stem", {"derivation_type": DerivationType.CAUSATIVE}),
    "des": ("derived_stem", {"derivation_type": DerivationType.DESIDERATIVE}),
    "intens": ("derived_stem", {"derivation_type": DerivationType.OTHER}),
}

CASE_ABBR_MAP = {
    "acc": GrammaticalCase.ACCUSATIVE,
    "abl": GrammaticalCase.ABLATIVE,
    "gen": GrammaticalCase.GENITIVE,
    "lok": GrammaticalCase.LOCATIVE,
    "loc": GrammaticalCase.LOCATIVE,
    "dat": GrammaticalCase.DATIVE,
    "instr": GrammaticalCase.INSTRUMENTAL,
}

GENDER_LETTER_MAP = {"m": Gender.MASCULINE, "f": Gender.FEMININE, "n": Gender.NEUTER}
NUMBER_ABBR_MAP = {"pl": NumberType.PLURAL, "du": NumberType.DUAL, "sg": NumberType.SINGULAR}

# ----------------------------------------------------------------------------
# Logging
# ----------------------------------------------------------------------------


def setup_logging(log_file: str) -> logging.Logger:
    log_path = Path(log_file)
    log_path.parent.mkdir(parents=True, exist_ok=True)

    logger = logging.getLogger("import_frisch")
    logger.setLevel(logging.DEBUG)
    logger.handlers.clear()

    fmt = logging.Formatter(
        "%(asctime)s %(levelname)-8s %(message)s", datefmt="%Y-%m-%d %H:%M:%S"
    )

    file_handler = logging.FileHandler(log_path, encoding="utf-8")
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(fmt)

    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(fmt)

    logger.addHandler(file_handler)
    logger.addHandler(console_handler)
    return logger


log = setup_logging(LOG_FILE)

# ----------------------------------------------------------------------------
# .env handling
# ----------------------------------------------------------------------------


def read_env(path: str) -> dict:
    """Minimal .env parser: KEY=VALUE per line, '#' comments, blank lines ok."""
    env = {}
    env_path = Path(path)
    if not env_path.exists():
        log.error("ENV file not found: %s", path)
        raise FileNotFoundError(f"ENV file not found: {path}")

    with env_path.open(encoding="utf-8") as f:
        for line_no, raw_line in enumerate(f, start=1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                log.warning(".env line %d ignored (no '='): %r", line_no, raw_line)
                continue
            key, _, value = line.partition("=")
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            env[key] = value

    log.info("Loaded %d variables from %s", len(env), path)
    return env


def required(env: dict, key: str) -> str:
    value = env.get(key)
    if not value:
        log.error("Missing required env variable: %s", key)
        raise ValueError(f"Missing required env variable: {key}")
    return value


def create_db_connection(env):
    return psycopg.connect(
        host=required(env, "DB_HOST"),
        port=int(env.get("DB_PORT", "5432")),
        dbname=required(env, "DB_NAME"),
        user=required(env, "DB_USER"),
        password=required(env, "DB_PASSWORD"),
    )


# ----------------------------------------------------------------------------
# Source parsing
# ----------------------------------------------------------------------------

BLOCK_RE = re.compile(
    r"<L>(?P<id>\d+)<pc>(?P<pc>\d+)<k1>(?P<k1>.*?)<k2>(?P<k2>.*?)\r?\n"
    r"(?P<body>.*?)<LEND>",
    re.DOTALL,
)

DIV_LANG_RE = re.compile(
    r'<div n="1"/>\s*(?P<num>\d)\s*<lang n="(?P<lang>\w+)">(?P<text>.*?)</lang>',
    re.DOTALL,
)

LANG_MAP = {"czech": "cs", "russian": "ru", "english": "en"}

ROMAN_RE = re.compile(r"^(?P<roman>I{1,3}|IV|V|VI{0,3}|IX|X)\s+")
ROMAN_VALUES = {
    "I": 1, "II": 2, "III": 3, "IV": 4, "V": 5,
    "VI": 6, "VII": 7, "VIII": 8, "IX": 9, "X": 10,
}

ROOT_MARK_RE = re.compile(r"^[√ѵ✓]\s*")

GENDER_VARIANT_RE = re.compile(r"\((?P<gender>[mfn])\.\s*(?P<suffixes>-\S+(?:,\s*-\S+)*)\)")

CROSSREF_RE = re.compile(r"\bv\.\s+(?P<target>.+)$")
VERB_CLASS_RE = re.compile(r"\bv\.\s+(?P<roman>I{1,3}|IV|V|VI{0,3}|IX|X)\s+(?P<root>\S+)")

PRINCIPAL_PARTS_RE = re.compile(
    r"\b(?P<tag>ved|inf|pp|caus|pass|des|intens|fut|aor|pf)\.\s+(?P<form>[^\s,;()]+)"
)

# " +ava (avakar)"  /  " +pass. kriyate"  /  " +caus. kārayati, -te"
# " +agnitva n. abstr."  /  " +aṅkuravant poss."  /  " +prep. + acc., abl."
RELATED_FORM_RE = re.compile(r"^\s*\+\s*(?P<rest>.+)$")
GOVERNMENT_RE = re.compile(r"^prep\.\s*\+\s*(?P<cases>.+)$")
PREVERB_RE = re.compile(r"^(?P<preverb>[a-zA-Zāīūṛṝḷḹṃḥṅñṭḍṇśṣ]+)\s*\((?P<surface>[^)]+)\)")

# Gender/number/proper-noun marker at the start of a ';'-delimited gloss segment,
# e.g. "m. bůh", "n. pr. jméno démona", "m. pl. neloupané zrní", "m., n. pr. ..."
GLOSS_SENSE_MARKER_RE = re.compile(
    r"^(?P<gm>(?:[mfn]\.,?\s*)+)(?:(?P<number>pl|du|sg)\.\s*)?(?:(?P<proper>pr)\.\s*)?(?P<rest>.*)$",
    re.DOTALL,
)


def strip_accents_lower(text: str) -> str:
    norm = unicodedata.normalize("NFKD", text)
    return "".join(c for c in norm if not unicodedata.combining(c)).lower()


# ----------------------------------------------------------------------------
# Block / headline parsing
# ----------------------------------------------------------------------------


def parse_block(match: re.Match) -> dict:
    entry_id = int(match.group("id"))
    page_no = int(match.group("pc"))
    k1 = match.group("k1").strip()
    k2 = match.group("k2").strip()
    body = match.group("body")

    lines = body.split("\r\n") if "\r\n" in body else body.split("\n")
    lines = [ln for ln in lines if ln.strip() != ""]

    headline = ""
    gloss_blob = ""
    for i, ln in enumerate(lines):
        if ln.lstrip().startswith("<div"):
            gloss_blob = "\n".join(lines[i:])
            break
        headline = (headline + " " + ln).strip() if headline else ln.strip()
    if not gloss_blob:
        gloss_blob = body

    glosses = []
    for m in DIV_LANG_RE.finditer(gloss_blob):
        lang = LANG_MAP.get(m.group("lang"))
        if lang is None:
            log.warning("Entry %d: unknown language tag %r", entry_id, m.group("lang"))
            continue
        glosses.append({"lang_code": lang, "seq": int(m.group("num")), "text": m.group("text").strip()})

    return {
        "entry_id": entry_id,
        "page_no": page_no,
        "k1": k1,
        "k2": k2,
        "raw_headline": headline,
        "glosses": glosses,
    }


def parse_headline(entry_id: int, headline: str) -> dict:
    """Extract grammatical structure from the raw headline text."""
    result = {
        "homonym_index": None,
        "is_root": False,
        "lemma_iast": None,
        "pos_entries": [],        # list of (PartOfSpeech, qualifier)
        "genders": [],            # list of (Gender, stem_suffix or None)
        "grammar_note": None,
        "is_related_form": False,
        "related_form": None,     # dict for related_form table, if applicable
        "verb_forms": [],         # list of dicts, columns for frisch.verb_form
        "derived_stems": [],      # list of dicts, columns for frisch.derived_stem
        "verb_class": None,       # int, if this entry itself carries a class
        "cross_refs": [],         # list of (ref_kind, target_raw)
        "is_crossref_only": False,
    }

    text = headline.strip()

    # 1. Related/derived sub-entry line: " +..."
    rel_match = RELATED_FORM_RE.match(headline)
    if rel_match:
        result["is_related_form"] = True
        result["related_form"] = classify_related_form(entry_id, rel_match.group("rest"))
        return result

    # 2. Homonym roman numeral prefix: "I aṅga pcl."
    m = ROMAN_RE.match(text)
    if m:
        roman = m.group("roman")
        result["homonym_index"] = ROMAN_VALUES.get(roman)
        text = text[m.end():]

    # 3. Root marker √ / ѵ / ✓
    m = ROOT_MARK_RE.match(text)
    if m:
        result["is_root"] = True
        text = text[m.end():]

    # 4. Lemma = first whitespace-delimited token (strip trailing */-/,)
    m = re.match(r"(?P<lemma>\S+)", text)
    if m:
        result["lemma_iast"] = m.group("lemma").rstrip("*").rstrip(",").rstrip("-").strip()
    else:
        log.warning("Entry %d: could not extract lemma from headline: %r", entry_id, headline)

    # 5. Cross-reference: "akārya v. akartavya" / "√kṛ karoti v. I kar"
    cm = CROSSREF_RE.search(text)
    if cm:
        target = cm.group("target").strip()
        result["cross_refs"].append(("see", target))
        pre_v_text = text[: cm.start()].strip()
        pre_v_tokens = pre_v_text.split()
        if len(pre_v_tokens) <= 1:  # just the lemma itself (no POS/forms before "v.")
            result["is_crossref_only"] = True

    # 6. Verb class declared inline: "v. I kar"
    vcm = VERB_CLASS_RE.search(text)
    if vcm:
        result["verb_class"] = ROMAN_VALUES.get(vcm.group("roman"))

    # 7. POS abbreviations (also seeds gender for m./f./n.)
    #    Gender-variant parentheticals like "(f. -ī)" are masked out first so
    #    their "f." isn't double-counted as the headword's own primary gender.
    text_for_pos = GENDER_VARIANT_RE.sub(lambda m: " " * (m.end() - m.start()), text)
    for pm in POS_ABBR_RE.finditer(text_for_pos):
        pos_code, qualifier, gender = POS_ABBR_MAP[pm.group(1)]
        result["pos_entries"].append((pos_code, qualifier))
        if gender is not None:
            result["genders"].append((gender, None))  # None suffix = primary gender

    if result["is_root"] or result["verb_class"] is not None:
        result["pos_entries"].append((PartOfSpeech.VERB, None))

    # 8. Gender-conditioned stem variants: "(f. -ī, -ā)"
    for gm in GENDER_VARIANT_RE.finditer(text):
        gender = GENDER_LETTER_MAP[gm.group("gender")]
        suffixes = [s.strip() for s in gm.group("suffixes").split(",")]
        for suf in suffixes:
            result["genders"].append((gender, suf))

    # 9. Verb principal parts: ved./inf./pp./caus./pass./des./intens./fut./aor./pf.
    is_vedic_next = False
    for pp in PRINCIPAL_PARTS_RE.finditer(text):
        tag = pp.group("tag")
        form_text = pp.group("form")

        if tag == "ved":
            is_vedic_next = True
            # "ved." on its own with no other tag means an alternate present
            # stem form; store it as a plain finite present form.
            result["verb_forms"].append({
                "form_type": FormType.FINITE,
                "tense": Tense.PRESENT,
                "mood": Mood.INDICATIVE,
                "voice": Voice.MIDDLE if form_text.endswith("te") else Voice.ACTIVE,
                "person": Person.THIRD,
                "number_type": NumberType.SINGULAR,
                "is_vedic": True,
                "form_text": form_text,
                "raw_tag": "ved.",
            })
            continue

        kind, cols = PRINCIPAL_PART_TAG_MAP.get(tag, (None, None))
        if kind is None:
            log.debug("Entry %d: unrecognized principal-part tag %r", entry_id, tag)
            continue

        row = dict(cols)
        row["raw_tag"] = f"{tag}."
        if kind == "verb_form":
            row["form_text"] = form_text
            row["is_vedic"] = is_vedic_next
            result["verb_forms"].append(row)
        else:
            row["surface_form"] = form_text
            result["derived_stems"].append(row)
        is_vedic_next = False

    # 10. Present stem forms directly after the lemma, e.g. "karoti, kurute,"
    #     heuristic: finite forms (ending -ti/-te) appearing before any tag.
    after_lemma = text
    if result["lemma_iast"]:
        idx = text.find(result["lemma_iast"])
        if idx != -1:
            after_lemma = text[idx + len(result["lemma_iast"]):]
    # only look in the segment before the first recognized tag, to avoid
    # re-capturing forms already attributed to inf./pp./fut./etc above
    first_tag_pos = len(after_lemma)
    tag_m = re.search(r"\b(?:ved|inf|pp|caus|pass|des|intens|fut|aor|pf)\.", after_lemma)
    if tag_m:
        first_tag_pos = tag_m.start()
    present_zone = after_lemma[:first_tag_pos]
    for fm in re.finditer(r"\b([a-zāīūṛṝḷḹṃḥṅñṭḍṇśṣ]+(?:ti|te))\b", present_zone):
        form = fm.group(1)
        result["verb_forms"].append({
            "form_type": FormType.FINITE,
            "tense": Tense.PRESENT,
            "mood": Mood.INDICATIVE,
            "voice": Voice.MIDDLE if form.endswith("te") else Voice.ACTIVE,
            "person": Person.THIRD,
            "number_type": NumberType.SINGULAR,
            "is_vedic": False,
            "form_text": form,
            "raw_tag": None,
        })

    return result


def classify_related_form(entry_id: int, rest: str) -> dict:
    """Classify a ' +...' related-form line."""
    rest = rest.strip()
    out = {
        "derivation_type": DerivationType.OTHER,
        "preverb": None,
        "surface_form": None,
        "case_government": None,
        "raw_text": rest,
    }

    gm = GOVERNMENT_RE.match(rest)
    if gm:
        cases = []
        for tok in re.split(r",\s*", gm.group("cases")):
            tok = tok.strip().rstrip(".")
            cases.append(CASE_ABBR_MAP.get(tok, tok))
        out["derivation_type"] = DerivationType.OTHER
        out["case_government"] = cases
        return out

    if rest.startswith("pass."):
        out["derivation_type"] = DerivationType.SIMPLE_INFLECTION
        out["surface_form"] = rest[len("pass."):].strip().split(",")[0].strip() or None
        return out
    if rest.startswith("caus."):
        out["derivation_type"] = DerivationType.CAUSATIVE
        out["surface_form"] = rest[len("caus."):].strip().split(",")[0].strip() or None
        return out
    if rest.startswith("des."):
        out["derivation_type"] = DerivationType.DESIDERATIVE
        out["surface_form"] = rest[len("des."):].strip().split(",")[0].strip() or None
        return out
    if rest.startswith("intens."):
        out["derivation_type"] = DerivationType.OTHER
        out["surface_form"] = rest[len("intens."):].strip().split(",")[0].strip() or None
        return out
    if "n. abstr." in rest:
        out["derivation_type"] = DerivationType.OTHER
        out["surface_form"] = rest.split("n. abstr.")[0].strip() or None
        return out
    if re.search(r"\bposs\.", rest):
        out["derivation_type"] = DerivationType.OTHER
        out["surface_form"] = rest.split("poss.")[0].strip() or None
        return out

    pm = PREVERB_RE.match(rest)
    if pm:
        out["derivation_type"] = DerivationType.COMPOUND_VERB
        out["preverb"] = pm.group("preverb")
        out["surface_form"] = pm.group("surface").strip()
        return out

    log.debug("Entry %d: unclassified related form: %r", entry_id, rest)
    return out


def parse_gloss_senses(entry_id: int, lang_code: str, text: str) -> list:
    """Split a gloss text into gender-tagged senses (see GLOSS_SENSE_MARKER_RE)."""
    senses = []
    segments = [s.strip() for s in text.split(";") if s.strip()]
    for seq, seg in enumerate(segments, start=1):
        m = GLOSS_SENSE_MARKER_RE.match(seg)
        if not m or not m.group("gm"):
            senses.append({
                "lang_code": lang_code, "seq": seq, "genders": None,
                "number_note": None, "is_proper_noun": False, "sense_text": seg,
            })
            continue
        letters = re.findall(r"[mfn]", m.group("gm"))
        genders = [GENDER_LETTER_MAP[letter] for letter in letters]
        number_note = NUMBER_ABBR_MAP.get(m.group("number"))
        senses.append({
            "lang_code": lang_code,
            "seq": seq,
            "genders": genders,
            "number_note": number_note,
            "is_proper_noun": bool(m.group("proper")),
            "sense_text": m.group("rest").strip(),
        })
    return senses


# ----------------------------------------------------------------------------
# Loading (pass 1: insert, pass 2: resolve references)
# ----------------------------------------------------------------------------


def insert_entry(cur, entry: dict, parsed: dict):
    cur.execute(
        """
        INSERT INTO frisch.dict_entry
        (entry_id, page_no, sort_key1, sort_key2, homonym_index, is_root,
         lemma_iast, lemma_ascii, is_related_form, is_crossref_only,
         grammar_note, raw_headline)
        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """,
        (
            entry["entry_id"],
            entry["page_no"],
            entry["k1"],
            entry["k2"],
            parsed["homonym_index"],
            parsed["is_root"],
            parsed["lemma_iast"] or entry["k2"],
            strip_accents_lower(parsed["lemma_iast"] or entry["k2"]),
            parsed["is_related_form"],
            parsed["is_crossref_only"],
            parsed.get("grammar_note"),
            entry["raw_headline"],
        ),
    )

    for pos_code, qualifier in parsed["pos_entries"]:
        cur.execute(
            """
            INSERT INTO frisch.entry_pos (entry_id, pos, qualifier)
            VALUES (%s, %s::frisch.part_of_speech, %s)
            ON CONFLICT DO NOTHING
            """,
            (entry["entry_id"], pos_code, qualifier or ""),
        )

    for gender, stem_suffix in parsed["genders"]:
        cur.execute(
            """
            INSERT INTO frisch.entry_gender (entry_id, gender, stem_suffix)
            VALUES (%s, %s::frisch.gender, %s)
            """,
            (entry["entry_id"], gender, stem_suffix),
        )

    for seq, vf in enumerate(parsed["verb_forms"], start=1):
        cur.execute(
            """
            INSERT INTO frisch.verb_form
            (entry_id, form_type, tense, mood, voice, person, number_type,
             is_vedic, form_text, raw_tag, seq)
            VALUES (%s, %s::frisch.form_type, %s::frisch.tense, %s::frisch.mood,
                    %s::frisch.voice, %s::frisch.person, %s::frisch.number_type,
                    %s, %s, %s, %s)
            """,
            (
                entry["entry_id"],
                vf["form_type"],
                vf.get("tense"),
                vf.get("mood"),
                vf.get("voice"),
                vf.get("person"),
                vf.get("number_type"),
                vf.get("is_vedic", False),
                vf["form_text"],
                vf.get("raw_tag"),
                seq,
            ),
        )

    for seq, ds in enumerate(parsed["derived_stems"], start=1):
        cur.execute(
            """
            INSERT INTO frisch.derived_stem (entry_id, derivation_type, surface_form, raw_tag, seq)
            VALUES (%s, %s::frisch.derivation_type, %s, %s, %s)
            """,
            (entry["entry_id"], ds["derivation_type"], ds.get("surface_form"), ds.get("raw_tag"), seq),
        )

    if parsed["verb_class"] is not None:
        cur.execute(
            """
            INSERT INTO frisch.verb_class (entry_id, conj_class)
            VALUES (%s, %s)
            ON CONFLICT (entry_id) DO UPDATE SET conj_class = EXCLUDED.conj_class
            """,
            (entry["entry_id"], parsed["verb_class"]),
        )

    for ref_kind, target_raw in parsed["cross_refs"]:
        cur.execute(
            """
            INSERT INTO frisch.cross_reference (entry_id, ref_kind, target_raw)
            VALUES (%s, %s, %s)
            """,
            (entry["entry_id"], ref_kind, target_raw),
        )

    if parsed["is_related_form"] and parsed["related_form"] is not None:
        rf = parsed["related_form"]
        cur.execute(
            """
            INSERT INTO frisch.related_form
            (entry_id, derivation_type, preverb, surface_form, case_government, raw_text)
            VALUES (%s, %s::frisch.derivation_type, %s, %s,
                    (%s)::text[]::frisch.grammatical_case[], %s)
            """,
            (
                entry["entry_id"],
                rf["derivation_type"],
                rf["preverb"],
                rf["surface_form"],
                rf["case_government"],
                rf["raw_text"],
            ),
        )

    for g in entry["glosses"]:
        cur.execute(
            """
            INSERT INTO frisch.gloss (entry_id, lang_code, seq, gloss_text)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (entry_id, lang_code, seq) DO NOTHING
            """,
            (entry["entry_id"], g["lang_code"], g["seq"], g["text"]),
        )

        for sense in parse_gloss_senses(entry["entry_id"], g["lang_code"], g["text"]):
            cur.execute(
                """
                INSERT INTO frisch.gloss_sense
                (entry_id, lang_code, seq, genders, number_note, is_proper_noun, sense_text)
                VALUES (%s, %s, %s, (%s)::text[]::frisch.gender[], %s::frisch.number_type, %s, %s)
                ON CONFLICT (entry_id, lang_code, seq) DO NOTHING
                """,
                (
                    entry["entry_id"],
                    sense["lang_code"],
                    sense["seq"],
                    sense["genders"],
                    sense["number_note"],
                    sense["is_proper_noun"],
                    sense["sense_text"],
                ),
            )


def load_pass1(conn, source_path: str):
    log.info("Reading source file: %s", source_path)
    text = Path(source_path).read_text(encoding="utf-8")

    matches = list(BLOCK_RE.finditer(text))
    log.info("Found %d entry blocks", len(matches))

    last_headword_entry_id = None  # nearest preceding non-related entry, for parent_entry_id
    processed = 0
    warnings_count = 0

    with conn.cursor() as cur:
        for match in matches:
            entry = parse_block(match)
            parsed = parse_headline(entry["entry_id"], entry["raw_headline"])

            if not parsed["lemma_iast"] and not parsed["is_related_form"]:
                log.warning("Entry %d has no parsed lemma; raw=%r", entry["entry_id"], entry["raw_headline"])
                warnings_count += 1

            insert_entry(cur, entry, parsed)

            if parsed["is_related_form"]:
                if last_headword_entry_id is not None:
                    cur.execute(
                        "UPDATE frisch.dict_entry SET parent_entry_id = %s WHERE entry_id = %s",
                        (last_headword_entry_id, entry["entry_id"]),
                    )
                    cur.execute(
                        "UPDATE frisch.related_form SET base_entry_id = %s WHERE entry_id = %s",
                        (last_headword_entry_id, entry["entry_id"]),
                    )
                else:
                    log.warning(
                        "Entry %d is a related form but no preceding headword found", entry["entry_id"]
                    )
            else:
                last_headword_entry_id = entry["entry_id"]

            processed += 1
            if processed % BATCH_SIZE == 0:
                conn.commit()
                log.info("Committed after %d / %d entries", processed, len(matches))

    conn.commit()
    log.info(
        "Pass 1 complete: %d entries processed, %d lemma-extraction warnings",
        processed, warnings_count,
    )


def load_pass2_resolve_references(conn):
    log.info("Pass 2: resolving cross-references and related-form targets")

    with conn.cursor() as cur:
        cur.execute("SELECT entry_id, lemma_ascii FROM frisch.dict_entry")
        rows = cur.fetchall()

    lemma_index: dict[str, list[int]] = {}
    for entry_id, lemma_ascii in rows:
        lemma_index.setdefault(lemma_ascii, []).append(entry_id)
    log.info("Built lemma index with %d distinct keys over %d entries", len(lemma_index), len(rows))

    def resolve(target_raw: str):
        first = target_raw.split(",")[0].strip()
        tokens = first.split()
        candidate = tokens[-1] if tokens else first
        candidate = candidate.strip("-")
        key = strip_accents_lower(candidate)
        matches = lemma_index.get(key)
        if not matches:
            return None
        return matches[0]

    unresolved_refs = 0
    resolved_refs = 0
    with conn.cursor() as cur:
        cur.execute("SELECT id, target_raw FROM frisch.cross_reference WHERE target_entry_id IS NULL")
        pending = cur.fetchall()

    with conn.cursor() as upd:
        for ref_id, target_raw in pending:
            target_id = resolve(target_raw)
            if target_id is None:
                unresolved_refs += 1
                continue
            upd.execute(
                "UPDATE frisch.cross_reference SET target_entry_id = %s WHERE id = %s",
                (target_id, ref_id),
            )
            resolved_refs += 1

    conn.commit()
    log.info(
        "Cross-reference resolution: %d resolved, %d unresolved (left NULL for manual review)",
        resolved_refs, unresolved_refs,
    )


def print_summary(conn):
    tables = [
        "dict_entry", "entry_pos", "entry_gender", "verb_form", "derived_stem",
        "related_form", "cross_reference", "gloss", "gloss_sense",
    ]
    counts = {}
    with conn.cursor() as cur:
        for t in tables:
            cur.execute(f"SELECT count(*) FROM frisch.{t}")
            (counts[t],) = cur.fetchone()
        cur.execute("SELECT count(*) FROM frisch.cross_reference WHERE target_entry_id IS NOT NULL")
        (counts["cross_reference_resolved"],) = cur.fetchone()

    log.info("=" * 60)
    log.info("Import summary:")
    for t in tables:
        log.info("  %-20s %d", t, counts[t])
    log.info("  %-20s %d", "cross_reference resolved", counts["cross_reference_resolved"])
    log.info("=" * 60)


# ----------------------------------------------------------------------------
# Main
# ----------------------------------------------------------------------------


def main():
    log.info("=== import_frisch.py started ===")
    try:
        env = read_env(ENV_FILE_PATH)
        conn = create_db_connection(env)
        log.info("Connected to database %s@%s", env.get("DB_NAME"), env.get("DB_HOST"))
    except Exception:
        log.exception("Failed to initialize DB connection")
        sys.exit(1)

    try:
        load_pass1(conn, FRISCH_PATH)
        load_pass2_resolve_references(conn)
        print_summary(conn)

    except Exception:
        log.exception("Import failed, rolling back current transaction")
        conn.rollback()
        sys.exit(1)
    finally:
        conn.close()
        log.info("=== import_frisch.py finished ===")


if __name__ == "__main__":
    main()