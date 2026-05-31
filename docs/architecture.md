# Architecture — Топология и монорепозиторий

> Связанные файлы: [README.md](README.md) · [conventions.md](conventions.md) · [infra/keycloak.md](infra/keycloak.md) · [api-gateway.md](services/api-gateway.md)
> Status: **DRAFT**

---

## 1. Соглашения по именованию

| Элемент | Паттерн | Пример |
|---|---|---|
| Base package | `sm.selflearn.samskrtam.<сервис>` | `sm.selflearn.samskrtam.dictionary` |
| Gradle group | `sm.selflearn` | — |
| artifactId | `samskrtam-<сервис>-service` | `samskrtam-dictionary-service` |
| Main class (Java) | `sm.selflearn.samskrtam.<сервис>.Application` | — |
| Main class (Kotlin) | `sm.selflearn.samskrtam.<сервис>.ApplicationKt` | — |
| Docker image | `registry.gitlab.local/samskrtam/<сервис>-service` | — |

### Пакеты по сервисам

| Сервис | Язык | Base package |
|---|---|---|
| api-gateway | Java 21 | `sm.selflearn.samskrtam.gateway` |
| user-service | Java 21 | `sm.selflearn.samskrtam.user` |
| content-service | Java 21 | `sm.selflearn.samskrtam.content` |
| quiz-service | Java 21 + WebFlux | `sm.selflearn.samskrtam.quiz` |
| dictionary-service | **Kotlin** | `sm.selflearn.samskrtam.dictionary` |
| statistics-service | Java 21 | `sm.selflearn.samskrtam.statistics` |
| shared/kafka-events | Java 21 | `sm.selflearn.samskrtam.events` |
| shared/common-dto | Java 21 | `sm.selflearn.samskrtam.common` |

---

## 2. Физическая инфраструктура

```
┌─────────────────────────────────────────────────────────────┐
│                    Локальная сеть                            │
│                                                             │
│  ┌──────────────────┐     ┌──────────────────────────────┐  │
│  │   VM-1: GitLab   │     │   Kubernetes Cluster         │  │
│  │                  │     │                              │  │
│  │  GitLab CE       │     │  VM-2: control-plane         │  │
│  │  GitLab Runner   │◄────│  VM-3: worker-1              │  │
│  │  Container       │     │  VM-4: worker-2              │  │
│  │  Registry        │     │  VM-5: worker-3              │  │
│  │                  │     │                              │  │
│  │  gitlab.local    │     │  Portainer (управление)      │  │
│  └──────────────────┘     └──────────────────────────────┘  │
│                                                             │
│  Рабочая машина: Docker Compose (локальная разработка)      │
└─────────────────────────────────────────────────────────────┘
```

| VM | Роль | Сервисы |
|---|---|---|
| VM-1 | GitLab | GitLab CE, GitLab Runner, Container Registry |
| VM-2 | k8s control plane | kube-apiserver, etcd, scheduler, GitLab Agent |
| VM-3 | k8s worker-1 | Pod рабочая нагрузка |
| VM-4 | k8s worker-2 | Pod рабочая нагрузка |
| VM-5 | k8s worker-3 | Pod рабочая нагрузка + Portainer Agent |

---

## 3. Монорепозиторий — структура

