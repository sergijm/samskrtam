# Conventions: API Design, Swagger, обработка ошибок

> Часть `docs/conventions.md`. Основной файл: `docs/conventions.md`

---

## 1. Swagger / OpenAPI

Агрегация в Gateway через springdoc.swagger-ui.urls (5 сервисов). Swagger UI: /swagger-ui.html через Gateway. Все публичные эндпоинты аннотируются.

## 2. Обработка ошибок

GlobalExceptionHandler (WebFlux — quiz-service, gateway) + ErrorHandlingFilter (Gateway). Детали — в спецификациях соответствующих сервисов.

## 3. API Design

### Пагинация

Параметры запроса: page (0-based), size (default 20, max 100), sort (field,asc|desc).

Формат: GET /api/v1/...?page=0&size=20&sort=createdAt,desc → PageResponse<...>

### Версионирование API

Текущая версия: v1 в path.
- Обратно совместимые изменения (новые поля, новые эндпоинты) — без смены версии.
- Ломающие изменения — новый префикс /api/v2/, v1 поддерживается параллельно минимум один релиз.

### Rate Limiting (Gateway)

auth/**: replenishRate=5, burstCapacity=10
api/**: replenishRate=20, burstCapacity=40

### CORS (Gateway)

allowed-origins из CORS_ALLOWED_ORIGINS, методы GET/POST/PUT/PATCH/DELETE/OPTIONS, заголовки Authorization/Content-Type/X-Request-Id, credentials=true, max-age=3600.

## 4. Качество кода

Checkstyle (root build.gradle.kts, toolVersion = "10.17.0", config/checkstyle/checkstyle.xml).
SpotBugs (effort = MAX, confidence = MEDIUM, exclude.xml).

Порядок CI: test → jacocoTestReport → jacocoTestCoverageVerification → checkstyleMain → spotbugsMain. Сборка падает при нарушении любого этапа.