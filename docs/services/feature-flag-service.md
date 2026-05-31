# Feature Flag Service — Спецификация

> Сервис: `services/feature-flag-service`
> Порт: `8088`
> Management порт: `${FEATURE_FLAG_MANAGEMENT_PORT}` (9091 по умолчанию)
> Стек: Java 21 + Virtual Threads + Spring Boot 3.3 + Redis
> Status: **DRAFT**

---

## 1. Ответственность

Feature Flag Service управляет булевыми флагами которые включают/выключают поведение
других сервисов **без перезапуска**. Изменение флага через API применяется немедленно —
сервисы-клиенты читают состояние из Redis.

**Что даёт:**
- Включить/выключить Rate Limiting в Gateway без деплоя
- Быстро откатить новую функциональность при инциденте
- Постепенный rollout (флаг включён для 10% пользователей — будущее)

**Что НЕ делает:**
- Не хранит конфигурацию сервисов (это application.yml / ConfigMap)
- Не управляет секретами
- Не является A/B-тест платформой (можно вырасти в неё позже)

---

## 2. Реестр флагов

На старте — один флаг. Реестр расширяется по мере роста проекта.

| Флаг | Тип | По умолчанию | Потребитель | Описание |
|---|---|---|---|---|
| `RATE_LIMITING_ENABLED` | boolean | `true` | api-gateway | Включает Redis Rate Limiting |

### Добавление нового флага

1. Добавить константу в enum `FeatureFlag` (shared модуль или feature-flag-service)
2. Добавить строку в таблицу выше
3. Описать потребителя и поведение при `false`
4. Добавить запись в `V1__seed_flags.sql`

---

## 3. Redis как хранилище

Флаги хранятся в Redis как строки. Это даёт:
- Мгновенное распространение изменений (pub/sub или polling)
- Персистентность при `appendonly yes` в Redis конфиге
- Возможность читать флаги напрямую из Redis в Gateway (без HTTP-вызова)

```
Redis key:   feature:flag:{FLAG_NAME}
Redis value: "true" | "false"
TTL:         нет (персистентны до явного удаления)
```

Пример:
```
feature:flag:RATE_LIMITING_ENABLED = "true"
```

### Кеш в клиентах (api-gateway)

Клиенты не ходят в Redis при каждом запросе — используют локальный кеш с TTL 5 секунд:

```
Запрос 1  → читаем из Redis → кешируем на 5 сек
Запросы 2-N (в течение 5 сек) → читаем из локального кеша
Запрос N+1 (через 5 сек) → читаем из Redis снова
```

Задержка применения изменения флага: **до 5 секунд**.
Для операционных нужд (отключить rate limiting при инциденте) — приемлемо.

---

## 4. API

### GET /api/v1/flags

Список всех флагов с текущим состоянием. Только для ADMIN.

**Response 200:**
```json
[
  {
    "name":        "RATE_LIMITING_ENABLED",
    "enabled":     true,
    "description": "Включает Redis Rate Limiting в api-gateway",
    "updatedAt":   "2024-05-01T12:00:00Z",
    "updatedBy":   "admin@samskrtam.local"
  }
]
```

---

### GET /api/v1/flags/{name}

Состояние конкретного флага. Только для ADMIN.

**Path param:** `name` — имя флага из реестра (например `RATE_LIMITING_ENABLED`)

**Response 200:**
```json
{
  "name":        "RATE_LIMITING_ENABLED",
  "enabled":     true,
  "description": "Включает Redis Rate Limiting в api-gateway",
  "updatedAt":   "2024-05-01T12:00:00Z",
  "updatedBy":   "admin@samskrtam.local"
}
```

**Response 404** — флаг не найден в реестре.

---

### PATCH /api/v1/flags/{name}

Изменение состояния флага. Только для ADMIN.

**Request body:**
```json
{ "enabled": false }
```

**Response 200** — обновлённый флаг (см. GET выше).

**Поведение:**
1. Валидирует что `name` существует в реестре
2. Записывает новое значение в Redis: `SET feature:flag:{name} "false"`
3. Сохраняет аудит-запись в PostgreSQL (кто, когда, что изменил)
4. Публикует Redis pub/sub событие `feature-flag-changed` (для будущих подписчиков)
5. Возвращает обновлённый флаг

---

### GET /api/v1/flags/{name}/history

История изменений флага. Только для ADMIN.

**Response 200:**
```json
[
  {
    "changedAt":  "2024-05-01T12:00:00Z",
    "changedBy":  "admin@samskrtam.local",
    "oldValue":   true,
    "newValue":   false,
    "reason":     "Отключение rate limiting на время инцидента"
  }
]
```