```
samskrtam-app/
├── docs/                             ← спецификации SDD
├── gradle/
│   └── libs.versions.toml
├── settings.gradle.kts
├── build.gradle.kts
├── docker-compose.yml
├── docker-compose.override.yml
│
├── infrastructure/
│   ├── api-gateway/                  ← Java 21 + WebFlux
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/gateway/
│   │           ├── Application.java
│   │           ├── GatewayRoutesConfig.java
│   │           ├── SecurityConfig.java
│   │           └── filter/
│   │               ├── IdentityHeaderFilter.java
│   │               ├── RateLimitFilter.java
│   │               └── RequestIdFilter.java
│   ├── keycloak/
│   │   └── realm-export.json
│   ├── tempo/
│   │   └── tempo.yaml
│   ├── loki/
│   │   └── loki.yaml
│   ├── prometheus/
│   │   └── prometheus.yaml
│   └── grafana/
│       ├── datasources/
│       └── dashboards/
│
├── user-service/                 ← Java 21 + Virtual Threads
│   ├── build.gradle.kts
│   └── src/main/java/
│       └── sm/selflearn/samskrtam/auth/
│           ├── Application.java
│           ├── controller/
│           │   └── AuthController.java
│           ├── service/
│           │   ├── TokenService.java
│           │   └── KeycloakAdminService.java
│           └── client/
│               └── KeycloakClient.java
│
├── services/
│   ├── content-service/              ← Java 21 + Virtual Threads
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/content/
│   │           ├── Application.java
│   │           ├── QuizController.java
│   │           ├── QuizService.java
│   │           ├── model/
│   │           └── dto/
│   │
│   ├── quiz-service/                ← Java 21 + WebFlux + R2DBC
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/quiz/
│   │
│   ├── dictionary-service/           ← Kotlin + Coroutines
│   │   ├── build.gradle.kts
│   │   └── src/main/kotlin/
│   │       └── sm/selflearn/samskrtam/dictionary/
│   │           ├── Application.kt
│   │           ├── DictionaryController.kt
│   │           ├── DictionaryService.kt
│   │           ├── model/
│   │           ├── external/
│   │           └── dto/
│   │
│   └── statistics-service/           ← Java 21 + Virtual Threads
│       └── src/main/java/
│           └── sm/selflearn/samskrtam/statistics/
│               ├── Application.java
│               ├── StatisticsController.java
│               ├── consumer/
│               ├── model/
│               └── dto/
│
├── shared/
│   ├── kafka-events/                 ← Java 21 (совместимость со всеми)
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/events/
│   │           ├── AnswerSubmitted.java
│   │           ├── SessionCompleted.java
│   │           └── QuizType.java
│   └── common-dto/                   ← Java 21
│       └── src/main/java/
│           └── sm/selflearn/samskrtam/common/
│               └── ErrorResponse.java
│
├── frontend/
│   └── src/
│
├── k8s/
│   ├── namespace.yaml
│   ├── infrastructure/
│   │   ├── postgres/
│   │   ├── kafka/
│   │   ├── redis/
│   │   └── keycloak/
│   └── services/
│       ├── api-gateway/
│       ├── user-service/
│       ├── content-service/
│       ├── quiz-service/
│       ├── dictionary-service/
│       └── statistics-service/
│
└── .gitlab-ci.yml
```

---

## 4. Gradle

### settings.gradle.kts

```kotlin
rootProject.name = "samskrtam-app"

include(
    ":infrastructure:api-gateway",
    ":services:user-service",
    ":services:content-service",
    ":services:quiz-service",
    ":services:dictionary-service",
    ":services:statistics-service",
    ":shared:kafka-events",
    ":shared:common-dto"
)
```

### gradle/libs.versions.toml

