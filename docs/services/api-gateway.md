# API Gateway — Spring Cloud Gateway

> Язык: **Java 21 + WebFlux** (Gateway требует реактивный стек)
> Модуль: `infrastructure/api-gateway`
> Порт: 8090
> Status: **UPDATED**

---

## 1. Ответственность

- Единая точка входа для всех клиентов
- Валидация JWT через Keycloak JWKS (кроме публичных endpoints)
- Добавление X-User-* заголовков для downstream сервисов
- Маршрутизация запросов по path
- Rate limiting через Redis
- CORS для фронтенда

Gateway **не содержит бизнес-логики** — только инфраструктурные фильтры.

---

## 2. Зависимости

| Сервис | Зачем |
|---|---|
| Keycloak | JWKS URI для валидации JWT + OAuth2 Authorization Code flow |
| Redis | Rate Limiting (токен-бакет) + OAuth2 state |
| feature-flag-service | Флаг `RATE_LIMITING_ENABLED` — включить/выключить rate limiting без перезапуска |
| user-service | OAuth2 sync (создание/обновление профиля после OAuth2 callback) |

```kotlin
// infrastructure/api-gateway/build.gradle.kts
dependencies {
    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.security.oauth2)        // Resource Server (валидация JWT)
    implementation(libs.spring.security.oauth2.client) // OAuth2 Client (Authorization Code flow)
    implementation(libs.spring.redis.reactive)
    implementation(libs.spring.webflux)                // WebClient (Keycloak, user-service, feature-flag-service)
}
```

---

## 3. Таблица маршрутов

| Path | Сервис | Auth |
|---|---|---|
| `/api/v1/auth/oauth2/{provider}` | **Gateway (OAuth2 Client)** | Public |
| `/api/v1/auth/oauth2/callback` | **Gateway (OAuth2 Client)** | Public |
| `/api/v1/auth/refresh` | **Gateway (OAuth2 Client)** | Public |
| `/api/v1/auth/**` | user-service:8087 | **Public** |
| `/api/v1/content/public/**` | content-service:8081 | STUDENT |
| `/api/v1/content/**` | content-service:8081 | ADMIN |
| `/api/v1/quiz/**` | quiz-service:8082 | STUDENT |
| `/api/v1/dictionary/**` | dictionary-service:8085 | STUDENT |
| `/api/v1/statistics/**` | statistics-service:8086 | STUDENT |
| `/actuator/health` | gateway | Public |

> `/api/v1/auth/oauth2/{provider}`, `/api/v1/auth/oauth2/callback` и `/api/v1/auth/refresh` — обрабатываются
> самим Gateway, **не проксируются**. Gateway выступает OAuth2 Client:
> хранит `client_secret` в env, инициирует Authorization Code flow,
> получает code от Keycloak, обменивает на токены, передаёт в user-service.
> Фронтенд никогда не видит `client_secret`.
>
> `/api/v1/auth/**` — остальные auth-эндпоинты (логин/пароль, регистрация,
> logout, forgot-password) проксируются в user-service. Безопасность обеспечивает user-service.

> ⚠️ Маршруты определены в `GatewayRoutesConfig.java` через Java DSL — не в `application.yml`.
> В `application.yml` только `default-filters` (rate limiting).
> Если маршрут не проксируется — сначала проверяй `GatewayRoutesConfig.java`.

---

## 3a. OAuth2 Authorization Code Flow (через Gateway)

Фронтенд инициирует OAuth2 flow редиреком на Gateway — `client_secret` остаётся на сервере.

```
1. Frontend
   → GET /api/v1/auth/oauth2/google
     (без параметров, без client_secret)
     // Перед этим Frontend сохраняет текущий URL в localStorage для последующего редиректа.

2. Gateway (OAuthController)
   → генерирует state, сохраняет в Redis с TTL 10 мин
   → строит Authorization URL к Keycloak
   → 302 Redirect → Keycloak /authorize?client_id=...&state=...&redirect_uri=...

3. Пользователь аутентифицируется в Keycloak / выбирает Google аккаунт

4. Keycloak
   → 302 Redirect → /api/v1/auth/oauth2/callback?code=...&state=...

5. Gateway (OAuthController)
   → проверяет state из Redis (защита от CSRF)
   → POST Keycloak /token { code, client_id, client_secret }  ← секрет только здесь
   → получает access_token + refresh_token от Keycloak
   → POST user-service /api/v1/users/oauth2/sync { keycloakToken }
     (user-service создаёт/обновляет профиль, возвращает собственный JWT)
   → 302 Redirect → ${FRONTEND_URL}/auth/callback#token=...

6. Frontend (AuthCallbackPage)
   → читает token из URL fragment (не из query — не логируется на сервере)
   → сохраняет в memory / httpOnly cookie
   // Frontend считывает сохраненный redirectPath из localStorage и перенаправляет пользователя на него.
```

