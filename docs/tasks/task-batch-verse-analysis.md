# Задачи: пакетный анализ стихов («Анализировать всё») + analyzerName

> Оркестратор: Агент 0. Контракты: Агент 6 (см. sangraha-service.md §2–§5.2, §7,
> openapi/sangraha/sangraha-service.yaml, openapi/sangraha/schemas/sangraha-schemas.yaml,
> docs/tasks/attachments/B-batch-verse-analysis.md — уже обновлены, это входной
> контракт для задач ниже).
> Задачи разбиты на шаги ~30 сек работы модели (DeepSeek V4 Flash / Qwen3 Coder 30B A3B
> Instruct) — каждый шаг самодостаточен, ссылается на конкретный файл/раздел контракта.

---

## Агент 2 — Backend (sangraha-service)

**B1. Промпт.** Заменить файл `resources/prompts/verse-analysis.md` целиком
содержимым приложения `docs/tasks/attachments/B-batch-verse-analysis.md` (tool
переименован в `submit_verse_analyses`, принимает/возвращает массив `verses[]`
с `verseIndex` — см. sangraha-service.md §5.1–§5.2).

**B2. Миграция.** Добавить
`services/sangraha-service/src/main/resources/db/migration/V2__verse_analyses_add_analyzer_name.sql`:
`ALTER TABLE sangraha.verse_analyses ADD COLUMN analyzer_name varchar(200)`,
backfill существующих строк значением `model_name`, затем `SET NOT NULL`
(см. sangraha-service.md §3).

**B3. Entity.** В `model/VerseAnalysis.java` добавить поле `analyzerName`
(`@Column(name = "analyzer_name", nullable = false)`), рядом с существующим
`modelName` (см. sangraha-service.md §2).

**B4. DTO.** В `dto/VerseAnalysisDto.java` добавить поле `analyzerName` рядом
с `modelName` — sangraha-schemas.yaml#VerseAnalysisDto.

**B5. Tool-схема (batch).** В `service/LlmToolSchemaBuilder` переименовать
`buildFunctionDefinitionSchema()` в `buildBatchFunctionDefinitionSchema()`:
корневой объект — свойство `verses` (`type: array`, `items` = текущий
per-verse объект + добавленное поле `verseIndex` (`type: integer`, добавить в
`required`) + существующие textDevanagari/textIast/translationRu/
translationEn/sandhiSplits/words). Аналогично обновить
`service/JsonSchemas.buildVerseAnalysisSchema()` — обернуть в `verses[]` с
`verseIndex`, переименовать метод в `buildVerseAnalysesSchema()`.

**B6. LlmPromptBuilder.** Заменить `extractSystemPrompt()` без изменений
сигнатуры (файл промпта тот же, просто новое содержимое из B1). Добавить
`buildBatchUserPrompt(List<Verse> verses)`: для каждого стиха в списке (индекс
= позиция в списке = `verseIndex`) вывести блок `verseIndex/textDevanagari/
textIast` (использовать `verse.getRawText()`, как в текущем
`buildUserPrompt`), между блоками пустая строка, в конце — тот же блок правил
сандхи (`emenau-sandhi-rules.json`), что и в `buildUserPrompt` (см.
attachments/B-batch-verse-analysis.md, секция `## user`).

**B7. Strategy.** В `service/strategy/SinglePassStrategy.call(Verse verse)`
изменить сигнатуру на `call(List<Verse> verses)`: использовать
`promptBuilder.buildBatchUserPrompt(verses)`,
`toolSchemaBuilder.buildBatchFunctionDefinitionSchema()`, имя tool —
`submit_verse_analyses`. Интерфейс `LlmCallStrategy.call(Verse verse)` →
`call(List<Verse> verses)`; `TwoPassStrategy` аналогично (pass2 tool тоже
`submit_verse_analyses`/batch-схема; pass1 — тот же текст со списком стихов
через `buildBatchUserPrompt`, без изменений остальной логики двух проходов).

**B8. LlmClient.** `call(Verse verse)` → `call(List<Verse> verses)`,
делегирует в `strategy.call(verses)`. `extractToolArguments` — переименовать
внутреннюю константу `TOOL_NAME` на `submit_verse_analyses`, метод возвращает
`JsonNode` поля `arguments.get("verses")` (весь массив, а не один объект) —
переименовать в `extractVersesArguments`, возвращает `JsonNode` (ArrayNode)
или `null`, если tool_call некорректен/имя не совпадает.

