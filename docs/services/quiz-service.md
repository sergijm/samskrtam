# quiz-service

> Домен: Quiz Sessions — прохождение квизов пользователем
> Язык: **Java 21 + WebFlux (Reactor)**
> Модуль: `services/quiz-service`
> Порт: 8082
> Status: **UPDATED**

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
- `start` — генерация вопросов через content-service, сохранение сессии
- `resume` — восстановление из БД, генерация дистракторов на лету
- `answer` — проверка ответа, сохранение, публикация события
- `complete` — завершение сессии, публикация события
- `progress` — проверка незавершённой сессии

> ⚠️ Параметр `statusFilter` (`start`/`start-or-resume`, бакетный отбор NEW/LEARNING/REVIEW для LessonPage) описан в контракте, но не реализован — см. [quiz-generator-spec.md §3](quiz-service/quiz-generator-spec.md#3-quizgeneratorconfig--параметры-отбора) (предупреждение) и §7 п.5 (DoD).

Подробнее: [quiz-service-sessions.md](quiz-service/quiz-service-sessions.md)

## 3. Репозитории и хранение данных

Пять ReactiveCrudRepository: QuizSessionRepository, QuizAnswerRepository, SessionQuestionRepository, OutboxEventRepository, QuizItemScoreRepository (единая таблица прогресса `quiz.quiz_item_score`, ADR-007 — источник статуса NEW/LEARNING/MASTERED/REVIEW для LessonPage; не путать с on-the-fly расчётом successRate для колонки «Попытки», см. §7 ниже).

Подробнее (включая дистракторы и word score): [quiz-service-repositories.md](quiz-service/quiz-service-repositories.md)

## 4. Kafka и Outbox Pattern

Публикация только через Transactional Outbox Pattern. События: QuizAnsweredEvent, QuizSessionStatusChangedEvent.

Подробнее: [quiz-service-kafka.md](quiz-service/quiz-service-kafka.md)

## 5. Миграции БД

Стартовый файл V1__combined_schema.sql (схема `quiz`). Таблицы: quiz_session, quiz_answers, session_questions, outbox_events.

Подробнее: [quiz-service-migrations.md](quiz-service/quiz-service-migrations.md)

## 6. Интеграция с content-service (ContentClient)

WebClient с методами generateQuizData, getDeclensionForms, getVocabularyWordsForLesson и др. Полный список: [quiz-service-architecture.md](quiz-service/quiz-service-architecture.md)

**ИЗМЕНЕНО:** `generateQuizData` дополнительно принимает `filterScope`/`filterCaseTypes`/`filterNumberTypes`/`filterCombinations` и прокидывает их как query-параметры в content-service — scope pre-filter вопросов теперь выполняется там (см. content-service.md, quiz-declension.md §3.4), quiz-service больше не содержит `SessionCreationService.applyScopeFilter`.

## 7. Зависимости (build.gradle.kts)

spring-boot-webflux, spring-boot-r2dbc, r2dbc-postgresql, spring-kafka, flyway-core, postgresql (для Flyway), samskrtam-dtos(shared).