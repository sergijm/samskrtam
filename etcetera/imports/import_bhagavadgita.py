#!/usr/bin/env python3
"""
Импорт чистого санскритско-русского текста Бхагавад-гиты 1914
с samskrtam.ru в PostgreSQL-схему sangraha.

Заполняются только:
  works
  chapters
  verses
  verse_analyses

Грамматический разбор и verse_words НЕ создаются.

Зависимости:
  pip install requests beautifulsoup4 "psycopg[binary]"

Подключение:
  export DATABASE_URL='postgresql://user:password@host:5432/dbname'

Запуск:
  python import_bhagavadgita_1914.py
  python import_bhagavadgita_1914.py --dry-run
"""

from __future__ import annotations

import argparse
import logging
import os
import sys
from dataclasses import dataclass, field
import re
from typing import Optional

import psycopg
import requests
from bs4 import BeautifulSoup


URL = "https://samskrtam.ru/parallel-corpus/bhagavadgita-1914.html"

WORK_SLUG = "bhagavad-gita-kamenskaya-1914"
WORK_TITLE_RU = "Бхагавад-гита"
WORK_TITLE_EN = "Bhagavad Gita"
WORK_TITLE_SA_IAST = "Bhagavad-gītā"
WORK_TITLE_SA_DEVANAGARI = None
WORK_AUTHOR = "А.А. Каменская; И.В. де Манциарли"

CHAPTER_RE = re.compile(r"^Беседа\s+(.+)$", re.IGNORECASE)
VERSE_NO_RE = re.compile(r"^(\d{1,3})$")
NOTE_RE = re.compile(r"^\d+\.\s+")
SANSKRIT_MARK_RE = re.compile(r"[।॥]")
SPEAKER_RE = re.compile(
    r"^(?:Дхритараштра|Санджая|Арджуна|Благословенный)"
    r"\s+(?:сказал|сказала|молвил|молвила):?\s*$",
    re.IGNORECASE,
)

CHAPTER_NUMBERS = {
    "первая": 1, "вторая": 2, "третья": 3, "четвертая": 4,
    "пятая": 5, "шестая": 6, "седьмая": 7, "восьмая": 8,
    "девятая": 9, "десятая": 10, "одиннадцатая": 11,
    "двенадцатая": 12, "тринадцатая": 13, "четырнадцатая": 14,
    "пятнадцатая": 15, "шестнадцатая": 16, "семнадцатая": 17,
    "восемнадцатая": 18,
}


@dataclass
class Verse:
    number: int
    sanskrit: str
    translation_ru: str


@dataclass
class Chapter:
    number: int
    heading_ru: str
    title_ru: Optional[str] = None
    verses: list[Verse] = field(default_factory=list)


def clean_text(value: str) -> str:
    return re.sub(r"\s+", " ", value or "").strip()


def is_sanskrit_block(text: str) -> bool:
    if not text or not SANSKRIT_MARK_RE.search(text):
        return False

    cyrillic = len(re.findall(r"[А-Яа-яЁё]", text))
    latin = len(
        re.findall(
            r"[A-Za-zāīūṛṝḷḹṃḥṅñṇṭḍśṣĀĪŪṚṜḶḸṂḤṄÑṆṬḌŚṢ]",
            text,
        )
    )
    return cyrillic == 0 and latin > 5


def remove_non_content_nodes(soup: BeautifulSoup) -> None:
    for selector in (
            "script", "style", "noscript", "iframe", "svg",
            "sup", ".footnote", ".sidenote", ".note",
    ):
        for node in soup.select(selector):
            node.decompose()

    for a in soup.find_all("a"):
        href = (a.get("href") or "").lower()
        cls = " ".join(a.get("class") or []).lower()
        if "footnote" in cls or "sidenote" in cls or href.startswith("#fn"):
            a.decompose()


def extract_blocks(soup: BeautifulSoup) -> list[str]:
    remove_non_content_nodes(soup)

    block_tags = [
        "h1", "h2", "h3", "h4", "h5", "h6",
        "p", "li", "td", "blockquote", "div",
    ]

    blocks: list[str] = []
    for tag in soup.find_all(block_tags):
        if tag.find(block_tags):
            continue

        text = clean_text(tag.get_text(" ", strip=True))
        if text:
            blocks.append(text)

    return blocks


def chapter_number_from_heading(heading: str) -> Optional[int]:
    match = CHAPTER_RE.match(heading)
    if not match:
        return None
    return CHAPTER_NUMBERS.get(match.group(1).strip().lower())


