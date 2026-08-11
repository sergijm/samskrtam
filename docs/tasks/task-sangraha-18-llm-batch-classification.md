# Задача: sangraha-service — модуль lexicon-classification: LLM batch-классификация

**Что:** Таблицы/сущности `LemmaClassification`, `ClassificationBatch`,
`ClassificationRun`, промпт + tool-calling классификации, эндпоинт запуска
прогона.
**Зачем:** См. `docs/services/sangraha-service/lemma-classification.md` §1.6–§1.7, §2–§3.

## Зависит от
task-sangraha-17-lemma-aggregation.md. Переиспользует LLM-клиент/конвенции
`sangraha-service.md` §5 (env `SANGRAHA_LLM_BASE_URL`/`SANGRAHA_LLM_API_KEY`/
`SANGRAHA_LLM_MODEL`, tool calling — по образцу существующего анализа стиха).

## Шаги

### Часть A — схема и сущности

1. Миграция: таблица `sangraha.classification_run` — поля по §1.7 (`schemeCode`, `requestedBatchCount`, `completedBatchCount`, `status`, `requestedBy`, `createdAt`, `completedAt`).
2. Миграция: таблица `sangraha.classification_batch` — поля по §1.7 (`schemeCode`, `runId` FK, `lemmaCount`, `status`, `errorMessage`, `llmModel`, `createdAt`, `completedAt`).
3. Миграция: таблица `sangraha.lemma_classification` — поля по §1.6 (`lemmaId` FK CASCADE, `schemeCode` FK, `categoryCode`, `glossRu`, `glossEn`, `confidence`, `status` DEFAULT `CANDIDATE`, `llmModel`, `batchId` FK, `reviewedBy`, `reviewedAt`), `UNIQUE(lemma_id, scheme_code)`.
4. Entity `ClassificationRun`, `ClassificationBatch`, `LemmaClassification` (enum `ClassificationStatus{CANDIDATE,APPROVED,REJECTED}`).

### Часть B — промпт и tool

5. Файл `prompts/lemma-classification.md` — по образцу `prompts/verse-analysis.md` (структура/формат файла), содержимое по §2.1: место под список 42 категорий (шаблонизируется на каждый вызов, не статичный текст) + инструкция «одно наиболее вероятное значение перевода», без выбора по контексту конкретного стиха.
6. `LemmaClassificationPromptBuilder`: подставляет актуальный список `CurriculumSemanticTopic` (code+labelRu/En+description) в шаблон промпта.
7. Tool-схема `submit_lemma_classification` — параметры по §2.3 (массив `{lemmaId, categoryCode, glossRu, glossEn, confidence?}`), `confidence` необязателен.
8. `LemmaClassificationLlmClient.classifyBatch(lemmas: List<Lemma>)`: собирает вход батча (§2.2 — `lemmaIast`/`lemmaDevanagari`/`dominantPosCode`/`gender` + до 2 примеров `surfaceIast`+контекст из `VerseWord` этой леммы, если есть), вызывает LLM с tool `submit_lemma_classification`, парсит ответ.

### Часть C — валидация ответа

9. Валидация `categoryCode`: должен присутствовать среди `CurriculumSemanticTopic.code` — если нет, `categoryCode = null` в сохраняемой строке, но `glossRu`/`glossEn` (если валидны) сохраняются (§2.3).
10. Валидация письменности (§2.4): `glossRu`/`glossEn` не должны содержать деванагари-символы (regex по диапазону Unicode Devanagari) — при нарушении вся строка леммы отбрасывается (не сохраняется, останется неклассифицированной для следующего run).
11. Если ответ модели не содержит вызова tool'а, либо JSON невалиден — весь батч `FAILED` (не по одной лемме).

### Часть D — оркестрация прогона

12. `LemmaClassificationRunService.startRun(schemeCode, batchSize, batchCount)`: шаг отбора кандидатов — `Lemma` без `LemmaClassification(schemeCode, status != REJECTED)`, сортировка по `frequencyRank ASC`, `LIMIT batchSize*batchCount`.
13. Разбить отобранный список на батчи по `batchSize` подряд (детерминированно, без shuffle).
14. Последовательная обработка батчей: создать `ClassificationBatch(status=PENDING)` → вызвать LLM (шаг 8) → валидация (шаги 9–11) → upsert `LemmaClassification` на валидные строки (`status=CANDIDATE`) → `ClassificationBatch.status = SUCCESS`, либо при ошибке — `FAILED` + `errorMessage`, продолжить со следующим батчем (не прерывать run).
15. `ClassificationRun.completedBatchCount` инкрементируется после каждого батча (успешного или нет); финальный `status = COMPLETED`, если все успешны, иначе `COMPLETED_WITH_ERRORS`.
16. `POST /sangraha/internal/lexicon/classification/runs` (ADMIN) `{schemeCode, batchSize, batchCount}` — `batchCount` обязателен, без дефолта (§3 шаг 2); валидация `ClassificationScheme.isActive = true`, иначе 400. Возвращает результат по §3 шаг 6.
17. `GET /sangraha/internal/lexicon/classification/runs/{runId}` — текущий/финальный статус прогона.

## DoD
- [ ] Отбор кандидатов не включает леммы с `status=REJECTED` классификацией по той же схеме
- [ ] Неизвестный `categoryCode` от модели не роняет сохранение перевода (шаг 9)
- [ ] Деванагари в `glossRu`/`glossEn` отклоняет строку (шаг 10)
- [ ] Ошибка одного батча не прерывает остальные батчи прогона (шаг 14)
- [ ] Повторный запуск `runs` по тем же лемам после первого успешного прогона не выбирает их снова (`UNIQUE(lemma_id, scheme_code)` + фильтр отбора)
