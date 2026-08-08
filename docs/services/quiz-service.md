# quiz-service

> Домен: Quiz Sessions — прохождение квизов пользователем
> Язык: **Java 21 + WebFlux (Reactor)**
> Модуль: `services/quiz-service`
> Порт: 8082

---

## 1. Архитектура и стек

Единый сервис для прохождения квизов всех типов (склонения, спряжения, лексика). Использует реактивный стек (WebFlux, R2DBC, ReactiveKafkaProducerTemplate) из-за интенсивного I/O.

Подробнее: [quiz-service-architecture.md](quiz-service/quiz-service-architecture.md)
### Хранение данных

| Хранилище | Что хранит | Зачем |
|---|---|---|
| PostgreSQL/R2DBC (схема `quiz`) | сессии (`quiz_session`), ответы (`quiz_answers`), вопросы сессии (`session_questions`), события Outbox | надёжность, возможность продолжить сессию, статистика |

## 2. Механика сессии

Жизненный цикл: IN_PROGRESS → COMPLETED. Эндпоинты:
- `start` — генерация вопросов через curriculum-service, сохранение сессии
- `resume` — восстановление из БД, генерация дистракторов на лету
- `answer` — проверка ответа, сохранение, публикация события
- `complete` — завершение сессии, публикация события
- `progress` — проверка незавершённой сессии

### Универсальный (curriculum-driven) compose-путь

`POST /api/v2/quiz/compose` — сессия собирается из топиков (`QuestComposeRequest{topics:[{topicCode,count}], userLocale}`, можно смешивать GRAMMAR и LEXICON). curriculum-service материализует вопросы (prompt+correctAnswer+distractors+payload); quiz-service сохраняет их в `session_questions` (опции фиксируются при старте, столбцы V14: answer_mode/correct_answer/options/payload/topic_code) и возвращает `ComposeQuizResponse`. Композиция и рендер — curriculum ([curriculum-session-composition.md](curriculum-session-composition.md)), quiz-service — оркестрация и хранение.

Полный жизненный цикл compose-сессии реализован через `ComposedSessionService` (резерв по `lessonId == null` в `QuizSessionService`): resume/retake — регидратация вопросов с опциями из `session_questions` (`ComposedQuestionMapper.toQuestionDto`, детерминированные id опций), answer — сравнение выбранной опции с `correctAnswer` (+ `QuizAnswer`, инкремент счёта, outbox `QuizAnsweredEvent`), complete — статус COMPLETED + outbox. Прогресс пишется в `quiz_item_score` по `(ItemType, quest_item.id)` через `QuestProgressTypes` (enum не расширялся, решение 2026-08). Реализация: `QuestComposeService`, `ComposedQuestionMapper`, `ComposedSessionService`, `ComposeSessionController`, `CurriculumClient`.

**MATCHING-ответы (DECLENSION_MATCH, инкремент 5):** рендер двумя колонками — строки слева из `payload.pairs` (row id = pairId), метки справа (case+number) из уникальных пар (optionType=MATCH_LABEL, caseType/numberType в опции). Ответ приходит `matchSubmissions: [{rowId, optionId}]` в `AnswerRequest` и проверяется сверкой каждой пары с эталонными парами payload; вопрос верен только при полном сопоставлении всех пар (бинарь, без partial credit, как и в остальных типах).

**v2 lesson (данные урока из curriculum-service):** `GET /api/v2/lessons/grammar/{topicCode}` (`LessonV2Controller` → `GrammarLessonV2Service`) — строит `GrammarLesson` из curriculum-данных топика (topic metadata + quest items с морфо-атрибутами, curriculum `GET /api/v2/curriculum/topics/{code}/lesson`) и прогресса из `quiz_item_score` (ключ `(ItemType, quest_item.id)`). Заменяет легаси v1-путь через curriculum-service (`GrammarProgressBuilder`), который оставлен только для старых content-уроков. Фронтенд grammar-lesson страницы переведён на v2.

### Прогресс-отбор — реализован без рефакторинга ключа

enum `ItemType` не расширялся (решение 2026-08). Quest-единицы отбираются и пишутся как
`(ItemType.DECLENSION_FORM | VOCABULARY_WORD, externalRefId=quest_item.id)` через
`QuestProgressTypes`; ref-id пространства не пересекаются, scoped-агрегации легаси не затронуты.
Отбор на compose — `QuizGenerator` по пулу топика + `QuestSelectionPlanner.takeRoundRobin`
(смешивание типов), затем compose по `itemIds`. Опция «чистого» ключа с кодами `QuestItemType`
(`itemType`→String) — отложена, см. §6 [curriculum-session-composition.md](curriculum-session-composition.md).

Параметр `statusFilter` (`start`/`start-or-resume`, бакетный отбор NEW/LEARNING/REVIEW для LessonPage) **реализован**: `@RequestParam statusFilter` в `QuizSessionController`, ветка в `QuizSessionService.startOrResumeSession`, отбор — `QuizStatusFilteredGenerator`, запросы `findInProgressByStatusFilter`/`findLearningItems`/`findReviewItems`, колонка `quiz_session.status_filter` (миграция V10). См. [quiz-generator-spec.md §3](quiz-service/quiz-generator-spec.md#3-quizgeneratorconfig--параметры-отбора) и §7 п.5.

Подробнее: [quiz-service-sessions.md](quiz-service/quiz-service-sessions.md)

## 3. Репозитории и хранение данных

Пять ReactiveCrudRepository: QuizSessionRepository, QuizAnswerRepository, SessionQuestionRepository, OutboxEventRepository, QuizItemScoreRepository (единая таблица прогресса `quiz.quiz_item_score`, architecture.md §3.6 — источник статуса NEW/LEARNING/MASTERED/REVIEW для LessonPage; не путать с on-the-fly расчётом successRate для колонки «Попытки», см. §7 ниже).

Подробнее (включая дистракторы и word score): [quiz-service-repositories.md](quiz-service/quiz-service-repositories.md)

## 4. Kafka и Outbox Pattern

Публикация только через Transactional Outbox Pattern. События: QuizAnsweredEvent, QuizSessionStatusChangedEvent.

Подробнее: [quiz-service-kafka.md](quiz-service/quiz-service-kafka.md)

## 5. Миграции БД

Стартовый файл V1__combined_schema.sql (схема `quiz`). Таблицы: quiz_session, quiz_answers, session_questions, outbox_events.

Подробнее: [quiz-service-migrations.md](quiz-service/quiz-service-migrations.md)

## 6. Интеграция с curriculum-service (ContentClient)

WebClient с методами generateQuizData, getDeclensionForms, getVocabularyWordsForLesson и др. Полный список: [quiz-service-architecture.md](quiz-service/quiz-service-architecture.md)

`generateQuizData` принимает `filterScope`/`filterCaseTypes`/`filterNumberTypes`/`filterCombinations` и прокидывает их как query-параметры в curriculum-service — scope pre-filter вопросов выполняется там (см. curriculum-service.md, quiz-declension.md §3.4); quiz-service такой фильтрации сам не выполняет.

## 7. Зависимости (build.gradle.kts)

spring-boot-webflux, spring-boot-r2dbc, r2dbc-postgresql, spring-kafka, flyway-core, postgresql (для Flyway), samskrtam-dtos(shared).