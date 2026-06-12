# Conventions

> Соглашения, обязательные для всех сервисов проекта.
> Связанные файлы: [architecture.md](./architecture.md) · [README.md](./README.md)
> Status: **UPDATED**

---

## 1. Конфигурация

### .env и application.yml

Все значения, специфичные для окружения, хранятся в корневом `.env` файле. В `application.yml` **не используются значения по умолчанию** — только ссылки на переменные окружения.

```yaml
# ✅ Правильно
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}

# ❌ Неправильно
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/samskrtam}
    username: ${DB_USER:samskrtam}
    password: ${DB_PASSWORD:secret}
```

`.env` находится в корне монорепо, добавлен в `.gitignore`. В репозитории хранится `.env.example` со всеми ключами и комментариями, но без значений.

### Структура .env

```bash
# ── Database ────────────────────────────────────────────
DB_HOST=postgres
DB_PORT=5432
DB_NAME=samskrtam
DB_USER=samskrtam
DB_PASSWORD=

# ── JDBC (content-service, user-service, statistics-service) ──
SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}

# ── R2DBC (quiz-service, dictionary-service) ────────────
SPRING_R2DBC_URL=r2dbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}

# ── Redis ───────────────────────────────────────────────
REDIS_HOST=redis
REDIS_PORT=6379

# ── Kafka ───────────────────────────────────────────────
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# ── Keycloak ────────────────────────────────────────────
KEYCLOAK_URL=http://keycloak:8080
KEYCLOAK_REALM=samskrtam
KEYCLOAK_CLIENT_ID=samskrtam-frontend
KEYCLOAK_CLIENT_SECRET=
KEYCLOAK_JWKS_URI=http://keycloak:8080/realms/samskrtam/protocol/openid-connect/certs

# ── Services ────────────────────────────────────────────
CONTENT_SERVICE_URL=http://content-service:8081
USER_SERVICE_URL=http://user-service:8087
DICTIONARY_SERVICE_URL=http://dictionary-service:8083
STATISTICS_SERVICE_URL=http://statistics-service:8084
FEATURE_FLAG_SERVICE_URL=http://feature-flag-service:8085
CORS_ALLOWED_ORIGINS=http://localhost:3000

# ── Observability ───────────────────────────────────────
MANAGEMENT_PORT=8099
OTEL_EXPORTER_OTLP_ENDPOINT=http://tempo:4318
TRACING_SAMPLING_PROBABILITY=1.0     # 1.0 = 100% dev, 0.1 = 10% prod

# ── Swagger ─────────────────────────────────────────────
SPRINGDOC_ENABLED=true               # false в production

# ── Connection Pool ─────────────────────────────────────
DB_POOL_MAX_SIZE=10
DB_POOL_MIN_IDLE=2
DB_POOL_CONNECTION_TIMEOUT_MS=3000

# ── Graceful Shutdown ───────────────────────────────────
GRACEFUL_SHUTDOWN_TIMEOUT=30s

# ── Spring ──────────────────────────────────────────────
SPRING_PROFILES_ACTIVE=default

# ── MinIO ───────────────────────────────────────────────
MINIO_URL=http://minio:9000
MINIO_PUBLIC_URL=http://localhost:9000
MINIO_ACCESS_KEY=
MINIO_SECRET_KEY=
MINIO_BUCKET_AVATARS=avatars
MINIO_BUCKET_DOCUMENTS=documents

# ── Quiz Service ────────────────────────────────────────
QUIZ_SERVICE_PORT=8082
APP_OUTBOX_FIXED_DELAY_MS=5000 # Интервал для OutboxEventPublisherService

# ── Statistics Service ──────────────────────────────────
STATISTICS_SERVICE_PORT=8086
SPRING_KAFKA_STREAMS_APPLICATION_ID=statistics-service-application
SPRING_KAFKA_STREAMS_STATE_DIR=/tmp/kafka-streams

# ── Frontend ────────────────────────────────────────────
VITE_API_URL=http://localhost:8090
```

