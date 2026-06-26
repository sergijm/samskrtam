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

### Propagation между сервисами

`WebClient` автоматически добавляет заголовок `traceparent` при наличии `micrometer-tracing-bridge-otel` в classpath. Gateway пробрасывает `traceparent` входящего запроса во все downstream — также автоматически. Ручная настройка не требуется.

### Docker Compose — Observability сервисы

## 4. Actuator

### Management порты

Каждый сервис выносит Actuator на **свой** порт, настраиваемый через `.env`.
Это позволяет запускать все сервисы локально без конфликтов портов.
Gateway **не проксирует** management порты — они недоступны снаружи.

| Сервис | Env переменная | Порт по умолчанию |
|---|---|---|
| api-gateway | `GATEWAY_MANAGEMENT_PORT` | 9090 |
| feature-flag-service | `FEATURE_FLAG_SERVICE_PORT` | 9091 |
| user-service | `USER_MANAGEMENT_PORT` | 9092 |
| content-service | `CONTENT_MANAGEMENT_PORT` | 9093 |
| quiz-service | `QUIZ_MANAGEMENT_PORT` | 9094 |
| dictionary-service | `DICTIONARY_MANAGEMENT_PORT` | 9095 |
| statistics-service | `STATISTICS_MANAGEMENT_PORT` | 9096 |


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


---

## 5. Swagger / OpenAPI

### Зависимости

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

Все публичные эндпоинты аннотируются.

---

## 6. Обработка ошибок


### GlobalExceptionHandler — WebFlux (quiz-service, gateway)


### ErrorHandlingFilter — Gateway


---

## 7. API Design

### Пагинация

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


## 9. Docker

### Dockerfile (все Java-сервисы)


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

---

## 15. Kafka

### Именование топиков

Имена топиков должны следовать шаблону: `<domain>-<event>-events`.

| Топик | Описание |
|---|---|
| `quiz-answered-events` | События об ответах на вопросы квизов |
| `quiz-session-status-changed-events` | События об изменении статуса сессии квиза |
| `user-quiz-statistics-output` | Выходной топик для агрегированной статистики |

---

## 16. Архитектурные правила кода

> Эти правила применяются **с учётом стека сервиса**.
> Перед применением проверь: сервис использует JPA или R2DBC?
> Таблица в начале раздела — быстрый справочник.

### Стек по сервисам

| Сервис | ORM | Применимы JPA-связи | Применим @MappedSuperclass |
|---|---|---|---|
| content-service | JPA (Hibernate) | ✅ | ✅ |
| user-service | JPA (Hibernate) | ✅ | ✅ |
| statistics-service | JPA (Hibernate) | ✅ | ✅ |
| feature-flag-service | JPA (Hibernate) | ✅ | ✅ |
| quiz-service | **R2DBC** | ❌ | ❌ |
| dictionary-service | **R2DBC** | ❌ | ❌ |
| api-gateway | нет ORM | ❌ | ❌ |

---

### 16.1 Связи между сущностями (только JPA-сервисы)

Для маппинга связей используй JPA-аннотации, не `@Column` с UUID.

```java
// ✅ Правильно — связь через JPA
@Entity
public class Solution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @OneToMany(mappedBy = "solution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolutionSandhiRule> sandhiRules = new ArrayList<>();
}

// ❌ Неправильно — UUID вместо связи
@Entity
public class Solution {
    @Column(name = "task_id")
    private UUID taskId;  // нет типобезопасности, нет lazy loading
}
```

**`@ManyToMany` — когда использовать и когда нет:**

Используй `@ManyToMany` с `@JoinTable` только если у связи нет собственных полей
и она никогда их не получит:

```java
// ✅ @ManyToMany — связь без семантики, только ссылки
@ManyToMany
@JoinTable(
    name = "sandhi_rules_group_map",
    schema = "eamenau",
    joinColumns = @JoinColumn(name = "rule_id"),
    inverseJoinColumns = @JoinColumn(name = "group_id")
)
private List<SandhiRuleGroup> groups = new ArrayList<>();
```

Если у связи есть или могут появиться собственные поля (порядок, статус, дата) —
создавай отдельный entity-класс:

```java
// ✅ Отдельный entity — связь со своей семантикой
@Entity
@Table(name = "solution_sandhi_rules", schema = "eamenau")
public class SolutionSandhiRule {

    @EmbeddedId
    private SolutionSandhiRuleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("solutionId")
    private Solution solution;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sandhiRuleId")
    private SandhiRule sandhiRule;

    // В будущем сюда можно добавить: порядок применения, комментарий и т.д.
}

// ❌ Неправильно — @ManyToMany когда связь имеет семантику
// (нельзя будет добавить поля без полного рефакторинга)
```

