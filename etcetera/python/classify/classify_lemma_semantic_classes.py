#!/usr/bin/env python3

# напиши python скрипт который
# читает семантический классификатор из lingua.semantic_class (parent_id != null)
# читает переводы из currigulum.lemma_translation
# формирует промпт в облачную llm, которая должна привязать семантическую категорию к переводу
# результат привязки пишет в таблицу "curriculum"."lemma_semantic_class"
#
# подключени к БД и выбор LLM сделай аналогично etcetera/python/classify/classify_nominal_lemmas.py

import argparse
import json
import logging
import os
import re
import sys
import time

import yaml

import psycopg
from openai import OpenAI


DEFAULT_BATCH_SIZE = 20
LOG_FILE = r"C:\MyDev\samskrtam\logs/lemma_semantic_class_llm.log"

# Configuration
LLM_MODEL = "deepseek-v4-flash"
ENV_FILE_PATH = r"C:\MyDev\samskrtam\.env"
LLM_CONFIG_PATH = r"C:\MyDev\samskrtam\llm.yaml"


SYSTEM_PROMPT = """
You are an expert in Sanskrit lexicography and semantics.

You are given:
  * a fixed list of available semantic categories (each has an id and a
    human-readable label in ru / en), and
  * a batch of lexical translations (each has an id, the lemma in IAST,
    the language, and the gloss / translation text).

For every translation, decide which of the available semantic categories
it belongs to. A translation may belong to zero, one, or several
categories. Choose only from the provided category ids; never invent
new ones. When unsure, prefer the broader / parent category but only if
it is present in the provided list (note: only leaf categories are
provided, so pick the closest leaf).

Return exactly one result for every input translation id.

Return JSON only.

The JSON response MUST have exactly this structure:

{
  "results": [
    {
      "translation_id": "00000000-0000-0000-0000-000000000000",
      "semantic_class_ids": [
        "11111111-1111-1111-1111-111111111111"
      ]
    }
  ]
}

The field names MUST be "translation_id" and "semantic_class_ids".
Do not rename, omit, or abbreviate any field.
If a translation matches no category, return an empty array.
"""


def setup_logging():
    logger = logging.getLogger("lemma-semantic-class")

    if logger.handlers:
        return logger

    logger.setLevel(logging.DEBUG)
    formatter = logging.Formatter("%(asctime)s | %(levelname)s | %(message)s")

    file_handler = logging.FileHandler(LOG_FILE, encoding="utf-8")
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(formatter)

    console_handler = logging.StreamHandler()
    console_handler.setLevel(logging.INFO)
    console_handler.setFormatter(formatter)

    logger.addHandler(file_handler)
    logger.addHandler(console_handler)

    return logger


def load_env(path):
    """Minimal .env loader (no python-dotenv dependency)."""
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


def load_llm_config(path, model, env):
    """
    Load the selected LLM configuration from llm.yaml.
    String values may contain ${VAR} placeholders resolved from .env.
    """
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

        return re.sub(r"\\$\\{([^}]+)\\}", replace_var, value)

    return {
        key: resolve(value)
        for key, value in llm_config.items()
    }


def create_llm_client(llm_config):
    return OpenAI(
        base_url=required(llm_config, "base-url"),
        api_key=required(llm_config, "api-key"),
    )


def load_semantic_classes(conn):
    """
    Leaf semantic categories from lingua.semantic_class
    (parent_id IS NOT NULL). Returns a list of dicts
    {id, code, name_ru, name_en, label}.
    """
    sql = """
          SELECT id,
                 code,
                 name_ru,
                 name_en,
                 parent_id
          FROM lingua.semantic_class
          WHERE parent_id IS NOT NULL
          ORDER BY code
          """

    classes = []

    with conn.cursor() as cur:
        cur.execute(sql)

        for row in cur.fetchall():
            class_id, code, name_ru, name_en, _parent = row
            label = f"{code}: {name_ru or ''} / {name_en or ''}".strip(" /")

            classes.append(
                {
                    "id": str(class_id),
                    "code": code,
                    "name_ru": name_ru,
                    "name_en": name_en,
                    "label": label,
                }
            )

    return classes


