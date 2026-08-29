#!/usr/bin/env python3

import argparse
import json
import logging
import sys
import time

import psycopg
from openai import OpenAI


DEFAULT_BATCH_SIZE = 20
LOG_FILE = r"C:\MyDev\samskrtam\logs/lemma_semantic_class_llm.log"

LLM_MODEL = "deepseek-v4-flash"
ENV_FILE_PATH = r"C:\MyDev\samskrtam\.env"
LLM_BASE_URL = "https://api.aitunnel.ru/v1"


SYSTEM_PROMPT = """
You are an expert in Sanskrit lexicography and semantics.

You are given:
  * a fixed list of available semantic categories (each has an id and a
    human-readable label in ru), and
  * a batch of lemmas (each has an id, the lemma in IAST, and the Russian
    gloss / translation text).

For every lemma, decide which of the available semantic categories
it belongs to. A lemma may belong to zero, one, or several
categories. Choose only from the provided category ids; never invent
new ones. When unsure, prefer the broader / parent category but only if
it is present in the provided list (note: only leaf categories are
provided, so pick the closest leaf).

Return exactly one result for every input lemma id.

Return JSON only.

The JSON response MUST have exactly this structure:

{
  "results": [
    {
      "lemma_id": "00000000-0000-0000-0000-000000000000",
      "semantic_class_ids": [
        "11111111-1111-1111-1111-111111111111"
      ]
    }
  ]
}

The field names MUST be "lemma_id" and "semantic_class_ids".
Do not rename, omit, or abbreviate any field.
If a lemma matches no category, return an empty array.
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


def create_llm_client(env):
    return OpenAI(
        base_url=LLM_BASE_URL,
        api_key=required(env, "LLM_API_KEY"),
    )


def load_semantic_classes(conn):
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
            label = f"{code}: {name_ru or ''}".strip(" /")

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


def get_unprocessed_lemmas(conn, limit):
    sql = """
          SELECT l.id,
                 l.lemma_iast,
                 lt.gloss
          FROM curriculum.lemma l
          JOIN curriculum.lemma_translation lt ON lt.lemma_id = l.id
          WHERE lt.language = 'ru'
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
                "gloss": row[2],
            }
            for row in cur.fetchall()
        ]


def call_llm(client, model, classes, lemmas, logger):
    payload = {
        "semantic_classes": classes,
        "lemmas": lemmas,
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
    logger.debug("lemma_count=%d", len(lemmas))
    logger.debug("class_count=%d", len(classes))

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
            max_tokens=128000,
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


def validate_results(data, requested_lemmas, allowed_class_ids, logger):
    if not isinstance(data, dict):
        logger.error("LLM response must be a JSON object, skipping batch")
        return {}

    results = data.get("results")

    if not isinstance(results, list):
        logger.error("LLM response must contain a 'results' array, skipping batch")
        return {}

    requested = {t["id"] for t in requested_lemmas}
    validated = {}

    for item in results:
        if not isinstance(item, dict):
            logger.warning("Invalid result item: %s", item)
            continue

        lemma_id = item.get("lemma_id")
        class_ids = item.get("semantic_class_ids")

        if lemma_id not in requested:
            logger.warning("Unexpected lemma_id returned by LLM: %s", lemma_id)
            continue

        if not isinstance(class_ids, list):
            logger.warning("semantic_class_ids must be a list for %s", lemma_id)
            continue

        for cid in class_ids:
            if cid not in allowed_class_ids:
                logger.warning("Unknown semantic_class_id %s for lemma %s", cid, lemma_id)

        if lemma_id in validated:
            validated[lemma_id].extend(cid for cid in class_ids if cid in allowed_class_ids and cid not in validated[lemma_id])
        else:
            validated[lemma_id] = [cid for cid in class_ids if cid in allowed_class_ids]

    missing = requested - set(validated)

    if missing:
        examples = sorted(missing)[:20]
        logger.warning(
            "LLM returned only %d of %d lemmas. Missing: %d. Examples: %s",
            len(validated), len(requested), len(missing), examples
        )

    return validated


def save_results(conn, results, logger):
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
            "Bind Sanskrit lemmas to semantic categories using an LLM"
        )
    )

    parser.add_argument(
        "--batch-size",
        type=int,
        default=DEFAULT_BATCH_SIZE,
        help=(
            "Number of lemmas per LLM request "
            f"(default: {DEFAULT_BATCH_SIZE})"
        ),
    )

    args = parser.parse_args()

    logger = setup_logging()

    logger.info("Starting lemma semantic-class binding")

    env = load_env(ENV_FILE_PATH)

    logger.info("Model: %s", LLM_MODEL)
    logger.info("Batch size: %d", args.batch_size)

    client = create_llm_client(env)

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
            lemmas = get_unprocessed_lemmas(
                conn,
                args.batch_size,
            )

            if not lemmas:
                logger.info("No more unprocessed lemmas.")
                break

            batch_number += 1

            logger.info(
                "START batch=%d lemmas=%d",
                batch_number,
                len(lemmas),
            )

            started = time.time()

            try:
                raw = call_llm(
                    client=client,
                    model=LLM_MODEL,
                    classes=classes,
                    lemmas=lemmas,
                    logger=logger,
                )

                results = validate_results(
                    raw,
                    lemmas,
                    allowed_class_ids,
                    logger,
                )

                logger.info(
                    "Validation successful: %d/%d",
                    len(results),
                    len(lemmas),
                )

                if results:
                    save_results(
                        conn=conn,
                        results=results,
                        logger=logger,
                    )

                    elapsed = time.time() - started

                    bound = sum(1 for v in results.values() if v)
                    logger.info(
                        "DONE batch=%d saved=%d lemmas_with_class=%d "
                        "elapsed=%.2fs",
                        batch_number,
                        len(results),
                        bound,
                        elapsed,
                    )
                else:
                    logger.info("SKIP batch=%d — no valid results", batch_number)

            except Exception:
                conn.rollback()
                logger.exception(
                    "BATCH %d FAILED — skipped, continuing to next batch",
                    batch_number,
                )
                continue

    logger.info("Finished successfully.")


if __name__ == "__main__":
    main()