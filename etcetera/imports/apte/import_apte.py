"""
Entry point for the Apte dictionary import pipeline.

Usage:
    python import_apte.py --pass structure   # Pass 1 only
    python import_apte.py --pass grammar      # Pass 2 only (requires pass 1 done)
    python import_apte.py --pass all          # both, in order

Prerequisites:
    1. apte_dictionary_schema.sql applied to the target database
    2. migration_unmatched_tokens.sql applied
    3. .env at ENV_FILE_PATH (see config.py) with:
         DB_HOST=...
         DB_PORT=5432
         DB_NAME=...
         DB_USER=...
         DB_PASSWORD=...
"""

import argparse
import sys

import psycopg2

from config import log, ENV_FILE_PATH, AP_TXT_PATH, read_env, get_db_dsn
from parse_structure import run_pass1
from parse_grammar import run_pass2


def create_db_connection(env: dict):
    dsn = get_db_dsn(env)
    conn = psycopg2.connect(dsn)
    return conn


def main():
    parser = argparse.ArgumentParser(description="Import Apte dictionary into PostgreSQL")
    parser.add_argument(
        "--pass", dest="stage", choices=["structure", "grammar", "all"],
        default="all", help="Which pass to run",
    )
    args = parser.parse_args()

    log.info("=== import_apte.py started (pass=%s) ===", args.stage)
    try:
        env = read_env(ENV_FILE_PATH)
        conn = create_db_connection(env)
        log.info("Connected to database %s@%s", env.get("DB_NAME"), env.get("DB_HOST"))
    except Exception:
        log.exception("Failed to initialize DB connection")
        sys.exit(1)

    try:
        if args.stage in ("structure", "all"):
            log.info("--- Pass 1: structure ---")
            run_pass1(conn, AP_TXT_PATH)

        if args.stage in ("grammar", "all"):
            log.info("--- Pass 2: grammar ---")
            run_pass2(conn)

    except Exception:
        log.exception("Import failed")
        conn.rollback()
        sys.exit(1)
    finally:
        conn.close()

    log.info("=== import_apte.py finished ===")


if __name__ == "__main__":
    main()
