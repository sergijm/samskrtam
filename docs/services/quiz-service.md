# quiz-service

> Домен: Quiz Sessions — прохождение квизов пользователем
> Язык: **Java 21 + WebFlux (Reactor)**
> Модуль: `services/quiz-service`
> Порт: 8082
> Status: **DRAFT**

---

## 1. Описание

Единый сервис для прохождения квизов всех типов: склонения, спряжения, лексика. Обрабатывает жизненный цикл сессии: старт, ответы, завершение. Детальная история ответов сохраняется в базе данных `quiz-service`. После завершения сессии публикует обогащенное событие в Kafka. Данные (вопросы, варианты, слова) получает от `content-service` через реактивный HTTP-клиент.

Разделение ответственности:
- **content-service** — что есть в квизах (данные, настройки)
- **quiz-service** — как пользователь их проходит (сессии, ответы, события)

### Стек

WebFlux выбран осознанно: quiz-service интенсивно работает с I/O одновременно — обращается к content-service, пишет в Postgres, публикует в Kafka. Реактивный pipeline позволяет держать всё это в одном неблокирующем потоке без thread-per-request overhead. Это противоположность content-service и statistics-service, где Virtual Threads дают ту же пропускную способность с меньшей сложностью — но там нет такого fan-out I/O.

> **Следствие:** весь стек — реактивный. JPA/Hibernate несовместимы с WebFlux — используется **R2DBC**. Kafka producer работает через реактивный `ReactiveKafkaProducerTemplate`. HTTP-клиент к content-service — `WebClient`.

### Хранение данных

| Хранилище | Что хранит | Зачем |
|---|---|---|
| PostgreSQL/R2DBC (схема `quiz`) | активные и завершённые сессии, ответы, вопросы сессий | надёжность, возможность продолжить сессию, хранение всех данных сессии |

---

## 2. Поддерживаемые типы квизов

---

## 3. Механика сессии

```
GET /api/v1/quiz/{type}/sessions/start[?quizId=uuid]
  ↓ WebClient → content-service /session-data (реактивно)
  ↓ для DECLENSIONS/CONJUGATIONS: генерирует N вопросов из sessionData.questions
  ↓ для VOCABULARY: генерирует N вопросов из sessionData.vocabularyWords (Sanskrit->Translation или Translation->Sanskrit)
  ↓ flatMap: сохраняет сессию в Postgres (R2DBC) в `quiz.quiz_sessions`
  ↓ flatMap: сохраняет сгенерированные вопросы в Postgres (R2DBC) в `quiz.session_questions`
  → Mono<StartSessionResponse>

GET /api/v1/quiz/{type}/sessions/{id}/resume
  ↓ R2DBC → Postgres, восстанавливает QuizSession
  ↓ R2DBC → Postgres, получает все SessionQuestion для сессии
  → Mono<ResumeSessionResponse>

POST /api/v1/quiz/{type}/sessions/{id}/answer
  ↓ R2DBC → Postgres, получает QuizSession
  ↓ R2DBC → Postgres, получает SessionQuestion для текущего вопроса
  ↓ проверяет ответ (для VOCABULARY учитывает targetLanguage, используя vocabularyWordsJson из QuizSession)
  ↓ flatMap: R2DBC сохраняет QuizAnswer + обновляет QuizSession (answered_questions, score)
  → Mono<AnswerResponse>

POST /api/v1/quiz/{type}/sessions/{id}/complete
  ↓ R2DBC → Postgres, получает QuizSession
  ↓ R2DBC → Postgres, получает все QuizAnswer для сессии
  ↓ R2DBC → Postgres, получает все SessionQuestion для сессии
  ↓ R2DBC обновляет статус QuizSession → COMPLETED
  ↓ Kafka publishSessionCompleted (с полной историей ответов и объяснениями)
  → Mono<CompleteSessionResponse>

GET /api/v1/quiz-sessions/progress?userId={userId}&quizId={quizId}
  ↓ R2DBC → Postgres, находит последнюю незавершенную сессию для userId и quizId
  → Mono<QuizProgressDto>
```