### Spring Profiles

| Profile | Окружение | Swagger | Tracing sampling |
|---|---|---|---|
| `default` | локальная разработка | включён | 100% |
| `staging` | тестовый стенд | включён | 100% |
| `production` | prod | **выключен** | 10% |

Профиль активируется через `SPRING_PROFILES_ACTIVE` в `.env`.

---


### Адреса сервисов по окружениям

Для локальной разработки (IDEA, запуск сервисов вне Docker/Kubernetes) используются localhost-адреса:

```bash
CONTENT_SERVICE_URL=http://localhost:8081
USER_SERVICE_URL=http://localhost:8087
DICTIONARY_SERVICE_URL=http://localhost:8083
STATISTICS_SERVICE_URL=http://statistics-service:8084
FEATURE_FLAG_SERVICE_URL=http://feature-flag-service:8085
```

Для Kubernetes используются DNS-имена сервисов:

```bash
CONTENT_SERVICE_URL=http://content-service:8081
USER_SERVICE_URL=http://user-service:8087
DICTIONARY_SERVICE_URL=http://dictionary-service:8083
STATISTICS_SERVICE_URL=http://statistics-service:8084
FEATURE_FLAG_SERVICE_URL=http://feature-flag-service:8085
```


## 2. Логирование

### Обязательные правила

- Использовать **`@Slf4j`** (Lombok). `System.out.*` и `System.err.*` запрещены.
- Логи только в **JSON** формате через `logstash-logback-encoder` — для интеграции с Loki.
- Sensitive данные (email, пароли, токены, `userId` в URL) **не логируются ни на каком уровне**.
- В реактивном стеке (quiz-service, api-gateway) использовать `doOnError` для логирования с **сохранением** ошибки в pipeline, `onErrorResume` — только для обработки с подменой.
- Ошибки публикации в Kafka через Outbox Pattern должны логироваться с уровнем `ERROR` или `WARN` в зависимости от возможности повторной обработки.

### Уровни логирования

| Уровень | Когда использовать |
|---|---|
| `TRACE` | Вход в методы сервисного слоя, вызываемые из контроллеров |
| `DEBUG` | Результаты бизнес-решений: кэш-промах, восстановление сессии, выбор ветки логики |
| `INFO` | Старт/стоп сервиса, применение Flyway миграций, подключение к Kafka, успешная публикация Kafka-событий |
| `WARN` | Downstream сервис вернул 4xx, retry попытка, деградация функциональности, временные ошибки Kafka |
| `ERROR` | Downstream недоступен (5xx, timeout, connection refused), необработанное исключение, **ошибка публикации в Kafka после всех попыток**, ошибка обработки Kafka Streams |

### Шаблоны

```java
// ✅ Вход в метод сервиса (TRACE)
@Slf4j
@Service
public class SessionService {

    public Mono<StartSessionResponse> startSession(UUID quizId, UUID userId) {
        log.trace("startSession: quizId={}, userId={}", quizId, userId);
        // ...
    }
}

// ✅ Бизнес-решение (DEBUG)
log.debug("Cache miss for sessionId={}, restoring from Postgres", sessionId);

// ✅ Ошибка в реактивном стеке — doOnError логирует, не прерывает pipeline
return cacheService.get(sessionId)
        .doOnError(e -> log.error("Redis unavailable, sessionId={}", sessionId, e))
        .onErrorResume(e -> restoreFromPostgres(sessionId));

// ✅ Downstream недоступен (ERROR)
.onStatus(HttpStatusCode::is5xxServerError, response ->
        response.bodyToMono(String.class)
                .doOnNext(body -> log.error(
                        "content-service error: status={}, body={}, traceId={}",
                        response.statusCode(), body, MDC.get("traceId")))
                .flatMap(body -> Mono.error(new ContentServiceException(body)))
)

// ✅ Downstream вернул 4xx (WARN — штатная ситуация)
.onStatus(HttpStatusCode::is4xxClientError, response -> {
        log.warn("content-service 4xx: status={}, quizId={}", response.statusCode(), quizId);
        return Mono.error(new QuizNotFoundException(quizId));
})

// ❌ Запрещено
System.out.println("Starting session");
log.info("User email: {}", user.getEmail());   // sensitive data
```

