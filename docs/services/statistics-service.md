# statistics-service

> Домен: Statistics
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/statistics-service`
> Порт: 8086
> Status: **DRAFT**

---

## 1. Описание

Получает события из Kafka и строит аналитику. Нет синхронной связи с Quiz сервисами — только через очередь. Java 21 Virtual Threads упрощают Kafka consumer — обычный блокирующий код.

---

## 2. Сущности

**AnswerRecord** (таблица answer_records): id (UUID), eventId (UUID, unique — идемпотентность), userId, lessonType, quizId, questionId, selectedOptionId, correct (boolean), responseTimeMs (int), occurredAt

**SessionRecord** (таблица session_records): id (UUID), eventId (UUID, unique), userId, lessonType, quizId, score (int), totalQuestions (int), durationMs (long), occurredAt

---

## 3. Flyway Migrations

3 миграции: V1 — schema statistics; V2 — answer_records (event_id UNIQUE для идемпотентности, индексы по user_id, question_id, occurred_at); V3 — session_records (event_id UNIQUE, индексы по user_id, occurred_at).

---

## 4. Kafka Consumers

Consumer должен быть **идемпотентным**: повторная доставка одного и того же события (rebalance, retry после DLQ) не должна создавать дублирующую запись. Идемпотентность реализована через `UNIQUE (event_id)` в БД и `INSERT ... ON CONFLICT DO NOTHING` в репозитории.

Два Kafka consumer: `AnswerSubmittedConsumer` (topic: quiz.answer.submitted) и `SessionCompletedConsumer` (topic: quiz.session.completed), оба с groupId = statistics-service. Вызывают `StatisticsService.recordAnswer()` / `StatisticsService.recordSession()`.

`StatisticsService`: метод `recordAnswer` — INSERT с ON CONFLICT (event_id) DO NOTHING в answer_records; `recordSession` — аналогично в session_records. Если inserted == 0, лог WARN с eventId.

Репозитории: `AnswerRecordRepository` с `insertIfNotExists` (native query, ON CONFLICT), `findByUserId`, `findByUserIdAndQuizId`. `SessionRecordRepository` с `insertIfNotExists`, `findByUserId`.

---

## 5. API

```
GET /api/v1/statistics/me              → личная статистика
GET /api/v1/statistics/me/heatmap      → тепловая карта ошибок
GET /api/v1/statistics/me/history      → история сессий
GET /api/v1/statistics/attempts/{id}   → детали попытки
GET /api/v1/leaderboard                → лидерборд (алгоритмы в leaderboard.md)
  ?type=xp|elo|accuracy|streak|skill|composite
  &period=all|weekly|monthly
  &lessonType=DECLENSIONS|CONJUGATIONS|VOCABULARY
  &groupId=<uuid>
  &limit=1-100
```

### GET /api/v1/statistics/leaderboard — Response

Ответ: { entries[{rank, username, totalPoints, totalSessions, isCurrentUser}] }. Алгоритмы лидерборда — в [leaderboard.md](leaderboard.md).

---

## 6. Backend структура

Пакет `consumer/`: AnswerSubmittedConsumer, SessionCompletedConsumer.
`controller/`: StatisticsController.
`service/`: StatisticsService, LeaderboardService.
`algorithm/`: XpService, EloService, EloCalculator, AccuracyService, StreakService, SkillRatingService, CompositeScoreService.
`scheduler/`: LeaderboardRefreshScheduler.
`repository/`: AnswerRecordRepository, SessionRecordRepository, XpLedgerRepository, XpTotalsRepository, EloRatingRepository, EloHistoryRepository, UserStreakRepository, SkillRatingRepository, AccuracyCacheRepository.
`model/`: AnswerRecord, SessionRecord, XpLedger, XpTotals, EloRating, EloHistory, UserStreak, SkillRating.
`dto/`: PersonalStatsResponse, HeatmapResponse, LeaderboardResponse, LeaderboardEntry, AttemptDetailResponse.

---

## 7. application.yml

Порт 8086, virtual threads enabled, datasource через env, default_schema: statistics, Kafka consumer (group-id: statistics-service, auto-offset-reset: earliest, trusted packages: sm.selflearn.samskrtam.events). JWT не валидируется — сервис доверяет X-User-Id от Gateway.

---

## 8. Acceptance Criteria

- [ ] AnswerSubmitted из Kafka → сохраняется в answer_records
- [ ] SessionCompleted из Kafka → сохраняется в session_records
- [ ] При недоступности сервиса события ждут в Kafka (не теряются)
- [ ] Повторная доставка одного события не создаёт дублирующую запись (ON CONFLICT DO NOTHING по event_id)
- [ ] Дубликат логируется на уровне WARN с eventId для диагностики
- [ ] Лидерборд: очки = сумма лучших результатов по каждому квизу
- [ ] Тепловая карта показывает вопросы с correctRate < 0.5

---


## Lesson History API

> Спецификация: [lesson-pages-spec.md](../frontend/lesson-pages-spec.md) · [lesson-openapi.yaml](../frontend/lesson-openapi.yaml)

Для страниц уроков `VocabularyLessonPage` и `GrammarLessonPage` statistics-service предоставляет агрегированную статистику по отдельным словам и вопросам.

**Новые эндпоинты:**

```
GET /api/v1/statistics/lessons/vocabulary/{slug}
    → VocabularyLesson с nSuccess/nAll/status по каждому слову

GET /api/v1/statistics/lessons/grammar/{type}
    → GrammarLesson с nSuccess/nAll/status по каждому вопросу

GET /api/v1/statistics/lessons/vocabulary/{slug}/words/{wordId}/history
    → история ответов пользователя на слово в уроке (paginated)

GET /api/v1/statistics/lessons/grammar/{type}/questions/{questionId}/history
    → история ответов пользователя на вопрос в уроке (paginated)
```

Данные агрегируются из таблицы `quiz_answers` с JOIN на `quiz_session` для фильтрации по `quizId`. Статус слова/вопроса вычисляется на лету по формуле:
- `MASTERED`: `nAll > 0` и `successRate >= 80%`
- `LEARNING`: `nAll > 0` и `successRate >= 50%` и `successRate < 80%`
- `REVIEW`: `nAll > 0` и `successRate < 50%`
- `NEW`: `nAll = 0`


## 9. Открытые вопросы

- [ ] Кэшировать лидерборд в Redis?
- [ ] Недельный/месячный рейтинг?
- [ ] CQRS для read-моделей статистики?
