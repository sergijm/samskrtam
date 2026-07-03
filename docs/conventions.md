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

### Структура .env (ключевые группы, полный список — в `.env.example` в корне монорепо)

Секции: Database (`DB_HOST/PORT/NAME/USER/PASSWORD`); JDBC (`SPRING_DATASOURCE_URL`); R2DBC (`SPRING_R2DBC_URL`); Redis (`REDIS_HOST/PORT`); Kafka (`KAFKA_BOOTSTRAP_SERVERS`); Keycloak (`KEYCLOAK_URL/REALM/CLIENT_ID/SECRET/JWKS_URI`); Services (`CONTENT_SERVICE_URL`, `USER_SERVICE_URL`, `DICTIONARY_SERVICE_URL`, `STATISTICS_SERVICE_URL`, `FEATURE_FLAG_SERVICE_URL`, `CORS_ALLOWED_ORIGINS`); Observability (`MANAGEMENT_PORT`, `OTEL_EXPORTER_OTLP_ENDPOINT`, `TRACING_SAMPLING_PROBABILITY`); Swagger (`SPRINGDOC_ENABLED`); Connection Pool (`DB_POOL_MAX_SIZE/MIN_IDLE/CONNECTION_TIMEOUT_MS`); Graceful Shutdown (`GRACEFUL_SHUTDOWN_TIMEOUT`); Spring (`SPRING_PROFILES_ACTIVE`); MinIO (`MINIO_URL/PUBLIC_URL/ACCESS_KEY/SECRET_KEY/BUCKET_*`); Quiz Service (`QUIZ_SERVICE_PORT`, `APP_OUTBOX_FIXED_DELAY_MS`); Statistics Service (`STATISTICS_SERVICE_PORT`, `SPRING_KAFKA_STREAMS_*`); Frontend (`VITE_API_URL`).

### Spring Profiles

| Profile | Окружение | Swagger | Tracing sampling |
|---|---|---|---|
| `default` | локальная разработка | включён | 100% |
| `staging` | тестовый стенд | включён | 100% |
| `production` | prod | **выключен** | 10% |

Профиль активируется через `SPRING_PROFILES_ACTIVE` в `.env`.

---


### Адреса сервисов по окружениям

Локально (IDEA вне Docker): `localhost` + порт сервиса. В Kubernetes: DNS-имя сервиса (`http://<service-name>:<port>`). Список сервисов: content-service (8081), user-service (8087), dictionary-service (8083), statistics-service (8084), feature-flag-service (8085).


## 2. Логирование

### Обязательные правила

- Использовать **`@Slf4j`** (Lombok). `System.out.*` и `System.err.*` запрещены.
- Логи только в **JSON** формате через `logstash-logback-encoder` — для интеграции с Loki.
- Sensitive данные (email, пароли, токены, `userId` в URL) **не логируются ни на каком уровне**.
- В реактивном стеке (quiz-service, api-gateway) использовать `doOnError` для логирования с **сохранением** ошибки в pipeline, `onErrorResume` — только для обработки с подменой.
- Ошибки публикации в Kafka через Outbox Pattern должны логироваться с уровнем `ERROR` или `WARN` в зависимости от возможности повторной обработки.

### Уровни логирования

TRACE — вход в сервисные методы; DEBUG — бизнес-решения (кэш-промах, выбор ветки); INFO — старт/стоп, миграции, Kafka; WARN — 4xx downstream, retry, деградация; ERROR — 5xx/timeout, необработанные исключения, ошибки Kafka после всех попыток.

---

## 3. Сквозная трассировка (Micrometer Tracing)

Стек: Micrometer Tracing → OpenTelemetry → Grafana Tempo (трейсы); Logback JSON → Promtail → Loki (логи + traceId); Micrometer → /actuator/prometheus → Prometheus (метрики); единый UI — Grafana.

Конфигурация (`application.yml` всех сервисов): `management.tracing.sampling.probability` из `TRACING_SAMPLING_PROBABILITY`, `management.otlp.tracing.endpoint` из `OTEL_EXPORTER_OTLP_ENDPOINT`, propagation type = `w3c`.

Virtual Threads: traceId автоматически через MDC + Micrometer. WebFlux: требуется ReactorContextAccessor (ThreadLocal не работает). Propagation: WebClient автоматически добавляет traceparent; Gateway пробрасывает во все downstream.

Docker Compose: стандартные сервисы Tempo, Loki, Prometheus, Grafana.

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

Значения в `.env` и в k8s ConfigMap. Prometheus scrape targets — те же порты, `metrics_path: /actuator/prometheus`.

### Health Groups

Liveness (`/actuator/health/liveness`): JVM alive + disk space → restart pod. Readiness (`/actuator/health/readiness`): DB, Redis, Kafka → вывод из rotation. Liveness **не включает** внешние зависимости.

### Кастомные метрики

quiz-service: `quiz.answers.total` (Counter, tags: lessonType/correct), `quiz.session.duration` (Timer, lessonType), `quiz.cache.miss` (Counter). statistics-service: `kafka.events.processed` (Counter, topic). dictionary-service: `dictionary.cache.hit` (Counter).