```toml
[versions]
java = "21"
kotlin = "2.0.0"
spring-boot = "3.3.0"
spring-cloud = "2023.0.1"
coroutines = "1.8.1"
kotest = "5.9.0"
postgresql-jdbc = "42.7.3"
postgresql-r2dbc = "1.0.4"
flyway = "10.0.0"

[libraries]
# Spring Boot starters
spring-web             = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-webflux         = { module = "org.springframework.boot:spring-boot-starter-webflux" }
spring-data-jpa        = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }
spring-r2dbc           = { module = "org.springframework.boot:spring-boot-starter-data-r2dbc" }
spring-security-oauth2 = { module = "org.springframework.boot:spring-boot-starter-oauth2-resource-server" }
spring-kafka           = { module = "org.springframework.kafka:spring-kafka" }
spring-redis           = { module = "org.springframework.boot:spring-boot-starter-data-redis" }
spring-redis-reactive  = { module = "org.springframework.boot:spring-boot-starter-data-redis-reactive" }
spring-cloud-gateway   = { module = "org.springframework.cloud:spring-cloud-starter-gateway" }

# JDBC / R2DBC
postgresql-jdbc        = { module = "org.postgresql:postgresql",        version.ref = "postgresql-jdbc" }
postgresql-r2dbc       = { module = "org.postgresql:r2dbc-postgresql",  version.ref = "postgresql-r2dbc" }

# Flyway
flyway-core            = { module = "org.flywaydb:flyway-core",         version.ref = "flyway" }
flyway-postgresql      = { module = "org.flywaydb:flyway-database-postgresql", version.ref = "flyway" }

# Kotlin (только для dictionary-service)
coroutines-core        = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core",    version.ref = "coroutines" }
coroutines-reactor     = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor", version.ref = "coroutines" }

# Testing
spring-test            = { module = "org.springframework.boot:spring-boot-starter-test" }
kotest-runner          = { module = "io.kotest:kotest-runner-junit5",                   version.ref = "kotest" }
kotest-spring          = { module = "io.kotest.extensions:kotest-extensions-spring",    version.ref = "kotest" }

[plugins]
kotlin-jvm     = { id = "org.jetbrains.kotlin.jvm",           version.ref = "kotlin" }
kotlin-spring  = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
spring-boot    = { id = "org.springframework.boot",           version.ref = "spring-boot" }
```

### Корневой build.gradle.kts

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)    apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot)   apply false
}

subprojects {
    group   = "sm.selflearn"
    version = "0.0.1-SNAPSHOT"
}
```

### Шаблон build.gradle.kts — Java 21 сервис (Virtual Threads)

```kotlin
// services/content-service/build.gradle.kts
plugins {
    alias(libs.plugins.spring.boot)
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(libs.spring.web)            // Spring MVC (не WebFlux!)
    implementation(libs.spring.data.jpa)
    implementation(libs.spring.security.oauth2)
    implementation(libs.postgresql.jdbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(project(":shared:common-dto"))

    testImplementation(libs.spring.test)
}

// Virtual Threads включаются одной строкой в application.yml:
// spring.threads.virtual.enabled: true
```

### build.gradle.kts — Kotlin сервис (dictionary-service)

```kotlin
// services/dictionary-service/build.gradle.kts
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.spring.webflux)
    implementation(libs.spring.r2dbc)
    implementation(libs.spring.security.oauth2)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.reactor)
    implementation(libs.postgresql.r2dbc)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(project(":shared:common-dto"))

    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.spring)
}

springBoot {
    mainClass = "sm.selflearn.samskrtam.dictionary.ApplicationKt"
}
```

---

## 5. Технологический стек

### Java 21 сервисы (Virtual Threads)

| Технология | Назначение |
|---|---|
| Java 21 | Язык + Virtual Threads (Project Loom) |
| Spring Boot 3.3 | Фреймворк |
| Spring MVC | HTTP (блокирующий стиль, VT делает его async) |
| Spring Data JPA | Доступ к БД (обычный JDBC) |
| PostgreSQL JDBC | Драйвер БД |
| Flyway | Миграции |
| JUnit 5 | Тестирование |

```yaml
# application.yml для всех Java 21 сервисов
spring:
  threads:
    virtual:
      enabled: true   # одна строка включает Virtual Threads