---

## 4. Зависимости

```kotlin
// services/quiz-service/build.gradle.kts
dependencies {
    implementation(libs.spring.boot.webflux)
    implementation(libs.spring.boot.r2dbc)
    implementation(libs.r2dbc.postgresql)           // io.r2dbc:r2dbc-postgresql
    // implementation(libs.spring.boot.data.redis.reactive) // Redis caching removed
    implementation(libs.spring.kafka)               // ReactiveKafkaProducerTemplate
    implementation(libs.flyway.core)                // Flyway — только для миграций (JDBC)
    implementation(libs.postgresql)                 // JDBC driver — только для Flyway
    implementation(libs.jackson.module.kotlin)
    implementation(project(":shared:quiz-dtos")) // Объединенный модуль
}
```

> **Flyway + R2DBC:** R2DBC не поддерживает Flyway напрямую. Решение — добавить JDBC datasource только для Flyway (отдельный бин `FlywayConfig`), а R2DBC использовать для всей бизнес-логики. Это стандартная практика.

---

## 5. Репозитории (ReactiveCrudRepository)

```java
// sm/selflearn/samskrtam/quiz/repository/QuizSessionRepository.java
public interface QuizSessionRepository
        extends ReactiveCrudRepository<QuizSession, UUID> {

    Flux<QuizSession> findByUserIdAndStatus(UUID userId, SessionStatus status);
    Mono<QuizSession> findByIdAndUserId(UUID id, UUID userId);
    Mono<QuizSession> findTopByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(UUID userId, UUID quizId, SessionStatus status); // NEW: Find by specific quizId
    // ... другие методы ...
}

// sm/selflearn/samskrtam/quiz/repository/QuizAnswerRepository.java
public interface QuizAnswerRepository
        extends ReactiveCrudRepository<QuizAnswer, UUID> {

    Flux<QuizAnswer> findBySessionId(UUID sessionId);
    Mono<Boolean> existsBySessionIdAndSessionQuestionId(UUID sessionId, UUID sessionQuestionId); // Corrected field name
}

// sm/selflearn/samskrtam/quiz/repository/SessionQuestionRepository.java // NEW
public interface SessionQuestionRepository extends ReactiveCrudRepository<SessionQuestion, UUID> {
    Flux<SessionQuestion> findBySessionId(UUID sessionId);
    Mono<SessionQuestion> findBySessionIdAndQuestionId(UUID sessionId, UUID questionId);
}
```

---

## 6. Кэш в Redis (Reactive)

**Удалено:** Кэширование сессий в Redis было удалено. Все данные сессии теперь хранятся в PostgreSQL.

---

## 7. ContentClient (WebClient)

```java
// sm/selflearn/samskrtam/quiz/service/ContentClient.java
@Component
public class ContentClient {

    private final WebClient webClient;
    private final String contentBaseUrl;

    public ContentClient(WebClient webClient, @Value("${content.service.url}") String contentBaseUrl) {
        this.webClient = webClient;
        this.contentBaseUrl = contentBaseUrl;
    }

    public Mono<SessionDataResponse> getSessionData(UUID quizId) {
        return webClient.get()
                .uri(contentBaseUrl + "/api/v1/content/quizzes/{id}/session-data", quizId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found in content-service: " + quizId)))
                .bodyToMono(SessionDataResponse.class);
    }

    // getDeclensionForms и getVocabularyWordsForQuiz теперь вызываются только из content-service
    // quiz-service получает все необходимые данные через getSessionData
}
```

---

