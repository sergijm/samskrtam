# sangraha-service

> Домен: Sangraha (सङ्ग्रह — «собрание, свод») — санскритские произведения: иерархия
> книга → глава → стих, LLM-анализ стиха (транслитерация, перевод, сандхи, грамматика).
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/sangraha-service`
> Порт: `8089` (фиксирован, согласован с Агентом 5 DevOps)
> Схема БД: `sangraha`
> Status: **DRAFT**

---

## 1. Описание

Хранит санскритские тексты (произведения) в виде дерева **Work → Chapter → Verse** и
результаты их LLM-анализа: транслитерация IAST ⇄ devanagari, перевод (ru/en), разбор
сандхи, пословная грамматика.

Сервис **не хранит словарь и не ходит синхронно ни в `content-service`, ни в
`dictionary-service`**. Единственный канал наружу — Kafka: после анализа стиха
sangraha-service публикует слова этого стиха, `content-service` асинхронно строит из
них категории лексики и словарные квизы (см. §6). Сопоставление слов со словарными
статьями `dictionary-service` **в текущей итерации не делается** (см. §8).

Разделение ответственности:
- **sangraha-service** — тексты, их структура, LLM-анализ (грамматика стиха)
- **content-service** — лексика для VOCABULARY-квизов (получает слова из Kafka)
- **dictionary-service** — полный словарь (MW/Frisch), не связан с sangraha в этой итерации

---

## 2. Сущности

```java
// sm/selflearn/samskrtam/sangraha/model/Work.java
@Entity
@Table(name = "works", schema = "sangraha")
public class Work {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;                 // "bhagavad-gita" — используется как код категории лексики

    @Column(name = "title_ru", nullable = false)
    private String titleRu;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "description_ru")
    private String descriptionRu;

    @Column(name = "description_en")
    private String descriptionEn;

    private String author;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

// sm/selflearn/samskrtam/sangraha/model/Chapter.java
@Entity
@Table(name = "chapters", schema = "sangraha")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "work_id", nullable = false)
    private UUID workId;

    @Column(nullable = false)
    private String slug;                 // уникален в пределах work; categoryCode = "{work.slug}.{chapter.slug}"

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "title_ru", nullable = false)
    private String titleRu;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

// sm/selflearn/samskrtam/sangraha/model/Verse.java
@Entity
@Table(name = "verses", schema = "sangraha")
public class Verse {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "text_devanagari", columnDefinition = "TEXT")
    private String textDevanagari;

    @Column(name = "text_iast", columnDefinition = "TEXT")
    private String textIast;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerseStatus status;          // DRAFT | ANALYZED | ANALYZING | FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

// sm/selflearn/samskrtam/sangraha/model/VerseAnalysis.java
// 1:1 с Verse, перезаписывается при повторном анализе (версии не хранятся, см. §8)
@Entity
@Table(name = "verse_analyses", schema = "sangraha")
public class VerseAnalysis {
    @Id
    @Column(name = "verse_id")
    private UUID verseId;

    @Column(name = "translation_ru", columnDefinition = "TEXT", nullable = false)
    private String translationRu;

    @Column(name = "translation_en", columnDefinition = "TEXT", nullable = false)
    private String translationEn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sandhi_splits", columnDefinition = "JSONB", nullable = false)
    private String sandhiSplits;         // [{ "surface": "...", "components": ["...", "..."] }]

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_model_response", columnDefinition = "JSONB")
    private String rawModelResponse;     // сырой tool_call.arguments — для отладки промпта

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;
}

