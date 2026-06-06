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

---

## 3. Механика сессии

```
GET /api/v1/quiz/{type}/sessions/start[?quizId=uuid]
  ↓ WebClient → content-service /session-data (реактивно)
  ↓ для DECLENSIONS/CONJUGATIONS: случайно выбирает N вопросов из sessionData.questions
  ↓ для VOCABULARY: случайно выбирает N слов из sessionData.vocabularyWords, генерирует вопросы (Sanskrit->Translation или Translation->Sanskrit)
  ↓ flatMap: сохраняет сессию в Postgres (R2DBC) + кладёт в Redis (Reactive)
  → Mono<StartSessionResponse>

GET /api/v1/quiz/{type}/sessions/{id}/resume
  ↓ Redis.get → если hit: возвращает из Redis
  ↓ если miss: R2DBC → Postgres, восстанавливает SessionCache → кладёт в Redis
  → Mono<ResumeSessionResponse>

POST /api/v1/quiz/{type}/sessions/{id}/answer
  ↓ Redis.get → correctOptionId
  ↓ проверяет ответ (для VOCABULARY учитывает targetLanguage)
  ↓ flatMap: R2DBC сохраняет QuizAnswer + Redis обновляет SessionCache
  → Mono<AnswerResponse>

POST /api/v1/quiz/{type}/sessions/{id}/complete
  ↓ R2DBC получает все QuizAnswer для сессии
  ↓ R2DBC обновляет статус → COMPLETED
  ↓ Redis.delete (сессия закрыта)
  ↓ Kafka publishSessionCompleted (с полной историей ответов)
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
    implementation(project(":shared:quiz-content-dtos")) // Добавлено для VocabularyWordDto, Gender, QuizType, Difficulty
}
```

> **Flyway + R2DBC:** R2DBC не поддерживает Flyway напрямую. Решение — добавить JDBC datasource только для Flyway (отдельный бин `FlywayConfig`), а R2DBC использовать для всей бизнес-логики. Это стандартная практика.

---

## 5. Репозитории (ReactiveCrudRepository)

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

## 6. Кэш в Redis (Reactive)

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
    private List<VocabularyWordDto> allVocabularyWords; // НОВОЕ ПОЛЕ: для лексических квизов
}

public class CachedQuestion {
    private UUID questionId;
    private String text;
    private String explanationRu;
    private String explanationEn;

    // Для квизов на склонения
    private UUID declensionStemId;
    private Case targetCase;
    private Number targetNumber;

    // Для лексических квизов
    private UUID vocabularyWordId;
    private QuestionLanguage questionSourceLanguage; // e.g., SANSKRIT, ENGLISH, RUSSIAN
    private QuestionLanguage questionTargetLanguage; // e.g., SANSKRIT, ENGLISH, RUSSIAN
    private String correctTranslationRu; // Правильный перевод на русский
    private String correctTranslationEn; // Правильный перевод на английский

    private String correctFormIast; // Правильная форма в IAST (для склонений или лексики)
    private String correctFormDevanagari; // Правильная форма в Деванагари (для склонений или лексики)
}

// sm/selflearn/samskrtam/quiz/model/QuestionLanguage.java
public enum QuestionLanguage {
    SANSKRIT,
    ENGLISH,
    RUSSIAN
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

    public Mono<List<DeclensionFormDto>> getDeclensionForms(UUID declensionStemId) {
        return webClient.get()
                .uri(contentBaseUrl + "/api/v1/content/declension-stems/{id}/forms", declensionStemId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("DECLENSION_STEM_NOT_FOUND", "Declension stem not found in content-service: " + declensionStemId)))
                .bodyToFlux(DeclensionFormDto.class)
                .collectList();
    }

    public Mono<List<VocabularyWordDto>> getVocabularyWordsForQuiz(UUID quizId, int limit) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(contentBaseUrl + "/api/v1/content/quizzes/{quizId}/vocabulary-words")
                        .queryParam("limit", limit)
                        .build(quizId))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        r -> Mono.error(new SamskrtamException("VOCABULARY_WORDS_NOT_FOUND", "Vocabulary words not found for quiz: " + quizId)))
                .bodyToFlux(VocabularyWordDto.class)
                .collectList();
    }
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

