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

---

## 4. Flyway Migrations

6 миграций Flyway: V1 — schema content; V2 — таблица quizzes; V3 — questions; V4 — question_options (FK на correct_option_id после создания); V5 — vocabulary_words; V6 — seed начальных VOCABULARY квизов (animals, numbers, body-parts, nature, 1, 2).

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

```
GET /api/v1/content/quizzes/{id}/session-data           → всё необходимое для старта сессии
```

Ответ: { quizId, lessonType, questionsPerSession, questions[{id, textRu, textEn, explanationRu, explanationEn, correctOptionId, options[{id, textRu, textEn}]}], vocabularyWords (null для не-VOCABULARY) }

---

## 6. Backend структура

Пакет `controller/`: LessonController, QuestionController, VocabularyController, internal/SessionDataController.
Пакет `service/`: LessonService, QuestionService, VocabularyService.
Пакет `repository/`: LessonRepository, QuestionRepository, QuestionOptionRepository, VocabularyWordRepository.
Пакет `model/`: Quiz, Question, QuestionOption, VocabularyWord, LessonType, Difficulty.
Пакет `dto/`: CreateQuizRequest, CreateQuestionRequest, QuizDetailResponse, SessionDataResponse, VocabularyWordRequest.

---

## 7. application.yml

Порт 8081, virtual threads enabled, datasource через env, ddl-auto: validate, default_schema: content, flyway schemas: content.

---

## 8. Acceptance Criteria

- [ ] Только ADMIN получает доступ к write-операциям (403 для STUDENT)
- [ ] `GET /session-data` доступен без роли ADMIN (для quiz-service)
- [ ] Нельзя сохранить вопрос без ровно 1 правильного варианта
- [ ] Удаление квиза и вопроса — soft delete
- [ ] `vocabulary_words` возвращаются только для квизов с `quiz_type = VOCABULARY`
- [ ] Slug уникален и соответствует паттерну `^[a-z0-9][a-z0-9-]*$`

---

## 9. Открытые вопросы

- [ ] Импорт вопросов и слов из CSV для массового добавления?
- [ ] Кэшировать ли session-data в quiz-service (Redis)?

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
