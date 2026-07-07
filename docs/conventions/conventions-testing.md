# Conventions: Тестирование

> Часть `docs/conventions.md`. Основной файл: `docs/conventions.md`

---

## 1. Структура тестов

src/test/java/sm/selflearn/samskrtam/{service}/:
- unit/service/ — JUnit 5 + Mockito, без Spring контекста
- unit/util/
- integration/api/ — MockMvc / WebTestClient, HTTP контракты
- integration/repository/ — Testcontainers, реальная БД
- arch/ — ArchUnit

## 2. Именование тестов

Метод именуется по шаблону: methodName_stateUnderTest_expectedBehavior.

Примеры:
- startSession_quizNotFound_returns404
- submitAnswer_alreadyAnswered_returnsConflict
- getEntry_cacheHit_doesNotCallExternalApi

## 3. Покрытие (JaCoCo)

Минимальный порог для классов сервисного слоя — 80%.

Настройка в build.gradle.kts:
- element = CLASS
- includes = sm.selflearn.samskrtam.*.service.*
- limit = 0.80

## 4. Обязательные тест-кейсы

| Сервис | Сценарий |
|---|---|
| quiz-service | старт сессии, верный/неверный ответ, fallback Redis→Postgres, дубликат ответа, сохранение события в Outbox-таблицу |
| content-service | CRUD квиза, session-data, STUDENT получает 403 на write, генерация VOCABULARY квизов по иерархии категорий |
| user-service | логин (успех/неверный пароль), регистрация (дубликат email) |
| statistics-service | агрегация статистики из Kafka-событий с помощью Kafka Streams |
| dictionary-service | cache hit, cache miss + внешний запрос, внешний API недоступен |
| api-gateway | нет JWT → 401, STUDENT на /content → 403, rate limit → 429 |