**B9. VerseAnalysisSaver.** Добавить параметр `analyzerName` (в данной
реализации = тому же значению, что и `modelName`) в `saveResults(...)` —
пробросить в `VerseAnalysis.builder().analyzerName(analyzerName)`.

**B10. VerseAnalysisService.** Переименовать текущий `analyze(UUID verseId,
String rawText)`:
1. Общий приватный метод `runAnalysis(List<Verse> verses)`: выставляет всем
   переданным стихам статус `ANALYZING`, вызывает `llmClient.call(verses)`,
   `llmClient.extractVersesArguments(response)`, итерирует
   `versesNode` — по полю `verseIndex` каждого элемента находит
   соответствующий `Verse` из входного списка (по позиции в списке = индексу),
   валидирует обязательные поля (как в текущем коде), вызывает
   `analysisSaver.saveResults(..., analyzerName)` **в try/catch на каждый
   элемент** — ошибка по одному стиху не должна прерывать обработку остальных
   (лог ошибки + `analysisSaver.markFailed(verse)` для этого стиха, continue).
   Стихи, для которых элемент `verses[]` вообще не пришёl (нет такого
   verseIndex в ответе) — тоже `markFailed`.
2. `analyze(UUID verseId, String rawText)` — сохраняет `rawText` в стих (как
   сейчас), вызывает `runAnalysis(List.of(verse))`.
3. Новый метод `analyzeChapter(UUID chapterId)`: находит главу, выбирает
   `verseRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(chapterId)`,
   фильтрует по `status IN (DRAFT, FAILED)`; если список пуст — бросает
   исключение (контроллер должен вернуть `409`, см. B11); иначе вызывает
   `runAnalysis(filteredVerses)` и возвращает список `verseId` этих стихов.

**B11. Controller.** В `controller/VerseAnalyzeController` (или новом
`ChapterAnalyzeController` рядом с ним) добавить
`POST /api/v1/sangraha/chapters/{chapterId}/verses/analyze-all` — вызывает
`verseAnalysisService.analyzeChapter(chapterId)`, возвращает `202` с телом
`{chapterId, verseIds}` (sangraha-schemas.yaml#AnalyzeAllVersesResponse);
`IllegalStateException`/аналог с "no verses to analyze" → маппится в `409`
(тем же механизмом, что уже обрабатывает 404 для "not found", см. текущий
`@ExceptionHandler`, если он есть — иначе добавить локальный catch в
контроллере).

---

## Агент 3 — Frontend

**F1. API-клиент.** В `frontend/src/api/sangraha.ts` добавить
`analyzeAllVerses: (chapterId: string) => api.post<{ chapterId: string;
verseIds: string[] }>(`${BASE}/chapters/${chapterId}/verses/analyze-all`)`.

**F2. Хук.** В `frontend/src/hooks/useSangraha.ts` добавить
`useAnalyzeAllVerses()` — `useMutation`, `mutationFn: (chapterId: string) =>
sangrahaApi.analyzeAllVerses(chapterId)`, `onSuccess` инвалидирует
`['sangraha', 'chapter', chapterId]` и `['sangraha', 'work']` (тот же паттерн,
что у `useAnalyzeVerse`).

**F3. Кнопка на ChapterPage.** В `frontend/src/pages/sangraha/ChapterPage.tsx`
добавить кнопку «Анализировать всё» (`t('sangraha.action.analyzeAll')`) в
блок шапки страницы (рядом с заголовком главы, использовать существующий
`IconButton`/PrimeReact `Button` с иконкой `pi-play` или `pi-sync`). По клику
— `useAnalyzeAllVerses().mutate(chapterId)`. `disabled`, если среди
`chapter.verses` нет ни одного со `status` `DRAFT`/`FAILED`, и во время
выполнения мутации (`isPending`) — показать `pi-spin pi-spinner`.

**F4. i18n.** В `frontend/src/i18n/locales/en/sangraha.json` и `ru/sangraha.json`
добавить ключ `sangraha.action.analyzeAll`: en `"Analyze All"`, ru
`"Анализировать всё"`.

---

## DoD

- [ ] Реализация соответствует `sangraha-service.md` §2, §3, §5.1–§5.2, §7 и
      этому файлу
- [ ] `docs/openapi/sangraha/sangraha-service.yaml` +
      `schemas/sangraha-schemas.yaml` не меняются Агентами 2/3 — это готовый
      контракт от Агента 6
- [ ] Существующий одиночный анализ (`POST /verses/{verseId}/analyze`)
      продолжает работать (реализован через тот же batch-путь с одним
      элементом)
- [ ] Checkstyle/SpotBugs чисты, миграция накатывается на чистую БД
