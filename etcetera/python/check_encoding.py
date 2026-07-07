import os
import chardet
from pathlib import Path

# ========== КОНСТАНТЫ ==========
DEFAULT_DIR = r"C:\MyDev\samskrtam"
EXTENSIONS = ('.java', '.ts', '.tsx', '.yaml', '.yml')

def detect_encoding(file_path: str) -> str:
    """
    Определяет кодировку файла с помощью chardet.
    Возвращает название кодировки или 'unknown' в случае ошибки.
    """
    try:
        with open(file_path, 'rb') as f:
            raw_data = f.read(10000)  # Читаем первые 10KB для определения кодировки
            result = chardet.detect(raw_data)
            return result.get('encoding', 'unknown')
    except Exception as e:
        return f"error: {str(e)}"

def scan_directory(root_dir: str) -> None:
    """
    Рекурсивно обходит директорию и определяет кодировку файлов с заданными расширениями.
    """
    root_path = Path(root_dir).resolve()

    if not root_path.exists():
        print(f"❌ Ошибка: Директория '{root_path}' не найдена!")
        return

    print(f"🔍 Сканирование директории: {root_path}")
    print(f"📁 Расширения: {', '.join(EXTENSIONS)}")
    print("-" * 70)

    total_files = 0
    total_errors = 0
    encoding_stats = {}

    for file_path in root_path.rglob('*'):
        # Пропускаем директории
        if not file_path.is_file():
            continue

        # Проверяем расширение
        if file_path.suffix.lower() in EXTENSIONS:
            encoding = detect_encoding(str(file_path))
            total_files += 1

            # Статистика по кодировкам
            if encoding not in encoding_stats:
                encoding_stats[encoding] = 0
            encoding_stats[encoding] += 1

            # Выводим информацию о файле
            rel_path = file_path.relative_to(root_path)
            status = "✅" if encoding != 'unknown' else "⚠️"
            print(f"{status} {rel_path}: {encoding}")

    # Выводим итоговую статистику
    print("\n" + "-" * 70)
    print("📊 СТАТИСТИКА")
    print(f"  Всего файлов просканировано: {total_files}")
    print(f"  Ошибок определения кодировки: {total_errors}")
    print("  Распределение по кодировкам:")
    for enc, count in sorted(encoding_stats.items(), key=lambda x: x[1], reverse=True):
        print(f"    {enc}: {count} файлов")
    print("-" * 70)

# ========== ТОЧКА ВХОДА ==========
if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Определяет кодировку файлов с заданными расширениями."
    )
    parser.add_argument(
        "dir", nargs="?", default=DEFAULT_DIR,
        help=f"Путь к директории для сканирования (по умолчанию: {DEFAULT_DIR})"
    )
    args = parser.parse_args()

    scan_directory(args.dir)