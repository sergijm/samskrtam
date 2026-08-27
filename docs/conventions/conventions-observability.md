# Conventions: Observability (логирование, трассировка, метрики)

> Часть `docs/conventions.md`. Основной файл: `docs/conventions.md`

---

## 1. Логирование

### Обязательные правила

- Использовать **@Slf4j** (Lombok). System.out.* и System.err.* запрещены.
- Логи только в **JSON** формате через logstash-logback-encoder — для интеграции с Loki.
- Sensitive данные (email, пароли, токены, userId в URL) **не логируются ни на каком уровне**.
- В реактивном стеке (quiz-service, api-gateway) использовать doOnError для логирования с **сохранением** ошибки в pipeline, onErrorResume — только для обработки с подменой.
- Ошибки публикации в Kafka через Outbox Pattern должны логироваться с уровнем ERROR или WARN в зависимости от возможности повторной обработки.

### Уровни логирования

- TRACE: вход в сервисные методы
- DEBUG: бизнес-решения (кэш-промах, выбор ветки)
- INFO: старт/стоп, миграции, Kafka
- WARN: 4xx downstream, retry, деградация
- ERROR: 5xx/timeout, необработанные исключения, ошибки Kafka после всех попыток

## 2. Сквозная трассировка (Micrometer Tracing)

Стек: Micrometer Tracing → OpenTelemetry → Grafana Tempo (трейсы); Logback JSON → Promtail → Loki (логи + traceId); Micrometer → /actuator/prometheus → Prometheus (метрики); единый UI — Grafana.

Конфигурация (application.yml всех сервисов):
- management.tracing.sampling.probability из TRACING_SAMPLING_PROBABILITY
- management.otlp.tracing.endpoint из OTEL_EXPORTER_OTLP_ENDPOINT
- propagation type = w3c

Virtual Threads: traceId автоматически через MDC + Micrometer.
WebFlux: требуется ReactorContextAccessor (ThreadLocal не работает).
Propagation: WebClient автоматически добавляет traceparent; Gateway пробрасывает во все downstream.

Стек Observability: Tempo, Loki, Prometheus, Grafana.

## 3. Actuator

### Management порты

Каждый сервис выносит Actuator на свой порт, настраиваемый через .env. Gateway не проксирует management порты.

| Сервис | Env переменная | Порт по умолчанию |
|---|---|---|
| api-gateway | GATEWAY_MANAGEMENT_PORT | 9090 |
| user-service | USER_MANAGEMENT_PORT | 9092 |
| curriculum-service | CONTENT_MANAGEMENT_PORT | 9093 |
| quiz-service | QUIZ_MANAGEMENT_PORT | 9094 |
| dictionary-service | DICTIONARY_MANAGEMENT_PORT | 9095 |
| statistics-service | STATISTICS_MANAGEMENT_PORT | 9096 |

### Health Groups

- Liveness (/actuator/health/liveness): JVM alive + disk space
- Readiness (/actuator/health/readiness): DB, Redis, Kafka
- Liveness не включает внешние зависимости

### Кастомные метрики

- quiz-service: quiz.answers.total (Counter, tags: lessonType/correct), quiz.session.duration (Timer, lessonType), quiz.cache.miss (Counter)
- statistics-service: kafka.events.processed (Counter, topic)
- dictionary-service: dictionary.cache.hit (Counter)