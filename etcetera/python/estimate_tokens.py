import os
import argparse
from pathlib import Path

# ========== КОНСТАНТЫ ==========
DEFAULT_DIR = "C:\MyDev\samskrtam"  # Директория по умолчанию (текущая)
EXTENSIONS = ('.java', '.md', '.tsx', '*.yaml')  # Расширения файлов для анализа
EXCLUDED_DIRS = {
    'node_modules', '.git', 'build', 'dist', 'target', '__pycache__',
    '.idea', '.vscode', 'venv', 'env', '.venv', 'logs', 'tmp', 'temp',
    '.gradle', '.kotlin', 'gradle', 'etcetera'
}
# ========== ФУНКЦИИ ==========
def estimate_tokens(text: str) -> int:
    """Грубая оценка количества токенов (1 токен ≈ 4 символа)."""
    return len(text) // 4

def count_files_and_tokens(root_dir: str) -> None:
    """
    Рекурсивно обходит root_dir, считает файлы с расширениями из EXTENSIONS,
    оценивает количество токенов в каждом файле и выводит статистику.
    Папки из EXCLUDED_DIRS пропускаются.
    """
    total_files = 0
    total_tokens = 0
    max_tokens_per_file = 0
    max_file_path = ""

    print(f"Анализ директории: {os.path.abspath(root_dir)}")
    print(f"Расширения: {', '.join(EXTENSIONS)}")
    print(f"Исключены: {', '.join(sorted(EXCLUDED_DIRS))}\n")
    print("-" * 70)

    for path in Path(root_dir).rglob('*'):
        # Пропускаем исключенные папки
        if any(excluded in path.parts for excluded in EXCLUDED_DIRS):
            continue

        if path.suffix.lower() in EXTENSIONS and path.is_file():
            try:
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
            except (UnicodeDecodeError, PermissionError, OSError):
                continue

            tokens = estimate_tokens(content)
            total_files += 1
            total_tokens += tokens
            if tokens > max_tokens_per_file:
                max_tokens_per_file = tokens
                max_file_path = path

            print(f"{path.relative_to(root_dir)}: ~{tokens} токенов")

    print("\n" + "-" * 70)
    print("📊 ОБЩАЯ СТАТИСТИКА")
    print(f"  Всего файлов: {total_files}")
    print(f"  Примерно токенов: {total_tokens:,}")
    print(f"  Среднее токенов на файл: {total_tokens // total_files if total_files else 0:,}")
    if max_file_path:
        print(f"  Максимальный файл: {max_file_path.relative_to(root_dir)} (~{max_tokens_per_file} токенов)")

# ========== ТОЧКА ВХОДА ==========
if __name__ == "__main__":
    parser = argparse.ArgumentParser(
        description="Оценить количество токенов в файлах с заданными расширениями."
    )
    parser.add_argument(
        "dir", nargs="?", default=DEFAULT_DIR,
        help=f"Путь к директории для анализа (по умолчанию: {DEFAULT_DIR})"
    )
    args = parser.parse_args()

    count_files_and_tokens(args.dir)