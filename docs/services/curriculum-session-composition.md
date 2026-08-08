# Curriculum Quiz Session Composition (v2)

> Домен: сессия квиза · Сервис: curriculum-service · API-версия: **v2** (`/api/v2/curriculum/sessions/**`)
> Связанные файлы: [curriculum-service.md](./curriculum-service.md) · [curriculum-quest-items.md](./curriculum-quest-items.md) ·
> [quiz-service.md](./quiz-service.md) · [quiz-generator-spec.md](./quiz-service/quiz-generator-spec.md)

---

## 1. Назначение и границы

Один эндпоинт собирает **последовательность вопросов сессии** из запрошенных топиков.
Вызывающий передаёт список топиков и количество вопросов на каждый; curriculum-service
возвращает готовую последовательность в случайном порядке. Каждый вопрос уже
материализован (prompt, correctAnswer, distractors, payload) — рендер и дистракторы
генерируются batch-генератором офлайн (см. curriculum-quest-items.md §4), в рантайме
ничего не вычисляется.

**Разделение ответственности (архитектурное решение 2026-08):**
- curriculum-service — **что спросить**: композиция последовательности, рендер вопросов, дистракторы.
- quiz-service — **как проходит пользователь**: отбор с учётом прогресса (due/new/reserve,
  statusFilter), жизненный цикл сессии, `quiz_item_score`, outbox-события.

Топиков много и они двух доменов (GRAMMAR — десятки, LEXICON — десятки, см.
curriculum-service.md §V3 `topic.domain`). Сессия может смешивать оба домена и несколько
топиков сразу — для этого контракта нет разницы между доменами и типами вопросов.

---

## 2. Эндпоинт

POST `/api/v2/curriculum/sessions/compose`

### 2.1. Запрос

Тело:

- topics — обязательный список, непустой; каждый элемент (два взаимоисключающих режима):
  - topicCode — уникальный код топика (`curriculum.topic.code`, например `a-stem-masc`)
  - count — количество вопросов из этого топика (целое ≥ 1; значения < 1 игнорируются) —
    режим случайной выборки
  - itemIds — список id материализованных вопросов (`quest_item.id`), которые надо вернуть
    **ровно этими**; при непустом itemIds имеет приоритет над count (режим «по отобранным
    id», используется когда quiz-service уже выбрал единицы по прогрессу)
- userLocale — опциональный, хинт локализации в payload

### 2.2. Ответ

Тело:

- items — список готовых вопросов в случайном порядке (перемешивание выполняется после
  сборки всех топиков, номера проставляются после перемешивания); каждый элемент:
  - questionNumber — позиция 1..N в итоговой последовательности
  - topicCode — топик, из которого взят вопрос
  - item — материализованный вопрос (см. curriculum-quest-items.md §6: id, itemType,
    answerMode, prompt, correctAnswer, distractors, payload)

### 2.3. Ошибки

- 400 — пустой список topics; или топик не имеет ни одного материализованного вопроса
  (для GRAMMAR-топиков вопросы появляются после batch-генерации, для LEXICON-топиков —
  после появления их материализации, см. §4); или в режиме itemIds какой-то id не найден /
  не принадлежит топику
- 404 — неизвестный topicCode

### 2.4. Семантика выборки

В режиме **count** на каждый топик берётся случайная выборка из **всех** материализованных
типов вопросов этого топика (пропорционально пулу): типы не указываются в запросе и не
смешиваются с приоритетами. Если пул меньше запрошенного count — возвращаются все имеющиеся.
Это инвариант: композиция не ветвится по domain/типу вопроса, новый тип или лексический
топик включаются без изменения кода эндпоинта.

В режиме **itemIds** возвращаются ровно запрошенные единицы (проверяется их существование
и принадлежность топику — ошибка 400 при несоответствии). В обоих режимах итоговый порядок —
случайный, номера проставляются после перемешивания. quiz-service использует режим itemIds
для прогресс-отбора (§5).

---

## 3. Реализация

- `questsession/controller/QuizSessionComposerController` — POST `/api/v2/curriculum/sessions/compose`
- `questsession/service/QuizSessionComposerService` — сборка и перемешивание
- `questsession/dto/QuizSessionComposeRequest` / `TopicItemSpec` / `QuizSessionComposeResponse` / `ComposedQuizItemDto`
- `QuestItemRepository.findRandomByTopicId(topicId, limit)` — случайная выборка по всем типам;
  `findByTopicId(topicId)` — полный пул (под прогресс-отбор quiz-service, см. §5);
  `findAllById(ids)` — выборка по точным id (режим itemIds)

