# sangraha-service / Детальный грамматический разбор слова (VerseWord)

> Родитель: [sangraha-service.md](../sangraha-service.md) §2, §3, §5.1, §7.
> Задача: VersePage должен показывать не только поверхностную морфологию
> (падеж/число/род и т.п.), но и полный лексико-деривационный разбор слова —
> лемму отдельно от словоформы, корень, тип формы (финитная/нефинитная),
> словообразование, отдельно словарное и контекстное значение, уверенность
> анализа. Источники: миграция Flyway (прислана оркестратору как готовый SQL,
> задача Агенту 2 — применить как новую единственную `V1__create_schema.sql`,
> история миграций сведена к одному файлу — проект без прод-данных) и
> обновлённый промпт `prompts/verse-analysis.md` §4 (см. §3 ниже).
> Status: **DRAFT**

---

## 1. Сущности (задача Агенту 2)

**VerseWord** (таблица verse_words) — не JSONB, реляционная модель, три таблицы:

Собственные поля verse_words: id (UUID), verseId (UUID), position (int).
Surface form: surfaceIast, surfaceDevanagari — точная словоформа из текста,
без нормализации.
Лексический анализ: lemmaIast (словарная форма/лемма), stem (основа,
опционально), root (корень/дхату, опционально), pos (часть речи).
Тип формы: formType (FINITE|INFINITIVE|ABSOLUTIVE|PARTICIPLE|GERUNDIVE|
OTHER_NONFINITE|NOMINAL|ADJECTIVAL|PRONOMINAL|INDECLINABLE), isFinite
(boolean, nullable).
Словарное значение леммы (не путать со значением словоформы, переезд в
vocabulary_words отложен — это данные для квизов, а не грамматика, см.
sangraha-service.md §1): lemmaGlossRu, lemmaGlossEn.
Контекстное значение конкретной словоформы (бывшие gloss_ru/gloss_en,
переименованы во избежание путаницы с lemma_gloss_*): contextGlossRu,
contextGlossEn — обязательны (NOT NULL).
Внутренние морфофонемные правила: formationRuleNumbers — как и раньше,
JSON-массив int, сериализован в TEXT-колонку `formation_rule_numbers`.
Уверенность анализа: analysisConfidence (HIGH|MEDIUM|LOW, CHECK-constraint),
ambiguityNotes (текст, nullable).
Ссылка на словарную статью для квизов: vocabularyWordId (UUID, nullable) —
без изменений.

**VerseWordMorphology** (таблица verse_word_morphology, 1:1 с verse_words,
FK verse_word_id = PK, ON DELETE CASCADE) — строка создаётся только если
для слова вообще есть применимая морфология; поля внутри тоже могут быть
null:
caseType, gender, numberType — для именных/адъективных/местоименных форм.
person, tense, mood — только для финитных глагольных форм.
voice — залог; применим и к финитным, и к нефинитным формам (причастие,
герундив, абсолютив тоже имеют залог), в отличие от person/tense/mood.

**VerseWordDerivation** (таблица verse_word_derivation, 1:1 с verse_words,
FK verse_word_id = PK, ON DELETE CASCADE) — строка создаётся только если
словообразование установлено:
derivationType (SIMPLE_INFLECTION|ABSOLUTIVE|PARTICIPLE|GERUNDIVE|
INFINITIVE|CAUSATIVE|DESIDERATIVE|DENOMINATIVE|COMPOUND_VERB|OTHER).
derivationalSuffix, derivationalBase — IAST, опционально.
description — человекочитаемое пояснение словообразования, опционально.

Не создавать jsonb-колонки `morphology`/`derivation` — по решению
оркестратора реляционная структура (отдельные таблицы) предпочтительнее
JSONB для этого домена (разграничение ответственности, индексируемость
обычными btree-индексами, типобезопасность через JPA-энумы).

## 1а. NounStem (таблица noun_stems) — DEPRECATED, заменена NominalLemma

