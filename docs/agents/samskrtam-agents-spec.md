# SamskrtamApp — Мультиагентная спецификация

> Проект: SamskrtamApp · Платформа изучения санскрита  
> Паттерн: Specification-Driven Development · Микросервисы · Монорепо  
> Стек: Java 21 + Virtual Threads · WebFlux · React/TypeScript · PostgreSQL · Keycloak · Kafka

---

## Общая концепция

Каждый агент — автономная единица с чётко ограниченной областью ответственности, собственными входными артефактами (документы из `docs/`) и ожидаемым выходом. Агенты работают последовательно или параллельно в зависимости от зависимостей. Оркестратор (агент-планировщик) распределяет задачи и разрешает конфликты между агентами.

---

## Агент 0 — Оркестратор (Planner Agent)

**Роль:** Управление жизненным циклом задачи, декомпозиция, координация остальных агентов.

**Входные документы:**
- `docs/README.md` — обзор, milestones, open questions
- `docs/architecture.md` — топология, монорепо, Gradle
- `docs/conventions.md` — соглашения, git, CI

**Ответственность:**
- Получает задачу в свободной форме (например: «реализовать VOCABULARY квиз»)
- Декомпозирует на подзадачи по сервисам и слоям
- Определяет порядок выполнения с учётом зависимостей (например, сначала content-service DTO, потом quiz-service, потом фронтенд)
- Назначает подзадачи конкретным агентам
- Проверяет, что выходные артефакты одного агента соответствуют входным ожиданиям другого
- Формирует итоговый отчёт о выполнении

**Принципы работы:**
- Milestone-ориентированность: каждое действие привязано к M1–M7 из README
- Не пишет код сам — только планирует и координирует
- При конфликте спецификаций эскалирует к человеку с конкретным вопросом
- Следит за open questions — если задача затрагивает нерешённый вопрос, фиксирует блокер

---

## Агент 1 — Backend: API Gateway & Infrastructure Agent

**Роль:** Разработка и поддержка инфраструктурного слоя.

**Входные документы:**
- `docs/services/api-gateway.md`
- `docs/infra/keycloak.md`
- `docs/services/feature-flag-service.md`
- `docs/conventions.md` (разделы: конфигурация, трассировка, Docker)

**Ответственность:**
- Spring Cloud Gateway: маршруты (`GatewayRoutesConfig.java`), фильтры (`IdentityHeaderFilter`, `RateLimitFilter`, `RequestIdFilter`)
- OAuth2/OIDC flow: Authorization Code (Google, Mail.ru), ROPC login, token refresh, logout
- JWT-валидация через Keycloak JWKS, добавление X-User-* заголовков в downstream
- Rate limiting через Redis (токен-бакет), управляемый feature-flag `RATE_LIMITING_ENABLED`
- Feature Flag Service: CRUD флагов, хранение в Redis, API для сервисов
- Keycloak: настройка realm, клиентов, identity providers, JWT claims (`roles`, `sub`)
- k8s NetworkPolicy — изоляция трафика, только Gateway имеет доступ к сервисам

**Стек агента:** Java 21 + WebFlux (Gateway), Java 21 + Virtual Threads (feature-flag-service)

**Выходные артефакты:**
- Рабочий Gateway с таблицей маршрутов из `api-gateway.md §3`
- Feature Flag API (CRUD + Redis pub/sub)
- `realm-export.json` для Keycloak с настроенными провайдерами
- Dockerfile для gateway и feature-flag-service

**Ограничения:**
- Gateway не содержит бизнес-логики — только инфраструктурные фильтры
- Virtual Threads в Gateway неприменимы (WebFlux/Reactor)
- `client_secret` только через env, никогда в коде

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

**Ответственность:**

### user-service (Java 21 + Virtual Threads, порт 8087)
- Регистрация, смена пароля, восстановление пароля (SMTP)
- Профили пользователей, аватарки через MinIO (presigned URL)
- Синхронизация с Keycloak через Outbox Pattern
- Управление группами и блокировками

### content-service (Java 21 + Virtual Threads, порт 8081)
- Управление уроками (Lesson): CRUD, идентификация по slug и lessonId
- Содержит N слов на урок; слова могут быть иерархически категоризированы (`VocabularyCategory`, `VocabularyWordCategory`)
- Возвращает `LessonSummaryDto` для фронтенда
- Генерация session-data для quiz-service (набор слов для квиза)
- Разграничение доступа: ADMIN (write), STUDENT (read public)