### Формат JSON (logback-spring.xml)

```xml
<configuration>
  <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
      <providers>
        <mdc/>           <!-- включает traceId, spanId из MDC -->
        <arguments/>
        <logLevel/>
        <loggerName/>
        <message/>
        <stackTrace/>
      </providers>
      <customFields>{"service":"${spring.application.name}"}</customFields>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="JSON"/>
  </root>

  <logger name="sm.selflearn.samskrtam" level="DEBUG"/>
</configuration>
```

---

## 3. Сквозная трассировка (Micrometer Tracing)

### Стек

```
Micrometer Tracing → OpenTelemetry → Grafana Tempo   (трейсы)
Logback JSON → Promtail → Grafana Loki               (логи с traceId)
Micrometer → /actuator/prometheus → Prometheus       (метрики)
Grafana                                              (единый UI)
```

### Зависимости (все Java-сервисы)

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.logstash.logback.encoder)
}
```

```toml
# gradle/libs.versions.toml
[versions]
micrometer-tracing = "1.3.0"
opentelemetry       = "1.38.0"
logstash-logback    = "7.4"

[libraries]
micrometer-tracing-bridge-otel = { module = "io.micrometer:micrometer-tracing-bridge-otel", version.ref = "micrometer-tracing" }
opentelemetry-exporter-otlp    = { module = "io.opentelemetry:opentelemetry-exporter-otlp",  version.ref = "opentelemetry" }
micrometer-registry-prometheus = { module = "io.micrometer:micrometer-registry-prometheus",  version.ref = "micrometer-tracing" }
logstash-logback-encoder       = { module = "net.logstash.logback:logstash-logback-encoder", version.ref = "logstash-logback" }
```

### application.yml (общий блок, все сервисы)

```yaml
management:
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY}
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT}
  propagation:
    type: w3c    # W3C traceparent — современный стандарт
```

### Virtual Threads — MDC работает стандартно

В сервисах на Virtual Threads (`content-service`, `user-service`, `statistics-service`) `traceId` автоматически попадает в MDC через Micrometer. Дополнительных настроек не требуется.

### WebFlux — ReactorContextAccessor

В сервисах на WebFlux (`api-gateway`, `quiz-service`) MDC не работает через ThreadLocal. Требуется `ReactorContextAccessor`:

```java
// config/TracingConfig.java (только WebFlux сервисы)
@Configuration
public class TracingConfig {

    @Bean
    public ReactorContextAccessor reactorContextAccessor() {
        // Пробрасывает traceId/spanId из Reactor Context в MDC
        return new ReactorContextAccessor();
    }
}
```

Без этого бина `traceId` в логах quiz-service и gateway будет `null`.

### Propagation между сервисами

`WebClient` автоматически добавляет заголовок `traceparent` при наличии `micrometer-tracing-bridge-otel` в classpath. Gateway пробрасывает `traceparent` входящего запроса во все downstream — также автоматически. Ручная настройка не требуется.

### Docker Compose — Observability сервисы

```yaml
  tempo:
    image: grafana/tempo:2.5.0
    ports: ["3200:3200", "4318:4318"]   # 4318 = OTLP HTTP
    command: ["-config.file=/etc/tempo.yaml"]
    volumes:
      - ./infrastructure/tempo/tempo.yaml:/etc/tempo.yaml

  prometheus:
    image: prom/prometheus:v2.52.0
    ports: ["9090:9090"]
    volumes:
      - ./infrastructure/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml

  loki:
    image: grafana/loki:3.0.0
    ports: ["3100:3100"]

  promtail:
    image: grafana/promtail:3.0.0
    volumes:
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - ./infrastructure/promtail/promtail.yaml:/etc/promtail/config.yml

  grafana:
    image: grafana/grafana:11.0.0
    ports: ["3001:3000"]
    environment:
      GF_AUTH_ANONYMOUS_ENABLED: "true"
      GF_AUTH_ANONYMOUS_ORG_ROLE: Admin
    volumes:
      - ./infrastructure/grafana/datasources:/etc/grafana/provisioning/datasources
      - ./infrastructure/grafana/dashboards:/etc/grafana/provisioning/dashboards
