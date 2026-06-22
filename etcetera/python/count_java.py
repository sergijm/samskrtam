import os
import sys

def count_java_files_and_lines(directory):
    """Рекурсивно подсчитывает количество .java файлов и строк в них"""
    java_files = []
    total_lines = 0
    total_files = 0

    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                full_path = os.path.join(root, file)
                java_files.append(full_path)
                total_files += 1

                # Считаем строки в файле
                try:
                    with open(full_path, 'r', encoding='utf-8') as f:
                        lines = f.readlines()
                        # Считаем все строки (включая пустые)
                        total_lines += len(lines)
                except Exception as e:
                    print(f"⚠️ Не удалось прочитать {full_path}: {e}")

    return java_files, total_files, total_lines

def main():
    # Путь к вашей директории
    directory = r"C:\MyDev\samskrtam"

    # Проверяем, существует ли директория
    if not os.path.exists(directory):
        print(f"❌ Ошибка: Директория '{directory}' не найдена!")
        sys.exit(1)

    # Собираем все Java файлы и считаем строки
    java_files, total_files, total_lines = count_java_files_and_lines(directory)

    # Выводим результаты
    print("="*70)
    print("📊 СТАТИСТИКА JAVA ПРОЕКТА")
    print("="*70)
    print(f"\n📁 Директория: {directory}")
    print(f"\n📄 Найдено Java файлов: {total_files}")
    print(f"📝 Общее количество строк: {total_lines:,}")
    print(f"📏 Среднее строк на файл: {total_lines // total_files if total_files > 0 else 0:,}")

    # Статистика по сервисам (первые 2 уровня папок)
    print("\n" + "="*70)
    print("📦 СТАТИСТИКА ПО СЕРВИСАМ/МОДУЛЯМ")
    print("="*70)

    services = {}
    for file_path in java_files:
        rel_path = os.path.relpath(file_path, directory)
        parts = rel_path.split(os.sep)

        # Определяем сервис (первый уровень)
        if len(parts) >= 1:
            service = parts[0]
            if service not in services:
                services[service] = {"files": 0, "lines": 0}
            services[service]["files"] += 1

            # Считаем строки для этого сервиса
            try:
                with open(file_path, 'r', encoding='utf-8') as f:
                    lines = len(f.readlines())
                    services[service]["lines"] += lines
            except:
                pass

    # Сортируем по количеству файлов (по убыванию)
    sorted_services = sorted(services.items(), key=lambda x: x[1]["files"], reverse=True)

    for service, stats in sorted_services:
        print(f"\n  📂 {service}")
        print(f"     📄 Файлов: {stats['files']}")
        print(f"     📝 Строк:  {stats['lines']:,}")
        print(f"     📏 Среднее: {stats['lines'] // stats['files'] if stats['files'] > 0 else 0:,} строк/файл")

    # Детальная статистика по типу файлов (Entity, Service, Controller, DTO, Mapper, Repository, etc)
    print("\n" + "="*70)
    print("📋 СТАТИСТИКА ПО ТИПАМ КЛАССОВ")
    print("="*70)

    types = {
        "Entity": 0,
        "Repository": 0,
        "Service": 0,
        "Controller": 0,
        "DTO": 0,
        "Mapper": 0,
        "Config": 0,
        "Util": 0,
        "Exception": 0,
        "Other": 0
    }

    for file_path in java_files:
        filename = os.path.basename(file_path)
        if "Entity" in filename or file_path.endswith("entity") or "entity" in file_path.lower():
            types["Entity"] += 1
        elif "Repository" in filename or file_path.endswith("repository") or "repository" in file_path.lower():
            types["Repository"] += 1
        elif "Service" in filename or file_path.endswith("service") or "service" in file_path.lower():
            types["Service"] += 1
        elif "Controller" in filename or file_path.endswith("controller") or "controller" in file_path.lower():
            types["Controller"] += 1
        elif "DTO" in filename or file_path.endswith("dto") or "dto" in file_path.lower():
            types["DTO"] += 1
        elif "Mapper" in filename or file_path.endswith("mapper") or "mapper" in file_path.lower():
            types["Mapper"] += 1
        elif "Config" in filename or file_path.endswith("config") or "config" in file_path.lower():
            types["Config"] += 1
        elif "Util" in filename or file_path.endswith("util") or "util" in file_path.lower():
            types["Util"] += 1
        elif "Exception" in filename or file_path.endswith("exception") or "exception" in file_path.lower():
            types["Exception"] += 1
        else:
            types["Other"] += 1

    for type_name, count in types.items():
        if count > 0:
            print(f"  {type_name}: {count} файлов")

    print("\n" + "="*70)
    print("📈 ОБЩАЯ СТАТИСТИКА")
    print("="*70)
    print(f"  Всего сервисов/модулей: {len(services)}")
    print(f"  Всего Java файлов:      {total_files}")
    print(f"  Всего строк кода:       {total_lines:,}")
    print(f"  Среднее строк на файл:  {total_lines // total_files if total_files > 0 else 0:,}")
    print(f"  Среднее файлов на сервис: {total_files // len(services) if len(services) > 0 else 0}")
    print("="*70)

    # Сохраняем результаты в файл
    output_file = os.path.join(directory, "java_stats.txt")
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write("="*70 + "\n")
        f.write("📊 СТАТИСТИКА JAVA ПРОЕКТА\n")
        f.write("="*70 + "\n")
        f.write(f"\nДиректория: {directory}\n")
        f.write(f"Java файлов: {total_files}\n")
        f.write(f"Общее количество строк: {total_lines:,}\n")
        f.write(f"Среднее строк на файл: {total_lines // total_files if total_files > 0 else 0:,}\n\n")

        f.write("="*70 + "\n")
        f.write("📦 СТАТИСТИКА ПО СЕРВИСАМ/МОДУЛЯМ\n")
        f.write("="*70 + "\n")
        for service, stats in sorted_services:
            f.write(f"\n{service}:\n")
            f.write(f"  Файлов: {stats['files']}\n")
            f.write(f"  Строк:  {stats['lines']:,}\n")
            f.write(f"  Среднее: {stats['lines'] // stats['files'] if stats['files'] > 0 else 0:,} строк/файл\n")

    print(f"\n💾 Полная статистика сохранена в: {output_file}")

if __name__ == "__main__":
    main()