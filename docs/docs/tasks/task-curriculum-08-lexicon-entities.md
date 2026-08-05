# Задача: curriculum-service — модуль lexicon: миграции и JPA-сущности

**Что:** Подключить `V2__add_topic_domain.sql`/`V3__create_lexicon_schema.sql`, создать JPA-сущности пакета `sm.selflearn.samskrtam.curriculum.lexicon`.
**Зачем:** См. `docs/services/lexicon.md`, `docs/services/lexical-curriculum.md` §1.

## Зависит от
task-curriculum-02-migration.md, task-curriculum-03-entities.md (переиспользуется `Topic`, добавляется поле `domain`)

## Шаги

### Миграции
1. Файлы `V2__add_topic_domain.sql`, `V3__create_lexicon_schema.sql` уже подготовлены (`services/curriculum-service/src/main/resources/db/migration/`) — проверить порядок применения после `V1`, прогнать на чистой БД, убедиться что `V2` не ломает уже накопленные grammar-темы (DEFAULT `'GRAMMAR'`).

### Entity: Topic (расширение)
2. Добавить в существующий `Topic` (task-03) поле `domain` (enum `TopicDomain { GRAMMAR, LEXICON }`, `@Enumerated(EnumType.STRING)`, `@Column(name = "domain")`).

### Новые Entity (пакет `...curriculum.lexicon`)
3. `Lexeme`: `id`, `lemmaIast`, `lemmaDevanagari`, `lemmaSlp1`, `glossRu`, `glossEn`, `longDefinitionRu`, `longDefinitionEn` (nullable), `gender` (enum, nullable), `status` (enum `DRAFT|AI_ENRICHED|APPROVED|REJECTED`), `createdAt`/`updatedAt`.
4. `WordForm`: `id`, `lexemeId` (`@ManyToOne` lazy), `formIast`, `formDevanagari`, `grammaticalNote` (nullable), `sourceOccurrenceId` (nullable, `@ManyToOne` lazy).
5. `FrequencyBand` (справочник, `code` — `@Id`), `LexemeFrequency` (`@EmbeddedId(lexemeId, source)`, `rank`).
6. `SemanticTopic` (self-referencing `parentId`), `LexemeSemanticTopic` (`@EmbeddedId(lexemeId, semanticTopicId)`).
7. `PartOfSpeech` (справочник, `code` — `@Id`, `group` enum), `LexemePos` (`@EmbeddedId(lexemeId, posCode)`).
8. `MorphologyClass` (справочник), `LexemeMorphology` (`@EmbeddedId(lexemeId, morphologyClassCode)`).
9. `Source`, `SourceOccurrence` (`@ManyToOne` на `Source` и `Lexeme`).
10. `UserCollection`, `UserCollectionItem` (`@EmbeddedId(collectionId, lexemeId)`).
11. `UserLexemeProgress` (`@EmbeddedId(userId, lexemeId)`).
12. `LexicalTopicBinding` (`@EmbeddedId(lexicalTopicId, lexemeId)` — `lexicalTopicId` ссылается на `Topic.id`, обычный FK, можно смело сделать `@ManyToOne` на `Topic`, т.к. одна БД).
13. `VocabularyQuizDefinition` (поля по `V3` миграции, `kind` enum).
14. Репозитории `JpaRepository` для каждой сущности с минимально нужными finder-методами (по аналогии с task-03/task-07, не описываются здесь подробно — реализующий агент подбирает по потребностям задач 09/10).

## Критерии готовности (DoD)
- [ ] Миграции применяются последовательно V1→V2→V3 без ошибок
- [ ] Все сущности мапятся без дополнительных изменений схемы
- [ ] Юнит-тест: создать Lexeme, привязать 2 SemanticTopic + 1 POS + 1 MorphologyClass, прочитать обратно — все связи не теряются
