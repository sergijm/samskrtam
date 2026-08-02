#!/usr/bin/env python3
"""
Скрипт для транслитерации declension_forms из IAST в деванагари.
Использует библиотеку indic-transliteration.
"""

import os
import sys
import argparse
import logging
from dotenv import load_dotenv
import psycopg2
from psycopg2 import sql
from indic_transliteration import sanscript
from indic_transliteration.sanscript import transliterate, IAST, DEVANAGARI

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)


def load_env_file(env_file_path):
    """Загрузка .env файла"""
    if not os.path.exists(env_file_path):
        logger.error(f"Файл .env не найден: {env_file_path}")
        sys.exit(1)

    load_dotenv(env_file_path)

    # Проверка наличия всех необходимых переменных
    required_vars = ['DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD']
    missing_vars = [var for var in required_vars if not os.getenv(var)]

    if missing_vars:
        logger.error(f"Отсутствуют переменные в .env файле: {', '.join(missing_vars)}")
        sys.exit(1)

    return {
        'host': os.getenv('DB_HOST'),
        'port': int(os.getenv('DB_PORT')),
        'dbname': os.getenv('DB_NAME'),
        'user': os.getenv('DB_USER'),
        'password': os.getenv('DB_PASSWORD')
    }


def transliterate_iast_to_devanagari(text):
    """Транслитерация из IAST в деванагари"""
    if not text or text.strip() == '':
        return None
    try:
        return transliterate(text.strip(), IAST, DEVANAGARI)
    except Exception as e:
        logger.warning(f"Ошибка транслитерации '{text}': {e}")
        return None


def get_forms_to_update(conn):
    """Получение форм, которые нужно обновить"""
    query = """
            SELECT
                declension_stem_id,
                case_type,
                number_type,
                form_iast,
                form_devanagari
            FROM content.declension_forms
            --WHERE form_devanagari IS NULL OR form_devanagari = ''
            ORDER BY declension_stem_id, case_type, number_type \
            """

    with conn.cursor() as cur:
        cur.execute(query)
        rows = cur.fetchall()
        logger.info(f"Найдено {len(rows)} записей для обновления")
        return rows


def update_form(conn, declension_stem_id, case_type, number_type, devanagari_text):
    """Обновление формы в базе данных"""
    query = """
            UPDATE content.declension_forms
            SET form_devanagari = %s
            WHERE declension_stem_id = %s
              AND case_type = %s
              AND number_type = %s \
            """

    with conn.cursor() as cur:
        cur.execute(query, (devanagari_text, declension_stem_id, case_type, number_type))
        return cur.rowcount


def update_stems(conn):
    """Дополнительно обновляем stem_devanagari в таблице declension_stems если они пустые"""
    query = """
            SELECT
                id,
                stem_iast,
                stem_devanagari
            FROM content.declension_stems
            WHERE stem_devanagari IS NULL OR stem_devanagari = '' \
            """

    with conn.cursor() as cur:
        cur.execute(query)
        stems = cur.fetchall()

        if stems:
            logger.info(f"Найдено {len(stems)} основ для обновления")
            updated_count = 0

            for stem_id, stem_iast, _ in stems:
                devanagari = transliterate_iast_to_devanagari(stem_iast)
                if devanagari:
                    update_query = """
                                   UPDATE content.declension_stems
                                   SET stem_devanagari = %s
                                   WHERE id = %s \
                                   """
                    cur.execute(update_query, (devanagari, stem_id))
                    updated_count += 1
                    logger.debug(f"Обновлена основа: {stem_iast} -> {devanagari}")

            conn.commit()
            logger.info(f"Обновлено {updated_count} основ")
        else:
            logger.info("Все основы уже имеют деванагари")


def main():
    parser = argparse.ArgumentParser(
        description='Транслитерация declension_forms из IAST в деванагари'
    )
    parser.add_argument(
        'env_file',
        help='Путь к .env файлу с настройками подключения к БД'
    )
    parser.add_argument(
        '--batch-size',
        type=int,
        default=100,
        help='Размер пакета для обновления (по умолчанию: 100)'
    )
    parser.add_argument(
        '--dry-run',
        action='store_true',
        help='Пробный запуск без сохранения в БД'
    )
    parser.add_argument(
        '--verbose',
        action='store_true',
        help='Подробный вывод'
    )

    args = parser.parse_args()

    if args.verbose:
        logging.getLogger().setLevel(logging.DEBUG)

    # Загрузка .env файла
    logger.info(f"Загрузка .env файла: {args.env_file}")
    db_config = load_env_file(args.env_file)

    # Подключение к БД
    try:
        conn = psycopg2.connect(**db_config)
        logger.info("Подключение к БД установлено")
    except Exception as e:
        logger.error(f"Ошибка подключения к БД: {e}")
        sys.exit(1)

    try:
        with conn:
            # Обновляем формы
            forms = get_forms_to_update(conn)

            if not forms:
                logger.info("Нет записей для обновления")
                return

            updated_count = 0
            error_count = 0

            for idx, (stem_id, case_type, number_type, form_iast, current_devanagari) in enumerate(forms, 1):
                logger.debug(f"Обработка формы {idx}/{len(forms)}: {form_iast}")

                # Транслитерация
                devanagari = transliterate_iast_to_devanagari(form_iast)

                if devanagari is None:
                    logger.warning(f"Не удалось транслитерировать: {form_iast}")
                    error_count += 1
                    continue

                if args.dry_run:
                    logger.info(f"[DRY RUN] {form_iast} -> {devanagari}")
                    updated_count += 1
                else:
                    # Обновление в БД
                    try:
                        rows = update_form(conn, stem_id, case_type, number_type, devanagari)
                        if rows > 0:
                            updated_count += 1
                            logger.debug(f"Обновлено: {form_iast} -> {devanagari}")
                        else:
                            logger.warning(f"Не найдена запись для обновления: {form_iast}")
                            error_count += 1
                    except Exception as e:
                        logger.error(f"Ошибка обновления '{form_iast}': {e}")
                        error_count += 1

                # Коммит каждые batch_size записей
                if not args.dry_run and idx % args.batch_size == 0:
                    conn.commit()
                    logger.info(f"Сохранено {idx} записей")

            if not args.dry_run:
                conn.commit()

            # Дополнительно обновляем основы
            if not args.dry_run:
                update_stems(conn)

            # Статистика
            logger.info("=" * 50)
            logger.info("Результат:")
            logger.info(f"  Обработано записей: {len(forms)}")
            logger.info(f"  Успешно обновлено: {updated_count}")
            logger.info(f"  Ошибок: {error_count}")
            if args.dry_run:
                logger.info("  (DRY RUN - изменения не сохранены)")
            logger.info("=" * 50)

    except Exception as e:
        logger.error(f"Ошибка выполнения скрипта: {e}")
        conn.rollback()
        sys.exit(1)
    finally:
        conn.close()
        logger.info("Соединение с БД закрыто")


if __name__ == "__main__":
    main()