#!/usr/bin/env python3
# Import DCS CoNLL-U files into the supplied Sangraha PostgreSQL schema.
# Usage:
#   python import_dcs_conllu.py --db 'postgresql://...' --dir /path/to/dcs/data
# Dependency:
#   pip install "psycopg[binary]>=3.1"
# Errors are logged; processing continues. Each sentence is its own transaction.

from __future__ import annotations
import argparse, csv, json, logging, re, sys, uuid
from dataclasses import dataclass
from pathlib import Path

try:
    import psycopg
except ImportError:
    psycopg = None
try:
    import psycopg2
except ImportError:
    psycopg2 = None

LOG = logging.getLogger("dcs-import")
VOICE = {"Act": "ACTIVE", "Mid": "MIDDLE", "Pass": "PASSIVE"}

@dataclass
class Token:
    tid: str
    form: str
    lemma: str
    upos: str
    xpos: str
    feats: dict[str, str]
    misc: dict[str, str]

@dataclass
class Sentence:
    comments: list[str]
    tokens: list[Token]

    def metadata(self):
        out = {}
        for line in self.comments:
            if line.startswith("#") and "=" in line:
                k, v = line[1:].split("=", 1)
                out[k.strip()] = v.strip()
        return out

    def text(self):
        return self.metadata().get("text") or " ".join(t.form for t in self.tokens)

def kv(value):
    if not value or value == "_":
        return {}
    out = {}
    for item in value.split("|"):
        if "=" in item:
            k, v = item.split("=", 1)
            out[k] = v
    return out

def parse_conllu(path):
    comments, tokens = [], []

    def flush():
        nonlocal comments, tokens
        if comments or tokens:
            s = Sentence(comments, tokens)
            comments, tokens = [], []
            return s
        return None

    with path.open("r", encoding="utf-8-sig", errors="replace") as fh:
        for lineno, line in enumerate(fh, 1):
            line = line.rstrip("\r\n")
            if not line:
                s = flush()
                if s:
                    yield s
                continue
            if line.startswith("#"):
                comments.append(line)
                continue
            fields = line.split("\t")
            if len(fields) != 10:
                LOG.warning("%s:%d: expected 10 columns, got %d; row skipped",
                            path, lineno, len(fields))
                continue
            tid = fields[0]
            if "-" in tid or "." in tid:
                continue
            tokens.append(Token(
                tid,
                "" if fields[1] == "_" else fields[1],
                "" if fields[2] == "_" else fields[2],
                "" if fields[3] == "_" else fields[3],
                "" if fields[4] == "_" else fields[4],
                kv(fields[5]), kv(fields[9])
            ))
    s = flush()
    if s:
        yield s

def norm(s):
    return re.sub(r"[^a-z0-9]", "", s.lower())

def lookup_value(row, *names):
    data = {norm(k): v for k, v in row.items() if k is not None}
    for name in names:
        value = data.get(norm(name))
        if value and value != "_":
            return value
    return None

class Lookup:
    def __init__(self, root):
        self.by_id, self.by_lemma = {}, {}
        if not root:
            return
        path = root / "dictionary.csv"
        if not path.exists():
            return
        try:
            with path.open("r", encoding="utf-8-sig", errors="replace", newline="") as fh:
                sample = fh.read(8192)
                fh.seek(0)
                try:
                    dialect = csv.Sniffer().sniff(sample, delimiters=",;\t")
                except csv.Error:
                    dialect = csv.excel
                count = 0
                for row in csv.DictReader(fh, dialect=dialect):
                    rid = lookup_value(row, "id", "lemma_id", "lexicon_id", "word_id")
                    lemma = lookup_value(row, "lemma", "stem", "word", "form")
                    if rid:
                        self.by_id[str(rid)] = row
                    if lemma:
                        self.by_lemma[lemma] = row
                    count += 1
            LOG.info("Loaded %d dictionary rows from %s", count, path)
        except Exception:
            LOG.exception("Could not load optional lookup file %s", path)

    def row(self, lemma_id, lemma):
        if lemma_id and str(lemma_id) in self.by_id:
            return self.by_id[str(lemma_id)]
        return self.by_lemma.get(lemma or "", {})

    def get(self, lemma_id, lemma, *names):
        return lookup_value(self.row(lemma_id, lemma), *names)

def find_lookup(root):
    candidates = [
        root / "conllu" / "files" / "lookup",
        root / "conllu" / "lookup",
        root / "files" / "lookup",
        root / "lookup",
        ]
    for p in candidates:
        if p.is_dir():
            return p
    for p in root.glob("**/lookup"):
        if p.is_dir():
            return p
    return None

