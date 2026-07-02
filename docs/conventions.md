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
| quiz-service | `quiz.answers.total` | Counter | `lessonType`, `correct` |
| quiz-service | `quiz.session.duration` | Timer | `lessonType` |
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

### ADR-002: Семантика Quiz vs Lesson vs Activity

**Статус:** Принято

**Контекст:** Термин «quiz» использовался для обозначения и урока (единицы контента) и квиза (набора вопросов). Одновременно принято решение о будущей абстракции Activity.

**Решение:**
- **Lesson** = единица контента (`A_STEM_DECLENSIONS`, словарный урок)
- **Quiz** = конкретная активность: случайная выборка вопросов из урока на одну сессию
- **QuizSession** = прохождение квиза пользователем — правильное название, не трогать
- **Activity** = будущая абстракция над `Quiz`, `Flashcard`, `RecallExercise` и др. (реализация в M5+)

**Следствие:**
- `QuizRepository`/`QuizContentService` переименовываются в `LessonRepository`/`LessonContentService`
- `QuizSession`/`QuizAnswer` — не переименовываются (семантически верны)
- `quizId` в контексте статистики, указывающий на урок, переименовывается в `lessonId`
- Kafka топики `quiz-answered-events`, `quiz-session-status-changed-events` — не переименовываются
- Роут `/api/v1/quiz/` остаётся неизменным (принадлежит quiz-service)
- `QuizListItemResponse` удаляется как дубль `LessonItemResponse`

### ADR-003: Хранение окончаний склонений в БД

**Статус:** Принято

**Контекст:** Для уроков склонений (declensions) необходима эталонная таблица окончаний, по которой quiz-service может:
1. Строить правильные ответы для каждого вопроса (caseType + numberType + gender).
2. Проверять ответы пользователя, сравнивая с эталоном.
3. Гибко поддерживать разные окончания для мужского и женского рода в рамках одного типа гласной (если потребуется).

**Решение:**
Окончания хранятся в базе данных в таблице `case_endings` со схемой:

| Колонка | Тип | Описание |
|---|---|---|
| `vowel_type` | varchar | Тип тематической гласной (`A_MASC`, `A_NEUT`, `A_FEM`, `I`, `I_LONG`, `U`, `U_LONG`, `R`) |
| `gender` | varchar | Грамматический род (`MASCULINE`, `FEMININE`, `NEUTER`, `UNSPECIFIED`) |
| `case_type` | varchar | Падеж (`NOMINATIVE`, `ACCUSATIVE`, ...) |
| `number_type` | varchar | Число (`SINGULAR`, `DUAL`, `PLURAL`) |
| `ending` | varchar | Окончание в IAST (например, `aḥ`, `am`) |

**Ключ:** (vowel_type, gender, case_type, number_type)

**Примечание:** Для уроков, где род не различает окончания (declensions-i, declensions-u, declensions-r), в таблице gender = `UNSPECIFIED`. Для уроков с одним родом (declensions-a-masc, declensions-a-neut, declensions-a-fem, declensions-i-long, declensions-u-long) gender = фактический род.

**Следствие:**
- Quiz-service при генерации вопроса читает окончание из `case_endings` по ключу (vowelType, gender, caseType, numberType).
- Таблица заполняется при инициализации данных (миграция Flyway или seed).
- Для проверки ответа не требуется дополнительная логика — прямое сравнение с эталоном.

### ADR-004: Формирование вопросов для уроков с двумя родами

**Статус:** Принято

**Контекст:** Уроки `declensions-i`, `declensions-u`, `declensions-r` охватывают два рода, но окончания для мужского и женского рода совпадают во всех падежах и числах.

**Решение:**
- Вопросы для этих уроков не дублируются по роду — каждому сочетанию (caseType, numberType) соответствует ровно один вопрос.
- Поле `gender` в GrammarQuestionProgress для таких уроков передаётся как `null` или `UNSPECIFIED`.
- Общее количество вопросов в уроке = 24 (8 падежей × 3 числа).
- Для уроков с одним родом (declensions-a-masc, declensions-a-neut, declensions-a-fem, declensions-i-long, declensions-u-long) поле `gender` обязательно и количество вопросов также 24 (gender фиксирован).

**Следствие:**
- Клиентская агрегация прогресса по ключу (gender, caseType, numberType) для уроков с двумя родами использует gender = UNSPECIFIED.
- API возвращает gender в ответе всегда, но для смешанных уроков он равен UNSPECIFIED.

### ADR-005: Единство окончаний для основ -i, -u, -ṛ независимо от рода

**Статус:** Принято