```

---

## 4. Actuator

### Management порты

Каждый сервис выносит Actuator на **свой** порт, настраиваемый через `.env`.
Это позволяет запускать все сервисы локально без конфликтов портов.
Gateway **не проксирует** management порты — они недоступны снаружи.

| Сервис | Env переменная | Порт по умолчанию |
|---|---|---|
| api-gateway | `GATEWAY_MANAGEMENT_PORT` | 9090 |
| feature-flag-service | `FEATURE_FLAG_MANAGEMENT_PORT` | 9091 |
| user-service | `USER_MANAGEMENT_PORT` | 9092 |
| content-service | `CONTENT_MANAGEMENT_PORT` | 9093 |
| quiz-service | `QUIZ_MANAGEMENT_PORT` | 9094 |
| dictionary-service | `DICTIONARY_MANAGEMENT_PORT` | 9095 |
| statistics-service | `STATISTICS_MANAGEMENT_PORT` | 9096 |

```yaml
# application.yml (шаблон для всех сервисов)
# Значение ${SERVICE_MANAGEMENT_PORT} — своё для каждого сервиса
management:
  server:
    port: ${SERVICE_MANAGEMENT_PORT}
  endpoints:
    web:
      exposure:
        include: health, info, prometheus, loggers
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never
      group:
        liveness:
          include: livenessState, diskSpace
        readiness:
          include: readinessState, db, redis, kafka
```

В `.env` (локальная разработка):
```
GATEWAY_MANAGEMENT_PORT=9090
FEATURE_FLAG_SERVICE_PORT=9091
USER_MANAGEMENT_PORT=9092
CONTENT_MANAGEMENT_PORT=9093
QUIZ_MANAGEMENT_PORT=9094
DICTIONARY_MANAGEMENT_PORT=9095
STATISTICS_MANAGEMENT_PORT=9096
```

В k8s каждый Deployment объявляет `containerPort` со своим значением из ConfigMap.
Prometheus scrape targets используют конкретные порты каждого сервиса (см. ниже).

### Health Groups

| Probe | Эндпоинт | Включает | Поведение k8s |
|---|---|---|---|
| Liveness | `/actuator/health/liveness` | JVM alive, disk space | Restart пода при падении |
| Readiness | `/actuator/health/readiness` | DB, Redis, Kafka | Вывод из rotation при падении |

> Liveness не включает DB и Kafka — временная недоступность внешних зависимостей не должна вызывать рестарт пода.

### Prometheus scrape

```yaml
# infrastructure/prometheus/prometheus.yml
scrape_configs:
  - job_name: 'samskrtam-services'
    static_configs:
      - targets:
          - 'api-gateway:9090'
          - 'feature-flag-service:9091'
          - 'user-service:9092'
          - 'content-service:9093'
          - 'quiz-service:9094'
          - 'dictionary-service:9095'
          - 'statistics-service:9096'
    metrics_path: /actuator/prometheus
```

### Кастомные метрики

| Сервис | Метрика | Тип | Теги |
|---|---|---|---|
| quiz-service | `quiz.answers.total` | Counter | `quizType`, `correct` |
| quiz-service | `quiz.session.duration` | Timer | `quizType` |
| quiz-service | `quiz.cache.miss` | Counter | — |
| statistics-service | `kafka.events.processed` | Counter | `topic` |
| dictionary-service | `dictionary.cache.hit` | Counter | — |

```java
// Пример регистрации кастомной метрики
@Service
@RequiredArgsConstructor
public class SessionService {

