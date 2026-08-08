# Миграция quiz-service с content-service на curriculum-service v2

> Статус: **план** (реализация — отдельные PR по этапам).
> Задача: удалить content-service и класс `ContentClient`, полностью перевести quiz-service на curriculum-service v2.
> Связанные: [quiz-service-architecture.md](./quiz-service-architecture.md) · [curriculum-session-composition.md](../curriculum-session-composition.md) · [curriculum-quest-items.md](../curriculum-quest-items.md) · [quest-engine.md](../quest-engine.md)
> Лимит документа: 350 строк.

---

## 0. Цель и границы

content-service удалён из состава (connection refused на 8081). `ContentClient`
протоколирует мертвый транспорт к `content-service` v1. Необходимо убрать зависимость у
quiz-service и закрыть источники данных через curriculum-service v2.

**Принцип (архитектурное решение 2026-08):** понятия **lesson в системе нет** — остаётся
только **topic** (`curriculum.topic`, код как slug). Любые пережитки «урока» в quiz-service
(lessonId, lessonType, lesson-названия/слаги, LessonItemResponse, GrammarLesson,
VocabularyLesson) переводятся на topic-семантику. `quiz_session.lesson_id`/`lesson_type`
сохраняются только как обратно-совместимые nullable-колонки для старых записей; новые
сессии их больше не заполняют (compose-форма — `lessonId == null`).

**Не входит в периметр:** наполнение данных (lexeme/lexeme_morphology), вопрос о судьбе
старой v1-реализации DECLENSION_FORM в curriculum-service, решение о налиичности колонок.

---

## 1. Инвентаризация зависимостей

`ContentClient` (13 методов, base `/api/v1/content/**`) используется в 8 классах +
2 мёртвых поля. Полная карта:

| # | Метод ContentClient | Потребители (файл:строка) | Назначение |
|---|---|---|---|
| M1 | generateQuizData (all 3 overloads) | SessionCreationService.java:48,77,113 | генерация вопросов сессии |
| M2 | getLessonItem(id) | QuizDataAssembler:39, SessionHistoryPaginationService:70, UserSessionService:99 | заголовок урока в StartOrResumeResponse/QuizSummaryDTO |
| M3 | getLessonItemBySlug | LessonService:38,53,74; GrammarProgressBuilder:43 | резолв slug→lessonId |
| M4 | getVocabularyWordsForLesson | LessonService:40 | список слов vocab-урока |
| M5 | getVocabularyWordById | LessonService:55 | слово по id |
| M6 | getQuizzesByCategory | LessonService:65 | список уроков по типу |
| M7 | getDeclensionForms | DeclensionOptionGeneratorService:31,95; GrammarProgressBuilder:45 | формы для опций/грамматики |
| M8 | getDeclensionStemsForLesson | GrammarProgressBuilder:45 | основы урока |
| M9 | getCaseEndingsForLesson | GrammarProgressBuilder:59 | окончания урока |
| M10 | getVocabularyWordIdsForLesson | — (не вызывается) | мёртвый |
| M11 | getCaseEndingsByVowelType | — (не вызывается) | мёртвый |

**Мёртвые поля для немедленного удаления:**
- `SessionOperationsService.java:55` — `contentClient` инжектится, но нигде не вызывается.
- `QuizSessionMapper.java:21` + `import ...ContentClient` (строка 10) — не используется.

---

## 2. Фаза 0 — чистка неиспользуемого (маленький PR, без data)

Удалить:
- поле `contentClient` (`SessionOperationsService.java:55`);
- поле+импорт `QuizSessionMapper.java` (10, 21);
- комментарии с `{@link ContentClient}` в `QuizGenerator.java` (24, 48) и javadoc в `CurriculumClient.java:27`.

Компиляция и тесты не затрагиваются (нет вызовов). Делается первым делом, безопасно.

---

## 3. Фаза 1 — Core: запуск сессий через compose (без новых данных)

**Статус: реализовано.** `POST /api/v1/quiz/{slug}/sessions/start` и `/start-or-resume`
принимают опциональный `topicCode` (+`count`); при его наличии маршрутизируются на
`QuizSessionService.startOrResumeSessionByTopic` → `QuestComposeService.compose` и маппятся
в `StartOrResumeResponse` (lesson-поля null). Компаньон: принцип topic-only (§0) — /start
переключён на topic. Легаси lessonId-путь сохранён как необязательная ветка.

### Проблема M1: `SessionCreationService` строит сессию (plain/filter/status-filtered)
из `GeneratedQuizData`, которое отдаёт content-service on-the-fly. В v2 генерации «на
лету» нет — только материализованные строки `curriculum.quest_item` + `compose`.

**Решение:** это уже реализовано для compose-пути (`QuestComposeService`). Командуем
порядок: для plain/dfiltered/status-filtered сессий перейти на тот же самый механизм —
`CurriculumClient.fetchTopicPool(topicCode)` → `QuizGenerator.generate` (крутило) →
`composeSession(itemIds)`. Создаётся как `ComposeQuizResponse`, а не `StartOrResumeResponse`
там, где раньше былstart.

**Нюансы, требующие решения:**
1. Отсутствие `itemType` в композ-контракте — при выборе из пула фильтр по типу делается
   локально (фильтр `TopicQuestPoolController` отдаёт `(id,itemType)`), затем отобранные
   id передаются в compose.