def map_pos(upos):
    return {
        "NOUN": "NOUN", "PROPN": "NOUN",
        "VERB": "VERB", "AUX": "VERB",
        "ADJ": "ADJECTIVE", "ADV": "ADVERB",
        "PRON": "PRONOUN", "DET": "PRONOUN",
        "NUM": "NUMERAL", "PART": "PARTICLE",
        "CCONJ": "CONJUNCTION", "SCONJ": "CONJUNCTION",
        "INTJ": "INTERJECTION", "ADP": "INDECLINABLE",
        "X": "OTHER", "SYM": "OTHER",
    }.get(upos.upper(), "OTHER")

def form_type(token, pos):
    vf = token.feats.get("VerbForm")
    if pos == "VERB":
        if vf == "Inf": return "INFINITIVE", False
        if vf == "Conv": return "ABSOLUTIVE", False
        if vf == "Part": return "PARTICIPLE", False
        if vf == "Ger": return "GERUNDIVE", False
        if vf == "Fin" or any(k in token.feats for k in ("Person", "Mood", "Tense")):
            return "FINITE", True
        return "OTHER_NONFINITE", False
    if pos == "NOUN": return "NOMINAL", None
    if pos == "ADJECTIVE": return "ADJECTIVAL", None
    if pos == "PRONOUN": return "PRONOMINAL", None
    if pos in {"PARTICLE", "ADVERB", "INDECLINABLE", "CONJUNCTION", "INTERJECTION"}:
        return "INDECLINABLE", None
    if pos == "NUMERAL": return "NOMINAL", None
    return None, None

def derivation_type(token, pos, ft):
    vf = token.feats.get("VerbForm")
    if vf == "Conv": return "ABSOLUTIVE"
    if vf == "Inf": return "INFINITIVE"
    if vf == "Part": return "PARTICIPLE"
    if vf == "Ger": return "GERUNDIVE"
    if pos == "VERB" and ft == "FINITE": return "SIMPLE_INFLECTION"
    if ft in {"NOMINAL", "ADJECTIVAL", "PRONOMINAL"}: return "SIMPLE_INFLECTION"
    return None

def make_analysis(token, lookup):
    pos = map_pos(token.upos)
    ft, finite = form_type(token, pos)
    f = token.feats
    lid = token.misc.get("LemmaId")

    # DCS documents column 3 as "Lemma or stem". For verbs it is commonly
    # the lexical/dhatu-like lemma; no root is invented for non-verbs.
    root = token.lemma if pos == "VERB" and token.lemma else None
    stem = lookup.get(lid, token.lemma, "stem", "morphological_stem")
    if stem is None and pos in {"NOUN", "ADJECTIVE"}:
        stem = token.lemma or None

    return {
        "surface_iast": token.form or "",
        "surface_devanagari": token.misc.get("Devanagari", ""),
        "lemma_iast": token.lemma or token.form or "",
        "stem": stem,
        "root": root,
        "pos": pos,
        "form_type": ft,
        "is_finite": finite,
        "lemma_gloss_ru": lookup.get(
            lid, token.lemma, "lemma_gloss_ru", "gloss_ru", "meaning_ru",
            "russian", "ru", "translation_ru"
        ),
        "lemma_gloss_en": lookup.get(
            lid, token.lemma, "lemma_gloss_en", "gloss_en", "meaning_en",
            "english", "en", "translation_en"
        ),
        "context_gloss_ru": token.misc.get("GlossRu") or lookup.get(
            lid, token.lemma, "context_gloss_ru", "gloss_ru", "meaning_ru"
        ) or "",
        "context_gloss_en": token.misc.get("GlossEn") or lookup.get(
            lid, token.lemma, "context_gloss_en", "gloss_en", "meaning_en"
        ) or "",
        # DCS CoNLL-U contains no emenau rule-number annotations.
        "formation_rule_numbers": "[]",
        "analysis_confidence": "HIGH" if token.lemma and token.upos else "MEDIUM",
        "ambiguity_notes": None,
        "morphology": {
            "case": f.get("Case"), "gender": f.get("Gender"),
            "number": f.get("Number"), "person": f.get("Person"),
            "tense": f.get("Tense"), "mood": f.get("Mood"),
            "voice": VOICE.get(f.get("Voice"), f.get("Voice")),
        },
        "derivation_type": derivation_type(token, pos, ft),
        "derivational_suffix": lookup.get(
            lid, token.lemma, "derivational_suffix", "suffix", "formation_suffix"
        ),
        "derivational_base": lookup.get(
            lid, token.lemma, "derivational_base", "base", "formation_base"
        ),
        "description": lookup.get(
            lid, token.lemma, "derivation_description", "description"
        ),
    }

