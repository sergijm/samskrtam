# quiz-service

> Домен: Quiz Sessions — прохождение квизов пользователем
> Язык: **Java 21 + WebFlux (Reactor)**
> Модуль: `services/quiz-service`
> Порт: 8082
> Status: **UPDATED**

---

## 1. Описание

Единый сервис для прохождения квизов всех типов: склонения, спряжения, лексика. Обрабатывает жизненный цикл сессии: старт, ответы, завершение. Детальная история ответов сохраняется в базе данных `quiz-service`. После завершения сессии публикует обогащенное событие в Kafka **с использованием Outbox Pattern**. Данные (вопросы, варианты, слова) получает от `content-service` через реактивный HTTP-клиент.

Разделение ответственности:
- **content-service** — что есть в квизах (данные, настройки)
- **quiz-service** — как пользователь их проходит (сессии, ответы, события)

### Стек

WebFlux выбран осознанно: quiz-service интенсивно работает с I/O одновременно — обращается к content-service, пишет в Postgres, публикует в Kafka. Реактивный pipeline позволяет держать всё это в одном неблокирующем потоке без thread-per-request overhead. Это противоположность content-service и statistics-service, где Virtual Threads дают ту же пропускную способность с меньшей сложностью — но там нет такого fan-out I/O.

> **Следствие:** весь стек — реактивный. JPA/Hibernate несовместимы с WebFlux — используется **R2DBC**. Kafka producer работает через реактивный `ReactiveKafkaProducerTemplate`. HTTP-клиент к content-service — `WebClient`.

### Хранение данных

| Хранилище | Что хранит | Зачем |
|---|---|---|
| PostgreSQL/R2DBC (схема `quiz`) | активные и завершённые сессии, ответы, вопросы сессий, **события Outbox** | надёжность, возможность продолжить сессию, хранение всех данных сессии |

---

## 2. Поддерживаемые типы квизов

---

## 3. Механика сессии

```
GET /api/v1/quiz/{type}/sessions/start[?quizId=uuid]
  ↓ WebClient → content-service /session-data (реактивно)
  ↓ для DECLENSIONS/CONJUGATIONS: генерирует N вопросов из sessionData.questions
  ↓ для VOCABULARY: генерирует N вопросов из sessionData.vocabularyWords (Sanskrit->Translation или Translation->Sanskrit)
  ↓ flatMap: сохраняет сессию в Postgres (R2DBC) в `quiz.quiz_session`
  ↓ flatMap: сохраняет сгенерированные вопросы в Postgres (R2DBC) в `quiz.session_questions`
  ↓ **Публикует QuizSessionStatusChangedEvent (status=IN_PROGRESS) через Outbox Pattern**
  → Mono<StartSessionResponse>

GET /api/v1/quiz/{type}/sessions/{id}/resume
  ↓ R2DBC → Postgres, восстанавливает QuizSession
  ↓ R2DBC → Postgres, получает все SessionQuestion для сессии
  ↓ **Если статус сессии изменился на IN_PROGRESS, публикует QuizSessionStatusChangedEvent через Outbox Pattern**
  → Mono<ResumeSessionResponse>

POST /api/v1/quiz/{type}/sessions/{id}/answer
  ↓ R2DBC → Postgres, получает QuizSession
  ↓ R2DBC → Postgres, получает SessionQuestion для текущего вопроса
  ↓ проверяет ответ (для VOCABULARY учитывает targetLanguage, используя vocabularyWordsJson из QuizSession)
  ↓ flatMap: R2DBC сохраняет QuizAnswer + обновляет QuizSession (answered_questions, score)
  ↓ **Публикует QuizAnsweredEvent через Outbox Pattern**
  → Mono<AnswerResponse>

POST /api/v1/quiz/{type}/sessions/{id}/complete
  ↓ R2DBC → Postgres, получает QuizSession
  ↓ R2DBC → Postgres, получает все QuizAnswer для сессии
  ↓ R2DBC → Postgres, получает все SessionQuestion для сессии
  ↓ R2DBC обновляет статус QuizSession → COMPLETED
  ↓ **Публикует QuizSessionStatusChangedEvent (status=COMPLETED) через Outbox Pattern**
  → Mono<CompleteSessionResponse>

GET /api/v1/quiz-sessions/progress?userId={userId}&quizId={quizId}
  ↓ R2DBC → Postgres, находит последнюю незавершенную сессию для userId и quizId
  → Mono<QuizProgressDto>
```

---

## 4. Зависимости

```
// services/quiz-service/build.gradle.kts
dependencies {
    implementation(libs.spring.boot.webflux)
    implementation(libs.spring.boot.r2dbc)
    implementation(libs.r2dbc.postgresql)
    implementation(libs.spring.kafka)
    implementation(libs.flyway.core)
    implementation(libs.postgresql)
    implementation(project(":shared:samskrtam-dtos"))
}
```

> **Flyway + R2DBC:** R2DBC не поддерживает Flyway напрямую. Решение — добавить JDBC datasource только для Flyway (отдельный бин `FlywayConfig`), а R2DBC использовать для всей бизнес-логики. Это стандартная практика.