**Статус: deprecated.** Реализована (миграция + JPA), но выявлен недостаток
дизайна: `stem_iast`/`stem_class`/`confidence` — свойства **леммы**, а не
конкретного вхождения слова в стих, поэтому хранение по `verse_word_id`
дублирует одну и ту же классификацию на каждое вхождение (например, `rāma`
может встречаться в корпусе сотни раз — и все сотни строк `noun_stems` несли
бы одинаковые `stem_iast`/`stem_class`). Заменена таблицей `nominal_lemmas`
(см. §1б ниже), где классификация хранится один раз на лемму. Таблица
`noun_stems` **не удаляется** в миграции — оставлена для отката/сверки, но
поиск (`sangraha-service.md` §9) больше её не использует. JPA-сущность
`NounStem`/поле `VerseWord.nounStems` из кода не удаляются в рамках текущей
задачи — отдельное решение об удалении принимается позже, после проверки
`nominal_lemmas` на реальных данных.

## 1б. NominalLemma (таблица nominal_lemmas)

Одна строка на лемму (`lemma_iast`, `UNIQUE`), а не на вхождение слова —
классификация не дублируется. Название `nominal_lemmas`, а не `noun_lemmas`
— задел на будущее для других склоняемых частей речи (прилагательные,
местоимения, числительные), не только существительных.

Поля: id (BIGSERIAL, PK — единственная таблица в sangraha с числовым PK,
остальные используют UUID; сознательное отклонение по присланному SQL,
пересмотр — по решению Агента 2/оркестратора отдельным тикетом, не блокирует
эту задачу), lemmaIast (TEXT, NOT NULL, UNIQUE), stemIast (TEXT, nullable),
stemClass (TEXT, nullable, без CHECK-constraint — намеренно, набор значений
не ограничивается текущими 7 regular-classes ради будущего расширения на
другие части речи), confidence (TEXT, nullable, CHECK-constraint HIGH|MEDIUM|
LOW — тот же паттерн, что у `analysisConfidence`), model (TEXT, nullable),
createdAt/updatedAt (TIMESTAMPTZ, NOT NULL, DEFAULT now()).

**Связь с VerseWord — не через JPA-relationship.** Нет физической FK-колонки
в `verse_words`, указывающей на `nominal_lemmas` — связь только по совпадению
текста `lemma_iast`. Натягивать `@ManyToOne`/`@OneToMany` через
`referencedColumnName` не нужно (усложнение ORM ради несуществующей
физической связи) — на этапе поиска (§9) делается обычный batch-запрос
репозитория `findByLemmaIastIn(Collection<String>)` по набору лемм слов-
кандидатов.

## 2. Новые Java-энумы (задача Агенту 2, пакет model/)

FormType: FINITE, INFINITIVE, ABSOLUTIVE, PARTICIPLE, GERUNDIVE,
OTHER_NONFINITE, NOMINAL, ADJECTIVAL, PRONOMINAL, INDECLINABLE.
DerivationType: SIMPLE_INFLECTION, ABSOLUTIVE, PARTICIPLE, GERUNDIVE,
INFINITIVE, CAUSATIVE, DESIDERATIVE, DENOMINATIVE, COMPOUND_VERB, OTHER.
AnalysisConfidence: HIGH, MEDIUM, LOW.
Существующий PartOfSpeech расширяется до полного набора из промпта: NOUN,
VERB, ADJECTIVE, PRONOUN, ADVERB, PARTICLE, INDECLINABLE, NUMERAL,
CONJUNCTION, INTERJECTION, OTHER (сейчас в нём отсутствуют ADVERB, PARTICLE,
CONJUNCTION, INTERJECTION, OTHER — из-за этого пробела текущий safeEnum()
в VerseAnalysisSaver молча обнулял бы такие значения от LLM).

## 3. Изменение LLM tool-схемы (задача Агенту 2)

