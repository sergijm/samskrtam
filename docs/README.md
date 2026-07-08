# SamskrtamApp — Project Specification v0.5

> Specification-Driven Development · Microservices · Monorepo
> Stack: Java 21 + Virtual Threads · WebFlux (gateway, quiz-service) · React/TypeScript · PostgreSQL · Keycloak · Kafka · MinIO · Grafana Stack (Tempo · Loki · Prometheus)
> Status: **UPDATED**

---

## 1. Обзор проекта

**Название:** SamskrtamApp
**Назначение:** Платформа для изучения санскрита через квизы, поиск по словарю и отслеживание прогресса.
**Архитектура:** Микросервисы (монорепо), Contract-First SDD
**Язык интерфейса:** Русский + English (i18n с первого дня)

SamskrtamApp построен как production-grade референсная реализация современной Java-микросервисной системы. Архи архитектура намеренно охватывает паттерны, актуальные при высокой нагрузке: событийная асинхронность через Kafka, stateless-сервисы, централизованная аутентификация через Gateway, Specification-Driven Development.

---

## 2. Goals & Non-Goals

### Goals (v1.0)
- Квизы по грамматике санскрита (склонения, спряжения) и лексике
- Словарь с fallback на внешнее API
- Статистика через очередь событий
- Групповой лидерборд
- Аутентификация: Keycloak (собственный аккаунт + Google + Mail.ru)
- Горизонтальное масштабирование: stateless сервисы, Kafka для async
- Bilingual UI (ru / en)
- [Упражнения по сандхи (Eamenau)](./services/content-service/eamenau.md) — разбор правил сандхи на материале учебника Eméneau

### Non-Goals (v1.0)
- Mobile native app
- Аудио произношения

---

## 3. Язык и стек по сервисам

| Сервис | Язык    | Async модель | Причина |
|---|---------|---|---|
| api-gateway | Java 21 | WebFlux (Reactor) | Gateway требует реактивный стек |
| feature-flag-service | Java 21 | Virtual Threads | Простой CRUD + Redis, нет смысла в реактивщине |
| user-service | Java 21 | Virtual Threads | Профили, регистрация, аватарки, блокировка |
| content-service | Java 21 | Virtual Threads | CRUD настроек квизов и вопросов. Поддерживает иерархические категории для VOCABULARY квизов. Содержит домен Eamenau (упражнения по сандхи). |
| quiz-service | Java 21 | WebFlux (Reactor) + R2DBC | Единый сервис прохождения всех квизов. Использует Outbox Pattern для публикации событий в Kafka. |
| **dictionary-service** | Java 21 | Virtual Threads|
| statistics-service | Java 21 | Kafka Streams | Расчет статистики с использованием Kafka Streams. |
| shared/samskrtam-dtos | Java 21 | — | Объединенный модуль для всех DTO и событий квизов, контента и статистики |
| shared/common-dto | Java 21 | — | Совместимость со всеми сервисами |

> **Ключевое решение:** Java 21 Virtual Threads позволяют писать блокирующий
> код (обычный JDBC, RestTemplate) который JVM автоматически делает
> неблокирующим. Не нужен R2DBC и WebFlux там где они избыточны.

---

## 4. Bounded Contexts

```mermaid
graph TD
  subgraph Identity ["🔐 Identity (Keycloak)"]
    KC[Keycloak Server]
  end

  subgraph Gateway ["🚪 API Gateway"]
    GW[Spring Cloud Gateway\nJava 21 + WebFlux]
  end

  subgraph IdentityUsers ["👤 Users & Identity — Java 21 + Virtual Threads"]
    AS[user-service\nKeycloak proxy]
  end

  subgraph Content ["📝 Content — Java 21 + Virtual Threads"]
    CS[content-service\nнастройки и содержание квизов]
  end

  subgraph Quiz ["📚 Quiz Service — Java 21 + WebFlux (Reactor)"]
    QS[quiz-service\nпрохождение квизов]
  end

  subgraph Dictionary ["📖 Dictionary — Java 21 + Virtual Threads"]
    DS[dictionary-service]
  end

  subgraph Stats ["📊 Statistics — Java 21 + Kafka Streams"]
    ST[statistics-service]
  end

  Browser --> GW
  GW -->|валидирует JWT| KC
  GW --> US
  US -->|Admin REST API| KC
  US -->|presigned URL| MinIO[(MinIO)]
  GW --> QS
  GW --> CS
  GW --> DS
  GW --> ST
  QS -->|читает квизы и вопросы| CS
  QS -->|публикует QuizAnsweredEvent, QuizSessionStatusChangedEvent| Kafka
  Kafka --> ST
```

---

## 5. Specification Files

### Architecture
| Файл | Содержание |
|---|---|
| [README.md](./README.md) | Этот файл — обзор проекта |
| [architecture.md](./architecture.md) | Топология, технологии, монорепо, CI/CD, Kubernetes |
| [conventions.md](./conventions.md) | Соглашения: конфигурация, логирование, трассировка, тесты, git |
| [infra/keycloak.md](./infra/keycloak.md) | Аутентификация, identity providers, JWT claims |
| [services/api-gateway.md](./services/api-gateway.md) | Маршруты, фильтры, rate limiting, OAuth2 flow |
| [services/feature-flag-service.md](./services/feature-flag-service.md) | Feature flags — управление поведением без деплоя |


