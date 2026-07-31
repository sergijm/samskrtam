#!/usr/bin/env python3
"""
Скрипт импорта файлов CoNLL-U из DCS в PostgreSQL со схемой sangraha.
- Название произведения: из первой строки файла.
- Название главы и chapter_id: из шапки файла (## chapter / ## chapter_id).
- Текст стиха (raw_text): из метаданных каждого отдельного предложения/блока.
"""

import os
import re
import sys
import logging
import argparse
from typing import Dict, Any, Optional, List
import psycopg2
from psycopg2.extras import RealDictCursor

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] %(message)s',
    handlers=[
        logging.FileHandler("conllu_import.log", encoding='utf-8'),
        logging.StreamHandler(sys.stdout)
    ]
)
logger = logging.getLogger("dcs_importer")

STATUS_DRAFT = "DRAFT"

POS_MAPPING = {
    "NOUN": "NOUN", "PROPN": "NOUN", "VERB": "VERB",
    "ADJ": "ADJECTIVE", "PRON": "PRONOUN", "ADV": "ADVERB",
    "CCONJ": "CONJUNCTION", "SCONJ": "CONJUNCTION", "ADP": "INDECLINABLE",
    "PART": "PARTICLE", "INTJ": "INTERJECTION", "NUM": "NUMERAL",
    "IND": "INDECLINABLE", "X": "OTHER", "PUNCT": "OTHER"
}

GENDER_MAPPING = {
    "MASC": "MASCULINE", "FEM": "FEMININE", "NEUT": "NEUTER",
    "M": "MASCULINE", "F": "FEMININE", "N": "NEUTER"
}

CASE_MAPPING = {
    "NOM": "NOMINATIVE", "ACC": "ACCUSATIVE", "INS": "INSTRUMENTAL",
    "DAT": "DATIVE", "ABL": "ABLATIVE", "GEN": "GENITIVE",
    "LOC": "LOCATIVE", "VOC": "VOCATIVE"
}

NUMBER_MAPPING = {
    "SING": "SINGULAR", "DUAL": "DUAL", "PLUR": "PLURAL",
    "SG": "SINGULAR", "DU": "DUAL", "PL": "PLURAL"
}

PERSON_MAPPING = {"1": "FIRST", "2": "SECOND", "3": "THIRD"}

TENSE_MAPPING = {
    "PRES": "PRESENT", "PAST": "IMPERFECT", "FUT": "FUTURE",
    "IMP": "IMPERFECT", "PERF": "PERFECT", "AOR": "AORIST", "COND": "CONDITIONAL"
}

MOOD_MAPPING = {
    "IND": "INDICATIVE", "IMP": "IMPERATIVE", "SUB": "OPTATIVE",
    "OPT": "OPTATIVE", "BEN": "BENEDICTIVE", "PREC": "BENEDICTIVE"
}

VOICE_MAPPING = {"ACT": "ACTIVE", "PASS": "PASSIVE", "MID": "MIDDLE"}

FORM_TYPE_MAPPING = {
    "FIN": "FINITE", "PART": "PARTICIPLE", "INF": "INFINITIVE",
    "GER": "GERUNDIVE", "ABS": "ABSOLUTIVE"
}

def parse_feats(feats_str: str) -> Dict[str, str]:
    feats = {}
    if not feats_str or feats_str == "_":
        return feats
    for part in feats_str.split("|"):
        if "=" in part:
            k, v = part.split("=", 1)
            feats[k.strip().lower()] = v.strip().upper()
    return feats

def map_value(mapping: Dict[str, str], raw_val: Optional[str]) -> Optional[str]:
    if not raw_val or raw_val == "_":
        return None
    return mapping.get(raw_val.upper())

def generate_slug(text: str) -> str:
    slug = re.sub(r'[^a-z0-9]+', '-', text.lower()).strip('-')
    return slug if slug else "unknown"

def get_work_name_from_first_line(file_path: str) -> str:
    try:
        with open(file_path, "r", encoding="utf-8") as f:
            first_line = f.readline().strip()
            if not first_line:
                return os.path.splitext(os.path.basename(file_path))[0]

            cleaned = re.sub(r'^#+\s*', '', first_line)
            if ":" in cleaned:
                key, val = cleaned.split(":", 1)
                val = val.strip()
                if val:
                    return val
            if cleaned:
                return cleaned
    except Exception as e:
        logger.warning(f"Не удалось прочитать первую строку файла {file_path}: {e}")

    return os.path.splitext(os.path.basename(file_path))[0]

