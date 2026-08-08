# Задача: curriculum-service + sangraha-service — batch-импорт лексики из корпуса

**Что:** Экспорт `VerseWord[]` на стороне sangraha-service + детерминированный
(без LLM) batch-импорт в `curriculum.lexeme` на стороне curriculum-service.
**Зачем:** См. `docs/services/lexicon-content-pipeline.md` §1–§4 — единственный
источник 2000 лемм, только эвристики, без AI-enrichment.

## Зависит от
task-curriculum-13-lexicon-schema-entities.md

---

## Часть A — sangraha-service (новый internal-эндпоинт)

1. `VerseWordExportController`: `GET /sangraha/internal/content/verse-words/export?cursor={verseId}&limit=500` — не публичный, не через gateway (по аналогии с `sangraha-service.md` §9).
2. Запрос: все `VerseWord` из `Verse` со `status = ANALYZED`, отсортированные по `verseId`, постранично курсором (`verseId > cursor`, `LIMIT 500`).
3. Поля ответа на строку: `verseId`, `workSlug`, `chapterSlug`, `verseOrderIndex`, `lemmaIast`, `stem`, `surfaceIast`, `surfaceDevanagari`, `pos`, `lemmaGlossRu`, `lemmaGlossEn`, `gender` (из `VerseWordMorphology`, nullable), `vowelType` (из `VerseWordMorphology`, nullable).
4. Ответ: `{ items: [...], nextCursor: "uuid-or-null" }`, `nextCursor = null`, когда строк больше нет.
5. Юнит-тест: 3 стиха разного статуса (ANALYZED/DRAFT/FAILED) — экспорт возвращает слова только из ANALYZED.

## Часть B — curriculum-service: клиент к sangraha-service

6. `SangrahaExportClient` (`RestClient`, env `SANGRAHA_SERVICE_URL`, уже используется по аналогии с `sangraha-service.md` §9/`CONTENT_SERVICE_URL`): метод `fetchPage(cursor, limit)`, парсинг ответа §4.
7. `LexiconImportService.importFromSangraha()`: цикл по страницам через `SangrahaExportClient` до `nextCursor = null`, накапливает все строки в память (объём ≤ несколько десятков тысяч строк, приемлемо для batch-джобы).

## Часть C — группировка, частота, эвристики (без LLM)

8. Группировка накопленных строк по `(lemmaSlp1, gender)` — `lemmaSlp1` вычисляется из `lemmaIast` существующим IAST→SLP1 конвертером проекта (тот же, что уже есть для slug, см. `sangraha-service.md` §8).
9. Подсчёт `occurrenceCount` на группу, сортировка групп по убыванию `occurrenceCount` (tie-break — алфавит по `lemmaSlp1``) → присвоение `frequencyRank` (1..N).
10. Маппинг `VerseWord.pos` → `curriculum.part_of_speech.code`: таблица соответствий constant-map в коде (зафиксировать по факту встреченных значений `pos` в выгрузке — если код не найден, оставить `posCode = null`, не бросать ошибку).
11. Маппинг `gender`: прямой перенос значений enum (те же значения, что уже согласованы в `sangraha-service.md` §9).
12. Маппинг `vowelType`/verb-класс → `morphology_class.code`: по таблице соответствий 1:1 (`lexical-curriculum.md` §5); fallback — по последней букве `stem`, если `vowelType` не заполнен, тем же правилом, что уже описано в `sangraha-service.md` §9.
13. Выбор `glossRu`/`glossEn` группы — от строки-представителя с наибольшим числом вхождений внутри группы (tie-break — наименьший `verseId`).

## Часть D — upsert в БД

14. `LexemeRepository.findByLemmaSlp1AndGender(...)` — если найдено, обновить только `frequencyRank`/пересчитать occurrences (не трогать `status`/ручные правки полей); если не найдено — создать новую строку `status = CANDIDATE`.
15. Для найденных `posCode`/`morphologyClassCode` — заполнить `LexemePos`/`LexemeMorphology`, если связей ещё нет (не затирать существующие ручные привязки).
16. `Source`/`SourceOccurrence`: `findByExternalSangrahaWorkSlug(workSlug)`, если нет — создать; для каждой исходной строки — `SourceOccurrence` (dedup по `(sourceId, lexemeId, locationRef, surfaceFormIast)`).
17. После полного импорта — пересчёт `totalOccurrencesCache`/`uniqueLemmaCountCache` по всем затронутым `Source` одним batch-запросом (`COUNT`/`COUNT DISTINCT`).

## Часть E — API и идемпотентность

18. `POST /api/v2/lexicon/import/from-sangraha` (ADMIN) — синхронно запускает шаги 7–17, возвращает `{ importedCount, updatedCount, totalLexemeCount, sourcesTouched }`.
19. Повторный вызов на тех же данных не создаёт дублей `Lexeme`/`SourceOccurrence` (проверяется интеграционным тестом: два подряд вызова с одинаковой выгрузкой → второй вызов `importedCount = 0`).

## DoD
- [ ] Экспорт sangraha-service отдаёт только `ANALYZED` стихи, работает постранично
- [ ] Импорт корректно группирует по `(lemmaSlp1, gender)` и считает `frequencyRank`
- [ ] Повторный запуск идемпотентен (шаг 19)
- [ ] `totalOccurrencesCache`/`uniqueLemmaCountCache` совпадают с прямым `COUNT` после импорта
