# quiz-service: Kafka и Outbox Pattern

> Часть спецификации quiz-service. Основной файл: `docs/services/quiz-service.md`

---

## 1. Архитектура публикации событий

quiz-service публикует события в Kafka исключительно через Transactional Outbox Pattern. Это гарантирует, что событие не потеряется при перезапуске сервиса между сохранением данных в БД и отправкой в Kafka.

Прямая публикация из бизнес-логики без outbox запрещена.

## 2. OutboxEventRepository

Поля:
- id (UUID)
- aggregate_type (VARCHAR: например, "QuizSession")
- aggregate_id (VARCHAR: например, sessionId)
- event_type (VARCHAR: QUIZ_ANSWERED / QUIZ_SESSION_STATUS_CHANGED)
- payload (TEXT: JSON события)
- created_at (TIMESTAMP WITH TIME ZONE)
- status (VARCHAR: NEW / PUBLISHED / FAILED)
- error_message (TEXT)
- retry_count (INTEGER)
- processed_at (TIMESTAMP WITH TIME ZONE)

### OutboxEventType (Enum)
- QUIZ_ANSWERED
- QUIZ_SESSION_STATUS_CHANGED

### OutboxStatus (Enum)
- NEW
- PUBLISHED
- FAILED

## 3. OutboxEventPublisherService

Периодически опрашивает таблицу outbox_events на новые события (status = NEW), публикует их в Kafka через ReactiveKafkaProducerTemplate и обновляет статус.

Методы:
- publishOutboxEvents: @Scheduled метод для запуска публикации.
- publishEvent(OutboxEvent event): публикует одно событие.
- getTopicForEventType(OutboxEventType eventType): определяет топик Kafka.

Топики именуются по правилу <domain>-<event>-events (kebab-case):
- quiz-answered-events
- quiz-session-status-changed-events

## 4. События

### QuizAnsweredEvent
Публикуется после каждого ответа пользователя. Содержит идентификатор сессии, пользователя, урока, слова, правильность ответа, время ответа.

### QuizSessionStatusChangedEvent
Публикуется при старте сессии (status=IN_PROGRESS) и завершении (status=COMPLETED). Консумируется statistics-service для агрегации.