// sm/selflearn/samskrtam/sangraha/model/VerseWord.java
@Entity
@Table(name = "verse_words", schema = "sangraha")
public class VerseWord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "verse_id", nullable = false)
    private UUID verseId;

    @Column(name = "position", nullable = false)
    private int position;                // порядок слова в стихе

    @Column(name = "surface_iast", nullable = false)
    private String surfaceIast;          // словоформа как в тексте

    @Column(name = "surface_devanagari", nullable = false)
    private String surfaceDevanagari;

    @Column(name = "lemma_iast", nullable = false)
    private String lemmaIast;            // словарная форма

    @Column(nullable = false)
    private String stem;

    private String root;

    @Enumerated(EnumType.STRING)
    private PartOfSpeech pos;            // NOUN, VERB, ADJECTIVE, PRONOUN, INDECLINABLE, ...

    @Enumerated(EnumType.STRING)
    private Gender gender;               // MASCULINE, FEMININE, NEUTER, UNSPECIFIED — как в content-service

    @Enumerated(EnumType.STRING)
    private GrammaticalCase caseType;    // NOMINATIVE .. LOCATIVE, VOCATIVE, null для indeclinable

    @Enumerated(EnumType.STRING)
    private NumberType numberType;       // SINGULAR, DUAL, PLURAL

    @Enumerated(EnumType.STRING)
    private Person person;               // FIRST, SECOND, THIRD — для глаголов

    @Enumerated(EnumType.STRING)
    private Tense tense;                 // PRESENT, IMPERFECT, ... — для глаголов

    @Enumerated(EnumType.STRING)
    private Mood mood;                   // INDICATIVE, OPTATIVE, IMPERATIVE ...

    @Enumerated(EnumType.STRING)
    private Voice voice;                 // ACTIVE, MIDDLE, PASSIVE

    @Column(name = "gloss_ru", nullable = false)
    private String glossRu;

    @Column(name = "gloss_en", nullable = false)
    private String glossEn;
}
```

---

## 3. Flyway Migrations (эскиз)

```sql
-- V1__create_schema.sql
CREATE SCHEMA IF NOT EXISTS sangraha;

-- V2__create_works.sql
CREATE TABLE sangraha.works (
    id             UUID         NOT NULL DEFAULT gen_random_uuid(),
    slug           VARCHAR(80)  UNIQUE NOT NULL,
    title_ru       VARCHAR(255) NOT NULL,
    title_en       VARCHAR(255) NOT NULL,
    description_ru VARCHAR(1000),
    description_en VARCHAR(1000),
    author         VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT pk_works PRIMARY KEY (id),
    CONSTRAINT ck_work_slug CHECK (slug ~ '^[a-z0-9][a-z0-9-]*$')
);

-- V3__create_chapters.sql
CREATE TABLE sangraha.chapters (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    work_id     UUID        NOT NULL REFERENCES sangraha.works(id),
    slug        VARCHAR(80) NOT NULL,
    order_index INT         NOT NULL,
    title_ru    VARCHAR(255) NOT NULL,
    title_en    VARCHAR(255) NOT NULL,
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT pk_chapters PRIMARY KEY (id),
    CONSTRAINT uq_chapter_slug UNIQUE (work_id, slug),
    CONSTRAINT ck_chapter_slug CHECK (slug ~ '^[a-z0-9][a-z0-9-]*$')
);

-- V4__create_verses.sql
CREATE TABLE sangraha.verses (
    id              UUID NOT NULL DEFAULT gen_random_uuid(),
    chapter_id      UUID NOT NULL REFERENCES sangraha.chapters(id),
    order_index     INT  NOT NULL,
    text_devanagari TEXT,
    text_iast       TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ,
    deleted_at      TIMESTAMPTZ,
    CONSTRAINT pk_verses PRIMARY KEY (id),
    CONSTRAINT ck_verse_status CHECK (status IN ('DRAFT','ANALYZING','ANALYZED','FAILED'))
);

-- V5__create_verse_analyses.sql
CREATE TABLE sangraha.verse_analyses (
    verse_id            UUID NOT NULL REFERENCES sangraha.verses(id),
    translation_ru      TEXT NOT NULL,
    translation_en      TEXT NOT NULL,
    sandhi_splits       JSONB NOT NULL,
    raw_model_response  JSONB,
    model_name          VARCHAR(100) NOT NULL,
    analyzed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_verse_analyses PRIMARY KEY (verse_id)
);

