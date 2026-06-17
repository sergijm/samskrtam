# dictionary-service

> Домен: Dictionary
> Язык: **Java 21 + Virtual Threads (JPA/JDBC)**
> Модуль: `services/dictionary-service`
> Порт: 8085
> Связанные файлы: [mw-parser.md](./mw-parser.md) · [architecture.md](../architecture.md)
> Status: **DRAFT**

---

## 1. Описание

Сервис для работы с санскритскими словарями. Словари хранятся целиком в локальной базе данных PostgreSQL, каждый в своей отдельной схеме. Реализует функциональность поиска слов и получения полных словарных статей.

## 2. Стек

| Технология | Назначение |
|---|---|
| Java 21 | Язык + Virtual Threads (Project Loom) |
| Spring Boot 3.3 | Фреймворк |
| Spring MVC | HTTP (блокирующий стиль, VT делает его async) |
| Spring Data JPA | Доступ к БД (обычный JDBC) |
| PostgreSQL JDBC | Драйвер БД |
| Flyway | Миграции |
| Jsoup | Парсинг HTML (для импорта/обновления статей) |

## 3. Хранение данных

Словари хранятся в отдельных схемах PostgreSQL.

| Схема | Описание |
|---|---|
| `d_fri` | Словарь Фриша |
| `d_mw` | Словарь Монье-Вильямса |

Каждая схема содержит таблицы для хранения слов и их метаданных (например, `words`, `meanings`, `grammar_details`).

## 4. Механика поиска

### 4.1. Поиск по списку (`/search`)

Пользователь вводит запрос (`query`), сервис возвращает ранжированный список похожих слов из выбранного словаря. Результаты включают `slp1Spelling` (оригинальное написание) и `slp1Normalized` (нормализованное написание без диакритики, используемое для отображения на фронтенде и для запроса статьи).

### 4.2. Загрузка статьи (`/entry`)

По клику на слово из списка, фронтенд передает `slp1Normalized` в качестве параметра `slp1Spelling`. Сервис загружает и возвращает полную словарную статью, используя это значение для поиска.

## 5. API

### 5.1. GET /api/v1/dictionary/search?query={query}

Возвращает ранжированный список слов, соответствующих запросу `query`, из словаря Монье-Вильямса.

**Параметры:**
*   `query`: Query Parameter (Строка поиска, может быть в любой транслитерации, бэкенд нормализует ее в SLP1)

**Пример ответа 200:**
```json
[
  {
    "slp1Spelling": "deva",
    "slp1Normalized": "deva",
    "iastSpelling": "deva",
    "similarity": 1.0
  },
  {
    "slp1Spelling": "devaka",
    "slp1Normalized": "devaka",
    "iastSpelling": "devaka",
    "similarity": 0.8
  }
]
```

### 5.2. GET /api/v1/dictionary/entry?slp1Spelling={slp1Normalized}

Возвращает полную словарную статью для указанного слова. Фронтенд передает `slp1Normalized` из результатов поиска в параметр `slp1Spelling`.

**Параметры:**
*   `slp1Spelling`: Query Parameter (Нормализованное SLP1 написание слова, полученное из `slp1Normalized` в результатах поиска)

**Пример ответа 200:**
```json
{
  "entries": [
    {
      "recordId": "MW_deva_1",
      "key1": "deva",
      "key1Display": "deva",
      "key2": "deva",
      "homonymNum": "1",
      "eCode": "1",
      "page": 492,
      "columnNum": 3,
      "isSupplement": false,
      "mainTranslation": "a god, deity, divine being, celestial (opposed to man)",
      "rawBody": "<body>...</body>",
      "displayTitle": "deva",
      "lexicalInfo": [],
      "sanskritWords": [],
      "homonyms": [],
      "abbreviations": [],
      "literarySources": [],
      "infoTags": []
    }
  ]
}
```

## 6. Backend структура

```
sm/selflearn/samskrtam/dictionary/
├── Application.java
├── controller/
│   └── DictionaryController.java       ← Публичные эндпоинты (поиск, статья)
├── service/
│   └── DictionaryService.java          ← Основная бизнес-логика
│   └── TransliterationService.java     ← Сервис для транслитерации и нормализации
├── repository/
│   ├── MwEntryRepository.java          ← Репозиторий для основных статей Monier-Williams
│   ├── MwSanskritWordRepository.java   ← Репозиторий для санскритских слов в статьях
│   └── ... (другие репозитории для деталей статей)
├── model/
│   ├── MwEntry.java                    ← Сущность основной статьи Monier-Williams
│   ├── MwSanskritWord.java             ← Сущность санскритского слова
│   └── ... (другие сущности для деталей статей)
└── dto/
    ├── MwWordSearchDto.java            ← DTO для результатов поиска слов
    └── MwEntryDto.java                 ← DTO для полной статьи (содержит List<MwDictionaryEntryDto>)
```

## 7. Ключевые классы

*   **`DictionaryController`**: Обрабатывает HTTP-запросы для поиска и получения статей.
*   **`DictionaryService`**: Содержит основную бизнес-логику, координирует работу репозиториев и сервисов.
*   **`MwDictionaryEntryService`**: Сервис для сборки полной словарной статьи из различных сущностей.
*   **`MwSanskritWordRepository`**: JPA репозиторий для доступа к данным санскритских слов, включая поиск по `slp1Normalized`.

## 8. application.yml

```yaml
server:
  port: 8085

spring:
  application:
    name: dictionary-service
  threads:
    virtual:
      enabled: true
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: d_mw # Схема по умолчанию, может быть переопределена
  flyway:
    url: ${SPRING_DATASOURCE_URL}
    user: ${DB_USER}
    password: ${DB_PASSWORD}
    schemas: d_mw, d_fri # Flyway будет работать с обеими схемами
```

## 9. Acceptance Criteria

*   [ ] Поиск (`/search`) возвращает список слов, отсортированный по релевантности, включая `slp1Normalized`.
*   [ ] `GET /entry` возвращает полную статью для существующего слова по `slp1Spelling` (который фактически является `slp1Normalized` с фронтенда).
*   [ ] `GET /entry` возвращает 404 для несуществующего слова.
*   [ ] Сервис корректно работает с несколькими схемами БД (`d_fri`, `d_mw`).
*   [ ] Поддерживается поиск по IAST.

## 10. Открытые вопросы

*   [ ] Как будет определяться "релевантность" для поиска (`/search`)? Полнотекстовый поиск PostgreSQL?
*   [ ] Нужна ли поддержка поиска по частям слова (wildcard search)?
*   [ ] Как будут обрабатываться синонимы и варианты написания?
*   [ ] Как будет реализована связь между словами и грамматической информацией (род, склонение, спряжение)?
