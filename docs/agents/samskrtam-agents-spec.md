# SamskrtamApp — Мультиагентная спецификация

> Проект: SamskrtamApp · Платформа изучения санскрита  
> Паттерн: Specification-Driven Development · Микросервисы · Монорепо  
> Стек: Java 21 + Virtual Threads · WebFlux · React/TypeScript · PostgreSQL · Keycloak · Kafka

---

## Общая концепция

Каждый агент — автономная единица с чётко ограниченной областью ответственности, собственными входными артефактами (документы из `docs/`) и ожидаемым выходом. Агенты работают последовательно или параллельно в зависимости от зависимостей. Оркестратор (агент-планировщик) распределяет задачи и разрешает конфликты между агентами.

---

## Агент 0 — Оркестратор (Planner Agent)

**Роль:** Управление жизненным циклом задачи, декомпозиция, координация агентов.

**Входные документы:** docs/README.md, docs/architecture.md, docs/conventions.md

**Ответственность:** декомпозиция задач, назначение агентам, проверка контрактов между артефактами, формирование отчёта. Milestone-ориентированность (M1–M7), не пишет код, эскалирует конфликты человеку, фиксирует блокеры по open questions.

---

## Агент 1 — API Gateway & Infrastructure Agent

**Роль:** Инфраструктурный слой (Gateway, Keycloak, Feature Flags, k8s).

**Входные документы:** docs/services/api-gateway.md, docs/infra/keycloak.md, docs/services/feature-flag-service.md, docs/conventions.md

**Ответственность:** Spring Cloud Gateway (маршруты, фильтры IdentityHeader/RateLimit/RequestId), OAuth2/OIDC (Authorization Code, ROPC, refresh, logout), JWT-валидация через Keycloak JWKS, rate limiting (Redis + feature-flag), Feature Flag Service (CRUD, Redis), Keycloak realm/clients/providers, k8s NetworkPolicy.

**Стек:** Java 21 + WebFlux (Gateway), Java 21 + Virtual Threads (feature-flag-service).

**Выход:** Gateway с маршрутами, Feature Flag API, realm-export.json, Dockerfile.

**Ограничения:** Gateway без бизнес-логики, Virtual Threads неприменимы (WebFlux), client_secret только через env.

---

## Агент 2 — Backend: Domain Services Agent

**Роль:** Разработка бизнес-сервисов предметной области.

**Входные документы:**
- `docs/services/user-service.md`
- `docs/services/content-service.md`
- `docs/services/quiz-service.md`
- `docs/services/dictionary-service.md`
- `docs/services/statistics-service.md`
- `docs/services/leaderboard.md`
- `docs/services/sangraha-service.md`
- `docs/quizzes/quiz-declension.md`
- `docs/conventions.md`

**Ответственность:** user-service (8087): регистрация, пароли, SMTP, профили, MinIO, Keycloak Outbox, группы. content-service (8081): CRUD уроков, категории слов, session-data для quiz-service, ADMIN/STUDENT доступ. quiz-service (8082, WebFlux+R2DBC): сессии start→answer→complete, resume, WordAnswerHistory, Outbox→Kafka (QuizAnsweredEvent, QuizSessionStatusChangedEvent), WebClient к content-service. dictionary-service (8085): поиск по slp1Spelling, cache-aside Redis→внешнее API, fallback. sangraha-service (порт из env): иерархия Work→Chapter→Verse, LLM-анализ (OpenAI tool calling), синхронный REST-вызов content-service для синхронизации лексики (см. architecture.md §3.5), write только ADMIN. statistics-service (8086, Kafka Streams): потребление quiz-answered-events/quiz-session-status-changed-events, агрегация, REST API, лидерборд (см. leaderboard.md).

**Выход:** сервисы с Flyway-миграциями, OpenAPI, Kafka-топики, shared DTO.

**Ограничения:** quiz-service только R2DBC, конфигурация через env, пакеты sm.selflearn.samskrtam.<сервис>.

---

## Агент 3 — Frontend Agent

**Роль:** React/TypeScript фронтенд.

**Входные документы:** docs/frontend/frontend-overview.md, user-frontend.md, feature-flags-frontend.md, docs/frontend/pages/lesson-pages-spec.md, docs/openapi/lesson-aggregation-openapi.yaml, sangraha-service.md §7, docs/conventions.md (i18n, git).

**Ответственность:** роутинг (публичные + защищённые), auth flow (Keycloak Authorization Code через Gateway, authStore, ProtectedRoute), компоненты по доменам, i18n ru/en, темы lara-light-blue/lara-dark-blue, React Query + Zustand, AdminPage.

**Стек:** React 18, TypeScript 5, Vite 5, PrimeReact 10.x, PrimeFlex 3.x, Axios, React Query 5, Zustand 4, i18next.

**Выход:** структура frontend/src, API-клиенты, хуки (useAuth, useQuiz, useDictionary, useStatistics), stores (authStore, themeStore, localeStore), рабочие страницы.

