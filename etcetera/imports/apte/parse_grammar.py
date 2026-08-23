"""
Pass 2 — grammatical parsing of entries already loaded by parse_structure.py.

STATUS: heuristic / needs calibration.
I could not fetch the raw ap.txt content directly (GitHub blocks automated
access to it), so grammar-span extraction below is built from the documented
CDSL markup conventions (<lex>, <ab>) and the one real example available
(csl-orig/v02/ae/ae.txt line 65: "...Ⓒ<lex>v. t.</lex>Ⓓ<s>apahf</s> 1 <ab>P</ab>.").
Before trusting the output at scale:
    1. Run this on a sample of ~50 entries.
    2. Inspect unmatched_tokens AND a random sample of *matched* facts.
    3. Adjust GRAMMAR_SPAN_RE / the fast-path verb regex to fit what you
       actually see in ap.txt's <lex>/<ab> tags.

Strategy:
    - If the entry has explicit <lex>...</lex> or <ab>...</ab> tags, use
      their content as the grammar span (high confidence).
    - Otherwise fall back to a fast-path regex for the extremely common
      "<digit> <P|A|U|Ubh>." verb-class+pada pattern appearing right after
      the headword.
    - Every span is tokenized and matched token-by-token against
      grammar_abbreviations (loaded once from DB into a dict).
    - Unmatched tokens are logged to unmatched_tokens rather than dropped.
"""

import re
from collections import defaultdict

from psycopg2.extras import execute_values

from config import log, BATCH_SIZE

# ----------------------------------------------------------------------------
# Grammar span extraction
# ----------------------------------------------------------------------------

LEX_TAG_RE = re.compile(r"<lex>(.*?)</lex>", re.S)
AB_TAG_RE = re.compile(r"<ab>(.*?)</ab>", re.S)

# Fast path: "<class> <pada>." e.g. "10 U.", "1 P.", "4 Ā."
# Applied to the raw text immediately following the last of <k1>/<k2>/<hom>.
VERB_FASTPATH_RE = re.compile(
    r"(?P<class>\d{1,2})\s*(?P<pada>P|Ā|A|U|Ubh)\.?"
)

# Where the "header" (structural markup) ends and entry content begins:
# last of <k1>...</k1-ish>, i.e. after the final </hom> tag, or after <k2>
# value up to the next '<' or start of text. We approximate by finding the
# offset right after the <hom>N or <k2>... field.
HEADER_END_RE = re.compile(r"(?:<hom>[^<]+|<k2>[^<]+|<k1>[^<]+)")


def extract_grammar_span(raw_markup: str) -> tuple[str, float]:
    """
    Returns (span_text, confidence). confidence=1.0 for explicit <lex>/<ab>
    tags, 0.6 for the header-adjacent fallback heuristic.
    """
    lex_matches = LEX_TAG_RE.findall(raw_markup)
    ab_matches = AB_TAG_RE.findall(raw_markup)
    if lex_matches or ab_matches:
        return " ".join(lex_matches + ab_matches), 1.0

    # Fallback: look at ~40 chars right after the header fields for the
    # verb fast-path pattern or a bare abbreviation cluster.
    last_header = None
    for m in HEADER_END_RE.finditer(raw_markup):
        last_header = m
    if last_header is None:
        return "", 0.0

    tail_start = last_header.end()
    tail = raw_markup[tail_start:tail_start + 60]
    tail = re.sub(r"^[^\w]*", "", tail)  # strip leading punctuation/newlines
    return tail, 0.6


def tokenize_span(span: str) -> list[str]:
    """
    Split a grammar span into candidate tokens. Keeps periods attached
    (abbreviations are looked up with periods normalized away), splits
    on whitespace and commas.
    """
    raw_tokens = re.split(r"[,\s]+", span.strip())
    return [t for t in raw_tokens if t]


def normalize_token(token: str) -> str:
    return token.strip().strip(".").lower()


# ----------------------------------------------------------------------------
# Abbreviation lookup
# ----------------------------------------------------------------------------

def load_abbreviation_index(conn) -> dict:
    """
    Returns {normalized_token: [(abbrev_id, category, mapped_value), ...]}
    A token can map to multiple categories (e.g. 'm.' -> part_of_speech
    AND gender_type), hence the list.
    """
    index = defaultdict(list)
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, abbrev_normalized, category, mapped_value FROM grammar_abbreviations"
        )
        for abbrev_id, norm, category, mapped_value in cur.fetchall():
            index[norm].append((abbrev_id, category, mapped_value))
    log.info("Loaded %d distinct abbreviation tokens", len(index))
    return index


# ----------------------------------------------------------------------------
# Fact assembly
# ----------------------------------------------------------------------------

# Map category name -> grammar_facts column name
CATEGORY_TO_COLUMN = {
    "pada_type": "pada",
    "verb_class": "verb_class",
    "derivation_type": "derivation_type",
    "part_of_speech": "part_of_speech",
    "gender_type": "gender",
    "number_type": "grammatical_number",
    "grammatical_case": "grammatical_case",
    "tense_type": "tense",
    "mood_type": "mood",
    "form_type": "form_type",
    "voice_type": "voice",
    "person_type": "person",
}