---

## 5. Схема БД

```sql
-- V1__create_feature_flags.sql
CREATE TABLE feature_flags.flags (
    name        VARCHAR(100) NOT NULL,
    description TEXT         NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_flags PRIMARY KEY (name)
);

-- Начальные данные
INSERT INTO feature_flags.flags (name, description) VALUES
    ('RATE_LIMITING_ENABLED', 'Включает Redis Rate Limiting в api-gateway');

-- V2__create_flag_audit.sql
CREATE TABLE feature_flags.flag_audit (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    flag_name   VARCHAR(100) NOT NULL,
    old_value   BOOLEAN      NOT NULL,
    new_value   BOOLEAN      NOT NULL,
    changed_by  VARCHAR(255) NOT NULL,   -- email из JWT X-User-Id
    reason      TEXT,                    -- опциональный комментарий
    changed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_flag_audit PRIMARY KEY (id),
    CONSTRAINT fk_flag_name  FOREIGN KEY (flag_name) REFERENCES feature_flags.flags(name)
);

CREATE INDEX idx_audit_flag_name ON feature_flags.flag_audit (flag_name, changed_at DESC);
```

---

## 6. Структура кода

```
sm/selflearn/samskrtam/featureflag/
├── Application.java
├── controller/
│   └── FeatureFlagController.java      ← CRUD API
├── service/
│   └── FeatureFlagService.java         ← логика + Redis + аудит
├── repository/
│   ├── FlagRepository.java             ← JPA (PostgreSQL, метаданные флагов)
│   └── FlagAuditRepository.java        ← JPA (история изменений)
├── model/
│   ├── Flag.java                       ← JPA entity
│   └── FlagAudit.java                  ← JPA entity
└── dto/
    ├── FlagResponse.java
    ├── FlagUpdateRequest.java
    └── FlagHistoryEntry.java
```

---

## 7. Реализация

### FeatureFlagService.java

```java
// sm/selflearn/samskrtam/featureflag/service/FeatureFlagService.java
@Service
@Slf4j
@RequiredArgsConstructor
public class FeatureFlagService {

    private static final String REDIS_KEY_PREFIX = "feature:flag:";
    private static final String PUBSUB_CHANNEL   = "feature-flag-changed";

    private final StringRedisTemplate       redis;
    private final FlagRepository            flagRepository;
    private final FlagAuditRepository       auditRepository;

    /**
     * Читает текущее состояние флага из Redis.
     * Вызывается клиентами (api-gateway) напрямую из Redis —
     * этот метод используется для инициализации / fallback.
     */
    public boolean isEnabled(String flagName) {
        String value = redis.opsForValue().get(REDIS_KEY_PREFIX + flagName);
        if (value == null) {
            log.warn("Flag not found in Redis: {}, defaulting to false", flagName);
            return false;
        }
        return Boolean.parseBoolean(value);
    }

    public List<FlagResponse> getAllFlags() {
        return flagRepository.findAll().stream()
                .map(flag -> toResponse(flag, isEnabled(flag.getName())))
                .toList();
    }

    public FlagResponse getFlag(String name) {
        Flag flag = flagRepository.findById(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Flag not found: " + name));
        return toResponse(flag, isEnabled(name));
    }

    @Transactional
    public FlagResponse updateFlag(String name, boolean newValue, String changedBy, String reason) {
        Flag flag = flagRepository.findById(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Flag not found: " + name));

        boolean oldValue = isEnabled(name);

        if (oldValue == newValue) {
            return toResponse(flag, oldValue);   // нет изменений — idempotent
        }

        // 1. Обновляем Redis (немедленно применяется всеми клиентами)
        redis.opsForValue().set(REDIS_KEY_PREFIX + name, String.valueOf(newValue));

        // 2. Аудит в PostgreSQL
        auditRepository.save(FlagAudit.builder()
                .flagName(name)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedBy(changedBy)
                .reason(reason)
                .changedAt(Instant.now())
                .build());

        // 3. Pub/Sub — уведомляем подписчиков (будущие клиенты с push-моделью)
        redis.convertAndSend(PUBSUB_CHANNEL, name + "=" + newValue);

        log.info("Feature flag updated: name={}, {}→{}, by={}", name, oldValue, newValue, changedBy);

        return toResponse(flag, newValue);
    }

    // Вызывается при старте — синхронизирует Redis с БД (на случай очистки Redis)
    @PostConstruct
    public void seedRedisFromDb() {
        flagRepository.findAll().forEach(flag -> {
            String key = REDIS_KEY_PREFIX + flag.getName();
            // setIfAbsent — не перезаписываем если уже есть (оператор мог изменить)
            redis.opsForValue().setIfAbsent(key, "true");
        });
        log.info("Feature flags seeded to Redis");
    }

    private FlagResponse toResponse(Flag flag, boolean enabled) {
        return new FlagResponse(
                flag.getName(), enabled, flag.getDescription(),
                auditRepository.findTopByFlagNameOrderByChangedAtDesc(flag.getName())
                        .map(FlagAudit::getChangedAt).orElse(flag.getCreatedAt()),
                auditRepository.findTopByFlagNameOrderByChangedAtDesc(flag.getName())
                        .map(FlagAudit::getChangedBy).orElse("system")
        );
    }
}
```