def get_unprocessed_translations(conn, limit):
    """
    Lemmas from curriculum.lemma that are not yet bound to any semantic class.
    Uses the en translation gloss as context for the LLM.
    """
    sql = """
          SELECT l.id,
                 l.lemma_iast,
                 'en',
                 lt.gloss
          FROM curriculum.lemma l
          JOIN curriculum.lemma_translation lt ON lt.lemma_id = l.id
          WHERE lt.language = 'en'
            AND NOT EXISTS (
              SELECT 1
              FROM curriculum.lemma_semantic_class lsc
              WHERE lsc.lemma_id = l.id
          )
          ORDER BY l.lemma_iast
          LIMIT %s \
          """

    with conn.cursor() as cur:
        cur.execute(sql, (limit,))

        return [
            {
                "id": str(row[0]),
                "lemma_iast": row[1],
                "language": row[2],
                "gloss": row[3],
            }
            for row in cur.fetchall()
        ]


def call_llm(client, model, max_completion_tokens, classes, translations, logger):
    """Send one batch to the LLM and return parsed JSON."""

    payload = {
        "semantic_classes": classes,
        "translations": translations,
    }

    messages = [
        {
            "role": "system",
            "content": SYSTEM_PROMPT,
        },
        {
            "role": "user",
            "content": json.dumps(payload, ensure_ascii=False),
        },
    ]

    logger.debug("=" * 100)
    logger.debug("LLM REQUEST")
    logger.debug("model=%s", model)
    logger.debug("max_completion_tokens=%s", max_completion_tokens)
    logger.debug("translation_count=%d", len(translations))
    logger.debug(
        "class_count=%d",
        len(classes),
    )

    logger.debug(
        "REQUEST MESSAGES:\n%s",
        json.dumps(messages, ensure_ascii=False, indent=2),
    )

    started = time.time()

    try:
        response = client.chat.completions.create(
            model=model,
            messages=messages,
            temperature=0,
            max_tokens=max_completion_tokens,
            response_format={"type": "json_object"},
        )

    except Exception:
        elapsed = time.time() - started
        logger.exception("LLM REQUEST FAILED after %.2fs", elapsed)
        raise

    elapsed = time.time() - started
    logger.debug("LLM RESPONSE received in %.2fs", elapsed)

    try:
        raw_response = response.model_dump()
    except Exception:
        raw_response = str(response)

    logger.debug(
        "RAW RESPONSE:\n%s",
        json.dumps(raw_response, ensure_ascii=False, indent=2, default=str),
    )

    if getattr(response, "usage", None):
        try:
            usage = response.usage.model_dump()
        except Exception:
            usage = str(response.usage)

        logger.debug(
            "USAGE:\n%s",
            json.dumps(usage, ensure_ascii=False, indent=2, default=str),
        )

    if not response.choices:
        raise RuntimeError("LLM returned no choices")

    content = response.choices[0].message.content

    logger.debug("RAW CONTENT:\n%s", content or "<EMPTY>")

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
        json.dumps(data, ensure_ascii=False, indent=2),
    )

    logger.debug("=" * 100)

    return data


