# Задача: curriculum-service — DTO и мапперы

**Что:** Java-record DTO по схемам `docs/openapi/curriculum/schemas/curriculum.yaml` + MapStruct-маппер Entity↔DTO.
**Зачем:** Контракт REST-слоя должен буква-в-букву соответствовать OpenAPI v2.

## Зависит от
task-curriculum-03-entities.md

## Шаги
1. Пакет `sm.selflearn.samskrtam.curriculum.dto`. Все DTO — `record`, поля и имена — как в `schemas/curriculum.yaml` (camelCase в JSON совпадает с именами полей record).
2. `TopicDto(UUID id, String code, String titleRu, String titleEn, LearningLevel learningLevel, boolean isEvergreen, Short displayOrder, List<LearningLevel> appearsInLevels, OffsetDateTime createdAt, OffsetDateTime updatedAt)` — `appearsInLevels` заполняется только сервисным слоем при `GET /topics/{id}` (см. task-07), в остальных местах `null`.
3. `PrerequisiteStrengthDto` — переиспользовать enum из Entity-слоя (task-03) напрямую, отдельный DTO-enum не создавать.
4. `TopicPrerequisiteDto(TopicDto topic, PrerequisiteStrength strength)` — обратите внимание: `topic` — это вложенный **резолвленный** TopicDto самого prerequisite (не id), см. схему.
5. `CreateTopicRequest(String code, String titleRu, String titleEn, LearningLevel learningLevel, Boolean isEvergreen, Short displayOrder)` — с `jakarta.validation` аннотациями: `@NotBlank @Size(max=80)` на `code`, `@NotBlank @Size(max=200)` на оба title, `@NotNull` на `learningLevel`.
6. `UpdateTopicRequest(String titleRu, String titleEn, LearningLevel learningLevel, Boolean isEvergreen, Short displayOrder)` — без `code` (иммутабельно), `@NotNull` на `learningLevel`.
7. `AddPrerequisiteRequest(UUID prerequisiteTopicId, PrerequisiteStrength strength)` — оба `@NotNull`.
8. `GraphLayerDto(int layer, List<TopicDto> topics)`, `TopicGraphResponse(List<GraphLayerDto> layers, List<TopicDto> evergreen)`.
9. `ErrorResponseDto(int status, String message, String details)` — переиспользовать общий обработчик ошибок, если такой уже есть в shared-модуле (`shared/common-dto`), иначе создать локально.
10. `LevelSummaryDto(LearningLevel level, int topicCount)`.
11. `TopicMapper` (MapStruct, `componentModel = "spring"`): `Topic → TopicDto`, `CreateTopicRequest → Topic` (без id/createdAt/updatedAt — их проставляет сервисный слой).

Схемы `ComplexQuizDto`/`ComplexQuizSummaryDto`/`UpsertComplexQuizRequest` — см. отдельную задачу `task-curriculum-07-complex-quiz.md`, не создавать здесь, чтобы не дублировать работу.

## Критерии готовности (DoD)
- [ ] Поля DTO 1:1 совпадают с `docs/openapi/curriculum/schemas/curriculum.yaml`
- [ ] MapStruct генерирует маппер без warnings (unmapped target properties явно проигнорированы через `@Mapping(target = ..., ignore = true)`)
- [ ] Bean Validation аннотации соответствуют ограничениям из схемы (`minLength`, `maxLength`)
