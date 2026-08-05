# Conventions: Конфигурация

> Часть `docs/conventions.md`. Основной файл: `docs/conventions.md`

---

## 1. .env и application.yml

Все значения, специфичные для окружения, хранятся в корневом `.env` файле. В `application.yml` **не используются значения по умолчанию** — только ссылки на переменные окружения.

Пример правильного `application.yml`:

spring.datasource.url: ${SPRING_DATASOURCE_URL}
spring.datasource.username: ${DB_USER}
spring.datasource.password: ${DB_PASSWORD}

`.env` находится в корне монорепо, добавлен в `.gitignore`. В репозитории хранится `.env.example` со всеми ключами и комментариями, но без значений.

### Структура .env (ключевые группы)

- Database: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
- JDBC: SPRING_DATASOURCE_URL
- R2DBC: SPRING_R2DBC_URL
- Redis: REDIS_HOST, REDIS_PORT
- Kafka: KAFKA_BOOTSTRAP_SERVERS
- Keycloak: KEYCLOAK_URL, KEYCLOAK_REALM, KEYCLOAK_CLIENT_ID, KEYCLOAK_SECRET, KEYCLOAK_JWKS_URI
- Services: CONTENT_SERVICE_URL, USER_SERVICE_URL, DICTIONARY_SERVICE_URL, STATISTICS_SERVICE_URL, FEATURE_FLAG_SERVICE_URL, CORS_ALLOWED_ORIGINS
- Observability: MANAGEMENT_PORT, OTEL_EXPORTER_OTLP_ENDPOINT, TRACING_SAMPLING_PROBABILITY
- Swagger: SPRINGDOC_ENABLED
- Connection Pool: DB_POOL_MAX_SIZE, DB_POOL_MIN_IDLE, DB_CONNECTION_TIMEOUT_MS
- Graceful Shutdown: GRACEFUL_SHUTDOWN_TIMEOUT
- Spring: SPRING_PROFILES_ACTIVE
- MinIO: MINIO_URL, MINIO_PUBLIC_URL, MINIO_ACCESS_KEY, MINIO_SECRET_KEY, MINIO_BUCKET_*
- Quiz Service: QUIZ_SERVICE_PORT, APP_OUTBOX_FIXED_DELAY_MS
- Statistics Service: STATISTICS_SERVICE_PORT, SPRING_KAFKA_STREAMS_*
- Frontend: VITE_API_URL

## 2. Spring Profiles

- default: локальная разработка, Swagger включён, tracing sampling 100%
- staging: тестовый стенд, Swagger включён, tracing sampling 100%
- production: prod, Swagger выключен, tracing sampling 10%

Профиль активируется через `SPRING_PROFILES_ACTIVE` в `.env`.

## 3. Адреса сервисов по окружениям

Локально (IDEA): localhost + порт сервиса.

Порты: content-service (8081), user-service (8087), dictionary-service (8083), statistics-service (8084), feature-flag-service (8085).