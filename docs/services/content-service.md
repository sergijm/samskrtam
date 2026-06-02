# content-service

> Домен: Quiz Content — настройки и содержание квизов
> Язык: **Java 21 + WebFlux (Reactor)**
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

## 4. Backend структура

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

## 5. application.yml

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

## 6. Acceptance Criteria

- [ ] Только ADMIN получает доступ к write-операциям (403 для STUDENT)
- [ ] `GET /session-data` доступен без роли ADMIN (для quiz-service)
- [ ] Нельзя сохранить вопрос без ровно 1 правильного варианта
- [ ] Удаление квиза и вопроса — soft delete
- [ ] `vocabulary_words` возвращаются только для квизов с `quiz_type = VOCABULARY`
- [ ] Slug уникален и соответствует паттерну `^[a-z0-9][a-z0-9-]*$`

---

## 7. Открытые вопросы

- [ ] Импорт вопросов и слов из CSV для массового добавления?
- [ ] Кэшировать ли session-data в quiz-service (Redis)?