```

### Kotlin сервис (dictionary-service)

| Технология | Назначение |
|---|---|
| Kotlin 2.0 | Язык |
| Spring Boot 3.3 | Фреймворк |
| Spring WebFlux | Реактивный HTTP |
| Kotlin Coroutines | suspend fun поверх Reactor |
| Spring Data R2DBC | Реактивный доступ к БД |
| PostgreSQL R2DBC | Реактивный драйвер |
| Kotest | Тестирование |

### API Gateway

| Технология | Назначение |
|---|---|
| Java 21 | Язык |
| Spring Cloud Gateway | Маршрутизация |
| Spring WebFlux | Реактивный стек (обязателен для Gateway) |
| Spring Security OAuth2 | JWT валидация |
| Redis | Rate limiting |

### Инфраструктура

| Технология | Назначение |
|---|---|
| Keycloak 24 | Identity Provider |
| PostgreSQL 16 | БД (своя схема на сервис) |
| Kafka 3.x | Очередь событий |
| Redis 7 | Rate limiting, сессии |
| GitLab CE | Git, CI/CD, Container Registry |
| GitLab Agent | Подключение k8s к GitLab |
| Portainer | Визуальное управление кластером |

### Frontend

| Технология | Назначение |
|---|---|
| React 18 | UI |
| TypeScript 5 | Язык |
| React Query | Server state |
| Zustand | Client state |
| i18next | i18n (ru/en) |

---

## 6. Принципы межсервисного взаимодействия

### Синхронное (REST через Gateway)
```
Browser → API Gateway → Сервис
```

### Асинхронное (Kafka)
```
Quiz Service → [Kafka topic] → Statistics Service
```

### Правило заголовков от Gateway
```
X-User-Id:     <uuid>    ← из JWT sub
X-User-Role:   <role>    ← из JWT realm_access.roles[0]
X-User-Locale: <locale>  ← из JWT locale
X-Request-Id:  <uuid>    ← для distributed tracing
```

Микросервисы **не валидируют JWT** — доверяют заголовкам от Gateway.

---

## 7. Изоляция БД по сервисам

```
PostgreSQL instance
├── schema: users         ← user-service         (JPA/JDBC)
├── schema: content       ← content-service      (JPA/JDBC)
├── schema: quiz          ← quiz-service         (R2DBC + Flyway/JDBC)
├── schema: dictionary    ← dictionary-service    (R2DBC)
└── schema: statistics    ← statistics-service    (JPA/JDBC)
```

Сервисы не делают JOIN через схемы — только через API.

---

## 8. Docker Compose (локальная разработка)

```yaml
services:

  keycloak:
    image: quay.io/keycloak/keycloak:24.0
    command: start-dev --import-realm
    ports: ["8080:8080"]
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    volumes:
      - ./infrastructure/keycloak/realm-export.json:/opt/keycloak/data/import/realm.json

  postgres:
    image: postgres:16-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_PASSWORD: samskrtam
      POSTGRES_DB: samskrtam

  kafka:
    image: confluentinc/cp-kafka:7.6.0
    ports: ["9092:9092"]
    environment:
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  api-gateway:
    build: ./infrastructure/api-gateway
    ports:
      - "8090:8090"
      - "${GATEWAY_MANAGEMENT_PORT}:${GATEWAY_MANAGEMENT_PORT}"
    depends_on: [keycloak, redis]
    environment:
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/samskrtam/protocol/openid-connect/certs
      SPRING_DATA_REDIS_HOST: redis
      MANAGEMENT_SERVER_PORT: ${GATEWAY_MANAGEMENT_PORT}
      MANAGEMENT_TRACING_SAMPLING_PROBABILITY: ${TRACING_SAMPLING_PROBABILITY}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}

  feature-flag-service:
    build: ./services/feature-flag-service
    ports:
      - "8088:8088"
      - "${FEATURE_FLAG_MANAGEMENT_PORT}:${FEATURE_FLAG_MANAGEMENT_PORT}"
    depends_on: [redis]
    environment:
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PORT: 6379
      MANAGEMENT_SERVER_PORT: ${FEATURE_FLAG_MANAGEMENT_PORT}
      MANAGEMENT_TRACING_SAMPLING_PROBABILITY: ${TRACING_SAMPLING_PROBABILITY}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}

  user-service:
    build: ./services/user-service
    ports:
      - "8087:8087"
      - "${USER_MANAGEMENT_PORT}:${USER_MANAGEMENT_PORT}"
    depends_on: [postgres, keycloak, minio]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam
      KEYCLOAK_URL: http://keycloak:8080
      KEYCLOAK_REALM: samskrtam
      KEYCLOAK_CLIENT_SECRET: ${KEYCLOAK_CLIENT_SECRET}
      MINIO_URL: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY}
      MINIO_SECRET_KEY: ${MINIO_SECRET_KEY}
      MINIO_PUBLIC_URL: ${MINIO_PUBLIC_URL}
      SPRING_THREADS_VIRTUAL_ENABLED: "true"
      MANAGEMENT_SERVER_PORT: ${USER_MANAGEMENT_PORT}
      MANAGEMENT_TRACING_SAMPLING_PROBABILITY: ${TRACING_SAMPLING_PROBABILITY}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}

  minio:
    image: minio/minio:RELEASE.2024-05-01T01-11-10Z
    ports: ["9000:9000", "9001:9001"]
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY}
    command: server /data --console-address ":9001"
    volumes:
      - minio_data:/data

  content-service:
    build: ./services/content-service
    ports:
      - "8081:8081"
      - "${CONTENT_MANAGEMENT_PORT}:${CONTENT_MANAGEMENT_PORT}"
    depends_on: [postgres]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam?currentSchema=content
      SPRING_THREADS_VIRTUAL_ENABLED: "true"
      MANAGEMENT_SERVER_PORT: ${CONTENT_MANAGEMENT_PORT}
      MANAGEMENT_TRACING_SAMPLING_PROBABILITY: ${TRACING_SAMPLING_PROBABILITY}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}

  quiz-service:
    build: ./services/quiz-service
    ports:
      - "8082:8082"
      - "${QUIZ_MANAGEMENT_PORT}:${QUIZ_MANAGEMENT_PORT}"
    depends_on: [postgres, content-service, kafka, redis]
    environment:
      SPRING_R2DBC_URL: r2dbc:postgresql://postgres:5432/samskrtam
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam
      CONTENT_SERVICE_URL: http://content-service:8081
      SPRING_DATA_REDIS_HOST: redis
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      SPRING_THREADS_VIRTUAL_ENABLED: "true"
      MANAGEMENT_SERVER_PORT: ${QUIZ_MANAGEMENT_PORT}
      MANAGEMENT_TRACING_SAMPLING_PROBABILITY: ${TRACING_SAMPLING_PROBABILITY}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}

  dictionary-service:
    build: ./services/dictionary-service
    ports:
      - "8085:8085"
      - "${DICTIONARY_MANAGEMENT_PORT}:${DICTIONARY_MANAGEMENT_PORT}"
    depends_on: [postgres]
    environment:
      SPRING_R2DBC_URL: r2dbc:postgresql://postgres:5432/samskrtam?schema=dictionary
      MANAGEMENT_SERVER_PORT: ${DICTIONARY_MANAGEMENT_PORT}
      MANAGEMENT_TRACING_SAMPLING_PROBABILITY: ${TRACING_SAMPLING_PROBABILITY}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}

  statistics-service:
    build: ./services/statistics-service
    ports:
      - "8086:8086"
      - "${STATISTICS_MANAGEMENT_PORT}:${STATISTICS_MANAGEMENT_PORT}"
    depends_on: [postgres, kafka]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam?currentSchema=statistics
      SPRING_THREADS_VIRTUAL_ENABLED: "true"
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      MANAGEMENT_SERVER_PORT: ${STATISTICS_MANAGEMENT_PORT}
      MANAGEMENT_TRACING_SAMPLING_PROBABILITY: ${TRACING_SAMPLING_PROBABILITY}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${OTEL_EXPORTER_OTLP_ENDPOINT}

  tempo:
    image: grafana/tempo:latest
    ports: ["3200:3200", "4317:4317"]
    volumes:
      - ./infrastructure/tempo/tempo.yaml:/etc/tempo.yaml
    command: ["-config.file=/etc/tempo.yaml"]

  loki:
    image: grafana/loki:latest
    ports: ["3100:3100"]
    volumes:
      - ./infrastructure/loki/loki.yaml:/etc/loki/local-config.yaml

  prometheus:
    image: prom/prometheus:latest
    ports: ["9090:9090"]
    volumes:
      - ./infrastructure/prometheus/prometheus.yaml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:latest
    ports: ["3001:3000"]
    depends_on: [tempo, loki, prometheus]
    environment:
      GF_AUTH_ANONYMOUS_ENABLED: "true"
      GF_AUTH_ANONYMOUS_ORG_ROLE: Admin
    volumes:
      - ./infrastructure/grafana/datasources:/etc/grafana/provisioning/datasources
      - ./infrastructure/grafana/dashboards:/etc/grafana/provisioning/dashboards

  frontend:
    build: ./frontend
    ports: ["3000:3000"]
    environment:
      VITE_API_URL: http://localhost:8090
