# quiz-service

> Домен: Quiz Sessions — прохождение квизов пользователем
> Язык: **Java 21 + WebFlux (Reactor)**
> Модуль: `services/quiz-service`
> Порт: 8082
> Status: **DRAFT**

---

## 1. Описание

Единый сервис для прохождения квизов всех типов: склонения, спряжения, лексика. Обрабатывает жизненный цикл сессии: старт, ответы, завершение. После каждого ответа публикует событие в Kafka. Данные (вопросы, варианты, слова) получает от `content-service` через реактивный HTTP-клиент.

Разделение ответственности:
- **content-service** — что есть в квизах (данные, настройки)
- **quiz-service** — как пользователь их проходит (сессии, ответы, события)

### Стек

WebFlux выбран осознанно: quiz-service интенсивно работает с I/O одновременно — обращается к content-service, читает/пишет Redis, пишет в Postgres, публикует в Kafka. Реактивный pipeline позволяет держать всё это в одном неблокирующем потоке без thread-per-request overhead. Это противоположность content-service и statistics-service, где Virtual Threads дают ту же пропускную способность с меньшей сложностью — но там нет такого fan-out I/O.

> **Следствие:** весь стек — реактивный. JPA/Hibernate несовместимы с WebFlux — используется **R2DBC**. Kafka producer работает через реактивный `ReactiveKafkaProducerTemplate`. HTTP-клиент к content-service — `WebClient`.

### Хранение данных

| Хранилище | Что хранит | Зачем |
|---|---|---|
| PostgreSQL/R2DBC (схема `quiz`) | активные и завершённые сессии, ответы | надёжность, возможность продолжить сессию |
| Redis (Reactive) | текущее состояние активной сессии | быстрый доступ при каждом ответе |

Postgres — источник истины. Redis — кэш поверх него. При промахе кэша сессия восстанавливается из Postgres. После завершения сессии запись остаётся в Postgres со статусом `COMPLETED`.

---

## 2. Поддерживаемые типы квизов

| QuizType | URL-префикс | Механика вопроса |
|---|---|---|
| `DECLENSIONS` | `/quiz/declensions` | Multiple-choice из фиксированных вариантов |
| `CONJUGATIONS` | `/quiz/conjugations` | Multiple-choice из фиксированных вариантов |
| `VOCABULARY` | `/quiz/vocabulary` | Multiple-choice, дистракторы из того же квиза |

---

## 3. Механика сессии

```
GET /api/v1/quiz/{type}/sessions/start[?quizId=uuid]
  ↓ WebClient → content-service /session-data (реактивно)
  ↓ случайно выбирает N вопросов
  ↓ для VOCABULARY — генерирует дистракторы
  ↓ flatMap: сохраняет сессию в Postgres (R2DBC) + кладёт в Redis (Reactive)
  → Mono<StartSessionResponse>

GET /api/v1/quiz/{type}/sessions/{id}/resume
  ↓ Redis.get → если hit: возвращает из Redis
  ↓ если miss: R2DBC → Postgres, восстанавливает SessionCache → кладёт в Redis
  → Mono<ResumeSessionResponse>

POST /api/v1/quiz/{type}/sessions/{id}/answer
  ↓ Redis.get → correctOptionId
  ↓ проверяет ответ
  ↓ flatMap: R2DBC сохраняет QuizAnswer + Redis обновляет SessionCache
  ↓ Kafka publishAnswerSubmitted (fire-and-forget через subscribe)
  → Mono<AnswerResponse>

POST /api/v1/quiz/{type}/sessions/{id}/complete
  ↓ R2DBC обновляет статус → COMPLETED
  ↓ Redis.delete (сессия закрыта)
  ↓ Kafka publishSessionCompleted
  → Mono<CompleteSessionResponse>
```

---

## 4. Зависимости

```kotlin
// services/quiz-service/build.gradle.kts
dependencies {
    implementation(libs.spring.boot.webflux)
    implementation(libs.spring.boot.r2dbc)
    implementation(libs.r2dbc.postgresql)           // io.r2dbc:r2dbc-postgresql
    implementation(libs.spring.boot.data.redis.reactive)
    implementation(libs.spring.kafka)               // ReactiveKafkaProducerTemplate
    implementation(libs.flyway.core)                // Flyway — только для миграций (JDBC)
    implementation(libs.postgresql)                 // JDBC driver — только для Flyway
    implementation(libs.jackson.module.kotlin)
    implementation(project(":shared:kafka-events"))
}
```

