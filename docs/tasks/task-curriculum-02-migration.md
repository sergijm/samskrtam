# Задача: curriculum-service — Flyway-миграция

**Что:** Подключить готовую миграцию `V1__create_curriculum_schema.sql`.
**Зачем:** Схема `curriculum` с таблицами `topic` и `topic_prerequisite`, см. `docs/services/curriculum-service.md` §2.

## Контекст
**Milestone:** см. текущий milestone plan.
**Затронутые сервисы:** curriculum-service
**Зависит от:** task-curriculum-01-scaffold.md

## Шаги
1. Файл `V1__create_curriculum_schema.sql` (уже подготовлен, лежит в `services/curriculum-service/src/main/resources/db/migration/V1__create_curriculum_schema.sql`) — проверить, что путь совпадает с `spring.flyway.locations` из `application.yml`.
2. Убедиться, что миграция применяется на пустой БД (`CREATE SCHEMA IF NOT EXISTS curriculum` + таблицы `topic`, `topic_prerequisite` с FK/CHECK/индексами как в файле — **не менять содержимое файла**, если не найдена ошибка).
3. Добавить БД `curriculum` (или отдельную схему в общем PostgreSQL-инстансе dev-окружения) в локальный docker-compose переопределения агента, если требуется для локального прогона (сам docker-compose.yaml не трогать — это задача Агента 5).
4. Прогнать `flyway:migrate` / поднять приложение и убедиться, что миграция применилась (`SELECT * FROM flyway_schema_history`).

## Критерии готовности (DoD)
- [ ] Миграция применяется без ошибок на чистой БД
- [ ] Таблицы `curriculum.topic`, `curriculum.topic_prerequisite` существуют с полями/constraints из миграции
- [ ] Повторный старт приложения не переприменяет миграцию (idempotent)
