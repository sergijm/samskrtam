# dictionary-service

> Домен: Dictionary
> Язык: **Kotlin + Coroutines + R2DBC**
> Модуль: `services/dictionary-service`
> Порт: 8085
> Связанные файлы: [mw-parser.md](./mw-parser.md) · [architecture.md](../architecture.md)
> Status: **DRAFT**

---

## 1. Описание

Единственный сервис на Kotlin в проекте — для практики языка. Реализует двухэтапный поиск:

1. **Поиск по списку** — возвращает ранжированный список похожих слов из Monier-Williams CSL API
2. **Загрузка статьи** — по клику загружает полную статью, парсит грамматику и сохраняет в локальную БД (Cache-aside)

Спецификация парсера словарных статей: [mw-parser.md](./mw-parser.md)

---

## 2. Внешние API (Cologne Sanskrit Lexicon)

### Поиск списка слов

```
GET https://sanskrit-lexicon.uni-koeln.de/scans/csl-apidev/simple-search/v1.1/getword_list_1.0.php
  ?input=<слово в транслитерации SLP1>
  &dict=MW
  &output=deva    ← деванагари в dicthwoutput
  &accent=no
  &limit=10
```

**Реальный ответ API:**

```json
{
  "dict":   "mw",
  "input":  "slp1",
  "output": "deva",
  "accent": "no",
  "result": [
    {
      "key":          "ga",
      "keyin":        "ga",
      "xml":          "ga:AP,AP90,BEN,MW,...;gaM:SKD;gaH:AP,SKD",
      "status":       200,
      "user_key_flag": true,
      "dicthw":       "ga",
      "dicthwoutput": "ग",
      "wf":           0
    },
    {
      "key":          "gA",
      "keyin":        "gA",
      "xml":          "gA:AP,AP90,BEN,MW,...",
      "status":       200,
      "user_key_flag": false,
      "dicthw":       "gA",
      "dicthwoutput": "गा",
      "wf":           49
    }
  ]
}
```

**Поля ответа:**

| Поле | Тип | Описание |
|---|---|---|
| `key` | String | SLP1 ключ для запроса статьи |
| `keyin` | String | Введённый пользователем ключ |
| `xml` | String | Словари содержащие это слово (MW, BEN, CAE...) |
| `status` | Int | HTTP статус записи (200 = найдено) |
| `user_key_flag` | Boolean | `true` = точное совпадение с запросом |
| `dicthw` | String | Headword в SLP1 (каноническая форма) |
| `dicthwoutput` | String | Headword в деванагари |
| `wf` | Int | Weight factor — релевантность. `-1` = нет данных, `0` = минимум, выше = лучше |

**Ранжирование результатов:**

```
user_key_flag = true  → точное совпадение, показывать первым
wf > 0                → сортировка по убыванию wf
wf = 0 или -1         → показывать последними
```

### Загрузка словарной статьи

```
GET https://sanskrit-lexicon.uni-koeln.de/scans/csl-apidev/listview.php
  ?key=<ключ слова>
  &output=deva       ← деванагари в ответе
  &dict=MW           ← Monier-Williams словарь
  &accent=no
  &input=slp1        ← транслитерация входных данных
```

Возвращает HTML со словарной статьёй — требует парсинга (см. [mw-parser.md](./mw-parser.md)).

### Транслитерация SLP1

CSL API принимает слова в формате **SLP1** (Sanskrit Library Phonetic encoding):

```
deva  → deva   (простые слова без изменений)
devī  → devI   (долгое ī → I)
rāma  → rAma   (долгое ā → A)
```

В v1 принимаем ввод пользователя как есть и передаём в API напрямую.
Конвертер IAST → SLP1 — открытый вопрос для v2.

---

## 3. Двухэтапный флоу поиска

```
Пользователь вводит слово → нажимает "Найти"
  ↓
GET /api/v1/dictionary/search?q=deva
  ↓
MonierWilliamsClient.searchList("deva")
  → GET csl-apidev/getword_list_1.0.php?input=deva&dict=MW&limit=10
  → ранжированный список: ["deva", "devaka", "devakī", ...]
  ↓
Фронт показывает горизонтальный кликабельный список

Пользователь кликает на слово "deva"
  ↓
GET /api/v1/dictionary/entry?key=deva
  ↓
Cache-aside:
  repository.findByKey("deva")
    → найдено  → вернуть из БД (мгновенно)
    → не найдено
        → MonierWilliamsClient.fetchHtml("deva")
          → GET csl-apidev/listview.php?key=deva&output=deva&dict=MW&...
          → HTML статья
        → MWParser.parse(key, html)       ← см. mw-parser.md
        → repository.save(entry)
        → вернуть entry
  ↓
DictionaryEntryResponse → фронт
```

---

## 4. Сущности

