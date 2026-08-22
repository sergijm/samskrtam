#!/usr/bin/env python3

import argparse
import json
import logging
import os
import re
import sys
import time
from datetime import datetime

import yaml

import psycopg
from openai import OpenAI


DEFAULT_BATCH_SIZE = 40
LOG_FILE = r"C:\MyDev\samskrtam\logs\declension_generator.log"
PROMPTS_LOG_FILE = r"C:\MyDev\samskrtam\logs\llm_prompts_responses.log"

# Configuration
#LLM_MODEL = "deepseek-v4-pro"
LLM_MODEL = "gemini-3.7-flash"
# LLM_MODEL = "claude-sonnet-5"
ENV_FILE_PATH = r"C:\MyDev\samskrtam\.env"
LLM_CONFIG_PATH = r"C:\MyDev\samskrtam\llm.yaml"

CASES = [
    "NOMINATIVE",
    "ACCUSATIVE",
    "INSTRUMENTAL",
    "DATIVE",
    "ABLATIVE",
    "GENITIVE",
    "LOCATIVE",
    "VOCATIVE",
]

NUMBERS = [
    "SINGULAR",
    "DUAL",
    "PLURAL",
]

CASE_LABELS_RU = {
    "NOMINATIVE": "Именительный",
    "ACCUSATIVE": "Винительный",
    "INSTRUMENTAL": "Творительный",
    "DATIVE": "Дательный",
    "ABLATIVE": "Отложительный",
    "GENITIVE": "Родительный",
    "LOCATIVE": "Местный",
    "VOCATIVE": "Звательный",
}

SYSTEM_PROMPT = """
You are an expert in Classical Sanskrit morphology and declension.

For every Sanskrit nominal lemma (noun) given in IAST, generate
all 24 forms (8 cases × 3 numbers) of its declension.

For each case+number combination, provide:
- form_iast: the form in IAST transliteration
- form_devanagari: the form in Devanagari script

ALSO determine the correct vowel_type for each lemma.

VOWEL TYPE REFERENCE:

For NOUNS (based on stem ending):
- A_STEM: stems ending in -a (e.g., deva, phala, rāma)
- AA_STEM: stems ending in -ā (e.g., senā, bhāryā, gaṅgā)
- I_STEM: stems ending in -i (e.g., kavi, giri, agni)
- II_STEM: stems ending in -ī (e.g., nadī, kṣiti, patnī)
- U_STEM: stems ending in -u (e.g., guru, śatru, mṛdu)
- UU_STEM: stems ending in -ū (e.g., vadhu, kāṅkṣū)
- R_STEM: stems ending in -ṛ (e.g., pitṛ, mātṛ, bhātrṛ)
- IN_STEM: stems ending in -in (e.g., yogin, mālin, bālin)
- AN_STEM: stems ending in -an (e.g., rājan, ātman, nāman, brahman)
- AS_STEM: stems ending in -as (e.g., manas, tapas, tejas)
- IS_STEM: stems ending in -is (e.g., havis, varcis, etc.)
- US_STEM: stems ending in -us (e.g., cakṣus)
- ANT_STEM: present participles ending in -ant/-at (e.g., bhavant, gacchant)
- VAT_STEM: stems ending in -vat/-mant (e.g., bhagavat, guṇavat)
- ROOT_STEM: root/consonant-final stems (e.g., vāc, marut, āp, dhi)
- O_STEM: stems ending in -o (e.g., go)
- AU_STEM: stems ending in -au (e.g., nau, glāu)

IMPORTANT RULES:
1. For every lemma, use its nominal declension pattern based on its stem ending.
2. For dual forms, always use the correct dual endings.
3. For vocative, use the correct vocative form (may differ from nominative).
4. If a particular form does not exist, still provide the most appropriate form or the nominative as fallback.

CONFIDENCE LEVELS:
- HIGH: You are absolutely certain about the vowel_type and all forms
- MIDDLE: You are reasonably confident but there might be some uncertainty
- LOW: You are unsure or the lemma is unusual/ambiguous

CRITICAL INSTRUCTION:
Return ONLY valid JSON. Do not include any explanatory text, comments, or markdown formatting.
Your entire response must be a single JSON object.

The JSON response MUST have exactly this structure:

{
  "results": [
    {
      "lemma_iast": "deva",
      "vowel_type": "A_STEM",
      "confidence": "HIGH",
      "forms": {
        "NOMINATIVE": {
          "SINGULAR": {"iast": "devaḥ", "devanagari": "देवः"},
          "DUAL": {"iast": "devau", "devanagari": "देवौ"},
          "PLURAL": {"iast": "devāḥ", "devanagari": "देवाः"}
        },
        "ACCUSATIVE": {
          "SINGULAR": {"iast": "devam", "devanagari": "देवम्"},
          "DUAL": {"iast": "devau", "devanagari": "देवौ"},
          "PLURAL": {"iast": "devān", "devanagari": "देवान्"}
        },
        ... (all 8 cases × 3 numbers)
      }
    }
  ]
}

For each lemma, you MUST provide:
- lemma_iast (the lemma in IAST)
- vowel_type (one of the types listed above)
- confidence (HIGH, MIDDLE, or LOW)
- ALL 24 forms (8 cases × 3 numbers)

Do not skip any case or number combination.
"""