-- V6__create_verse_words.sql
CREATE TABLE sangraha.verse_words (
    id                  UUID NOT NULL DEFAULT gen_random_uuid(),
    verse_id            UUID NOT NULL REFERENCES sangraha.verses(id),
    position            INT  NOT NULL,
    surface_iast        VARCHAR(200) NOT NULL,
    surface_devanagari  VARCHAR(200) NOT NULL,
    lemma_iast          VARCHAR(200) NOT NULL,
    stem                VARCHAR(200) NOT NULL,
    root                VARCHAR(200),
    pos                 VARCHAR(30),
    gender              VARCHAR(20),
    case_type           VARCHAR(20),
    number_type         VARCHAR(20),
    person              VARCHAR(20),
    tense               VARCHAR(20),
    mood                VARCHAR(20),
    voice               VARCHAR(20),
    gloss_ru            VARCHAR(500) NOT NULL,
    gloss_en            VARCHAR(500) NOT NULL,
    CONSTRAINT pk_verse_words PRIMARY KEY (id)
);

CREATE INDEX idx_verse_words_verse_id ON sangraha.verse_words (verse_id);

-- V7__create_outbox_events.sql  (см. §6, паттерн как в user-service/quiz-service)
CREATE TABLE sangraha.outbox_events (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id  UUID        NOT NULL,      -- verseId
    event_type    VARCHAR(50) NOT NULL,
    payload       JSONB       NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,
    retry_count   INT         NOT NULL DEFAULT 0,
    error_message TEXT,
    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT ck_event_type CHECK (event_type IN ('VERSE_VOCABULARY_EXTRACTED')),
    CONSTRAINT ck_status     CHECK (status IN ('PENDING','PROCESSED','FAILED'))
);

CREATE INDEX idx_outbox_pending ON sangraha.outbox_events (status, created_at)
    WHERE status = 'PENDING';
```

---

## 4. API

Права доступа: **весь write-контур — только `ADMIN`** (как в content-service). Отдельная
роль «редактор/переводчик» отложена на будущую итерацию (см. §8). Чтение доступно всем
аутентифицированным пользователям.

```
GET    /api/v1/sangraha/works                         → плитки произведений
POST   /api/v1/sangraha/works                          → создать произведение (ADMIN)
GET    /api/v1/sangraha/works/{workId}                 → произведение + дерево chapters/verses
                                                            (order_index, для каждого verse — textIast
                                                            (для отображения "начало стиха" в дереве), status)
PUT    /api/v1/sangraha/works/{workId}                 → обновить метаданные (ADMIN)
DELETE /api/v1/sangraha/works/{workId}                 → soft delete (ADMIN)

POST   /api/v1/sangraha/works/{workId}/chapters        → добавить главу (ADMIN)
PUT    /api/v1/sangraha/chapters/{chapterId}            → обновить главу (ADMIN)
DELETE /api/v1/sangraha/chapters/{chapterId}            → soft delete (ADMIN)

POST   /api/v1/sangraha/chapters/{chapterId}/verses    → добавить стих (пустой, DRAFT) (ADMIN)
GET    /api/v1/sangraha/verses/{verseId}                → стих: текст + (если ANALYZED) VerseAnalysis + VerseWord[]
PUT    /api/v1/sangraha/verses/{verseId}/text           → сохранить введённый текст (devanagari и/или iast) (ADMIN)
POST   /api/v1/sangraha/verses/{verseId}/analyze        → запустить LLM-анализ (ADMIN, см. §5); синхронный
                                                            ответ или 202 + опрос статуса — решает Агент 2
