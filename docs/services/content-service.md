# content-service

> Домен: Quiz Content — настройки и содержание квизов
> Язык: **Java 21 + Virtual Threads (JPA/JDBC)**
> Модуль: `services/content-service`
> Порт: 8081
> Status: **DRAFT**

---

## 1. Описание

Хранит **настройки и содержание всех квизов**: метаданные квизов (тип, сложность, slug), вопросы, варианты ответов, а также лексику для словарных квизов. Доступен только для роли `ADMIN` (запись) и внутренне для `quiz-service` (чтение). Использует JPA/JDBC с Virtual Threads.

Разделение ответственности:
- **content-service** — что есть в квизах (данные, настройки)
- **quiz-service** — как пользователь их проходит (сессии, ответы, события)

---

## 2. Типы квизов

| QuizType | Описание |
|---|---|
| `DECLENSIONS` | Квиз по падежным формам санскрита (данные для этого квиза хранятся и управляются в content-service) |
| `CONJUGATIONS` | Квиз по спряжениям глаголов |
| `VOCABULARY` | Квиз по лексике (slug-based) |

---

## 3. API

### Управление квизами (ADMIN)

```
GET    /api/v1/content/quizzes                          → список квизов (фильтр по quizType)
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
GET /api/v1/content/quizzes/{quizId}/vocabulary-words   → получить словарные слова для квиза (для LexicalOptionGeneratorService)
```

Ответ `GET /session-data`:
```json
{
  "quizId": "uuid",
  "quizType": "DECLENSIONS",
  "questionsPerSession": 20,
  "questions": [
    {
      "id": "uuid",
      "textRu": "Какое окончание у именительного падежа ед.ч. основ на -a?",
      "textEn": "What is the nominative singular ending of -a stems?",
      "explanationRu": "...",
      "explanationEn": "...",
      "correctOptionId": "uuid",
      "options": [
        { "id": "uuid", "textRu": "-aḥ", "textEn": "-aḥ" },
        { "id": "uuid", "textRu": "-am", "textEn": "-am" }
      ]
    }
  ],
  "vocabularyWords": [ // НОВОЕ ПОЛЕ: заполняется только для VOCABULARY квизов
    {
      "id": "uuid",
      "wordIast": "deva",
      "wordDevanagari": "देव",
      "translationEn": "god",
      "translationRu": "бог",
      "gender": "MASCULINE",
      "stem": "deva",
      "root": null,
      "dictionaryEntry": "..."
    }
  ]
}
```

---

## 4. Схема БД

### Таблица `content.quizzes`:

Ключевые колонки таблицы `content.quizzes`:

| Колонка | Тип | Описание |
|---|---|---|
| `id` | UUID | PK |
| `quiz_type` | VARCHAR | `DECLENSIONS`, `CONJUGATIONS`, `VOCABULARY` |
| `slug` | VARCHAR | Уникальный идентификатор (`^[a-z0-9][a-z0-9-]*$`) |
| `title_ru` / `title_en` | VARCHAR | Название квиза |
| `difficulty` | VARCHAR | Уровень сложности |
| `questions_per_session` | INT | Количество вопросов в одной сессии. Действует для всех типов квизов — см. [content-service.md](content-service.md) раздел 4. **По умолчанию: 20** |
| `deleted_at` | TIMESTAMPTZ | Soft delete — `NULL` если активен |

`questions_per_session` передаётся в `session-data` и используется quiz-service при старте сессии для случайной выборки вопросов.

### Таблица `content.vocabulary_words`:

| Поле | Тип | Описание |
|---|---|---|
| `id` | UUID | PK |
| `word_iast` | VARCHAR(255) | Слово в IAST |
| `word_devanagari` | VARCHAR(255) | Слово в Деванагари |
| `translation_en` | VARCHAR(500) | Перевод на английский |
| `translation_ru` | VARCHAR(500) | Перевод на русский |
| `gender` | VARCHAR(20) | Род слова (`MASCULINE`, `FEMININE`, `NEUTER`, `UNKNOWN`) |
| `stem` | VARCHAR(255) | Основа слова |
| `root` | VARCHAR(255) | Корень слова |
| `dictionary_entry` | TEXT | Полная словарная статья |
| `created_at` | TIMESTAMPTZ | Время создания записи |
| `updated_at` | TIMESTAMPTZ | Время последнего обновления записи |

---

## 5. Backend структура

```
sm/selflearn/samskrtam/content/
├── Application.java
├── controller/
│   ├── QuizContentController.java
│   ├── QuizManagementController.java
│   ├── VocabularyController.java       ← НОВЫЙ контроллер для словарных слов
│   └── internal/
│       └── SessionDataController.java    ← /session-data для quiz-service
├── service/
│   ├── QuizService.java
│   ├── QuestionService.java
│   └── VocabularyService.java          ← НОВЫЙ сервис для словарных слов
├── repository/
│   ├── QuizRepository.java
│   ├── QuestionRepository.java
│   ├── QuestionOptionRepository.java
│   └── VocabularyWordRepository.java   ← НОВЫЙ репозиторий для словарных слов
├── model/
│   ├── Quiz.java
│   ├── Question.java
│   ├── QuestionOption.java
│   ├── VocabularyWord.java             ← НОВАЯ сущность для словарных слов
│   ├── Case.java
│   ├── Number.java
│   ├── VowelType.java
│   ├── DeclensionForm.java
│   ├── DeclensionStem.java
│   └── DeclensionFormId.java
└── dto/
    ├── CreateQuizRequest.java
    ├── CreateQuestionRequest.java
    ├── QuizDetailResponse.java
    ├── SessionDataResponse.java
    ├── VocabularyWordRequest.java
    ├── VocabularyWordDto.java          ← НОВЫЙ DTO для словарных слов
```

```
sm/selflearn/samskrtam/quiz/content/dto/ // Обновленный путь для DTO, ранее находившихся в content/dto
├── Gender.java
├── QuizType.java
├── Difficulty.java
```

---

## 6. application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: content-service
  threads:
    virtual:
      enabled: true
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${DB_USER:samskrtam}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: content
  flyway:
    schemas: content
```

---

## 7. Acceptance Criteria

- [ ] Только ADMIN получает доступ к write-операциям (403 для STUDENT)
- [ ] `GET /session-data` доступен без роли ADMIN (для quiz-service)
- [ ] Нельзя сохранить вопрос без ровно 1 правильного варианта
- [ ] Удаление квиза и вопроса — soft delete
- [ ] `vocabularyWords` возвращаются только для квизов с `quiz_type = VOCABULARY`
- [ ] Slug уникален и соответствует паттерну `^[a-z0-9][a-z0-9-]*$`
- [ ] `questions_per_session` не может быть меньше 1

---

## 8. Открытые вопросы

- [ ] Импорт вопросов и слов из CSV для массового добавления?
- [ ] Кэшировать ли session-data в quiz-service (Redis)?
