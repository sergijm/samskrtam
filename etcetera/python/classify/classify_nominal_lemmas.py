#!/usr/bin/env python3

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


DEFAULT_BATCH_SIZE = 2000
LOG_FILE = "nominal_lemmas_llm.log"

# Configuration
LLM_MODEL = "deepseek-v4-pro"
ENV_FILE_PATH = "C:\MyDev\samskrtam\.env"
LLM_CONFIG_PATH = "C:\MyDev\samskrtam\llm.yaml"

STEM_CLASSES = {
    "A_STEM",
    "AA_STEM",
    "I_STEM",
    "II_STEM",
    "U_STEM",
    "UU_STEM",
    "R_STEM",
}

CONFIDENCES = {
    "HIGH",
    "MEDIUM",
    "LOW",
}


SYSTEM_PROMPT = """
You are an expert in Classical Sanskrit morphology.

For every Sanskrit nominal lemma given in IAST, determine its
underlying nominal stem and the vowel class of that stem.

Return exactly one result for every input lemma.

Allowed stem_class values:

A_STEM  = stem ending in -a
AA_STEM = stem ending in -ā
I_STEM  = stem ending in -i
II_STEM = stem ending in -ī
U_STEM  = stem ending in -u
UU_STEM = stem ending in -ū
R_STEM  = stem ending in -ṛ

The field stem_iast must contain the full underlying Sanskrit stem
in IAST, including its final vowel.

Examples:

rāma  -> rāma  -> A_STEM
nadī  -> nadī  -> II_STEM
phala -> phala -> A_STEM
guru  -> guru  -> U_STEM
vadhū -> vadhū -> UU_STEM

Do not simply infer the stem from the nominative singular form.
Use your knowledge of Sanskrit nominal morphology.

If the lemma is ambiguous, irregular, or you are not sufficiently
certain, give your best answer and set confidence to LOW.

Return JSON only.

The JSON response MUST have exactly this structure:

{
  "results": [
    {
      "lemma_iast": "deva",
      "stem_iast": "deva",
      "stem_class": "A_STEM",
      "confidence": "HIGH"
    }
  ]
}

The field name MUST be "lemma_iast", not "lemma".
Do not rename, omit, or abbreviate any field.
"""


def setup_logging():
    logger = logging.getLogger("nominal-lemmas")
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
    """
    Minimal .env loader.
    No python-dotenv dependency required.
    """

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
        raise RuntimeError(
            f"Missing required environment variable: {name}"
        )

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
        base_url=required(
            llm_config,
            "base-url",
        ),
        api_key=required(
            llm_config,
            "api-key",
        ),
    )


def get_unprocessed_lemmas(conn, limit):
    """
    Select the most frequent NOUN lemmas which are not yet
    present in nominal_lemmas.
    """

    sql = """
          SELECT
              vw.lemma_iast,
              COUNT(*) AS frequency
          FROM sangraha.verse_words vw
                   LEFT JOIN sangraha.nominal_lemmas nl
                             ON nl.lemma_iast = vw.lemma_iast
          WHERE vw.pos = 'NOUN'
            AND vw.lemma_iast IS NOT NULL
            AND nl.id IS NULL
          GROUP BY vw.lemma_iast
          ORDER BY COUNT(*) DESC, vw.lemma_iast
          LIMIT %s \
          """

    with conn.cursor() as cur:
        cur.execute(sql, (limit,))

        return [
            row[0]
            for row in cur.fetchall()
        ]


def call_llm(
        client,
        model,
        max_completion_tokens,
        lemmas,
        logger,
):
    """
    Send one batch to the LLM and return parsed JSON.

    Logs:
      - request parameters
      - complete request messages
      - complete raw SDK response
      - token usage
      - raw content
      - parsed JSON
    """

    payload = {
        "lemmas": lemmas
    }

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
        "lemma_count=%d",
        len(lemmas),
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

    # Full SDK response
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

    # Token usage
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
        raise RuntimeError(
            "LLM returned no choices"
        )

    content = response.choices[0].message.content

    logger.debug(
        "RAW CONTENT:\n%s",
        content or "<EMPTY>",
        )

    if not content:
        raise RuntimeError(
            "LLM returned empty response"
        )

    try:
        data = json.loads(content)

    except json.JSONDecodeError as e:

        logger.error(
            "JSON PARSE ERROR: %s",
            e,
        )

        logger.error(
            "INVALID JSON CONTENT:\n%s",
            content,
        )

        raise RuntimeError(
            f"LLM returned invalid JSON: {e}"
        )

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