def parse_page(html: str) -> tuple[dict, list[Chapter]]:
    soup = BeautifulSoup(html, "html.parser")
    blocks = extract_blocks(soup)

    chapters: list[Chapter] = []
    current: Optional[Chapter] = None
    i = 0
    pending_title = False

    while i < len(blocks):
        block = blocks[i]

        chapter_no = chapter_number_from_heading(block)
        if chapter_no is not None:
            current = Chapter(number=chapter_no, heading_ru=block)
            chapters.append(current)
            pending_title = False
            i += 1
            continue

        if current is not None and block.startswith("Так в достославных"):
            pending_title = True
            i += 1
            continue

        if pending_title and current is not None:
            if block and not CHAPTER_RE.match(block):
                current.title_ru = block
            pending_title = False
            i += 1
            continue

        if current is None:
            i += 1
            continue

        match = VERSE_NO_RE.match(block)
        if not match:
            i += 1
            continue

        verse_number = int(match.group(1))
        j = i + 1
        sanskrit_parts: list[str] = []

        while j < len(blocks) and is_sanskrit_block(blocks[j]):
            sanskrit_parts.append(blocks[j])
            j += 1

        if not sanskrit_parts:
            logging.warning(
                "Глава %s, стих %s: санскритский текст не найден",
                current.number, verse_number,
            )
            i += 1
            continue

        translation_parts: list[str] = []
        while j < len(blocks):
            candidate = blocks[j]

            if CHAPTER_RE.match(candidate) or VERSE_NO_RE.match(candidate):
                break
            if candidate.startswith("Так в достославных"):
                break
            if NOTE_RE.match(candidate):
                j += 1
                continue
            if is_sanskrit_block(candidate):
                break
            if SPEAKER_RE.match(candidate):
                j += 1
                continue

            translation_parts.append(candidate)
            j += 1
            break

        translation = clean_text(" ".join(translation_parts))
        if not translation:
            logging.warning(
                "Глава %s, стих %s: русский перевод не найден; "
                "сохраняем пустую строку",
                current.number, verse_number,
            )

        current.verses.append(
            Verse(
                number=verse_number,
                sanskrit=clean_text(" ".join(sanskrit_parts)),
                translation_ru=translation,
            )
        )
        i = j

    metadata = {
        "title_ru": WORK_TITLE_RU,
        "title_en": WORK_TITLE_EN,
        "title_sa_iast": WORK_TITLE_SA_IAST,
        "title_sa_devanagari": WORK_TITLE_SA_DEVANAGARI,
        "author": WORK_AUTHOR,
    }
    return metadata, chapters


def fetch_html(url: str = URL, timeout: int = 30) -> str:
    headers = {
        "User-Agent": (
            "Mozilla/5.0 (compatible; SangrahaCorpusImporter/1.0; "
            "+https://samskrtam.ru/)"
        )
    }
    response = requests.get(url, headers=headers, timeout=timeout)
    response.raise_for_status()
    response.encoding = response.apparent_encoding or response.encoding
    return response.text


def upsert_work(cur, metadata: dict) -> str:
    cur.execute(
        """
        INSERT INTO sangraha.works
        (slug, title_ru, title_en, title_sa_iast,
         title_sa_devanagari, author)
        VALUES
            (%(slug)s, %(title_ru)s, %(title_en)s, %(title_sa_iast)s,
             %(title_sa_devanagari)s, %(author)s)
        ON CONFLICT (slug) DO UPDATE SET
                                         title_ru = EXCLUDED.title_ru,
                                         title_en = EXCLUDED.title_en,
                                         title_sa_iast = EXCLUDED.title_sa_iast,
                                         title_sa_devanagari = EXCLUDED.title_sa_devanagari,
                                         author = EXCLUDED.author,
                                         deleted_at = NULL
        RETURNING id
        """,
        {"slug": WORK_SLUG, **metadata},
    )
    row = cur.fetchone()
    if not row:
        raise RuntimeError("Не удалось получить id произведения")
    return str(row[0])


def upsert_chapter(cur, work_id: str, chapter: Chapter) -> str:
    slug = f"chapter-{chapter.number}"
    title_ru = chapter.title_ru or chapter.heading_ru
    title_en = f"Chapter {chapter.number}"

    cur.execute(
        """
        INSERT INTO sangraha.chapters
        (work_id, slug, order_index, title_ru, title_en,
         title_sa_iast, title_sa_devanagari)
        VALUES
            (%s, %s, %s, %s, %s, NULL, NULL)
        ON CONFLICT (work_id, slug) DO UPDATE SET
                                                  order_index = EXCLUDED.order_index,
                                                  title_ru = EXCLUDED.title_ru,
                                                  title_en = EXCLUDED.title_en,
                                                  deleted_at = NULL
        RETURNING id
        """,
        (work_id, slug, chapter.number, title_ru, title_en),
    )
    row = cur.fetchone()
    if not row:
        raise RuntimeError(f"Не удалось получить id главы {chapter.number}")
    return str(row[0])


def upsert_verse(cur, chapter_id: str, verse: Verse) -> str:
    cur.execute(
        """
        SELECT id
        FROM sangraha.verses
        WHERE chapter_id = %s AND order_index = %s
        ORDER BY created_at
        LIMIT 1
        """,
        (chapter_id, verse.number),
    )
    row = cur.fetchone()

    if row:
        verse_id = str(row[0])
        cur.execute(
            """
            UPDATE sangraha.verses
            SET text_devanagari = NULL,
                text_iast = %s,
                raw_text = %s,
                status = 'ANALYZED',
                deleted_at = NULL,
                updated_at = now()
            WHERE id = %s
            """,
            (verse.sanskrit, verse.sanskrit, verse_id),
        )
        return verse_id

    cur.execute(
        """
        INSERT INTO sangraha.verses
        (chapter_id, order_index, text_devanagari, text_iast,
         raw_text, status)
        VALUES
            (%s, %s, NULL, %s, %s, 'ANALYZED')
        RETURNING id
        """,
        (chapter_id, verse.number, verse.sanskrit, verse.sanskrit),
    )
    row = cur.fetchone()
    if not row:
        raise RuntimeError(f"Не удалось получить id стиха {verse.number}")
    return str(row[0])