def parse_conllu_file(file_path: str):
    """
    Парсит conllu-файл.
    Собирает общие метаданные файла (включая шапку), а также список блоков (метаданные блока + токены).
    """
    file_meta = {}
    blocks = []

    with open(file_path, "r", encoding="utf-8") as f:
        current_block_meta = {}
        tokens = []
        is_first_lines = True

        for line in f:
            raw_line = line
            line = line.strip()

            if line.startswith("#"):
                cleaned = re.sub(r'^#+\s*', '', line)
                if ":" in cleaned:
                    k, v = cleaned.split(":", 1)
                    key = k.strip().lower()
                    val = v.strip()

                    # Запоминаем глобальные метаданные (глава, id файла и т.д.) пока не встретили пустую строку или токены
                    if is_first_lines:
                        file_meta[key] = val

                    current_block_meta[key] = val

            if not line:
                is_first_lines = False
                if tokens or current_block_meta:
                    # Сохраняем блок (копируем метаданные блока)
                    blocks.append((dict(current_block_meta), tokens))
                    current_block_meta = {}
                    tokens = []
                continue

            if not line.startswith("#"):
                is_first_lines = False
                parts = line.split("\t")
                if len(parts) >= 10:
                    if "-" in parts[0] or "." in parts[0]:
                        continue
                    tokens.append({
                        "id": parts[0], "form": parts[1], "lemma": parts[2],
                        "upos": parts[3], "xpos": parts[4], "feats": parse_feats(parts[5]),
                    })

        if tokens or current_block_meta:
            blocks.append((dict(current_block_meta), tokens))

    return file_meta, blocks

class SangrahaImporter:
    def __init__(self, db_conn_string: str):
        self.conn = psycopg2.connect(db_conn_string)
        self.conn.autocommit = False
        self.work_cache = {}
        self.chapter_cache = {}

    def close(self):
        if self.conn:
            self.conn.close()

    def get_or_create_work(self, work_title: str) -> str:
        if not work_title:
            work_title = "Unknown Work"
        if work_title in self.work_cache:
            return self.work_cache[work_title]

        with self.conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute("SELECT id FROM sangraha.works WHERE title_en = %s;", (work_title,))
            res = cur.fetchone()
            if res:
                work_id = res["id"]
            else:
                slug = generate_slug(work_title)
                cur.execute(
                    """
                    INSERT INTO sangraha.works (slug, title_ru, title_en, title_sa_iast)
                    VALUES (%s, %s, %s, %s) RETURNING id;
                    """,
                    (slug, work_title, work_title, work_title)
                )
                work_id = cur.fetchone()["id"]
                self.conn.commit()

            self.work_cache[work_title] = work_id
            return work_id

    def get_or_create_chapter(self, work_id: str, chapter_title: str) -> str:
        if not chapter_title:
            chapter_title = "General Chapter"

        cache_key = (work_id, chapter_title)
        if cache_key in self.chapter_cache:
            return self.chapter_cache[cache_key]

        with self.conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(
                "SELECT id FROM sangraha.chapters WHERE work_id = %s AND title_en = %s;",
                (work_id, chapter_title)
            )
            res = cur.fetchone()
            if res:
                chapter_id = res["id"]
            else:
                cur.execute(
                    "SELECT COALESCE(MAX(order_index), 0) + 1 AS next_idx FROM sangraha.chapters WHERE work_id = %s;",
                    (work_id,)
                )
                next_order_index = cur.fetchone()["next_idx"]

                slug = generate_slug(f"ch-{chapter_title}")
                cur.execute(
                    """
                    INSERT INTO sangraha.chapters (work_id, slug, order_index, title_ru, title_en)
                    VALUES (%s, %s, %s, %s, %s) RETURNING id;
                    """,
                    (work_id, slug, next_order_index, chapter_title, chapter_title)
                )
                chapter_id = cur.fetchone()["id"]
                self.conn.commit()

            self.chapter_cache[cache_key] = chapter_id
            return chapter_id

    def insert_verse_data(self, chapter_id: str, verse_num: int, text: str, tokens: List[Dict[str, Any]]):
        with self.conn.cursor(cursor_factory=RealDictCursor) as cur:
            cur.execute(
                """
                INSERT INTO sangraha.verses (chapter_id, order_index, raw_text, status)
                VALUES (%s, %s, %s, %s) RETURNING id;
                """,
                (chapter_id, verse_num, text, STATUS_DRAFT)
            )
            verse_id = cur.fetchone()["id"]

            for idx, token in enumerate(tokens, start=1):
                pos_enum = map_value(POS_MAPPING, token["upos"]) or map_value(POS_MAPPING, token["xpos"])
                feats = token["feats"]
                form_type = map_value(FORM_TYPE_MAPPING, feats.get("verbform"))

                form_val = token["form"] if token["form"] != "_" else ""
                lemma_val = token["lemma"] if token["lemma"] != "_" else ""

                cur.execute(
                    """
                    INSERT INTO sangraha.verse_words (
                        verse_id, position, surface_iast, surface_devanagari,
                        lemma_iast, pos, form_type, context_gloss_ru, context_gloss_en
                    ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s) RETURNING id;
                    """,
                    (
                        verse_id, idx, form_val, form_val, lemma_val,
                        pos_enum, form_type, "", ""
                    )
                )
                word_id = cur.fetchone()["id"]

                gender = map_value(GENDER_MAPPING, feats.get("gender"))
                g_case = map_value(CASE_MAPPING, feats.get("case"))
                num_type = map_value(NUMBER_MAPPING, feats.get("number"))
                person = map_value(PERSON_MAPPING, feats.get("person"))
                tense = map_value(TENSE_MAPPING, feats.get("tense"))
                mood = map_value(MOOD_MAPPING, feats.get("mood"))
                voice = map_value(VOICE_MAPPING, feats.get("voice"))

                if any([g_case, gender, num_type, person, tense, mood, voice]):
                    cur.execute(
                        """
                        INSERT INTO sangraha.verse_word_morphology (
                            verse_word_id, case_type, gender, number_type,
                            person, tense, mood, voice
                        ) VALUES (%s, %s, %s, %s, %s, %s, %s, %s);
                        """,
                        (word_id, g_case, gender, num_type, person, tense, mood, voice)
                    )

            self.conn.commit()