---

## 5. Репозитории (ReactiveCrudRepository)

### `QuizSessionRepository`
*   `id`: UUID
*   `user_id`: UUID
*   `quiz_id`: UUID
*   `quiz_type`: VARCHAR
*   `total_questions`: INT
*   `answered_questions`: INT
*   `score`: INT
*   `status`: VARCHAR
*   `started_at`: TIMESTAMP WITH TIME ZONE
*   `completed_at`: TIMESTAMP WITH TIME ZONE
*   `vocabulary_words_json`: TEXT
*   `generated_quiz_data_id`: UUID

### `QuizAnswerRepository`
*   `id`: UUID
*   `session_id`: UUID
*   `question_id`: UUID
*   `selected_option_id`: UUID
*   `is_correct`: BOOLEAN
*   `response_time_ms`: INT
*   `answered_at`: TIMESTAMP WITH TIME ZONE
*   `selected_form_iast`: VARCHAR
*   `correct_form_iast`: VARCHAR

### `SessionQuestionRepository`
*   `id`: UUID
*   `session_id`: UUID
*   `question_id`: UUID
*   `question_number`: INT
*   `text`: TEXT
*   `explanation_ru`: TEXT
*   `explanation_en`: TEXT
*   `declension_stem_id`: UUID
*   `target_case`: VARCHAR
*   `target_number`: VARCHAR
*   `correct_form_iast`: VARCHAR
*   `correct_form_devanagari`: VARCHAR
*   `vocabulary_word_id`: UUID
*   `question_source_language`: VARCHAR
*   `question_target_language`: VARCHAR
*   `correct_translation_ru`: VARCHAR
*   `correct_translation_en`: VARCHAR

### `OutboxEventRepository`
*   `id`: UUID
*   `aggregate_type`: VARCHAR
*   `aggregate_id`: VARCHAR
*   `event_type`: VARCHAR
*   `payload`: TEXT
*   `created_at`: TIMESTAMP WITH TIME ZONE
*   `status`: VARCHAR
*   `error_message`: TEXT
*   `retry_count`: INTEGER
*   `processed_at`: TIMESTAMP WITH TIME ZONE

---

## 6. Кэш в Redis (Reactive)

**Удалено:** Кэширование сессий в Redis было удалено. Все данные сессии теперь хранятся в PostgreSQL.

---

## 6а. Word Score Calculation (On-the-fly)

**Удалено:** Таблица `quiz.word_statistics` и связанная модель `WordStatistics` удалены.

Вместо этого score слова (количество попыток, правильных ответов, последняя дата) вычисляется **на лету** через SQL-запрос к трём существующим таблицам:

```sql
SELECT
  COUNT(*) AS total_attempts,
  SUM(CASE WHEN qa.is_correct THEN 1 ELSE 0 END) AS correct_answers,
  MAX(qa.answered_at) AS last_seen_at
FROM quiz.quiz_answers qa
JOIN quiz.quiz_session qs ON qa.session_id = qs.id
JOIN quiz.session_questions sq ON qa.question_id = sq.id
WHERE qs.user_id = :userId
  AND qs.quiz_id = :quizId
  AND sq.vocabulary_word_id = :wordId
```

Этот запрос выполняется асинхронно через R2DBC `@Query` в `UserSessionService`.
Используется в `LessonServiceImpl` для отображения прогресса по каждому слову урока.

---

## 7. ContentClient (WebClient)

Внутренний клиент для взаимодействия с `content-service`.

### `ContentClient`
*   `webClient`: WebClient
*   `contentBaseUrl`: String

Методы:
*   `getSessionData(UUID quizId)`: Mono<SessionDataResponse>

---

"## 7a. Депрекация correctAnswerRu/correctAnswerEn в пользу caseEnding

Поле `caseEnding` теперь является единственным источником эталонного окончания для грамматических вопросов.
- Поля `correctAnswerRu` и `correctAnswerEn` помечены как `deprecated` в OpenAPI-спецификации.
- **Builder:** `GrammarProgressBuilder` использует метод `findCaseEnding(gender, form, caseEndings)`, который матчит `CaseEndingDto` по `(gender, caseType, numberType)`.
- **Frontend:** должен использовать `caseEnding` для отображения правильного окончания.
- `correctAnswerRu/En` оставлены для обратной совместимости, но больше не заполняются.

## 8. Kafka (ReactiveKafkaProducerTemplate)"

Публикация событий реализована через **Outbox Pattern** — запись в таблицу `quiz.outbox_events` в той же транзакции что и сохранение ответа/завершение сессии. Это гарантирует что событие не потеряется при перезапуске сервиса между сохранением и отправкой в Kafka.

> **Почему не fire-and-forget `.subscribe()`:** `.subscribe()` без ожидания результата в реактивном pipeline означает, что при падении сервиса между `save(answer)` и отправкой в Kafka событие будет потеряно. Outbox атомарно решает эту проблему.

### Outbox Event Structure (внутренняя для quiz-service)

