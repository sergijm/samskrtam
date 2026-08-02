# Задача: `GET /verse` и `POST /verse/analysis` — произвольный список стихов

> Оркестратор: Агент 0. Контракт: Агент 6 (см. `docs/services/sangraha-service/
> batch-verse-review.md` — источник истины для задач ниже). Затронут только
> sangraha-service.
>
> Задачи разбиты на шаги ~30 сек работы модели (DeepSeek V4 Flash / Qwen3 Coder
> 30B A3B Instruct) — каждый шаг самодостаточен.

---

## Агент 2 — Backend (sangraha-service)

**B1. `GET /api/v1/sangraha/verse` — новый контроллер-метод.** Query-параметр
`id` — список (Spring сам собирает повторяющиеся `?id=...&id=...` в
`List<UUID>` при объявлении параметра как `@RequestParam List<UUID> id`).
Найти все `Verse` по этим id (`findAllByIdInAndDeletedAtIsNull` — если такого
метода нет в `VerseRepository`, добавить), не найденные/удалённые id — просто
отсутствуют в ответе, не ошибка. Собрать `VersesBatchResponseDto` (новый
record/DTO): `verses: List<VerseBatchItemDto>`, каждый элемент — `id`,
`workSlug`, `workTitleRu`, `workTitleEn`, `chapterSlug`, `chapterTitleRu`,
`chapterTitleEn`, `verseOrderIndex`, `textIastPreview` (переиспользовать ту же
обрезку текста, что уже используется для дерева `GET /works/{workSlug}` —
найти существующий метод/утилиту превью, не писать новую с нуля), `status`.
Порядок элементов ответа — по порядку `id` в запросе.

**B2. `POST /api/v1/sangraha/verse/analysis` — новый контроллер-метод.**
`ADMIN`-only (тот же security-конфиг, что у `POST /verses/{id}/analyze` и
`analyze-all`). Тело — `{ verseIds: List<UUID> }`. Вызывает новый сервисный
метод (B3), возвращает `202 Accepted` с телом `{ verseIds: List<UUID> }` (id,
реально найденные и отправленные на анализ — не найденные/удалённые из
входного списка исключаются молча, аналогично B1).

**B3. `VerseAnalysisService.analyzeVerses(List<UUID> verseIds)`.** Загрузить
`Verse` по id (`findAllByIdInAndDeletedAtIsNull`, тот же метод, что в B1),
пропустить не найденные. **Не фильтровать по статусу** — в отличие от
`analyzeChapter`, здесь анализируются все переданные стихи безусловно, включая
уже `ANALYZED` (полная перезапись существующего анализа). Разбить результат на
чанки константного размера (новая константа, например `private static final int
ANALYSIS_CHUNK_SIZE = 20` — точное число не критично, ориентир: не больше, чем
самая крупная глава в текущем корпусе, чтобы не выйти за уже проверенный на
практике размер LLM-промпта; если есть способ быстро прикинуть реальный
максимум по данным — использовать его, иначе оставить как есть с TODO-
комментарием) и вызвать уже существующий приватный `runAnalysis(List<Verse>)`
на каждый чанк последовательно (метод сейчас `private` — сделать `private`
остаётся, чанкинг вызывается изнутри того же класса `VerseAnalysisService`,
новый публичный метод `analyzeVerses` просто добавляется рядом с
`analyze`/`analyzeChapter`). Вернуть список id реально загруженных (не
пропущенных) стихов.

**B4. DTO-классы.** `VersesBatchResponseDto`/`VerseBatchItemDto` — в том же
пакете, где остальные response-DTO контроллеров sangraha (`controller/dto/`
или аналогичный, найти по существующим `AnalyzeAllVersesResponse` и
соседним классам). `AnalyzeVersesRequest`/`AnalyzeVersesResponse` — аналогично,
`record`, по образцу уже существующего `AnalyzeAllVersesResponse`.

**B5. Тесты.**
- Юнит/интеграционный тест на B1: запрос с одним несуществующим и одним
  существующим id — в ответе только существующий, без ошибки; проверить, что
  `status` в ответе соответствует реальному статусу стиха (включая `DRAFT`).
- Юнит-тест на чанкинг (B3): список из `2.5 * ANALYSIS_CHUNK_SIZE` id — мок
  `runAnalysis`/LLM-клиента вызывается ровно 3 раза (или соответствующее
  число чанков), суммарно на все переданные стихи.
- Интеграционный тест на B2/B3: стих со статусом `ANALYZED` передаётся в
  `verseIds` — после вызова его `VerseAnalysis` перезаписан (например,
  сравнить `updatedAt`/содержимое до и после), не пропущен как «уже
  проанализирован».

---

## Критерии готовности

- [ ] B1: `GET /verse?id=...` возвращает `status` каждого стиха, не
      фильтрует по `ANALYZED`, отсутствующие id молча пропускаются
- [ ] B2–B3: `POST /verse/analysis` работает безусловно (включая повторный
      анализ `ANALYZED`), с чанкингом, `ADMIN`-only
- [ ] B4: DTO соответствуют `batch-verse-review.md`
- [ ] B5: все три теста проходят