### FeatureFlagController.java

```java
// sm/selflearn/samskrtam/featureflag/controller/FeatureFlagController.java
@RestController
@RequestMapping("/api/v1/flags")
@RequiredArgsConstructor
public class FeatureFlagController {

    private final FeatureFlagService service;

    @GetMapping
    public List<FlagResponse> getAll() {
        return service.getAllFlags();
    }

    @GetMapping("/{name}")
    public FlagResponse get(@PathVariable String name) {
        return service.getFlag(name);
    }

    @PatchMapping("/{name}")
    public FlagResponse update(
            @PathVariable String name,
            @RequestBody FlagUpdateRequest request,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-Reason", required = false) String reason) {
        return service.updateFlag(name, request.enabled(), userId, reason);
    }

    @GetMapping("/{name}/history")
    public List<FlagHistoryEntry> history(@PathVariable String name) {
        return service.getHistory(name);
    }
}
```

---

## 8. Клиент в api-gateway (FeatureFlagClient)

api-gateway читает флаги **напрямую из Redis** — без HTTP-запроса к feature-flag-service.
Это даёт низкую латентность и устойчивость к недоступности feature-flag-service.

```java
// sm/selflearn/samskrtam/gateway/featureflag/FeatureFlagClient.java
@Component
@Slf4j
@RequiredArgsConstructor
public class FeatureFlagClient {

    private static final String  KEY_PREFIX  = "feature:flag:";
    private static final Duration CACHE_TTL  = Duration.ofSeconds(5);

    private final ReactiveStringRedisTemplate redis;

    // Локальный кеш: флаг → значение, инвалидируется через 5 сек
    private final Map<FeatureFlag, CachedFlag> localCache = new ConcurrentHashMap<>();

    /**
     * Читает состояние флага.
     * Сначала проверяет локальный кеш (TTL 5 сек), потом Redis.
     * При недоступности Redis возвращает дефолтное значение флага.
     */
    public Mono<Boolean> isEnabled(FeatureFlag flag) {
        CachedFlag cached = localCache.get(flag);
        if (cached != null && !cached.isExpired()) {
            return Mono.just(cached.value());
        }

        return redis.opsForValue()
                .get(KEY_PREFIX + flag.name())
                .map(value -> {
                    boolean result = Boolean.parseBoolean(value);
                    localCache.put(flag, new CachedFlag(result, Instant.now().plus(CACHE_TTL)));
                    return result;
                })
                .onErrorResume(ex -> {
                    log.warn("Failed to read feature flag {} from Redis, using default={}",
                            flag, flag.defaultValue(), ex);
                    return Mono.just(flag.defaultValue());
                })
                .defaultIfEmpty(flag.defaultValue());
    }

    private record CachedFlag(boolean value, Instant expiresAt) {
        boolean isExpired() { return Instant.now().isAfter(expiresAt); }
    }
}
```

```java
// sm/selflearn/samskrtam/gateway/featureflag/FeatureFlag.java
// Enum с реестром флагов — shared между gateway и feature-flag-service
public enum FeatureFlag {

    RATE_LIMITING_ENABLED(true);   // дефолт true — безопасное умолчание

    private final boolean defaultValue;

    FeatureFlag(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean defaultValue() { return defaultValue; }
}
```

---

## 9. application.yml

```yaml
server:
  port: ${FEATURE_FLAG_SERVICE_PORT:8088}

spring:
  application:
    name: feature-flag-service

  datasource:
    url: ${FEATURE_FLAG_DB_URL}
    username: ${FEATURE_FLAG_DB_USER}
    password: ${FEATURE_FLAG_DB_PASSWORD}

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  flyway:
    schemas: feature_flags
    locations: classpath:db/migration

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

  threads:
    virtual:
      enabled: true

management:
  server:
    port: ${FEATURE_FLAG_MANAGEMENT_PORT}
  endpoints:
    web:
      exposure:
        include: health, prometheus
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never
  tracing:
    sampling:
      probability: ${TRACING_SAMPLING_PROBABILITY}
  otlp:
    tracing:
      endpoint: ${OTEL_EXPORTER_OTLP_ENDPOINT}
```

