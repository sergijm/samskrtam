"""
Pass 1 — structural parse of ap.txt into the `entries` and `page_breaks`
tables (see apte_dictionary_schema.sql).

This pass only touches the CDSL structural markup that is documented in
csl-orig's *-meta2.txt files (<L>, <pc>, <k1>, <k2>, <hom>, <LEND>,
[PageV-PPP-C+ N]). It does NOT interpret grammar or gloss text — that is
pass 2 (parse_grammar.py).

Usage:
    python import_apte.py --pass structure
"""

import re
from dataclasses import dataclass, field

from psycopg2.extras import execute_values

from config import log, BATCH_SIZE, DICTIONARY_CODE

# ----------------------------------------------------------------------------
# Regexes for the documented markup
# ----------------------------------------------------------------------------

# One entry: <L>...  up to (not including) the next <L> or end of file,
# terminated by <LEND>. We split on <L> boundaries first, then trim at <LEND>,
# which tolerates occasional missing/duplicated <LEND> markers better than a
# single greedy regex would.
ENTRY_SPLIT_RE = re.compile(r"(?=<L>)")

# Header line fields. Order in the source is fixed (L, pc, k1, k2, hom) but
# hom is optional, so match each field independently rather than assuming
# a fixed sequence.
FIELD_L_RE   = re.compile(r"<L>([^<]+)")
FIELD_PC_RE  = re.compile(r"<pc>([^<]+)")
FIELD_K1_RE  = re.compile(r"<k1>([^<]+)")
FIELD_K2_RE  = re.compile(r"<k2>([^<]+)")
FIELD_HOM_RE = re.compile(r"<hom>([^<]+)")

# <L> value can be "1182" or "1182-1"
LNUM_RE = re.compile(r"^(\d+)(?:-(\S+))?$")

# <pc> value like "234-1" or "234-1,2" (volume-page,column) — actual CDSL
# convention varies by dictionary; adjust after inspecting real ap.txt lines.
# This handles "VOL-PAGE,COL" and "PAGE,COL" (single-volume works).
PC_RE = re.compile(r"^(?:(\d+)-)?(\d+)(?:,(\S+))?$")

# [PageV-PPP-C+ N] or [PageV-PPP+ N]
PAGE_BREAK_RE = re.compile(
    r"\[Page(\d+)-(\d+)(?:-(\w+))?\+\s*(\w+)\]"
)


@dataclass
class ParsedEntry:
    lnum: str
    lnum_sort: int | None
    lnum_suffix: str | None
    pc_volume: int | None
    pc_page: int | None
    pc_column: str | None
    k1_slp1: str
    k2_original: str | None
    homonym_num: int | None
    raw_markup: str
    body_text: str
    page_breaks: list = field(default_factory=list)  # list of dict


def split_entries(raw_text: str) -> list[str]:
    """Split the full ap.txt content into raw <L>...<LEND> blocks."""
    chunks = ENTRY_SPLIT_RE.split(raw_text)
    blocks = []
    for chunk in chunks:
        chunk = chunk.strip()
        if not chunk.startswith("<L>"):
            continue  # preamble / front matter before the first <L>
        if "<LEND>" not in chunk:
            log.warning("Block missing <LEND>, skipping first 80 chars: %r", chunk[:80])
            continue
        block, _, _trailing = chunk.partition("<LEND>")
        blocks.append(block + "<LEND>")
    return blocks


def strip_markup(text: str) -> str:
    """
    Rough markup stripper for body_text: removes tag pairs but keeps their
    text content. Devanagari {#...#} and italics {%...%} are unwrapped
    rather than dropped, since the SLP1/IAST text inside is still useful
    for full-text search.
    """
    text = re.sub(r"\{#(.*?)#\}", r"\1", text, flags=re.S)   # devanagari
    text = re.sub(r"\{%(.*?)%\}", r"\1", text, flags=re.S)   # italics
    text = re.sub(r"\{@(.*?)@\}", r"\1", text, flags=re.S)   # bold
    text = re.sub(r"<[^>]+>", "", text)                       # any remaining tag
    text = re.sub(r"\[Page[^\]]*\]", "", text)                # page breaks
    text = re.sub(r"\s+", " ", text).strip()
    return text