def slugify(s):
    return (re.sub(r"[^a-z0-9]+", "-", s.lower()).strip("-") or "item")[:80]

def filename_meta(path):
    # DCS documentation: Text-0007-Citation form of chapter, chapter citation, chapter ID.
    m = re.match(r"^(.*?)-(\d{4})-(.*)$", path.stem)
    if m:
        return m.group(1), m.group(3) or f"chapter-{m.group(2)}", int(m.group(2))
    return path.stem, path.stem, None

def connect(dsn):
    if psycopg is not None:
        return psycopg.connect(dsn)
    if psycopg2 is not None:
        return psycopg2.connect(dsn)
    raise RuntimeError('Install "psycopg[binary]" or "psycopg2-binary".')

def get_or_create_work(conn, cache, key):
    if key in cache:
        return cache[key]
    slug = slugify(key)
    with conn.cursor() as c:
        c.execute('SELECT id FROM "sangraha"."works" WHERE slug=%s', (slug,))
        row = c.fetchone()
        if row:
            cache[key] = row[0]
            return row[0]
        wid = uuid.uuid4()
        title = key[:255]
        c.execute(
            '''INSERT INTO "sangraha"."works"
                   (id,slug,title_ru,title_en,title_sa_iast)
               VALUES (%s,%s,%s,%s,%s)''',
            (wid, slug, title, title, title)
        )
    conn.commit()
    cache[key] = wid
    return wid

def get_or_create_chapter(conn, cache, work_id, key, order_index):
    ck = (str(work_id), key)
    if ck in cache:
        return cache[ck]
    slug = slugify(key)
    with conn.cursor() as c:
        c.execute(
            'SELECT id FROM "sangraha"."chapters" WHERE work_id=%s AND slug=%s',
            (work_id, slug)
        )
        row = c.fetchone()
        if row:
            cache[ck] = row[0]
            return row[0]
        cid = uuid.uuid4()
        title = key[:255]
        c.execute(
            '''INSERT INTO "sangraha"."chapters"
                   (id,work_id,slug,order_index,title_ru,title_en,title_sa_iast)
               VALUES (%s,%s,%s,%s,%s,%s,%s)''',
            (cid, work_id, slug, order_index, title, title, title)
        )
    conn.commit()
    cache[ck] = cid
    return cid

def import_sentence(conn, chapter_id, order_index, sent, lookup, source):
    # Punctuation is not a word in verse-analysis.md; raw/text retain it.
    words = [t for t in sent.tokens if t.upos != "PUNCT"]
    if not words:
        return False

    md = sent.metadata()
    text_iast = md.get("text") or sent.text()
    text_dev = md.get("text_devanagari") or md.get("textDevanagari")

    with conn.cursor() as c:
        c.execute(
            '''SELECT id FROM "sangraha"."verses"
               WHERE chapter_id=%s AND order_index=%s''',
            (chapter_id, order_index)
        )
        old = c.fetchone()
        if old:
            c.execute('DELETE FROM "sangraha"."verses" WHERE id=%s', (old[0],))
        verse_id = old[0] if old else uuid.uuid4()

        c.execute(
            '''INSERT INTO "sangraha"."verses"
               (id,chapter_id,order_index,raw_text,text_devanagari,text_iast,status)
               VALUES (%s,%s,%s,%s,%s,%s,'ANALYZED')''',
            (verse_id, chapter_id, order_index, text_iast, text_dev, text_iast)
        )

        # No translation/sandhi-rule analysis is present in the DCS CoNLL-U
        # source. Do not invent it.
        raw_model = json.dumps({
            "source": str(source),
            "importer": "import_dcs_conllu.py",
            "note": "No model translation/sandhi-rule analysis in DCS CoNLL-U source."
        }, ensure_ascii=False)
        c.execute(
            '''INSERT INTO "sangraha"."verse_analyses"
               (verse_id,translation_ru,translation_en,sandhi_splits,
                raw_model_response,model_name)
               VALUES (%s,%s,%s,'[]'::jsonb,%s::jsonb,%s)''',
            (verse_id, "", "", raw_model, "DCS-CoNLL-U-import")
        )

        for position, token in enumerate(words):
            a = make_analysis(token, lookup)
            wid = uuid.uuid4()
            c.execute(
                '''INSERT INTO "sangraha"."verse_words"
                   (id,verse_id,position,surface_iast,surface_devanagari,
                    lemma_iast,stem,root,pos,form_type,is_finite,
                    lemma_gloss_ru,lemma_gloss_en,context_gloss_ru,context_gloss_en,
                    formation_rule_numbers,analysis_confidence,ambiguity_notes,
                    vocabulary_word_id)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s)''',
                (
                    wid, verse_id, position, a["surface_iast"],
                    a["surface_devanagari"], a["lemma_iast"], a["stem"], a["root"],
                    a["pos"], a["form_type"], a["is_finite"],
                    a["lemma_gloss_ru"], a["lemma_gloss_en"],
                    a["context_gloss_ru"], a["context_gloss_en"],
                    a["formation_rule_numbers"], a["analysis_confidence"],
                    a["ambiguity_notes"], None
                )
            )

            m = a["morphology"]
            c.execute(
                '''INSERT INTO "sangraha"."verse_word_morphology"
                   (verse_word_id,case_type,gender,number_type,person,tense,mood,voice)
                   VALUES (%s,%s,%s,%s,%s,%s,%s,%s)''',
                (
                    wid, m["case"], m["gender"], m["number"], m["person"],
                    m["tense"], m["mood"], m["voice"]
                )
            )

            c.execute(
                '''INSERT INTO "sangraha"."verse_word_derivation"
                   (verse_word_id,derivation_type,derivational_suffix,
                    derivational_base,description)
                   VALUES (%s,%s,%s,%s,%s)''',
                (
                    wid, a["derivation_type"], a["derivational_suffix"],
                    a["derivational_base"], a["description"]
                )
            )

    conn.commit()
    return True

