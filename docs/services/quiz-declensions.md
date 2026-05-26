# quiz-declensions-service

> Домен: Quiz Content — Склонения
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/quiz-declensions-service`
> Порт: 8082
> Эталонный паттерн для всех quiz-сервисов
> Status: **DRAFT**

---

## 1. Описание

Квиз по склонениям санскрита. Реализует механику multiple-choice сессии. После каждого ответа публикует событие в Kafka. Является эталоном — quiz-conjugations и quiz-vocabulary копируют этот паттерн.

---

## 2. Предметная область

### 8 падежей санскрита

| № | Падеж (ru) | Падеж (en) | Функция |
|---|---|---|---|
| 1 | Именительный | Nominative | Подлежащее |
| 2 | Винительный | Accusative | Прямое дополнение |
| 3 | Творительный | Instrumental | Орудие |
| 4 | Дательный | Dative | Косвенное дополнение |
| 5 | Отложительный | Ablative | Удаление |
| 6 | Родительный | Genitive | Принадлежность |
| 7 | Местный | Locative | Место |
| 8 | Звательный | Vocative | Обращение |

### Таблица склонения — deva (основа -a, м.р.)

| Падеж | Singular | Dual | Plural |
|---|---|---|---|
| Nominative | devaḥ | devau | devāḥ |
| Accusative | devam | devau | devān |
| Instrumental | devena | devābhyām | devaiḥ |
| Dative | devāya | devābhyām | devebhyaḥ |
| Ablative | devāt | devābhyām | devebhyaḥ |
| Genitive | devasya | devayoḥ | devānām |
| Locative | deve | devayoḥ | deveṣu |
| Vocative | deva | devau | devāḥ |

---

## 3. Механика сессии

```
GET /api/v1/quiz/declensions/sessions/start
  ↓ 10 перемешанных вопросов (без правильных ответов)
POST /api/v1/quiz/declensions/sessions/{id}/answer
  ↓ фидбек + объяснение → Kafka AnswerSubmitted
POST /api/v1/quiz/declensions/sessions/{id}/complete
  ↓ Kafka SessionCompleted → редирект на statistics
```

---

## 4. API

```
GET  /api/v1/quiz/declensions/sessions/start
POST /api/v1/quiz/declensions/sessions/{id}/answer
POST /api/v1/quiz/declensions/sessions/{id}/complete
```

### POST /sessions/{id}/answer — Request

```json
{
  "questionId": "uuid",
  "selectedOptionId": "uuid"
}
```

### POST /sessions/{id}/answer — Response

```json
{
  "isCorrect": true,
  "correctOptionId": "uuid",
  "explanation": "Дательный ед.ч. основ на -a — окончание -āya",
  "explanationEn": "Dative singular of -a stems takes ending -āya",
  "questionNumber": 3,
  "totalQuestions": 10
}
```

---

## 5. Backend структура

```
sm/selflearn/samskrtam/quiz/declensions/
├── Application.java
├── controller/
│   └── SessionController.java
├── service/
│   ├── SessionService.java        ← логика сессии
│   └── ScoringService.java        ← проверка ответов
├── event/
│   └── QuizEventPublisher.java    ← Kafka producer
├── model/
│   └── QuizSession.java           ← хранится в Redis
└── dto/
    ├── StartSessionResponse.java
    ├── AnswerRequest.java
    └── AnswerResponse.java
```

---

## 6. Kafka events

```java
// После каждого ответа
publisher.publishAnswerSubmitted(new AnswerSubmitted(
    userId,
    QuizType.DECLENSIONS,
    session.getQuizId(),
    request.getQuestionId(),
    request.getSelectedOptionId(),
    result.isCorrect(),
    elapsedMs
));

// После завершения сессии
publisher.publishSessionCompleted(new SessionCompleted(
    userId,
    QuizType.DECLENSIONS,
    session.getQuizId(),
    session.getScore(),
    session.getTotalQuestions(),
    session.getDurationMs()
));
```

---

## 7. application.yml

```yaml
server:
  port: 8082

spring:
  application:
    name: quiz-declensions-service
  threads:
    virtual:
      enabled: true
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${DB_USER:samskrtam}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: 6379
  kafka:
    bootstrap-servers: ${SPRING_KAFKA_BOOTSTRAP_SERVERS:kafka:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${KEYCLOAK_JWKS_URI}

quiz:
  session:
    questions-per-session: 10
    ttl-minutes: 60
```

---

## 8. Acceptance Criteria

- [ ] Сессия содержит 10 случайных вопросов
- [ ] Правильный ответ не передаётся клиенту до POST /answer
- [ ] После каждого ответа — AnswerSubmitted в Kafka
- [ ] После завершения — SessionCompleted в Kafka
- [ ] Нельзя ответить дважды на один вопрос
- [ ] Сессия хранится в Redis (переживает restart)

---

## 9. Открытые вопросы

- [ ] TTL сессии в Redis — сколько минут?
- [ ] Деванагари или только IAST в вопросах?
- [ ] Режим "только ошибки"?