    private final MeterRegistry meterRegistry;

    private void recordAnswer(String quizType, boolean correct) {
        Counter.builder("quiz.answers.total")
                .tag("quizType", quizType)
                .tag("correct", String.valueOf(correct))
                .register(meterRegistry)
                .increment();
    }
}
```

---

## 5. Swagger / OpenAPI

### Зависимости

```kotlin
// WebMVC (content-service, user-service, statistics-service)
implementation(libs.springdoc.openapi.webmvc)

// WebFlux (api-gateway, quiz-service)
implementation(libs.springdoc.openapi.webflux)
```

> ⚠️ **Критично:** `webmvc` и `webflux` варианты несовместимы. Подключение `webmvc` в WebFlux сервис вызывает `ClassCastException` при старте.

### application.yml

```yaml
springdoc:
  api-docs:
    enabled: ${SPRINGDOC_ENABLED}
    path: /api-docs
  swagger-ui:
    enabled: ${SPRINGDOC_ENABLED}
    path: /swagger-ui.html
```

### Агрегация в Gateway

```yaml
# application.yml (api-gateway)
springdoc:
  swagger-ui:
    enabled: ${SPRINGDOC_ENABLED}
    urls:
      - { name: user-service,       url: "http://user-service:8087/api-docs" }
      - { name: content-service,    url: "http://content-service:8081/api-docs" }
      - { name: quiz-service,       url: "http://quiz-service:8082/api-docs" }
      - { name: dictionary-service, url: "http://dictionary-service:8085/api-docs" }
      - { name: statistics-service, url: "http://statistics-service:8086/api-docs" }
```

Swagger UI: `http://localhost:8090/swagger-ui.html`

### Обязательные аннотации

Все публичные эндпоинты аннотируются. Минимальный набор:

```java
@RestController
@Tag(name = "Declensions Quiz", description = "Квиз по склонениям санскрита")
public class DeclensionsSessionController {

    @GetMapping("/sessions/start")
    @Operation(summary = "Начать сессию квиза")
    @ApiResponse(responseCode = "200", description = "Сессия создана")
    @ApiResponse(responseCode = "401", description = "Не аутентифицирован")
    @ApiResponse(responseCode = "404", description = "Квиз не найден",
                 content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Mono<StartSessionResponse> startSession(...) { }
}
```

---

## 6. Обработка ошибок

### Кастомные исключения

```java
// shared/common-dto
public abstract class SamskrtamException extends RuntimeException {
    private final String errorCode;

    protected SamskrtamException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

// Примеры в quiz-service
public class QuizNotFoundException     extends SamskrtamException {
    public QuizNotFoundException(UUID id)     { super("QUIZ_NOT_FOUND",     "Quiz not found: " + id); }
}
public class SessionNotFoundException  extends SamskrtamException {
    public SessionNotFoundException(UUID id)  { super("SESSION_NOT_FOUND",  "Session not found: " + id); }
}
public class AlreadyAnsweredException  extends SamskrtamException {
    public AlreadyAnsweredException(UUID id)  { super("ALREADY_ANSWERED",   "Already answered: " + id); }
}
```

### Единый ErrorResponse

```java
// shared/common-dto
public record ErrorResponse(
    String  errorCode,    // машиночитаемый код
    String  message,      // человекочитаемое сообщение
    String  traceId,      // для корреляции с логами
    Instant timestamp
) {
    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, MDC.get("traceId"), Instant.now());
    }
}
```

```json
{
  "errorCode": "QUIZ_NOT_FOUND",
  "message": "Quiz not found: 3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "timestamp": "2025-05-30T12:00:00Z"
}
```

