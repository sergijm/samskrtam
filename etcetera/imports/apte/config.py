"""
Configuration for the Apte (ap.txt) import pipeline.

Follows the same .env-loading pattern as config_example.py:
secrets/connection info live in .env, everything else is a plain
module-level constant here.
"""

import logging
import sys
from pathlib import Path

# ----------------------------------------------------------------------------
# Configuration
# ----------------------------------------------------------------------------

LOG_FILE = r"C:\MyDev\logs\import_apte.log"
ENV_FILE_PATH = r"C:\MyDev\samskrtam\.env"

# Source text file, i.e. csl-orig/v02/ap/ap.txt checked out locally.
AP_TXT_PATH = r"C:\MyDev\samskrtam\data\ap\ap.txt"

DICTIONARY_CODE = "ap"

# Commit every N parsed entries during pass 1 / pass 2 (progress + safety checkpoint).
BATCH_SIZE = 500

# ----------------------------------------------------------------------------
# Logging
# ----------------------------------------------------------------------------

log = logging.getLogger("import_apte")
log.setLevel(logging.INFO)

_formatter = logging.Formatter(
    "%(asctime)s [%(levelname)s] %(message)s", datefmt="%Y-%m-%d %H:%M:%S"
)

_file_handler = logging.FileHandler(LOG_FILE, encoding="utf-8")
_file_handler.setFormatter(_formatter)
log.addHandler(_file_handler)

_console_handler = logging.StreamHandler(sys.stdout)
_console_handler.setFormatter(_formatter)
log.addHandler(_console_handler)


# ----------------------------------------------------------------------------
# .env handling  (identical contract to config_example.py)
# ----------------------------------------------------------------------------

def read_env(path: str) -> dict:
    """Minimal .env parser: KEY=VALUE per line, '#' comments, blank lines ok."""
    env = {}
    env_path = Path(path)
    if not env_path.exists():
        log.error("ENV file not found: %s", path)
        raise FileNotFoundError(f"ENV file not found: {path}")

    with env_path.open(encoding="utf-8") as f:
        for line_no, raw_line in enumerate(f, start=1):
            line = raw_line.strip()
            if not line or line.startswith("#"):
                continue
            if "=" not in line:
                log.warning(".env line %d ignored (no '='): %r", line_no, raw_line)
                continue
            key, _, value = line.partition("=")
            key = key.strip()
            value = value.strip().strip('"').strip("'")
            env[key] = value

    log.info("Loaded %d variables from %s", len(env), path)
    return env


def required(env: dict, key: str) -> str:
    value = env.get(key)
    if not value:
        log.error("Missing required env variable: %s", key)
        raise ValueError(f"Missing required env variable: {key}")
    return value


def get_db_dsn(env: dict) -> str:
    """
    Build a psycopg2 DSN from .env values:
      DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
    """
    host = required(env, "DB_HOST")
    port = env.get("DB_PORT", "5432")
    name = required(env, "DB_NAME")
    user = required(env, "DB_USER")
    password = required(env, "DB_PASSWORD")
    return f"host={host} port={port} dbname={name} user={user} password={password}"