def setup_logging():
    """Setup main logger for the script."""
    logger = logging.getLogger("declension-generator")
    logger.setLevel(logging.DEBUG)

    if logger.handlers:
        return logger

    formatter = logging.Formatter(
        "%(asctime)s | %(levelname)s | %(message)s"
    )

    # Ensure log directory exists
    log_dir = os.path.dirname(LOG_FILE)
    if log_dir and not os.path.exists(log_dir):
        os.makedirs(log_dir, exist_ok=True)

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


def setup_prompts_logger():
    """Setup separate logger for LLM prompts and responses."""
    prompts_logger = logging.getLogger("llm-prompts")
    prompts_logger.setLevel(logging.DEBUG)

    if prompts_logger.handlers:
        return prompts_logger

    # Ensure log directory exists
    log_dir = os.path.dirname(PROMPTS_LOG_FILE)
    if log_dir and not os.path.exists(log_dir):
        os.makedirs(log_dir, exist_ok=True)

    formatter = logging.Formatter(
        "%(asctime)s | %(message)s"
    )

    file_handler = logging.FileHandler(
        PROMPTS_LOG_FILE,
        encoding="utf-8",
    )
    file_handler.setLevel(logging.DEBUG)
    file_handler.setFormatter(formatter)

    prompts_logger.addHandler(file_handler)

    return prompts_logger


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

        return re.sub(r"\$\{([^}]+)\}", replace_var, value)

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


def get_unprocessed_lexemes(conn, limit):
    """
    Select noun lexemes that don't have declension forms yet.
    Selection is by lemma_iast absence in the declension_form table.

    Args:
        conn: Database connection
        limit: Maximum number of lexemes to return
    """

    sql = """
          WITH lexemes_to_process AS (
              SELECT DISTINCT l.id, l.lemma_iast, l.lemma_devanagari, l.gender
              FROM curriculum.lexeme l
              WHERE l.lemma_iast NOT IN (
                  SELECT DISTINCT df.lemma_iast
                  FROM curriculum.declension_form df
              )
                AND EXISTS (
                  SELECT 1
                  FROM curriculum.lexeme_pos lp
                  WHERE lp.lexeme_id = l.id
                    AND lp.pos_code = 'noun'
                )
                AND l.gender IS NOT NULL
                AND l.gender != 'UNSPECIFIED'
                AND l.lemma_iast not like '%%a'
              ORDER BY l.lemma_iast
              LIMIT %s
          )
          SELECT
              lp.id,
              lp.lemma_iast,
              lp.lemma_devanagari,
              lp.gender,
              (
                  SELECT array_agg(pos_code)
                  FROM curriculum.lexeme_pos
                  WHERE lexeme_id = lp.id
              ) AS pos_codes
          FROM lexemes_to_process lp
          ORDER BY lp.lemma_iast
          """

    with conn.cursor() as cur:
        cur.execute(sql, (limit,))
        rows = cur.fetchall()

        return [
            {
                "id": row[0],
                "lemma_iast": row[1],
                "lemma_devanagari": row[2],
                "gender": row[3],
                "pos_codes": row[4] if row[4] else [],
            }
            for row in rows
        ]


