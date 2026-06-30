import os
import sys
from collections import defaultdict

# Константа с путем по умолчанию
DEFAULT_DIR = r"C:\MyDev\samskrtam"

# Расширения файлов для анализа
#EXTENSIONS = ('.java', '.ts', '.tsx', '.yaml', '.yml')
EXTENSIONS = '.java'
#EXTENSIONS = ( '.yaml')
#EXTENSIONS = '.md'

# Директории для исключения при сканировании
EXCLUDED_DIRS = {
    'node_modules', '.git', 'build', 'dist', 'target', '__pycache__',
    '.idea', '.vscode', 'venv', 'env', '.venv', 'logs', 'tmp', 'temp',
    '.gradle', '.kotlin', 'gradle', 'etcetera'
}

def count_lines_in_file(file_path):
    """Подсчитывает количество строк в файле"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return len(f.readlines())
    except Exception as e:
        print(f"⚠️ Не удалось прочитать {file_path}: {e}")
        return 0

def get_file_extension(filename):
    """Возвращает расширение файла"""
    return os.path.splitext(filename)[1].lower() or 'no_extension'

def scan_directory(directory):
    """Сканирует директорию и собирает информацию о файлах"""
    files_info = []
    total_files_scanned = 0
    skipped_files = 0

    for root, dirs, files in os.walk(directory):
        # Игнорируем исключенные директории
        dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS and not d.startswith('.')]

        for file in files:
            # Игнорируем скрытые файлы
            if file.startswith('.'):
                continue

            extension = get_file_extension(file)

            # Проверяем, входит ли расширение в список разрешенных
            if extension not in EXTENSIONS:
                skipped_files += 1
                continue

            full_path = os.path.join(root, file)
            lines_count = count_lines_in_file(full_path)

            # Относительный путь для красивого вывода
            rel_path = os.path.relpath(full_path, directory)

            files_info.append({
                'path': full_path,
                'rel_path': rel_path,
                'filename': file,
                'extension': extension,
                'lines': lines_count
            })
            total_files_scanned += 1

    if skipped_files > 0:
        print(f"ℹ️ Пропущено файлов с другими расширениями: {skipped_files}")

    return files_info

def main():
    # Используем DEFAULT_DIR, если не передан аргумент командной строки
    if len(sys.argv) > 1:
        directory = sys.argv[1]
    else:
        # Проверяем, существует ли DEFAULT_DIR
        if os.path.exists(DEFAULT_DIR) and os.path.isdir(DEFAULT_DIR):
            directory = DEFAULT_DIR
            print(f"ℹ️ Используется путь по умолчанию: {DEFAULT_DIR}")
        else:
            # Если DEFAULT_DIR не существует, запрашиваем путь у пользователя
            print(f"⚠️ Путь по умолчанию '{DEFAULT_DIR}' не найден!")
            directory = input("Введите путь к директории: ").strip()
            if not directory:
                directory = os.getcwd()  # Текущая директория по умолчанию

    # Проверяем, существует ли директория
    if not os.path.exists(directory):
        print(f"❌ Ошибка: Директория '{directory}' не найдена!")
        sys.exit(1)

    if not os.path.isdir(directory):
        print(f"❌ Ошибка: '{directory}' не является директорией!")
        sys.exit(1)

    print("="*80)
    print("📊 АНАЛИЗ ФАЙЛОВ С БОЛЕЕ 100 СТРОК КОДА")
    print("="*80)
    print(f"📁 Директория: {directory}")
    print(f"📋 Анализируемые расширения: {', '.join(EXTENSIONS)}")
    print(f"🚫 Исключенные директории: {', '.join(sorted(EXCLUDED_DIRS)[:10])}{'...' if len(EXCLUDED_DIRS) > 10 else ''}")
    print()

    # Сканируем директорию
    print("⏳ Сканирование файлов...")
    files_info = scan_directory(directory)
    print(f"✅ Найдено файлов с указанными расширениями: {len(files_info)}\n")

    if not files_info:
        print("❌ Не найдено файлов с указанными расширениями!")
        return

    # Фильтруем файлы с более чем 100 строками
    large_files = [f for f in files_info if f['lines'] > 100]

    if not large_files:
        print("✅ Нет файлов с более чем 100 строками кода!")
        return

    print(f"📄 Найдено файлов с >100 строк: {len(large_files)}\n")

    # Разделяем на Java и не-Java файлы (Java - в приоритете)
    java_files = [f for f in large_files if f['extension'] == '.java']
    other_files = [f for f in large_files if f['extension'] != '.java']

    # Сортируем Java файлы по количеству строк (по убыванию)
    java_files.sort(key=lambda x: x['lines'], reverse=True)

    # Группируем остальные файлы по расширению
    files_by_extension = defaultdict(list)
    for file_info in other_files:
        files_by_extension[file_info['extension']].append(file_info)

    # Сортируем расширения по алфавиту
    sorted_extensions = sorted(files_by_extension.keys())

    total_large_files = 0
    total_large_lines = 0

    # ============ ВЫВОД JAVA ФАЙЛОВ ============
    if java_files:
        print("="*80)
        print("☕ JAVA ФАЙЛЫ С БОЛЕЕ 100 СТРОК")
        print("="*80)
        print(f"📁 Всего Java файлов: {len(java_files)}")
        print("-" * 80)

        for idx, file_info in enumerate(java_files, 1):
            print(f"  {idx:3d}. {file_info['rel_path']}")
            print(f"       📝 Строк: {file_info['lines']:,}")
            total_large_files += 1
            total_large_lines += file_info['lines']

        # Статистика по Java файлам
        total_java_lines = sum(f['lines'] for f in java_files)
        avg_java_lines = total_java_lines // len(java_files) if java_files else 0
        print(f"\n  📊 Статистика Java файлов:")
        print(f"     Всего файлов: {len(java_files)}")
        print(f"     Всего строк: {total_java_lines:,}")
        print(f"     Среднее строк: {avg_java_lines:,}")
        print(f"     Максимум строк: {java_files[0]['lines']:,}")
        print(f"     Минимум строк: {java_files[-1]['lines']:,}")
        print("="*80)
        print()

    # ============ ВЫВОД ОСТАЛЬНЫХ ФАЙЛОВ ============
    if other_files:
        print("="*80)
        print("📄 ОСТАЛЬНЫЕ ФАЙЛЫ С БОЛЕЕ 100 СТРОК (НЕ JAVA)")
        print("="*80)
        print(f"📁 Всего файлов: {len(other_files)}")
        print("-" * 80)

        # Сначала показываем группировку по расширениям
        for extension in sorted_extensions:
            files_list = files_by_extension[extension]
            # Сортируем файлы внутри расширения по количеству строк (по убыванию)
            files_list.sort(key=lambda x: x['lines'], reverse=True)

            ext_display = extension if extension else "Без расширения"
            print(f"\n📁 {ext_display} ({len(files_list)} файлов)")
            print("-" * 80)

            for idx, file_info in enumerate(files_list, 1):
                print(f"  {idx:2d}. {file_info['rel_path']}")
                print(f"      📝 Строк: {file_info['lines']:,}")
                total_large_files += 1
                total_large_lines += file_info['lines']

            # Статистика по расширению
            total_ext_lines = sum(f['lines'] for f in files_list)
            avg_ext_lines = total_ext_lines // len(files_list) if files_list else 0
            print(f"\n  📊 Статистика для {ext_display}:")
            print(f"     Всего файлов: {len(files_list)}")
            print(f"     Всего строк: {total_ext_lines:,}")
            print(f"     Среднее строк: {avg_ext_lines:,}")
            print(f"     Максимум строк: {files_list[0]['lines']:,}" if files_list else "")
            print(f"     Минимум строк: {files_list[-1]['lines']:,}" if files_list else "")

    # Общая статистика
    print("\n" + "="*80)
    print("📈 ОБЩАЯ СТАТИСТИКА")
    print("="*80)
    print(f"  Всего файлов с >100 строк: {total_large_files}")
    print(f"  Всего строк в этих файлах: {total_large_lines:,}")
    print(f"  Среднее строк на файл: {total_large_lines // total_large_files if total_large_files > 0 else 0:,}")

    # Топ-10 самых больших файлов (всех)
    print("\n" + "="*80)
    print("🏆 ТОП-10 САМЫХ БОЛЬШИХ ФАЙЛОВ")
    print("="*80)
    top_files = sorted(large_files, key=lambda x: x['lines'], reverse=True)[:10]
    for idx, file_info in enumerate(top_files, 1):
        java_marker = "☕" if file_info['extension'] == '.java' else "📄"
        print(f"  {idx:2d}. {java_marker} {file_info['rel_path']}")
        print(f"      📝 {file_info['lines']:,} строк | 📁 {file_info['extension'] or 'без расширения'}")

    # Сохраняем результаты в файл
    output_file = os.path.join(directory, "large_files_report.txt")
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("="*80 + "\n")
        f.write("📊 ОТЧЕТ О ФАЙЛАХ С БОЛЕЕ 100 СТРОК КОДА\n")
        f.write("="*80 + "\n")
        f.write(f"Директория: {directory}\n")
        f.write(f"Анализируемые расширения: {', '.join(EXTENSIONS)}\n")
        f.write(f"Всего файлов с >100 строк: {total_large_files}\n")
        f.write(f"Всего строк: {total_large_lines:,}\n\n")

        # Java файлы
        if java_files:
            f.write("="*80 + "\n")
            f.write("☕ JAVA ФАЙЛЫ С БОЛЕЕ 100 СТРОК\n")
            f.write("="*80 + "\n")
            f.write(f"Всего Java файлов: {len(java_files)}\n\n")

            for file_info in java_files:
                f.write(f"  {file_info['rel_path']} - {file_info['lines']:,} строк\n")

            total_java_lines = sum(f['lines'] for f in java_files)
            f.write(f"\nСтатистика Java: {len(java_files)} файлов, {total_java_lines:,} строк\n\n")

        # Остальные файлы
        if other_files:
            f.write("="*80 + "\n")
            f.write("📄 ОСТАЛЬНЫЕ ФАЙЛЫ (НЕ JAVA)\n")
            f.write("="*80 + "\n")

            for extension in sorted_extensions:
                files_list = files_by_extension[extension]
                files_list.sort(key=lambda x: x['lines'], reverse=True)

                ext_display = extension if extension else "Без расширения"
                f.write(f"\n{ext_display} ({len(files_list)} файлов):\n")
                f.write("-"*80 + "\n")

                for file_info in files_list:
                    f.write(f"  {file_info['rel_path']} - {file_info['lines']:,} строк\n")

                total_ext_lines = sum(f['lines'] for f in files_list)
                f.write(f"\n  Статистика: {len(files_list)} файлов, {total_ext_lines:,} строк\n")

    print(f"\n💾 Полный отчет сохранен в: {output_file}")
    print("="*80)

if __name__ == "__main__":
    main()