2. Деление «по count на тип» — `QuizSelectionPlanner.takeRoundRobin` уже интерлейсит типы.

### Непосредственные действия
- `SessionCreationService` → заменить `contentClient.generateQuizData` на вызов
  `CurriculumClient.composeSession` (через обёртку QuestComposeService или новый метод).
- `SessionFactory.createSession/createFilteredSession/createStatusFilteredSession` перейти
  на compose-форму (`createComposedSession` + session_questions из compose-ответа).

---

## 4. Фаза 2 — Уроки/история (M2,M3,M6)

### Проблема M2/M3/M6: 
### Решение: 
- `getLessonItem(id)`/`getLessonItemBySlug(slug)` → `CurriculumClient.fetchTopicLesson(topicCode)`
  (`GetStudent /api/v2/curriculum/topics/{code}/lesson`) → `TopicLessonDto`. Маппинг
  `lesson→TopicLessonDto` в `LessonItemResponse` (обёртка-маппер в quiz-service).
- `getQuizzesByCategory(category)` → `CurriculumClient.fetchLessons()`
  (`Get /api/v2/curriculum/lessons`) → `TopicLessonSummaryDto`, фильтр по коду категории.

**Bloque:** `LessonItemResponse` содержит поля `description/difficulty/wordCount`, которых
нет в `TopicLessonSummaryDto`. Решение: сузить DTO ответа quiz-service либо добавить поля
в curriculum. Пока mapping — только доступные поля.

---

## 5. Фаза 3 — Vocabulary/лексика (M4,M5,M10)

### Проблема: **в v2 НЕТ vocabulary-эндпоинтов.**
- `getVocabularyWordsForLesson` (M4), `getVocabularyWordById` (M5), `getVocabularyWordIdsForLesson` (M10).
- В v2 только метаданные топиков (`TopicLessonSummaryDto`) и лексии-дашборд без полных слов.

### Решение (требует доработки curriculum-service):
Добавить в curriculum-service v2:
1. `GET /api/v2/curriculum/topics/{code}/vocabulary-words` → `List<VocabularyWordDto>`
   (по сути отражение уже имеющейся модели `curriculum.lexeme` + `lexical_topic_binding`).
2. `GET /api/v2/curriculum/vocabulary/words/{wordId}` → `VocabularyWordDto`.
3. `GET /api/v2/curriculum/lessons` — расширить `TopicLessonSummaryDto` (description/difficulty/wordCount) если требуется фронту.

Это отдельный PR в curriculum-service; quiz-service просто переключается на новые методы
`CurriculumClient`.

---

## 6. Фаза 4 — Склонение/грамматика (M7,M8,M9)

### Проблема
`GrammarProgressBuilder` и `DeclensionOptionGeneratorService` тянут stems/forms/case-endings
для **старого grammar-progress view**.

### Решение: уже есть заменение — `GrammarLessonV2Service` (v2 lesson view строит
`GrammarLesson` из `GET /api/v2/curriculum/topics/{code}/lesson`). Легаси в1-путь через
`GrammarProgressBuilder` оставить только пока работают старые content-уроки; при удалении
speak payload удалить `GrammarProgressBuilder` целиком. `DeclensionOptionGeneratorService`
переводится на **материализованные** `quest_item` (опиц расчитывается из `quest_item.payload`
и `distractors` вместо запроса форм на лету).

---

## 7. Порядок и зависимость веток

```
Фаза 0 (чистка)      — отдельный маленький PR, деплой сразу
Фаза 1 (core compose) — PR quiz-service, не блокируется данными
Фаза 2 (lessons)      — PR quiz-service + curriculum (mapping, опц. поля)
Фаза 3 (vocabulary)   — PR curriculum-service (новые эндпоинты) + quiz-service
Фаза 4 (declension)   — PR quiz-service (deprecate GrammarProgressBuilder)
Фаза 5 (удаление)     — DELETE ContentClient class, content: service: url из yml/env
```

Каждый этап содержит тесты (по conv/testing → §2. именование). JaCoCo ≥80% сервисного слоя.

---

## 8. Открытые вопросы

1. (F0+F1) Чистка неиспользуемого и переключение `/start` на topic — **выполнено**;
   `ContentClient` ещё жив и используется rest-классами (Фазы 2–5).
2. Поля `description/difficulty/wordCount` LessonItemResponse: убрать из фронт-контракта или
   добавить в curriculum `TopicLesson*Dto`.
2. Опция «чистого» ключа `quiz_item_score` с кодами `QuestItemType` (вместо `(ItemType,quest_item.id)`)
   — отложена, не переносится.
3. on-the-fly генерация дистракторов для `DeclensionOptionGeneratorService` в композе —
   полностью ли нивелируется материализованными `distractors` из `quest_item`.
4. **FREE_TEXT в композе**: `ComposedQuestionMapper.buildOptionsJson` для `answerMode=FREE_TEXT`
   теперь возвращает пустой массив опций (корректный ответ не утекает в опции), `questionType`
   маппится в `FREE_TEXT`; фронт рисует текстовое поле (`QuizFreeTextPanel`) и шлёт
   `selectedFormIast`. Верификация по `selectedFormIast`/`correctAnswer` уже была
   (`ComposedSessionService#resolveSelectedText`).