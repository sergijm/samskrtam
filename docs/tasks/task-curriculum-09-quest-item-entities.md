# Задача: curriculum-service — сущности QuestItem + shared payload-record'ы

**Что:** JPA-сущность `QuestItem`, репозиторий, и shared payload-модель (`shared/samskrtam-dtos`)
для 4 типов заданий склонения.
**Зачем:** См. `docs/services/quest-item-model.md` §1/§3, `docs/services/curriculum-quest-items.md` §1/§3.

## Зависит от
task-curriculum-08-quest-item-schema.md

## Шаги

### Shared-модуль (`shared/samskrtam-dtos`, пакет `sm.selflearn.samskrtam.quest`)
1. Если ещё не существуют — создать `QuestItemType` (интерфейс), `QuestDomain` (enum), `AnswerMode` (enum, значения `FREE_TEXT, SINGLE_CHOICE, MULTI_SELECT, SPAN_SELECT, MATCHING`), `QuestItemPayload` (маркерный интерфейс) — точные сигнатуры см. `docs/services/quest-item-model.md` §1. Если уже существуют (перенесены ранее из curriculum-service) — просто добавить значение `MATCHING` в `AnswerMode`, если его нет.
2. В пакете `sm.selflearn.samskrtam.quest` создать класс-держатель `GrammarQuestItemTypes` (если не существует) с константами `DECLENSION_FORM`, `DECLENSION_FORM_CHOICE`, `CASE_RECOGNITION`, `DECLENSION_MATCH` — точные `code()`/`domain()`/`defaultAnswerMode()` см. `docs/services/quest-item-model.md` §3.
3. В новом пакете `sm.selflearn.samskrtam.quest.declension` создать 3 record'а, реализующих `QuestItemPayload` (точные поля — `docs/services/curriculum-quest-items.md` §2.1–2.4):
   - `DeclensionFormPayload(String lemmaIast, String lemmaDevanagari, String morphologyClassCode, String gender, String caseType, String numberType, String correctFormIast, String correctFormDevanagari)`
   - `CaseRecognitionPayload(String wordFormIast, String wordFormDevanagari, String lemmaIast, String morphologyClassCode, String correctCaseType, String correctNumberType, String correctGender, boolean genderRequired, List<String> distractorCombinations)`
   - `DeclensionMatchPayload(String lemmaIast, String morphologyClassCode, List<DeclensionMatchPair> pairs)` с вложенным `DeclensionMatchPair(String pairId, String wordFormIast, String wordFormDevanagari, String caseType, String numberType)`

### curriculum-service (пакет `sm.selflearn.samskrtam.curriculum.questitem`)
4. `QuestItem` (`@Entity`, `@Table(schema = "curriculum", name = "quest_item")`): поля `id` (UUID, `@Id @GeneratedValue`), `topicId` (UUID), `itemType` (String), `answerMode` (String), `prompt` (String, `@Column(columnDefinition = "text")`), `correctAnswer` (String, nullable, `columnDefinition = "text"`), `distractors` (String — храним сериализованный JSON, маппинг через `@JdbcTypeCode(SqlTypes.JSON)` или `@Column(columnDefinition = "jsonb")` + Hibernate Types, как уже сделано для jsonb-полей в lexicon-сущностях — свериться с существующим примером использования jsonb в проекте, если такой есть, иначе — простейший вариант через `@Convert` с Jackson), `payload` (аналогично jsonb), `generatorSource` (String), `createdAt` (Instant, `@PrePersist`).
5. `QuestItemRepository extends JpaRepository<QuestItem, UUID>`: методы `findByTopicIdAndItemType(UUID topicId, String itemType, Pageable pageable)` (для случайной выборки — см. task-11), `countByTopicIdAndItemType(UUID topicId, String itemType)`, `deleteByTopicIdAndItemType(UUID topicId, String itemType)` (для regenerate).
6. `QuestItemGenerationKey` (`@Entity`, `@Table(schema = "curriculum", name = "quest_item_generation_key")`): `id`, `questItemId` (UUID), `generationKey` (String, unique).
7. `QuestItemGenerationKeyRepository extends JpaRepository<QuestItemGenerationKey, UUID>`: `existsByGenerationKey(String key)`.

## Критерии готовности (DoD)
- [ ] Модуль собирается, jsonb-поля читаются/пишутся корректно (юнит-тест на save+findById с непустым payload)
- [ ] Значение `AnswerMode.MATCHING` доступно и используется в `DECLENSION_MATCH`
