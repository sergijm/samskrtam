# Architecture — Топология и монорепозиторий

> Связанные файлы: [README.md](README.md) · [conventions.md](conventions.md) · [infra/keycloak.md](infra/keycloak.md) · [api-gateway.md](services/api-gateway.md)
> Status: **UPDATED**

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

| Сервис | Язык | Base package | Описание изменений |
|---|---|---|---|
| api-gateway | Java 21 | `sm.selflearn.samskrtam.gateway` | Добавлена прямая обработка запросов на `/api/v1/auth/refresh` без проксирования в `user-service`. |
| user-service | Java 21 | `sm.selflearn.samskrtam.user` | |
| content-service | Java 21 | `sm.selflearn.samskrtam.samskrtam.content` | Логика генерации VOCABULARY квизов обновлена для учета иерархии категорий слов и инвариантного к регистру поиска по `slug`. |
| quiz-service | Java 21 + WebFlux + R2DBC | `sm.selflearn.samskrtam.quiz` | Использует Outbox Pattern для публикации событий в Kafka. Внутренние классы Outbox (Event, EventType, Status) перемещены в `sm.selflearn.samskrtam.quiz.outbox`. |
| statistics-service | Java 21 + Kafka Streams | `sm.selflearn.samskrtam.statistics` | Полностью переделан для расчета статистики с использованием Kafka Streams. |
| shared/quiz-dtos | Java 21 | `sm.selflearn.samskrtam.quiz` | Содержит внешние DTO для квизов, контента, статистики и **Kafka-событий** (`QuizAnsweredEvent`, `QuizSessionStatusChangedEvent`, `StatisticEvent`). |
| shared/common-dto | Java 21 | `sm.selflearn.samskrtam.common` | |
| dictionary-service | Java 21 | `sm.selflearn.samskrtam.dictionary` | Фронтенд теперь передает `slp1Normalized` в параметр `slp1Spelling` для запроса словарной статьи. |

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
│       └── sm/selflearn/samskrtam/user/
│           ├── Application.java
│           ├── controller/
│           │   └── UserProfileController.java
│   │       ├── service/
│   │       │   ├── UserProfileService.java
│   │       │   └── KeycloakAdminService.java
│   │       └── client/
│   │           └── KeycloakClient.java
│   │
├── services/
│   ├── content-service/              ← Java 21 + Virtual Threads
│   │   ├── build.gradle.kts
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/content/
│   │           ├── Application.java
│   │           ├── QuizController.java
│   │           ├── QuizService.java
│   │           ├── model/
│   │           │   └── VocabularyCategory.java (добавлено)
│   │           │   └── VocabularyWordCategory.java (добавлено)
│   │           ├── dto/
│   │           └── repository/
│   │               └── VocabularyCategoryRepository.java (добавлено)
│   │               └── VocabularyWordCategoryRepository.java (добавлено)
│   │
│   ├── quiz-service/                ← Java 21 + WebFlux + R2DBC
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/quiz/
│   │           ├── outbox/ (добавлено: OutboxEvent, OutboxEventType, OutboxStatus)
│   │           ├── scheduler/ (добавлено: OutboxEventPublisherService)
│   │           └── repository/
│   │               └── OutboxEventRepository.java (обновлено)
│   │
│   └── statistics-service/           ← Java 21 + Virtual Threads
│       └── src/main/java/
│           └── sm/selflearn/samskrtam/statistics/
│               ├── Application.java
│               ├── StatisticsController.java
│               ├── config/
│               │   └── KafkaStreamsConfig.java (добавлено)
│               ├── model/
│               └── dto/
│
├── shared/
│   ├── quiz-dtos/                    ← Java 21 (объединенные DTO для квизов, контента, статистики и событий Kafka)
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/quiz/
│   │           ├── event/ (обновлено: QuizAnsweredEvent, QuizSessionStatusChangedEvent, StatisticEvent)
│   │           └── ... (другие DTO)
│   └── common-dto/                   ← Java 21
│       └── src/main/java/
│           └── sm/selflearn/samskrtam/common/
│               └── ErrorResponse.java
│
├── frontend/
│   └── src/
│       └── api/
│       │   └── axios.ts (обновлено: сохранение redirectPath)
│       └── pages/
│       │   └── AuthCallbackPage.tsx (обновлено: использование redirectPath)
│       └── store/
│           └── authStore.ts (обновлено: управление redirectPath)
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
│       ├── feature-flag-service/
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
    ":services:statistics-service",
    ":shared:quiz-dtos", // Объединенный модуль
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
flyway = "11.20.3"
lombok = "1.18.32"
logstash-logback = "7.4"
spring-kafka = "3.1.5"
spring-kafka-streams = "3.1.5" # Добавлена версия для Kafka Streams
reactor-kafka = "1.3.23.RELEASE"
springdoc = "2.5.0"
jsoup = "1.17.2"
keycloak = "24.0.4"
minio = "8.5.1"
micrometer-tracing = "1.3.0"
opentelemetry = "1.38.0"
jackson = "2.17.0"

