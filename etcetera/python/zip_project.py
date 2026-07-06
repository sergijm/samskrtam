import os
import zipfile
from pathlib import Path

# ========== КОНСТАНТЫ ==========
DEFAULT_DIR = r"C:\MyDev\samskrtam"
DEST_FILE = r"C:\MyDev\samskrtam.zip"
EXTENSIONS = ('.java', '.ts', '.tsx', '.yaml', '.yml', '.md', '.sql', '.json')
EXCLUDED_DIRS = {
    'node_modules', '.git', 'build', 'dist', 'target', '__pycache__',
    '.idea', '.vscode', 'venv', 'env', '.venv', 'logs', 'tmp', 'temp',
    '.gradle', '.kotlin', 'gradle', 'etcetera'
}

def estimate_tokens(text: str) -> int:
    """Грубая оценка: 1 токен ≈ 4 символа."""
    return len(text) // 4

def estimate_project_size(root_dir: str) -> tuple:
    """
    Оценивает общий размер и количество токенов для всех файлов,
    которые попадут в архив (без упаковки).
    """
    root_path = Path(root_dir).resolve()
    total_size = 0
    total_tokens = 0
    file_count = 0

    for file_path in root_path.rglob('*'):
        if not file_path.is_file():
            continue
        if any(excluded in file_path.parts for excluded in EXCLUDED_DIRS):
            continue
        if file_path.suffix.lower() in EXTENSIONS:
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    content = f.read()
                total_size += len(content.encode('utf-8'))
                total_tokens += estimate_tokens(content)
                file_count += 1
            except (UnicodeDecodeError, PermissionError, OSError):
                continue

    return file_count, total_size, total_tokens

def zip_project(output_zip: str, root_dir: str = None) -> None:
    if root_dir is None:
        root_dir = DEFAULT_DIR

    root_path = Path(root_dir).resolve()
    output_path = Path(output_zip).resolve()

    # Проверяем, существует ли директория
    if not root_path.exists():
        print(f"❌ Ошибка: Директория '{root_path}' не найдена!")
        return

    # Сначала оцениваем размер
    file_count, total_size, total_tokens = estimate_project_size(root_dir)
    size_mb = total_size / (1024 * 1024)

    print("📊 ПРЕДВАРИТЕЛЬНАЯ ОЦЕНКА")
    print("-" * 70)
    print(f"  Директория: {root_path}")
    print(f"  Расширения: {', '.join(EXTENSIONS)}")
    print(f"  Исключено: {', '.join(sorted(EXCLUDED_DIRS))}")
    print()
    print(f"  Файлов: {file_count}")
    print(f"  Размер (текст): {size_mb:.2f} MB")
    print(f"  Примерно токенов: {total_tokens:,}")
    if size_mb > 0:
        print(f"  Токенов / МБ: {total_tokens / size_mb:.0f}")
    print()

    # Проверяем, помещается ли в контекстное окно (для разных моделей)
    print("📌 РЕКОМЕНДАЦИИ ПО МОДЕЛЯМ")
    print("-" * 70)
    models = {
        "Gemini 1.5/2.5 Pro (1M)": 1_000_000,
        "Claude 3.7 Sonnet (200K)": 200_000,
        "GPT-4 Turbo (128K)": 128_000,
        "DeepSeek-V3 (128K)": 128_000,
        "Qwen2.5-Coder 32B (128K)": 128_000,
        "Llama 3.1 70B (128K)": 128_000,
    }
    for model_name, max_tokens in models.items():
        fits = total_tokens <= max_tokens
        icon = "✅" if fits else "❌"
        print(f"  {icon} {model_name}: {max_tokens:,} токенов {'(влезает)' if fits else '(НЕ влезает)'}")

    print("\n" + "-" * 70)

    # Создаем архив
    created_count = 0
    with zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED) as zipf:
        for file_path in root_path.rglob('*'):
            if not file_path.is_file():
                continue
            if any(excluded in file_path.parts for excluded in EXCLUDED_DIRS):
                continue
            if file_path.resolve() == output_path:
                continue
            if file_path.suffix.lower() in EXTENSIONS:
                arcname = file_path.relative_to(root_path)
                zipf.write(file_path, arcname)
                created_count += 1
                print(f"  Добавлен: {arcname}")

    print("\n" + "-" * 70)
    print(f"✅ Архив создан: {output_path}")
    print(f"📦 Файлов добавлено: {created_count}")
    if output_path.exists():
        print(f"📏 Размер ZIP: {output_path.stat().st_size / (1024 * 1024):.2f} MB")
    print(f"📝 Примерно токенов: {total_tokens:,}")

if __name__ == "__main__":
    import argparse
    parser = argparse.ArgumentParser(
        description="Упаковывает файлы в ZIP с сохранением структуры."
    )
    parser.add_argument(
        "output", nargs="?", default=DEST_FILE,
        help=f"Имя выходного ZIP-файла (по умолчанию: {DEST_FILE})"
    )
    parser.add_argument(
        "dir", nargs="?", default=None,
        help=f"Корневая директория проекта (по умолчанию: {DEFAULT_DIR})"
    )
    args = parser.parse_args()

    zip_project(args.output, args.dir)