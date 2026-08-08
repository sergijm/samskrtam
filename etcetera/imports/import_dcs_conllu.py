#!/usr/bin/env python3
"""
Скрипт импорта файлов CoNLL-U из DCS в PostgreSQL со схемой sangraha.
- Название произведения: из первой строки файла.
- Название главы и chapter_id: из шапки файла (## chapter / ## chapter_id).
- Текст стиха (raw_text): из метаданных каждого отдельного предложения/блока.
- Параллельная загрузка с настраиваемым количеством потоков.
"""

import os
import re
import sys
import logging
import argparse
from typing import Dict, Any, Optional, List, Tuple
from concurrent.futures import ThreadPoolExecutor, as_completed
from threading import Lock
import psycopg2
from psycopg2.extras import RealDictCursor
from psycopg2.pool import SimpleConnectionPool

logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s [%(levelname)s] [%(threadName)s] %(message)s',
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

# Глобальная блокировка для работы с кэшами
cache_lock = Lock()


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
    """Извлекает название произведения из первой строки файла"""
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


def parse_conllu_file(file_path: str) -> Tuple[Dict[str, str], List[Tuple[Dict[str, str], List[Dict[str, Any]]]]]:
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

                    if is_first_lines:
                        file_meta[key] = val

                    current_block_meta[key] = val

            if not line:
                is_first_lines = False
                if tokens or current_block_meta:
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
    def __init__(self, db_conn_string: str, max_connections: int = 10):
        self.db_conn_string = db_conn_string
        self.max_connections = max_connections
        self.pool = None
        self._init_pool()

        # Кэши для работы с существующими записями
        self.work_cache = {}
        self.chapter_cache = {}
        self.source_cache = None
        self.cache_lock = Lock()

    def _init_pool(self):
        """Инициализирует пул соединений"""
        try:
            self.pool = SimpleConnectionPool(
                1,  # min connections
                self.max_connections,  # max connections
                self.db_conn_string
            )
            logger.info(f"Пул соединений создан (макс: {self.max_connections})")
        except Exception as e:
            logger.error(f"Ошибка создания пула соединений: {e}")
            raise

    def get_connection(self):
        """Получает соединение из пула"""
        if not self.pool:
            self._init_pool()
        return self.pool.getconn()

    def return_connection(self, conn):
        """Возвращает соединение в пул"""
        if self.pool:
            self.pool.putconn(conn)

    def close(self):
        """Закрывает все соединения в пуле"""
        if self.pool:
            self.pool.closeall()
            logger.info("Все соединения закрыты")

    def get_or_create_source(self) -> str:
        """Получает или создает источник DCS"""
        if self.source_cache:
            return self.source_cache

        conn = self.get_connection()
        try:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute("SELECT id FROM sangraha.sources WHERE code = 'DCS';")
                res = cur.fetchone()
                if res:
                    source_id = res["id"]
                else:
                    cur.execute(
                        """
                        INSERT INTO sangraha.sources (code, title_en, title_ru)
                        VALUES (%s, %s, %s) RETURNING id;
                        """,
                        ("DCS", "Digital Corpus of Sanskrit", "Цифровой корпус санскрита")
                    )
                    source_id = cur.fetchone()["id"]
                    conn.commit()
                self.source_cache = source_id
                return source_id
        finally:
            self.return_connection(conn)

    def check_work_exists(self, work_title: str) -> bool:
        """Проверяет, существует ли уже произведение с таким названием"""
        if work_title in self.work_cache:
            return True

        conn = self.get_connection()
        try:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute("SELECT id FROM sangraha.works WHERE title_en = %s;", (work_title,))
                res = cur.fetchone()
                if res:
                    with self.cache_lock:
                        self.work_cache[work_title] = res["id"]
                    return True
                return False
        finally:
            self.return_connection(conn)

    def get_or_create_work(self, work_title: str) -> str:
        """Получает или создает произведение"""
        if not work_title:
            work_title = "Unknown Work"

        with self.cache_lock:
            if work_title in self.work_cache:
                return self.work_cache[work_title]

        source_id = self.get_or_create_source()
        conn = self.get_connection()

        try:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                cur.execute("SELECT id FROM sangraha.works WHERE title_en = %s;", (work_title,))
                res = cur.fetchone()
                if res:
                    work_id = res["id"]
                else:
                    slug = generate_slug(work_title)
                    cur.execute(
                        """
                        INSERT INTO sangraha.works (slug, title_ru, title_en, title_sa_iast, source_id)
                        VALUES (%s, %s, %s, %s, %s) RETURNING id;
                        """,
                        (slug, work_title, work_title, work_title, source_id)
                    )
                    work_id = cur.fetchone()["id"]
                    conn.commit()

                with self.cache_lock:
                    self.work_cache[work_title] = work_id
                return work_id
        finally:
            self.return_connection(conn)

    def get_or_create_chapter(self, work_id: str, chapter_title: str) -> str:
        """Получает или создает главу"""
        if not chapter_title:
            chapter_title = "General Chapter"

        cache_key = (work_id, chapter_title)
        with self.cache_lock:
            if cache_key in self.chapter_cache:
                return self.chapter_cache[cache_key]

        conn = self.get_connection()
        try:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
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
                    conn.commit()

                with self.cache_lock:
                    self.chapter_cache[cache_key] = chapter_id
                return chapter_id
        finally:
            self.return_connection(conn)

    def insert_verse_data(self, chapter_id: str, verse_num: int, text: str, tokens: List[Dict[str, Any]]):
        """Вставляет данные стиха и его токенов"""
        conn = self.get_connection()
        try:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
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

                conn.commit()
        except Exception as e:
            conn.rollback()
            raise e
        finally:
            self.return_connection(conn)

    def process_single_file(self, file_path: str, work_name: str, dir_name: str) -> Tuple[str, int, int]:
        """
        Обрабатывает один файл и возвращает (имя_файла, кол-во_стихов, статус)
        """
        file_name = os.path.basename(file_path)
        verses_imported = 0
        status = "SUCCESS"

        try:
            # Проверяем, что название произведения совпадает с названием директории
            file_work_name = get_work_name_from_first_line(file_path)
            if file_work_name != dir_name:
                error_msg = f"Название произведения '{file_work_name}' не совпадает с названием директории '{dir_name}'"
                logger.error(f"[{file_name}] {error_msg}")
                return file_name, 0, "SKIPPED: work_name_mismatch"

            # Парсим файл
            file_meta, blocks = parse_conllu_file(file_path)

            # Получаем название главы
            chapter_raw = file_meta.get("chapter")
            chapter_id_meta = file_meta.get("chapter_id")
            if chapter_raw:
                chapter_name = f"{chapter_raw}"
                if chapter_id_meta:
                    chapter_name += f" (id:{chapter_id_meta})"
            else:
                chapter_name = os.path.splitext(file_name)[0]

            # Получаем или создаем work и chapter
            work_id = self.get_or_create_work(work_name)
            chapter_id = self.get_or_create_chapter(work_id, chapter_name)

            # Обрабатываем блоки
            verse_seq = 1
            for block_meta, tokens in blocks:
                if not tokens:
                    continue

                # Получаем текст стиха
                block_text = block_meta.get("text")
                if not block_text or block_text == work_name:
                    text = " ".join(t["form"] for t in tokens if t["form"] != "_")
                else:
                    text = block_text

                self.insert_verse_data(chapter_id, verse_seq, text, tokens)
                verses_imported += 1
                verse_seq += 1

            logger.info(f"[{file_name}] Успешно загружен: {verses_imported} стихов")
            return file_name, verses_imported, status

        except Exception as e:
            error_msg = str(e)
            logger.error(f"[{file_name}] Ошибка: {error_msg}")
            return file_name, 0, f"ERROR: {error_msg[:100]}"