```

### Порты сервисов

| Сервис | Порт |
|---|---|
| Frontend | 3000 |
| API Gateway | 8090 |
| Keycloak | 8080 |
| user-service | 8087 |
| MinIO API | 9000 |
| MinIO Console | 9001 |
| content-service | 8081 |
| quiz-service | 8082 |
| api-gateway (main) | 8090 |
| feature-flag-service | 8088 |
| user-service | 8087 |
| content-service | 8081 |
| quiz-service | 8082 |
| dictionary-service | 8085 |
| statistics-service | 8086 |
| PostgreSQL | 5432 |
| Kafka | 9092 |
| Redis | 6379 |
| Tempo (OTLP HTTP) | 4318 |
| Tempo (OTLP gRPC) | 4317 |
| Tempo (HTTP API) | 3200 |
| Prometheus | 9090 |
| Loki | 3100 |
| Grafana | 3001 |
| **Management (Actuator) порты** | |
| api-gateway management | 9090 (`GATEWAY_MANAGEMENT_PORT`) |
| feature-flag-service management | 9091 (`FEATURE_FLAG_MANAGEMENT_PORT`) |
| user-service management | 9092 (`USER_MANAGEMENT_PORT`) |
| content-service management | 9093 (`CONTENT_MANAGEMENT_PORT`) |
| quiz-service management | 9094 (`QUIZ_MANAGEMENT_PORT`) |
| dictionary-service management | 9095 (`DICTIONARY_MANAGEMENT_PORT`) |
| statistics-service management | 9096 (`STATISTICS_MANAGEMENT_PORT`) |

> Management порты настраиваются через `.env` — у каждого сервиса свой,
> чтобы можно было запускать все сервисы локально без конфликтов.
> Подробнее — conventions.md раздел 4.

---

## 9. Kubernetes манифесты

Манифесты для развертывания всего приложения в Kubernetes кластере хранятся в директории `k8s/`. Они используют параметризацию для тегов образов (`${IMAGE_TAG}`), которые подставляются на этапе CI/CD.

### Структура `k8s/`

Структура директории организована для разделения инфраструктурных компонентов (БД, очереди) и бизнес-сервисов приложения.

```
k8s/
├── namespace.yaml                  # Неймспейс для всего приложения
│
├── infrastructure/                 # Манифесты для stateful-сервисов
│   ├── postgres/
│   │   ├── persistentvolumeclaim.yaml
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   └── secret.yaml
│   ├── kafka/
│   ├── redis/
│   └── keycloak/
│
└── services/                       # Манифесты для stateless-сервисов
    └── <service-name>/
        ├── deployment.yaml         # Описание развертывания (образ, реплики, env)
        ├── service.yaml            # Открытие доступа к поду внутри кластера
        ├── configmap.yaml          # Конфигурация (не-секретная)
        └── secret.yaml             # Секреты (пароли, токены)
