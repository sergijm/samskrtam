# content-service

> Домен: Quiz Content — CRUD
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/content-service`
> Порт: 8081
> Status: **DRAFT**

---

## 1. Описание

CRUD сервис для управления учебным контентом. Хранит квизы, вопросы, варианты ответов. Доступен только для роли ADMIN. Quiz-сервисы читают контент при старте сессии.

Virtual Threads позволяют использовать обычный JPA/JDBC без WebFlux.

---

## 2. Сущности

```java
// sm/selflearn/samskrtam/content/model/Quiz.java
@Entity
@Table(name = "quizzes", schema = "content")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "title_ru", nullable = false)
    private String titleRu;

    @Column(name = "title_en", nullable = false)
    private String titleEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizType quizType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

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
```

---

## 3. Flyway Migrations

```sql
-- V1__create_schema.sql
CREATE SCHEMA IF NOT EXISTS content;

-- V2__create_quizzes.sql
CREATE TABLE content.quizzes (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    title_ru    VARCHAR(255) NOT NULL,
    title_en    VARCHAR(255) NOT NULL,
    quiz_type   VARCHAR(20) NOT NULL,
    difficulty  VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ,
    CONSTRAINT pk_quizzes      PRIMARY KEY (id),
    CONSTRAINT ck_quiz_type    CHECK (quiz_type IN ('DECLENSIONS','CONJUGATIONS','VOCABULARY')),
    CONSTRAINT ck_difficulty   CHECK (difficulty IN ('BEGINNER','INTERMEDIATE','ADVANCED'))
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
```

---

## 4. API

```
GET    /api/v1/content/quizzes
GET    /api/v1/content/quizzes/{id}/questions
POST   /api/v1/content/quizzes
PUT    /api/v1/content/quizzes/{id}
DELETE /api/v1/content/quizzes/{id}
POST   /api/v1/content/quizzes/{id}/questions
PUT    /api/v1/content/questions/{id}
DELETE /api/v1/content/questions/{id}
PUT    /api/v1/content/questions/{id}/correct-option
```

---

## 5. Backend структура

```
sm/selflearn/samskrtam/content/
├── Application.java
├── controller/
│   ├── QuizController.java
│   └── QuestionController.java
├── service/
│   ├── QuizService.java
│   └── QuestionService.java
├── repository/
│   ├── QuizRepository.java
│   └── QuestionRepository.java
├── model/
│   ├── Quiz.java
│   ├── Question.java
│   ├── QuestionOption.java
│   ├── QuizType.java
│   └── Difficulty.java
└── dto/
    ├── CreateQuizRequest.java
    ├── CreateQuestionRequest.java
    └── QuizDetailResponse.java
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
      enabled: true              # Virtual Threads
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
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${KEYCLOAK_JWKS_URI}
```

---

## 7. Acceptance Criteria

- [ ] Только ADMIN получает доступ (403 для STUDENT)
- [ ] Нельзя сохранить вопрос без ровно 1 правильного варианта
- [ ] Удаление квиза — soft delete
- [ ] Quiz-сервисы получают correctOptionId в ответе

---

## 8. Открытые вопросы

- [ ] Импорт вопросов из CSV для массового добавления?
- [ ] Кэшировать ли контент в Quiz-сервисах?