### quiz-service (Java 21 + WebFlux + R2DBC, порт 8082)
- Управление квиз-сессиями (quiz_session), каждая из которых — попытка пользователя пройти часть урока (например, 20 слов из 100)
- Сессия привязана к уроку (lessonId); у одного урока может быть много квизов одного пользователя
- Жизненный цикл сессии: start → answer → complete; поддержка resume незавершённых сессий
- `WordAnswerHistory` агрегирует ответы на каждое слово в рамках квиза (через JOIN по quiz_session с фильтром lessonId + userId)
- Outbox Pattern: `QuizAnsweredEvent`, `QuizSessionStatusChangedEvent` → Kafka
- Реактивный HTTP к content-service через WebClient

### dictionary-service (Java 21 + Virtual Threads, порт 8085)
- Поиск словарных статей по `slp1Spelling`
- Cache-aside: Redis → внешнее API (Sanskrit Heritage / Monier-Williams)
- Fallback при недоступности внешнего API

### sangraha-service (Java 21 + Virtual Threads, порт из env `SANGRAHA_SERVICE_PORT`)
- Санскритские произведения: иерархия Work → Chapter → Verse
- LLM-анализ стиха через OpenAI-совместимый API с tool calling (транслитерация, перевод ru/en, сандхи, пословная грамматика) — см. `sangraha-service.md §5`
- Transactional Outbox → Kafka `sangraha-vocabulary-events` на каждый проанализированный стих (первый продюсер этого топика; content-service — первый в проекте `@KafkaListener`-консьюмер, см. `content-service.md §11`)
- Никаких синхронных HTTP-вызовов в content-service/dictionary-service — только Kafka (ADR-006)
- Разграничение доступа: весь write — ADMIN (роль «редактор/переводчик» отложена)

### statistics-service (Java 21 + Kafka Streams, порт 8086)
- Потребление `quiz-answered-events` и `quiz-session-status-changed-events`
- Агрегация статистики через Kafka Streams (`KafkaStreamsConfig`)
- REST API для фронтенда: личная статистика, тепловая карта ошибок
- Лидерборд: алгоритмы XP, Elo, Skill, Composite (см. `leaderboard.md`)

**Выходные артефакты:**
- Реализованные сервисы с Flyway-миграциями
- OpenAPI-спецификации (springdoc, `SPRINGDOC_ENABLED=true` в dev)
- Kafka-топики: `quiz-answered-events`, `quiz-session-status-changed-events`, `user-quiz-statistics-output`
- Shared-модули: `shared/samskrtam-dtos` (включает `LessonSummaryDto`, `QuizSessionDto`, `WordAnswerHistoryDto`), `shared/common-dto`

**Ограничения:**
- quiz-service: только R2DBC (JPA несовместима с WebFlux)
- Все значения конфигурации — из env, без дефолтов в `application.yml`
- Именование пакетов: `sm.selflearn.samskrtam.<сервис>`

---

## Агент 3 — Frontend Agent

**Роль:** Разработка React/TypeScript фронтенда.

**Входные документы:**
- `docs/frontend/frontend.md`
- `docs/frontend/user-frontend.md`
- `docs/frontend/feature-flags-frontend.md`
- `docs/frontend/lesson-pages-spec.md`
- `docs/frontend/lesson-openapi.yaml`
- `docs/services/sangraha-service.md` (§7 — фронтенд-эскиз: WorksPage, WorkPage/TreeGrid, VersePage)
- `docs/conventions.md` (i18n, git)

**Ответственность:**
- Роутинг: публичные страницы (HomePage, Login, Register) + защищённые (Dashboard, QuizPage, Dictionary, Statistics, Leaderboard, AdminPage)
- Auth flow: Keycloak Authorization Code через Gateway, `authStore` (Zustand), `ProtectedRoute`
- Компоненты по доменам: quiz, statistics, dictionary, lesson, user, group, common
- i18n: ru/en через i18next, с первого дня
- Темы: `lara-light-blue` / `lara-dark-blue` (PrimeReact), динамическая смена без перезагрузки
- Управление состоянием: React Query (server state) + Zustand (auth, locale, theme)
- AdminPage: управление квизами, вопросами, feature flags, пользователями

**Стек:** React 18 · TypeScript 5 · Vite 5 · PrimeReact 10.x · PrimeFlex 3.x · Axios · React Query 5 · Zustand 4 · i18next

**Выходные артефакты:**
- Полная структура `frontend/src/` согласно `frontend.md §2`
- API-клиенты по доменам (`api/quiz.ts`, `api/dictionary.ts`, etc.)
- Хуки: `useAuth`, `useQuiz`, `useDictionary`, `useStatistics`
- Stores: `authStore`, `themeStore`, `localeStore`
- Рабочие страницы: QuizPage с прогрессом, VocabularyLessonPage, GrammarLessonPage, StatisticsPage с тепловой картой