#### `OutboxEvent`
*   `id`: UUID
*   `aggregateType`: String (например, "QuizSession")
*   `aggregateId`: String (например, quizSessionId)
*   `eventType`: OutboxEventType (например, QUIZ_ANSWERED, QUIZ_SESSION_STATUS_CHANGED)
*   `payload`: String (JSON события QuizAnsweredEvent, QuizSessionStatusChangedEvent)
*   `createdAt`: Instant
*   `status`: OutboxStatus (NEW, PUBLISHED, FAILED)
*   `errorMessage`: String
*   `retryCount`: Integer
*   `processedAt`: Instant

#### `OutboxEventType` (Enum)
*   `QUIZ_ANSWERED`
*   `QUIZ_SESSION_STATUS_CHANGED`

#### `OutboxStatus` (Enum)
*   `NEW`
*   `PUBLISHED`
*   `FAILED`

### Outbox Event Publisher Service

Сервис `OutboxEventPublisherService` периодически опрашивает таблицу `quiz.outbox_events` на наличие новых событий (`status = NEW`), публикует их в Kafka и обновляет статус.

#### `OutboxEventPublisherService`
*   `outboxEventRepository`: OutboxEventRepository
*   `reactiveKafkaProducerTemplate`: ReactiveKafkaProducerTemplate
*   `objectMapper`: ObjectMapper

Методы:
*   `publishOutboxEvents()`: @Scheduled метод для запуска публикации.
*   `publishEvent(OutboxEvent event)`: Публикует одно событие в Kafka.
*   `getTopicForEventType(OutboxEventType eventType)`: Определяет топик Kafka для типа события.

---

## 11. Механика реактивного pipeline (QuizSessionService)

Методы `QuizSessionService` используют реактивные цепочки для обработки логики сессий, включая сохранение данных и публикацию событий Outbox.

### `startSession(UUID quizId, UUID userId, String userLocale)`
*   Получает данные квиза из `content-service`.
*   Генерирует и сохраняет новую сессию в `quiz.quiz_session`.
*   Публикует `QuizSessionStatusChangedEvent` (статус `IN_PROGRESS`) через Outbox Pattern.

### `submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale)`
*   Проверяет существование сессии и принадлежность пользователю.
*   Проверяет, не был ли вопрос уже отвечен.
*   Получает детали вопроса из `content-service`.
*   Сохраняет ответ пользователя в `quiz.quiz_answers` и обновляет `quiz.quiz_session`.
*   Публикует `QuizAnsweredEvent` через Outbox Pattern.

### `completeSession(UUID sessionId, UUID userId)`
*   Проверяет существование сессии и принадлежность пользователю.
*   Обновляет статус сессии в `quiz.quiz_session` на `COMPLETED`.
*   Публикует `QuizSessionStatusChangedEvent` (статус `COMPLETED`) через Outbox Pattern.

---

## 12. Миграции базы данных

Все миграции для `quiz-service` объединены в один файл `V1__combined_schema.sql`.

### `quiz.quiz_session`
*   `id`: UUID (PK)
*   `user_id`: UUID
*   `quiz_id`: UUID
*   `quiz_type`: VARCHAR(50)
*   `total_questions`: INT
*   `answered_questions`: INT
*   `score`: INT
*   `status`: VARCHAR(50)
*   `started_at`: TIMESTAMP WITH TIME ZONE
*   `completed_at`: TIMESTAMP WITH TIME ZONE
*   `vocabulary_words_json`: TEXT
*   `generated_quiz_data_id`: UUID

### `quiz.quiz_answers`
*   `id`: UUID (PK)
*   `session_id`: UUID (FK на `quiz.quiz_session`)
*   `question_id`: UUID
*   `selected_option_id`: UUID
*   `is_correct`: BOOLEAN
*   `response_time_ms`: INT
*   `answered_at`: TIMESTAMP WITH TIME ZONE
*   `selected_form_iast`: VARCHAR(255)
*   `correct_form_iast`: VARCHAR(255)

### `quiz.session_questions`
*   `id`: UUID (PK)
*   `session_id`: UUID (FK на `quiz.quiz_session`)
*   `question_id`: UUID
*   `question_number`: INT
*   `text`: TEXT
*   `explanation_ru`: TEXT
*   `explanation_en`: TEXT
*   `declension_stem_id`: UUID
*   `target_case`: VARCHAR(50)
*   `target_number`: VARCHAR(50)
*   `correct_form_iast`: VARCHAR(255)
*   `correct_form_devanagari`: VARCHAR(255)
*   `vocabulary_word_id`: UUID
*   `question_source_language`: VARCHAR(50)
*   `question_target_language`: VARCHAR(50)
*   `correct_translation_ru`: VARCHAR(255)
*   `correct_translation_en`: VARCHAR(255)

### `quiz.outbox_events`
*   `id`: UUID (PK)
*   `aggregate_type`: VARCHAR(255)
*   `aggregate_id`: VARCHAR(255)
*   `event_type`: VARCHAR(255)
*   `payload`: TEXT
*   `created_at`: TIMESTAMP WITH TIME ZONE
*   `status`: VARCHAR(255)
*   `error_message`: TEXT
*   `retry_count`: INTEGER
*   `processed_at`: TIMESTAMP WITH TIME ZONE
