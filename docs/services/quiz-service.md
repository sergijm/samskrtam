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
- `resume` — восстановление из БД
- `answer` — проверка ответа, сохранение, публикация события
- `complete` — завершение сессии, публикация события

### Универсальный (curriculum-driven) compose-путь

`POST /api/v2/quiz/compose` — сессия собирается из топиков (`QuestComposeRequest{topics:[{topicCode,count}], userLocale}`, можно смешивать GRAMMAR и LEXICON). curriculum-service материализует вопросы (prompt+correctAnswer+distractors+payload); quiz-service сохраняет их в `session_questions` (опции фиксируются при старте, столбцы V14: answer_mode/correct_answer/options/payload/topic_code) и возвращает `ComposeQuizResponse`. Композиция и рендер — curriculum ([curriculum-session-composition.md](curriculum-session-composition.md)), quiz-service — оркестрация и хранение.

Полный жизненный цикл реализован через `ComposedSessionService`: resume/retake — регидратация вопросов с опциями из `session_questions` (`ComposedQuestionMapper.toQuestionDto`, детерминированные id опций), answer — сравнение выбранной опции с `correctAnswer` (+ `QuizAnswer`, инкремент счёта, outbox `QuizAnsweredEvent`), complete — статус COMPLETED + outbox. Прогресс пишется в `quiz_item_score` через `QuestProgressTypes`. Реализация: `QuestComposeService`, `ComposedQuestionMapper`, `ComposedSessionService`, `ComposeSessionController`.

**MATCHING-ответы (DECLENSION_MATCH, инкремент 5):** рендер двумя колонками — строки слева из `payload.pairs` (row id = pairId), метки справа (case+number) из уникальных пар (optionType=MATCH_LABEL, caseType/numberType в опции). Ответ приходит `matchSubmissions: [{rowId, optionId}]` в `AnswerRequest` и проверяется сверкой каждой пары с эталонными парами payload; вопрос верен только при полном сопоставлении всех пар.

**v2 lesson (данные урока из curriculum-service):** `GET /api/v2/lessons/grammar/{topicCode}` (`LessonV2Controller` → `GrammarLessonV2Service`) — строит `GrammarLesson` из curriculum-данных топика (topic metadata + quest items с морфо-атрибутами, curriculum `GET /api/v2/curriculum/topics/{code}/lesson`) и прогресса из `quiz_item_score`.

### Прогресс-отбор

enum `ItemType` не расширялся (решение 2026-08). Quest-единицы отбираются и пишутся как
`(ItemType.DECLENSION_FORM | VOCABULARY_WORD, externalRefId=quest_item.id)` через
`QuestProgressTypes`; ref-id пространства не пересекаются, scoped-агрегации легаси не затронуты.
Отбор на compose — `QuizGenerator` по пулу топика + `QuestSelectionPlanner.takeRoundRobin`
(смешивание типов), затем compose по `itemIds`. Опция «чистого» ключа с кодами `QuestItemType`
(`itemType`→String) — отложена, см. §6 [curriculum-session-composition.md](curriculum-session-composition.md).

Параметр `progressTagSetId` (именованный срез прогресса NEW/LEARNING/MASTERED/DIFFICULT) **реализован**: отбор — `QuizProgressTagSetGenerator`, запросы `findNewItems`/`findLearningItems`/`findDifficultItems`/`findMasteredItems`, колонка `quiz_session.progress_tag_set_id`. См. [quiz-generator-spec.md §3](quiz-service/quiz-generator-spec.md#3-quizgeneratorconfig--параметры-отбора) и §7 п.5.

Подробнее: [quiz-service-sessions.md](quiz-service/quiz-service-sessions.md)

## 3. Репозитории и хранение данных

Пять ReactiveCrudRepository: QuizSessionRepository, QuizAnswerRepository, SessionQuestionRepository, OutboxEventRepository, QuizItemScoreRepository (единая таблица прогресса `quiz.quiz_item_score`, architecture.md §3.6 — источник статусов NEW/LEARNING/MASTERED + ортогонального DIFFICULT, из которых строятся прогресс-сеты для LessonPage; не путать с on-the-fly расчётом successRate для колонки «Попытки», см. §7 ниже).

Подробнее (включая дистракторы и word score): [quiz-service-repositories.md](quiz-service/quiz-service-repositories.md)

## 4. Kafka и Outbox Pattern

Публикация только через Transactional Outbox Pattern. События: QuizAnsweredEvent, QuizSessionStatusChangedEvent.

Подробнее: [quiz-service-kafka.md](quiz-service/quiz-service-kafka.md)

## 5. Миграции БД

Стартовый файл V1__combined_schema.sql (схема `quiz`). Таблицы: quiz_session, quiz_answers, session_questions, outbox_events.

Подробнее: [quiz-service-migrations.md](quiz-service/quiz-service-migrations.md)

## 6. Интеграция с curriculum-service

WebClient `CurriculumClient` — все запросы к curriculum-service (API v2): `fetchTopicPool`, `fetchQuestItems`, `composeSession`, `fetchTopicLesson`, `fetchLessons`, `fetchParadigmPage`. Полный список: [quiz-service-architecture.md](quiz-service/quiz-service-architecture.md)

## 7. Зависимости (build.gradle.kts)

spring-boot-webflux, spring-boot-r2dbc, r2dbc-postgresql, spring-kafka, flyway-core, postgresql (для Flyway), samskrtam-dtos(shared).