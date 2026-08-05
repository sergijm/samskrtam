# Задача: curriculum-service — Flyway-миграция

**Что:** Подключить готовую миграцию `V1__create_curriculum_schema.sql`.
**Зачем:** Схема `curriculum` с таблицами `topic` и `topic_prerequisite`, см. `docs/services/curriculum-service.md` §2.

## Контекст
**Milestone:** см. текущий milestone plan.
**Затронутые сервисы:** curriculum-service
**Зависит от:** task-curriculum-01-scaffold.md

## Шаги
1. Файл `V1__create_curriculum_schema.sql` (уже подготовлен, лежит в `services/curriculum-service/src/main/resources/db/migration/V1__create_curriculum_schema.sql`) — проверить, что путь совпадает с `spring.flyway.locations` из `application.yml`.
