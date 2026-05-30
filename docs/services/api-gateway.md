# API Gateway — Spring Cloud Gateway

> Язык: **Java 21 + WebFlux** (Gateway требует реактивный стек)
> Модуль: `infrastructure/api-gateway`
> Порт: 8090
> Status: **DRAFT**

---

## 1. Ответственность

- Единая точка входа для всех клиентов
- Валидация JWT через Keycloak JWKS (кроме публичных endpoints)
- Добавление X-User-* заголовков для downstream сервисов
- Маршрутизация запросов по path
- Rate limiting через Redis
- CORS для фронтенда

Gateway **не содержит бизнес-логики** — только инфраструктурные фильтры.

> **Примечание:** Spring Cloud Gateway построен на Reactor/WebFlux —
> Virtual Threads здесь неприменимы. Это единственный Java сервис
> который использует реактивный стек.

---

## 2. Зависимости

```kotlin
// infrastructure/api-gateway/build.gradle.kts
plugins {
    alias(libs.plugins.spring.boot)
    id("java")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(21) }
}

dependencies {
    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.security.oauth2)
    implementation(libs.spring.redis.reactive)
}
```

---

## 3. Таблица маршрутов

| Path | Сервис | Auth |
|---|---|---|
| `/api/v1/auth/**` | auth-service:8087 | **Public** |
| `/api/v1/content/public/**` | content-service:8081 | STUDENT |
| `/api/v1/content/**` | content-service:8081 | ADMIN |
| `/api/v1/quiz/**` | quiz-service:8082 | STUDENT |
| `/api/v1/dictionary/**` | dictionary-service:8085 | STUDENT |
| `/api/v1/statistics/**` | statistics-service:8086 | STUDENT |
| `/actuator/health` | gateway | Public |

> `/api/v1/auth/**` — публичный маршрут. Сюда приходят запросы логина,
> регистрации, восстановления пароля — до получения токена.
> Безопасность обеспечивает сам auth-service.

> `/api/v1/content/public/**` — публичный (для STUDENT) маршрут к content-service.
> Используется для чтения списка квизов на главной странице.
> Доступ на запись (POST/PUT/DELETE) закрыт на уровне content-service.

> ⚠️ Маршруты определены в `GatewayRoutesConfig.java` через Java DSL — не в `application.yml`.
> В `application.yml` только `default-filters` (rate limiting).
> Если маршрут не проксируется — сначала проверяй `GatewayRoutesConfig.java`.

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
                    Jwt jwt = auth.getToken();
                    Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                    List<?> roles = realmAccess != null
                            ? (List<?>) realmAccess.get("roles")
                            : List.of();
                    String role = roles.isEmpty() ? "STUDENT" : roles.get(0).toString();

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id",     jwt.getSubject())
                            .header("X-User-Role",   role)
                            .header("X-User-Locale", Optional.ofNullable(
                                    jwt.getClaimAsString("locale")).orElse("ru"))
                            .build();

                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                // Публичный маршрут — нет principal, пропускаем без заголовков
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() { return -2; }
}
```

---

## 7. Rate Limiting

```yaml
# application.yml
spring:
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 20
            redis-rate-limiter.burstCapacity: 40
            # "#{@userKeyResolver}" — Spring EL, ссылается на бин userKeyResolver
            # Бин объявлен в GatewayRoutesConfig.java — оба файла обязательны
            key-resolver: "#{@userKeyResolver}"
```

```java
// GatewayRoutesConfig.java — бин на который ссылается key-resolver выше
@Bean
public KeyResolver userKeyResolver() {
    return exchange -> exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class)
            .map(auth -> auth.getToken().getSubject())
            .onErrorReturn("anonymous")
            .defaultIfEmpty("anonymous");
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
          jwk-set-uri: ${KEYCLOAK_JWKS_URI:http://keycloak:8080/realms/samskrtam/protocol/openid-connect/certs}

  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}

  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 20
            redis-rate-limiter.burstCapacity: 40
            key-resolver: "#{@userKeyResolver}"

cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}

management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true
      show-details: never

logging:
  level:
    root: INFO
    sm.selflearn.samskrtam: DEBUG
  pattern:
    console: '{"time":"%d{ISO8601}","level":"%p","service":"api-gateway","trace":"%X{traceId}","msg":"%m"}%n'
```

---

## 9. Acceptance Criteria

- [ ] `POST /api/v1/auth/login` без токена → проксируется в auth-service (не 401)
- [ ] `GET /api/v1/quiz/**` без токена → 401
- [ ] `GET /api/v1/content/public/quizzes` с токеном STUDENT → проксируется
- [ ] `GET /api/v1/content/**` с токеном STUDENT → 403
- [ ] `GET /api/v1/content/**` с токеном ADMIN → проксируется
- [ ] Downstream сервис получает X-User-Id, X-User-Role, X-User-Locale
- [ ] Rate limit превышен → 429
- [ ] `/actuator/health` без токена → 200
- [ ] 401 и 403 возвращают JSON, не HTML

---

## 10. Открытые вопросы

- [ ] CORS origins: только localhost:3000 или настраиваемые через env?
- [ ] Distributed tracing: Micrometer + Zipkin?
- [ ] Rate limiting для /api/v1/auth/** — отдельный лимит против brute force?