def log_llm_interaction(
        prompts_logger,
        batch_number,
        lemmas,
        messages,
        response_data,
        elapsed_time,
        success=True,
        error=None,
):
    """
    Log the complete LLM interaction (prompts and response) to a separate file.
    """
    timestamp = datetime.now().isoformat()

    log_entry = {
        "timestamp": timestamp,
        "batch_number": batch_number,
        "lemmas": lemmas,
        "lemma_count": len(lemmas),
        "messages": messages,
        "success": success,
        "elapsed_seconds": round(elapsed_time, 2),
    }

    if success:
        log_entry["response"] = response_data
    else:
        log_entry["error"] = str(error) if error else "Unknown error"

    # Write as JSON for easy parsing later
    prompts_logger.info(json.dumps(log_entry, ensure_ascii=False, indent=2))
    prompts_logger.info("=" * 100)


def extract_json_from_response(content):
    """
    Extract JSON from LLM response that might contain markdown or explanatory text.
    """
    if not content:
        return None

    # Try to find JSON in markdown code blocks
    json_pattern = r'```(?:json)?\s*(\{.*?\})\s*```'
    match = re.search(json_pattern, content, re.DOTALL)
    if match:
        return match.group(1)

    # Try to find JSON object directly
    json_pattern = r'(\{.*\})'
    match = re.search(json_pattern, content, re.DOTALL)
    if match:
        return match.group(1)

    return content