**R2DBC (quiz-service, dictionary-service):**

В R2DBC нет поддержки `@OneToMany`, `@ManyToMany`, `@JoinColumn`.
Связи хранятся как UUID-поля и разрешаются вручную через отдельные запросы:

```java
// ✅ R2DBC — UUID-ссылка, JOIN вручную
@Table(name = "quiz_answers", schema = "quiz")
public class QuizAnswer {
    @Id
    private UUID id;
    private UUID sessionId;   // ← не @ManyToOne, просто UUID
    private UUID questionId;
}
```

---

### 16.2 Общие поля сущностей (@MappedSuperclass, только JPA)

Если несколько entity-классов имеют одинаковые поля — выноси их в
абстрактный суперкласс с `@MappedSuperclass`:

```java
// ✅ Базовый класс с общими полями
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

// ✅ Использование
@Entity
@Table(name = "quizzes", schema = "content")
public class Quiz extends BaseEntity {
    private String slug;
    private String titleRu;
    // id, createdAt, updatedAt — унаследованы
}
```

Кандидаты для `BaseEntity` в проекте: `Quiz`, `VocabularyWord`, `DeclensionStem`,
`Exercise`, `Task`, `Solution` — у всех есть `id`, у большинства логично иметь `createdAt`.

---

### 16.3 Интерфейсы для сервисов

Каждый сервисный класс должен реализовывать интерфейс.
Это уже принято в проекте (`LessonService → LessonServiceImpl`).
Применять везде:

```java
// ✅ Интерфейс — контракт
public interface VocabularyService {
    List<VocabularyWordDto> getVocabularyWordsForQuiz(String slug, int limit);
    List<VocabularyWordDto> getVocabularyWordsForQuizById(UUID quizId, int limit);
}

// ✅ Реализация — детали
@Service
@RequiredArgsConstructor
public class VocabularyServiceImpl implements VocabularyService {
    // ...
}
```

Исключение — сервисы-утилиты без альтернативных реализаций (`OutboxEventCreator`,
`QuizDataAssembler`) могут быть без интерфейса.

---

### 16.4 Разделение Entity и DTO

Entity-классы не выходят за пределы сервисного слоя.
Контроллеры принимают и возвращают только DTO.
Маппинг — в сервисном слое вручную или через выделенный mapper-класс:

```java
// ✅ Mapper-класс в сервисном слое
@Component
public class QuizMapper {

    public QuizSummaryDto toSummaryDto(Quiz quiz) {
        return QuizSummaryDto.builder()
                .id(quiz.getId())
                .slug(quiz.getSlug())
                .titleRu(quiz.getTitleRu())
                .titleEn(quiz.getTitleEn())
                .difficulty(quiz.getDifficulty())
                .build();
    }

    public Quiz toEntity(CreateQuizRequest request) {
        return Quiz.builder()
                .slug(request.getSlug())
                .titleRu(request.getTitleRu())
                .titleEn(request.getTitleEn())
                .build();
    }
}

// ❌ Неправильно — маппинг в контроллере
@GetMapping("/{id}")
public QuizSummaryDto getQuiz(@PathVariable UUID id) {
    Quiz quiz = quizRepository.findById(id)...;
    return new QuizSummaryDto(quiz.getId(), quiz.getSlug(), ...); // ← не сюда
}

// ❌ Неправильно — Entity в ответе контроллера
@GetMapping("/{id}")
public Quiz getQuiz(@PathVariable UUID id) { ... } // ← Entity утекает в API
```

---

### 16.5 Запрещённые паттерны

```java
// ❌ God-класс — сущность с 30+ полями без декомпозиции
@Entity
public class UserProfile {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String country;
    private String city;
    private String street;
    // ... ещё 25 полей
    // Решение: выделить Address как @Embeddable или отдельный @Entity
}

// ❌ Список ID вместо связи (только для JPA-сервисов)
@Entity
public class Exercise {
    @ElementCollection
    private List<UUID> taskIds; // ← нет lazy loading, нет типобезопасности
    // Решение: @OneToMany private List<Task> tasks
}

// ❌ Дублирование кода вместо общего суперкласса
@Entity public class Quiz    { private UUID id; private Instant createdAt; ... }
@Entity public class Lesson  { private UUID id; private Instant createdAt; ... }
// Решение: extends BaseEntity

// ❌ Публичные поля в Entity
@Entity
public class SandhiRule {
    public UUID id;       // ← всегда private + @Getter (Lombok) или геттеры
    public String text;
}
```