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
| Main class (Java) | `sm.selflearn.samskrtam.<сервис>.Application` | — |
| Docker image | `registry.gitlab.local/samskrtam/<сервис>-service` | — |

### Пакеты по сервисам

| Сервис | Язык | Base package | Описание                                                                                                                                            |
|---|---|---|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| api-gateway | Java 21 | `sm.selflearn.samskrtam.gateway` |                                                                                                                                                     |
| user-service | Java 21 | `sm.selflearn.samskrtam.user` |                                                                                                                                                     |
| content-service | Java 21 | `sm.selflearn.samskrtam.samskrtam.content` | Логика генерации и данные для квизов.                                                                                                               |                                                                                                             |
| quiz-service | Java 21 + WebFlux + R2DBC | `sm.selflearn.samskrtam.quiz` | m.quiz.outbox`.                                                                                                                                     | Логика прохождения квизов.
| statistics-service | Java 21 + Kafka Streams | `sm.selflearn.samskrtam.statistics` | Расчет статистики с использованием Kafka Streams.                                                                                                   |
| shared/samskrtam-dtos | Java 21 | `sm.selflearn.samskrtam.quiz` | Содержит внешние DTO для квизов, контента, статистики и **Kafka-событий** (`QuizAnsweredEvent`, `QuizSessionStatusChangedEvent`, `StatisticEvent`). |
| shared/common-dto | Java 21 | `sm.selflearn.samskrtam.common` |                                                                                                                                                     |
| dictionary-service | Java 21 | `sm.selflearn.samskrtam.dictionary` | Словари санскрита.                                                                                                                                  |
| sangraha-service | Java 21 | `sm.selflearn.samskrtam.sangraha` | Санскритские произведения (Work → Chapter → Verse), LLM-анализ стихов. См. [services/sangraha-service.md](services/sangraha-service.md), ADR-006.  |

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