```kotlin
// sm/selflearn/samskrtam/dictionary/model/DictionaryEntry.kt
@Table("dictionary_entries")
data class DictionaryEntry(
    @Id val id:             UUID    = UUID.randomUUID(),
    val key:                String,          // SLP1 ключ CSL API
    val word:               String,          // IAST транслитерация
    val wordDevanagari:     String? = null,
    val meanings:           String,          // JSON array строк
    val partOfSpeech:       String? = null,  // "noun" | "adjective" | "verb" | "particle"
    val grammaticalGender:  String? = null,  // "masculine" | "feminine" | "neuter"
    val feminineEnding:     String? = null,  // "ई" из mf(ई)n.
    val verbRoot:           String? = null,  // dhātu для глаголов
    val verbClass:          Int?    = null,  // 1, 4, 10 и т.д.
    val cslId:              String? = null,  // первый [ID=...] из статьи
    val rawHtml:            String? = null,  // оригинальный HTML для перепарсинга
    val source:             String  = "MONIER_WILLIAMS",
    val createdAt:          Instant = Instant.now(),
    val updatedAt:          Instant = Instant.now()
)

// sm/selflearn/samskrtam/dictionary/model/SearchResult.kt
data class SearchResult(
    val key:    String,  // SLP1 ключ для запроса статьи
    val word:   String,  // отображаемая форма
    val weight: Double   // релевантность из CSL API
)
```

---

## 5. Flyway Migrations

```sql
-- V1__create_schema.sql
CREATE SCHEMA IF NOT EXISTS dictionary;

-- V2__create_dictionary_entries.sql
CREATE TABLE dictionary.dictionary_entries (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    key               VARCHAR(200) NOT NULL,
    word              VARCHAR(200) NOT NULL,
    word_devanagari   VARCHAR(200),
    meanings          JSONB        NOT NULL,
    part_of_speech    VARCHAR(50),
    grammatical_gender VARCHAR(20),
    feminine_ending   VARCHAR(20),
    verb_root         VARCHAR(100),
    verb_class        SMALLINT,
    csl_id            VARCHAR(20),
    raw_html          TEXT,
    source            VARCHAR(30)  NOT NULL DEFAULT 'MONIER_WILLIAMS',
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_dictionary PRIMARY KEY (id),
    CONSTRAINT uq_key        UNIQUE (key)
);

CREATE INDEX idx_dictionary_key  ON dictionary.dictionary_entries (key);
CREATE INDEX idx_dictionary_word ON dictionary.dictionary_entries (word);
```

---

## 6. API

```
GET  /api/v1/dictionary/search?q={query}   → ранжированный список слов
GET  /api/v1/dictionary/entry?key={key}    → полная статья (Cache-aside)
POST /api/v1/dictionary/admin/reparse      → перепарсинг всех статей из rawHtml
```

### GET /api/v1/dictionary/search?q=ga — Response 200

Реальный ответ CSL API содержит поля `key`, `dicthwoutput`, `wf`, `user_key_flag`.
Сервис маппит их в упрощённый формат для фронтенда:

```json
{
  "query": "ga",
  "total": 6,
  "results": [
    {
      "key":            "ga",
      "wordDevanagari": "ग",
      "wordSlp1":       "ga",
      "isExactMatch":   true,
      "isInMW":         true,
      "weight":         0
    },
    {
      "key":            "gA",
      "wordDevanagari": "गा",
      "wordSlp1":       "gA",
      "isExactMatch":   false,
      "isInMW":         true,
      "weight":         49
    },
    {
      "key":            "gf",
      "wordDevanagari": "गृ",
      "wordSlp1":       "gf",
      "isExactMatch":   false,
      "isInMW":         true,
      "weight":         11
    }
  ]
}
```

> Слова с `isInMW: false` отображаются в списке но помечаются серым —
> полная статья MW для них может отсутствовать.
> Слово с `isExactMatch: true` выделяется визуально в горизонтальном списке.

### Поля CSL API → DTO маппинг

| CSL поле | DTO поле | Примечание |
|---|---|---|
| `key` | `key` | SLP1 ключ для запроса статьи |
| `dicthwoutput` | `wordDevanagari` | деванагари |
| `dicthw` | `wordSlp1` | SLP1 транслитерация |
| `user_key_flag` | `isExactMatch` | точное совпадение |
| `wf` | `weight` | релевантность |
| `xml` | `isInMW` | парсится наличие "MW" в строке |
| `keyin` | — | не передаётся фронтенду |
| `status` | — | фильтруем != 200, не передаём |

### GET /api/v1/dictionary/entry?key=deva — Response 200

