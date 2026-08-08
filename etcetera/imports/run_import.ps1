# Путь к корневой директории с файлами
$BaseDir = "C:\MyDev\OliverHellwig\sanskrit\dcs\data\conllu\files"
# Строка подключения к базе данных
$DbConn = "postgresql://postgres:postgres@localhost:5436/samskrtam"
# Путь к вашему Python-скрипту (если он не в текущей папке, укажите полный путь)
$PythonScript = "import_dcs_conllu.py"

# Проверка существования базовой директории
if (-not (Test-Path -Path $BaseDir)) {
    Write-Error "Директория не найдена: $BaseDir"
    exit 1
}

# Получаем список всех папок первого уровня внутри BaseDir
$Directories = Get-ChildItem -Path $BaseDir -Directory

Write-Host "Найдено директорий для импорта: $($Directories.Count)" -ForegroundColor Cyan

foreach ($dir in $Directories) {
    $dirName = $dir.Name
    $targetDir = $dir.FullName

    Write-Host "----------------------------------------" -ForegroundColor Yellow
    Write-Host "Запуск импорта для произведения: $dirName" -ForegroundColor Green
    Write-Host "Путь: $targetDir" -ForegroundColor Gray
    Write-Host "----------------------------------------" -ForegroundColor Yellow

    # Вызов Python-скрипта
    python $PythonScript --db $DbConn --dir $targetDir

    # Проверка кода возврата Python-скрипта
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Скрипт завершился с ошибкой для директории: $dirName (код: $LASTEXITCODE)"
    }
}

Write-Host "Все задачи импорта выполнены." -ForegroundColor Green