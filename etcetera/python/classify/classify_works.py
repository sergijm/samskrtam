#!/usr/bin/env python3

import argparse
import json
import logging
import os
import re
import sys
import time
import uuid
from collections import Counter

import yaml
import psycopg
from openai import OpenAI

# Configuration - используем те же параметры, что и в исходном скрипте
DEFAULT_BATCH_SIZE = 80
LOG_FILE = "C:\\MyDev\\samskrtam\\logs\\works_classifier_llm.log"
LLM_MODEL = "deepseek-v4-pro"
ENV_FILE_PATH = "C:\\MyDev\\samskrtam\\.env"
LLM_CONFIG_PATH = "C:\\MyDev\\samskrtam\\llm.yaml"

SYSTEM_PROMPT = """
You are an expert in Sanskrit literature classification.

For each Sanskrit work title provided in IAST, you need to:
1. Translate the title into Russian and English
2. Classify the work according to the traditional Sanskrit literary categories

Available classification codes (use these exact codes):
- veda: Veda (Веды)
- samhita: Saṃhitā (Самхита)
- brahmana: Brāhmaṇa (Брахмана)
- aranyaka: Āraṇyaka (Араньяка)
- upanishad: Upaniṣad (Упанишада)
- vedanga: Vedāṅga (Веданги)
- shiksha: Śikṣā (Шикша)
- kalpa: Kalpa (Кальпа)
- shrautasutra: Śrautasūtra (Шраута-сутра)
- grihyasutra: Gṛhyasūtra (Грихья-сутра)
- dharmasutra: Dharmasūtra (Дхарма-сутра)
- vyakarana: Vyākaraṇa (Грамматика)
- nirukta: Nirukta (Нирукта)
- jyotisha: Jyotiṣa (Джьотиша)
- chandas: Chandas (Чхандас)
- itihasa: Itihāsa (Итихаса)
- purana: Purāṇa (Пурана)
- smriti: Smṛti (Смрити)
- darshana: Darśana (Даршана)
- nyaya: Nyāya (Ньяя)
- vaisheshika: Vaiśeṣika (Вайшешика)
- samkhya: Sāṃkhya (Санкхья)
- yoga: Yoga (Йога)
- mimamsa: Mīmāṃsā (Миманса)
- vedanta: Vedānta (Веданта)
- buddhist: Bauddha (Буддизм)
- jaina: Jaina (Джайнизм)
- agama: Āgama (Агама)
- tantra: Tantra (Тантра)
- ayurveda: Āyurveda (Аюрведа)
- rasashastra: Rasaśāstra (Расашастра)
- kavya: Kāvya (Кавья)
- mahakavya: Mahākāvya (Махакавья)
- shataka: Śataka (Шатака)
- katha: Kathā (Катха)
- dutakavya: Dūtakāvya (Дутакавья)
- nataka: Nāṭaka (Драма)
- kosha: Kośa (Коша)
- nighantu: Nighaṇṭu (Нигханту)
- stotra: Stotra (Стотра)
- commentary: Commentary (Комментарий)
- bhashya: Bhāṣya (Бхашья)
- vrtti: Vṛtti (Вритти)
- tika: Ṭīkā (Тика)
- vivriti: Vivṛti (Виврити)
- nibandha: Nibandha (Нибандха)

Return JSON only with exactly this structure:
{
  "results": [
    {
      "id": "uuid_string_from_input",
      "title_sa_iast": "original_title",
      "title_ru": "translation_in_russian",
      "title_en": "translation_in_english",
      "classification_codes": ["code1", "code2"]
    }
  ]
}

For each work, provide:
1. Accurate Russian translation
2. Accurate English translation  
3. One or more classification codes that best describe the work

If uncertain, provide your best guess and include all possible classifications.
Do not rename, omit, or abbreviate any field.
"""


def setup_logging():
    """Настройка логирования как в исходном скрипте"""
    logger = logging.getLogger("works-classifier")
    logger.setLevel(logging.DEBUG)

    if logger.handlers:
        return logger

    formatter = logging.Formatter(
        "%(asctime)s | %(levelname)s | %(message)s"
    )

    file_handler = logging.FileHandler(
        LOG_FILE,
        encoding="utf-8",
    )
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(formatter)

    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(formatter)

    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

    return logger