**Поддерживаемые провайдеры** (`{provider}`):

| provider | Keycloak Identity Provider alias |
|---|---|
| `google` | `google` |
| `mailru` | `mailru` |

**State параметр:**

```
state = base64(randomBytes(32))
Redis key: oauth2:state:{state}
Redis value: { provider, createdAt }
TTL: 10 минут
```

Проверка state при callback защищает от CSRF-атак.
Если state не найден в Redis или не совпадает — 400 Bad Request.

**Redirect URI в Keycloak:**
```
Valid redirect URIs += http://localhost:8090/api/v1/auth/oauth2/callback
                       https://samskrtam.local/api/v1/auth/oauth2/callback
```

> ⚠️ `redirect_uri` должен совпадать с тем что зарегистрировано в Keycloak клиенте.
> Обновить в `keycloak.md` и в Keycloak Admin Console.

---

## 4. Правила авторизации (SecurityConfig)

```
Публичные (без JWT):
  /actuator/health/**
  /actuator/health/liveness
  /actuator/health/readiness
  OPTIONS /**              ← CORS preflight
  /api/v1/auth/**          ← вся аутентификация

Только ADMIN:
  /api/v1/content/**       ← за исключением /content/public/**

Любой аутентифицированный:
  /api/v1/content/public/**
  /api/**

Всё остальное:
  denyAll()
```

---

## 5. Глобальные фильтры (порядок выполнения)

```
Order -3  SecurityFilter          ← валидация JWT (Spring Security)
Order -2  IdentityHeaderFilter    ← X-User-Id, X-User-Role, X-User-Locale
Order -1  RateLimitFilter         ← Redis rate limiting
Order  0  RequestIdFilter         ← X-Request-Id для tracing
Order  1  LoggingFilter           ← structured logging
```

---

## 6. IdentityHeaderFilter

Извлекает данные из валидного JWT и передаёт downstream сервисам как заголовки.
Для публичных маршрутов (`/api/v1/auth/**`) principal отсутствует — фильтр
пропускает запрос без заголовков.

```java
@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    var jwt = auth.getToken();
                    String userId = jwt.getSubject();
                    String role   = extractRole(jwt.getClaimAsMap("realm_access"));
                    String locale = Optional.ofNullable(jwt.getClaimAsString("locale"))
                            .orElse("ru");

                    log.debug("IdentityHeaderFilter: userId={}, role={}, locale={}",
                            userId, role, locale);

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id",     userId)
                            .header("X-User-Role",   role)
                            .header("X-User-Locale", locale)
                            .build();

                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                // Публичный маршрут — нет principal, пропускаем без заголовков
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
```

---

## 7. Rate Limiting

Rate Limiting управляется через Feature Flag Service.
Флаг `RATE_LIMITING_ENABLED` читается при каждом запросе из Redis — без перезапуска Gateway.

Подробная спецификация Feature Flag Service → [feature-flag-service.md](feature-flag-service.md).

```yaml
# application.yml — rate limiting вынесен из default-filters:
# конфигурация через код (ConditionalRateLimiterFilter), не через yaml
spring:
  cloud:
    gateway:
      # default-filters пусты — rate limiting применяется условно в фильтре
      default-filters: []
```

```java
// filter/ConditionalRateLimiterFilter.java
/**
 * Глобальный фильтр Order -1.
 * Применяет Redis Rate Limiting только если флаг RATE_LIMITING_ENABLED = true.
 * Состояние флага читается из Redis при каждом запросе (TTL кеш 5 сек).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConditionalRateLimiterFilter implements GlobalFilter, Ordered {

    private final FeatureFlagClient featureFlagClient;
    private final RedisRateLimiter  redisRateLimiter;
    private final KeyResolver       userKeyResolver;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return featureFlagClient.isEnabled(FeatureFlag.RATE_LIMITING_ENABLED)
                .flatMap(enabled -> {
                    if (!enabled) {
                        return chain.filter(exchange);  // флаг выключен — пропускаем
                    }
                    return userKeyResolver.resolve(exchange)
                            .flatMap(key -> redisRateLimiter.isAllowed("samskrtam", key))
                            .flatMap(response -> {
                                if (response.isAllowed()) {
                                    return chain.filter(exchange);
                                }
                                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                return exchange.getResponse().setComplete();
                            });
                });
    }

    @Override public int getOrder() { return -1; }
}
```