> **Flyway + R2DBC:** R2DBC не поддерживает Flyway напрямую. Решение — добавить JDBC datasource только для Flyway (отдельный бин `FlywayConfig`), а R2DBC использовать для всей бизнес-логики. Это стандартная практика.

---

## 5. Сущности (R2DBC)

```java
// sm/selflearn/samskrtam/quiz/model/QuizSession.java
// R2DBC — нет @Entity, нет @Column(nullable), используем аннотации Spring Data R2DBC
@Table("quiz.quiz_sessions")
public class QuizSession {

    @Id
    private UUID id;
    private UUID userId;
    private UUID quizId;
    private String quizType;      // QuizType enum → String вручную
    private String status;        // SessionStatus enum → String вручную
    private String questionIds;   // JSON array UUID — сериализуем вручную (нет JSONB маппера в R2DBC)
    private int score;
    private int totalQuestions;
    private Instant startedAt;
    private Instant completedAt;
}

// sm/selflearn/samskrtam/quiz/model/QuizAnswer.java
@Table("quiz.quiz_answers")
public class QuizAnswer {

    @Id
    private UUID id;
    private UUID sessionId;
    private UUID questionId;
    private UUID selectedOptionId;
    private UUID correctOptionId;
    private boolean correct;
    private int responseTimeMs;
    private Instant answeredAt;
}
```

> **R2DBC и JSONB:** R2DBC-postgresql поддерживает JSONB, но требует кастомного `Codec`. В v1 проще хранить `question_ids` как `TEXT` (JSON-строка) и сериализовать через ObjectMapper вручную.

---

## 6. Репозитории (ReactiveCrudRepository)

```java
// sm/selflearn/samskrtam/quiz/repository/QuizSessionRepository.java
public interface QuizSessionRepository
        extends ReactiveCrudRepository<QuizSession, UUID> {

    Flux<QuizSession> findByUserIdAndStatus(UUID userId, String status);
    Mono<QuizSession> findByIdAndUserId(UUID id, UUID userId);
}

// sm/selflearn/samskrtam/quiz/repository/QuizAnswerRepository.java
public interface QuizAnswerRepository
        extends ReactiveCrudRepository<QuizAnswer, UUID> {

    Flux<QuizAnswer> findBySessionId(UUID sessionId);

    // Проверка дубликата перед сохранением ответа
    Mono<Boolean> existsBySessionIdAndQuestionId(UUID sessionId, UUID questionId);
}
```

---

## 7. Flyway Migrations

```sql
-- V1__create_schema.sql
CREATE SCHEMA IF NOT EXISTS quiz;

-- V2__create_quiz_sessions.sql
CREATE TABLE quiz.quiz_sessions (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    quiz_id         UUID        NOT NULL,
    quiz_type       VARCHAR(20) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    question_ids    TEXT        NOT NULL,   -- JSON array строк UUID
    score           INT         NOT NULL DEFAULT 0,
    total_questions INT         NOT NULL,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,

    CONSTRAINT pk_quiz_sessions PRIMARY KEY (id),
    CONSTRAINT ck_quiz_type     CHECK (quiz_type IN ('DECLENSIONS','CONJUGATIONS','VOCABULARY')),
    CONSTRAINT ck_status        CHECK (status IN ('ACTIVE','COMPLETED','ABANDONED'))
);

CREATE INDEX idx_sessions_user_id ON quiz.quiz_sessions (user_id);
CREATE INDEX idx_sessions_status  ON quiz.quiz_sessions (user_id, status);

-- V3__create_quiz_answers.sql
CREATE TABLE quiz.quiz_answers (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid(),
    session_id         UUID        NOT NULL REFERENCES quiz.quiz_sessions(id),
    question_id        UUID        NOT NULL,
    selected_option_id UUID        NOT NULL,
    correct_option_id  UUID        NOT NULL,
    correct            BOOLEAN     NOT NULL,
    response_time_ms   INT,
    answered_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_quiz_answers     PRIMARY KEY (id),
    CONSTRAINT uq_session_question UNIQUE (session_id, question_id)
);

CREATE INDEX idx_answers_session_id ON quiz.quiz_answers (session_id);
```

---

## 8. Кэш в Redis (Reactive)

Ключ: `quiz:session:{sessionId}`. Используется `ReactiveRedisTemplate<String, SessionCache>`.