## 8. Kafka (ReactiveKafkaProducerTemplate)

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
// sm/selflearn/samskrtam/quiz/event/OutboxEvent.java (R2DBC)
@Table("quiz.outbox_events") // Схема "quiz"
public class OutboxEvent {
    @Id
    private UUID id;
    private String aggregateId;   // userId — ключ партиции Kafka
    private String topic;         // quiz.answer.submitted / quiz.session.completed
    private String payload;       // JSON события
    private OutboxStatus status;        // PENDING / PROCESSED / FAILED
    private OutboxEventType eventType; // Added eventType field
    private Instant createdAt;
    private Instant processedAt;
    private int retryCount;
    private String errorMessage;
}
```

```sql
-- V1__initial_quiz_schema.sql (часть, относящаяся к outbox_events)
CREATE TABLE quiz.outbox_events (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id  VARCHAR(255) NOT NULL,
    topic         VARCHAR(255) NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(50)  NOT NULL,
    event_type    VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,
    retry_count   INT          NOT NULL DEFAULT 0,
    error_message TEXT,

    CONSTRAINT pk_quiz_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_status ON quiz.outbox_events (status);
CREATE INDEX idx_outbox_events_aggregate_id ON quiz.outbox_events (aggregate_id);
```

---

## 11. Пример реактивного pipeline (GrammarSessionService)

```java
// sm/selflearn/samskrtam/quiz/service/GrammarSessionService.java
public Mono<StartSessionResponse> startSession(UUID quizId, UUID userId, String userLocale) {
    return contentClient.getSessionData(quizId)
            .flatMap(sessionData -> {
                List<CachedQuestion> cachedQuestions;
                List<VocabularyWordDto> allVocabularyWords = new ArrayList<>();
                String vocabularyWordsJson = null;

                if (sessionData.getQuizType() == QuizType.VOCABULARY) {
                    // ... генерация вопросов и сериализация vocabularyWordsJson ...
                } else {
                    // ... маппинг вопросов ...
                }

                Collections.shuffle(cachedQuestions);

                QuizSession newSession = QuizSession.builder()
                        // ... поля ...
                        .vocabularyWordsJson(vocabularyWordsJson) // Сохраняем JSON слов
                        .build();

                return quizSessionRepository.save(newSession)
                        .flatMap(savedSession -> {
                            // Сохраняем сгенерированные вопросы в session_questions
                            List<SessionQuestion> sessionQuestions = cachedQuestions.stream()
                                    .map(cq -> SessionQuestion.builder()
                                            .sessionId(savedSession.getId())
                                            .questionId(cq.getQuestionId())
                                            // ... остальные поля из CachedQuestion ...
                                            .build())
                                    .collect(Collectors.toList());
                            return sessionQuestionRepository.saveAll(sessionQuestions)
                                    .then(buildStartSessionResponse(savedSession, cachedQuestions, allVocabularyWords, userLocale));
                        });
            });
}

public Mono<ResumeSessionResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
    return quizSessionRepository.findById(sessionId)
            .flatMap(session -> sessionQuestionRepository.findBySessionId(sessionId).collectList() // Получаем вопросы сессии
                    .flatMap(sessionQuestions -> {
                        // ... десериализация vocabularyWordsJson при необходимости ...
                        // ... маппинг SessionQuestion в CachedQuestion ...
                        return buildResumeSessionResponse(session, cachedQuestions, allVocabularyWords, userLocale);
                    }));
}

public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
    return quizSessionRepository.findById(sessionId)
            .flatMap(session -> quizAnswerRepository.existsBySessionIdAndSessionQuestionId(sessionId, request.getQuestionId())
                    .flatMap(alreadyAnswered -> {
                        if (alreadyAnswered) {
                            return Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId()));
                        } else {
                            return sessionQuestionRepository.findBySessionIdAndQuestionId(sessionId, request.getQuestionId()) // Получаем конкретный вопрос сессии
                                    .flatMap(sessionQuestion -> {
                                        // ... десериализация vocabularyWordsJson при необходимости ...
                                        // ... конвертация SessionQuestion в CachedQuestion ...
                                        // ... проверка ответа ...
                                        return quizAnswerRepository.save(newAnswer)
                                                .then(quizSessionRepository.incrementAnsweredQuestionsAndScore(sessionId, isCorrect))
                                                .thenReturn(AnswerResponse.builder()
                                                        // ... поля ответа ...
                                                        .build());
                                    });
                        }
                    }));
}