def build_fact_row(entry_id: int, span: str, confidence: float,
                    matches: list[tuple], unmatched: list[str]):
    """
    Collapses all matched (category, value, abbrev_id) tuples for one span
    into a single grammar_facts row (one row per grammatical "reading").
    If you expect multiple distinct readings per entry (e.g. homonyms with
    different POS), call this once per sense/reading instead of once per
    whole entry.
    """
    fact = {col: None for col in CATEGORY_TO_COLUMN.values()}
    source_ids = []
    for abbrev_id, category, mapped_value in matches:
        col = CATEGORY_TO_COLUMN.get(category)
        if col is None:
            continue
        fact[col] = mapped_value
        source_ids.append(abbrev_id)

    return (
        entry_id,
        fact["part_of_speech"], fact["derivation_type"], fact["form_type"],
        fact["gender"], fact["grammatical_case"], fact["mood"],
        fact["grammatical_number"], fact["person"], fact["tense"],
        fact["voice"], fact["verb_class"], fact["pada"],
        source_ids or None,
        span,
        confidence,
        (f"{len(unmatched)} unmatched token(s): {unmatched}" if unmatched else None),
    )


INSERT_FACT_SQL = """
    INSERT INTO grammar_facts
        (entry_id, part_of_speech, derivation_type, form_type,
         gender, grammatical_case, mood, grammatical_number, person,
         tense, voice, verb_class, pada,
         source_abbrev_ids, raw_grammar_span, confidence, parse_notes)
    VALUES %s
"""

INSERT_UNMATCHED_SQL = """
    INSERT INTO unmatched_tokens (entry_id, token_raw, token_normalized, context_span)
    VALUES %s
"""

INSERT_LOG_SQL = """
    INSERT INTO import_log (entry_id, stage, status, message)
    VALUES %s
"""


def run_pass2(conn) -> None:
    abbrev_index = load_abbreviation_index(conn)

    with conn.cursor(name="ap_entries_cursor") as cur:  # server-side cursor, ap.txt is large
        cur.itersize = BATCH_SIZE
        cur.execute("SELECT id, raw_markup FROM entries ORDER BY id")

        fact_rows = []
        unmatched_rows = []
        log_rows = []
        processed = 0

        def flush():
            with conn.cursor() as write_cur:
                if fact_rows:
                    execute_values(write_cur, INSERT_FACT_SQL, fact_rows)
                if unmatched_rows:
                    execute_values(write_cur, INSERT_UNMATCHED_SQL, unmatched_rows)
                if log_rows:
                    execute_values(write_cur, INSERT_LOG_SQL, log_rows)
            conn.commit()
            fact_rows.clear()
            unmatched_rows.clear()
            log_rows.clear()

        for entry_id, raw_markup in cur:
            span, confidence = extract_grammar_span(raw_markup)
            if not span:
                log_rows.append((entry_id, "grammar_parse", "failed", "no grammar span found"))
                processed += 1
                continue

            tokens = tokenize_span(span)
            matches = []
            unmatched = []

            # Fast-path verb class+pada check first (covers the most common
            # pattern and avoids relying on the generic tokenizer for it).
            fp = VERB_FASTPATH_RE.match(span.strip())
            consumed_fastpath = False
            if fp:
                cls = fp.group("class")
                pada = fp.group("pada")
                cls_hits = abbrev_index.get(cls, [])
                pada_hits = abbrev_index.get(normalize_token(pada), [])
                if any(cat == "verb_class" for _, cat, _ in cls_hits) and \
                   any(cat == "pada_type" for _, cat, _ in pada_hits):
                    matches.extend(h for h in cls_hits if h[1] == "verb_class")
                    matches.extend(h for h in pada_hits if h[1] == "pada_type")
                    consumed_fastpath = True

            for token in tokens:
                norm = normalize_token(token)
                if not norm:
                    continue
                if consumed_fastpath and (norm == normalize_token(fp.group("class"))
                                           or norm == normalize_token(fp.group("pada"))):
                    continue
                hits = abbrev_index.get(norm)
                if hits:
                    matches.extend(hits)
                else:
                    unmatched.append(token)
                    unmatched_rows.append((entry_id, token, norm, span[:120]))

            if matches:
                fact_rows.append(build_fact_row(entry_id, span, confidence, matches, unmatched))
                status = "ok" if not unmatched else "partial"
            else:
                status = "failed"

            log_rows.append((entry_id, "grammar_parse", status,
                              f"span={span!r} matched={len(matches)} unmatched={len(unmatched)}"))

            processed += 1
            if processed % BATCH_SIZE == 0:
                flush()
                log.info("Pass 2: processed %d entries", processed)

        flush()
        log.info("Pass 2 complete: %d entries processed", processed)