### GlobalExceptionHandler — WebMVC

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SamskrtamException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(SamskrtamException ex) {
        log.warn("Business exception: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(resolveStatus(ex))
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("INTERNAL_ERROR", "Internal server error"));
    }

    private HttpStatus resolveStatus(SamskrtamException ex) {
        return switch (ex.getErrorCode()) {
            case "QUIZ_NOT_FOUND", "SESSION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "ALREADY_ANSWERED"                    -> HttpStatus.CONFLICT;
            default                                    -> HttpStatus.BAD_REQUEST;
        };
    }
}
```

### GlobalExceptionHandler — WebFlux (quiz-service, gateway)

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SamskrtamException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(SamskrtamException ex) {
        log.warn("Business exception: code={}", ex.getErrorCode());
        return ResponseEntity.status(resolveStatus(ex))
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.of("INTERNAL_ERROR", "Internal server error"));
    }
}
```

### ErrorHandlingFilter — Gateway

```java
@Component
@Slf4j
public class ErrorHandlingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
                .then(Mono.defer(() -> {
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    String path    = exchange.getRequest().getPath().toString();
                    String traceId = exchange.getRequest().getHeaders().getFirst("traceparent");

                    if (status != null && status.is5xxServerError()) {
                        log.error("Downstream 5xx: status={}, path={}, traceId={}",
                                status, path, traceId);
                    } else if (status != null && status.is4xxClientError()
                            && !status.equals(HttpStatus.UNAUTHORIZED)) {
                        log.warn("Downstream 4xx: status={}, path={}, traceId={}",
                                status, path, traceId);
                    }
                    return Mono.empty();
                }))
                .onErrorResume(ex -> {
                    // Downstream недоступен — connection refused, timeout
                    log.error("Downstream unreachable: path={}, error={}",
                            exchange.getRequest().getPath(), ex.getMessage(), ex);
                    exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() { return Ordered.LOWEST_PRECEDENCE; }
}
```

---

## 7. API Design

### Пагинация

```java
// shared/common-dto
public record PageResponse<T>(
    List<T> content,
    int     page,
    int     size,
    long    totalElements,
    int     totalPages,
    boolean last
) {}
```

Параметры запроса: `page` (0-based), `size` (default 20, max 100), `sort` (`field,asc|desc`).

```
GET /api/v1/content/quizzes?page=0&size=20&sort=createdAt,desc
→ PageResponse<QuizSummaryResponse>
```

### Версионирование API

Текущая версия: `v1` в path. Правила:
- Обратно совместимые изменения (новые поля, новые эндпоинты) — без смены версии.
- Ломающие изменения — новый префикс `/api/v2/`, `v1` поддерживается параллельно минимум один релиз.

### Rate Limiting (Gateway)

```yaml
auth/**:  replenishRate=5,  burstCapacity=10   # защита от brute force
api/**:   replenishRate=20, burstCapacity=40   # стандартный лимит
```

### CORS (Gateway)

```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}
  allowed-methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
  allowed-headers: Authorization, Content-Type, X-Request-Id
  allow-credentials: true
  max-age: 3600
```

---

## 8. Тесты

### Структура

```
src/test/java/sm/selflearn/samskrtam/{service}/
├── unit/
│   ├── service/       ← JUnit 5 + Mockito, без Spring контекста
│   └── util/
├── integration/
│   ├── api/           ← MockMvc / WebTestClient, HTTP контракты
│   └── repository/    ← Testcontainers, реальная БД
└── arch/              ← ArchUnit
```

### Именование

```java
// methodName_stateUnderTest_expectedBehavior
void startSession_quizNotFound_returns404() {}
void submitAnswer_alreadyAnswered_returnsConflict() {}
void getEntry_cacheHit_doesNotCallExternalApi() {}
```

### Покрытие (JaCoCo)

Минимальный порог для классов сервисного слоя — **80%**.

```kotlin
// build.gradle.kts
tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            element = "CLASS"
            includes = listOf("sm.selflearn.samskrtam.*.service.*")
            limit { minimum = "0.80".toBigDecimal() }
        }
    }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
```