**Ограничения:** фронтенд без client_secret, redirectPath при 401, только PrimeReact.

---

## Агент 4 — Testing Agent

**Роль:** Тесты и покрытие (JUnit 5, Mockito, Testcontainers, ArchUnit, JaCoCo, Checkstyle, SpotBugs).

**Входные документы:** docs/conventions.md (тесты, JaCoCo, Checkstyle, SpotBugs), спецификации сервисов.

**Ответственность:** unit-тесты (без Spring, именование methodName_stateUnderTest_expectedBehavior), интеграционные тесты (Testcontainers: PostgreSQL, Redis, Kafka) с обязательными сценариями для каждого сервиса (quiz: сессии/ответы/outbox; content: CRUD/403; user: логин/дубль; statistics: Kafka агрегация; dictionary: cache hit/miss; gateway: 401/403/429). Архитектурные тесты ArchUnit. JaCoCo ≥ 80% сервисного слоя. Checkstyle + SpotBugs.

**Выход:** тест-классы, build.gradle.kts с порогами, конфиги checkstyle.xml, spotbugs/exclude.xml, отчёт.

---

## Агент 5 — DevOps & Observability Agent

**Роль:** IaC, CI/CD, мониторинг, трассировка.

**Входные документы:** docs/architecture.md, docs/conventions.md (Docker, Graceful Shutdown, Connection Pool).

**Ответственность:** Docker Compose (все сервисы + PostgreSQL, Redis, Kafka, Keycloak, MinIO), Dockerfile (multi-stage, graceful shutdown, connection pool через env), Kubernetes (namespace, Deployments, Services, ConfigMaps, Secrets, NetworkPolicy: только Gateway к сервисам, топология control-plane + 3 worker), GitLab CI (build→test→coverage→lint→docker→push→deploy, Container Registry), Observability (Tempo OTLP, Loki JSON logging, Prometheus Micrometer, Grafana дашборды, management port 8099).

**Выход:** docker-compose.yml/.override.yml, .env.example, Dockerfile для каждого сервиса, k8s/ манифесты, .gitlab-ci.yml, конфиги Tempo/Loki/Prometheus/Grafana.

**Ограничения:** VM-1 = GitLab (не worker), Portainer на VM-5, open question: nginx-ingress vs Traefik.

---

## Агент 6 — API Contract & Documentation Agent

**Роль:** Контракты, OpenAPI, документация.

**Входные документы:** docs/openapi/lesson-aggregation-openapi.yaml, docs/services/*.md, docs/README.md.

**Ответственность:** Contract-First (изменение API начинается с docs, а не с кода), OpenAPI YAML/JSON для каждого сервиса, shared DTO актуальность, консистентность Kafka-событий, именование топиков (<domain>-<event>-events), актуализация docs/ при изменении API, проверка open questions и ADR, контроль Gateway маршрутов.

**Выход:** OpenAPI YAML, обновлённые docs/services/*.md, отчёт о расхождениях, актуальный README.md.

**Ограничения:** не пишет бизнес-код, изменения shared DTO согласует с Агентами 2 и 3.

**Поддержание компактности документации:**
- Не использовать блоки кода (Java-entities, SQL-миграции, JSON-схемы, TypeScript-интерфейсы, YAML-конфиги). Вместо этого использовать текстовые описания алгортмов. Сущности описывать как перечисления полей и их типов - каждое поле в новой строке.
- Все markdown-файлы в `docs/` не должны превышать 350 строк. Если файл вырос сверх лимита — разбить на отдельные файлы
- Проверка на превышение лимита выполняется после каждого изменения документации или при планировании нового релиза.

---

## Зависимости между агентами

```
Оркестратор (0)
    ├──► API Contract Agent (6)          ← первый: контракты
    │         │
    │         ▼
    ├──► Gateway & Infra Agent (1)        ← параллельно с Domain Agent
    │         │
    │         ▼
    ├──► Domain Services Agent (2)        ← после контрактов
    │         │
    │         ├──► Frontend Agent (3)     ← после domain API
    │         │
    │         └──► Testing Agent (4)      ← параллельно с frontend
    │
    └──► DevOps Agent (5)                 ← параллельно
```

**Критические зависимости:** Frontend (3) ждёт от Contract (6) OpenAPI и от Domain (2) endpoints; Testing (4) ждёт Domain (2); DevOps (5) ждёт всех; Gateway (1) ждёт Contract (6).

---

## Соглашения для агентов

**Именование веток:** feat/fix/test/chore(<scope>): description. Scope: quiz-service, gateway, content-service, frontend, shared.

**Конфигурация:** без дефолтов в application.yml — только ${ENV_VAR}, секреты только через env, .env.example актуален.

**Definition of Done:** 1) реализация соответствует docs/; 2) тесты + покрытие ≥ 80% сервисного слоя; 3) Checkstyle и SpotBugs чисты; 4) OpenAPI обновлён; 5) Dockerfile работает; 6) PR прошёл CI + code review.