---

## 5. Swagger / OpenAPI

Агрегация в Gateway: `springdoc.swagger-ui.urls` — 5 сервисов (user-service:8087, content-service:8081, quiz-service:8082, dictionary-service:8085, statistics-service:8086). Swagger UI: `/swagger-ui.html` через Gateway. Все публичные эндпоинты аннотируются.

---

## 6. Обработка ошибок

GlobalExceptionHandler (WebFlux — quiz-service, gateway) + ErrorHandlingFilter (Gateway). Детали — в спецификациях соответствующих сервисов.

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

Graceful shutdown: `server.shutdown: graceful`, `spring.lifecycle.timeout-per-shutdown-phase` из `GRACEFUL_SHUTDOWN_TIMEOUT`.

---

## 10. Качество кода

Checkstyle (root `build.gradle.kts`: `toolVersion = "10.17.0"`, `config/checkstyle/checkstyle.xml`). SpotBugs (`effort = MAX`, `confidence = MEDIUM`, `exclude.xml`). CI order: `test → jacocoTestReport → jacocoTestCoverageVerification → checkstyleMain → spotbugsMain`. Сборка падает при нарушении любого этапа.

---

## 11. Git Conventions

### Conventional Commits

```
<type>(<scope>): <description>
```

Типы: feat/fix/docs/refactor/test/chore/perf. Scope — имя сервиса: `quiz-service`, `gateway`, `content-service`, `shared`. Примеры: `feat(quiz-service): add session resume endpoint`, `fix(gateway): return 502 when downstream unreachable`, `docs(conventions): add logging rules`, `test(statistics-service): add Kafka consumer integration test`, `chore(deps): bump springdoc to 2.5.0`.

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

- Secrets management (K8s Secrets vs Vault); CHANGELOG (ручной vs semantic-release); Grafana dashboards (JSON в репозитории vs ручная настройка); ArchUnit тесты (shared/arch-rules vs дублирование); Testcontainers reuse mode.
- [x] k8s NetworkPolicy — реализовано (доступ к сервисам только от Gateway).

---

## 14. Архитектурные решения (ADR)

### ADR-001: Разделение auth между Gateway и user-service

**Gateway** → OAuth2/OIDC (login ROPC, refresh, logout, Google/Mail.ru редиректы, Authorization Code flow). **user-service** → жизненный цикл аккаунта (регистрация, восстановление/смена пароля, верификация email, invite). Граница: OAuth2 протокол — Gateway, бизнес-логика аккаунта — user-service через Keycloak Admin API.

### ADR-002: Семантика Quiz vs Lesson vs Activity

**Lesson** = единица контента (склонение, словарный урок). **Quiz** = выборка вопросов из урока на сессию. **QuizSession** = прохождение квиза (не переименовывается). **Activity** = будущая абстракция (M5+). Следствие: `QuizRepository`/`QuizContentService` → `LessonRepository`/`LessonContentService`; `quizId` в статистике → `lessonId`; Kafka-топики и роут `/api/v1/quiz/` не меняются; `QuizListItemResponse` удалён.

### ADR-003: Хранение окончаний склонений в БД

Таблица `case_endings (vowel_type, gender, case_type, number_type, ending)` — эталон падежных окончаний. Ключ: (vowel_type, gender, case_type, number_type). Для уроков без родового различия (declensions-i/u/r) gender = UNSPECIFIED. Quiz-service читает окончание по ключу при генерации вопроса; проверка ответа — прямое сравнение.

### ADR-004: Формирование вопросов для уроков с двумя родами

Уроки declensions-i/u/r (два рода, одинаковые окончания) — 24 вопроса (8 caseType × 3 numberType), gender = UNSPECIFIED. Уроки с одним родом (declensions-a-masc/neut/fem, declensions-i-long/u-long) — поля gender обязательно, тоже 24 вопроса.

### ADR-005: Единство окончаний для основ -i, -u, -ṛ независимо от рода

Для vowel_type I/I_LONG/U/U_LONG/R окончания одинаковы для всех родов. Прогресс агрегируется раздельно по gender (разные основы/слова). Хранение: либо одна запись с gender = UNSPECIFIED, либо дублирующие строки с разным gender — на усмотрение Агента 2. Агент 3 получает одинаковый caseEnding, но агрегирует прогресс раздельно.

### ADR-006: sangraha-service — произведения, LLM-анализ стихов, синхронизация лексики через Kafka

Новый сервис `sangraha-service` (Java 21, Virtual Threads, схема `sangraha`). LLM-анализ стиха (OpenAI-совместимый) — строго через tool calling (`submit_verse_analysis`), без парсинга свободного текста. Никаких синхронных HTTP-вызовов к content-service/dictionary-service — только Kafka, topic `sangraha-vocabulary-events` (transactional outbox). Иерархия work.slug → chapter.slug маппится на VocabularyCategory.code в content-service для бесплатного VOCABULARY-квиза. Дедупликация слов по (wordIast, stem). Версионирование analysis не хранится (перезапись). Права: write — только ADMIN. Порт: 8089.

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
