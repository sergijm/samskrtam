# content-service

> Домен: Lesson Content — настройки и содержание уроков (см. ADR-002)
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/content-service`
> Порт: 8081
> Status: **DRAFT**

---

## 1. Описание

Хранит **настройки и содержание всех квизов**: метаданные квизов (тип, сложность, slug), вопросы, варианты ответов, а также лексику для словарных квизов. Доступен только для роли `ADMIN` (запись) и внутренне для `quiz-service` (чтение). Virtual Threads позволяют использовать обычный JPA/JDBC без WebFlux.

Разделение ответственности:
- **content-service** — что есть в квизах (данные, настройки)
- **quiz-service** — как пользователь их проходит (сессии, ответы, события)

---

## 2. Типы квизов

| LessonType | Описание |
|---|---|
| `DECLENSIONS` | Квиз по падежным формам санскрита |
| `CONJUGATIONS` | Квиз по спряжениям глаголов |
| `VOCABULARY` | Квиз по лексике (slug-based) |

---

## 3. Сущности

**Quiz** (таблица quizzes): id (UUID), slug (string, unique), titleRu, titleEn, descriptionRu, descriptionEn, lessonType (DECLENSIONS|CONJUGATIONS|VOCABULARY), difficulty (BEGINNER|INTERMEDIATE|ADVANCED), questionsPerSession (int, default 10), createdAt, deletedAt

**Question** (таблица questions): id (UUID), quizId (UUID), textRu (TEXT), textEn (TEXT), explanationRu (TEXT), explanationEn (TEXT), correctOptionId (UUID), deletedAt

**QuestionOption** (таблица question_options): id (UUID), questionId (UUID), textRu, textEn

**VocabularyWord** (таблица vocabulary_words, только для VOCABULARY квизов): id (UUID), quizId (UUID), word (IAST), wordDevanagari, translationRu, translationEn, partOfSpeech, example

**DeclensionStem** (таблица `content.declension_stems`, для DECLENSIONS квизов; **отсутствовала в этом документе — добавлено**): id (UUID), stemIast, stemDevanagari (колонка существует в БД, но не заполняется миграцией-сидом V2 — данных нет), vowelType (A_STEM|AA_STEM|I_STEM|II_STEM|U_STEM|UU_STEM|R_STEM), gender.
**NEW (задача Агенту 2, см. ниже):** добавить `translationRu`, `translationEn` — сейчас перевода основы нет вообще ни в БД, ни в entity.

**DeclensionForm** (таблица `content.declension_forms`): PK (declensionStemId, caseType, numberType), formIast, formDevanagari — уже заполнены (сид из `raw_data.sanskrit_declensions_enriched`).

---

## 4. Flyway Migrations

6 миграций Flyway: V1 — schema content; V2 — таблица quizzes; V3 — questions; V4 — question_options (FK на correct_option_id после создания); V5 — vocabulary_words; V6 — seed начальных VOCABULARY квизов (animals, numbers, body-parts, nature, 1, 2).

`declension_stems`/`declension_forms` создаются и заполняются отдельно, в `V2__init_grammar_quizzes.sql` (сид из `raw_data.sanskrit_declensions_enriched`), не входят в перечисление выше — **несоответствие в этом документе, зафиксировано, не исправляется в рамках текущей задачи**.

**NEW, требуется новая миграция (Агент 2):** `ALTER TABLE content.declension_stems ADD COLUMN translation_ru VARCHAR(255), ADD COLUMN translation_en VARCHAR(255);` + data-fix UPDATE для заполнения `stem_devanagari` и новых `translation_ru/en` по всем текущим строкам таблицы — данные предоставляет пользователь (см. §9).

**NEW, требуется миграция на удаление (Агент 2):** `DROP TABLE content.generated_questions; DROP TABLE content.generated_quiz_data;` — таблицы persist-хранения сгенерированных вопросов сессии удаляются, т.к. это дублировало `quiz.session_questions` (см. §3а и quiz-service.md §12). Порядок важен — сначала дочерняя таблица (FK на generated_quiz_data_id).

---

## 5. API


> Полная OpenAPI спецификация для Lesson Pages: [lesson-openapi.yaml](../openapi/lesson-openapi.yaml)

### Управление уроками (ADMIN)

```
GET    /api/v1/content/quizzes                          → список уроков (фильтр по lessonType)
GET    /api/v1/content/quizzes/{id}                     → детали квиза
POST   /api/v1/content/quizzes                          → создать квиз
PUT    /api/v1/content/quizzes/{id}                     → обновить квиз
DELETE /api/v1/content/quizzes/{id}                     → soft delete

