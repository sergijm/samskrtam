#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Import Sanskrit verses from Wisdomlib HTML editions into the sangraha schema.

Tables filled:
  - sangraha.sources   (source.code = 'wizdomlib')
  - sangraha.works     (one per edited work, e.g. Hitopadesha)
  - sangraha.chapters  (one per "Book N - Title" section)
  - sangraha.verses    (every || N || stanza inside the chapter body)

XPath used for parsing:
  Work title      : //h3[contains(@class,'h1')]/a
  Chapter title   : //section[contains(@class,'heading')]//h1
  Verse paragraphs: //div[@id='scontent']/p
  (the "Analyze grammar" span is removed before reading text:
   .//span[@class='sanskrit-av'])
  Verse number    : regex r"\\|\\|\\s*(\\d+)\\s*\\|\\|" on the paragraph text
"""

import argparse
import logging
import re
import sys
from glob import glob
from pathlib import Path

import psycopg
from bs4 import BeautifulSoup

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
LOG_FILE = r"C:\MyDev\logs\import_wisdomlib.log"
ENV_FILE_PATH = r"C:\MyDev\samskrtam\.env"
SOURCE_CODE = "wizdomlib"
BATCH_SIZE = 500

# ---------------------------------------------------------------------------
# Selectors  (XPath equivalents used for analysis)
#   Work title      : //h3[contains(@class,'h1')]/a        ->  "h3.h1 a"
#   Chapter title   : //section[contains(@class,'heading')]//h1 -> "section.heading h1"
#   Verse paragraphs: //div[@id='scontent']/p             ->  "div#scontent p"
#   Analyze-grammar : .//span[@class='sanskrit-av']        ->  "span.sanskrit-av"
# ---------------------------------------------------------------------------
SEL_WORK_TITLE = "h3.h1 a"
SEL_CHAPTER_TITLE = "section.heading h1"
SEL_VERSES = "div#scontent p"
SEL_VERSE_AV = "span.sanskrit-av"

VERSE_NUMBER_RE = re.compile(r"\|\|\s*(\d+)\s*\|\|")
CHAPTER_NUMBER_RE = re.compile(r"book\s+(\d+)", re.IGNORECASE)


# ---------------------------------------------------------------------------
# Logging / env / db (mirrors etcetera/python/classify/classify_works.py)
# ---------------------------------------------------------------------------
def setup_logging():
    logger = logging.getLogger("import-wisdomlib")
    if logger.handlers:
        return logger
    logger.setLevel(logging.DEBUG)
    fmt = logging.Formatter("%(asctime)s | %(levelname)s | %(message)s")

    fh = logging.FileHandler(LOG_FILE, encoding="utf-8")
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(fmt)

    ch = logging.StreamHandler()
    ch.setLevel(logging.INFO)
    ch.setFormatter(fmt)

    logger.addHandler(fh)
    logger.addHandler(ch)
    return logger


def read_env(path):
    env = {}
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, _, value = line.partition("=")
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            env[key] = value
    return env


def required(env, name):
    value = env.get(name)
    if not value:
        raise RuntimeError(f"Missing required environment variable: {name}")
    return value


def create_db_connection(env):
    return psycopg.connect(
        host=required(env, "DB_HOST"),
        port=int(env.get("DB_PORT", "5432")),
        dbname=required(env, "DB_NAME"),
        user=required(env, "DB_USER"),
        password=required(env, "DB_PASSWORD"),
    )


def slugify(text):
    slug = re.sub(r"\[[^\]]*\]", "", text)          # drop "[sanskrit]"
    slug = re.sub(r"[^a-z0-9]+", "-", text.lower())
    return slug.strip("-") or "unknown"


# ---------------------------------------------------------------------------
# DB helpers (idempotent get-or-create)
# ---------------------------------------------------------------------------
def get_or_create_source(conn, code):
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM sangraha.sources WHERE code = %s", (code,))
        row = cur.fetchone()
        if row:
            return row[0]
        cur.execute(
            "INSERT INTO sangraha.sources (code, title_en, title_ru) "
            "VALUES (%s, %s, %s) RETURNING id",
            (code, "Wisdomlib", "Wisdomlib"),
        )
        return cur.fetchone()[0]


def get_or_create_work(conn, source_id, raw_title):
    title_en = re.sub(r"\s*\[[^\]]*\]", "", raw_title).strip()
    title_ru = title_en
    slug = slugify(raw_title)
    with conn.cursor() as cur:
        cur.execute("SELECT id FROM sangraha.works WHERE slug = %s", (slug,))
        row = cur.fetchone()
        if row:
            return row[0]
        cur.execute(
            "INSERT INTO sangraha.works "
            "(slug, title_ru, title_en, title_sa_iast, source_id) "
            "VALUES (%s, %s, %s, %s, %s) RETURNING id",
            (slug, title_ru, title_en, title_en, source_id),
        )
        return cur.fetchone()[0]


def get_or_create_chapter(conn, work_id, title_text):
    order_match = CHAPTER_NUMBER_RE.search(title_text)
    order_index = int(order_match.group(1)) if order_match else None
    slug = slugify(title_text)
    title_en = title_text.strip()
    title_ru = title_en
    # sa iast = part after the dash, if any
    title_sa_iast = title_text.split("-", 1)[-1].strip() if "-" in title_text else None

    with conn.cursor() as cur:
        cur.execute(
            "SELECT id FROM sangraha.chapters WHERE work_id = %s AND slug = %s",
            (work_id, slug),
        )
        row = cur.fetchone()
        if row:
            return row[0]
        cur.execute(
            "INSERT INTO sangraha.chapters "
            "(work_id, slug, order_index, title_ru, title_en, title_sa_iast) "
            "VALUES (%s, %s, %s, %s, %s, %s) RETURNING id",
            (work_id, slug, order_index, title_ru, title_en, title_sa_iast),
        )
        return cur.fetchone()[0]


def verse_exists(conn, chapter_id, order_index):
    with conn.cursor() as cur:
        cur.execute(
            "SELECT 1 FROM sangraha.verses "
            "WHERE chapter_id = %s AND order_index = %s",
            (chapter_id, order_index),
        )
        return cur.fetchone() is not None


def insert_verse(conn, chapter_id, order_index, raw_text):
    with conn.cursor() as cur:
        cur.execute(
            "INSERT INTO sangraha.verses "
            "(chapter_id, order_index, raw_text, text_iast, status) "
            "VALUES (%s, %s, %s, %s, 'DRAFT')",
            (chapter_id, order_index, raw_text, raw_text),
        )


# ---------------------------------------------------------------------------
# Parsing
# ---------------------------------------------------------------------------
def parse_verses(soup):
    """Return list of (order_index:int, raw_text:str) for a parsed HTML soup.

    Each verse line lives inside an <em> element; lines are joined with ' |'
    (space before, none after, matching the source edition) and the stanza
    number '|| N ||' is appended from the paragraph tail.
    """
    verses = []
    for p in soup.select(SEL_VERSES):
        for av in p.select(SEL_VERSE_AV):
            av.decompose()
        ems = p.find_all("em")
        if not ems:
            continue
        lines = [re.sub(r"\s+", " ", e.get_text()).strip() for e in ems]
        body = " |".join(lines)
        m = VERSE_NUMBER_RE.search(p.get_text())
        if not m:
            continue
        order_index = int(m.group(1))
        raw_text = f"{body} || {order_index} ||"
        verses.append((order_index, raw_text))
    return verses


def parse_file(path, conn, log, dry_run):
    with open(path, "r", encoding="utf-8") as f:
        soup = BeautifulSoup(f.read(), "html.parser")
    work_node = soup.select_one(SEL_WORK_TITLE)
    chapter_node = soup.select_one(SEL_CHAPTER_TITLE)
    if not work_node or not chapter_node:
        log.warning("Skipping %s: missing work/chapter title", path)
        return 0, 0

    work_title = work_node.get_text().strip()
    chapter_title = chapter_node.get_text().strip()
    verses = parse_verses(soup)
    if not verses:
        log.warning("No verses found in %s", path)
        return 0, 0

    log.info("%s -> work=%r chapter=%r verses=%d",
             Path(path).name, work_title, chapter_title, len(verses))

    if dry_run:
        return 0, len(verses)

    work_id = get_or_create_work(conn, conn._wiz_source_id, work_title)
    chapter_id = get_or_create_chapter(conn, work_id, chapter_title)

    inserted = 0
    for order_index, raw_text in verses:
        if verse_exists(conn, chapter_id, order_index):
            continue
        insert_verse(conn, chapter_id, order_index, raw_text)
        inserted += 1
        if inserted % BATCH_SIZE == 0:
            conn.commit()
            log.info("  committed %d verses so far", inserted)
    conn.commit()
    return inserted, len(verses)


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------
def main():
    log = setup_logging()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("directory", help="Directory with *.html files")
    parser.add_argument("--dry-run", action="store_true",
                        help="Parse and report, do not write to DB")
    args = parser.parse_args()

    directory = args.directory
    if directory.startswith("directory="):
        directory = directory[len("directory="):]
    directory = directory.lstrip("\ufeff\u202a\u202b\u202c\u202d\u202e").strip()

    files = glob(directory.rstrip("/\\") + "/**/*.html", recursive=True)
    # also include non-recursive
    files += glob(directory.rstrip("/\\") + "/*.html")
    files = sorted(set(files))

    if not files:
        log.error("No *.html files found in %s", directory)
        sys.exit(1)

    log.info("Found %d html files (dry_run=%s)", len(files), args.dry_run)

    total_inserted = 0
    total_parsed = 0

    if args.dry_run:
        for f in files:
            ins, parsed = parse_file(f, None, log, dry_run=True)
            total_inserted += ins
            total_parsed += parsed
    else:
        try:
            env = read_env(ENV_FILE_PATH)
            conn = create_db_connection(env)
            conn._wiz_source_id = get_or_create_source(conn, SOURCE_CODE)
            log.info("Connected; source id=%s", conn._wiz_source_id)
        except Exception:
            log.exception("Failed to initialize DB connection")
            sys.exit(1)

        try:
            for f in files:
                ins, parsed = parse_file(f, conn, log, dry_run=False)
                total_inserted += ins
                total_parsed += parsed
        finally:
            conn.close()

    log.info("Done. Parsed %d verses, inserted %d new.",
             total_parsed, total_inserted)


if __name__ == "__main__":
    main()
