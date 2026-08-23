# ----------------------------------------------------------------------------
# Configuration
# ----------------------------------------------------------------------------

LOG_FILE = r"C:\MyDev\logs\<your_file>.log"
ENV_FILE_PATH = r"C:\MyDev\samskrtam\.env"
<FILE_TO_PARSE>_PATH = r"<your_path>"

# Commit every N parsed entries during pass 1 (progress + safety checkpoint).
BATCH_SIZE = 500

# ----------------------------------------------------------------------------
# .env handling
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

def get_llm_api_key(env: dict) -> str:
    """Get LLM API key from environment variables."""
    api_key = env.get("LLM_API_KEY")
    if not api_key:
        log.error("LLM_API_KEY not found in .env file")
        raise ValueError("LLM_API_KEY not found in .env file")
    return api_key

def required(env: dict, key: str) -> str:
    value = env.get(key)
    if not value:
        log.error("Missing required env variable: %s", key)
        raise ValueError(f"Missing required env variable: {key}")
    return value

# Использование в main:
def main():
    log.info("=== import_frisch.py started ===")
    try:
        env = read_env(ENV_FILE_PATH)
        conn = create_db_connection(env)

        # Получение API ключа из .env
        api_key = get_llm_api_key(env)
        log.info("LLM_API_KEY loaded successfully")

        log.info("Connected to database %s@%s", env.get("DB_NAME"), env.get("DB_HOST"))
    except Exception:
        log.exception("Failed to initialize DB connection")
        sys.exit(1)
    # ... остальной код