Оба места, где объявлена JSON Schema для `submit_verse_analysis.words[]`
(`LlmToolSchemaBuilder` — реальная схема, отправляемая модели, и
`JsonSchemas.verseAnalysisSchema` — валидатор ответа, сейчас не подключён,
но должен остаться консистентным) обновляются по единому списку полей,
согласно новому §4 промпта `prompts/verse-analysis.md` (прислан
оркестратору целиком, заменяет прежний абзац "4. words — ..."):
position, surfaceIast, surfaceDevanagari, lemmaIast, root, stem, pos,
formType, isFinite, morphology (вложенный объект: person, number, case,
gender, tense, mood, voice — так LLM удобнее возвращать, backend уже
раскладывает это в отдельные Java-объекты VerseWordMorphology/
VerseWordDerivation при сохранении, схема БД от формы ответа LLM не
зависит), derivationType, derivationalSuffix, derivationalBase, derivation
(вложенный объект-дубль с description), lemmaGlossRu, lemmaGlossEn,
glossRu/glossEn (маппятся в contextGlossRu/contextGlossEn), 
formationRuleNumbers, analysisConfidence, ambiguityNotes.
Обязательные (required) поля тула — как раньше: position, surfaceIast,
surfaceDevanagari, lemmaIast, stem, pos, glossRu, glossEn — плюс новые
обязательные: formType, analysisConfidence (модель обязана явно оценить
уверенность и тип формы для каждого слова, остальные поля допускают null).

## 4. VerseAnalysisSaver (задача Агенту 2)

Метод buildWords() при разборе tool_call дополнительно:
парсит вложенный объект `morphology` из JSON-ответа и, если хотя бы одно
поле в нём не null, создаёт VerseWordMorphology и связывает с VerseWord
(cascade ALL на стороне VerseWord.morphology, orphanRemoval — при
пересоздании слов стиха старые строки в дочерних таблицах удаляются
автоматически через deleteAllByVerseId + orphanRemoval, отдельный
DELETE по дочерним таблицам не нужен).
парсит derivationType/derivationalSuffix/derivationalBase (плоские поля)
и derivation.description (вложенный объект) — если хотя бы одно из них не
null, создаёт VerseWordDerivation аналогично.
маппит glossRu/glossEn из ответа LLM в contextGlossRu/contextGlossEn
сущности VerseWord (переименование поля — не полей в JSON tool-схеме,
которая может сохранить прежние имена glossRu/glossEn для краткости
промпта, либо тоже переименовать в contextGlossRu/contextGlossEn — решает
Агент 2 по месту, главное единообразие между промптом, tool-схемой и
парсингом).

## 5. VersePage — раскрывающиеся строки (задача Агенту 3)

Таблица `words[]` под стихом остаётся как сейчас (surfaceIast/surfaceDevanagari,
краткая морфология, contextGlossRu/En, formationRuleNumbers) — новые колонки
не добавляются (перегружает таблицу). Вместо этого:
клик по строке слова разворачивает панель с полным разбором: surfaceDevanagari,
lemmaIast, root, stem, pos, formType, isFinite, вся морфология из
`morphology` (caseType, gender, numberType, person, tense, mood, voice —
показывать только заполненные поля, null не рендерить как пустую строку),
вся деривация из `derivation` (derivationType, derivationalSuffix,
derivationalBase, description), lemmaGlossRu/En рядом с contextGlossRu/En
(двумя подписанными строками, чтобы визуально не путать словарное и
контекстное значение — см. sangraha-service.md §1, разграничение важно).
analysisConfidence и ambiguityNotes — присутствуют в VerseWordDto (см.
sangraha-schemas.yaml), но **не отображаются** на VersePage в этой
итерации (поля добавлены только для тестирования бэкенда) — открытый
вопрос на будущее: показывать ли ambiguityNotes как тултип у слов с
LOW/MEDIUM confidence.

## 6. Definition of Done (доп. к общему, см. samskrtam-agents-spec.md)

Единая миграция V1 применяется на чистой БД без ошибок (droppable/
recreatable — проект без прод-данных).
Round-trip: LLM tool_call → VerseAnalysisSaver → VerseWordRepository →
VerseMapper.toWordDto → VerseWordDto — покрыт интеграционным тестом
(Агент 4) хотя бы на одном примере вроде vaśīkṛtya (ABSOLUTIVE, root kṛ)
из промпта.
