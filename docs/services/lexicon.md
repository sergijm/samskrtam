# curriculum-service: домен lexicon — доменная модель лексики

> Домен: Lexeme, таксономии (frequency/semantic/POS/morphology), Source/occurrences,
> UserCollection, UserLexemeProgress
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/curriculum-service` (тот же сервис, что и Topic/TopicPrerequisite/
> LearningLevel/ComplexQuiz, см. `curriculum-service.md` — **не отдельный сервис**,
> решение пересмотрено после первой версии, см. §0 ниже)
> Порт: `8091` (общий с остальным curriculum-service)
> Схема БД: `curriculum` (общая с Topic/ComplexQuiz — не отдельная схема `lexicon`)
> Пакет: `sm.selflearn.samskrtam.curriculum.lexicon` (под-пакет)
> Status: **DRAFT**

> Связанные файлы: [lexical-curriculum.md](./lexical-curriculum.md) (LexicalTopic, 50–80
> тем, семантическая/POS/морфологическая таксономия — наполнение),
> [lexical-quizzes.md](./lexical-quizzes.md) (типы квизов, adaptive selection),
> [lexicon-content-pipeline.md](./lexicon-content-pipeline.md) (наполнение 2000 слов),
> [curriculum-service.md](./curriculum-service.md) (интеграция, см. §0 ниже),
> [curriculum-service.md §11](./curriculum-service.md#11-internal-rest-приём-словаря-из-sangraha-service),
> [dictionary-service.md](./dictionary-service.md), [sangraha-service.md](./sangraha-service.md).

---

## 0. Что уже есть в проекте, почему этого недостаточно, и почему это модуль curriculum-service, а не отдельный сервис

Перед проектированием проверено пересечение с тремя существующими моделями:

1. **`dictionary-service`** (`d_mw`/`d_fri` схемы) — это read-only зеркало внешних
   словарей (Monier-Williams, Frisch) для поиска словарных статей по клику. Он не
   хранит учебные метаданные (частотность, тематику, прогресс) и не должен — этот
   домен со словарями не конкурирует и в этой итерации **не используется** как
   источник наполнения (решение: единственный источник 2000 лемм — корпус
   sangraha-service, см. §0 п.2 и `lexicon-content-pipeline.md` §1; привлечение
   dictionary-service как доп. источника `meaning`/`gender` — возможное будущее
   расширение, не в периметре текущей итерации).

2. **`curriculum-service.VocabularyWord`/`VocabularyCategory`** (§39, §296–300
   `curriculum-service.md`) — это уже существующая, но **связанная контрактом с
   `Quiz`/`LessonType.VOCABULARY`** модель: слово физически привязано к
   flashcard-квизу (изначально `quizId`, по факту — join с `Quiz` через
   `VocabularyWordQuiz`, что в `curriculum-service.md` зафиксировано как
   расхождение) и к иерархической `VocabularyCategory` (work → chapter → verse,
   создаётся синком из sangraha-service). Это **валидный, работающий** механизм
   для конкретного сценария «слова этого стиха», но:
   - не поддерживает многомерные таксономии (frequency/POS/morphology) как
     независимые измерения;
   - не различает `Lexeme` и `WordForm` (хранит уже готовую пару IAST+deva одной
     словоформы, а не лемму отдельно);
   - не имеет `UserCollection`/`UserLexemeProgress`;
   - жёстко завязан на генерацию flashcard-сессий через `quiz-service` per-`Quiz`.

   **Решение:** не переписывать `curriculum-service.VocabularyWord` немедленно —
   таблицы §1–§6 ниже становятся новым источником истины для *учебной* лексики
   (2000 лемм + таксономии + progress), но живут физически в curriculum-service.
   Существующий sangraha→curriculum-service поток
   (`POST /content/internal/sangraha/vocabulary-quiz`) в этой итерации **не
   трогается** — он продолжает создавать per-verse VOCABULARY-квизы как раньше.
   Точка интеграции на будущее: этот internal-эндпоинт можно переключить (или
   продублировать) на curriculum-service, чтобы `SourceOccurrence` (§5) заполнялся
   из тех же данных, которые сейчас улетают в `curriculum-service` — это отдельная
   миграционная задача, не в периметре текущей.

3. **`curriculum-service.Topic`** (см. `curriculum-service.md`, первая версия
   этого документа) — уже содержит граф prerequisite, `learningLevel`
   (`L0`…`L6`) и `ComplexQuiz`. Изначально (первая версия этого документа)
   лексика проектировалась как отдельный сервис `lexicon-service` (свой порт,
   своя схема), интегрированный с curriculum-service только по значению id
   (`topicId`, без физического FK — по аналогии с `LearningMaterial`). **Решение
   пересмотрено:** лексика реализуется как модуль (под-пакет + набор таблиц той
   же схемы `curriculum`) **внутри curriculum-service**, не отдельный сервис.

   Причины пересмотра (в пользу заказчика, не автоматически «лучше» —
   зафиксировано как явный компромисс):
   - `Lexeme ↔ Topic` — по факту очень тесная связь (`lexical_topic_binding`,
     §1 `lexical-curriculum.md`), и в рамках одной БД она становится обычным FK
     с гарантией целостности на уровне БД, а не «ссылкой по значению», которую
     нужно было бы вручную защищать от рассинхронизации между сервисами;
   - объём операционных издержек нового сервиса (Dockerfile, NetworkPolicy,
     Gateway-роут, CI-джоб — см. `curriculum-service.md` §8, тот же список, что
     обсуждался при выделении curriculum-service из curriculum-service) для
     обоснованно самостоятельного сервиса — оправдан, а плодить второй такой же
     сервис через несколько дней после первого, для тесно связанного домена —
     нет;
   - за счёт этого решения `ComplexQuiz`/`VocabularyQuizDefinition` (§2
     `lexical-quizzes.md`) тоже становятся проще: один и тот же
     `curriculum.complex_quiz.id` используется и для чисто grammar-, и для
     lexical-, и для смешанных (grammar+lexical) интегрированных практик без
     каких-либо межсервисных вызовов.

   Цена компромисса (сознательно принимается): схема `curriculum` перестаёт
   быть «маленькой» (2 таблицы → ~15), домен сервиса расширяется с «учебный
   план» на «учебный план + лексика» — это уже не идеально узкий bounded
   context, но задача явно требует «не создавать изолированную архитектуру» и
   пользователь подтвердил объединение — see git history обсуждения.

---

## 1. Lexeme — словарная единица

Таблица `curriculum.lexeme`:
id (UUID, PK)
lemmaIast (VARCHAR 100, NOT NULL)
lemmaDevanagari (VARCHAR 100, NOT NULL)
lemmaSlp1 (VARCHAR 100, NOT NULL — нормализованная форма для поиска/дедупа, тот же принцип, что `slp1Normalized` в dictionary-service)
glossRu / glossEn (VARCHAR 300, NOT NULL — короткое значение для flashcard/quiz, не энциклопедическая статья)
longDefinitionRu / longDefinitionEn (TEXT, NULL — более полное объяснение для `LearningMaterial`, не для quiz-вопроса)
gender (VARCHAR 20, NULL — `MASCULINE`|`FEMININE`|`NEUTER`|`UNSPECIFIED`; осмысленно только для именных лексем)
status (VARCHAR 20, NOT NULL, DEFAULT `DRAFT` — `DRAFT`|`CANDIDATE`|`APPROVED`|`REJECTED`;
`CANDIDATE` = импортирована из sangraha-corpus, поля заполнены **эвристиками**, без
LLM (решение по задаче: без AI-enrichment), ожидает ручного ADMIN-review — см.
`lexicon-content-pipeline.md` §2–§3)
createdAt / updatedAt (TIMESTAMPTZ, NOT NULL)

**Уникальность/дедуп:** `UNIQUE(lemmaSlp1, gender)` — умышленно не просто по
`lemmaSlp1`, т.к. омонимы с разным родом (редко, но встречается) — разные лексемы;
омонимы с одинаковым родом и разным значением в этой версии **не разделяются**
(упрощение — см. открытые вопросы `lexicon-content-pipeline.md` §5).

**Явно НЕ на Lexeme:** `frequencyRank`, `semanticTopic`, `pos`, `morphologyClass`,
`source` — всё это отдельные таблицы-связи (§2–§5), не колонки, ровно по
требованию «не превращать в взаимоисключающие категории».

---

## 2. WordForm — конкретная словоформа

Таблица `curriculum.word_form`:
id (UUID, PK)
lexemeId (UUID, FK → lexeme.id, ON DELETE CASCADE)
formIast / formDevanagari (VARCHAR 100, NOT NULL)
grammaticalNote (VARCHAR 200, NULL — свободный текст типа "3rd sg. present", не
структурированный enum — структурная грамматика словоформ (падеж/число/лицо) уже
исчерпывающе генерируется curriculum-service `declension_forms`/`conjugation`
таблицами для грамматического квиза; здесь `WordForm` нужен **только** для показа
реально атрестованной формы в контекстном lexical-квизе, не для генерации парадигм)
sourceOccurrenceId (UUID, FK → source_occurrence.id, NULL — если форма взята из
конкретного вхождения в текст)
createdAt (TIMESTAMPTZ, NOT NULL)

Не каждая Lexeme обязана иметь WordForm — для большинства из 2000 базовых лемм
на старте будет только сама лемма (§1), `WordForm` наполняется постепенно по мере
разбора текстов sangraha-service (§5) и не блокирует использование леммы в
quiz'ах (recognition/recall квизы работают с леммой напрямую, contextual —
предпочтительно с `WordForm`, см. `lexical-quizzes.md` §2).

---

## 3. Таксономии как связи, не поля

### 3.1 Frequency

Таблица `curriculum.lexeme_frequency`:
lexemeId (UUID, FK → lexeme.id, ON DELETE CASCADE)
source (VARCHAR 50, NOT NULL — например `CURATED_2000`, на будущее — другие
корпуса частотности)
rank (INTEGER, NOT NULL)
PRIMARY KEY (lexemeId, source)

Сделано отдельной таблицей (1 лексема — потенциально несколько источников
частотности), а не колонками на `Lexeme`, чтобы не блокировать появление второго
корпуса (например, частотность по конкретному произведению, в отличие от общей
учебной частотности) без миграции схемы `Lexeme`.

`band` (частотная полоса, `Top100`/`Top250`/…) **не хранится** как отдельное
значение на строке — вычисляется на чтение по `rank` и справочнику диапазонов
`curriculum.frequency_band` (§ниже), чтобы изменение границ полос не требовало
перезаписи 2000 строк. Обоснование конкретной разбивки границ — в
`lexical-curriculum.md` §2.

Справочник `curriculum.frequency_band` (редко меняется, не растёт пользователем):
code (VARCHAR 20, PK), minRank (INT, NOT NULL), maxRank (INT, NOT NULL),
labelRu / labelEn (VARCHAR 60, NOT NULL), sortOrder (SMALLINT, NOT NULL)

### 3.2 Semantic taxonomy

Таблица `curriculum.semantic_topic` (иерархический справочник, ~30–50 строк,
наполнение — `lexical-curriculum.md` §3):
id (UUID, PK), code (VARCHAR 60, UNIQUE), nameRu / nameEn (VARCHAR 100, NOT NULL),
parentId (UUID, FK → semantic_topic.id, NULL — корневые категории типа `Nature`,
`People`, `Everyday life`, `Abstract`)

Таблица `curriculum.lexeme_semantic_topic` (M:N, ключевое требование §4 задачи):
lexemeId (UUID, FK), semanticTopicId (UUID, FK), PRIMARY KEY (lexemeId, semanticTopicId)

Лексема может относиться к любому числу узлов дерева, включая узлы на разных
уровнях иерархии (и к листу, и к его родителю одновременно, если это осмысленно —
не enforced запретом).

### 3.3 Part of speech

Таблица `curriculum.part_of_speech` (фиксированный небольшой справочник, не
растущий, ~15 строк, см. полный список в `lexical-curriculum.md` §4):
code (VARCHAR 20, PK), group (VARCHAR 20, NOT NULL — `NOMINAL`|`VERBAL`|`INDECLINABLE`),
nameRu / nameEn (VARCHAR 60, NOT NULL)

Таблица `curriculum.lexeme_pos` (M:N):
lexemeId (UUID, FK), posCode (VARCHAR 20, FK → part_of_speech.code),
PRIMARY KEY (lexemeId, posCode)

На практике у подавляющего большинства лексем — ровно одна строка; M:N оставлена
для случаев реальной полифункциональности (например, причастие, функционирующее
и как `participle`, и как самостоятельное `adjective` в словарном значении).

### 3.4 Morphology taxonomy

Таблица `curriculum.morphology_class` (фиксированный справочник, ~20 строк —
`a-stem`, `ā-stem`, `i-stem`, `u-stem`, `ṛ-stem`, `irregular` для именных;
`class-1`…`class-10` для глагольных; полный список — `lexical-curriculum.md` §5):
code (VARCHAR 20, PK), appliesTo (VARCHAR 10, NOT NULL — `NOUN`|`VERB`),
nameRu / nameEn (VARCHAR 60, NOT NULL)

Таблица `curriculum.lexeme_morphology` (M:N):
lexemeId (UUID, FK), morphologyClassCode (VARCHAR 20, FK), PRIMARY KEY (lexemeId, morphologyClassCode)

**Мост к grammar-curriculum:** `morphology_class.code` — это то же смысловое
пространство, что `curriculum-service` использует для grammar-тем (например,
Topic `a-stem-masculine` в grammar-curriculum и `morphologyClassCode = a-stem` в
lexicon — независимые сущности, но с совпадающим словарём кодов, специально для
того, чтобы можно было построить связку «эта лексика иллюстрирует эту
грамматическую тему» на уровне API-агрегации фронтенда, без FK между сервисами).

---

## 4. Source / SourceOccurrence

Таблица `curriculum.source`:
id (UUID, PK), code (VARCHAR 60, UNIQUE — slug, например `bhagavad-gita`),
titleRu / titleEn (VARCHAR 200, NOT NULL), kind (VARCHAR 20, NOT NULL —
`EPIC`|`PHILOSOPHICAL`|`FABLE`|`OTHER`), totalOccurrencesCache (INTEGER, NOT NULL,
DEFAULT 0), uniqueLemmaCountCache (INTEGER, NOT NULL, DEFAULT 0),
externalSangrahaWorkSlug (VARCHAR 100, NULL — если источник синхронизирован из
sangraha-service, см. §0 п.2), createdAt / updatedAt

Таблица `curriculum.source_occurrence`:
id (UUID, PK), sourceId (UUID, FK → source.id, ON DELETE CASCADE), lexemeId (UUID,
FK → lexeme.id, ON DELETE CASCADE), locationRef (VARCHAR 100, NOT NULL — свободная
ссылка на место, например `"1.2.3"` — chapter.section.verse, или sangraha
`verseId`), surfaceFormIast (VARCHAR 100, NOT NULL — реально встреченная
словоформа), createdAt (TIMESTAMPTZ, NOT NULL)

`totalOccurrencesCache`/`uniqueLemmaCountCache` — денормализованный кэш
(`COUNT(*)`/`COUNT(DISTINCT lexemeId)` по `source_occurrence`), пересчитывается
после батч-загрузки occurrences, не на каждую вставку (для источников с тысячами
occurrences пересчёт на каждый insert избыточен). Именно эти два числа и есть
пример из задачи: «Pañcatantra — 1240 occurrences, 620 unique lemmas».

---

## 5. UserCollection / UserCollectionItem

Таблица `curriculum.user_collection`:
id (UUID, PK), ownerId (UUID, NOT NULL — из `X-User-Id`), name (VARCHAR 100,
NOT NULL — пользовательское, не переводится), description (TEXT, NULL),
visibility (VARCHAR 10, NOT NULL, DEFAULT `PRIVATE` — `PRIVATE`|`SHARED`),
createdAt / updatedAt

Таблица `curriculum.user_collection_item`:
collectionId (UUID, FK → user_collection.id, ON DELETE CASCADE), lexemeId (UUID,
FK → lexeme.id, ON DELETE CASCADE), addedVia (VARCHAR 20, NOT NULL —
`MANUAL`|`DICTIONARY_SEARCH`|`TEXT_READING`|`QUIZ_RESULT`|`LEARNING_RESULT`),
addedAt (TIMESTAMPTZ, NOT NULL), PRIMARY KEY (collectionId, lexemeId)

Коллекции **не участвуют** в глобальных таксономиях (§3) — это личное
пространство пользователя, не видимое в `semantic_topic`/`frequency_band`/т.д.
`addedVia = QUIZ_RESULT`/`LEARNING_RESULT` предполагает вызов из quiz-service
(«добавить в Difficult words» по кнопке на экране результатов) — сам вызов, это
`POST /lexicon/collections/{id}/items` от лица пользователя, не отдельный
внутренний контракт.

---

## 6. UserLexemeProgress

Таблица `curriculum.user_lexeme_progress`:
userId (UUID, NOT NULL), lexemeId (UUID, FK → curriculum.lexeme.id, ON DELETE CASCADE),
masteryScore (SMALLINT, NOT NULL, DEFAULT 0 — 0–100), exposureCount (INTEGER,
NOT NULL, DEFAULT 0), correctCount (INTEGER, NOT NULL, DEFAULT 0), incorrectCount
(INTEGER, NOT NULL, DEFAULT 0), lastSeenAt (TIMESTAMPTZ, NULL), nextReviewAt
(TIMESTAMPTZ, NULL), createdAt / updatedAt, PRIMARY KEY (userId, lexemeId)

Формула обновления `masteryScore`/`nextReviewAt` при ответе — та же
spaced-repetition логика, что уже применяется для грамматических item-score в
quiz-service (`ADR-007`, статусы `NEW`/`LEARNING`/`MASTERED` + ортогональный `DIFFICULT`,
срезы сессий через `progressTagSetId`, см.
`quiz-sessions.yaml` `ProgressTagSetIdParam`) — переиспользуется алгоритм, не код (эта
таблица физически в curriculum-service, не в `quiz-service`, т.к. принадлежит
лексическому домену, а не сессиям конкретного квиза — то же разделение
ответственности, что и у `quiz_item_score`/`grammar_form_score` в quiz-service
относительно curriculum-service). Детали пересчёта — при реализации, вне периметра
этого документа (ссылка на ADR-007 как источник формулы). **Явно не смешивается
с таксономией** — ни одна из таблиц §3 не содержит `userId`, ни одна строка
`user_lexeme_progress` не содержит частоты/темы/POS.

---

## 7. Сводная схема связей (без API — см. lexical-quizzes.md)

Lexeme (1) ── (N) WordForm
Lexeme (N) ── (N) SemanticTopic   [lexeme_semantic_topic]
Lexeme (N) ── (N) PartOfSpeech    [lexeme_pos]
Lexeme (N) ── (N) MorphologyClass [lexeme_morphology]
Lexeme (1) ── (N) LexemeFrequency [по одному на каждый frequency source]
Lexeme (N) ── (N) Source          [через source_occurrence, с деталями occurrence]
Lexeme (N) ── (N) UserCollection  [user_collection_item]
Lexeme (1) ── (N) UserLexemeProgress [по одному на каждого пользователя]
Lexeme (N) ── (N) Topic (`domain=LEXICON`) [lexical_topic_binding, см. lexical-curriculum.md §1 — теперь обычный FK на `curriculum.topic.id` в той же БД, не ссылка по значению]

Ни одна из этих связей не создаёт копию `Lexeme` — ровно требование §18/§23
задачи: `गजः` — одна строка в `curriculum.lexeme`, всё остальное — рёбра графа.