```

### Общие манифесты

#### namespace.yaml

Все ресурсы приложения создаются в одном неймспейсе `samskrtam` для изоляции от других приложений в кластере.

```yaml
# k8s/namespace.yaml
apiVersion: v1
kind: Namespace
metadata:
  name: samskrtam
```

### Шаблоны манифестов для сервисов

Для каждого микросервиса (`content-service`, `dictionary-service` и т.д.) используется стандартный набор манифестов.

#### Шаблон Deployment

Этот манифест описывает, как запустить под с контейнером приложения. Он включает в себя ссылку на Docker-образ, переменные окружения, пробы (liveness/readiness) и запросы/лимиты по ресурсам.

```yaml
# k8s/services/content-service/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: content-service
  namespace: samskrtam
spec:
  replicas: 1
  selector:
    matchLabels:
      app: content-service
  template:
    metadata:
      labels:
        app: content-service
    spec:
      containers:
        - name: content-service
          image: registry.gitlab.local/samskrtam/content-service:${IMAGE_TAG}
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8081
            - containerPort: 9093   # CONTENT_MANAGEMENT_PORT — см. conventions.md
          envFrom:
            - configMapRef:
                name: content-service-config
            - secretRef:
                name: content-service-secret
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 9093
            initialDelaySeconds: 45
            periodSeconds: 15
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 9093
            initialDelaySeconds: 30
            periodSeconds: 10
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
```

#### Шаблон Service

`Service` предоставляет подам стабильный IP-адрес и DNS-имя внутри кластера, позволяя другим сервисам обращаться к ним по имени (например, `http://content-service:8081`).

