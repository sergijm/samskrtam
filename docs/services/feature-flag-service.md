# feature-flag-service

> Домен: Feature Flags — управление поведением системы без перезапуска
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/feature-flag-service`
> Порт: `8088`
> Схема БД: `feature_flags`
> Status: **DRAFT**

---

## 1. Описание

CRUD-сервис для управления feature flags. Флаги хранятся в Redis (кэш) и PostgreSQL (персистентность).
При изменении флага — Redis pub/sub для оповещения api-gateway и других сервисов о смене значения.

**Разделение ответственности:**
- **feature-flag-service** — CRUD, хранение, аудит
- **api-gateway** — потребление флагов (rate limiting, email notifications) через Redis pub/sub

---

## 2. Сущности

**FeatureFlag** (таблица feature_flags): name (VARCHAR 80, PK), enabled (BOOLEAN, default false), description (VARCHAR 255), updatedAt (TIMESTAMPTZ), updatedBy (VARCHAR 100, email администратора)

**FlagHistoryEntry** (таблица flag_history): id (UUID), flagName (VARCHAR 80, FK), changedAt (TIMESTAMPTZ), changedBy (VARCHAR 100), oldValue (BOOLEAN), newValue (BOOLEAN), reason (TEXT, опционально). Индекс: (flagName, changedAt DESC).

---

## 3. API

```
GET    /api/v1/flags                    → список всех флагов
GET    /api/v1/flags/{name}             → один флаг
PATCH  /api/v1/flags/{name}             → изменить { enabled } (только ADMIN)
GET    /api/v1/flags/{name}/history     → история изменений флага
```

Ответ GET /api/v1/flags: [{ name, enabled, description, updatedAt, updatedBy }]

Ответ PATCH: обновлённый FeatureFlag. При успехе — Redis pub/sub на канал feature-flags-changed.

---

## 4. Алгоритм

**GET /flags:** проверить Redis cache (key = feature-flag:{name}). Если нет — загрузить из PostgreSQL, записать в Redis с TTL 1 час. Возвращает список всех флагов.

**PATCH /flags/{name}:** валидировать флаг существует. Записать в PostgreSQL. Обновить Redis. Опубликовать событие в Redis pub/sub (канал feature-flags-changed, payload: { name, enabled }). Записать в flag_history.

**Redis pub/sub consumer в api-gateway:** при получении события — обновить локальный кэш флагов, применить/снять rate limiting.

---

## 5. Backend структура

Пакет `controller/`: FeatureFlagController.
`service/`: FeatureFlagService.
`repository/`: FeatureFlagRepository (JPA), FlagHistoryRepository.
`model/`: FeatureFlag, FlagHistoryEntry.
`redis/`: FeatureFlagCacheService (Redis read-through + pub/sub publisher).

---

## 6. application.yml

Порт 8088, virtual threads enabled, datasource через env, default_schema: feature_flags, Redis через spring.data.redis.*, flyway schemas: feature_flags.

---

## 7. Flyway миграции

V1 — schema feature_flags; V2 — таблица feature_flags; V3 — таблица flag_history с FK на feature_flags.

---

## 8. Acceptance Criteria

- GET /api/v1/flags возвращает список флагов из Redis (read-through)
- PATCH /api/v1/flags/{name} обновляет флаг и публикует событие в Redis pub/sub
- Только ADMIN может изменять флаги (403 для STUDENT)
- История изменений сохраняется и доступна через GET /flags/{name}/history
- Флаг не может быть удалён — только изменён (enabled = true/false)