def parse_entry(block: str) -> ParsedEntry | None:
    m_l = FIELD_L_RE.search(block)
    m_k1 = FIELD_K1_RE.search(block)
    if not m_l or not m_k1:
        log.warning("Entry missing <L> or <k1>, skipping: %r", block[:100])
        return None

    lnum_raw = m_l.group(1).strip()
    m_lnum = LNUM_RE.match(lnum_raw)
    lnum_sort = int(m_lnum.group(1)) if m_lnum else None
    lnum_suffix = m_lnum.group(2) if m_lnum else None

    pc_volume = pc_page = None
    pc_column = None
    m_pc = FIELD_PC_RE.search(block)
    if m_pc:
        m_pc_parsed = PC_RE.match(m_pc.group(1).strip())
        if m_pc_parsed:
            vol, page, col = m_pc_parsed.groups()
            pc_volume = int(vol) if vol else None
            pc_page = int(page) if page else None
            pc_column = col
        else:
            log.warning("Unparsed <pc> value %r in entry L=%s", m_pc.group(1), lnum_raw)

    k1 = m_k1.group(1).strip()

    m_k2 = FIELD_K2_RE.search(block)
    k2 = m_k2.group(1).strip() if m_k2 else None

    m_hom = FIELD_HOM_RE.search(block)
    hom = None
    if m_hom:
        hom_raw = m_hom.group(1).strip()
        hom = int(hom_raw) if hom_raw.isdigit() else None
        if hom is None:
            log.warning("Non-numeric <hom> value %r in entry L=%s", hom_raw, lnum_raw)

    page_breaks = []
    for seq, m_pb in enumerate(PAGE_BREAK_RE.finditer(block), start=1):
        vol, page, col, n = m_pb.groups()
        page_breaks.append({
            "seq_in_entry": seq,
            "volume": int(vol),
            "page": int(page),
            "column_label": col,
            "line_count": int(n) if n.isdigit() else None,
            "raw_marker": m_pb.group(0),
        })

    return ParsedEntry(
        lnum=lnum_raw,
        lnum_sort=lnum_sort,
        lnum_suffix=lnum_suffix,
        pc_volume=pc_volume,
        pc_page=pc_page,
        pc_column=pc_column,
        k1_slp1=k1,
        k2_original=k2,
        homonym_num=hom,
        raw_markup=block,
        body_text=strip_markup(block),
        page_breaks=page_breaks,
    )


# ----------------------------------------------------------------------------
# DB load
# ----------------------------------------------------------------------------

INSERT_ENTRY_SQL = """
    INSERT INTO entries
        (dictionary_id, lnum, lnum_sort, lnum_suffix,
         pc_volume, pc_page, pc_column,
         k1_slp1, k2_original, homonym_num,
         raw_markup, body_text)
    VALUES %s
    ON CONFLICT (dictionary_id, lnum) DO UPDATE SET
        raw_markup = EXCLUDED.raw_markup,
        body_text  = EXCLUDED.body_text,
        updated_at = now()
    RETURNING id, lnum
"""

INSERT_PAGE_BREAK_SQL = """
    INSERT INTO page_breaks
        (entry_id, seq_in_entry, volume, page, column_label, line_count, raw_marker)
    VALUES %s
    ON CONFLICT (entry_id, seq_in_entry) DO NOTHING
"""

INSERT_LOG_SQL = """
    INSERT INTO import_log (entry_id, stage, status, message)
    VALUES %s
"""


def get_dictionary_id(conn, code: str) -> int:
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM dictionaries WHERE code = %s", (code,))
        row = cur.fetchone()
        if not row:
            raise ValueError(f"Dictionary code {code!r} not found in dictionaries table")
        return row[0]


def run_pass1(conn, ap_txt_path: str) -> None:
    dictionary_id = get_dictionary_id(conn, DICTIONARY_CODE)

    log.info("Reading %s", ap_txt_path)
    with open(ap_txt_path, encoding="utf-8") as f:
        raw_text = f.read()

    blocks = split_entries(raw_text)
    log.info("Found %d raw <L>...<LEND> blocks", len(blocks))

    parsed_batch: list[ParsedEntry] = []
    ok_count = fail_count = 0

    def flush(batch: list[ParsedEntry]):
        nonlocal ok_count
        if not batch:
            return
        rows = [
            (
                dictionary_id, pe.lnum, pe.lnum_sort, pe.lnum_suffix,
                pe.pc_volume, pe.pc_page, pe.pc_column,
                pe.k1_slp1, pe.k2_original, pe.homonym_num,
                pe.raw_markup, pe.body_text,
            )
            for pe in batch
        ]
        with conn.cursor() as cur:
            id_rows = execute_values(cur, INSERT_ENTRY_SQL, rows, fetch=True)
            lnum_to_id = {lnum: eid for eid, lnum in id_rows}

            pb_rows = []
            log_rows = []
            for pe in batch:
                entry_id = lnum_to_id.get(pe.lnum)
                if entry_id is None:
                    continue
                for pb in pe.page_breaks:
                    pb_rows.append((
                        entry_id, pb["seq_in_entry"], pb["volume"], pb["page"],
                        pb["column_label"], pb["line_count"], pb["raw_marker"],
                    ))
                log_rows.append((entry_id, "structure_parse", "ok", None))

            if pb_rows:
                execute_values(cur, INSERT_PAGE_BREAK_SQL, pb_rows)
            if log_rows:
                execute_values(cur, INSERT_LOG_SQL, log_rows)
        conn.commit()
        ok_count += len(batch)
        log.info("Committed batch of %d entries (total so far: %d)", len(batch), ok_count)

    for block in blocks:
        pe = parse_entry(block)
        if pe is None:
            fail_count += 1
            continue
        parsed_batch.append(pe)
        if len(parsed_batch) >= BATCH_SIZE:
            flush(parsed_batch)
            parsed_batch = []

    flush(parsed_batch)

    log.info("Pass 1 complete: %d entries loaded, %d blocks failed to parse", ok_count, fail_count)