```yaml
# k8s/services/content-service/service.yaml
apiVersion: v1
kind: Service
metadata:
  name: content-service
  namespace: samskrtam
spec:
  selector:
    app: content-service
  ports:
    - protocol: TCP
      port: 8081
      targetPort: 8081
```

#### Шаблон ConfigMap

`ConfigMap` используется для хранения конфигурации, которая не является секретной. Эти значения передаются в контейнер как переменные окружения.

```yaml
# k8s/services/content-service/configmap.yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: content-service-config
  namespace: samskrtam
data:
  SPRING_THREADS_VIRTUAL_ENABLED: "true"
  SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: "http://keycloak.samskrtam:8080/realms/samskrtam/protocol/openid-connect/certs"
  # Другие не-секретные переменные
```

#### Шаблон Secret

Секреты хранятся в объекте `Secret` и так же передаются в контейнер. Манифесты секретов не хранятся в Git в открытом виде. Они создаются в кластере вручную или через CI/CD с помощью `kubectl create secret`.

Пример команды для создания секрета с паролем от БД:

```bash
kubectl create secret generic content-service-secret \
  --namespace=samskrtam \
  --from-literal=SPRING_DATASOURCE_URL='jdbc:postgresql://postgres.samskrtam:5432/samskrtam?currentSchema=content' \
  --from-literal=SPRING_DATASOURCE_USERNAME='samskrtam_user' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='<your-db-password>'
```

### Инфраструктурные компоненты

Для stateful-сервисов, таких как PostgreSQL, используются `Deployment` вместе с `PersistentVolumeClaim` для сохранения данных. В реальном продуктовом окружении для управления такими компонентами предпочтительнее использовать **Helm-чарты** или **Операторы**, но для упрощения в данном проекте используются базовые манифесты.

#### PersistentVolumeClaim для PostgreSQL

Этот манифест запрашивает у кластера дисковое пространство для хранения данных PostgreSQL.

```yaml
# k8s/infrastructure/postgres/persistentvolumeclaim.yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: samskrtam
spec:
  accessModes:
    - ReadWriteOnce # Монопольный доступ для одного пода
  resources:
    requests:
      storage: 5Gi # Запрашиваем 5 ГБ диска
```

---

## 10. CI/CD — GitLab

### GitLab Agent for Kubernetes

```yaml
# k8s/.gitlab/agents/samskrtam-agent/config.yaml
ci_access:
  projects:
    - id: samskrtam/samskrtam-app
```