def validate_results(data, requested_translations, allowed_class_ids, logger):
    """Strictly validate the LLM response before touching DB."""

    if not isinstance(data, dict):
        raise RuntimeError("LLM response must be a JSON object")

    results = data.get("results")

    if not isinstance(results, list):
        raise RuntimeError("LLM response must contain a 'results' array")

    requested = {t["id"] for t in requested_translations}
    validated = {}

    for item in results:
        if not isinstance(item, dict):
            raise RuntimeError(f"Invalid result item: {item!r}")

        translation_id = item.get("translation_id")
        class_ids = item.get("semantic_class_ids")

        if translation_id not in requested:
            raise RuntimeError(
                f"Unexpected translation_id returned by LLM: {translation_id!r}"
            )

        if translation_id in validated:
            raise RuntimeError(
                f"Duplicate result for translation_id: {translation_id}"
            )

        if not isinstance(class_ids, list):
            raise RuntimeError(
                f"semantic_class_ids must be a list for "
                f"{translation_id!r}"
            )

        for cid in class_ids:
            if cid not in allowed_class_ids:
                raise RuntimeError(
                    f"Unknown semantic_class_id {cid!r} for "
                    f"translation {translation_id!r}"
                )

        validated[translation_id] = class_ids

    missing = requested - set(validated)

    if missing:
        examples = sorted(missing)[:20]
        raise RuntimeError(
            f"LLM returned only {len(validated)} of "
            f"{len(requested)} translations.\n"
            f"Missing: {len(missing)}.\n"
            f"Examples: {examples}"
        )

    return validated


def save_results(conn, results, logger):
    """
    Insert one row per (translation, class) binding.
    The composite PK makes this safe to repeat.
    """
    sql = """
          INSERT INTO curriculum.lemma_semantic_class
               (lemma_id, semantic_class_id)
          VALUES (%s, %s)
          ON CONFLICT
              DO NOTHING \
          """

    total_rows = 0

    with conn.cursor() as cur:
        for lemma_id, class_ids in results.items():
            for cid in class_ids:
                cur.execute(sql, (lemma_id, cid))
                total_rows += 1

    conn.commit()

    logger.debug("Database commit successful: %d binding rows", total_rows)


def main():
    parser = argparse.ArgumentParser(
        description=(
            "Bind Sanskrit translations to semantic categories "
            "using an LLM"
        )
    )

    parser.add_argument(
        "--batch-size",
        type=int,
        default=DEFAULT_BATCH_SIZE,
        help=(
            "Number of translations per LLM request "
            f"(default: {DEFAULT_BATCH_SIZE})"
        ),
    )

    args = parser.parse_args()

    logger = setup_logging()

    logger.info("Starting lemma semantic-class binding")

    env = load_env(ENV_FILE_PATH)

    model = LLM_MODEL
    llm_config = load_llm_config(LLM_CONFIG_PATH, model, env)

    max_completion_tokens = int(
        llm_config.get("max-completion-tokens", "128000")
    )

    logger.info("Model: %s", model)
    logger.info("Batch size: %d", args.batch_size)
    logger.info("Max completion tokens: %d", max_completion_tokens)

    client = create_llm_client(llm_config)

    with create_db_connection(env) as conn:
        classes = load_semantic_classes(conn)

        if not classes:
            logger.error(
                "No semantic classes loaded from lingua.semantic_class "
                "(parent_id IS NOT NULL)."
            )
            sys.exit(1)

        logger.info("Loaded %d semantic classes", len(classes))

        allowed_class_ids = {c["id"] for c in classes}

        batch_number = 0

        while True:
            translations = get_unprocessed_translations(
                conn,
                args.batch_size,
            )

            if not translations:
                logger.info("No more unprocessed translations.")
                break

            batch_number += 1

            logger.info(
                "START batch=%d translations=%d",
                batch_number,
                len(translations),
            )

            started = time.time()

            try:
                raw = call_llm(
                    client=client,
                    model=model,
                    max_completion_tokens=max_completion_tokens,
                    classes=classes,
                    translations=translations,
                    logger=logger,
                )

                results = validate_results(
                    raw,
                    translations,
                    allowed_class_ids,
                    logger,
                )

                logger.info(
                    "Validation successful: %d/%d",
                    len(results),
                    len(translations),
                )

                save_results(
                    conn=conn,
                    results=results,
                    logger=logger,
                )

                elapsed = time.time() - started

                bound = sum(1 for v in results.values() if v)
                logger.info(
                    "DONE batch=%d saved=%d translations_with_class=%d "
                    "elapsed=%.2fs",
                    batch_number,
                    len(results),
                    bound,
                    elapsed,
                )

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
