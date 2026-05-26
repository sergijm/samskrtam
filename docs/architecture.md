# Architecture — Топология и монорепозиторий

> Связанные файлы: [README.md](README.md) · [keycloak.md](keycloak.md) · [api-gateway.md](services/api-gateway.md)
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
| content-service | Java 21 | `sm.selflearn.samskrtam.content` |
| quiz-declensions-service | Java 21 | `sm.selflearn.samskrtam.quiz.declensions` |
| quiz-conjugations-service | Java 21 | `sm.selflearn.samskrtam.quiz.conjugations` |
| quiz-vocabulary-service | Java 21 | `sm.selflearn.samskrtam.quiz.vocabulary` |
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
│   └── keycloak/
│       └── realm-export.json
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
│   ├── quiz-declensions-service/     ← Java 21 + Virtual Threads
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/quiz/declensions/
│   │
│   ├── quiz-conjugations-service/    ← Java 21 + Virtual Threads
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/quiz/conjugations/
│   │
│   ├── quiz-vocabulary-service/      ← Java 21 + Virtual Threads
│   │   └── src/main/java/
│   │       └── sm/selflearn/samskrtam/quiz/vocabulary/
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
│       ├── content-service/
│       ├── quiz-declensions-service/
│       ├── quiz-conjugations-service/
│       ├── quiz-vocabulary-service/
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
    ":services:content-service",
    ":services:quiz-declensions-service",
    ":services:quiz-conjugations-service",
    ":services:quiz-vocabulary-service",
    ":services:dictionary-service",
    ":services:statistics-service",
    ":shared:kafka-events",
    ":shared:common-dto"
)
```

### gradle/libs.versions.toml

```toml
[versions]
java                   = "21"
kotlin                 = "2.0.0"
spring-boot            = "3.3.0"
spring-cloud           = "2023.0.1"
coroutines             = "1.8.1"
kotest                 = "5.9.0"
postgresql-jdbc        = "42.7.3"
postgresql-r2dbc       = "1.0.4"
flyway                 = "10.0.0"

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
├── schema: content       ← content-service      (JPA/JDBC)
├── schema: declensions   ← quiz-declensions      (JPA/JDBC)
├── schema: conjugations  ← quiz-conjugations     (JPA/JDBC)
├── schema: vocabulary    ← quiz-vocabulary       (JPA/JDBC)
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
    ports: ["8090:8090"]
    depends_on: [keycloak, redis]
    environment:
      SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI: http://keycloak:8080/realms/samskrtam/protocol/openid-connect/certs
      SPRING_DATA_REDIS_HOST: redis

  content-service:
    build: ./services/content-service
    ports: ["8081:8081"]
    depends_on: [postgres]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam?currentSchema=content
      SPRING_THREADS_VIRTUAL_ENABLED: "true"

  quiz-declensions-service:
    build: ./services/quiz-declensions-service
    ports: ["8082:8082"]
    depends_on: [postgres, kafka, redis]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam?currentSchema=declensions
      SPRING_THREADS_VIRTUAL_ENABLED: "true"
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  quiz-conjugations-service:
    build: ./services/quiz-conjugations-service
    ports: ["8083:8083"]
    depends_on: [postgres, kafka, redis]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam?currentSchema=conjugations
      SPRING_THREADS_VIRTUAL_ENABLED: "true"
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  quiz-vocabulary-service:
    build: ./services/quiz-vocabulary-service
    ports: ["8084:8084"]
    depends_on: [postgres, kafka, redis]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam?currentSchema=vocabulary
      SPRING_THREADS_VIRTUAL_ENABLED: "true"
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

  dictionary-service:
    build: ./services/dictionary-service
    ports: ["8085:8085"]
    depends_on: [postgres]
    environment:
      SPRING_R2DBC_URL: r2dbc:postgresql://postgres:5432/samskrtam?schema=dictionary

  statistics-service:
    build: ./services/statistics-service
    ports: ["8086:8086"]
    depends_on: [postgres, kafka]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/samskrtam?currentSchema=statistics
      SPRING_THREADS_VIRTUAL_ENABLED: "true"
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

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
| content-service | 8081 |
| quiz-declensions-service | 8082 |
| quiz-conjugations-service | 8083 |
| quiz-vocabulary-service | 8084 |
| dictionary-service | 8085 |
| statistics-service | 8086 |
| PostgreSQL | 5432 |
| Kafka | 9092 |
| Redis | 6379 |

---

## 9. Kubernetes манифесты

### Структура k8s/

```
k8s/
├── namespace.yaml
├── infrastructure/
│   ├── postgres/
│   │   ├── deployment.yaml
│   │   ├── service.yaml
│   │   ├── persistentvolumeclaim.yaml
│   │   └── secret.yaml
│   ├── kafka/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   ├── redis/
│   │   ├── deployment.yaml
│   │   └── service.yaml
│   └── keycloak/
│       ├── deployment.yaml
│       ├── service.yaml
│       ├── ingress.yaml
│       └── configmap.yaml
└── services/
    └── <service-name>/
        ├── deployment.yaml
        ├── service.yaml
        ├── configmap.yaml
        └── secret.yaml
```

### Шаблон Deployment — Java 21 сервис

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
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: content-service-secret
                  key: datasource-url
            - name: SPRING_THREADS_VIRTUAL_ENABLED
              value: "true"
            - name: SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI
              valueFrom:
                configMapKeyRef:
                  name: content-service-config
                  key: keycloak-jwks-uri
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8081
            initialDelaySeconds: 20
            periodSeconds: 5
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
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

build:content-service:
  <<: *build-java-service
  variables:
    SERVICE: content-service

build:quiz-declensions:
  <<: *build-java-service
  variables:
    SERVICE: quiz-declensions-service

build:quiz-conjugations:
  <<: *build-java-service
  variables:
    SERVICE: quiz-conjugations-service

build:quiz-vocabulary:
  <<: *build-java-service
  variables:
    SERVICE: quiz-vocabulary-service

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

---

## 11. Открытые вопросы

- [ ] Ingress controller — nginx-ingress или Traefik?
- [ ] Persistent storage для PostgreSQL — local-path или NFS между нодами?
- [ ] Отдельный namespace для инфраструктуры (postgres, kafka, redis)?
- [ ] Distributed tracing — Jaeger или Zipkin?
- [ ] Автоматический деплой на main или только ручной?