def import_file(conn, path, lookup, work_cache, chapter_cache, stats):
    work_name, chapter_name, chapter_order = filename_meta(path)
    work_id = get_or_create_work(conn, work_cache, work_name)
    chapter_id = get_or_create_chapter(
        conn, chapter_cache, work_id, chapter_name, chapter_order
    )

    count = 0
    for count, sent in enumerate(parse_conllu(path)):
        try:
            if import_sentence(conn, chapter_id, count, sent, lookup, path):
                stats["verses"] += 1
        except Exception:
            conn.rollback()
            stats["errors"] += 1
            LOG.exception(
                "Import error: file=%s sentence/order=%d; skipped and continuing",
                path, count
            )
    stats["files"] += 1
    LOG.info("Finished %s: %d sentence blocks", path, count + 1)

def main():
    ap = argparse.ArgumentParser(description="Import DCS CoNLL-U into Sangraha")
    ap.add_argument("--db", required=True, help="PostgreSQL DSN/URL")
    ap.add_argument("--dir", required=True, type=Path, help="DCS root directory")
    ap.add_argument("--log-level", default="INFO",
                    choices=("DEBUG", "INFO", "WARNING", "ERROR"))
    args = ap.parse_args()

    logging.basicConfig(
        level=getattr(logging, args.log_level),
        format="%(asctime)s %(levelname)s %(name)s: %(message)s"
    )

    root = args.dir.expanduser().resolve()
    if not root.is_dir():
        LOG.error("Directory does not exist: %s", root)
        return 2

    files = sorted(root.rglob("*.conllu"))
    if not files:
        LOG.error("No *.conllu files found under %s", root)
        return 2

    lookup_root = find_lookup(root)
    lookup = Lookup(lookup_root)
    if lookup_root:
        LOG.info("Lookup directory: %s", lookup_root)
    else:
        LOG.warning("DCS lookup directory not found; optional gloss data stays NULL/empty")

    try:
        conn = connect(args.db)
    except Exception:
        LOG.exception("Database connection failed")
        return 3

    stats = {"files": 0, "verses": 0, "errors": 0}
    work_cache, chapter_cache = {}, {}

    try:
        with conn.cursor() as c:
            c.execute('SELECT 1 FROM "sangraha"."works" LIMIT 1')
        conn.commit()

        for path in files:
            try:
                import_file(conn, path, lookup, work_cache, chapter_cache, stats)
            except Exception:
                conn.rollback()
                stats["errors"] += 1
                LOG.exception("File-level error: %s; continuing", path)
    finally:
        conn.close()

    LOG.info("DONE: files=%d verses=%d errors=%d",
             stats["files"], stats["verses"], stats["errors"])
    return 0 if stats["errors"] == 0 else 1

if __name__ == "__main__":
    sys.exit(main())