### Обязательные тест-кейсы

| Сервис | Сценарий |
|---|---|
| quiz-service | старт сессии, верный/неверный ответ, fallback Redis→Postgres, дубликат ответа, **сохранение события в Outbox-таблицу** |
| content-service | CRUD квиза, session-data, STUDENT получает 403 на write, **генерация VOCABULARY квизов по иерархии категорий** |
| user-service | логин (успех/неверный пароль), регистрация (дубликат email) |
| statistics-service | **агрегация статистики из Kafka-событий с помощью Kafka Streams** |
| dictionary-service | cache hit, cache miss + внешний запрос, внешний API недоступен |
| api-gateway | нет JWT → 401, STUDENT на /content → 403, rate limit → 429 |

### ArchUnit

```java
@AnalyzeClasses(packages = "sm.selflearn.samskrtam")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule controllers_do_not_access_repositories =
            noClasses().that().haveNameMatching(".*Controller")
                    .should().accessClassesThat().haveNameMatching(".*Repository");

    @ArchTest
    static final ArchRule services_do_not_depend_on_controllers =
            noClasses().that().haveNameMatching(".*Service")
                    .should().dependOnClassesThat().haveNameMatching(".*Controller");

    @ArchTest
    static final ArchRule no_system_out =
            noClasses().should()
                    .callMethod(System.class, "out")
                    .orShould().callMethod(System.class, "err");
}
```

---

## 9. Docker