```java
// sm/selflearn/samskrtam/quiz/model/SessionCache.java
// Хранится в Redis как JSON
public class SessionCache {
    private UUID sessionId;
    private UUID userId;
    private UUID quizId;
    private QuizType quizType;
    private List<CachedQuestion> questions;   // включая correctOptionId
    private Set<UUID> answeredQuestionIds;
    private int score;
}

public class CachedQuestion {
    private UUID questionId;
    private UUID correctOptionId;   // не передаётся клиенту
    private String explanationRu;
    private String explanationEn;
}
```

```java
// sm/selflearn/samskrtam/quiz/service/SessionCacheService.java
@Service
public class SessionCacheService {

    private final ReactiveRedisTemplate<String, SessionCache> redisTemplate;
    private final QuizSessionRepository sessionRepository;
    private final QuizAnswerRepository answerRepository;
    private final ContentClient contentClient;

    private String key(UUID sessionId) {
        return "quiz:session:" + sessionId;
    }

    public Mono<SessionCache> get(UUID sessionId) {
        return redisTemplate.opsForValue().get(key(sessionId))
                .switchIfEmpty(restoreFromPostgres(sessionId));  // fallback
    }

    public Mono<Void> put(UUID sessionId, SessionCache cache) {
        return redisTemplate.opsForValue()
                .set(key(sessionId), cache)
                .then();
    }

    public Mono<Void> evict(UUID sessionId) {
        return redisTemplate.delete(key(sessionId)).then();
    }

    private Mono<SessionCache> restoreFromPostgres(UUID sessionId) {
        return sessionRepository.findById(sessionId)
                .flatMap(session ->
                    answerRepository.findBySessionId(sessionId).collectList()
                        .flatMap(answers ->
                            contentClient.getSessionData(session.getQuizId())
                                .map(data -> buildCache(session, answers, data))
                        )
                )
                .flatMap(cache -> put(sessionId, cache).thenReturn(cache));
    }
}
```

---

## 9. ContentClient (WebClient)

```java
// sm/selflearn/samskrtam/quiz/service/ContentClient.java
@Component
public class ContentClient {

    private final WebClient webClient;

    public ContentClient(@Value("${content.service.url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<SessionDataResponse> getSessionData(UUID quizId) {
        return webClient.get()
                .uri("/api/v1/content/quizzes/{id}/session-data", quizId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new QuizNotFoundException(quizId)))
                .bodyToMono(SessionDataResponse.class);
    }
}
```

---

## 10. Kafka (ReactiveKafkaProducerTemplate)

Публикация событий реализована через **Outbox Pattern** — запись в таблицу `quiz.outbox_events` в той же транзакции что и сохранение ответа/завершение сессии. Это гарантирует что событие не потеряется при перезапуске сервиса между сохранением и отправкой в Kafka.

> **Почему не fire-and-forget `.subscribe()`:** `.subscribe()` без ожидания результата в реактивном pipeline означает, что при падении сервиса между `save(answer)` и отправкой в Kafka событие будет потеряно. Outbox атомарно решает эту проблему.

```java
// sm/selflearn/samskrtam/quiz/event/OutboxEventPublisher.java
@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;
    private final OutboxEventRepository outboxRepository;

    /**
     * Публикует события из таблицы outbox в Kafka.
     * Вызывается @Scheduled процессором каждые 5 секунд.
     */
    public Flux<Void> publishPending() {
        return outboxRepository.findByStatus(OutboxStatus.PENDING)
                .flatMap(event -> kafkaTemplate
                        .send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .doOnSuccess(result -> log.debug(
                                "Published outbox event: id={}, topic={}", event.getId(), event.getTopic()))
                        .then(outboxRepository.markProcessed(event.getId()))
                        .onErrorResume(e -> {
                            log.error("Failed to publish outbox event: id={}", event.getId(), e);
                            return outboxRepository.markFailed(event.getId(), e.getMessage());
                        })
                );
    }
}
```

```java
// sm/selflearn/samskrtam/quiz/model/OutboxEvent.java (R2DBC)
@Table("quiz.outbox_events")
public class OutboxEvent {
    @Id
    private UUID id;
    private String aggregateId;   // userId — ключ партиции Kafka
    private String topic;         // quiz.answer.submitted / quiz.session.completed
    private String payload;       // JSON события
    private String status;        // PENDING / PROCESSED / FAILED
    private Instant createdAt;
    private Instant processedAt;
    private int retryCount;
    private String errorMessage;
}
```