def process_files(root_dir: str, db_string: str):
    importer = SangrahaImporter(db_string)
    total_files = 0
    total_verses = 0

    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith(".conllu"):
                total_files += 1
                file_path = os.path.join(dirpath, filename)

                # 1. Название произведения строго из первой строки файла
                work_name = get_work_name_from_first_line(file_path)

                # 2. Парсим файл на метаданные файла и блоки предложений
                file_meta, blocks = parse_conllu_file(file_path)

                # 3. Название главы и id из шапки файла
                chapter_raw = file_meta.get("chapter")
                chapter_id_meta = file_meta.get("chapter_id")

                if chapter_raw:
                    chapter_name = f"{chapter_raw}"
                    if chapter_id_meta:
                        chapter_name += f" (id:{chapter_id_meta})"
                else:
                    chapter_name = os.path.splitext(filename)[0]

                try:
                    work_id = importer.get_or_create_work(work_name)
                    chapter_id = importer.get_or_create_chapter(work_id, chapter_name)
                except Exception as e:
                    logger.error(f"Сбой Work/Chapter для файла {file_path}: {e}")
                    importer.conn.rollback()
                    continue

                verse_seq = 1
                for block_meta, tokens in blocks:
                    if not tokens:
                        continue

                    try:
                        # Текст стиха берем из конкретного блока (# text: ...), если есть,
                        # либо собираем из токенов, игнорируя шапочный work_name/text произведения
                        block_text = block_meta.get("text")
                        # Если в блоке под ключ text записалось название произведения (совпадает с work_name),
                        # то заменяем его на склейку токенов.
                        if not block_text or block_text == work_name:
                            text = " ".join(t["form"] for t in tokens if t["form"] != "_")
                        else:
                            text = block_text

                        importer.insert_verse_data(chapter_id, verse_seq, text, tokens)
                        total_verses += 1
                        verse_seq += 1
                    except Exception as e:
                        logger.error(f"Сбой стиха {verse_seq} в файле {file_path}: {e}")
                        importer.conn.rollback()

    importer.close()
    logger.info(f"Импорт завершен. Файлов обработано: {total_files}, Стихов/предложений: {total_verses}")

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--db", required=True)
    parser.add_argument("--dir", required=True)
    args = parser.parse_args()

    if not os.path.exists(args.dir):
        sys.exit(f"Директория не найдена: {args.dir}")
    process_files(args.dir, args.db)

if __name__ == "__main__":
    main()