# content-service

> Домен: Quiz Content — настройки и содержание квизов, упражнения Эмено
> Язык: **Java 21 + Virtual Threads (JPA/JDBC)**
> Модуль: `services/content-service`
> Порт: 8081
> Status: **UPDATED**

---

## 1. Описание

Хранит **настройки и содержание всех квизов**: метаданные квизов (тип, сложность, slug), вопросы, варианты ответов, а также лексику для словарных квизов. Доступен только для роли `ADMIN` (запись) и внутренне для `quiz-service` (чтение). Использует JPA/JDBC с Virtual Threads.

**Новая функциональность:** Упражнения по Сандхи из учебника Эмено. Сервис предоставляет список упражнений, детали каждого упражнения с задачами, а также решения для задач, включая используемые правила Сандхи.

**Особенность:** Генерация вопросов для `VOCABULARY` квизов теперь учитывает иерархию категорий слов, выбирая слова из родительской категории и всех ее дочерних категорий, основываясь на `quiz.slug`, который соответствует `vocabulary_categories.code` (поиск инвариантен к регистру).

Разделение ответственности:
- **content-service** — что есть в квизах (данные, настройки), упражнения Эмено
- **quiz-service** — как пользователь их проходит (сессии, ответы, события)

---

## 2. Типы квизов

| QuizType | Описание |
|---|---|
| `DECLENSIONS` | Квиз по падежным формам санскрита (данные для этого квиза хранятся и управляются в content-service) |
| `CONJUGATIONS` | Квиз по спряжениям глаголов |
| `VOCABULARY` | Квиз по лексике. Слова выбираются на основе `quiz.slug`, который соответствует `vocabulary_categories.code`. Поддерживается иерархия категорий: если `quiz.slug` соответствует родительской категории, включаются слова из всех ее подкатегорий. |

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

### API для упражнений Эмено

```
GET /api/v1/eamenau/exercises                           → Получить список всех упражнений Эмено
GET /api/v1/eamenau/exercises/{id}                      → Получить детали конкретного упражнения Эмено (включая задачи)
GET /api/v1/eamenau/exercises/tasks/{taskId}/solution   → Получить список решений для конкретной задачи (is_correct = true)
GET /api/v1/eamenau/exercises/{exerciseId}/sandhi-rules → Получить список уникальных правил Сандхи, используемых во всех задачах упражнения
```

### API для правил Сандхи Эмено

```
GET /api/v1/eamenau/sandhi-rules                        → Получить список всех правил Сандхи Эмено
```

### Внутреннее API для quiz-service

