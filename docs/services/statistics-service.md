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

## 2. Kafka Consumers

Consumer должен быть **идемпотентным**: повторная доставка одного и того же события (rebalance, retry после DLQ) не должна создавать дублирующую запись. Идемпотентность реализована через `UNIQUE (event_id)` в БД и `INSERT ... ON CONFLICT DO NOTHING` в репозитории.

```java
// sm/selflearn/samskrtam/statistics/consumer/AnswerSubmittedConsumer.java
@Component
@Slf4j
@RequiredArgsConstructor
public class AnswerSubmittedConsumer {

    private final StatisticsService statisticsService;

    @KafkaListener(topics = "quiz.answer.submitted", groupId = "statistics-service")
    public void handle(sm.selflearn.samskrtam.quiz.event.AnswerSubmitted event) { // Updated path
        log.debug("Received AnswerSubmitted: eventId={}, userId={}", event.getEventId(), event.getUserId());
        // Virtual Threads — обычный блокирующий код, никакого Mono/Flux
        statisticsService.recordAnswer(event);
    }
}

// sm/selflearn/samskrtam/statistics/consumer/SessionCompletedConsumer.java
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionCompletedConsumer {

    private final StatisticsService statisticsService;

    @KafkaListener(topics = "quiz.session.completed", groupId = "statistics-service")
    public void handle(sm.selflearn.samskrtam.quiz.event.SessionCompleted event) { // Updated path
        log.debug("Received SessionCompleted: eventId={}, userId={}", event.getEventId(), event.getUserId());
        statisticsService.recordSession(event);
    }
}
```

```java
// sm/selflearn/samskrtam/statistics/service/StatisticsService.java
@Service
@Slf4j
@RequiredArgsConstructor
public class StatisticsService {

    private final AnswerRecordRepository answerRepository;
    private final SessionRecordRepository sessionRepository;

    @Transactional
    public void recordAnswer(sm.selflearn.samskrtam.quiz.event.AnswerSubmitted event) { // Updated path
        // INSERT ... ON CONFLICT (event_id) DO NOTHING
        // Повторная доставка события игнорируется — дубликат не создаётся
        int inserted = answerRepository.insertIfNotExists(
                event.getEventId(),
                event.getUserId(),
                event.getQuizType().name(), // Assuming QuizType is an enum and needs .name()
                event.getQuizId(),
                event.getQuestionId(),
                event.getSelectedOptionId(),
                event.isCorrect(),
                event.getResponseTimeMs(),
                event.getOccurredAt()
        );
        if (inserted == 0) {
            log.warn("Duplicate AnswerSubmitted ignored: eventId={}", event.getEventId());
        }
    }

    @Transactional
    public void recordSession(sm.selflearn.samskrtam.quiz.event.SessionCompleted event) { // Updated path
        int inserted = sessionRepository.insertIfNotExists(
                event.getEventId(),
                event.getUserId(),
                event.getQuizType().name(), // Assuming QuizType is an enum and needs .name()
                event.getQuizId(),
                event.getScore(),
                event.getTotalQuestions(),
                event.getDurationMs(),
                event.getOccurredAt()
        );
        if (inserted == 0) {
            log.warn("Duplicate SessionCompleted ignored: eventId={}", event.getEventId());
        }
    }
}
```

```java
// sm/selflearn/samskrtam/statistics/repository/AnswerRecordRepository.java
public interface AnswerRecordRepository extends JpaRepository<AnswerRecord, UUID> {

    // Возвращает 1 если запись вставлена, 0 если event_id уже существует
    @Modifying
    @Query(value = """
            INSERT INTO statistics.answer_records
                (event_id, user_id, quiz_type, quiz_id, question_id,
                 selected_option_id, correct, response_time_ms, occurred_at)
            VALUES
                (:eventId, :userId, :quizType, :quizId, :questionId,
                 :selectedOptionId, :correct, :responseTimeMs, :occurredAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfNotExists(
            @Param("eventId")          UUID    eventId,
            @Param("userId")           UUID    userId,
            @Param("quizType")         String  quizType,
            @Param("quizId")           UUID    quizId,
            @Param("questionId")       UUID    questionId,
            @Param("selectedOptionId") UUID    selectedOptionId,
            @Param("correct")          boolean correct,
            @Param("responseTimeMs")   int     responseTimeMs,
            @Param("occurredAt")       Instant occurredAt
    );

    // Запросы для API
    List<AnswerRecord> findByUserId(UUID userId);
    List<AnswerRecord> findByUserIdAndQuizId(UUID userId, UUID quizId);
}

// sm/selflearn/samskrtam/statistics/repository/SessionRecordRepository.java
public interface SessionRecordRepository extends JpaRepository<SessionRecord, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO statistics.session_records
                (event_id, user_id, quiz_type, quiz_id, score, total_questions,
                 duration_ms, occurred_at)
            VALUES
                (:eventId, :userId, :quizType, :quizId, :score, :totalQuestions,
                 :durationMs, :occurredAt)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfNotExists(
            @Param("eventId")        UUID    eventId,
            @Param("userId")         UUID    userId,
            @Param("quizType")       String  quizType,
            @Param("quizId")         UUID    quizId,
            @Param("score")          int     score,
            @Param("totalQuestions") int     totalQuestions,
            @Param("durationMs")     long    durationMs,
            @Param("occurredAt")     Instant occurredAt
    );

    List<SessionRecord> findByUserId(UUID userId);
}
```

