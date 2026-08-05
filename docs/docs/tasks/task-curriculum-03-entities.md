# Задача: curriculum-service — JPA-сущности и репозитории

**Что:** `Topic`, `TopicPrerequisite` (+ composite key), Spring Data JPA репозитории.
**Зачем:** ORM-слой поверх схемы из `V1__create_curriculum_schema.sql`, см. `docs/services/curriculum-service.md` §2.

## Зависит от
task-curriculum-01-scaffold.md, task-curriculum-02-migration.md

## Шаги
1. `Topic` (`@Entity`, `@Table(schema = "curriculum", name = "topic")`): поля `id` (UUID, `@Id @GeneratedValue`), `code`, `titleRu` (`@Column(name = "title_ru")`), `titleEn`, `learningLevel` (`@Column(name = "learning_level")`, enum `LearningLevel { L0, L1, L2, L3, L4, L5, L6 }` через `@Enumerated(EnumType.STRING)`), `isEvergreen` (`@Column(name = "is_evergreen")`), `displayOrder` (`@Column(name = "display_order")`, nullable), `createdAt`, `updatedAt` (заполнять в `@PrePersist`/`@PreUpdate`). Никакого поля/ссылки на `LearningMaterial` — эта сущность 1:N ссылается на `Topic` в обратную сторону и живёт в content-service, см. `docs/services/learning-materials.md` §1 и `curriculum-service.md` §5.
2. `TopicPrerequisiteId` — `@Embeddable` composite key: `topicId`, `prerequisiteTopicId` (оба UUID).
3. `TopicPrerequisite` (`@Entity`, `@Table(schema = "curriculum", name = "topic_prerequisite")`, `@EmbeddedId TopicPrerequisiteId id`): поле `strength` (String или enum `PrerequisiteStrength { RECOMMENDED, HELPFUL }` через `@Enumerated(EnumType.STRING)`), `createdAt`. НЕ добавлять `@ManyToOne` на `Topic` внутри composite key — если нужен доступ к связанной теме, делать отдельным `@MapsId`-полем `prerequisiteTopic` (только для чтения, ленивая загрузка).
4. `TopicRepository extends JpaRepository<Topic, UUID>`: методы `findByCode(String code)`, `existsByCode(String code)`, `findByLearningLevel(LearningLevel level, Sort sort)`, `countByLearningLevel(LearningLevel level)` (для `/levels`).
5. `TopicPrerequisiteRepository extends JpaRepository<TopicPrerequisite, TopicPrerequisiteId>`: методы `findByIdTopicId(UUID topicId)` (прямые prerequisite темы), `findByIdPrerequisiteTopicId(UUID prerequisiteTopicId)` (обратный поиск — «что зависит от этой темы», нужен для обхода циклов в task-curriculum-05), `findAll()` (для построения полного графа в `/graph`).

## Критерии готовности (DoD)
- [ ] Сущности мапятся на существующие таблицы без дополнительных миграций
- [ ] Юнит-тест на сохранение Topic + TopicPrerequisite и чтение через оба репозитория
- [ ] `code` UNIQUE constraint долетает как понятная ошибка (проверяется в сервисном слое до insert, не через try/catch SQL exception)
