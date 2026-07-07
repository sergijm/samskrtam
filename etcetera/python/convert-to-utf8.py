import os
import chardet
from pathlib import Path

# ========== КОНСТАНТЫ ==========
DEFAULT_DIR = r"C:\MyDev\samskrtam"
EXTENSIONS = ('.java', '.ts', '.tsx', '.yaml', '.yml')

def detect_encoding(file_path: str) -> str | None:
    """
    Определяет кодировку файла с помощью chardet.
    Возвращает название кодировки или None в случае ошибки.
    """
    try:
        with open(file_path, 'rb') as f:
            raw_data = f.read(10000)
            result = chardet.detect(raw_data)
            return result.get('encoding')
    except Exception:
        return None

def convert_file_to_utf8(file_path: str) -> bool:
    """
    Конвертирует файл в UTF-8 с BOM (если нужно).
    Возвращает True если файл был конвертирован, False если уже UTF-8 или ошибка.
    """

    try:
        # Определяем текущую кодировку
        encoding = detect_encoding(file_path)

        # Пропускаем файлы, которые уже в UTF-8
        if encoding and encoding.lower() in ('utf-8', 'ascii', 'utf-8-sig'):
            return False

        # Читаем файл в байтах
        with open(file_path, 'rb') as f:
            content_bytes = f.read()

        # Если кодировка не определена, пытаемся прочитать как UTF-8
        if encoding is None:
            try:
                content_text = content_bytes.decode('utf-8')
                return False  # Файл уже читается как UTF-8
            except UnicodeDecodeError:
                # Если не получается, используем системную кодировку
                encoding = 'windows-1251'  # Или другую по умолчанию

        # Декодируем содержимое в текст
        content_text = content_bytes.decode(encoding, errors='replace')

        # Сохраняем как UTF-8 с BOM (для совместимости с Windows)
        with open(file_path, 'w', encoding='utf-8-sig') as f:
            f.write(content_text)

        return True

    except Exception as e:
        print(f"❌ Ошибка при конвертации {file_path}: {str(e)}")
        return False

def scan_and_convert(root_dir: str) -> None:
    """
    Рекурсивно обходит директорию и конвертирует все файлы с заданными расширениями в UTF-8.
    """
    root_path = Path(root_dir).resolve()

    if not root_path.exists():
        print(f"❌ Ошибка: Директория '{root_path}' не найдена!")
        return

    print(f"🔍 Сканирование и конвертация: {root_path}")
    print(f"📁 Расширения: {', '.join(EXTENSIONS)}")
    print("-" * 70)

    total_files = 0
    converted_files = 0
    already_utf8 = 0
    errors = 0
    encoding_stats = {}

    for file_path in root_path.rglob('*'):
        if not file_path.is_file():
            continue

        if file_path.suffix.lower() in EXTENSIONS:
            total_files += 1
            rel_path = file_path.relative_to(root_path)

            # Проверяем текущую кодировку
            encoding = detect_encoding(str(file_path))
            if encoding:
                encoding_stats[encoding] = encoding_stats.get(encoding, 0) + 1

            # Конвертируем файл
            if convert_file_to_utf8(str(file_path)):
                converted_files += 1
                print(f"✅ {rel_path}: {encoding} → UTF-8")
            else:
                already_utf8 += 1
                if encoding:
                    print(f"⏭️ {rel_path}: уже {encoding}")
                else:
                    print(f"⏭️ {rel_path}: уже UTF-8")

    # Выводим итоговую статистику
    print("\n" + "-" * 70)
    print("📊 СТАТИСТИКА")
    print(f"  Всего файлов обработано: {total_files}")
    print(f"  ✅ Конвертировано в UTF-8: {converted_files}")
    print(f"  ⏭️ Уже в UTF-8: {already_utf8}")
    print(f"  ❌ Ошибок: {errors}")
    print("  Распределение по исходным кодировкам:")
    for enc, count in sorted(encoding_stats.items(), key=lambda x: x[1], reverse=True):
        print(f"    {enc}: {count} файлов")
    print("-" * 70)

# ========== ТОЧКА ВХОДА ==========
if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Конвертирует файлы с заданными расширениями в UTF-8."
    )
    parser.add_argument(
        "dir", nargs="?", default=DEFAULT_DIR,
        help=f"Путь к директории (по умолчанию: {DEFAULT_DIR})"
    )
    args = parser.parse_args()

    # Запрашиваем подтверждение
    print(f"⚠️ ВНИМАНИЕ: Будет выполнена конвертация файлов в '{args.dir}'!")
    confirm = input("Продолжить? (y/n): ").strip().lower()
    if confirm == 'y':
        scan_and_convert(args.dir)
    else:
        print("❌ Отменено.")