POST   /api/v1/content/quizzes/{id}/questions           → добавить вопрос
GET    /api/v1/content/quizzes/{id}/questions           → вопросы квиза (с вариантами)
PUT    /api/v1/content/questions/{id}                   → обновить вопрос
DELETE /api/v1/content/questions/{id}                   → soft delete
PUT    /api/v1/content/questions/{id}/correct-option    → задать правильный вариант

POST   /api/v1/content/quizzes/{id}/vocabulary          → добавить слово (только VOCABULARY квизы)
PUT    /api/v1/content/vocabulary/{wordId}              → обновить слово
DELETE /api/v1/content/vocabulary/{wordId}              → удалить слово
```

### Внутреннее API для quiz-service

> **ИЗМЕНЕНО (архитектурное решение):** ранее `generate-quiz-data` генерировал вопросы **и**
> сохранял их в content-service (`content.generated_quiz_data`/`generated_questions`), а
> quiz-service на resume/answer/complete перезапрашивал их обратно по id
> (`GET /generated-quiz-data/{id}`, `GET /generated-questions/{id}`). Решено: это дублирование
> не нужно, т.к. quiz-service всё равно обязан хранить сгенерированные вопросы у себя (для
> SQL-статистики/истории, `quiz.session_questions`, см. quiz-service.md §12). Поэтому
> content-service теперь **не хранит ничего** — генерирует и сразу возвращает результат.
> Единственный вызываемый quiz-service эндпоинт для генерации вопросов сессии:

```
POST /api/v1/content/lessons/{quizId}/generate-quiz-data   → генерирует вопросы сессии и
                                                               ВОЗВРАЩАЕТ их, ничего не
                                                               сохраняя; вызывается один раз,
                                                               на старте сессии