DELETE /api/v1/sangraha/verses/{verseId}                → soft delete (ADMIN)
```

Ответ `GET /works/{workId}` — двухуровневое дерево для TreeGrid:
```json
{
  "id": "uuid", "slug": "bhagavad-gita", "titleRu": "Бхагавад-гита",
  "chapters": [
    {
      "id": "uuid", "slug": "1", "titleRu": "Глава 1", "orderIndex": 1,
      "categoryCode": "bhagavad-gita.1",
      "verses": [
        { "id": "uuid", "orderIndex": 1, "textIastPreview": "dhṛtarāṣṭra uvāca", "status": "ANALYZED" }
      ]
    }
  ]
}
```

---

## 5. LLM-анализ стиха (tool calling)

Конфигурация — только через env, без дефолтов в yml (см. конвенцию по секретам):

```
SANGRAHA_LLM_BASE_URL     # OpenAI-совместимый endpoint
SANGRAHA_LLM_API_KEY
SANGRAHA_LLM_MODEL        # например gpt-4.1 / другая OpenAI-совместимая модель
```

Backend вызывает `/chat/completions` (или `/responses`) с промптом (транслитерировать,
перевести на ru/en, разобрать сандхи, дать пословную грамматику) и **одним** объявленным
tool — модель обязана вернуть результат через `tool_calls`, а не свободным текстом:

```json
{
  "type": "function",
  "function": {
    "name": "submit_verse_analysis",
    "description": "Структурированный результат анализа санскритского стиха",
    "parameters": {
      "type": "object",
      "required": ["textDevanagari", "textIast", "translationRu", "translationEn", "sandhiSplits", "words"],
      "properties": {
        "textDevanagari": { "type": "string" },
        "textIast": { "type": "string" },
        "translationRu": { "type": "string" },
        "translationEn": { "type": "string" },
        "sandhiSplits": {
          "type": "array",
          "items": {
            "type": "object",
            "required": ["surface", "components"],
            "properties": {
              "surface": { "type": "string" },
              "components": { "type": "array", "items": { "type": "string" } }
            }
          }
        },
        "words": {
          "type": "array",
          "items": {
            "type": "object",
            "required": ["position", "surfaceIast", "surfaceDevanagari", "lemmaIast", "stem", "glossRu", "glossEn"],
            "properties": {
              "position": { "type": "integer" },
              "surfaceIast": { "type": "string" },
              "surfaceDevanagari": { "type": "string" },
              "lemmaIast": { "type": "string" },
              "stem": { "type": "string" },
              "root": { "type": "string" },
              "pos": { "type": "string", "enum": ["NOUN","VERB","ADJECTIVE","PRONOUN","INDECLINABLE","NUMERAL"] },
              "gender": { "type": "string", "enum": ["MASCULINE","FEMININE","NEUTER","UNSPECIFIED"] },
              "caseType": { "type": "string", "enum": ["NOMINATIVE","ACCUSATIVE","INSTRUMENTAL","DATIVE","ABLATIVE","GENITIVE","LOCATIVE","VOCATIVE"] },
              "numberType": { "type": "string", "enum": ["SINGULAR","DUAL","PLURAL"] },
              "person": { "type": "string", "enum": ["FIRST","SECOND","THIRD"] },
              "tense": { "type": "string" },
              "mood": { "type": "string" },
              "voice": { "type": "string", "enum": ["ACTIVE","MIDDLE","PASSIVE"] },
              "glossRu": { "type": "string" },
              "glossEn": { "type": "string" }
            }
          }
        }
      }
    }
  }
}
```

Backend:
1. Валидирует `tool_calls[0].function.arguments` по этой схеме (например через JSON Schema validator, не доверяем модели).
2. В одной транзакции: обновляет `Verse.textDevanagari/textIast` (если не были заданы вручную), пишет `VerseAnalysis` (перезаписывая предыдущую — см. §8), пересоздаёт `VerseWord[]` для стиха, переводит `Verse.status → ANALYZED`.
3. Пишет `OutboxEvent(VERSE_VOCABULARY_EXTRACTED)` в той же транзакции (transactional outbox).

Если пользователь ввёл текст только в одном представлении (только devanagari или только
iast) — второе представление также генерирует модель, и backend сохраняет оба.

---

## 6. Kafka: sangraha → content-service

```
topic: sangraha-vocabulary-events
key:   verseId
```

Публикуется **на каждый проанализированный стих** (не батчами по главе).

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

Consumer в `content-service` (новый `@KafkaListener`, первый консьюмер в этом сервисе):
1. `upsert VocabularyCategory(code = workSlug)` (root, если не существует — создать по workTitleRu/En).
2. `upsert VocabularyCategory(code = "{workSlug}.{chapterSlug}", parentId = root.id)`.

3. `upsert Quiz(type = VOCABULARY, slug = workSlug)` — **только на уровне произведения**. Quiz на уровне главы не создаётся: агрегация слов по поддереву категорий (`VocabularyService.getVocabularyWordsForQuiz`) уже поддерживает фильтрацию по `categoryCode = "{workSlug}.{chapterSlug}"` через дерево категорий. Отдельный Quiz на главу создал бы дублирование набора слов.
4. Для каждого слова: dedup по `(wordIast, stem)` — если `VocabularyWord` уже существует, не создавать заново, только добавить `VocabularyWordCategory(wordId, categoryId=chapter.id)`, если связи ещё нет.

Payload/типы события переиспользуются как shared DTO — `shared/samskrtam-dtos` содержит пакет `sangraha` с `SangrahaVocabularyEvent`. Решение Агента 6: заводим shared DTO, т.к. событие используется двумя сервисами (producer + consumer), локальный DTO создал бы дублирование и риск рассинхронизации.

---

## 7. Frontend (эскиз, детализирует Агент 3)

- **Страница произведений** (`/sangraha`) — плитки (`WorkCard`) со списком работ + кнопка «Добавить произведение» (ADMIN).
- **Страница произведения** (`/sangraha/{workSlug}`) — TreeGrid (PrimeReact TreeTable, по аналогии с остальным фронтом): колонка 1 — дерево «глава → стих (textIastPreview)», колонка 2 — иконка/ссылка на VOCABULARY-квиз `slug = categoryCode`. Кнопки «Добавить главу», «Добавить стих» (ADMIN).
- **Страница стиха** (`/sangraha/{workSlug}/verses/{verseId}`):
  - `status=DRAFT` → textarea для ввода devanagari/iast + кнопка «Анализ» → `POST /verses/{id}/analyze`.
  - `status=ANALYZED` → read-only: devanagari, iast, перевод ru/en, сандхи, таблица слов с грамматикой; кнопка «Редактировать» возвращает к textarea и повторяет анализ (перезапись, см. §8).

---

## 8. Открытые вопросы / отложено

- **Роль «редактор/переводчик»**: пока весь write — `ADMIN`. Отдельная роль (может вводить/анализировать стихи, но не управлять произведениями/главами) — следующая итерация; когда будет готова модель ролей, добавить `SANGRAHA_EDITOR` и обновить §4.
- **Связь слов стиха со словарём** (`dictionary-service`, поиск по `slp1`): сознательно не делаем в этой итерации — только грамматика от LLM. Если понадобится — отдельным Kafka-каналом (sangraha публикует, dictionary-service асинхронно обогащает через ответное событие), без синхронных вызовов между сервисами.
- **Quiz(VOCABULARY) — только на уровне произведения**: §6.3 решён — Quiz заводится только с `slug = workSlug`. Главы не получают отдельного Quiz, т.к. агрегация слов по поддереву категорий (`VocabularyService.getVocabularyWordsForQuiz`) уже поддерживает фильтрацию по `categoryCode = "{workSlug}.{chapterSlug}"` через дерево категорий. Отдельный Quiz на главу создал бы дублирование.
