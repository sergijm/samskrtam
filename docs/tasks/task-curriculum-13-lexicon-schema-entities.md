# Задача: curriculum-service — модуль lexicon: миграции и JPA-сущности

**Что:** Миграции `V2__add_topic_domain.sql`/`V3__create_lexicon_schema.sql` +
JPA-сущности пакета `sm.selflearn.samskrtam.curriculum.lexicon`.
**Зачем:** См. `docs/services/lexicon.md`, `docs/services/lexical-curriculum.md` §1.
**Важно:** `Lexeme.status` — `DRAFT|CANDIDATE|APPROVED|REJECTED` (без `AI_ENRICHED`,
см. `lexicon-content-pipeline.md` §1).

## Зависит от
task-curriculum-02-migration.md, task-curriculum-03-entities.md (переиспользуется
`Topic`, добавляется поле `domain`)

## Шаги (каждый — отдельный небольшой PR/коммит)

1. Миграция `V2__add_topic_domain.sql`: `ALTER TABLE curriculum.topic ADD COLUMN domain VARCHAR(10) NOT NULL DEFAULT 'GRAMMAR';` + `CHECK (domain IN ('GRAMMAR','LEXICON'))`.
2. Миграция `V3__create_lexicon_schema.sql`, часть 1: таблицы `lexeme`, `word_form` — поля и констрейнты по `lexicon.md` §1–§2, `UNIQUE(lemma_slp1, gender)` на `lexeme`.
3. `V3__create_lexicon_schema.sql`, часть 2: `frequency_band`, `lexeme_frequency` (`PRIMARY KEY(lexeme_id, source)`) — поля по `lexicon.md` §3.1.
4. `V3__create_lexicon_schema.sql`, часть 3: `semantic_topic` (self-FK `parent_id`), `lexeme_semantic_topic` (M:N) — поля по `lexicon.md` §3.2.
5. `V3__create_lexicon_schema.sql`, часть 4: `part_of_speech`, `lexeme_pos`, `morphology_class`, `lexeme_morphology` — поля по `lexicon.md` §3.3–§3.4.
6. `V3__create_lexicon_schema.sql`, часть 5: `source`, `source_occurrence` — поля по `lexicon.md` §4, включая `external_sangraha_work_slug`.
7. `V3__create_lexicon_schema.sql`, часть 6: `user_collection`, `user_collection_item`, `user_lexeme_progress`, `lexical_topic_binding` — поля по `lexicon.md` §5–§6, `lexical-curriculum.md` §1.
8. `V3__create_lexicon_schema.sql`, часть 7: `vocabulary_quiz_definition` — поля по `lexical-quizzes.md` §2.
9. Прогнать все миграции на чистой БД по порядку V1→V2→V3, зафиксировать успешный прогон в PR-описании.
10. Entity `Topic`: добавить поле `domain` (`enum TopicDomain{GRAMMAR,LEXICON}`, `@Enumerated(STRING)`).
11. Entity `Lexeme` + enum `LexemeStatus{DRAFT,CANDIDATE,APPROVED,REJECTED}`, enum `Gender{MASCULINE,FEMININE,NEUTER,UNSPECIFIED}`.
12. Entity `WordForm` (`@ManyToOne` на `Lexeme` lazy, nullable `sourceOccurrenceId`).
13. Entity `FrequencyBand` (`code` — `@Id`) + `LexemeFrequency` (`@EmbeddedId(lexemeId, source)`).
14. Entity `SemanticTopic` (self-ref `parentId`) + `LexemeSemanticTopic` (`@EmbeddedId`).
15. Entity `PartOfSpeech` (`code` — `@Id`, `group` enum) + `LexemePos` (`@EmbeddedId`).
16. Entity `MorphologyClass` + `LexemeMorphology` (`@EmbeddedId`).
17. Entity `Source` + `SourceOccurrence` (`@ManyToOne` на `Source` и `Lexeme`).
18. Entity `UserCollection` + `UserCollectionItem` (`@EmbeddedId(collectionId, lexemeId)`).
19. Entity `UserLexemeProgress` (`@EmbeddedId(userId, lexemeId)`).
20. Entity `LexicalTopicBinding` (`@EmbeddedId(lexicalTopicId, lexemeId)`, `@ManyToOne` на `Topic`).
21. Entity `VocabularyQuizDefinition` (`kind` enum по `lexical-quizzes.md` §2).
22. `JpaRepository` для каждой сущности (минимальные finder-методы: по `status`, по `lemmaSlp1`+`gender`, по `parentId` и т.д. — по потребности задач 14/15/16).
23. Юнит-тест: создать `Lexeme`, привязать 2 `SemanticTopic` + 1 `PartOfSpeech` + 1 `MorphologyClass`, прочитать обратно — связи не теряются.

## DoD
- [ ] Миграции V1→V2→V3 применяются без ошибок
- [ ] Все сущности мапятся без доп. изменений схемы
- [ ] Тест из шага 23 зелёный