def load_env(path):
    """Загрузка .env файла как в исходном скрипте"""
    env = {}

    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()

            if not line or line.startswith("#"):
                continue

            if "=" not in line:
                continue

            key, value = line.split("=", 1)

            key = key.strip()
            value = value.strip()

            if (
                    len(value) >= 2
                    and value[0] == value[-1]
                    and value[0] in ("'", '"')
            ):
                value = value[1:-1]

            env[key] = value

    return env


def required(env, name):
    """Получение обязательной переменной окружения"""
    value = env.get(name)

    if not value:
        raise RuntimeError(
            f"Missing required environment variable: {name}"
        )

    return value


def create_db_connection(env):
    """Создание подключения к БД"""
    return psycopg.connect(
        host=required(env, "DB_HOST"),
        port=int(env.get("DB_PORT", "5432")),
        dbname=required(env, "DB_NAME"),
        user=required(env, "DB_USER"),
        password=required(env, "DB_PASSWORD"),
    )


def load_llm_config(path, model, env):
    """Загрузка конфигурации LLM"""
    with open(path, "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)

    try:
        llm_config = config["llm"]["configs"][model]
    except (TypeError, KeyError):
        available = sorted(
            config.get("llm", {}).get("configs", {}).keys()
            if isinstance(config, dict)
            else []
        )
        raise RuntimeError(
            f"LLM model {model!r} not found in {path}. "
            f"Available models: {available}"
        )

    def resolve(value):
        if not isinstance(value, str):
            return value

        def replace_var(match):
            name = match.group(1)
            return env.get(name, os.environ.get(name, match.group(0)))

        return re.sub(r"\$\{([^}]+)\}", replace_var, value)

    return {
        key: resolve(value)
        for key, value in llm_config.items()
    }


def create_llm_client(llm_config):
    """Создание клиента OpenAI"""
    return OpenAI(
        base_url=required(llm_config, "base-url"),
        api_key=required(llm_config, "api-key"),
    )


def get_classification_mapping(conn):
    """
    Получение маппинга кодов классификации на UUID
    Возвращает словарь {code: uuid}
    """
    sql = """
          SELECT code, id
          FROM sangraha.works_class \
          """

    with conn.cursor() as cur:
        cur.execute(sql)
        return {row[0]: row[1] for row in cur.fetchall()}


def get_unprocessed_works(conn, limit):
    """
    Получение произведений, у которых нет перевода и классификации
    """
    sql = """
          SELECT
              w.id,
              w.title_sa_iast
          FROM sangraha.works w
          WHERE w.title_sa_iast IS NOT NULL
            AND w.title_sa_iast != ''
            AND (
              w.title_ru IS NULL
                  OR w.title_ru = ''
                  OR w.title_en IS NULL
                  OR w.title_en = ''
                  OR NOT EXISTS (
                  SELECT 1
                  FROM sangraha.works_work_class wwc
                  WHERE wwc.work_id = w.id
              )
              )
          ORDER BY w.created_at
          LIMIT %s \
          """

    with conn.cursor() as cur:
        cur.execute(sql, (limit,))
        # Возвращаем словарь {str(id): title}
        return {str(row[0]): row[1] for row in cur.fetchall()}


def call_llm(client, model, max_completion_tokens, works_dict, logger):
    """
    Отправка запроса к LLM для классификации произведений
    """
    # Формируем список объектов для отправки
    works_list = [
        {"id": work_id, "title_sa_iast": title}
        for work_id, title in works_dict.items()
    ]

    payload = {"works": works_list}

    messages = [
        {
            "role": "system",
            "content": SYSTEM_PROMPT,
        },
        {
            "role": "user",
            "content": json.dumps(
                payload,
                ensure_ascii=False,
            ),
        },
    ]

    logger.debug("=" * 100)
    logger.debug("LLM REQUEST")
    logger.debug("model=%s", model)
    logger.debug(
        "max_completion_tokens=%s",
        max_completion_tokens,
    )
    logger.debug(
        "work_count=%d",
        len(works_dict),
    )

    logger.debug(
        "REQUEST MESSAGES:\n%s",
        json.dumps(
            messages,
            ensure_ascii=False,
            indent=2,
        ),
    )

    started = time.time()

    try:
        response = client.chat.completions.create(
            model=model,
            messages=messages,
            temperature=0,
            max_tokens=max_completion_tokens,
            response_format={
                "type": "json_object"
            },
        )

    except Exception:
        elapsed = time.time() - started
        logger.exception(
            "LLM REQUEST FAILED after %.2fs",
            elapsed,
        )
        raise

    elapsed = time.time() - started

    logger.debug(
        "LLM RESPONSE received in %.2fs",
        elapsed,
    )

    # Полный ответ SDK
    try:
        raw_response = response.model_dump()
    except Exception:
        raw_response = str(response)

    logger.debug(
        "RAW RESPONSE:\n%s",
        json.dumps(
            raw_response,
            ensure_ascii=False,
            indent=2,
            default=str,
        ),
    )

    # Использование токенов
    if getattr(response, "usage", None):
        try:
            usage = response.usage.model_dump()
        except Exception:
            usage = str(response.usage)

        logger.debug(
            "USAGE:\n%s",
            json.dumps(
                usage,
                ensure_ascii=False,
                indent=2,
                default=str,
            ),
        )

    if not response.choices:
        raise RuntimeError("LLM returned no choices")

    content = response.choices[0].message.content

    logger.debug(
        "RAW CONTENT:\n%s",
        content or "<EMPTY>",
        )

    if not content:
        raise RuntimeError("LLM returned empty response")

    try:
        data = json.loads(content)
    except json.JSONDecodeError as e:
        logger.error("JSON PARSE ERROR: %s", e)
        logger.error("INVALID JSON CONTENT:\n%s", content)
        raise RuntimeError(f"LLM returned invalid JSON: {e}")

    logger.debug(
        "PARSED JSON:\n%s",
        json.dumps(
            data,
            ensure_ascii=False,
            indent=2,
        ),
    )

    logger.debug("=" * 100)

    return data


def validate_results(data, requested_works):
    """
    Валидация ответа LLM
    """
    if not isinstance(data, dict):
        raise RuntimeError("LLM response must be a JSON object")

    results = data.get("results")

    if not isinstance(results, list):
        raise RuntimeError("LLM response must contain a 'results' array")

    requested_ids = set(requested_works.keys())
    validated = {}

    for item in results:
        if not isinstance(item, dict):
            raise RuntimeError(f"Invalid result item: {item!r}")

        work_id = item.get("id")
        title_sa_iast = item.get("title_sa_iast")
        title_ru = item.get("title_ru")
        title_en = item.get("title_en")
        classification_codes = item.get("classification_codes", [])

        # Проверяем, что work_id присутствует и является строкой
        if not work_id:
            raise RuntimeError(f"Missing id in result item: {item!r}")

        work_id_str = str(work_id)

        if work_id_str not in requested_ids:
            raise RuntimeError(
                f"Unexpected work ID returned by LLM: {work_id_str!r}"
            )

        if work_id_str in validated:
            raise RuntimeError(
                f"Duplicate result for work ID: {work_id_str}"
            )

        if not title_ru or not title_en:
            raise RuntimeError(
                f"Missing translation for work: {work_id_str}"
            )

        if not isinstance(classification_codes, list):
            raise RuntimeError(
                f"classification_codes must be a list for work: {work_id_str}"
            )

        validated[work_id_str] = {
            "id": work_id_str,
            "title_sa_iast": title_sa_iast,
            "title_ru": title_ru,
            "title_en": title_en,
            "classification_codes": classification_codes,
        }

    missing = requested_ids - set(validated)

    if missing:
        examples = sorted(missing)[:20]
        raise RuntimeError(
            f"LLM returned only {len(validated)} of {len(requested_ids)} works.\n"
            f"Missing: {len(missing)}.\n"
            f"Examples: {examples}"
        )

    return validated


def save_results(conn, results, class_mapping, logger):
    """
    Сохранение результатов в БД
    """
    with conn.cursor() as cur:
        # Обновляем переводы в таблице works
        update_works_sql = """
                           UPDATE sangraha.works
                           SET
                               title_ru = %s,
                               title_en = %s
                           WHERE id = %s::uuid \
                           """

        # Удаляем старые связи для обновляемых произведений
        delete_class_sql = """
                           DELETE FROM sangraha.works_work_class
                           WHERE work_id = %s::uuid \
                           """

        # Вставляем новые связи
        insert_class_sql = """
                           INSERT INTO sangraha.works_work_class (work_id, class_id)
                           VALUES (%s::uuid, %s::uuid) \
                           """

        for work_id, result in results.items():
            # Обновляем переводы
            cur.execute(
                update_works_sql,
                (
                    result["title_ru"],
                    result["title_en"],
                    work_id,
                )
            )

            # Удаляем старые классификации
            cur.execute(delete_class_sql, (work_id,))

            # Вставляем новые классификации
            for code in result["classification_codes"]:
                class_id = class_mapping.get(code)
                if class_id:
                    cur.execute(
                        insert_class_sql,
                        (work_id, str(class_id))
                    )
                else:
                    logger.warning(
                        "Unknown classification code: %s for work %s",
                        code,
                        work_id
                    )

    conn.commit()

    logger.debug(
        "Database commit successful: %d works processed",
        len(results),
    )


def log_classification_statistics(results, logger):
    """Логирование статистики классификации"""
    all_codes = []
    for result in results.values():
        all_codes.extend(result["classification_codes"])

    code_counts = Counter(all_codes)

    logger.info("Classification statistics:")
    for code, count in sorted(code_counts.items()):
        logger.info("  %-20s %d", code, count)


def main():
    parser = argparse.ArgumentParser(
        description="Classify Sanskrit works using LLM"
    )

    parser.add_argument(
        "--batch-size",
        type=int,
        default=DEFAULT_BATCH_SIZE,
        help=f"Number of works per LLM request (default: {DEFAULT_BATCH_SIZE})",
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Perform a dry run without saving to database",
    )

    args = parser.parse_args()

    logger = setup_logging()

    logger.info("Starting Sanskrit works classification")
    logger.info("Log file: %s", LOG_FILE)

    env = load_env(ENV_FILE_PATH)

    model = LLM_MODEL
    llm_config = load_llm_config(
        LLM_CONFIG_PATH,
        model,
        env,
    )

    max_completion_tokens = int(
        llm_config.get(
            "max-completion-tokens",
            "128000",
        )
    )

    logger.info("Model: %s", model)
    logger.info("Batch size: %d", args.batch_size)
    logger.info("Max completion tokens: %d", max_completion_tokens)

    client = create_llm_client(llm_config)

    with create_db_connection(env) as conn:
        # Получаем маппинг классификаций
        class_mapping = get_classification_mapping(conn)
        logger.info("Loaded %d classification codes", len(class_mapping))

        batch_number = 0

        while True:
            # Получаем словарь {str(id): title}
            works_dict = get_unprocessed_works(
                conn,
                args.batch_size,
            )

            if not works_dict:
                logger.info("No more unprocessed works.")
                break

            batch_number += 1

            logger.info(
                "START batch=%d works=%d",
                batch_number,
                len(works_dict),
            )

            logger.debug(
                "BATCH WORKS:\n%s",
                json.dumps(
                    works_dict,
                    ensure_ascii=False,
                    indent=2,
                ),
            )

            started = time.time()

            try:
                raw = call_llm(
                    client=client,
                    model=model,
                    max_completion_tokens=max_completion_tokens,
                    works_dict=works_dict,
                    logger=logger,
                )

                results = validate_results(
                    raw,
                    works_dict,
                )

                logger.info(
                    "Validation successful: %d/%d",
                    len(results),
                    len(works_dict),
                )

                if not args.dry_run:
                    save_results(
                        conn=conn,
                        results=results,
                        class_mapping=class_mapping,
                        logger=logger,
                    )

                    logger.info(
                        "Results saved to database"
                    )
                else:
                    logger.info(
                        "DRY RUN: Results would be saved to database"
                    )
                    # В dry-run режиме показываем результаты
                    for work_id, result in results.items():
                        logger.info(
                            "Work: %s, RU: %s, EN: %s, Codes: %s",
                            result["title_sa_iast"],
                            result["title_ru"],
                            result["title_en"],
                            result["classification_codes"]
                        )

                elapsed = time.time() - started

                logger.info(
                    "DONE batch=%d processed=%d elapsed=%.2fs",
                    batch_number,
                    len(results),
                    elapsed,
                )

                log_classification_statistics(results, logger)

            except Exception:
                conn.rollback()
                logger.exception(
                    "BATCH %d FAILED — database transaction rolled back",
                    batch_number,
                )
                sys.exit(1)

    logger.info("Finished successfully.")


if __name__ == "__main__":
    main()