public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
    return quizSessionRepository.findById(sessionId)
            .flatMap(session -> Mono.zip(
                            quizAnswerRepository.findBySessionId(sessionId).collectList(),
                            sessionQuestionRepository.findBySessionId(sessionId).collectList() // Получаем все вопросы сессии
                    )
                    .flatMap(tuple -> {
                        // ... обработка ответов и вопросов для события Kafka ...
                        return quizSessionRepository.save(session)
                                .then(outboxMono)
                                .thenReturn(CompleteSessionResponse.builder()
                                        // ... поля ответа ...
                                        .build());
                    }));
}
```

---

## 12. Миграции базы данных

Все миграции для `quiz-service` объединены в один файл `V1__initial_quiz_schema.sql`.

```sql
-- services/quiz-service/src/main/resources/db/migration/V1__initial_quiz_schema.sql
-- Создание схемы quiz
CREATE SCHEMA IF NOT EXISTS quiz;

-- Создание таблицы quiz.quiz_sessions
CREATE TABLE quiz.quiz_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    quiz_id UUID NOT NULL,
    quiz_type VARCHAR(50) NOT NULL,
    total_questions INT NOT NULL,
    answered_questions INT NOT NULL,
    score INT NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    vocabulary_words_json TEXT -- Добавлено для хранения слов лексических квизов
);

CREATE INDEX idx_quiz_sessions_user_id ON quiz.quiz_sessions (user_id);
CREATE INDEX idx_quiz_sessions_quiz_id ON quiz.quiz_sessions (quiz_id);

-- Создание таблицы quiz.quiz_answers
CREATE TABLE quiz.quiz_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    session_question_id UUID NOT NULL, -- Ссылка на вопрос в session_questions
    selected_option_id UUID,
    correct BOOLEAN NOT NULL,
    response_time_ms INT NOT NULL,
    answered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    selected_form_iast VARCHAR(255),
    correct_form_iast VARCHAR(255),
    FOREIGN KEY (session_id) REFERENCES quiz.quiz_sessions(id) ON DELETE CASCADE
);

CREATE INDEX idx_quiz_answers_session_id ON quiz.quiz_answers (session_id);
CREATE INDEX idx_quiz_answers_session_question_id ON quiz.quiz_answers (session_question_id);

-- Создание таблицы quiz.session_questions
CREATE TABLE quiz.session_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL,
    question_id UUID, -- Оригинальный ID вопроса из content-service или сгенерированный
    text TEXT NOT NULL,
    explanation_ru TEXT,
    explanation_en TEXT,
    declension_stem_id UUID,
    target_case VARCHAR(50),
    target_number VARCHAR(50),
    correct_form_iast VARCHAR(255),
    correct_form_devanagari VARCHAR(255),
    vocabulary_word_id UUID,
    question_source_language VARCHAR(50),
    question_target_language VARCHAR(50),
    correct_translation_ru VARCHAR(255),
    correct_translation_en VARCHAR(255),
    FOREIGN KEY (session_id) REFERENCES quiz.quiz_sessions(id) ON DELETE CASCADE
);

CREATE INDEX idx_session_questions_session_id ON quiz.session_questions (session_id);

-- Создание таблицы quiz.outbox_events
CREATE TABLE quiz.outbox_events (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id  VARCHAR(255) NOT NULL,
    topic         VARCHAR(255) NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(50)  NOT NULL,
    event_type    VARCHAR(50)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,
    retry_count   INT          NOT NULL DEFAULT 0,
    error_message TEXT,

    CONSTRAINT pk_quiz_outbox PRIMARY KEY (id)
);

CREATE INDEX idx_outbox_events_status ON quiz.outbox_events (status);
CREATE INDEX idx_outbox_events_aggregate_id ON quiz.outbox_events (aggregate_id);
```