### Per-Service Specifications
| Файл | Содержание |
|---|---|
| [sangraha-service.md](./services/sangraha-service.md) | Java 21 + VT | Санскритские произведения, LLM-анализ стихов |
| [openapi/](./openapi/) | OpenAPI спецификации для всех сервисов |
| [services/api-gateway.md](./services/api-gateway.md) | Java 21 + WebFlux | Spring Cloud Gateway |
| [services/feature-flag-service.md](./services/feature-flag-service.md) | Java 21 + VT | Feature Flag Service |
| [services/user-service.md](./services/user-service.md) | Java 21 + VT | Логин, регистрация, OAuth, управление паролем |
| [services/content-service.md](./services/content-service.md) | Java 21 + VT | Настройки и содержание всех квизов |
| [services/content-service/eamenau.md](./services/content-service/eamenau.md) | — (домен content-service) | Упражнения по сандхи, фонемная система |
| [services/quiz-service.md](./services/quiz-service.md) | Java 21 + VT | Прохождение квизов пользователем |
| [services/dictionary-service.md](./services/dictionary-service.md) | Java 21 + Virtual Threads | Словарь + внешнее API |
| [services/statistics-service.md](./services/statistics-service.md) | Java 21 + VT | Статистика |
| [services/leaderboard.md](./services/leaderboard.md) | — | Алгоритмы лидерборда (XP, Elo, Skill, Composite) |

### Frontend
| Файл | Содержание |
|---|---|
| [frontend/frontend-overview.md](./frontend/frontend-overview.md) | Стек, структура, роуты, тема, i18n |
| [frontend/frontend-conventions.md](./frontend/frontend-conventions.md) | i18n, env, coding conventions, acceptance criteria |
| [frontend/frontend-state.md](./frontend/frontend-state.md) | TS-типы, API-хуки, Zustand stores |
| [frontend/user-frontend.md](./frontend/user-frontend.md) | Пользователи, группы, настройки |
| [frontend/feature-flags-frontend.md](./frontend/feature-flags-frontend.md) | UI управления feature flags (только ADMIN) |
| [frontend/pages/lesson-pages-spec.md](./frontend/pages/lesson-pages-spec.md) | Страницы уроков (VocabularyLessonPage, GrammarLessonPage) |

---

## 6. Milestones

| Milestone | Сервисы | Цель |
|---|---|---|
| **M1 — Foundation** | Gateway + Keycloak + user-service + content-service | Auth, CRUD контента, монорепо скелет |
| **M2 — First Quiz** | quiz-service (declensions) | Первый рабочий квиз, Contract-First |
| **M3 — Statistics** | statistics-service + Kafka | События, async обработка |
| **M4 — Dictionary** | dictionary-service (Java 21 + Virtual Threads) | Cache-aside, внешнее API |
| **M5 — More Quizzes** | quiz-service (conjugations + vocabulary) | Масштабирование паттерна |
| **M6 — Observability** | все сервисы | Distributed tracing, structured logging, metrics |
| **M7 — Polish** | все сервисы | i18n, UX, CI/CD финализация, load testing |

---

## 7. Open Questions

- [ ] Ingress controller в кластере — nginx-ingress или Traefik?
- [ ] Persistent storage для PostgreSQL в k8s — local-path или NFS?
- [ ] Внешнее API для словаря: Sanskrit Heritage или Monier-Williams приоритет?
- [ ] Mail.ru OAuth: актуальны ли endpoints в 2025?
- [ ] Автоматический деплой на main или только ручной (when: manual)?
- [ ] **Eamenau:** нужен ли API для фонемной системы (`GET /api/v1/eamenau/phonemes`)? — см. [services/content-service/eamenau.md](./services/content-service/eamenau.md)
- [ ] **Eamenau:** унифицировать написание `Eamenau` / `Emeneau` во всём проекте (в именах файлов фронтенда — `Emeneau`, в Java-коде — `Eamenau`)
- [x] **Семантика Quiz/Lesson/Activity** — закрыто ADR-002, см. [conventions.md §14](./conventions.md#14-архитектурные-решения-adr)
- [ ] **Eamenau:** `Answer` (варианты ответа к задаче) — реализован в модели, не используется в API. Планируется ли режим с выбором варианта?
- [x] **sangraha-service порт 8089** — фиксирован, согласован с Агентом 5 DevOps. Пометка «предварительно» снята.
- [x] **sangraha-service §6.3: Quiz(VOCABULARY) на уровне главы?** — решено: Quiz заводится только на уровне произведения (workSlug). Главы не получают отдельного Quiz.
- [x] **sangraha-service shared DTO для Kafka-события** — решено: заведён `SangrahaVocabularyEvent` в `shared/samskrtam-dtos` (пакет `sangraha`).