---

## 3. API

```
GET /api/v1/statistics/me              → личная статистика
GET /api/v1/statistics/me/heatmap      → тепловая карта ошибок
GET /api/v1/statistics/me/history      → история сессий
GET /api/v1/statistics/attempts/{id}   → детали попытки
GET /api/v1/leaderboard                → лидерборд (алгоритмы в leaderboard.md)
  ?type=xp|elo|accuracy|streak|skill|composite
  &period=all|weekly|monthly
  &quizType=DECLENSIONS|CONJUGATIONS|VOCABULARY
  &groupId=<uuid>
  &limit=1-100
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

### Правило подсчёта очков

Алгоритмы лидерборда (XP, Elo, Streak, Skill Rating, Composite и Weekly) вынесены
в отдельный файл → [leaderboard.md](leaderboard.md).

`StatisticsService` вызывает алгоритмы при обработке Kafka-событий:

```java
// recordSession() → XpService, EloService, SkillRatingService
// recordAnswer()  → StreakService, AccuracyService
```

---

## 4. Backend структура

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
│   └── LeaderboardService.java      ← роутит по type + period (см. leaderboard.md)
├── algorithm/                        ← алгоритмы лидерборда (см. leaderboard.md)
│   ├── XpService.java
│   ├── EloService.java
│   ├── EloCalculator.java
│   ├── AccuracyService.java
│   ├── StreakService.java
│   ├── SkillRatingService.java
│   └── CompositeScoreService.java
├── scheduler/
│   └── LeaderboardRefreshScheduler.java
├── repository/
│   ├── AnswerRecordRepository.java
│   ├── SessionRecordRepository.java
│   ├── XpLedgerRepository.java
│   ├── XpTotalsRepository.java
│   ├── EloRatingRepository.java
│   ├── EloHistoryRepository.java
│   ├── UserStreakRepository.java
│   ├── SkillRatingRepository.java
│   └── AccuracyCacheRepository.java
├── model/
│   ├── AnswerRecord.java
│   ├── SessionRecord.java
│   ├── XpLedger.java
│   ├── XpTotals.java
│   ├── EloRating.java
│   ├── EloHistory.java
│   ├── UserStreak.java
│   ├── SkillRating.java
│   └── CompositeScore.java
└── dto/
    ├── PersonalStatsResponse.java
    ├── HeatmapResponse.java
    ├── LeaderboardResponse.java
    ├── LeaderboardEntry.java
    └── AttemptDetailResponse.java
```

---

## 5. application.yml

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
        spring.json.trusted.packages: "sm.selflearn.samskrtam.quiz.event" # Updated path

# JWT не валидируется — сервис доверяет заголовкам X-User-* от Gateway.
# userId берётся из X-User-Id, переданного Gateway после валидации токена.
```

---

## 6. Acceptance Criteria

- [ ] AnswerSubmitted из Kafka → сохраняется в answer_records
- [ ] SessionCompleted из Kafka → сохраняется в session_records
- [ ] При недоступности сервиса события ждут в Kafka (не теряются)
- [ ] Повторная доставка одного события не создаёт дублирующую запись (ON CONFLICT DO NOTHING по event_id)
- [ ] Дубликат логируется на уровне WARN с eventId для диагностики
- [ ] Лидерборд: очки = сумма лучших результатов по каждому квизу
- [ ] Тепловая карта показывает вопросы с correctRate < 0.5

---

## 7. Открытые вопросы

- [ ] Кэшировать лидерборд в Redis?
- [ ] Недельный/месячный рейтинг?
- [ ] CQRS для read-моделей статистики?
