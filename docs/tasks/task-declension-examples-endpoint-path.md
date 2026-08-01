# Задачи: изменение контракта эндпоинта «Примеры» (вкладка DeclensionExamplesTab)

> Оркестратор: Агент 0. Контракты: Агент 6 (уже обновлены — см.
> `docs/services/content-service.md` §12, `docs/services/content-service/declension-examples.md`,
> `docs/frontend/pages/grammar-lesson-page.md` §2.2а, `docs/openapi/content/content-api.yaml`,
> `docs/openapi/content/schemas/content.yaml#DeclensionExamplesResponseDto` — это входной
> контракт для задач ниже).
>
> **Суть изменения:** путь эндпоинта `GET /content/public/lessons/{slug}/declension-paradigms/examples?index=N`
> заменён на `GET /content/public/lessons/{slug}/examples` — без query-параметра `index`.
> Раньше эндпоинт резолвил один конкретный `stem` шага карусели «Парадигмы» по `(slug, index)`.
> Теперь он резолвит словоизменительный класс `(vowelType, gender)` только по `slug` урока
> (все стемы одного урока склонений принадлежат одному классу) и отдаёт примеры сразу по
> **всем** ячейкам `(caseType, numberType)` этого класса за один запрос — постраничности
> больше нет.
>
> Задачи разбиты на шаги ~30 сек работы модели (DeepSeek V4 Flash / Qwen3 Coder 30B A3B
> Instruct) — каждый шаг самодостаточен, ссылается на конкретный файл/раздел контракта.

---

## Агент 2 — Backend (content-service)

**B1. Путь контроллера.** В контроллере, обслуживающем
`GET /content/public/lessons/{slug}/declension-paradigms/examples`
(искать по `operationId: getDeclensionExamplesForLesson` / классу, использующему
`DeclensionExamplesService`), изменить маппинг пути на
`/content/public/lessons/{slug}/examples`. Убрать `@RequestParam`/аргумент `index`
из метода контроллера и из сигнатуры вызываемого сервисного метода.

**B2. Резолвинг класса вместо стема.** В сервисе (`DeclensionExamplesService` или
одноимённый), в методе, который раньше принимал `(slug, index)`: убрать резолвинг
конкретного `DeclensionStem` по индексу. Вместо этого загрузить список стемов урока
тем же репозиторием/методом, что использует `getDeclensionParadigmForLesson`
(404 `LESSON_NOT_FOUND`, если урок не найден или пуст), и взять `vowelType`/`gender`
первого стема по той же стабильной сортировке — см.
`docs/services/content-service/declension-examples.md`, шаг 1 (актуальная версия).

**B3. Полный набор ячеек.** В том же методе — убрать чтение ячеек из
`DeclensionForm[]` конкретного стема (шаг 2 старой версии). Вместо этого использовать
фиксированный полный список `(caseType, numberType)` — все значения `CaseType` ×
`NumberType` (см. `docs/services/content-service/declension-examples.md`, шаг 2
актуальной версии) — как набор ячеек для поиска в `declension_example_groups` и для
батч-запроса в sangraha-service (шаги 3–5 логики не меняются, входные параметры
`vowelType`/`gender` для них теперь берутся из B2, а не из `stem`).

**B4. Проверить неиспользуемые импорты/поля.** После B1–B3 убедиться, что параметр
`index`/`ParadigmIndexQueryParam` и любые ссылки на конкретный `stem` (кроме
`vowelType`/`gender`) в этом методе/контроллере больше не используются — удалить
мёртвый код.

---

## Агент 3 — Frontend (GrammarLessonPage / DeclensionExamplesTab)

**F1. API-клиент.** В `frontend/src/api/lessonApi.ts` — в функции, вызывающей
`GET /content/public/lessons/{slug}/declension-paradigms/examples` (сигнатура
включает `index`), изменить URL на
`/api/v1/content/public/lessons/${slug}/examples` и убрать параметр/query `index`
из сигнатуры функции и из запроса.

**F2. Хук.** В `frontend/src/hooks/useLessons.ts` (или где определён
`useDeclensionExamples`) — изменить сигнатуру `useDeclensionExamples(slug)` (убрать
`currentIndex`), убрать `currentIndex` из ключа React Query
(`['declension-examples', slug]`).

**F3. Компонент вкладки.** В `frontend/src/components/lesson/DeclensionExamplesPanel.tsx`
(или странице `GrammarLessonPage.tsx`, где используется этот компонент) — убрать
локальный `currentIndex` (useState), панель навигации «← счётчик →» и синхронизацию
с шагами карусели «Парадигмы» для этой вкладки. Вкладка теперь рендерит сразу все
группы `groups[]` из ответа одним списком (порядок — `CASE_TYPES` → `NUMBER_TYPES`,
см. `docs/frontend/pages/grammar-lesson-page.md` §2.2а). Убрать использование/проброс
`totalCount` применительно к этой вкладке.

**F4. Заголовок вкладки.** Убрать рендер `stemIast`/`stemDevanagari`/перевода стема в
заголовке DeclensionExamplesPanel (в ответе `DeclensionExamplesResponseDto` этих полей
больше нет — только `groups[]` с `caseType`/`numberType`/`examples`). Заменить на
заголовок урока (`titleRu`/`titleEn`, тот же источник данных, что заголовок страницы
`GrammarLessonPage.tsx`).

**F5. Типы.** В `frontend/src/types/content-dtos.d.ts` — проверить/обновить тип
`DeclensionExamplesResponseDto` (или как он там называется) под актуальную схему
`docs/openapi/content/schemas/content.yaml#DeclensionExamplesResponseDto`: только
`groups: { caseType, numberType, examples[] }[]`, без полей стема на верхнем уровне.

---

## Агент 4 — Testing (после B1–B4, F1–F5)

**T1.** Обновить/добавить интеграционный тест content-service на
`GET /content/public/lessons/{slug}/examples`: без `index` в запросе, проверить, что
ответ содержит группы по всем ячейкам класса урока (не только по одному стему), и что
запрос с несуществующим `slug` даёт 404.