---

## 10. .env (локальная разработка)

```
FEATURE_FLAG_SERVICE_PORT=8088
FEATURE_FLAG_MANAGEMENT_PORT=9091

FEATURE_FLAG_DB_URL=jdbc:postgresql://localhost:5432/samskrtam?currentSchema=feature_flags
FEATURE_FLAG_DB_USER=samskrtam
FEATURE_FLAG_DB_PASSWORD=samskrtam

REDIS_HOST=localhost
REDIS_PORT=6379
```

---

## 11. docker-compose (уже в architecture.md)

```yaml
feature-flag-service:
  build: ./services/feature-flag-service
  ports:
    - "8088:8088"
    - "${FEATURE_FLAG_MANAGEMENT_PORT}:${FEATURE_FLAG_MANAGEMENT_PORT}"
  depends_on: [postgres, redis]
  environment:
    FEATURE_FLAG_DB_URL: jdbc:postgresql://postgres:5432/samskrtam?currentSchema=feature_flags
    FEATURE_FLAG_DB_USER: samskrtam
    FEATURE_FLAG_DB_PASSWORD: samskrtam
    REDIS_HOST: redis
    REDIS_PORT: 6379
    FEATURE_FLAG_SERVICE_PORT: 8088
    FEATURE_FLAG_MANAGEMENT_PORT: ${FEATURE_FLAG_MANAGEMENT_PORT}
    MANAGEMENT_TRACING_SAMPLING_PROBABILITY: ${TRACING_SAMPLING_PROBABILITY}
    OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}
```

---

## 12. Маршрут в api-gateway

`/api/v1/flags/**` проксируется в feature-flag-service и доступен только ADMIN:

```java
// GatewayRoutesConfig.java
.route("feature-flags", r -> r
        .path("/api/v1/flags/**")
        .uri(featureFlagServiceUrl))
```

```java
// SecurityConfig.java
.pathMatchers("/api/v1/flags/**").hasRole("ADMIN")
```

---

## 13. Последовательность при изменении флага

```
Admin UI
  → PATCH /api/v1/flags/RATE_LIMITING_ENABLED { "enabled": false }
  → api-gateway (авторизация ADMIN, проксирование)
  → feature-flag-service
      1. Записывает "false" в Redis:  SET feature:flag:RATE_LIMITING_ENABLED "false"
      2. Сохраняет аудит в PostgreSQL
      3. Публикует Redis pub/sub: "RATE_LIMITING_ENABLED=false"
  → api-gateway (FeatureFlagClient)
      - Следующий запрос через ≤5 сек читает "false" из Redis
      - ConditionalRateLimiterFilter пропускает без rate limiting
```

---

## 14. Acceptance Criteria

- [ ] `GET /api/v1/flags` возвращает список всех флагов с текущим состоянием
- [ ] `PATCH /api/v1/flags/RATE_LIMITING_ENABLED { "enabled": false }` отключает rate limiting в Gateway за ≤5 секунд
- [ ] `PATCH /api/v1/flags/RATE_LIMITING_ENABLED { "enabled": true }` включает rate limiting обратно
- [ ] История изменений доступна через `GET /api/v1/flags/{name}/history`
- [ ] Изменение флага недоступно без токена ADMIN (403)
- [ ] При недоступности Redis api-gateway использует дефолтное значение флага (`true` для RATE_LIMITING_ENABLED)
- [ ] При перезапуске feature-flag-service — флаги восстанавливаются в Redis из дефолтов (если ключ отсутствует)
- [ ] Аудит сохраняется в PostgreSQL с changedBy (userId из X-User-Id) и меткой времени
- [ ] `PATCH` с тем же значением (уже `true` → `true`) не создаёт аудит-запись
- [ ] Флаг не из реестра (`GET /api/v1/flags/UNKNOWN`) → 404

---

## 15. Открытые вопросы

- [ ] Нужен ли UI для управления флагами (отдельная страница в AdminPage)?
- [ ] Добавить поле `reason` в PATCH тело (зачем изменяем флаг)?
- [ ] Процентный rollout (`enabledForPercent: 10`) — нужен для A/B тестов?
- [ ] Уведомление в Slack/Telegram при изменении флага в production?
