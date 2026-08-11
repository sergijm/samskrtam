# Задача: curriculum-service — scaffold модуля

**Что:** Создать пустой Gradle-модуль `services/curriculum-service` по образцу `services/feature-flag-service` (тот же stack: Java 21, Virtual Threads, Spring Boot 3, JPA, Flyway, Actuator, springdoc-openapi).
**Зачем:** Новый независимый сервис учебного плана, см. `docs/services/curriculum-service.md`.

## Контекст
**Milestone:** см. текущий milestone plan.
**Затронутые сервисы:** curriculum-service (новый)
**Инициатор:** пользователь

## Входные данные
- [x] Спецификация: `docs/services/curriculum-service.md`
- [ ] API Gateway маршрут — НЕ входит в эту задачу (см. curriculum-service.md §6, задача Агента 1)

## Шаги
1. Скопировать структуру `build.gradle.kts` из `services/feature-flag-service/build.gradle.kts`, поменять `artifactId`/имя модуля на `curriculum-service`.
2. Добавить модуль в корневой `settings.gradle.kts` (`include("services:curriculum-service")`).
3. Пакет базового кода: `sm.selflearn.samskrtam.curriculum`.
4. `application.yml`: `server.port: 8091`, `spring.application.name: curriculum-service`, datasource через переменные окружения (`CURRICULUM_DB_URL`, `CURRICULUM_DB_USER`, `CURRICULUM_DB_PASSWORD` — без хардкода, см. `docs/conventions.md`), `spring.flyway.schemas: curriculum`.
5. Добавить `CurriculumServiceApplication` (обычный `@SpringBootApplication`, без WebFlux).
6. Подключить Actuator (`/actuator/health`) и springdoc-openapi (`/v3/api-docs`, `/swagger-ui.html`) — как в других Virtual Threads сервисах.
7. `.env.example` в корне репозитория: добавить `CURRICULUM_DB_URL`, `CURRICULUM_DB_USER`, `CURRICULUM_DB_PASSWORD`.

## Критерии готовности (DoD)
- [ ] Модуль собирается (`./gradlew :services:curriculum-service:build`)
- [ ] Приложение стартует локально с БД по env-переменным, `/actuator/health` отвечает `UP`
- [ ] Checkstyle/SpotBugs не падают