**Контекст:** Для уроков склонений с основами на -i, -u, -ṛ (`vowel_type`: I, I_LONG, U, U_LONG, R) исторически падежные окончания не различаются по грамматическому роду ни в одном падеже/числе. Это отличает их от основ на -a, где финальная гласная и род жёстко связаны (a-masc, a-neut, a-fem). При этом уроки `declensions-i`, `declensions-u`, `declensions-r` содержат слова как мужского, так и женского рода, и прогресс по каждому роду нужно отслеживать отдельно (разные основы/слова).

**Решение:**
- В таблице `case_endings` для `vowel_type = I | I_LONG | U | U_LONG | R` поле `gender` может быть любым (`MASCULINE`, `FEMININE`, `NEUTER`), но `ending_iast` для одного и того же `(case_type, number_type)` будет одинаковым для всех гендеров.
- Ключ агрегации прогресса остаётся `(gender, caseType, numberType)` для единообразия по всем 8 урокам склонений (включая declensions-i, declensions-u, declensions-r, где внутри урока две гендерные группы вопросов). Различие — в том, что `caseEnding` у двух гендерных групп совпадает, а `successRate` считается раздельно по роду, так как это разные основы/слова.
- В таблице `case_endings` допускаются дублирующие строки с одинаковым `ending_iast` и разным `gender`, либо (на усмотрение Агента 2) одна запись с `gender = UNSPECIFIED` для этих `vowel_type`. Выбор формата хранения — за Агентом 2 (Domain), но API должен возвращать корректные данные.

**Следствие:**
- ADR-003 (примечание про UNSPECIFIED для -i, -u, -r) дополнен: `UNSPECIFIED` — один из допустимых вариантов хранения; альтернатива — дублирующие записи с разным gender.
- ADR-004 уточнён: для уроков -i, -u, -r количество вопросов остаётся 24, если gender в рамках урока фиксирован; если урок содержит два рода — 48 (по 24 на каждый род), но окончания совпадают.
- Агент 2 (Domain) решает: хранить одну запись (UNSPECIFIED) или две (MASCULINE=FEMININE).
- Агент 3 (Frontend) получает `caseEnding` одинаковый для обоих родов в рамках одного (caseType, numberType), но прогресс агрегирует раздельно по роду.

### ADR-006: sangraha-service — произведения, LLM-анализ стихов, синхронизация лексики через Kafka

**Статус:** Принято

**Контекст:** Нужен функционал работы с санскритскими текстами (произведения → главы →
стихи), LLM-анализ стиха (транслитерация, перевод, сандхи, грамматика) и передача
извлечённой лексики в существующий механизм VOCABULARY-квизов content-service
(`VocabularyCategory`/`VocabularyWord`/`VocabularyWordCategory`, дерево категорий уже
поддерживает агрегацию слов по поддереву — см. `VocabularyService.getVocabularyWordsForQuiz`).

**Решение:**
- Заводится новый сервис **`sangraha-service`** (Java 21 + Virtual Threads, схема БД `sangraha`), а не домен внутри `content-service` — см. `docs/services/sangraha-service.md`.
- LLM (OpenAI-совместимый API) вызывается напрямую из `sangraha-service`, конфигурация только через env (`SANGRAHA_LLM_*`), без дефолтов в yml. Ответ модели принимается строго через **tool calling** (один tool `submit_verse_analysis` со строгой JSON-схемой) — свободный текст не парсится.
- Никаких синхронных HTTP-вызовов между `sangraha-service` и `content-service`/`dictionary-service`. Единственный канал — **Kafka**, topic `sangraha-vocabulary-events` (transactional outbox, как в `user-service`/`quiz-service`), событие публикуется **на каждый проанализированный стих**.
- Иерархия `work.slug` → `chapter.slug` используется как `code` в дереве `VocabularyCategory` content-service (`categoryCode = "{workSlug}.{chapterSlug}"`), что даёт VOCABULARY-квиз «бесплатно» через уже существующий механизм агрегации по поддереву.
- Дедупликация слов в content-service — по `(wordIast, stem)`: при совпадении не создаём новый `VocabularyWord`, только добавляем связь `VocabularyWordCategory`.
- Связь слов стиха со словарными статьями `dictionary-service` (`slp1`) **не делается** в этой итерации.
- Версии `VerseAnalysis`/`VerseWord` не хранятся — повторный анализ перезаписывает предыдущий результат.
- Права доступа: весь write-контур `sangraha-service` — только `ADMIN`. Отдельная роль «редактор/переводчик» — отложена.

