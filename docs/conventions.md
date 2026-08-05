# Conventions

> Соглашения, обязательные для всех сервисов проекта.
> Связанные файлы: [architecture.md](./architecture.md) · [README.md](./README.md)

---

## 1. Конфигурация

- Переменные окружения в `.env` (без дефолтов в `application.yml`)
- Spring Profiles: `default`/`staging`/`production`
- [Подробнее →](conventions/conventions-configuration.md)

## 2. Observability (логирование, трассировка, метрики)

- Логирование: `@Slf4j`, JSON (logstash-logback-encoder), без чувствительных данных
- Трассировка: Micrometer → OpenTelemetry → Tempo
- Actuator: management-порт, health groups (liveness/readiness), кастомные метрики
- [Подробнее →](conventions/conventions-observability.md)

## 3. API Design, Swagger, обработка ошибок

- Swagger-агрегация через Gateway
- Пагинация (page/size/sort), версионирование (v1/v2)
- Rate Limiting (`auth/**`, `api/**`), CORS
- Checkstyle, SpotBugs, порядок шагов CI
- [Подробнее →](conventions/conventions-api.md)

## 4. Тестирование

- Структура: unit/integration/arch
- Именование: `methodName_stateUnderTest_expectedBehavior`
- JaCoCo ≥ 80% сервисного слоя
- Обязательные тест-кейсы по сервисам
- [Подробнее →](conventions/conventions-testing.md)

## 5. Graceful shutdown

- `server.shutdown = graceful`, таймаут — из `GRACEFUL_SHUTDOWN_TIMEOUT`

## 6. Git Conventions

- Conventional Commits: `<type>(<scope>): <description>`
- Типы: `feat`/`fix`/`docs`/`refactor`/`test`/`chore`/`perf`
- Ветки: `main` → `feat/*`/`fix/*`/`chore/*`, PR требует прохождения CI и code review

## 7. Kafka

- Топики: `<domain>-<event>-events` (kebab-case)
- Публикация — только через Transactional Outbox Pattern
- Синхронные вызовы domain ↔ domain по HTTP допустимы только для узких сценариев «один producer, один consumer» без нужды в асинхронной доставке (пример — синхронизация лексики sangraha-service → content-service, см. [architecture.md §3.5](./architecture.md#35-sangraha-service-произведения-llm-анализ-стихов-синхронизация-лексики-через-rest)); в остальных случаях — Kafka + Outbox

## 8. Мапперы Entity/DTO

- `MapStruct @Mapper(componentModel = "spring")`, пакет `mapper/`
- Простой 1:1 маппинг выносится в `mapper/`
- DTO из нескольких источников — `.builder()` в `*Service` допустим
- `abstract class` с `@Autowired` в маппере — запрещён

## 9. Документация: паттерн «индекс + подпапка»

Для поддержания компактности документации (лимит 350 строк на файл, см. [`samskrtam-agents-spec.md`](./agents/samskrtam-agents-spec.md) §«Поддержание компактности документации»):

- Если markdown-файл спецификации приближается к лимиту 350 строк или тема естественно распадается на подтемы — создаётся файл-индекс с тем же именем на уровень выше и одноимённая подпапка с деталями.
- Примеры применения: `conventions.md` + `conventions/`, `services/quest-engine.md` (единый файл — целиком укладывается в лимит, подпапка не нужна).
- Разрозненные плоские файлы по одной теме (например, frontend, eamenau) реорганизуются по этому же паттерну, а не растут независимо.

## 10. Архитектурные решения

Ключевые архитектурные решения проекта (границы auth между Gateway и user-service, семантика Quiz/Lesson/Activity, модель окончаний склонений, местоимения, sangraha-service, единая таблица прогресса `quiz_item_score`) зафиксированы в [architecture.md §3](./architecture.md#3-ключевые-архитектурные-решения).

## 11. Открытые вопросы

Общепроектные открытые вопросы — в [architecture.md §4](./architecture.md#4-открытые-вопросы).