---

## 4. Статус и известные ограничения

Реализовано (инкремент 1):
- композиция по `(topicCode, count)` из GRAMMAR-топиков с материализованными вопросами
- случайный порядок, фиксация вопроса вместе с дистракторами

Реализовано (инкремент 2, curriculum-часть):
- режим `itemIds` — композиция ровно из переданных `quest_item.id` (для прогресс-отбора quiz-service)

Реализовано (инкремент 3, quiz-часть):
- `POST /api/v2/quiz/compose` — compose+persist+render по случайной выборке (byCount)
- полный жизненный цикл compose-сессии: resume/retake (регидратация опций), answer
  (сравнение с `correctAnswer`), complete (см. quiz-service.md §2); запись `quiz_item_score`
  отложена (решение 2026-08)

Реализовано (инкремент 4, прогресс-отбор):
- эндпоинт пула: `GET /api/v2/curriculum/topics/{topicCode}/quest-items` → `[{id, itemType}]`
  (`TopicQuestPoolController`, `QuestItemRepository.findByTopicId`)
- quiz-service отбирает по прогрессу (`QuizGenerator`: due/new/reserve на `quiz_item_score`
  по `externalRefId=quest_item.id`) и вызывает compose в режиме `itemIds` (§2.1);
  запись прогресса на ответ — `ComposedSessionService` → `QuizItemScoreService.upsertScore`
  (маппинг кода→`ItemType` в `QuestProgressTypes`, ключ прогресса не рефакторен, см. §6 №1)

НЕ реализовано (честный статус):
- материализация вопросов для LEXICON-топиков (batch-генератор покрывает только семейство
  DECLENSION_FORM, curriculum-quest-items.md §4) — пока LEXICON-топик с пустым пулом даёт 400

Реализовано (инкремент 5, MATCHING-ответы):
- рендеринг DECLENSION_MATCH: строки слева из `payload.pairs` (row id = pairId), метки справа
  (case+number) из уникальных пар с optionType=MATCH_LABEL и caseType/numberType в опции
- проверка ответа `matchSubmissions` (rowId→optionId): каждая пара сверяется с эталонными
  парами payload; вопрос верен только если сопоставлены ВСЕ пары без ошибок; прогресс пишется
  как обычно через `QuestProgressTypes`

---

## 5. План интеграции с quiz-service (следующий инкремент)

Целевой поток (контракт уже согласован):
1. quiz-service получает от фронтенда сессию как набор `(topicCode, count)` + опц. `statusFilter`.
2. quiz-service запрашивает у curriculum-service **полный пул** топика
   (`QuestItemRepository.findByTopicId`) и применяет свой прогресс-отбор (`quiz_item_score`,
   SRS): выбирает count единиц на топик с учётом due/new/reserve.
3. quiz-service вызывает `POST /sessions/compose` с отобранными единицами в режиме **itemIds**
   и получает готовые вопросы с дистракторами.
4. quiz-service сохраняет вопросы с дистракторами в `session_questions` (фиксируются при
   старте, одинаковы при resume).

**Статус:** шаги 1–4 выполнены: compose/lifecycle в quiz-service (см. quiz-service.md §2) + прогресс-отбор
(эндпоинт пула + `itemIds`-compose + запись прогресса). Ключ прогресса не рефакторен — прогресс отбирается по
`externalRefId=quest_item.id` под legacy `ItemType` через `QuestProgressTypes` (см. §6 №1).

---

## 6. Открытые вопросы

- Идентичность прогресса: `quiz_item_score` ключуется `(userId, itemType, externalRefId)`.
  Текущее решение (2026-08): enum `ItemType` НЕ расширялся; quest-единицы пишутся как
  `(ItemType.DECLENSION_FORM | VOCABULARY_WORD, quest_item.id)` через `QuestProgressTypes`.
  Внешние ref-id пространства (case_ending_id/vocabulary_word_id vs quest_item.id) не пересекаются,
  поэтому scoped-агрегации не мешают. Семантика прогресса склонений изменилась: стал на конкретную
  материализованную единицу (был на связку case/number). Если позже нужен «чистый» ключ с кодами
  QuestItemType — рефакторинг `itemType`→String (отложено).
- Проверка ответа на новые типы: для choice — реализовано (сравнение выбранной опции по id с
  `correctAnswer`), FREE_TEXT — сравнение текста `selectedFormIast`; **MATCHING** — реализовано
  (инкремент 5): сверка `matchSubmissions` (rowId→optionId) с эталонными парами payload; верно
  только при полном совпадении всех пар.
