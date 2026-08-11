# quiz-service: Архитектура, стек и ключевые решения

> Часть спецификации quiz-service. Основной файл: `docs/services/quiz-service.md`

---

## 1. Описание и назначение

Единый сервис для прохождения квизов всех типов (склонения, спряжения, лексика). Обрабатывает жизненный цикл сессии: старт, ответы, завершение. После завершения публикует события в Kafka через Outbox Pattern.

Разделение ответственности:
- curriculum-service — что есть в квизах (данные, настройки)
- quiz-service — как пользователь их проходит (сессии, ответы, события)

## 2. Стек

WebFlux выбран осознанно из-за интенсивного I/O: одновременные обращения к curriculum-service, запись в Postgres, публикация в Kafka. Реактивный pipeline позволяет держать всё в одном неблокирующем потоке.

Следствие: весь стек реактивный:
- База данных: R2DBC (не JPA/Hibernate)
- Kafka: ReactiveKafkaProducerTemplate
- HTTP-клиент: WebClient
- Flyway: добавляется JDBC datasource только для миграций (стандартная практика для R2DBC)

## 3. Зависимости

build.gradle.kts:
- spring-boot-webflux
- spring-boot-r2dbc
- r2dbc-postgresql
- spring-kafka
- flyway-core
- postgresql (для Flyway)
- samskrtam-dtos (shared)

## 4. Интеграция с curriculum-service (CurriculumClient)

WebClient для взаимодействия с curriculum-service (API v2):
- `fetchTopicPool(String topicCode)` — лёгкий пул (id, itemType, progressTag) для прогресс-отбора
- `fetchQuestItems(UUID topicId, String itemType, int limit)` — случайная выборка материализованных вопросов
- `composeSession(List<QuestSessionTopicDto>, String userLocale)` — композиция сессии из отобранных itemIds
- `fetchTopicLesson(String topicCode)` — read-model урока (metadata + tagMetadata)
- `fetchLessons()` — список всех уроков
- `fetchParadigmPage(String topicCode, int index)` — страница парадигмы склонений

Resume/answer/complete читают вопросы из своей БД.

## 5. Вопросы сессии — материализуются при старте и персистятся

Curriculum-service материализует вопросы (prompt + correctAnswer + distractors + payload), quiz-service сохраняет их в `session_questions` при старте и далее читает только из БД. Опции фиксируются при старте, id опций детерминированы — resume воспроизводит те же опции.

## 6. Контракт вопроса для фронтенда — деванагари и перевод основы

Путь данных:
curriculum.quest_item.prompt/correct_answer/distractors → curriculum-service compose → quiz-service сохраняет в session_questions → оттуда отдаётся во фронтенд.

Затронутые компоненты:
- curriculum-service: QuestItem, compose endpoint
- quiz-service: SessionQuestion, ComposedQuestionMapper, миграция session_questions columns (V14)

AnswerResponse без изменений — перевод основы для варианта ответа не требуется.