```json
{
  "key":             "deva",
  "word":            "deva",
  "wordDevanagari":  "देव",
  "meanings":        ["god, deity", "a divine being", "king (honorific)"],
  "partOfSpeech":    "noun",
  "grammaticalGender": "masculine",
  "feminineEnding":  null,
  "verbRoot":        null,
  "verbClass":       null,
  "cslId":           "95518",
  "source":          "MONIER_WILLIAMS",
  "cached":          true
}
```

---

## 7. Backend структура

```
sm/selflearn/samskrtam/dictionary/
├── Application.kt
├── controller/
│   └── DictionaryController.kt
├── service/
│   ├── DictionaryService.kt         ← Cache-aside + оркестрация
│   └── AdminDictionaryService.kt    ← reparse endpoint
├── parser/
│   ├── MWParser.kt                  ← оркестратор парсинга
│   ├── MWPatterns.kt                ← регексы (см. mw-parser.md)
│   ├── MWSenseExtractor.kt          ← извлечение значений
│   └── MWGrammarExtractor.kt        ← извлечение грамматики
├── external/
│   └── MonierWilliamsClient.kt      ← WebClient для CSL API
├── repository/
│   └── DictionaryRepository.kt      ← CoroutineCrudRepository
├── model/
│   ├── DictionaryEntry.kt
│   ├── SearchResult.kt
│   ├── Gender.kt
│   ├── WordClass.kt
│   └── VerbPada.kt
└── dto/
    ├── SearchResponse.kt
    └── DictionaryEntryResponse.kt
```

---

## 8. Ключевые классы

```kotlin
// DictionaryService.kt
@Service
class DictionaryService(
    private val repository: DictionaryRepository,
    private val mwClient:   MonierWilliamsClient,
    private val parser:     MWParser
) {
    // Шаг 1 — поиск (всегда через внешний API)
    suspend fun search(query: String): SearchResponse =
        mwClient.searchList(query)

    // Шаг 2 — статья (Cache-aside)
    suspend fun getEntry(key: String): DictionaryEntry =
        repository.findByKey(key)
            ?: mwClient.fetchHtml(key)
                .let  { html  -> parser.parse(key, html) }
                .also { entry -> repository.save(entry)  }
}

// MonierWilliamsClient.kt
@Component
class MonierWilliamsClient(private val webClient: WebClient) {

    private val baseUrl = "https://sanskrit-lexicon.uni-koeln.de/scans/csl-apidev"

    suspend fun searchList(query: String): SearchResponse =
        webClient.get()
            .uri("$baseUrl/simple-search/v1.1/getword_list_1.0.php") {
                it.queryParam("input", query)
                  .queryParam("dict",  "MW")
                  .queryParam("limit", 10)
                  .build()
            }
            .retrieve()
            .awaitBody()

    suspend fun fetchHtml(key: String): String =
        webClient.get()
            .uri("$baseUrl/listview.php") {
                it.queryParam("key",    key)
                  .queryParam("output", "deva")
                  .queryParam("dict",   "MW")
                  .queryParam("accent", "no")
                  .queryParam("input",  "slp1")
                  .build()
            }
            .retrieve()
            .awaitBody()
}
```

> **Зависимость:** парсер использует **Jsoup**.
> Добавить в `build.gradle.kts`: `implementation("org.jsoup:jsoup:1.17.2")`

---

## 9. application.yml

```yaml
server:
  port: 8085

spring:
  application:
    name: dictionary-service
  r2dbc:
    url: ${SPRING_R2DBC_URL}
    username: ${DB_USER:samskrtam}
    password: ${DB_PASSWORD}
  flyway:
    url: jdbc:postgresql://${DB_HOST:postgres}:5432/samskrtam
    schemas: dictionary
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${KEYCLOAK_JWKS_URI}

external:
  dictionary:
    csl-base-url: https://sanskrit-lexicon.uni-koeln.de/scans/csl-apidev
    timeout-ms: 10000
    search-limit: 10
```

---

## 10. Acceptance Criteria

- [ ] Поиск → ранжированный список слов из CSL API
- [ ] Клик на слово → статья из БД или внешний запрос
- [ ] Статья сохраняется в БД после первого запроса (с rawHtml)
- [ ] Повторный запрос того же слова → из БД без внешнего запроса
- [ ] Парсер извлекает: meanings, partOfSpeech, grammaticalGender (v1)
- [ ] CSL API недоступен → 503 с понятным сообщением
- [ ] `POST /admin/reparse` → перепарсирует все записи из rawHtml

---

## 11. Открытые вопросы

- [ ] Конвертер IAST → SLP1 (v2)
- [ ] Лицензия данных Monier-Williams — можно ли хранить локально?
- [ ] TTL для кэшированных статей — обновлять ли устаревшие?
- [ ] Full-text search по meanings (PostgreSQL tsvector)?
- [ ] Деванагари в вопросах поиска — нужна ли поддержка?
