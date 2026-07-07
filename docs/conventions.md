# Conventions

> Соглашения, обязательные для всех сервисов проекта.
> Связанные файлы: [architecture.md](./architecture.md) · [README.md](./README.md) · [adr.md](./adr.md)
> Status: **UPDATED**

---

## 1. Конфигурация

- Переменные окружения в .env (без дефолтов в application.yml)
- Spring Profiles: default/staging/production
- [Подробнее →](conventions/conventions-configuration.md)

## 2. Observability (логирование, трассировка, метрики)

- Логирование: @Slf4j, JSON (logstash-logback-encoder), no sensitive data
- Трассировка: Micrometer → OpenTelemetry → Tempo
- Actuator: management порт, health groups (liveness/readiness), кастомные метрики
- [Подробнее →](conventions/conventions-observability.md)

## 3. API Design, Swagger, обработка ошибок

- Swagger агрегация через Gateway
- Пагинация (page/size/sort), версионирование (v1/v2)
- Rate Limiting (auth/**, api/**), CORS
- Checkstyle, SpotBugs, CI порядок
- [Подробнее →](conventions/conventions-api.md)

## 4. Тестирование

- Структура: unit/integration/arch
- Именование: methodName_stateUnderTest_expectedBehavior
- JaCoCo ≥ 80% сервисного слоя
- Обязательные тест-кейсы по сервисам
- [Подробнее →](conventions/conventions-testing.md)

## 5. Docker

- Graceful shutdown: server.shutdown = graceful, таймаут из GRACEFUL_SHUTDOWN_TIMEOUT

## 6. Git Conventions

- Conventional Commits: <type>(<scope>): <description>
- Типы: feat/fix/docs/refactor/test/chore/perf
- Ветки: main → feat/*/fix/*/chore/*, PR требует CI + code review

## 7. Kafka

- Топики: <domain>-<event>-events (kebab-case)
- Публикация: только через Transactional Outbox Pattern
- Синхронные вызовы Domain↔Domain по HTTP не приветствуются (см. ADR-006)

## 8. Мапперы Entity/DTO

- MapStruct @Mapper(componentModel = "spring"), пакет mapper/
- Простой 1:1 маппинг — выносится в mapper/
- DTO из нескольких источников — .builder() в *Service допустим
- abstract class с @Autowired в маппере — запрещён
- [Полное описание с примерами в оригинальном файле](conventions.md)

## 9. Открытые вопросы

- Secrets management (K8s Secrets vs Vault)
- CHANGELOG (ручной vs semantic-release)
- Grafana dashboards (JSON в репо vs ручная настройка)
- ArchUnit тесты (shared/arch-rules vs дублирование)
- Testcontainers reuse mode
- [x] k8s NetworkPolicy — реализовано (доступ к сервисам только от Gateway)

## 10. Architecture Decisions (ADR)

Вынесены в [docs/adr.md](./adr.md). Основные: ADR-001 (auth), ADR-002 (Lesson/Quiz/Activity), ADR-003 (окончания склонений), ADR-004 (уроки с двумя родами), ADR-005 (единство окончаний -i, -u, -ṛ), ADR-006 (sangraha-service), ADR-007 (единая таблица quiz_item_score, без FK на content, производный статус).

