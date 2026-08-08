# Задача: curriculum-service — DeclensionQuestItemBatchGenerator

**Что:** Сервисный класс, материализующий все 4 типа `QuestItem` склонения для одной темы
(topic ↔ morphologyClassCode) на основе существующих `Lexeme`/`morphology_class`.
**Зачем:** См. `docs/services/curriculum-quest-items.md` §4.

## Зависит от
task-curriculum-09-quest-item-entities.md

## Шаги

1. Класс `DeclensionQuestItemBatchGenerator` (пакет `sm.selflearn.samskrtam.curriculum.questgen`), зависимости: `LexemeRepository`, `LexemeMorphologyRepository` (или их эквиваленты из lexicon-модуля — свериться с фактическими именами репозиториев лексикона, они уже реализованы), `QuestItemRepository`, `QuestItemGenerationKeyRepository`, конфигурационный класс `DeclensionMatchProperties` (шаг 6).
2. Метод `void generateForTopic(UUID topicId, String morphologyClassCode)`:
   - выбрать все `Lexeme`, связанные с `morphologyClassCode` через `lexeme_morphology`;
   - для каждой леммы получить парадигму окончаний (падеж×число, а также — если применимо — род) для этого `morphologyClassCode`. Источник парадигмы окончаний — статическая таблица правил в коде (константы по классам основ, см. `architecture.md §3.3` — перенести те же правила, которыми ранее пользовался curriculum-service, без придумывания новых); каждая пара (падеж, число) даёт одну словоформу.
3. Для каждой (лемма, падеж, число) — построить `generationKey = topicId + ":" + itemType + ":" + lexemeId + ":" + caseType + ":" + numberType"`, проверить `questItemGenerationKeyRepository.existsByGenerationKey(key)` — если уже есть, пропустить (идемпотентность).
4. Сгенерировать и сохранить `QuestItem` типа `DECLENSION_FORM` (answerMode FREE_TEXT, correctAnswer = словоформа, distractors пусто, payload = `DeclensionFormPayload`).
5. Сгенерировать и сохранить `QuestItem` типа `DECLENSION_FORM_CHOICE` (answerMode SINGLE_CHOICE, correctAnswer = словоформа, distractors — 2–3 случайные словоформы той же леммы других падежей/чисел, payload — тот же `DeclensionFormPayload`).
6. Класс `DeclensionMatchProperties` (`@ConfigurationProperties(prefix = "curriculum.quest-items.declension-match")`, поле `int pairsPerItem` default 5) — регистрация через `@EnableConfigurationProperties` в конфиг-классе приложения.
7. Метод `generateCaseRecognition(...)`: для каждой (лемма, падеж, число) — определить `genderRequired` (проверка: существует ли в рамках этой же темы другая лемма/род с точно такой же словоформой для того же падежа/числа — если да, `genderRequired = true`), собрать 2–3 дистрактора (другие грамматически валидные комбинации падеж+число[+род] для той же основы), сохранить `QuestItem` типа `CASE_RECOGNITION` (answerMode SINGLE_CHOICE, prompt = словоформа, correctAnswer = отформатированная строка вида `"{Case} {Number}"` либо `"{Case} {Number} {Gender}"`, payload = `CaseRecognitionPayload`).
8. Метод `generateMatch(...)`: собрать все словоформы леммы, разбить на блоки по `declensionMatchProperties.pairsPerItem()` (последний неполный блок — пропустить, если пар меньше половины от `pairsPerItem`), сохранить один `QuestItem` типа `DECLENSION_MATCH` на блок (answerMode MATCHING, correctAnswer = null, payload = `DeclensionMatchPayload` со списком пар с `pairId = UUID.randomUUID().toString()`).
9. Каждый успешно сохранённый `QuestItem` — сразу же создать соответствующую строку `QuestItemGenerationKey` в той же транзакции.

## Критерии готовности (DoD)
- [ ] Повторный вызов `generateForTopic` для уже сгенерированной темы не создаёт дублей (юнит-тест: вызвать дважды, проверить количество строк не изменилось)
- [ ] Юнит-тест на `genderRequired` — сконструировать 2 леммы разного рода с совпадающей словоформой одного падежа/числа, убедиться что для обеих `genderRequired = true`
