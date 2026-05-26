# API Gateway — Spring Cloud Gateway

> Язык: **Java 21 + WebFlux** (Gateway требует реактивный стек)
> Модуль: `infrastructure/api-gateway`
> Порт: 8090
> Status: **DRAFT**

---

## 1. Ответственность

- Единая точка входа
- Валидация JWT через Keycloak JWKS
- Добавление X-User-* заголовков для downstream сервисов
- Маршрутизация по path
- Rate limiting через Redis
- CORS для фронтенда

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
    implementation(libs.spring.redis.reactive)    // Redis для rate limiting
}
```

---

## 3. Таблица маршрутов

| Path | Сервис | Auth |
|---|---|---|
| `/api/v1/content/**` | content-service:8081 | ADMIN |
| `/api/v1/quiz/declensions/**` | quiz-declensions-service:8082 | STUDENT |
| `/api/v1/quiz/conjugations/**` | quiz-conjugations-service:8083 | STUDENT |
| `/api/v1/quiz/vocabulary/**` | quiz-vocabulary-service:8084 | STUDENT |
| `/api/v1/dictionary/**` | dictionary-service:8085 | STUDENT |
| `/api/v1/statistics/**` | statistics-service:8086 | STUDENT |
| `/actuator/health` | gateway | Public |

---

## 4. GatewayRoutesConfig.java

```java
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("content-service", r -> r
                .path("/api/v1/content/**")
                .uri("http://content-service:8081"))
            .route("quiz-declensions", r -> r
                .path("/api/v1/quiz/declensions/**")
                .uri("http://quiz-declensions-service:8082"))
            .route("quiz-conjugations", r -> r
                .path("/api/v1/quiz/conjugations/**")
                .uri("http://quiz-conjugations-service:8083"))
            .route("quiz-vocabulary", r -> r
                .path("/api/v1/quiz/vocabulary/**")
                .uri("http://quiz-vocabulary-service:8084"))
            .route("dictionary", r -> r
                .path("/api/v1/dictionary/**")
                .uri("http://dictionary-service:8085"))
            .route("statistics", r -> r
                .path("/api/v1/statistics/**")
                .uri("http://statistics-service:8086"))
            .build();
    }
}
```

---

## 5. IdentityHeaderFilter.java

```java
@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
            .cast(JwtAuthenticationToken.class)
            .flatMap(auth -> {
                Jwt jwt = auth.getToken();
                Map<String, Object> realmAccess =
                    jwt.getClaimAsMap("realm_access");
                List<?> roles = realmAccess != null
                    ? (List<?>) realmAccess.get("roles")
                    : List.of();
                String role = roles.isEmpty()
                    ? "STUDENT"
                    : roles.get(0).toString();

                ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id",     jwt.getSubject())
                    .header("X-User-Role",   role)
                    .header("X-User-Locale", jwt.getClaimAsString("locale") != null
                        ? jwt.getClaimAsString("locale") : "ru")
                    .build();

                return chain.filter(exchange.mutate().request(mutated).build());
            })
            .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() { return -2; }
}
```

---

## 6. SecurityConfig.java

```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/actuator/health").permitAll()
                .pathMatchers("/api/v1/content/**").hasRole("ADMIN")
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwkSetUri(
                    "${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}"
                ))
            )
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .build();
    }
}
```

---

## 7. application.yml

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
      port: 6379
  cloud:
    gateway:
      default-filters:
        - name: RequestRateLimiter
          args:
            redis-rate-limiter.replenishRate: 20
            redis-rate-limiter.burstCapacity: 40
            key-resolver: "#{@userKeyResolver}"
```

---

## 8. Acceptance Criteria

- [ ] Запрос без JWT → 401
- [ ] Запрос с истёкшим JWT → 401
- [ ] STUDENT к `/api/v1/content/**` → 403
- [ ] ADMIN к `/api/v1/content/**` → проксируется
- [ ] Downstream сервис получает X-User-Id, X-User-Role, X-User-Locale
- [ ] Rate limit превышен → 429
- [ ] `/actuator/health` без токена → 200

---

## 9. Открытые вопросы

- [ ] CORS origins: только localhost:3000 или через env?
- [ ] Distributed tracing: Micrometer + Zipkin?