def process_files_parallel(root_dir: str, db_string: str, max_workers: int = 4):
    """
    Параллельная обработка всех .conllu файлов в директории
    """
    # Получаем имя директории
    dir_name = os.path.basename(os.path.normpath(root_dir))

    # Собираем все .conllu файлы
    conllu_files = []
    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith(".conllu"):
                file_path = os.path.join(dirpath, filename)
                conllu_files.append(file_path)

    if not conllu_files:
        logger.warning(f"В директории {root_dir} не найдено .conllu файлов")
        return

    logger.info(f"Найдено {len(conllu_files)} .conllu файлов в директории '{dir_name}'")

    # Проверяем название произведения из первого файла
    first_file = conllu_files[0]
    work_name_from_file = get_work_name_from_first_line(first_file)
    logger.info(f"Название произведения из первого файла: '{work_name_from_file}'")

    # Проверяем, что название произведения совпадает с названием директории
    if work_name_from_file != dir_name:
        logger.error(f"Название произведения '{work_name_from_file}' не совпадает с названием директории '{dir_name}'")
        logger.error("Импорт отменен. Переименуйте директорию или исправьте названия в файлах.")
        return

    # Проверяем, не загружено ли уже это произведение
    importer = SangrahaImporter(db_string, max_connections=max_workers + 2)

    try:
        if importer.check_work_exists(work_name_from_file):
            logger.warning(f"Произведение '{work_name_from_file}' уже существует в базе данных")
            logger.warning("Импорт отменен. Для перезагрузки удалите существующее произведение.")
            return

        logger.info(f"Начинаем параллельную загрузку {len(conllu_files)} файлов (потоков: {max_workers})")

        total_verses = 0
        processed_files = 0
        failed_files = 0

        # Параллельная обработка файлов
        with ThreadPoolExecutor(max_workers=max_workers, thread_name_prefix="Importer") as executor:
            # Запускаем обработку всех файлов
            future_to_file = {
                executor.submit(importer.process_single_file, file_path, work_name_from_file, dir_name): file_path
                for file_path in conllu_files
            }

            # Обрабатываем результаты по мере завершения
            for future in as_completed(future_to_file):
                file_path = future_to_file[future]
                file_name = os.path.basename(file_path)

                try:
                    result_file_name, verses, status = future.result(timeout=60)
                    processed_files += 1
                    total_verses += verses

                    if "ERROR" in status or "SKIPPED" in status:
                        failed_files += 1

                except Exception as e:
                    logger.error(f"[{file_name}] Критическая ошибка при обработке: {e}")
                    failed_files += 1
                    processed_files += 1

        # Итоговая статистика
        logger.info("=" * 60)
        logger.info(f"Импорт завершен!")
        logger.info(f"Директория: {root_dir}")
        logger.info(f"Название произведения: '{work_name_from_file}'")
        logger.info(f"Всего файлов: {len(conllu_files)}")
        logger.info(f"Обработано: {processed_files}")
        logger.info(f"Успешно загружено: {processed_files - failed_files}")
        logger.info(f"С ошибками: {failed_files}")
        logger.info(f"Всего стихов: {total_verses}")
        logger.info("=" * 60)

    finally:
        importer.close()


def main():
    parser = argparse.ArgumentParser(description="Импорт CoNLL-U файлов в параллельном режиме")
    parser.add_argument("--db", required=True, help="Строка подключения к PostgreSQL")
    parser.add_argument("--dir", required=True, help="Путь к директории с .conllu файлами")
    parser.add_argument("--threads", type=int, default=4, help="Количество потоков (по умолчанию: 4)")
    args = parser.parse_args()

    if not os.path.exists(args.dir):
        sys.exit(f"Директория не найдена: {args.dir}")

    if args.threads < 1 or args.threads > 50:
        logger.warning(f"Количество потоков {args.threads} вне допустимого диапазона (1-50). Используем 4.")
        args.threads = 4

    process_files_parallel(args.dir, args.db, args.threads)


if __name__ == "__main__":
    main()