```java
// config/RateLimiterConfig.java
@Configuration
public class RateLimiterConfig {

    // replenishRate  = 20 запросов/сек на пользователя
    // burstCapacity  = 40 — максимальный всплеск
    // requestedTokens = 1 токен на запрос
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(20, 40, 1);
    }
}
```

---

## 8. application.yml

```yaml
server:
  port: 8090

spring:
  application:
    name: api-gateway

  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${KEYCLOAK_JWKS_URI}

  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT}

  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 20
            redis-rate-limiter.burstCapacity: 40
            key-resolver: "#{@userKeyResolver}"

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS}

# OAuth2 Client — для Authorization Code flow с Keycloak
oauth2:
  keycloak:
    base-url: ${KEYCLOAK_URL}
    realm: ${KEYCLOAK_REALM}
    client-id: ${KEYCLOAK_CLIENT_ID}
    client-secret: ${KEYCLOAK_CLIENT_SECRET}
    # callback URI должен быть зарегистрирован в Keycloak клиенте
    redirect-uri: ${GATEWAY_URL}/api/v1/auth/oauth2/callback

# URL для финального редиректа на фронтенд после успешного OAuth2
frontend:
  url: ${FRONTEND_URL}

# user-service для синхронизации профиля после OAuth2
services:
  user-service-url: ${USER_SERVICE_URL}

management:
  server:
    port: ${GATEWAY_MANAGEMENT_PORT}  # 9090 по умолчанию — свой у каждого сервиса
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
  propagation:
    type: w3c

logging:
  level:
    root: INFO
    sm.selflearn.samskrtam: DEBUG
```

---

## 9. Acceptance Criteria

- [ ] `POST /api/v1/auth/login` без токена → проксируется в user-service (не 401)
- [ ] `GET /api/v1/quiz/**` без токена → 401
- [ ] `GET /api/v1/content/public/quizzes` с токеном STUDENT → проксируется
- [ ] `GET /api/v1/content/**` с токеном STUDENT → 403
- [ ] `GET /api/v1/content/**` с токеном ADMIN → проксируется
- [ ] Downstream сервис получает X-User-Id, X-User-Role, X-User-Locale
- [ ] Rate limit превышен → 429
- [ ] `/actuator/health` без токена → 200
- [ ] 401 и 403 возвращают JSON, не HTML
- [ ] `GET /api/v1/auth/oauth2/google` → 302 на Keycloak Authorization Endpoint
- [ ] `GET /api/v1/auth/oauth2/mailru` → 302 на Keycloak Authorization Endpoint (mailru provider)
- [ ] Callback с валидным code и state → токен передаётся на фронтенд
- [ ] Callback с невалидным / истёкшим state → 400 Bad Request
- [ ] `client_secret` никогда не передаётся фронтенду и не логируется

---

## 10. Открытые вопросы

- [ ] CORS origins: только localhost:3000 или настраиваемые через env?
- [ ] Distributed tracing: реализовано через Micrometer + OpenTelemetry + Tempo (см. conventions.md)
- [x] Rate limiting для `/api/v1/auth/**` — добавить отдельный лимит против brute force: `replenishRate=5, burstCapacity=10` (уже указано в conventions.md, раздел Rate Limiting)

## 11. Требования безопасности сети (M1)

Поскольку downstream сервисы доверяют заголовкам `X-User-*` без криптографической проверки, сетевая изоляция является **обязательным требованием**, а не опциональным улучшением:

```yaml
# k8s/services/api-gateway/network-policy.yaml
# Разрешить всем сервисам принимать трафик ТОЛЬКО от api-gateway
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: allow-from-gateway-only
  namespace: samskrtam
spec:
  podSelector:
    matchExpressions:
      - key: app
        operator: In
        values:
          - user-service
          - content-service
          - quiz-service
          - dictionary-service
          - statistics-service
  ingress:
    - from:
        - podSelector:
            matchLabels:
              app: api-gateway
  policyTypes:
    - Ingress
```

> ⚠️ Без этой политики любой под в namespace может подделать `X-User-Id` и `X-User-Role` и получить доступ к данным любого пользователя.



## Authentication Responsibilities
Gateway owns login, refresh, logout, OAuth2 callback and JWT validation.

Registration and forgot-password are handled by user-service.
