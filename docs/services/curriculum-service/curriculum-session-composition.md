# Curriculum Quiz Session Composition (v2)

> Домен: сессия квиза · Состав/жизненный цикл: **quiz-service** · Материал вопросов: **curriculum-service** (`/api/v2/curriculum/quest-items`)
> Связанные файлы: [curriculum-service.md](../curriculum-service.md) · [curriculum-quest-items.md](curriculum-quest-items.md) ·
> [quiz-service.md](../quiz-service.md) · [quiz-generator-spec.md](../quiz-service/quiz-generator-spec.md)

---

## 1. Назначение и границы

Сессия квиза собирается из запрошенных топиков: последовательность готовых, уже
материализованных вопросов (prompt, correctAnswer, distractors, payload) — рендер и
дистракторы генерируются batch-генератором офлайн (см. curriculum-quest-items.md §4), в
рантайме ничего не вычисляется.

**Важно (по факту кода):** в curriculum-service **нет** эндпоинтов вида
`/api/v2/curriculum/sessions/**` и нет `POST …/sessions/compose`, нет и контроллера
`TopicQuestPoolController` / `GET /api/v2/curriculum/topics/{topicCode}/quest-items`. Состав и
жизненный цикл сессии реализованы в **quiz-service** (`POST /api/v2/quiz/compose`, resume/retake,
answer, complete — см. quiz-service.md §2). curriculum-service предоставляет только готовые
вопросы через `/api/v2/curriculum/quest-items` (`GET ?topicId&itemType&limit`,
`POST /select`, `POST /regenerate`).

_Архитектурное решение о разделении ответственности вынесено в единый раздел
«Архитектурные решения (ADR)» файла curriculum-service.md (ADR-5)._

Топиков много и они двух доменов (GRAMMAR — десятки, LEXICON — десятки, см.
curriculum-service.md §V3 `topic.domain`). Сессия может смешивать оба домена и несколько
топиков сразу — для контракта нет разницы между доменами и типами вопросов.

---

## 2. Поток сборки сессии (реальный)

Целевой и реализованный поток (compose/lifecycle — в quiz-service):

1. quiz-service получает от фронтенда сессию как набор `(topicCode, count)` + опц. `statusFilter`.
2. quiz-service запрашивает у curriculum-service **готовые вопросы** топика —
   `GET /api/v2/curriculum/quest-items?topicId=&itemType=&limit=` (либо
   `POST /api/v2/curriculum/quest-items/select` по прогресс-тегам) — и применяет свой
   прогресс-отбор (`quiz_item_score`, SRS: due/new/reserve).
3. quiz-service вызывает свой `POST /api/v2/quiz/compose` — compose+persist+render по
   выбранным единицам (случайный порядок, фиксация вопроса с дистракторами при старте).
4. quiz-service сохраняет вопросы в `session_questions` (фиксируются при старте, одинаковы
   при resume) и ведёт жизненный цикл (answer/complete), записывая `quiz_item_score`.

**Реализовано (инкременты 1–5):**
- композиция по `(topicCode, count)` из GRAMMAR-топиков с материализованными вопросами (в quiz-service);
- режим прогресс-отбора: quiz-service берёт пул из curriculum-service (`/quest-items`) и
  выбирает count единиц по `externalRefId=quest_item.id` под legacy `ItemType` (см. §4);
- `POST /api/v2/quiz/compose` — compose+persist+render; resume/retake (регидратация опций),
  answer (сравнение с `correctAnswer`), complete; запись `quiz_item_score` (см. quiz-service.md §2);
- рендеринг DECLENSION_MATCH (строки слева из `payload.pairs`, метки справа из уникальных пар),
  проверка `matchSubmissions` (rowId→optionId) — верно только при полном совпадении всех пар.

**НЕ реализовано (честный статус):** материализация вопросов для LEXICON-топиков
(batch-генератор покрывает только семейство DECLENSION_FORM, curriculum-quest-items.md §4) —
пока LEXICON-топик с пустым пулом даёт ошибку при сборке в quiz-service.

---

## 3. Реализация (классы)

- quiz-service: `QuizComposerController` — `POST /api/v2/quiz/compose`;
  `ComposedSessionService` — compose/lifecycle; `QuizItemScoreService.upsertScore` — запись
  прогресса; `QuizGenerator` — прогресс-отбор по `quiz_item_score`.
- curriculum-service: `QuestItemController` (`/api/v2/curriculum/quest-items`) — чтение и
  регенерация; `QuestItemRepository.findRandomByTopicId` / `selectByTopic` — выборка пула.

---

## 4. Ключ прогресса

_Архитектурное решение вынесено в единый раздел «Архитектурные решения (ADR)» файла
curriculum-service.md (ADR-6)._

`quiz_item_score` ключуется `(userId, itemType, externalRefId)`. Прогресс отбирается по
`externalRefId=quest_item.id` под legacy `ItemType` через `QuestProgressTypes`
(маппинг кода→`ItemType` не рефакторен). Семантика прогресса склонений изменилась: стал на
конкретную материализованную единицу (был на связку case/number).

Проверка ответа: choice — сравнение выбранной опции по id с `correctAnswer`; FREE_TEXT —
сравнение текста `selectedFormIast`; MATCHING — сверка `matchSubmissions` (rowId→optionId) с
эталонными парами payload; верно только при полном совпадении всех пар.

---

## 5. Открытые вопросы

- Чистый ключ прогресса с кодами `QuestItemType` — рефакторинг `itemType`→String (отложено, см. ADR-6).
- Наполнение `curriculum.lexeme`/`lexeme_morphology` реальными данными для склоняемых лемм —
  вне периметра, отдельный content pipeline.
- Материализация вопросов для LEXICON-топиков — будущая задача.