**Следствие:**
- Агент 2 (Domain), назначенный на `sangraha-service`, должен завести первый в проекте `@KafkaListener` — в `content-service` (consumer `sangraha-vocabulary-events`).
- **Shared DTO**: заведён `sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent` в `shared/samskrtam-dtos` (пакет `sangraha`). Решение Агента 6: событие используется двумя сервисами (producer + consumer), локальный DTO создал бы дублирование и риск рассинхронизации.
- **Порт**: фиксирован `8089`, согласован с Агентом 5 DevOps.
- **Quiz(VOCABULARY) — только на уровне произведения**: Quiz заводится с `slug = workSlug`. Главы не получают отдельного Quiz — агрегация слов по поддереву категорий (`VocabularyService.getVocabularyWordsForQuiz`) уже поддерживает фильтрацию по `categoryCode = "{workSlug}.{chapterSlug}"` через дерево категорий. Отдельный Quiz на главу создал бы дублирование набора слов.

---

## 15. Kafka

- Топики именуются `<domain>-<событие-во-множественном-числе>-events`, kebab-case: `quiz-answered-events`, `quiz-session-status-changed-events`, `sangraha-vocabulary-events`.
- Публикация — только через Transactional Outbox Pattern (таблица `outbox_events` в схеме сервиса-источника + плановый publisher), см. пример в `user-service`/`quiz-service`. Прямая публикация в Kafka из бизнес-логики без outbox — запрещена.
- Синхронные вызовы между доменными сервисами (Domain ↔ Domain) по HTTP не приветствуются там, где можно обойтись асинхронным событием — см. ADR-006.

## 16. Мапперы Entity/DTO

### 16.1 Общее правило

Маппинг entity/model → DTO выносится в отдельный пакет `mapper/` внутри сервиса. Реализация — **MapStruct** с `@Mapper(componentModel = "spring")`. Один файл маппера — одна доменная область (entity → DTO и обратно).

### 16.2 Запрещённые паттерны

- `abstract class` с `@Autowired` внутри маппера — запрещён (нарушает Single Responsibility, смешивает маппинг и бизнес-логику).
- Полная реализация entity → DTO вручную (десятки строк `.builder()...build()`, дублирующие структуру MapStruct-маппера) внутри `*Service`/`*Controller` — блокирующее замечание на code review для **нового** кода: такой маппинг выносится в `mapper/`.

> **Пересмотрено:** первоначальная версия правила («любой `.builder()` вне `mapper/` — нарушение») оказалась нереалистичной — на момент пересмотра `.builder()` присутствует в сервисном слое всех сервисов (~30 файлов), в основном там, где DTO собирается из **нескольких источников** (агрегат + вычисляемые поля + данные другого сервиса), что MapStruct не выражает естественно. Требовать 1:1-маппер под каждый такой случай означало плодить мапперы с одним полем и вызовом сервиса внутри — то, что сам же п. 16.2 запрещает. Существующий код **не переписывается ретроактивно**; правило действует вперёд, см. §16.3.

### 16.3 Когда `.builder()` в сервисном слое — нормально, а когда выносить в `mapper/`

Критерий — **источник данных**, а не факт использования `.builder()`:

- **Простой 1:1 маппинг** entity/model → DTO (поля переносятся почти без трансформации) — выносится в `mapper/` через MapStruct. Если видите `.builder()`, где просто одно поле в одно, без вызова сервисов/репозиториев — это кандидат на вынос.
- **DTO собирается из нескольких источников** (несколько entity, plus данные из другого сервиса/HTTP-клиента, plus вычисляемые/агрегированные поля) — `.builder()` прямо в `*Service` **допустим**. Пример: `QuizDataAssembler`, `SessionFactory` — там сборка DTO неотделима от бизнес-логики сборки сессии.
- **Практическое правило:** если тело маппинга можно описать одной MapStruct-аннотацией `@Mapping` — это mapper. Если требуется `if`/цикл/вызов другого бина — это часть сервисной логики, и `.builder()` в `*Service` — нормальный способ собрать результат, не обязательно продукт для `mapper/`.
- `@Mapper(uses = {OtherMapper.class})` — допустимо для композиции мапперов.
- Default-методы и `@AfterMapping` / `@BeforeMapping` — допустимы для пост-обработки полей, не требующих вызова сервисов.
- Маппинг DTO ↔ Entity внутри `*Repository` (например, `RowMapper`) — не подпадает под это правило.

### 16.4 Пример эталонной структуры

```java
// services/quiz-service/src/main/java/.../quiz/mapper/QuizAnswerMapper.java
@Mapper(componentModel = "spring")
public interface QuizAnswerMapper {

    @Mapping(target = "isCorrect", source = "isCorrect")
    @Mapping(target = "correctOptionId", source = "correctWordId")
    AnswerResponse toAnswerResponse(boolean isCorrect, UUID correctWordId, GeneratedQuizQuestionDto dto);
}
```

### 16.5 Code Review

- Ручной маппер вне `mapper/` — блокирующий замечание.
- `abstract class` с `@Autowired` в маппере — блокирующее замечание.
- Исключения из правила 16.2 требуют комментария в коде с причиной.