def call_llm(
        client,
        model,
        max_completion_tokens,
        lexemes,
        logger,
        prompts_logger,
        batch_number,
):
    """
    Send one batch to the LLM and return parsed JSON.
    """

    payload = {
        "lemmas": [l["lemma_iast"] for l in lexemes],
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

    # Log the request to the prompts logger
    lemma_list = [l["lemma_iast"] for l in lexemes]

    logger.debug("=" * 100)
    logger.debug("LLM REQUEST")
    logger.debug("model=%s", model)
    logger.debug(
        "max_completion_tokens=%s",
        max_completion_tokens,
    )
    logger.debug(
        "lemma_count=%d",
        len(lexemes),
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

        elapsed = time.time() - started
        success = True
        error = None

        # Log successful interaction
        try:
            raw_response = response.model_dump()
        except Exception:
            raw_response = str(response)

        log_llm_interaction(
            prompts_logger=prompts_logger,
            batch_number=batch_number,
            lemmas=lemma_list,
            messages=messages,
            response_data=raw_response,
            elapsed_time=elapsed,
            success=True,
        )

    except Exception as e:
        elapsed = time.time() - started
        success = False
        error = e

        # Log failed interaction
        log_llm_interaction(
            prompts_logger=prompts_logger,
            batch_number=batch_number,
            lemmas=lemma_list,
            messages=messages,
            response_data=None,
            elapsed_time=elapsed,
            success=False,
            error=e,
        )

        logger.exception(
            "LLM REQUEST FAILED after %.2fs",
            elapsed,
        )

        raise

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

    # Extract JSON from the response (handle markdown, etc.)
    json_content = extract_json_from_response(content)

    if not json_content:
        logger.error("Could not extract JSON from response")
        logger.error("Raw content:\n%s", content)
        raise RuntimeError("LLM response does not contain valid JSON")

    try:
        data = json.loads(json_content)

    except json.JSONDecodeError as e:
        logger.error(
            "JSON PARSE ERROR: %s",
            e,
        )

        logger.error(
            "INVALID JSON CONTENT:\n%s",
            json_content[:1000],  # Log first 1000 chars
        )

        # Log the invalid JSON
        log_llm_interaction(
            prompts_logger=prompts_logger,
            batch_number=batch_number,
            lemmas=lemma_list,
            messages=messages,
            response_data={"raw_content": json_content[:1000], "error": str(e)},
            elapsed_time=elapsed,
            success=False,
            error=f"JSON parse error: {e}",
        )

        raise RuntimeError(
            f"LLM returned invalid JSON: {e}"
        )

    # Check if data is a list and wrap it
    if isinstance(data, list):
        data = {"results": data}
    elif isinstance(data, dict) and "lemma_iast" in data and "forms" in data:
        # Single lemma response - wrap in results array
        data = {"results": [data]}

    logger.debug(
        "PARSED JSON:\n%s",
        json.dumps(
            data,
            ensure_ascii=False,
            indent=2,
        ),
    )

    # Log the parsed data for debugging
    logger.info(
        "LLM returned %d results for %d requested lemmas",
        len(data.get("results", [])),
        len(lexemes),
    )

    logger.debug("=" * 100)

    return data


def validate_results(data, requested_lemmas, strict=False):
    """
    Validate the LLM response.
    If strict=False, allow partial results and return what we have.
    """
    import logging

    if not isinstance(data, dict):
        raise RuntimeError(
            f"LLM response must be a JSON object, got {type(data)}"
        )

    # Get results array
    results_list = data.get("results")

    if not results_list or not isinstance(results_list, list):
        # Try to handle single lemma response
        if "lemma_iast" in data and "forms" in data:
            results_list = [data]
        else:
            raise RuntimeError(
                "LLM response must contain a 'results' array or a single lemma object"
            )

    results = {}
    low_confidence_lemmas = []
    duplicate_lemmas = []
    invalid_items = []
    missing_lemmas = []

    for idx, item in enumerate(results_list):
        if not isinstance(item, dict):
            invalid_items.append(f"Item {idx}: {item!r}")
            continue

        lemma = item.get("lemma_iast")
        forms = item.get("forms")
        vowel_type = item.get("vowel_type")
        confidence = item.get("confidence", "UNKNOWN")

        if not lemma:
            invalid_items.append(f"Item {idx}: missing 'lemma_iast'")
            continue

        if not forms:
            invalid_items.append(f"Item {idx} (lemma {lemma}): missing 'forms'")
            continue

        if not vowel_type:
            invalid_items.append(f"Item {idx} (lemma {lemma}): missing 'vowel_type'")
            continue

        if lemma not in requested_lemmas:
            invalid_items.append(f"Item {idx} (lemma {lemma}): unexpected lemma")
            continue

        # Check for duplicates
        if lemma in results:
            duplicate_lemmas.append(lemma)
            # Skip this duplicate - keep the first one
            continue

        if not isinstance(forms, dict):
            invalid_items.append(f"Item {idx} (lemma {lemma}): invalid forms")
            continue

        # Check all cases are present
        missing_cases = []
        for case in CASES:
            if case not in forms:
                missing_cases.append(case)
                continue

            case_data = forms[case]
            if not isinstance(case_data, dict):
                invalid_items.append(f"Item {idx} (lemma {lemma}): invalid case data for {case}")
                continue

            # Check all numbers are present
            missing_numbers = []
            for number in NUMBERS:
                if number not in case_data:
                    missing_numbers.append(number)
                    continue

                form_data = case_data[number]
                if not isinstance(form_data, dict):
                    invalid_items.append(f"Item {idx} (lemma {lemma}): invalid form data for {case}.{number}")
                    continue

                iast = form_data.get("iast")
                devanagari = form_data.get("devanagari")

                if not iast or not devanagari:
                    invalid_items.append(f"Item {idx} (lemma {lemma}): missing iast or devanagari for {case}.{number}")
                    continue

            if missing_numbers:
                invalid_items.append(
                    f"Item {idx} (lemma {lemma}): missing numbers {missing_numbers} for case {case}"
                )

        if missing_cases:
            invalid_items.append(
                f"Item {idx} (lemma {lemma}): missing cases {missing_cases}"
            )
            continue

        # Track low confidence
        if confidence == "LOW":
            low_confidence_lemmas.append(lemma)

        results[lemma] = {
            "forms": forms,
            "vowel_type": vowel_type,
            "confidence": confidence,
        }

    # Log warnings for invalid items
    logger = logging.getLogger("declension-generator")
    if invalid_items:
        logger.warning(
            "Found %d invalid items in LLM response:\n%s",
            len(invalid_items),
            "\n".join(invalid_items[:20])  # Show first 20
        )

    if duplicate_lemmas:
        logger.warning(
            "Found %d duplicate lemmas in LLM response: %s. Using first occurrence.",
            len(duplicate_lemmas),
            ", ".join(duplicate_lemmas[:10])
        )

    # Check which requested lemmas are missing
    missing = set(requested_lemmas) - set(results.keys())
    if missing:
        missing_lemmas = sorted(missing)
        logger.warning(
            "LLM returned only %d of %d requested lemmas. Missing %d lemmas.",
            len(results),
            len(requested_lemmas),
            len(missing)
        )
        logger.debug("Missing lemmas: %s", ", ".join(missing_lemmas[:20]))

    # If strict mode, raise error for missing lemmas
    if strict and missing:
        examples = missing_lemmas[:20]
        raise RuntimeError(
            f"LLM returned only {len(results)} of {len(requested_lemmas)} lemmas.\n"
            f"Missing: {len(missing)}.\n"
            f"Examples: {examples}\n"
            f"Invalid items: {len(invalid_items)}"
        )

    # Log low confidence warnings
    if low_confidence_lemmas:
        logger.warning(
            "LLM returned LOW confidence for %d lemmas: %s",
            len(low_confidence_lemmas),
            ", ".join(low_confidence_lemmas[:10]),
        )

    return results, missing_lemmas


def save_results(
        conn,
        results,
        lexeme_data,
        logger,
):
    """
    Save declension forms to the database including confidence.
    """

    sql = """
          INSERT INTO curriculum.declension_form
          (
              lemma_iast,
              vowel_type,
              case_type,
              number_type,
              form_iast,
              form_devanagari,
              confidence
          )
          VALUES (%s, %s, %s, %s, %s, %s, %s)
          ON CONFLICT (lemma_iast, vowel_type, case_type, number_type)
              DO UPDATE SET
                            form_iast = EXCLUDED.form_iast,
                            form_devanagari = EXCLUDED.form_devanagari,
                            confidence = EXCLUDED.confidence
          """

    total_rows = 0
    low_confidence_log = []

    with conn.cursor() as cur:
        for lemma, data in results.items():
            forms = data["forms"]
            vowel_type = data["vowel_type"]
            confidence = data["confidence"]

            # Log low confidence cases for manual review
            if confidence == "LOW":
                low_confidence_log.append({
                    "lemma": lemma,
                    "vowel_type": vowel_type,
                    "confidence": confidence,
                })

            logger.debug(
                "Lemma: %s, vowel_type: %s, confidence: %s",
                lemma,
                vowel_type,
                confidence,
            )

            for case in CASES:
                for number in NUMBERS:
                    form_data = forms[case][number]
                    cur.execute(
                        sql,
                        (
                            lemma,           # lemma_iast
                            vowel_type,      # vowel_type
                            case,            # case_type
                            number,          # number_type
                            form_data["iast"],      # form_iast
                            form_data["devanagari"], # form_devanagari
                            confidence,      # confidence
                        ),
                    )
                    total_rows += 1

    conn.commit()

    # Log low confidence summary
    if low_confidence_log:
        logger.warning(
            "LOW CONFIDENCE CASES (%d):\n%s",
            len(low_confidence_log),
            json.dumps(low_confidence_log, ensure_ascii=False, indent=2),
        )

    logger.debug(
        "Database commit successful: %d rows inserted/updated",
        total_rows,
    )

    return total_rows


def log_result_statistics(results, logger):

    total = len(results)
    high = sum(1 for d in results.values() if d["confidence"] == "HIGH")
    mid = sum(1 for d in results.values() if d["confidence"] == "MIDDLE")
    low = sum(1 for d in results.values() if d["confidence"] == "LOW")
    unknown = sum(1 for d in results.values() if d["confidence"] not in ["HIGH", "MIDDLE", "LOW"])

    logger.info("Declension generation statistics:")
    logger.info("  Lemmas processed: %d", total)
    logger.info("  Confidence: HIGH=%d, MIDDLE=%d, LOW=%d, UNKNOWN=%d",
                high, mid, low, unknown)

    total_forms = total * len(CASES) * len(NUMBERS)
    logger.info("  Total forms generated: %d", total_forms)


def main():

    parser = argparse.ArgumentParser(
        description=(
            "Generate Sanskrit declension forms for nouns "
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

    # Setup both loggers
    logger = setup_logging()
    prompts_logger = setup_prompts_logger()

    logger.info(
        "Starting declension form generation"
    )

    logger.info(
        "Main log file: %s",
        LOG_FILE,
    )

    logger.info(
        "LLM prompts log file: %s",
        PROMPTS_LOG_FILE,
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

            lexemes = get_unprocessed_lexemes(
                conn,
                args.batch_size,
            )

            if not lexemes:

                logger.info(
                    "No more unprocessed nouns."
                )

                break

            batch_number += 1

            # Build mapping from lemma_iast to lexeme data
            lexeme_data = {
                lex["lemma_iast"]: lex
                for lex in lexemes
            }

            lemma_list = [lex["lemma_iast"] for lex in lexemes]

            logger.info(
                "START batch=%d lemmas=%d",
                batch_number,
                len(lemma_list),
            )

            logger.debug(
                "BATCH LEMMAS:\n%s",
                json.dumps(
                    lemma_list,
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
                    lexemes=lexemes,
                    logger=logger,
                    prompts_logger=prompts_logger,
                    batch_number=batch_number,
                )

                # Validate with strict=False to allow partial results
                results, missing_lemmas = validate_results(
                    raw,
                    lemma_list,
                    strict=False,  # Allow partial results
                )

                logger.info(
                    "Validation successful: %d/%d lemmas (missing: %d)",
                    len(results),
                    len(lemma_list),
                    len(missing_lemmas),
                )

                # Save even if we have partial results
                if results:
                    total_rows = save_results(
                        conn=conn,
                        results=results,
                        lexeme_data=lexeme_data,
                        logger=logger,
                    )

                    elapsed = time.time() - started

                    logger.info(
                        "DONE batch=%d lemmas=%d (missing=%d) forms=%d elapsed=%.2fs",
                        batch_number,
                        len(results),
                        len(missing_lemmas),
                        total_rows,
                        elapsed,
                    )

                    log_result_statistics(
                        results,
                        logger,
                    )

                    # Log missing lemmas separately
                    if missing_lemmas:
                        logger.warning(
                            "Missing lemmas in batch %d: %s",
                            batch_number,
                            ", ".join(missing_lemmas[:20])
                        )
                else:
                    # No valid results at all
                    elapsed = time.time() - started
                    logger.error(
                        "BATCH %d: No valid results to save. Elapsed: %.2fs",
                        batch_number,
                        elapsed
                    )
                    # Rollback since nothing was saved
                    conn.rollback()

            except Exception as e:
                conn.rollback()

                logger.exception(
                    "BATCH %d FAILED — "
                    "database transaction rolled back",
                    batch_number,
                )

                # Don't exit on failure, try next batch
                logger.info("Continuing with next batch...")
                continue

    logger.info("Finished successfully.")


if __name__ == "__main__":
    main()