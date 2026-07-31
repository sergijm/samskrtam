# Задачи: детальный грамматический разбор слов на VersePage

> Оркестратор: Агент 0. Контракты: Агент 6 (см. sangraha-service.md,
> sangraha-service/verse-word-grammar.md, openapi/sangraha/schemas/sangraha-schemas.yaml —
> уже обновлены, это входной контракт для задач ниже).
> Задачи разбиты на шаги ~30 сек работы модели (DeepSeek V4 Flash / Qwen3 Coder 30B A3B
> Instruct) — каждый шаг самодостаточен, ссылается на конкретный файл/раздел контракта.

---

## Агент 2 — Backend (sangraha-service)

**B1. Миграция.** Заменить содержимое
`services/sangraha-service/src/main/resources/db/migration/V1__create_schema.sql`
на SQL, приложенный оркестратором к задаче (реляционная схема: verse_words
без старых морфологических колонок + новые таблицы verse_word_morphology,
verse_word_derivation, см. verse-word-grammar.md §1). Удалить файлы
V2–V10 из той же папки миграций.

**B2. Новые enum'ы.** В пакете `model/` создать `FormType.java`,
`DerivationType.java`, `AnalysisConfidence.java` — точный список значений
в verse-word-grammar.md §2. Расширить существующий `PartOfSpeech.java` до
полного списка оттуда же.

**B3. Entity VerseWordMorphology.** Создать `model/VerseWordMorphology.java`
— JPA entity, таблица `verse_word_morphology`, PK = `verse_word_id`
(`@Id` + `@OneToOne` + `@MapsId` на `VerseWord`), поля caseType/gender/
numberType/person/tense/mood/voice — verse-word-grammar.md §1.

**B4. Entity VerseWordDerivation.** Аналогично B3, таблица
`verse_word_derivation`, поля derivationType/derivationalSuffix/
derivationalBase/description.

**B5. Entity VerseWord.** Обновить `model/VerseWord.java`: убрать поля
gender/caseType/numberType/person/tense/mood/voice (переехали в B3),
переименовать glossRu/glossEn → contextGlossRu/contextGlossEn, добавить
formType, isFinite, lemmaGlossRu, lemmaGlossEn, analysisConfidence,
ambiguityNotes; добавить `@OneToOne(mappedBy="verseWord", cascade=ALL,
orphanRemoval=true)` на morphology и derivation.

**B6. DTO.** Обновить `dto/VerseWordDto.java` — nested record'ы
MorphologyDto/DerivationDto, полный список полей верхнего уровня —
sangraha-schemas.yaml#VerseWordDto (контракт уже обновлён Агентом 6).

**B7. Mapper.** Обновить `mapper/VerseMapper.toWordDto()` под новую
структуру VerseWord → VerseWordDto (маппинг morphology/derivation entity
→ nested DTO, null если entity отсутствует).

**B8. Saver.** Обновить `service/VerseAnalysisSaver.buildWords()`: парсить
вложенный `morphology` из ответа LLM в VerseWordMorphology (создавать
только если хотя бы одно поле не null), derivationType/Suffix/Base +
derivation.description — в VerseWordDerivation, glossRu/glossEn →
contextGlossRu/contextGlossEn.

**B9. LLM tool-схема.** Обновить `service/LlmToolSchemaBuilder.
buildFunctionDefinitionSchema()` — добавить в схему `words[]` поля
formType, isFinite, morphology{}, derivationType, derivationalSuffix,
derivationalBase, derivation{}, lemmaGlossRu, lemmaGlossEn,
analysisConfidence, ambiguityNotes (verse-word-grammar.md §3). Внести те
же изменения в `service/JsonSchemas.buildVerseAnalysisSchema()`.

**B10. Промпт.** Заменить файл
`resources/prompts/verse-analysis.md` целиком содержимым приложения
`docs/tasks/attachments/B10-verse-analysis.md` (заменён пункт 4 «words» на
полный лексико-грамматический разбор + обновлена шапка файла со списком
параметров tool).

---

## Агент 3 — Frontend (VersePage)

**F1.** В компоненте таблицы `words[]` на VersePage
(`frontend/src/pages/sangraha/VersePage.tsx`) сделать строки
разворачиваемыми по клику (аккордеон/expand-row, без новых постоянных
колонок в самой таблице).

**F2.** В развороте строки отрендерить: surfaceDevanagari, lemmaIast,
root, stem, pos, formType, isFinite; блок «морфология» — только заполненные
поля из caseType/gender/numberType/person/tense/mood/voice; блок
«словообразование» — derivationType/derivationalSuffix/derivationalBase/
description; lemmaGlossRu/En отдельной подписанной строкой рядом с
contextGlossRu/En (см. verse-word-grammar.md §5).

**F3.** Обновить TS-типы под `VerseWordDto` из
`docs/openapi/sangraha/schemas/sangraha-schemas.yaml` (nested
morphology/derivation). analysisConfidence/ambiguityNotes в типах
присутствуют, но в UI не рендерятся (зарезервировано на будущее).

---

## Агент 4 — Testing (после B1–B10)

**T1.** Интеграционный тест round-trip: фиктивный tool_call с примером
`vaśīkṛtya` (ABSOLUTIVE, root `kṛ`, см. verse-word-grammar.md, пример из
промпта) → VerseAnalysisSaver → VerseWordRepository → VerseMapper →
VerseWordDto — проверить, что morphology/derivation не теряются при
сохранении/чтении.
