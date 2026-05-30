# content-service

> Домен: Quiz Content — настройки и содержание квизов
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

| QuizType | Описание |
|---|---|
| `DECLENSIONS` | Квиз по падежным формам санскрита |
| `CONJUGATIONS` | Квиз по спряжениям глаголов |
| `VOCABULARY` | Квиз по лексике (slug-based) |

---

## 3. Сущности

```java
// sm/selflearn/samskrtam/content/model/Quiz.java
@Entity
@Table(name = "quizzes", schema = "content")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true)
    private String slug;              // "animals", "declensions-a-stem", "1" — опционально

    @Column(name = "title_ru", nullable = false)
    private String titleRu;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Column(name = "description_ru")
    private String descriptionRu;

    @Column(name = "description_en")
    private String descriptionEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizType quizType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    @Column(name = "questions_per_session")
    private int questionsPerSession = 10;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

// sm/selflearn/samskrtam/content/model/Question.java
@Entity
@Table(name = "questions", schema = "content")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "text_ru", nullable = false, columnDefinition = "TEXT")
    private String textRu;

    @Column(name = "text_en", nullable = false, columnDefinition = "TEXT")
    private String textEn;

    @Column(name = "explanation_ru", nullable = false, columnDefinition = "TEXT")
    private String explanationRu;

    @Column(name = "explanation_en", nullable = false, columnDefinition = "TEXT")
    private String explanationEn;

    @Column(name = "correct_option_id")
    private UUID correctOptionId;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}

// sm/selflearn/samskrtam/content/model/QuestionOption.java
@Entity
@Table(name = "question_options", schema = "content")
public class QuestionOption {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "text_ru", nullable = false)
    private String textRu;

    @Column(name = "text_en", nullable = false)
    private String textEn;
}

// sm/selflearn/samskrtam/content/model/VocabularyWord.java
// Используется только для VOCABULARY квизов — слова для динамической генерации вопросов
@Entity
@Table(name = "vocabulary_words", schema = "content")
public class VocabularyWord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    private String word;              // IAST транслитерация
    private String wordDevanagari;
    private String translationRu;
    private String translationEn;
    private String partOfSpeech;
    private String example;
}
```

---

## 4. Flyway Migrations

```sql
-- V1__create_schema.sql
CREATE SCHEMA IF NOT EXISTS content;

-- V2__create_quizzes.sql
CREATE TABLE content.quizzes (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    slug                  VARCHAR(50)  UNIQUE,
    title_ru              VARCHAR(255) NOT NULL,
    title_en              VARCHAR(255) NOT NULL,
    description_ru        VARCHAR(500),
    description_en        VARCHAR(500),
    quiz_type             VARCHAR(20)  NOT NULL,
    difficulty            VARCHAR(20)  NOT NULL DEFAULT 'BEGINNER',
    questions_per_session INT          NOT NULL DEFAULT 10,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at            TIMESTAMPTZ,
    CONSTRAINT pk_quizzes      PRIMARY KEY (id),
    CONSTRAINT ck_quiz_type    CHECK (quiz_type IN ('DECLENSIONS','CONJUGATIONS','VOCABULARY')),
    CONSTRAINT ck_difficulty   CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    CONSTRAINT ck_slug         CHECK (slug ~ '^[a-z0-9][a-z0-9-]*$')
);

-- V3__create_questions.sql
CREATE TABLE content.questions (
    id                UUID NOT NULL DEFAULT gen_random_uuid(),
    quiz_id           UUID NOT NULL REFERENCES content.quizzes(id),
    text_ru           TEXT NOT NULL,
    text_en           TEXT NOT NULL,
    explanation_ru    TEXT NOT NULL,
    explanation_en    TEXT NOT NULL,
    correct_option_id UUID,
    deleted_at        TIMESTAMPTZ,
    CONSTRAINT pk_questions PRIMARY KEY (id)
);

-- V4__create_question_options.sql
CREATE TABLE content.question_options (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    question_id UUID         NOT NULL REFERENCES content.questions(id),
    text_ru     VARCHAR(255) NOT NULL,
    text_en     VARCHAR(255) NOT NULL,
    CONSTRAINT pk_options PRIMARY KEY (id)
);

ALTER TABLE content.questions
    ADD CONSTRAINT fk_correct_option
    FOREIGN KEY (correct_option_id) REFERENCES content.question_options(id);

-- V5__create_vocabulary_words.sql
-- Слова для VOCABULARY квизов (динамическая генерация вопросов в quiz-service)
CREATE TABLE content.vocabulary_words (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    quiz_id          UUID         NOT NULL REFERENCES content.quizzes(id),
    word             VARCHAR(200) NOT NULL,
    word_devanagari  VARCHAR(200),
    translation_ru   VARCHAR(500) NOT NULL,
    translation_en   VARCHAR(500) NOT NULL,
    part_of_speech   VARCHAR(50),
    example          TEXT,
    CONSTRAINT pk_vocabulary_words PRIMARY KEY (id)
);

CREATE INDEX idx_vocabulary_words_quiz_id ON content.vocabulary_words (quiz_id);

-- V6__seed_vocabulary_quizzes.sql
-- Начальные квизы по лексике
INSERT INTO content.quizzes (slug, title_ru, title_en, quiz_type, difficulty)
VALUES
    ('animals',    'Животные',        'Animals',          'VOCABULARY', 'BEGINNER'),
    ('numbers',    'Числа',           'Numbers',          'VOCABULARY', 'BEGINNER'),
    ('body-parts', 'Тело',            'Body Parts',       'VOCABULARY', 'BEGINNER'),
    ('nature',     'Природа',         'Nature',           'VOCABULARY', 'BEGINNER'),
    ('1',          'Базовая лексика', 'Basic Vocabulary', 'VOCABULARY', 'BEGINNER'),
    ('2',          'Средний уровень', 'Intermediate',     'VOCABULARY', 'INTERMEDIATE');
```

---

## 5. API

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
```

Ответ `GET /session-data`:
```json
{
  "quizId": "uuid",
  "quizType": "DECLENSIONS",
  "questionsPerSession": 10,
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
  "vocabularyWords": null   // заполняется только для VOCABULARY квизов
}
```

---

## 6. Backend структура

```
sm/selflearn/samskrtam/content/
├── Application.java
├── controller/
│   ├── QuizController.java
│   ├── QuestionController.java
│   ├── VocabularyController.java
│   └── internal/
│       └── SessionDataController.java    ← /session-data для quiz-service
├── service/
│   ├── QuizService.java
│   ├── QuestionService.java
│   └── VocabularyService.java
├── repository/
│   ├── QuizRepository.java
│   ├── QuestionRepository.java
│   ├── QuestionOptionRepository.java
│   └── VocabularyWordRepository.java
├── model/
│   ├── Quiz.java
│   ├── Question.java
│   ├── QuestionOption.java
│   ├── VocabularyWord.java
│   ├── QuizType.java
│   └── Difficulty.java
└── dto/
    ├── CreateQuizRequest.java
    ├── CreateQuestionRequest.java
    ├── QuizDetailResponse.java
    ├── SessionDataResponse.java
    └── VocabularyWordRequest.java
```

---

## 7. application.yml

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