```sql
-- V4__create_quiz_outbox.sql
CREATE TABLE quiz.outbox_events (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id  VARCHAR(36) NOT NULL,
    topic         VARCHAR(100) NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,
    retry_count   INT          NOT NULL DEFAULT 0,
    error_message TEXT,

    CONSTRAINT pk_quiz_outbox PRIMARY KEY (id),
    CONSTRAINT ck_outbox_status CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_quiz_outbox_pending ON quiz.outbox_events (status, created_at)
    WHERE status = 'PENDING';
```

В `AnswerService.submitAnswer()` вместо прямой публикации — атомарная запись в outbox:

```java
// Внутри flatMap в SessionService, вместо eventPublisher.publishAnswerSubmitted(...)
return answerRepository.save(answer)
        .then(cacheService.put(sessionId, cache))
        .then(outboxRepository.save(OutboxEvent.forAnswerSubmitted(event)))  // атомарно
        .thenReturn(buildResponse(q, correct, cache));
```

---

## 11. Пример реактивного pipeline (SessionService)

```java
// sm/selflearn/samskrtam/quiz/service/SessionService.java
public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request) {
    return cacheService.get(sessionId)                          // Mono<SessionCache>
            .filter(cache -> cache.getUserId().equals(userId))
            .switchIfEmpty(Mono.error(new SessionNotFoundException(sessionId)))
            .filter(cache -> !cache.getAnsweredQuestionIds().contains(request.questionId()))
            .switchIfEmpty(Mono.error(new AlreadyAnsweredException(request.questionId())))
            .flatMap(cache -> {
                CachedQuestion q = cache.findQuestion(request.questionId());
                boolean correct = q.getCorrectOptionId().equals(request.selectedOptionId());

                QuizAnswer answer = QuizAnswer.builder()
                        .sessionId(sessionId)
                        .questionId(request.questionId())
                        .selectedOptionId(request.selectedOptionId())
                        .correctOptionId(q.getCorrectOptionId())
                        .correct(correct)
                        .responseTimeMs(request.responseTimeMs())
                        .answeredAt(Instant.now())
                        .build();

                cache.markAnswered(request.questionId(), correct);

                AnswerSubmitted event = new AnswerSubmitted(
                        userId, cache.getQuizType(), cache.getQuizId(),
                        request.questionId(), request.selectedOptionId(),
                        correct, request.responseTimeMs());

                // Атомарная запись ответа + outbox события в одном pipeline
                return answerRepository.save(answer)            // Mono<QuizAnswer>
                        .then(cacheService.put(sessionId, cache))
                        .then(outboxRepository.save(           // гарантия доставки
                                OutboxEvent.forAnswerSubmitted(event)))
                        .thenReturn(buildResponse(q, correct, cache));
            });
}
```

---

## 12. Генерация дистракторов (VOCABULARY)

```java
// sm/selflearn/samskrtam/quiz/service/DistractorService.java
public List<VocabularyWord> getDistractors(
        List<VocabularyWord> allWords,
        UUID correctWordId,
        int count   // обычно 3
) {
    List<VocabularyWord> pool = allWords.stream()
            .filter(w -> !w.getId().equals(correctWordId))
            .collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(pool);
    return pool.stream().limit(count).toList();
}
```

---

## 13. API

### Склонения и спряжения

```
GET  /api/v1/quiz/declensions/sessions/start
GET  /api/v1/quiz/declensions/sessions/{id}/resume
POST /api/v1/quiz/declensions/sessions/{id}/answer
POST /api/v1/quiz/declensions/sessions/{id}/complete

GET  /api/v1/quiz/conjugations/sessions/start
GET  /api/v1/quiz/conjugations/sessions/{id}/resume
POST /api/v1/quiz/conjugations/sessions/{id}/answer
POST /api/v1/quiz/conjugations/sessions/{id}/complete
```

### Лексика (slug-based)

```
GET  /api/v1/quiz/vocabulary                                       → список квизов (из content-service)
GET  /api/v1/quiz/vocabulary/{slug}                                → детали квиза
GET  /api/v1/quiz/vocabulary/{slug}/sessions/start
GET  /api/v1/quiz/vocabulary/{slug}/sessions/{id}/resume
POST /api/v1/quiz/vocabulary/{slug}/sessions/{id}/answer
POST /api/v1/quiz/vocabulary/{slug}/sessions/{id}/complete
```

### POST /sessions/{id}/answer — Request / Response

```json
// Request
{ "questionId": "uuid", "selectedOptionId": "uuid", "responseTimeMs": 4200 }

// Response
{
  "isCorrect": true,
  "correctOptionId": "uuid",
  "explanationRu": "Дательный ед.ч. основ на -a — окончание -āya",
  "explanationEn": "Dative singular of -a stems takes ending -āya",
  "questionNumber": 3,
  "totalQuestions": 10
}
```

