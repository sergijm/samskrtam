# Задача: curriculum-service — ComplexQuiz (Mixed Practice / Level Assessment)

**Что:** Сущности `ComplexQuiz`/`ComplexQuizTopic`, DTO, сервис с валидацией состава, REST-эндпоинты `/complex-quizzes*`, а также join-запрос для `Topic.appearsInLevels`.
**Зачем:** См. `docs/services/curriculum-service.md` §4 и `docs/openapi/curriculum/curriculum-service.yaml` (tag `complex-quizzes`).

## Зависит от
task-curriculum-02-migration.md (таблицы `complex_quiz`/`complex_quiz_topic` уже созданы), task-curriculum-03-entities.md, task-curriculum-04-dto-mapper.md

## Шаги

### Entities
1. `ComplexQuizType { MIXED_PRACTICE, LEVEL_ASSESSMENT }` (enum).
2. `ComplexQuiz` (`@Entity`, `@Table(schema = "curriculum", name = "complex_quiz")`): `id`, `type` (`@Enumerated(EnumType.STRING)`), `learningLevel` (переиспользовать enum `LearningLevel` из task-03), `titleRu`, `titleEn`, `questionCountHint` (nullable), `createdAt`, `updatedAt`.
3. `ComplexQuizTopic` (`@Entity`, `@Table(schema = "curriculum", name = "complex_quiz_topic")`, составной `@EmbeddedId` `ComplexQuizTopicId(complexQuizId, topicId)` — по аналогии с `TopicPrerequisiteId` из task-03).
4. `ComplexQuizRepository extends JpaRepository<ComplexQuiz, UUID>`: `findByLearningLevelAndType(...)`, `findByLearningLevel(...)`, `findByType(...)`, `findAll()`.
5. `ComplexQuizTopicRepository extends JpaRepository<ComplexQuizTopic, ComplexQuizTopicId>`: `findByIdComplexQuizId(UUID)`, `findByIdTopicId(UUID)` (нужен для `Topic.appearsInLevels`), `countByIdComplexQuizId(UUID)`.

### DTO
6. `ComplexQuizDto`, `ComplexQuizSummaryDto`, `UpsertComplexQuizRequest` — поля 1:1 из `docs/openapi/curriculum/schemas/curriculum.yaml`. `ComplexQuizSummaryDto.topicCount` — из `countByIdComplexQuizId`, темы не резолвятся.
7. `ComplexQuizMapper` (MapStruct) для `ComplexQuiz → ComplexQuizSummaryDto`; сборка полного `ComplexQuizDto` (с резолвленными `TopicDto`) — вручную в сервисе, не в мапере (нужен отдельный запрос тем).

### Бизнес-логика
8. `ComplexQuizService.validateComposition(ComplexQuizType type, List<UUID> topicIds)`:
   - если `topicIds` содержит дубликаты — `InvalidComplexQuizCompositionException` (→ 422);
   - если `type == MIXED_PRACTICE` и размер не в `[2, 4]` — исключение;
   - если `type == LEVEL_ASSESSMENT` и размер не в `[5, 7]` — исключение;
   - если хотя бы один `topicId` не существует в `TopicRepository` — `EntityNotFoundException` (→ 404), проверять до валидации диапазона.
9. `createComplexQuiz`/`updateComplexQuiz` вызывают `validateComposition`, затем пересобирают строки `ComplexQuizTopic` (при update — удалить старые, вставить новые, в одной транзакции).
10. Метод `List<LearningLevel> resolveAppearsInLevels(UUID topicId, LearningLevel ownLevel)`: `ownLevel` первым элементом, затем `distinct` отсортированные `learningLevel` всех `ComplexQuiz`, где эта тема участвует (через `complexQuizTopicRepository.findByIdTopicId` → `complexQuizRepository.findAllById(...)`). Вызывается из `TopicController.getTopic` (task-06, шаг 3).

### Controller
11. `ComplexQuizController` (`@RestController @RequestMapping("/api/v2/curriculum/complex-quizzes")`): `GET` (список, фильтры `level`/`type`, `ComplexQuizSummaryDto`), `GET /{id}` (404 если нет, иначе полный `ComplexQuizDto`), `POST` (ADMIN), `PUT /{id}` (ADMIN), `DELETE /{id}` (ADMIN, 204).
12. `GET /levels` и `GET /levels/{level}/topics` — если ещё не реализованы в task-06, добавить здесь как часть того же `TopicController` (не создавать отдельный контроллер ради двух простых эндпоинтов).

## Критерии готовности (DoD)
- [ ] Валидация диапазона 2-4 / 5-7 покрыта тестами (граничные значения: 1, 2, 4, 5, 7, 8 элементов; дубликаты; несуществующий topicId)
- [ ] `PUT` корректно заменяет состав тем (не оставляет «хвостов» в `complex_quiz_topic`)
- [ ] `Topic.appearsInLevels` в интеграционном тесте: тема учтена в `ComplexQuiz` уровня L2 и L3 при собственном `learningLevel = L1` → `appearsInLevels = [L1, L2, L3]`
- [ ] Все операции соответствуют `docs/openapi/curriculum/curriculum-service.yaml` (пути, коды ответов, включая 422)