В `GrammarSessionService.submitAnswer()` теперь только сохранение ответа:

```java
// Внутри flatMap в GrammarSessionService.submitAnswer()
return quizAnswerRepository.save(newAnswer)
        .then(sessionCacheService.put(sessionId, cache))
        .thenReturn(AnswerResponse.builder()
                .isCorrect(isCorrect)
                .correctOptionId(request.getSelectedOptionId())
                .explanationRu(cachedQuestion.getExplanationRu())
                .explanationEn(cachedQuestion.getExplanationEn())
                .questionNumber(cache.getAnsweredQuestionIds().size())
                .totalQuestions(cache.getQuestions().size())
                .build());
```

---

## 11. Пример реактивного pipeline (GrammarSessionService)

```java
// sm/selflearn/samskrtam/quiz/service/GrammarSessionService.java
public Mono<StartSessionResponse> startSession(UUID quizId, UUID userId, String userLocale) {
    return contentClient.getSessionData(quizId)
            .flatMap(sessionData -> {
                List<CachedQuestion> cachedQuestions;
                List<VocabularyWordDto> allVocabularyWords = null;

                if (sessionData.getQuizType() == QuizType.VOCABULARY) {
                    if (sessionData.getVocabularyWords() == null || sessionData.getVocabularyWords().isEmpty()) {
                        return Mono.error(new SamskrtamException("NO_VOCABULARY_WORDS", "No vocabulary words found for quiz: " + quizId));
                    }
                    allVocabularyWords = sessionData.getVocabularyWords();
                    cachedQuestions = generateVocabularyQuestions(sessionData, userLocale);
                } else {
                    cachedQuestions = sessionData.getQuestions().stream()
                            .map(qr -> CachedQuestion.builder()
                                    .questionId(qr.getId())
                                    .text(qr.getText())
                                    .explanationRu(qr.getExplanationRu())
                                    .explanationEn(qr.getExplanationEn())
                                    .declensionStemId(qr.getDeclensionStemId())
                                    .targetCase(qr.getTargetCase())
                                    .targetNumber(qr.getTargetNumber())
                                    .correctFormIast(qr.getCorrectFormIast())
                                    .correctFormDevanagari(qr.getCorrectFormDevanagari())
                                    .build())
                            .collect(Collectors.toList());
                }

                Collections.shuffle(cachedQuestions);

                QuizSession newSession = QuizSession.builder()
                        .id(null)
                        .userId(userId)
                        .quizId(quizId)
                        .quizType(sessionData.getQuizType())
                        .totalQuestions(sessionData.getQuestionsPerSession())
                        .answeredQuestions(0)
                        .score(0)
                        .status(SessionStatus.IN_PROGRESS)
                        .startedAt(Instant.now())
                        .build();

                return quizSessionRepository.save(newSession)
                        .flatMap(savedSession -> {
                            SessionCache sessionCache = SessionCache.builder()
                                    .sessionId(savedSession.getId())
                                    .userId(userId)
                                    .quizId(quizId)
                                    .quizType(sessionData.getQuizType())
                                    .questions(cachedQuestions)
                                    .answeredQuestionIds(new HashSet<>())
                                    .score(0)
                                    .allVocabularyWords(allVocabularyWords)
                                    .build();
                            return sessionCacheService.put(savedSession.getId(), sessionCache)
                                    .then(buildStartSessionResponse(sessionCache, userLocale));
                        });
            });
}

public Mono<ResumeSessionResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
    return sessionCacheService.get(sessionId)
            .filter(cache -> cache.getUserId().equals(userId))
            .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
            .flatMap(cache -> buildResumeSessionResponse(cache, userLocale));
}

public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
    return sessionCacheService.get(sessionId)
            .filter(cache -> cache.getUserId().equals(userId))
            .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
            .filter(cache -> !cache.getAnsweredQuestionIds().contains(request.getQuestionId()))
            .switchIfEmpty(Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId())))
            .flatMap(cache -> {
                CachedQuestion cachedQuestion = cache.findQuestion(request.getQuestionId());
                boolean isCorrect = cachedQuestion.getCorrectFormIast().equals(getOptionIast(request.getSelectedOptionId(), cachedQuestion, userLocale, cache.getAllVocabularyWords()));

                QuizAnswer newAnswer = QuizAnswer.builder()
                        .id(null)
                        .sessionId(sessionId)
                        .questionId(request.getQuestionId())
                        .selectedOptionId(request.getSelectedOptionId())
                        .correctFormIast(cachedQuestion.getCorrectFormIast())
                        .correct(isCorrect)
                        .responseTimeMs(request.getResponseTimeMs())
                        .answeredAt(Instant.now())
                        .build();

                return quizAnswerRepository.save(newAnswer)
                        .then(sessionCacheService.put(sessionId, cache))
                        .thenReturn(AnswerResponse.builder()
                                .isCorrect(isCorrect)
                                .correctOptionId(request.getSelectedOptionId())
                                .explanationRu(cachedQuestion.getExplanationRu())
                                .explanationEn(cachedQuestion.getExplanationEn())
                                .questionNumber(cache.getAnsweredQuestionIds().size())
                                .totalQuestions(cache.getQuestions().size())
                                .build());
            });
}

public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
    return sessionCacheService.get(sessionId)
            .filter(cache -> cache.getUserId().equals(userId))
            .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
            .flatMap(cache -> quizSessionRepository.findById(sessionId)
                    .flatMap(session ->
                            quizAnswerRepository.findBySessionId(sessionId)
                                    .collectList()
                                    .flatMap(quizAnswers -> {
                                        List<AnswerData> answerDataList = quizAnswers.stream()
                                                .map(qa -> AnswerData.builder()
                                                        .questionId(qa.getQuestionId())
                                                        .selectedOptionId(qa.getSelectedOptionId())
                                                        .correctFormIast(qa.getCorrectFormIast())
                                                        .correct(qa.isCorrect())
                                                        .responseTimeMs(qa.getResponseTimeMs())
                                                        .answeredAt(Instant.now())
                                                        .build())
                                                .collect(Collectors.toList());

                                        session.setStatus(SessionStatus.COMPLETED);
                                        session.setCompletedAt(Instant.now());
                                        session.setScore(cache.getScore());
                                        session.setAnsweredQuestions(cache.getAnsweredQuestionIds().size());

                                        SessionCompleted event = SessionCompleted.builder()
                                                .userId(userId)
                                                .quizType(cache.getQuizType())
                                                .quizId(cache.getQuizId())
                                                .score(cache.getScore())
                                                .totalQuestions(cache.getQuestions().size())
                                                .durationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis())
                                                .answers(answerDataList)
                                                .build();

                                        Mono<Void> outboxMono = Mono.fromCallable(() -> {
                                            try {
                                                return objectMapper.writeValueAsString(event);
                                            } catch (JsonProcessingException e) {
                                                log.error("Error serializing SessionCompleted event: {}", event, e);
                                                throw new SamskrtamException("JSON_SERIALIZATION_ERROR", "Failed to serialize SessionCompleted event", e);
                                            }
                                        })
                                                .flatMap(payload -> outboxEventRepository.save(OutboxEvent.builder()
                                                        .id(null)
                                                        .aggregateId(userId.toString())
                                                        .topic("quiz.session.completed")
                                                        .payload(payload)
                                                        .status(OutboxStatus.PENDING)
                                                        .eventType(OutboxEventType.SESSION_COMPLETED)
                                                        .build()))
                                                .then();

                                        return quizSessionRepository.save(session)
                                                .then(sessionCacheService.evict(sessionId))
                                                .then(outboxMono)
                                                .thenReturn(CompleteSessionResponse.builder()
                                                        .sessionId(sessionId)
                                                        .score(cache.getScore())
                                                        .totalQuestions(cache.getQuestions().size())
                                                        .durationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis())
                                                        .build());
                                    })
                    ));
}

private Mono<StartSessionResponse> buildStartSessionResponse(SessionCache cache, String userLocale) {
    return Flux.fromIterable(cache.getQuestions())
            .flatMap(cachedQuestion -> {
                if (cache.getQuizType() == QuizType.VOCABULARY) {
                    VocabularyWordDto correctWord = cache.getAllVocabularyWords().stream()
                            .filter(w -> w.getId().equals(cachedQuestion.getVocabularyWordId()))
                            .findFirst()
                            .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found in cache for ID: " + cachedQuestion.getVocabularyWordId()));

                    return lexicalOptionGeneratorService.generateOptions(
                            correctWord,
                            cache.getAllVocabularyWords(),
                            cachedQuestion.questionSourceLanguage,
                            cachedQuestion.questionTargetLanguage,
                            userLocale
                    ).map(options -> QuestionDto.builder()
                            .id(cachedQuestion.getQuestionId())
                            .text(cachedQuestion.getText())
                            .options(options)
                            .build());
                } else {
                    return declensionOptionGeneratorService.generateOptions(
                            cachedQuestion.getDeclensionStemId(),
                            cachedQuestion.getTargetCase(),
                            cachedQuestion.getTargetNumber(),
                            cachedQuestion.getCorrectFormIast()
                    ).map(options -> QuestionDto.builder()
                            .id(cachedQuestion.getQuestionId())
                            .text(cachedQuestion.getText())
                            .options(options)
                            .build());
                }
            })
            .collectList()
            .map(questions -> StartSessionResponse.builder()
                    .sessionId(cache.getSessionId())
                    .quizId(cache.getQuizId())
                    .quizType(cache.getQuizType())
                    .questions(questions)
                    .totalQuestions(cache.getQuestions().size())
                    .build());
}

private Mono<ResumeSessionResponse> buildResumeSessionResponse(SessionCache cache, String userLocale) {
    return Flux.fromIterable(cache.getQuestions())
            .flatMap(cachedQuestion -> {
                if (cache.getQuizType() == QuizType.VOCABULARY) {
                    VocabularyWordDto correctWord = cache.getAllVocabularyWords().stream()
                            .filter(w -> w.getId().equals(cachedQuestion.getVocabularyWordId()))
                            .findFirst()
                            .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found in cache for ID: " + cachedQuestion.getVocabularyWordId()));

                    return lexicalOptionGeneratorService.generateOptions(
                            correctWord,
                            cache.getAllVocabularyWords(),
                            cachedQuestion.questionSourceLanguage,
                            cachedQuestion.questionTargetLanguage,
                            userLocale
                    ).map(options -> QuestionDto.builder()
                            .id(cachedQuestion.getQuestionId())
                            .text(cachedQuestion.getText())
                            .options(options)
                            .build());
                } else {
                    return declensionOptionGeneratorService.generateOptions(
                            cachedQuestion.getDeclensionStemId(),
                            cachedQuestion.getTargetCase(),
                            cachedQuestion.getTargetNumber(),
                            cachedQuestion.getCorrectFormIast()
                    ).map(options -> QuestionDto.builder()
                            .id(cachedQuestion.getQuestionId())
                            .text(cachedQuestion.getText())
                            .options(options)
                            .build());
                }
            })
            .collectList()
            .map(questions -> ResumeSessionResponse.builder()
                    .sessionId(cache.getSessionId())
                    .quizId(cache.getQuizId())
                    .quizType(cache.getQuizType())
                    .questions(questions)
                    .totalQuestions(cache.getQuestions().size())
                    .answeredQuestions(cache.getAnsweredQuestionIds().size())
                    .score(cache.getScore())
                    .currentQuestionIndex(cache.getAnsweredQuestionIds().size())
                    .build());
}

private List<CachedQuestion> generateVocabularyQuestions(SessionDataResponse sessionData, String userLocale) {
    List<VocabularyWordDto> availableWords = new ArrayList<>(sessionData.getVocabularyWords());
    Collections.shuffle(availableWords);

    int questionsToGenerate = Math.min(sessionData.getQuestionsPerSession(), availableWords.size());
    List<CachedQuestion> generatedQuestions = new ArrayList<>();

    for (int i = 0; i < questionsToGenerate; i++) {
        VocabularyWordDto word = availableWords.get(i);
        UUID questionId = UUID.randomUUID();
        QuestionLanguage sourceLang;
        QuestionLanguage targetLang;
        String questionText;
        String correctFormIast;
        String correctFormDevanagari = null;

        if (random.nextBoolean()) { // Sanskrit -> Translation
            sourceLang = QuestionLanguage.SANSKRIT;
            targetLang = userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH;
            questionText = String.format(
                    userLocale.equals("ru") ? "Как переводится слово '%s'?" : "How is the word '%s' translated?",
                    userLocale.equals("ru") ? word.getWordDevanagari() : word.getWordIast()
            );
            correctFormIast = targetLang == QuestionLanguage.RUSSIAN ? word.getTranslationRu() : word.getTranslationEn();
        } else { // Translation -> Sanskrit
            sourceLang = userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH;
            targetLang = QuestionLanguage.SANSKRIT;
            questionText = String.format(
                    userLocale.equals("ru") ? "Как будет '%s' на санскрите?" : "How is '%s' in Sanskrit?",
                    sourceLang == QuestionLanguage.RUSSIAN ? word.getTranslationRu() : word.getTranslationEn()
            );
            correctFormIast = word.getWordIast();
            correctFormDevanagari = word.getWordDevanagari();
        }

        generatedQuestions.add(CachedQuestion.builder()
                .questionId(questionId)
                .text(questionText)
                .explanationRu(word.getDictionaryEntry())
                .explanationEn(word.getDictionaryEntry())
                .vocabularyWordId(word.getId())
                .questionSourceLanguage(sourceLang)
                .questionTargetLanguage(targetLang)
                .correctTranslationRu(word.getTranslationRu())
                .correctTranslationEn(word.getTranslationEn())
                .correctFormIast(correctFormIast)
                .correctFormDevanagari(correctFormDevanagari)
                .build());
    }
    return generatedQuestions;
}

private String getOptionIast(UUID selectedOptionId, CachedQuestion cachedQuestion, String userLocale, List<VocabularyWordDto> allVocabularyWords) {
    if (cachedQuestion.getQuizType() == QuizType.VOCABULARY) {
        VocabularyWordDto selectedWord = allVocabularyWords.stream()
                .filter(w -> w.getId().equals(selectedOptionId))
                .findFirst()
                .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Selected vocabulary word not found: " + selectedOptionId));

        if (cachedQuestion.getQuestionTargetLanguage() == QuestionLanguage.SANSKRIT) {
            return selectedWord.getWordIast();
        } else if (cachedQuestion.getQuestionTargetLanguage() == QuestionLanguage.RUSSIAN) {
            return selectedWord.getTranslationRu();
        } else if (cachedQuestion.getQuestionTargetLanguage() == QuestionLanguage.ENGLISH) {
            return selectedWord.getTranslationEn();
        }
        return null;
    } else {
        return cachedQuestion.getCorrectFormIast();
    }
}