```

Эндпоинты `GET /generated-quiz-data/{id}` и `GET /generated-questions/{questionId}` —
**удалены** вместе с их персистентным слоем (см. §3а). quiz-service самостоятельно хранит
результат этого вызова в `quiz.session_questions` и оттуда же читает на resume/answer/complete
(см. quiz-service.md §3, §5, §12) — content-service для этого больше не нужен.

Ответ `generate-quiz-data` — `GeneratedQuizData`: `{ lessonId, lessonType,
questionsPerSession, generatedQuestions[...], vocabularyWords (null для не-VOCABULARY) }`.
Поле `generatedQuizDataId` в DTO больше не нужно как внешний идентификатор для повторного
запроса к content-service — quiz-service при желании может сохранить какой-то свой
внутренний group-id, но это уже его внутреннее дело (см. quiz-service.md §12).

Для DECLENSIONS/CONJUGATIONS `generatedQuestions[]` — это `GeneratedQuizQuestionDto`:
`{id, quizId, questionNumber, text, explanationRu, explanationEn,
declensionStemId, targetCase, targetNumber, correctFormIast, correctFormDevanagari,
vocabularyWordId, questionSourceLanguage, questionTargetLanguage, correctTranslationRu,
correctTranslationEn, userLocale, stem, caseType, numberType, gender}`.

**NEW (задача Агенту 2):** добавить в `QuestionResponse`/`GeneratedQuizQuestionDto` поля
`stemDevanagari`, `stemTranslationRu`, `stemTranslationEn`, заполняемые из
`DeclensionStem.stemDevanagari/translationRu/translationEn` в
`DeclensionQuizGeneratorService.generateSingleQuestion(...)`.

**Дистракторы (варианты ответа) НЕ входят в этот ответ и не хранятся здесь** — они
генерируются в quiz-service на лету при каждом обращении к вопросу, в т.ч. на resume, для чего
content-service всё равно продолжает быть нужен через отдельный, независимый от этого,
эндпоинт `getDeclensionForms` (см. quiz-service.md §5а) — то есть **полной развязки
quiz-service от content-service на resume нет**, изменилось только то, что именно
content-service отдаёт по запросу.

---

## 3а. Генерация вопросов сессии — теперь без сохранения (было пропущено в этом документе)

Ранее в этом разделе описывались персистентные таблицы `content.generated_quiz_data`/
`content.generated_questions`. **Они удалены** вместе с `GeneratedQuizDataRecordRepository`,
`GeneratedQuestionRepository` и entity `GeneratedQuizDataRecord`/`GeneratedQuestion` — решено,
что per-session сгенерированный вопрос хранит только quiz-service (`quiz.session_questions`,
см. quiz-service.md §12), а content-service — чистый генератор без побочных эффектов записи.

`DeclensionQuizGeneratorService`/`QuestionGenerationService` остаются, но их персистентная
часть (`GenerateQuizService` в части сохранения) удаляется — они теперь только строят и
возвращают `List<QuestionResponse>`/`GeneratedQuizData`, ничего не пишут в БД
content-service.

---

## 6. Backend структура

Пакет `controller/`: LessonController, QuestionController, VocabularyController, LessonContentController (только `generate-quiz-data` — единственный внутренний эндпоинт для quiz-service; ранее здесь ошибочно упоминался несуществующий `internal/SessionDataController`, а также два уже удалённых read-эндпоинта, см. §5).
Пакет `service/`: LessonService, QuestionService, VocabularyService, DeclensionQuizGeneratorService, QuestionGenerationService (только генерация, персистентная часть удалена, см. §3а).
Пакет `repository/`: LessonRepository, QuestionRepository, QuestionOptionRepository, VocabularyWordRepository, DeclensionStemRepository, DeclensionFormRepository (`GeneratedQuizDataRecordRepository`/`GeneratedQuestionRepository` — удалены, см. §3а).
Пакет `model/`: Quiz, Question, QuestionOption, VocabularyWord, LessonType, Difficulty, DeclensionStem, DeclensionForm (`GeneratedQuizDataRecord`/`GeneratedQuestion` — удалены, см. §3а).
Пакет `dto/`: CreateQuizRequest, CreateQuestionRequest, QuizDetailResponse, VocabularyWordRequest, QuestionResponse, GeneratedQuizData, GeneratedQuizQuestionDto, DeclensionStemDto, DeclensionFormDto (DTO остаются — это транспортный формат ответа `generate-quiz-data`, просто больше не persist-ится).

---

## 7. application.yml

Порт 8081, virtual threads enabled, datasource через env, ddl-auto: validate, default_schema: content, flyway schemas: content.

---

## 8. Acceptance Criteria

- [ ] Только ADMIN получает доступ к write-операциям (403 для STUDENT)
- [ ] `generate-quiz-data` доступен без роли ADMIN (для quiz-service) — единственный внутренний эндпоинт, не пишет в БД
- [ ] Нельзя сохранить вопрос без ровно 1 правильного варианта
- [ ] Удаление квиза и вопроса — soft delete
- [ ] `vocabulary_words` возвращаются только для квизов с `quiz_type = VOCABULARY`
- [ ] Slug уникален и соответствует паттерну `^[a-z0-9][a-z0-9-]*$`

---

## 9. Открытые вопросы

- [ ] Импорт вопросов и слов из CSV для массового добавления?
- [ ] Кэшировать ли ответ `generate-quiz-data`/данные для дистракторов (`getDeclensionForms`) — актуально только для распределения нагрузки на content-service при большом числе одновременных `start`/`resume`; ответ `generate-quiz-data` теперь не переиспользуется (вызывается один раз на старте, дальше quiz-service хранит сам). Отложено до появления реальной нагрузки (см. quiz-service.md §3).

---

## 10. Домен Eamenau

Модуль упражнений по правилам сандхи санскрита. Полная спецификация: [services/eamenau.md](./eamenau.md).

Структура: модели (13 классов) в eamenau/model/, репозитории (12 интерфейсов) в eamenau/repository/, сервисы (EamenauService, EamenauExerciseService), контроллеры (EamenauController, EamenauExerciseController). Shared DTOs — в shared/samskrtam-dtos. Миграция: V2 — schema eamenau. Фронтенд: pages/eamenau/, components/eamenau/.

**Endpoints:** GET/PUT /api/v1/eamenau/sandhi-rules, exercises, solutions (ADMIN).

**Известные проблемы:** PUT /solutions/{id} без авторизации, Answer не используется, Phoneme без API.

---

## 11. Kafka Consumer: sangraha-vocabulary-events

**Контекст:** `sangraha-service` (см. [services/sangraha-service.md](./sangraha-service.md), ADR-006 в `docs/conventions.md`) анализирует санскритские стихи через LLM и на каждый проанализированный стих публикует извлечённые слова. `content-service` — единственный текущий consumer; это **первый `@KafkaListener` в проекте** (до сих пор все сервисы только продюсили события через Outbox).

```
topic: sangraha-vocabulary-events
key:   verseId (String, UUID)
group: content-service
```

### Payload (см. точную схему в sangraha-service.md §6)

```json
{
  "eventType": "VERSE_VOCABULARY_EXTRACTED",
  "verseId": "uuid",
  "workSlug": "bhagavad-gita",
  "workTitleRu": "Бхагавад-гита", "workTitleEn": "Bhagavad Gita",
  "chapterSlug": "1",
  "chapterTitleRu": "Глава 1", "chapterTitleEn": "Chapter 1",
  "words": [
    {
      "wordIast": "dhṛtarāṣṭraḥ", "wordDevanagari": "धृतराष्ट्रः",
      "stem": "dhṛtarāṣṭra", "root": null, "gender": "MASCULINE",
      "translationRu": "Дхритараштра", "translationEn": "Dhritarashtra",
      "explanationRu": "...", "explanationEn": "..."
    }
  ]
}
```

### Обработка (`SangrahaVocabularyEventListener` → `VocabularySyncService`)

Идемпотентно (топик может redeliver-ить при ребалансе/ретрае):

1. `VocabularyCategory` root: `findByCodeIgnoreCase(workSlug)`, если нет — создать (`nameRu = workTitleRu`, `nameEn = workTitleEn`, `parentId = null`).
2. `VocabularyCategory` chapter: `findByCodeIgnoreCase("{workSlug}.{chapterSlug}")`, если нет — создать с `parentId = root.id`.
3. Для root и/или chapter-категории — `upsert Quiz(quizType = VOCABULARY, slug = code)`, если квиза с таким `slug` ещё нет (`titleRu/En` берём из `workTitleRu/En`/`chapterTitleRu/En`). Решение — заводить квиз на уровне произведения, главы или обоих — принимает Агент 2 при реализации (открытый вопрос, см. §9 sangraha-service.md).
4. Для каждого слова из `words[]`: dedup по `(wordIast, stem)` — `findByWordIastAndStem`. Если найдено — не создавать новый `VocabularyWord`; если связи `VocabularyWordCategory(wordId, chapterCategory.id)` ещё нет — создать. Если не найдено — создать `VocabularyWord` (`wordIast`, `wordDevanagari`, `stem`, `root`, `gender`, `translationRu/En`, `explanationRu/En`) и сразу связать с категорией главы.
5. Ошибки обработки события (например, невалидный payload) — не ретраятся бесконечно; после N попыток — в DLQ-топик `sangraha-vocabulary-events-dlq` (конвенция DLQ — на усмотрение Агента 5 DevOps, если Kafka error handling ещё не типизирован в проекте).

### Открытые вопросы (для Агента 2 при реализации)

- [ ] Квиз заводится на уровне работы, главы или обоих одновременно?
- [ ] Нужен ли consumer-level retry/backoff и DLQ-топик, или на этом этапе допустим simple log-and-skip?
- [ ] `gender = null` от sangraha (для indeclinable-слов) — как мапится в `VocabularyWord.gender` (там `nullable = false`)? Вероятно `UNSPECIFIED` — подтвердить при реализации.