[libraries]
# Spring Boot starters
spring-web             = { module = "org.springframework.boot:spring-boot-starter-web" }
spring-webflux         = { module = "org.springframework.boot:spring-boot-starter-webflux" }
spring-data-jpa        = { module = "org.springframework.boot:spring-boot-starter-data-jpa" }
spring-r2dbc           = { module = "org.springframework.boot:spring-boot-starter-data-r2dbc" }
spring-security-oauth2 = { module = "org.springframework.boot:spring-boot-starter-oauth2-resource-server" }
spring-boot-starter-actuator = { module = "org.springframework.boot:spring-boot-starter-actuator" }
spring-boot-starter-mail = { module = "org.springframework.boot:spring-boot-starter-mail" }
spring-kafka           = { module = "org.springframework.kafka:spring-kafka", version.ref = "spring-kafka" }
spring-kafka-streams   = { module = "org.springframework.kafka:spring-kafka-streams", version.ref = "spring-kafka-streams" } # Добавлена зависимость Kafka Streams
spring-redis           = { module = "org.springframework.boot:spring-boot-starter-data-redis" }
spring-redis-reactive  = { module = "org.springframework.boot:spring-boot-starter-data-redis-reactive" }
spring-cloud-gateway   = { module = "org.springframework.cloud:spring-cloud-starter-gateway" }

# Lombok
lombok = { module = "org.projectlombok:lombok", version.ref = "lombok" }

# Logging
logstash-logback-encoder = { module = "net.logstash.logback:logstash-logback-encoder", version.ref = "logstash-logback" }

# JDBC / R2DBC
postgresql-jdbc        = { module = "org.postgresql:postgresql",        version.ref = "postgresql-jdbc" }
postgresql-r2dbc       = { module = "org.postgresql:r2dbc-postgresql",  version.ref = "postgresql-r2dbc" }

# Flyway
flyway-core            = { module = "org.flywaydb:flyway-core",         version.ref = "flyway" }
flyway-postgresql      = { module = "org.flywaydb:flyway-database-postgresql", version.ref = "flyway" }

# Kotlin (только для dictionary-service)
kotlin-reflect = { module = "org.jetbrains.kotlin:kotlin-reflect", version.ref = "kotlin" }
coroutines-core        = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core",    version.ref = "coroutines" }
coroutines-reactor     = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor", version.ref = "coroutines" }

# Reactor Kafka
reactor-kafka = { module = "io.projectreactor.kafka:reactor-kafka", version.ref = "reactor-kafka" }

# Springdoc
springdoc-openapi-webflux-ui = { module = "org.springdoc:springdoc-openapi-starter-webflux-ui", version.ref = "springdoc" }
springdoc-openapi-webmvc-ui = { module = "org.springdoc:springdoc-openapi-starter-webmvc-ui", version.ref = "springdoc" }

# Jsoup
jsoup = { module = "org.jsoup:jsoup", version.ref = "jsoup" }

# Keycloak
keycloak-admin-client = { module = "org.keycloak:keycloak-admin-client", version.ref = "keycloak" }

# MinIO
minio = { module = "io.minio:minio", version.ref = "minio" }

# Micrometer Tracing
micrometer-tracing-bridge-otel = { module = "io.micrometer:micrometer-tracing-bridge-otel", version.ref = "micrometer-tracing" }
opentelemetry-exporter-otlp = { module = "io.opentelemetry:opentelemetry-exporter-otlp", version.ref = "opentelemetry" }
micrometer-registry-prometheus = { module = "io.micrometer:micrometer-registry-prometheus", version.ref = "micrometer-tracing" }

# TOML
jackson-dataformat-toml = { module = "com.fasterxml.jackson.dataformat:jackson-dataformat-toml" }

# Jackson
jackson-core = { module = "com.fasterxml.jackson.core:jackson-core", version.ref = "jackson" }
jackson-databind = { module = "com.fasterxml.jackson.core:jackson-databind", version.ref = "jackson" }
jackson-annotations = { module = "com.fasterxml.jackson.core:jackson-annotations", version.ref = "jackson" }
jackson-datatype-jsr310 = { module = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310", version.ref = "jackson" }


# Testing
spring-test            = { module = "org.springframework.boot:spring-boot-starter-test" }
spring-security-test = { module = "org.springframework.security:spring-security-test" }
kotest-runner          = { module = "io.kotest:kotest-runner-junit5",                   version.ref = "kotest" }
kotest-spring          = { module = "io.kotest.extensions:kotest-extensions-spring",    version.ref = "kotest" }

[plugins]
kotlin-jvm     = { id = "org.jetbrains.kotlin.jvm",           version.ref = "kotlin" }
kotlin-spring  = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
spring-boot    = { id = "org.springframework.boot",           version.ref = "spring-boot" }