def validate_results(data, requested_lemmas):
    """
    Strictly validate the LLM response before touching DB.
    """

    if not isinstance(data, dict):
        raise RuntimeError(
            "LLM response must be a JSON object"
        )

    results = data.get("results")

    if not isinstance(results, list):
        raise RuntimeError(
            "LLM response must contain a 'results' array"
        )

    requested = set(requested_lemmas)
    validated = {}

    for item in results:

        if not isinstance(item, dict):
            raise RuntimeError(
                f"Invalid result item: {item!r}"
            )

        lemma = item.get("lemma_iast")
        stem = item.get("stem_iast")
        stem_class = item.get("stem_class")
        confidence = item.get("confidence")

        if lemma not in requested:
            raise RuntimeError(
                f"Unexpected lemma returned by LLM: "
                f"{lemma!r}"
            )

        if lemma in validated:
            raise RuntimeError(
                f"Duplicate result for lemma: {lemma}"
            )

        if not stem:
            raise RuntimeError(
                f"Empty stem for lemma: {lemma}"
            )

        if stem_class not in STEM_CLASSES:
            raise RuntimeError(
                f"Invalid stem_class for {lemma}: "
                f"{stem_class!r}"
            )

        if confidence not in CONFIDENCES:
            raise RuntimeError(
                f"Invalid confidence for {lemma}: "
                f"{confidence!r}"
            )

        validated[lemma] = {
            "stem_iast": stem,
            "stem_class": stem_class,
            "confidence": confidence,
        }

    missing = requested - set(validated)

    if missing:
        examples = sorted(missing)[:20]

        raise RuntimeError(
            f"LLM returned only "
            f"{len(validated)} of "
            f"{len(requested)} lemmas.\n"
            f"Missing: {len(missing)}.\n"
            f"Examples: {examples}"
        )

    return validated


def save_results(
        conn,
        results,
        model,
        logger,
):
    """
    Save one row per lemma.

    nominal_lemmas.lemma_iast is UNIQUE, making this operation
    safe to repeat.
    """

    sql = """
          INSERT INTO sangraha.nominal_lemmas
          (
              lemma_iast,
              stem_iast,
              stem_class,
              confidence,
              model
          )
          VALUES (%s, %s, %s, %s, %s)
          ON CONFLICT (lemma_iast)
              DO UPDATE SET
                            stem_iast = EXCLUDED.stem_iast,
                            stem_class = EXCLUDED.stem_class,
                            confidence = EXCLUDED.confidence,
                            model = EXCLUDED.model,
                            updated_at = now() \
          """

    with conn.cursor() as cur:

        for lemma, result in results.items():

            cur.execute(
                sql,
                (
                    lemma,
                    result["stem_iast"],
                    result["stem_class"],
                    result["confidence"],
                    model,
                ),
            )

    conn.commit()

    logger.debug(
        "Database commit successful: %d rows",
        len(results),
    )


def log_result_statistics(results, logger):

    counts = {}

    for result in results.values():

        key = (
            result["stem_class"],
            result["confidence"],
        )

        counts[key] = counts.get(key, 0) + 1

    logger.info("Classification statistics:")

    for (stem_class, confidence), count in sorted(
            counts.items()
    ):
        logger.info(
            "  %-8s %-6s %d",
            stem_class,
            confidence,
            count,
        )


def main():

    parser = argparse.ArgumentParser(
        description=(
            "Classify Sanskrit noun lemmas "
            "using an LLM"
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

    logger.info(
        "Starting nominal lemma classification"
    )

    logger.info(
        "Log file: %s",
        LOG_FILE,
    )

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

    logger.info(
        "Model: %s",
        model,
    )

    logger.info(
        "Batch size: %d",
        args.batch_size,
    )

    logger.info(
        "Max completion tokens: %d",
        max_completion_tokens,
    )

    client = create_llm_client(llm_config)

    with create_db_connection(env) as conn:

        batch_number = 0

        while True:

            lemmas = get_unprocessed_lemmas(
                conn,
                args.batch_size,
            )

            if not lemmas:

                logger.info(
                    "No more unprocessed noun lemmas."
                )

                break

            batch_number += 1

            logger.info(
                "START batch=%d lemmas=%d",
                batch_number,
                len(lemmas),
            )

            logger.debug(
                "BATCH LEMMAS:\n%s",
                json.dumps(
                    lemmas,
                    ensure_ascii=False,
                    indent=2,
                ),
            )

            started = time.time()

            try:

                raw = call_llm(
                    client=client,
                    model=model,
                    max_completion_tokens=(
                        max_completion_tokens
                    ),
                    lemmas=lemmas,
                    logger=logger,
                )

                results = validate_results(
                    raw,
                    lemmas,
                )

                logger.info(
                    "Validation successful: %d/%d",
                    len(results),
                    len(lemmas),
                )

                save_results(
                    conn=conn,
                    results=results,
                    model=model,
                    logger=logger,
                )

                elapsed = time.time() - started

                logger.info(
                    "DONE batch=%d saved=%d elapsed=%.2fs",
                    batch_number,
                    len(results),
                    elapsed,
                )

                log_result_statistics(
                    results,
                    logger,
                )

            except Exception:

                conn.rollback()

                logger.exception(
                    "BATCH %d FAILED — "
                    "database transaction rolled back",
                    batch_number,
                )

                sys.exit(1)

    logger.info("Finished successfully.")


if __name__ == "__main__":
    main()