**Ограничения:**
- Фронтенд никогда не получает `client_secret` — весь OAuth2 через Gateway
- `axios.ts`: сохранение `redirectPath` при 401 для корректного возврата после логина
- PrimeReact — не другие UI-библиотеки

---

## Агент 4 — Testing Agent

**Роль:** Написание тестов, настройка покрытия, интеграционное тестирование.

**Входные документы:**
- `docs/conventions.md` (разделы 7–8: тесты, JaCoCo, Checkstyle, SpotBugs)
- Спецификации всех сервисов (для обязательных тест-кейсов)

**Ответственность:**

### Unit-тесты (JUnit 5 + Mockito)
- Без Spring-контекста, быстрые
- Структура: `src/test/java/.../unit/service/`, `unit/util/`
- Именование: `methodName_stateUnderTest_expectedBehavior`

### Интеграционные тесты
- `integration/api/` — MockMvc / WebTestClient, HTTP-контракты
- `integration/repository/` — Testcontainers (PostgreSQL, Redis, Kafka)
- Обязательные сценарии из конвенций:
  - **quiz-service**: старт сессии, верный/неверный ответ, fallback Redis→Postgres, дубликат ответа, сохранение Outbox-события
  - **content-service**: CRUD, 403 для STUDENT на write, генерация VOCABULARY по иерархии категорий
  - **user-service**: логин (успех / неверный пароль), регистрация с дублем email
  - **statistics-service**: агрегация из Kafka-событий через Kafka Streams
  - **dictionary-service**: cache hit, cache miss + внешний запрос, внешний API недоступен
  - **api-gateway**: нет JWT → 401, STUDENT на /content → 403, rate limit → 429

### Архитектурные тесты (ArchUnit)
- Структура пакетов, запрет нежелательных зависимостей между слоями

### Покрытие (JaCoCo)
- Минимум 80% для классов сервисного слоя
- Порог прописан в `build.gradle.kts`, сборка падает при нарушении

### Статический анализ
- Checkstyle (конфиг: `config/checkstyle/checkstyle.xml`)
- SpotBugs (effort=MAX, confidence=MEDIUM)
- Порядок в CI: `test → jacocoTestReport → jacocoTestCoverageVerification → checkstyleMain → spotbugsMain`

**Выходные артефакты:**
- Тест-классы для каждого сервиса по структуре из `conventions.md §8`
- `build.gradle.kts` с JaCoCo-порогами
- `config/checkstyle/checkstyle.xml`, `config/spotbugs/exclude.xml`
- Отчёт о покрытии

---

## Агент 5 — DevOps & Observability Agent

**Роль:** Инфраструктура как код, CI/CD, мониторинг, трассировка.

**Входные документы:**
- `docs/architecture.md` (физическая инфраструктура, монорепо, k8s)
- `docs/conventions.md` (Docker, Graceful Shutdown, Connection Pool)

**Ответственность:**

### Docker Compose (локальная разработка)
- `docker-compose.yml`: все сервисы + инфраструктура (PostgreSQL, Redis, Kafka, Keycloak, MinIO)
- `docker-compose.override.yml`: dev-специфичные настройки (порты, volumes)
- Все env-переменные из `.env`

### Dockerfile (все Java-сервисы)
- Multi-stage build: builder → runtime
- Graceful shutdown: `server.shutdown=graceful`, таймаут из `${GRACEFUL_SHUTDOWN_TIMEOUT}`
- Connection pool: `${DB_POOL_MAX_SIZE}`, `${DB_POOL_MIN_IDLE}`, `${DB_POOL_CONNECTION_TIMEOUT_MS}`

### Kubernetes (k8s/)
- Namespace, Deployments, Services, ConfigMaps, Secrets
- Infrastructure: `k8s/infrastructure/` — PostgreSQL, Kafka, Redis, Keycloak
- Services: `k8s/services/` — по одному каталогу на сервис
- NetworkPolicy: только Gateway имеет доступ к бизнес-сервисам
- Топология: control-plane (VM-2) + worker-1..3 (VM-3..5)

### CI/CD (GitLab CI)
- `.gitlab-ci.yml`: build → test → coverage → lint → docker build → push → deploy
- GitLab Container Registry: `registry.gitlab.local/samskrtam/<сервис>-service`
- GitLab Agent для деплоя в k8s

