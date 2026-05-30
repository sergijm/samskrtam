# statistics-service

> Домен: Statistics
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/statistics-service`
> Порт: 8086
> Status: **DRAFT**

---

## 1. Описание

Получает события из Kafka и строит аналитику. Нет синхронной связи с Quiz сервисами — только через очередь. Java 21 Virtual Threads упрощают Kafka consumer — обычный блокирующий код.

---

## 2. Сущности

```java
// sm/selflearn/samskrtam/statistics/model/AnswerRecord.java
@Entity
@Table(name = "answer_records", schema = "statistics")
public class AnswerRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private QuizType quizType;
    private UUID quizId;
    private UUID questionId;
    private UUID selectedOptionId;
    private boolean correct;
    private int responseTimeMs;
    private Instant occurredAt;
}

// sm/selflearn/samskrtam/statistics/model/SessionRecord.java
@Entity
@Table(name = "session_records", schema = "statistics")
public class SessionRecord {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private UUID userId;
    @Enumerated(EnumType.STRING)
    private QuizType quizType;
    private UUID quizId;
    private int score;
    private int totalQuestions;
    private long durationMs;
    private Instant occurredAt;
}
```

---

## 3. Flyway Migrations

```sql
-- V1__create_schema.sql
CREATE SCHEMA IF NOT EXISTS statistics;

-- V2__create_answer_records.sql
CREATE TABLE statistics.answer_records (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id            UUID        NOT NULL,
    quiz_type          VARCHAR(20) NOT NULL,
    quiz_id            UUID        NOT NULL,
    question_id        UUID        NOT NULL,
    selected_option_id UUID        NOT NULL,
    correct            BOOLEAN     NOT NULL,
    response_time_ms   INT         NOT NULL,
    occurred_at        TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_answer_records PRIMARY KEY (id)
);

CREATE INDEX idx_answers_user_id  ON statistics.answer_records (user_id);
CREATE INDEX idx_answers_question ON statistics.answer_records (question_id);
CREATE INDEX idx_answers_occurred ON statistics.answer_records (occurred_at DESC);

-- V3__create_session_records.sql
CREATE TABLE statistics.session_records (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    quiz_type       VARCHAR(20) NOT NULL,
    quiz_id         UUID        NOT NULL,
    score           INT         NOT NULL,
    total_questions INT         NOT NULL,
    duration_ms     BIGINT      NOT NULL,
    occurred_at     TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_session_records PRIMARY KEY (id)
);

CREATE INDEX idx_sessions_user_id ON statistics.session_records (user_id);
CREATE INDEX idx_sessions_occurred ON statistics.session_records (occurred_at DESC);
```

---

## 4. Kafka Consumers

```java
// sm/selflearn/samskrtam/statistics/consumer/AnswerSubmittedConsumer.java
@Component
public class AnswerSubmittedConsumer {

    private final StatisticsService statisticsService;

    @KafkaListener(topics = "quiz.answer.submitted", groupId = "statistics-service")
    public void handle(AnswerSubmitted event) {
        // Virtual Threads — обычный блокирующий код, никакого Mono/Flux
        statisticsService.recordAnswer(event);
    }
}

// sm/selflearn/samskrtam/statistics/consumer/SessionCompletedConsumer.java
@Component
public class SessionCompletedConsumer {

    @KafkaListener(topics = "quiz.session.completed", groupId = "statistics-service")
    public void handle(SessionCompleted event) {
        statisticsService.recordSession(event);
    }
}
```

---

## 5. API

```
GET /api/v1/statistics/me              → личная статистика
GET /api/v1/statistics/me/heatmap      → тепловая карта ошибок
GET /api/v1/statistics/me/history      → история сессий
GET /api/v1/statistics/leaderboard     → рейтинг группы
GET /api/v1/statistics/attempts/{id}   → детали попытки
```

### GET /api/v1/statistics/leaderboard — Response

```json
{
  "entries": [
    { "rank": 1, "username": "anna", "totalPoints": 24,
      "totalSessions": 12, "isCurrentUser": false },
    { "rank": 2, "username": "ivan", "totalPoints": 16,
      "totalSessions": 8,  "isCurrentUser": true }
  ]
}
```

### Правило подсчёта очков лидерборда

```
Очки = сумма лучших score по каждому квизу

Пример:
  Квиз "Склонения": попытки [6, 8, 9] → берём 9
  Квиз "Спряжения": попытки [5, 7]    → берём 7
  Итого: 16 очков
```

---

## 6. Backend структура

```
sm/selflearn/samskrtam/statistics/
├── Application.java
├── consumer/
│   ├── AnswerSubmittedConsumer.java
│   └── SessionCompletedConsumer.java
├── controller/
│   └── StatisticsController.java
├── service/
│   ├── StatisticsService.java
│   └── LeaderboardService.java
├── repository/
│   ├── AnswerRecordRepository.java
│   └── SessionRecordRepository.java
├── model/
│   ├── AnswerRecord.java
│   └── SessionRecord.java
└── dto/
    ├── PersonalStatsResponse.java
    ├── HeatmapResponse.java
    ├── LeaderboardResponse.java
    └── AttemptDetailResponse.java
```

---

## 7. application.yml

```yaml
server:
  port: 8086

spring:
  application:
    name: statistics-service
  threads:
    virtual:
      enabled: true
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${DB_USER:samskrtam}
    password: ${DB_PASSWORD}
  jpa:
    properties:
      hibernate:
        default_schema: statistics
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    consumer:
      group-id: statistics-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "sm.selflearn.samskrtam.events"

# JWT не валидируется — сервис доверяет заголовкам X-User-* от Gateway.
# userId берётся из X-User-Id, переданного Gateway после валидации токена.
```

---

## 8. Acceptance Criteria

- [ ] AnswerSubmitted из Kafka → сохраняется в answer_records
- [ ] SessionCompleted из Kafka → сохраняется в session_records
- [ ] При недоступности сервиса события ждут в Kafka (не теряются)
- [ ] Лидерборд: очки = сумма лучших результатов по каждому квизу
- [ ] Тепловая карта показывает вопросы с correctRate < 0.5

---

## 9. Открытые вопросы

- [ ] Кэшировать лидерборд в Redis?
- [ ] Недельный/месячный рейтинг?
- [ ] CQRS для read-моделей статистики?