### Dockerfile (все Java-сервисы)

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew settings.gradle.kts gradle/ ./
COPY services/${SERVICE}/build.gradle.kts services/${SERVICE}/
RUN ./gradlew :services/${SERVICE}:dependencies --no-daemon
COPY services/${SERVICE}/src services/${SERVICE}/src
RUN ./gradlew :services/${SERVICE}:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app
COPY --from=builder /app/services/${SERVICE}/build/libs/*.jar app.jar
EXPOSE 8080 ${SERVICE_MANAGEMENT_PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Правила: runtime образ — `jre-alpine` (не `jdk`), запуск от non-root пользователя `app`, multi-stage build.

### Graceful Shutdown

```yaml
# application.yml (все сервисы)
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: ${GRACEFUL_SHUTDOWN_TIMEOUT}
```

### Connection Pool

```yaml
# HikariCP (content-service, user-service, statistics-service)
spring:
  datasource:
    hikari:
      maximum-pool-size: ${DB_POOL_MAX_SIZE}
      minimum-idle: ${DB_POOL_MIN_IDLE}
      connection-timeout: ${DB_POOL_CONNECTION_TIMEOUT_MS}

# R2DBC Pool (quiz-service, dictionary-service)
spring:
  r2dbc:
    pool:
      max-size: ${DB_POOL_MAX_SIZE}
      initial-size: ${DB_POOL_MIN_IDLE}
```

---

## 10. Качество кода

### Checkstyle

```kotlin
// build.gradle.kts (root)
allprojects {
    apply(plugin = "checkstyle")
    checkstyle {
        toolVersion = "10.17.0"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
    }
}
```

`config/checkstyle/checkstyle.xml` — Google Java Style, отступ 4 пробела.

### SpotBugs

```kotlin
plugins { id("com.github.spotbugs") version "6.0.9" }

spotbugs {
    effort = com.github.spotbugs.snom.Effort.MAX
    reportLevel = com.github.spotbugs.snom.Confidence.MEDIUM
    excludeFilter = rootProject.file("config/spotbugs/exclude.xml")
}
```

### Порядок проверок в CI

```
test → jacocoTestReport → jacocoTestCoverageVerification → checkstyleMain → spotbugsMain
```

Сборка падает при нарушении любого из этапов.

---

## 11. Git Conventions

### Conventional Commits

```
<type>(<scope>): <description>
```

| Type | Когда |
|---|---|
| `feat` | новая функциональность |
| `fix` | исправление бага |
| `docs` | только документация |
| `refactor` | рефакторинг без изменения поведения |
| `test` | тесты |
| `chore` | зависимости, конфигурация CI |
| `perf` | оптимизация производительности |

Scope — имя сервиса: `quiz-service`, `gateway`, `content-service`, `shared`.

```bash
feat(quiz-service): add session resume endpoint
fix(gateway): return 502 when downstream unreachable
docs(conventions): add logging rules
test(statistics-service): add Kafka consumer integration test
chore(deps): bump springdoc to 2.5.0
```

### Branching

```
main        ← стабильная ветка, деплой в production
├── feat/*  ← новая функциональность
├── fix/*   ← исправления
└── chore/* ← технический долг, зависимости
```

PR в `main` требует: прохождения CI + одного code review.

---

## 12. Makefile

```makefile
.PHONY: dev infra observe migrate test coverage lint check build down clean

dev:       ## Запустить все сервисы
	docker compose up -d

infra:     ## Только инфраструктура (БД, Kafka, Redis, Keycloak, MinIO)
	docker compose up -d postgres kafka redis keycloak minio

observe:   ## Observability стек (Tempo, Prometheus, Loki, Grafana)
	docker compose up -d tempo prometheus loki promtail grafana

migrate:   ## Применить Flyway миграции
	./gradlew flywayMigrate

test:      ## Запустить все тесты
	./gradlew test

coverage:  ## Тесты + отчёт о покрытии
	./gradlew test jacocoTestReport
	@echo "Report: build/reports/jacoco/test/html/index.html"

lint:      ## Статический анализ
	./gradlew checkstyleMain spotbugsMain

check:     ## Полная проверка (как в CI)
	./gradlew check

build:     ## Собрать все образы
	./gradlew bootJar && docker compose build

down:      ## Остановить всё
	docker compose down

clean:     ## Сбросить volumes (БД, Kafka)
	docker compose down -v
```

---

## 13. Открытые вопросы

- [ ] Secrets management в k8s — Kubernetes Secrets достаточно или нужен HashiCorp Vault?
- [ ] CHANGELOG — вести вручную или генерировать из Conventional Commits (semantic-release)?
- [ ] Grafana дашборды — версионировать как JSON в репозитории или настраивать вручную?
- [ ] ArchUnit тесты — выносить в `shared/arch-rules` или дублировать в каждом сервисе?
- [ ] Testcontainers — использовать reuse mode для ускорения локальных тестов?
- [x] k8s NetworkPolicy — ограничить доступ к сервисам только от Gateway: реализовано, см. [api-gateway.md](services/api-gateway.md) раздел 11.

---

## 14. Архитектурные решения (ADR)

### ADR-001: Разделение auth между Gateway и user-service

**Статус:** Принято, реализовано в спецификациях (api-gateway.md, user-service.md)

**Контекст:** Изначально user-service проксировал все операции с токенами (login, refresh, logout, OAuth2 callback). Это избыточно — Spring Cloud Gateway нативно поддерживает OAuth2 Client на WebFlux.

**Решение:**

| Компонент | Ответственность |
|---|---|
| **Gateway** | OAuth2/OIDC протокол: login (ROPC), refresh, logout, редиректы на Google/Mail.ru, обмен `code` на токен (Authorization Code flow) |
| **user-service** | Жизненный цикл аккаунта: регистрация, восстановление пароля, смена пароля, верификация email, invite flow |

**Граница:** всё что связано с OAuth2/OIDC протоколом — Gateway (конфигурация Spring Security). Всё что связано с бизнес-логикой управления аккаунтом — user-service (Keycloak Admin REST API).

**Следствие:** user-service не хранит токены и не проксирует OAuth2 запросы. Gateway не знает про бизнес-правила регистрации.

Спецификации api-gateway.md и user-service.md отражают это решение.
