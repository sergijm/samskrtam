# Задача: curriculum-service — REST-контроллер

**Что:** `TopicController`, реализующий все эндпоинты из `docs/openapi/curriculum/curriculum-service.yaml`.
**Зачем:** Публичный HTTP-контракт сервиса.

## Зависит от
task-curriculum-03-entities.md, task-curriculum-04-dto-mapper.md, task-curriculum-05-graph-service.md

## Шаги
1. `@RestController @RequestMapping("/api/v2/curriculum")`.
2. `GET /topics` (`?includeEvergreen=true|false`) → `List<TopicDto>`, без вычисления слоёв (просто `topicRepository.findAll()` + опциональный фильтр по `isEvergreen`).
3. `GET /topics/{id}` → объект `{topic, prerequisites}`; `prerequisites` — через `topicPrerequisiteRepository.findByIdTopicId(id)`, замаппленные в `TopicPrerequisiteDto` (с резолвленной темой). `topic.appearsInLevels` заполняется join-запросом по `complex_quiz_topic`/`complex_quiz` (см. task-07) плюс собственный `learningLevel` темы первым элементом списка. 404 (с `ErrorResponseDto`), если темы нет.
4. `POST /topics` — только роль `ADMIN` (`@PreAuthorize("hasRole('ADMIN')")` или эквивалент, используемый в других сервисах, см. `content-service.md` §5); 409, если `code` уже занят (проверка `existsByCode` до insert, не полагаться на constraint exception).
5. `PUT /topics/{id}` — ADMIN, 404 если не найдена, `code` из тела запроса игнорируется/отсутствует в DTO.
6. `DELETE /topics/{id}` — ADMIN, 404 если не найдена, 204 при успехе (каскад удаления prerequisite обеспечивает БД, дополнительный код не нужен).
7. `GET /topics/{id}/prerequisites` — 404 если тема не найдена, иначе список `TopicPrerequisiteDto`.
8. `POST /topics/{id}/prerequisites` — ADMIN; порядок проверок: (а) 404 если `id` или `prerequisiteTopicId` не существуют, (б) 409 если `id == prerequisiteTopicId` (self-loop), (в) 409 если `topicGraphService.wouldCreateCycle(id, prerequisiteTopicId)` вернул `true`, (г) иначе — сохранить, вернуть 201.
9. `DELETE /topics/{id}/prerequisites/{prerequisiteTopicId}` — ADMIN, 404 если связи нет, иначе 204.
10. `GET /graph` — публичный (не ADMIN), диагностический (не основная навигация — см. `curriculum-service.md` §6); загрузить все Topic и все TopicPrerequisite одним запросом каждый (не N+1), вызвать `topicGraphService.computeLayers(...)`, замаппить в `TopicGraphResponse`.
11. `GET /levels` и `GET /levels/{level}/topics` — простой CRUD поверх `TopicRepository.countByLearningLevel`/`findByLearningLevel`, без обращения к `TopicGraphService`.
12. Глобальный `@RestControllerAdvice` для `EntityNotFoundException → 404`, кастомного `TopicCycleException`/`DuplicateCodeException → 409`, `MethodArgumentNotValidException → 400`, кастомного `InvalidComplexQuizCompositionException → 422` (см. task-07), всё — в формате `ErrorResponseDto`.

## Критерии готовности (DoD)
- [ ] Все операции соответствуют `docs/openapi/curriculum/curriculum-service.yaml` (пути, коды ответов, поля тела)
- [ ] ADMIN-only эндпоинты возвращают 403 для не-ADMIN (интеграционный тест)
- [ ] Интеграционный тест на полный сценарий: создать 3 темы, связать A→B→C, попытаться добавить C→A (ожидаем 409), запросить `/graph` (ожидаем 3 слоя по одной теме)
- [ ] springdoc-generated `/v3/api-docs` не расходится по путям/операциям с ручным `curriculum-service.yaml` (используется только для сверки, источник истины — YAML в docs/)