def upsert_analysis(cur, verse_id: str, translation_ru: str) -> None:
    # translation_en и sandhi_splits в текущей SQL-схеме обязательны,
    # но источник содержит только русский перевод и без разбора сандхи.
    cur.execute(
        """
        INSERT INTO sangraha.verse_analyses
        (verse_id, translation_ru, translation_en, sandhi_splits,
         raw_model_response, model_name)
        VALUES
            (%s, %s, '', '[]'::jsonb, NULL, 'samskrtam.ru-import')
        ON CONFLICT (verse_id) DO UPDATE SET
                                             translation_ru = EXCLUDED.translation_ru,
                                             translation_en = EXCLUDED.translation_en,
                                             sandhi_splits = EXCLUDED.sandhi_splits,
                                             raw_model_response = NULL,
                                             model_name = EXCLUDED.model_name,
                                             analyzed_at = now()
        """,
        (verse_id, translation_ru),
    )


def import_data(conn, metadata: dict, chapters: list[Chapter]) -> tuple[int, int, int]:
    imported_chapters = 0
    imported_verses = 0
    failed_verses = 0

    with conn.cursor() as cur:
        try:
            work_id = upsert_work(cur, metadata)
            conn.commit()
        except Exception:
            conn.rollback()
            logging.exception("Ошибка импорта произведения; импорт остановлен")
            return 0, 0, 0

        for chapter in chapters:
            try:
                chapter_id = upsert_chapter(cur, work_id, chapter)
                conn.commit()
                imported_chapters += 1
            except Exception:
                conn.rollback()
                logging.exception(
                    "Ошибка импорта главы %s; глава пропущена",
                    chapter.number,
                )
                continue

            for verse in chapter.verses:
                # Вложенная транзакция psycopg становится SAVEPOINT:
                # ошибка одного стиха не откатывает предыдущие стихи.
                try:
                    with conn.transaction():
                        with conn.cursor() as verse_cur:
                            verse_id = upsert_verse(
                                verse_cur, chapter_id, verse
                            )
                            upsert_analysis(
                                verse_cur, verse_id, verse.translation_ru
                            )
                    imported_verses += 1
                except Exception:
                    failed_verses += 1
                    logging.exception(
                        "Ошибка импорта: глава %s, стих %s; стих пропущен",
                        chapter.number, verse.number,
                    )

    if failed_verses:
        logging.warning("Пропущено стихов из-за ошибок: %s", failed_verses)

    return 1, imported_chapters, imported_verses


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="скачать и распарсить страницу, но ничего не писать в БД",
    )
    parser.add_argument(
        "--url",
        default=URL,
        help="URL источника",
    )
    args = parser.parse_args()

    logging.basicConfig(
        level=os.getenv("LOG_LEVEL", "INFO").upper(),
        format="%(asctime)s %(levelname)s %(message)s",
    )

    try:
        logging.info("Загрузка %s", args.url)
        html = fetch_html(args.url)
    except Exception:
        logging.exception("Не удалось загрузить страницу")
        return 0

    try:
        metadata, chapters = parse_page(html)
    except Exception:
        logging.exception("Ошибка разбора HTML")
        return 0

    total_verses = sum(len(c.verses) for c in chapters)
    logging.info(
        "Распознано глав: %d, стихов: %d",
        len(chapters), total_verses,
    )

    if len(chapters) != 18:
        logging.warning(
            "Ожидалось 18 глав, найдено %d. Импорт продолжается.",
            len(chapters),
        )
    if total_verses != 700:
        logging.warning(
            "Ожидалось 700 стихов, найдено %d. Импорт продолжается.",
            total_verses,
        )

    for chapter in chapters:
        if not chapter.verses:
            logging.warning(
                "Глава %s распознана, но стихи не найдены",
                chapter.number,
            )

    if args.dry_run:
        for chapter in chapters:
            logging.info(
                "Глава %d: %s; стихов=%d",
                chapter.number,
                chapter.title_ru or chapter.heading_ru,
                len(chapter.verses),
                )
        return 0

    database_url = os.getenv("DATABASE_URL")
    if not database_url:
        logging.error("Не задана переменная окружения DATABASE_URL")
        return 0

    try:
        with psycopg.connect(database_url) as conn:
            work_count, chapter_count, verse_count = import_data(
                conn, metadata, chapters
            )
    except Exception:
        logging.exception("Критическая ошибка подключения/импорта БД")
        return 0

    logging.info(
        "Импорт завершён: works=%d chapters=%d verses=%d",
        work_count, chapter_count, verse_count,
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