### Observability (Grafana Stack)
- **Tempo**: distributed tracing через OTLP (`${OTEL_EXPORTER_OTLP_ENDPOINT}`)
- **Loki**: structured logging (logstash-logback-encoder → JSON)
- **Prometheus**: метрики через Micrometer (`micrometer-registry-prometheus`)
- **Grafana**: datasources и дашборды (JSON в репозитории)
- Management port: `${MANAGEMENT_PORT}=8099` для actuator/health, actuator/prometheus

**Выходные артефакты:**
- `docker-compose.yml`, `docker-compose.override.yml`
- `.env.example` со всеми ключами и комментариями
- Dockerfile для каждого сервиса
- `k8s/` манифесты (namespace, deployments, services, networkpolicies)
- `.gitlab-ci.yml`
- `infrastructure/tempo/tempo.yaml`, `loki/loki.yaml`, `prometheus/prometheus.yaml`, `grafana/`

**Ограничения:**
- VM-1 — GitLab (не k8s worker)
- Portainer на VM-5 (worker-3) для управления
- Open question: nginx-ingress vs Traefik — дождаться решения оркестратора

---

## Агент 6 — API Contract & Documentation Agent

**Роль:** Контроль контрактов между сервисами, поддержание OpenAPI-спецификаций и документации.

**Входные документы:**
- `docs/frontend/lesson-openapi.yaml`
- Все `docs/services/*.md`
- `docs/README.md`

**Ответственность:**

### Contract-First (SDD)
- Любое изменение API начинается с обновления спецификации (docs), не с кода
- Проверяет соответствие реализации спецификации (contract testing)
- Генерирует или валидирует OpenAPI YAML/JSON для каждого сервиса

### Shared DTOs
- Поддерживает актуальность `shared/samskrtam-dtos` и `shared/common-dto`
- Следит за консистентностью событий Kafka: `QuizAnsweredEvent`, `QuizSessionStatusChangedEvent`, `StatisticEvent`
- Именование топиков: `<domain>-<event>-events` (конвенция из `conventions.md §15`)

### Документация
- Актуализирует `docs/` при изменении API
- Проверяет `docs/README.md` open questions — фиксирует статус
- Следит за ADR (Architectural Decision Records) в `conventions.md §14`

### API Gateway маршруты
- Проверяет таблицу маршрутов `api-gateway.md §3` на соответствие реальным endpoints
- При добавлении нового endpoint — обновляет маршруты и документацию атомарно

**Выходные артефакты:**
- OpenAPI YAML для каждого сервиса
- Обновлённые `docs/services/*.md` при изменении контрактов
- Отчёт о расхождениях спецификации и реализации
- Актуальный `docs/README.md` с закрытыми open questions

**Ограничения:**
- Не пишет бизнес-код — только контракты и документацию
- Любое изменение shared DTO — согласование с Backend Domain Agent (Агент 2) и Frontend Agent (Агент 3)

---

## Зависимости между агентами

```
Оркестратор (0)
    ├──► API Contract Agent (6)          ← первый: определяет контракты
    │         │
    │         ▼
    ├──► Gateway & Infra Agent (1)        ← параллельно с Domain Agent
    │         │
    │         ▼
    ├──► Domain Services Agent (2)        ← после контрактов
    │         │
    │         ├──► Frontend Agent (3)     ← после domain API готово
    │         │
    │         └──► Testing Agent (4)      ← параллельно с frontend
    │
    └──► DevOps Agent (5)                 ← параллельно с остальными
```

### Критические зависимости

| Агент | Ждёт от | Что именно |
|---|---|---|
| Frontend (3) | Contract (6) | OpenAPI-спецификации для генерации типов |
| Frontend (3) | Domain (2) | Рабочие endpoints (или моки) |
| Testing (4) | Domain (2) | Реализация сервисов для интеграционных тестов |
| DevOps (5) | Все | Docker-образы для compose и k8s |
| Gateway (1) | Contract (6) | Финальная таблица маршрутов |

---

## Соглашения для агентов

### Именование веток (Conventional Commits)
```
feat(<scope>): <description>
fix(<scope>): <description>
test(<scope>): add integration tests
chore(deps): bump <library>
```
Scope = имя сервиса: `quiz-service`, `gateway`, `content-service`, `frontend`, `shared`.

### Конфигурация
- Никаких дефолтных значений в `application.yml` — только `${ENV_VAR}`
- Секреты только через env, не в коде и не в git
- `.env.example` обновляется при каждом новом env-ключе

### Definition of Done
1. Реализация соответствует спецификации в `docs/`
2. Тесты написаны (unit + integration), покрытие ≥ 80% сервисного слоя
3. Checkstyle и SpotBugs не падают
4. OpenAPI-спецификация обновлена (если изменился контракт)
5. Dockerfile работает, образ собирается
6. PR прошёл CI и получил один code review