```
GET /api/v1/content/quizzes/{quizId}/session-data           → всё необходимое для старта сессии
GET /api/v1/content/quizzes/{quizSlug}/vocabulary-words   → получить словарные слова для квиза (для LexicalOptionGeneratorService), фильтрация по quizSlug (code категории)
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

### Таблица `content.vocabulary_categories`:

| Поле | Тип | Описание |
|---|---|---|
| `id` | UUID | PK |
| `code` | VARCHAR(100) | Уникальный код категории (например, `basic-nouns`), соответствует `quiz.slug` |
| `parent_id` | UUID | FK на `id` этой же таблицы, для иерархии категорий |
| `name_ru` | VARCHAR(255) | Название категории на русском |
| `name_en` | VARCHAR(255) | Название категории на английском |
| `description_ru` | TEXT | Описание категории на русском |
| `description_en` | TEXT | Описание категории на английском |

### Таблица `content.vocabulary_word_categories`:

| Поле | Тип | Описание |
|---|---|---|
| `vocabulary_word_id` | UUID | FK на `content.vocabulary_words.id` |
| `category_id` | UUID | FK на `content.vocabulary_categories.id` |
| `created_at` | TIMESTAMPTZ | Время создания связи |

### Таблицы для упражнений Эмено (схема `eamenau`)

| Таблица | Описание |
|---|---|
| `eamenau.exercises` | Основная информация об упражнении (номер, буква, текст инструкции). |
| `eamenau.tasks` | Задачи внутри упражнения (текст задачи, ссылка на упражнение). |
| `eamenau.solutions` | Решения для задач (текст решения, пошаговое описание, флаг `is_correct`). |
| `eamenau.sandhi_rules` | Правила Сандхи (номер правила, тип, краткое описание, полный текст, примеры). |
| `eamenau.solution_sandhi_rules` | Связующая таблица между решениями и правилами Сандхи. |

---

## 5. Backend структура

```
sm/selflearn/samskrtam/content/
├── Application.java
├── controller/
│   ├── QuizContentController.java
│   ├── QuizManagementController.java
│   ├── VocabularyController.java
│   ├── EamenauController.java          ← Контроллер для правил Сандхи Эмено
│   ├── EamenauExerciseController.java  ← Контроллер для упражнений Эмено
│   └── internal/
│       └── SessionDataController.java
├── service/
│   ├── QuizService.java
│   ├── QuestionService.java
│   ├── VocabularyService.java
│   ├── QuestionGenerationService.java
│   ├── EamenauService.java             ← Сервис для правил Сандхи Эмено
│   └── EamenauExerciseService.java     ← Сервис для упражнений Эмено
├── repository/
│   ├── QuizRepository.java
│   ├── QuestionRepository.java
│   ├── QuestionOptionRepository.java
│   ├── VocabularyWordRepository.java
│   ├── VocabularyCategoryRepository.java
│   ├── VocabularyWordCategoryRepository.java
│   ├── ExerciseRepository.java         ← Репозиторий для упражнений Эмено
│   ├── TaskRepository.java             ← Репозиторий для задач Эмено
│   ├── SolutionRepository.java         ← Репозиторий для решений Эмено
│   ├── SolutionSandhiRuleRepository.java ← Репозиторий для связей решений и правил
│   └── SandhiRuleRepository.java       ← Репозиторий для правил Сандхи Эмено
├── model/
│   ├── Quiz.java
│   ├── Question.java
│   ├── QuestionOption.java
│   ├── VocabularyWord.java
│   ├── VocabularyCategory.java
│   ├── VocabularyWordCategory.java
│   ├── VocabularyWordCategoryId.java
│   ├── Case.java
│   ├── Number.java
│   ├── VowelType.java
│   ├── DeclensionForm.java
│   ├── DeclensionStem.java
│   ├── DeclensionFormId.java
│   ├── Exercise.java                   ← Сущность упражнения Эмено
│   ├── Task.java                       ← Сущность задачи Эмено
│   ├── Solution.java                   ← Сущность решения Эмено
│   ├── SolutionSandhiRule.java         ← Сущность связи решения и правила
│   └── SandhiRule.java                 ← Сущность правила Сандхи Эмено
└── dto/
    ├── CreateQuizRequest.java
    ├── CreateQuestionRequest.java
    ├── QuizDetailResponse.java
    ├── SessionDataResponse.java
    ├── VocabularyWordRequest.java
    ├── VocabularyWordDto.java
    ├── EamenauExerciseDto.java         ← DTO для упражнений Эмено
    ├── EamenauExerciseDetailDto.java   ← DTO для деталей упражнения Эмено
    ├── EamenauTaskDto.java             ← DTO для задач Эмено
    ├── SolutionDto.java                ← DTO для решений Эмено
    └── SandhiRuleInfo.java             ← DTO для информации о правиле Сандхи
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
    schemas: content, eamenau # Добавлена схема eamenau
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
- [ ] **Для VOCABULARY квизов:** генерация вопросов выбирает слова из категории, соответствующей `quiz.slug`, и всех ее дочерних категорий (поиск `quiz.slug` инвариантен к регистру).
- [ ] **Для упражнений Эмено:**
    - [ ] Список упражнений отображается с номером, буквой и сокращенной инструкцией.
    - [ ] При клике на упражнение открывается страница с полной инструкцией и списком задач.
    - [ ] При клике на задачу раскрывается панель с решением (текст решения, пошаговое описание, номера правил).
    - [ ] Номера правил в решении кликабельны и ведут на страницу правил, отфильтрованных по всем правилам, используемым в данном решении.
    - [ ] При наведении на номер правила отображается тултип с кратким описанием правила.

---

## 8. Открытые вопросы

- [ ] Импорт вопросов и слов из CSV для массового добавления?
- [ ] Кэшировать ли session-data в quiz-service (Redis)?