---

## 14. Backend структура

```
sm/selflearn/samskrtam/quiz/
├── Application.java
├── config/
│   ├── FlywayConfig.java              ← JDBC бин только для Flyway
│   ├── RedisConfig.java               ← ReactiveRedisTemplate<String, SessionCache>
│   └── WebClientConfig.java           ← WebClient бины
├── controller/
│   ├── DeclensionsSessionController.java
│   ├── ConjugationsSessionController.java
│   └── VocabularyController.java
├── service/
│   ├── SessionService.java            ← старт, resume, завершение (Mono/Flux)
│   ├── AnswerService.java             ← проверка ответов, сохранение
│   ├── SessionCacheService.java       ← ReactiveRedis + fallback к Postgres
│   ├── ContentClient.java             ← WebClient к content-service
│   └── DistractorService.java
├── event/
│   ├── OutboxEventPublisher.java      ← Scheduler, публикует PENDING события в Kafka
│   └── OutboxEventRepository.java     ← ReactiveCrudRepository для quiz.outbox_events
├── repository/
│   ├── QuizSessionRepository.java     ← ReactiveCrudRepository
│   └── QuizAnswerRepository.java      ← ReactiveCrudRepository
├── model/
│   ├── QuizSession.java               ← R2DBC @Table
│   ├── QuizAnswer.java                ← R2DBC @Table
│   ├── SessionCache.java              ← Redis DTO
│   ├── CachedQuestion.java
│   ├── SessionStatus.java
│   └── QuizType.java
└── dto/
    ├── StartSessionResponse.java
    ├── ResumeSessionResponse.java
    ├── AnswerRequest.java
    ├── AnswerResponse.java
    └── CompleteSessionResponse.java
```

---

## 15. application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: quiz-service

  # R2DBC — основной datasource для бизнес-логики
  r2dbc:
    url: ${SPRING_R2DBC_URL:r2dbc:postgresql://postgres:5432/samskrtam}
    username: ${DB_USER:samskrtam}
    password: ${DB_PASSWORD}
    properties:
      schema: quiz

  # JDBC — только для Flyway (отдельный бин в FlywayConfig)
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://postgres:5432/samskrtam}
    username: ${DB_USER:samskrtam}
    password: ${DB_PASSWORD}
  flyway:
    enabled: false   # отключаем auto, запускаем вручную из FlywayConfig
    schemas: quiz

  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: 6379

  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

content:
  service:
    url: ${CONTENT_SERVICE_URL:http://content-service:8081}

# JWT не валидируется — сервис доверяет заголовкам X-User-* от Gateway.
```

---

## 16. Acceptance Criteria

- [ ] Все эндпоинты возвращают `Mono<T>` / `Flux<T>` — нет блокирующих вызовов
- [ ] Сессия сохраняется в Postgres (R2DBC) при старте
- [ ] `correctOptionId` не передаётся клиенту до POST /answer
- [ ] Ответ сохраняется в Postgres и Redis при каждом POST /answer
- [ ] При промахе Redis — сессия восстанавливается из Postgres без ошибки клиенту
- [ ] GET /resume возвращает состояние незавершённой сессии через день
- [ ] После каждого ответа — `AnswerSubmitted` записывается в `quiz.outbox_events` атомарно с ответом
- [ ] После завершения — `SessionCompleted` в outbox, статус → COMPLETED в Postgres
- [ ] Outbox-процессор публикует PENDING события в Kafka и помечает их PROCESSED
- [ ] При сбое публикации в Kafka — событие остаётся PENDING и будет опубликовано при следующем запуске процессора
- [ ] Нельзя ответить дважды на один вопрос (UNIQUE constraint + 409 Conflict)
- [ ] Flyway миграции применяются при старте через `FlywayConfig` (JDBC бин)
- [ ] Дистракторы для VOCABULARY берутся из того же квиза

---

## 17. Открытые вопросы

- [ ] Сколько хранить завершённые сессии в `quiz.quiz_sessions`? (для истории достаточно statistics-service)
- [ ] Кэшировать ли session-data от content-service в Redis (снижение latency)?
- [ ] Деванагари или только IAST в вопросах?
- [ ] Режим «только ошибки» — повторить неправильные ответы?
- [ ] Обратный режим VOCABULARY — «Как будет по-санскритски слон»?
