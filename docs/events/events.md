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

Ключ = userId — все события одного пользователя идут в один partition (порядок гарантирован).

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

```java
@Component
public class AnswerSubmittedConsumer {

    @KafkaListener(topics = "quiz.answer.submitted", groupId = "statistics-service")
    public void handle(AnswerSubmitted event) {
        // Virtual Threads — блокирующий вызов, JVM делает его неблокирующим
        statisticsService.recordAnswer(event);
    }
}
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
- [ ] Java Records в shared модуле совместимы с Kotlin dictionary-service

---

## 8. Открытые вопросы

- [ ] Schema Registry для версионирования схем?
- [ ] Dead Letter Queue для событий которые не удалось обработать?
- [ ] Notification Service как второй consumer тех же топиков?