### .gitlab-ci.yml

```yaml
stages:
  - test
  - build
  - deploy

variables:
  REGISTRY: registry.gitlab.local/samskrtam
  IMAGE_TAG: $CI_COMMIT_SHORT_SHA

# ── TEST ──────────────────────────────────────────
test:backend:
  stage: test
  image: eclipse-temurin:21-jdk
  script:
    - ./gradlew test
  artifacts:
    reports:
      junit: "**/build/test-results/**/*.xml"

test:frontend:
  stage: test
  image: node:20-alpine
  script:
    - cd frontend && npm ci && npm test -- --watchAll=false

# ── BUILD ─────────────────────────────────────────
.build-java-service: &build-java-service
  stage: build
  image: docker:24
  services: [docker:24-dind]
  before_script:
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $CI_REGISTRY
  script:
    - ./gradlew :services:${SERVICE}:bootJar
    - docker build -t $REGISTRY/${SERVICE}:$IMAGE_TAG ./services/${SERVICE}
    - docker push $REGISTRY/${SERVICE}:$IMAGE_TAG
    - docker tag  $REGISTRY/${SERVICE}:$IMAGE_TAG $REGISTRY/${SERVICE}:latest
    - docker push $REGISTRY/${SERVICE}:latest

build:api-gateway:
  <<: *build-java-service
  variables:
    SERVICE: api-gateway

build:user-service:
  <<: *build-java-service
  variables:
    SERVICE: user-service

build:content-service:
  <<: *build-java-service
  variables:
    SERVICE: content-service

build:quiz-service:
  <<: *build-java-service
  variables:
    SERVICE: quiz-service

build:dictionary-service:
  <<: *build-java-service        # тот же процесс, Kotlin компилируется Gradle
  variables:
    SERVICE: dictionary-service

build:statistics-service:
  <<: *build-java-service
  variables:
    SERVICE: statistics-service

build:frontend:
  stage: build
  image: node:20-alpine
  script:
    - cd frontend && npm ci && npm run build
    - docker build -t $REGISTRY/frontend:$IMAGE_TAG ./frontend
    - docker push $REGISTRY/frontend:$IMAGE_TAG

# ── DEPLOY ────────────────────────────────────────
deploy:kubernetes:
  stage: deploy
  image: bitnami/kubectl:latest
  environment:
    name: production
    url: http://samskrtam.local
  when: manual
  only:
    - main
  script:
    - find k8s/services -name "deployment.yaml"
        -exec sed -i "s|\${IMAGE_TAG}|$IMAGE_TAG|g" {} \;
    - kubectl apply -f k8s/namespace.yaml
    - kubectl apply -f k8s/infrastructure/
    - kubectl apply -f k8s/services/
    - kubectl rollout status deployment/api-gateway     -n samskrtam
    - kubectl rollout status deployment/content-service -n samskrtam
    - kubectl rollout status deployment/dictionary-service -n samskrtam
    - kubectl rollout status deployment/statistics-service -n samskrtam
```

### GitLab CI Variables

| Variable | Описание | Protected |
|---|---|---|
| `CI_REGISTRY` | registry.gitlab.local | No |
| `CI_REGISTRY_USER` | Registry user | No |
| `CI_REGISTRY_PASSWORD` | Registry password | Yes |
| `DB_PASSWORD` | PostgreSQL password | Yes |
| `KEYCLOAK_ADMIN_PASSWORD` | Keycloak admin password | Yes |
| `KEYCLOAK_CLIENT_SECRET` | client_secret для samskrtam-frontend (confidential) | Yes |

---

## 11. Открытые вопросы

- [ ] Ingress controller — nginx-ingress или Traefik?
- [ ] Persistent storage для PostgreSQL — local-path или NFS между нодами?
- [ ] Отдельный namespace для инфраструктуры (postgres, kafka, redis)?
- [ ] Distributed tracing — Jaeger или Zipkin?
- [ ] Автоматический деплой на main или только ручной?
