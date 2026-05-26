# dictionary-service

> Домен: Dictionary
> Язык: **Kotlin + Coroutines + R2DBC**
> Модуль: `services/dictionary-service`
> Порт: 8085
> Status: **DRAFT**

---

## 1. Описание

Единственный сервис на Kotlin в проекте — для практики языка. Реализует Cache-aside паттерн: локальная БД → внешнее API. Kotlin Coroutines делают этот паттерн максимально выразительным.

---

## 2. Почему Kotlin здесь особенно уместен

```kotlin
// Cache-aside в три строки — лучший пример силы Kotlin + Coroutines
suspend fun lookup(word: String): DictionaryEntry =
    repository.findByWord(word)
        ?: fetchFromExternal(word).also { repository.save(it) }
```

Эквивалент на Java с WebFlux занял бы 15+ строк flatMap/switchIfEmpty.

---

## 3. Внешние API

| API | Приоритет | Формат |
|---|---|---|
| Sanskrit Heritage Site | Первичный | JSON |
| Monier-Williams | Fallback | XML/HTML |

---

## 4. Сущности

```kotlin
// sm/selflearn/samskrtam/dictionary/model/DictionaryEntry.kt
@Table("dictionary_entries")
data class DictionaryEntry(
    @Id val id:             UUID    = UUID.randomUUID(),
    val word:               String,
    val wordDevanagari:     String? = null,
    val meanings:           String,          // JSON array
    val partOfSpeech:       String? = null,
    val source:             EntrySource,
    val createdAt:          Instant = Instant.now(),
    val updatedAt:          Instant = Instant.now()
)

enum class EntrySource { LOCAL, SANSKRIT_HERITAGE, MONIER_WILLIAMS }
```

---

## 5. Flyway Migration

```sql
-- V1__create_schema.sql
CREATE SCHEMA IF NOT EXISTS dictionary;

-- V2__create_dictionary_entries.sql
CREATE TABLE dictionary.dictionary_entries (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    word             VARCHAR(100) NOT NULL,
    word_devanagari  VARCHAR(100),
    meanings         JSONB        NOT NULL,
    part_of_speech   VARCHAR(50),
    source           VARCHAR(30)  NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_dictionary PRIMARY KEY (id),
    CONSTRAINT uq_word UNIQUE (word)
);

CREATE INDEX idx_dictionary_word        ON dictionary.dictionary_entries (word);
CREATE INDEX idx_dictionary_word_prefix ON dictionary.dictionary_entries (word text_pattern_ops);
```

---

## 6. API

```
GET /api/v1/dictionary/{word}         → статья словаря
GET /api/v1/dictionary/search?q={q}   → поиск по префиксу (до 10 результатов)
```

### GET /api/v1/dictionary/{word} — Response 200

```json
{
  "word":           "deva",
  "wordDevanagari": "देव",
  "meanings":       ["god, deity", "king (honorific)"],
  "partOfSpeech":   "noun, masculine",
  "source":         "SANSKRIT_HERITAGE",
  "cached":         true
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
│   └── DictionaryService.kt          ← Cache-aside логика
├── external/
│   ├── ExternalDictionaryClient.kt   ← интерфейс
│   ├── SanskritHeritageClient.kt     ← WebClient, приоритет
│   └── MonierWilliamsClient.kt       ← WebClient, fallback
├── repository/
│   └── DictionaryRepository.kt       ← CoroutineCrudRepository
├── model/
│   ├── DictionaryEntry.kt
│   └── EntrySource.kt
└── dto/
    └── DictionaryResponse.kt
```

---

## 8. Ключевые классы

```kotlin
// DictionaryService.kt
@Service
class DictionaryService(
    private val repository: DictionaryRepository,
    private val heritageClient: SanskritHeritageClient,
    private val monierClient: MonierWilliamsClient
) {
    suspend fun lookup(word: String): DictionaryEntry =
        repository.findByWord(word)
            ?: fetchFromExternal(word).also { repository.save(it) }

    private suspend fun fetchFromExternal(word: String): DictionaryEntry =
        runCatching { heritageClient.fetch(word) }
            .getOrElse { monierClient.fetch(word) }
            ?: throw WordNotFoundException(word)
}

// DictionaryRepository.kt
interface DictionaryRepository : CoroutineCrudRepository<DictionaryEntry, UUID> {
    suspend fun findByWord(word: String): DictionaryEntry?
    fun findByWordStartingWith(prefix: String): Flow<DictionaryEntry>
}

// DictionaryController.kt
@RestController
@RequestMapping("/api/v1/dictionary")
class DictionaryController(private val service: DictionaryService) {

    @GetMapping("/{word}")
    suspend fun lookup(@PathVariable word: String): DictionaryResponse =
        service.lookup(word).toResponse()

    @GetMapping("/search")
    fun search(@RequestParam q: String): Flow<DictionaryResponse> =
        service.search(q).map { it.toResponse() }
}
```

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
    heritage-url: https://sanskrit.inria.fr/cgi-bin/SKT/sktdict.cgi
    monier-url: https://www.sanskrit-lexicon.uni-koeln.de/scans/MWScan/2020/web/webtc/getword.php
    timeout-ms: 5000
```

---

## 10. Acceptance Criteria

- [ ] Слово найдено локально → ответ без внешнего API
- [ ] Слово не найдено → запрос к Sanskrit Heritage
- [ ] Sanskrit Heritage недоступен → fallback на Monier-Williams
- [ ] Оба недоступны → 404
- [ ] После успешного внешнего запроса слово сохраняется локально
- [ ] Поиск по префиксу возвращает до 10 результатов как Flow

---

## 11. Открытые вопросы

- [ ] Лицензия данных Monier-Williams — можно ли хранить локально?
- [ ] Нужен ли seed-импорт базового словаря?
- [ ] TTL для кэшированных записей?
