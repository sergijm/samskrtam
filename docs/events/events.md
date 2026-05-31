# Events — Kafka события

> Связанные файлы: [architecture.md](../architecture.md) · [services/statistics-service.md](../services/statistics-service.md)
> Status: **DRAFT**

---

## 1. Концепция

Quiz сервисы (Java 21) публикуют события в Kafka. Statistics Service (Java 21) подписывается и обрабатывает асинхронно. Shared модуль `shared/kafka-events` на Java 21 — единый источник истины, совместим со всеми сервисами включая Kotlin dictionary-service.

---

## 2. Topics

| Topic | Producer | Consumer | Ключ | Retention |
|---|---|---|---|---|
| quiz.answer.submitted | все quiz-сервисы | statistics-service | userId | 7 дней |
| quiz.session.completed | все quiz-сервисы | statistics-service | userId | 7 дней |
| quiz.answer.submitted.dlt | — (Spring автоматически) | — (мониторинг/алерты) | userId | 30 дней |
| quiz.session.completed.dlt | — (Spring автоматически) | — (мониторинг/алерты) | userId | 30 дней |

Ключ = userId — все события одного пользователя идут в один partition (порядок гарантирован).

DLT-топики (Dead Letter Topic) создаются автоматически `DefaultErrorHandler`-ом. Сообщения в DLT требуют ручного разбора или отдельного consumer для replay.

---

## 3. Shared модуль — Java 21 Records

```java
// shared/kafka-events/src/main/java/sm/selflearn/samskrtam/events/AnswerSubmitted.java
public record AnswerSubmitted(
    UUID    eventId,
    Instant occurredAt,
    UUID    userId,
    QuizType quizType,
    UUID    quizId,
    UUID    questionId,
    UUID    selectedOptionId,
    boolean isCorrect,
    int     responseTimeMs
) {
    public AnswerSubmitted(UUID userId, QuizType quizType, UUID quizId,
                           UUID questionId, UUID selectedOptionId,
                           boolean isCorrect, int responseTimeMs) {
        this(UUID.randomUUID(), Instant.now(), userId, quizType,
             quizId, questionId, selectedOptionId, isCorrect, responseTimeMs);
    }
}

// shared/kafka-events/src/main/java/sm/selflearn/samskrtam/events/SessionCompleted.java
public record SessionCompleted(
    UUID    eventId,
    Instant occurredAt,
    UUID    userId,
    QuizType quizType,
    UUID    quizId,
    int     score,
    int     totalQuestions,
    long    durationMs
) {
    public SessionCompleted(UUID userId, QuizType quizType, UUID quizId,
                            int score, int totalQuestions, long durationMs) {
        this(UUID.randomUUID(), Instant.now(), userId, quizType,
             quizId, score, totalQuestions, durationMs);
    }
}

// shared/kafka-events/src/main/java/sm/selflearn/samskrtam/events/QuizType.java
public enum QuizType {
    DECLENSIONS,
    CONJUGATIONS,
    VOCABULARY
}
```

---

## 4. Producer (Quiz Service — Java 21)

```java
@Service
public class QuizEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishAnswerSubmitted(AnswerSubmitted event) {
        kafkaTemplate.send("quiz.answer.submitted",
            event.userId().toString(), event);
    }

    public void publishSessionCompleted(SessionCompleted event) {
        kafkaTemplate.send("quiz.session.completed",
            event.userId().toString(), event);
    }
}
```

---

## 5. Consumer (Statistics Service — Java 21)

Consumer должен быть защищён от зависания при ошибках. Без `DefaultErrorHandler` Spring Kafka уйдёт в бесконечный retry на одном сообщении и встанет.

```java
// sm/selflearn/samskrtam/statistics/config/KafkaConsumerConfig.java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Retry: 3 попытки с экспоненциальным backoff (1s → 2s → 4s)
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1_000);
        backOff.setMultiplier(2.0);

        // После исчерпания retry — отправить в Dead Letter Topic (.dlt)
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // Не ретраить десериализационные ошибки — сразу в DLT
        handler.addNotRetryableExceptions(DeserializationException.class);

        return handler;
    }
}
```

```java
// sm/selflearn/samskrtam/statistics/consumer/AnswerSubmittedConsumer.java
@Component
@Slf4j
@RequiredArgsConstructor
public class AnswerSubmittedConsumer {

    private final StatisticsService statisticsService;

    @KafkaListener(topics = "quiz.answer.submitted", groupId = "statistics-service")
    public void handle(AnswerSubmitted event) {
        log.debug("Processing AnswerSubmitted: eventId={}, userId={}", event.eventId(), event.userId());
        // Virtual Threads — блокирующий вызов, JVM делает его неблокирующим
        statisticsService.recordAnswer(event);
        log.debug("AnswerSubmitted processed: eventId={}", event.eventId());
    }
}

// sm/selflearn/samskrtam/statistics/consumer/SessionCompletedConsumer.java
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionCompletedConsumer {

    private final StatisticsService statisticsService;

    @KafkaListener(topics = "quiz.session.completed", groupId = "statistics-service")
    public void handle(SessionCompleted event) {
        log.debug("Processing SessionCompleted: eventId={}, userId={}", event.eventId(), event.userId());
        statisticsService.recordSession(event);
        log.debug("SessionCompleted processed: eventId={}", event.eventId());
    }
}
```

```yaml
# application.yml (statistics-service) — регистрация errorHandler бина
spring:
  kafka:
    listener:
      # Spring подхватит бин DefaultErrorHandler автоматически
      # если он один в контексте
      observation-enabled: true
    consumer:
      group-id: statistics-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.ErrorHandlingDeserializer
      properties:
        spring.deserializer.value.delegate.class: >
          org.springframework.kafka.support.serializer.JsonDeserializer
        spring.json.trusted.packages: sm.selflearn.samskrtam.events
```

---

## 6. Структура shared/kafka-events

```
shared/kafka-events/
├── build.gradle.kts
└── src/main/java/
    └── sm/selflearn/samskrtam/events/
        ├── AnswerSubmitted.java   ← Java Record
        ├── SessionCompleted.java  ← Java Record
        └── QuizType.java          ← Enum
```

```kotlin
// shared/kafka-events/build.gradle.kts
plugins {
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
// Нет Spring зависимостей — только чистые Java Records
// Совместим с любым сервисом (Java или Kotlin)
```

---

## 7. Acceptance Criteria

- [ ] После каждого ответа — AnswerSubmitted в Kafka
- [ ] После завершения сессии — SessionCompleted в Kafka
- [ ] Statistics Service обрабатывает события независимо от Quiz Service
- [ ] При недоступности Statistics Service события ждут в Kafka
- [ ] При ошибке обработки — 3 retry с backoff, затем сообщение уходит в DLT
- [ ] Десериализационные ошибки идут в DLT без retry
- [ ] Повторная доставка события не создаёт дубликат в statistics-service (идемпотентность по eventId)
- [ ] Java Records в shared модуле совместимы с Kotlin dictionary-service

---

## 8. Открытые вопросы

- [ ] Schema Registry для версионирования схем?
- [x] Dead Letter Queue — реализован через `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`
- [ ] Notification Service как второй consumer тех же топиков?
- [ ] Мониторинг DLT топиков — алерт при появлении сообщений в DLT?
