# Leaderboard — Спецификация алгоритмов

> Модуль: `services/statistics-service`
> Status: **DRAFT — выбор алгоритмов не финализирован**

В этом файле описаны алгоритмы лидерборда — глобального и внутри группы.
Сырые данные для всех алгоритмов поступают из `statistics.answer_records` и
`statistics.session_records` (см. [statistics-service.md](statistics-service.md)).

---

## 1. Обзор вариантов

| # | Алгоритм | Сложность реализации | Мотивирует новичков | Подходит для санскрита |
|---|---|---|---|---|
| 1 | Experience Points (XP) | ★☆☆ | Да | Да |
| 2 | Elo Rating | ★★★ | Нет (новичок всегда внизу) | Да |
| 3 | Accuracy Ranking | ★☆☆ | Нет (нужно 500+ ответов) | Да |
| 4 | Streak Leaderboard | ★☆☆ | Да | Да |
| 5 | Weekly Leaderboard | ★★☆ | Да (новичок может войти в топ) | Да |
| 6 | Skill Rating | ★★☆ | Нет | **Отлично** (категории санскрита) |
| 7 | Composite Score | ★★★ | Да | Да |

**Рекомендуемый MVP:** Варианты 1 + 5 + 6.
XP — понятная метрика, Weekly — мотивирует новичков, Skill Rating — уникально для санскрита.
Остальные можно добавить итеративно.

---

## 2. Вариант 1 — Experience Points (XP)

### Правила начисления

| Событие | XP |
|---|---|
| Правильный ответ | +10 |
| Неправильный ответ | +0 |
| Стрик 10 правильных подряд | +50 (бонус) |
| Perfect quiz (100% правильных) | +100 (бонус) |
| Hard quiz (любой результат) | ×1.5 к XP за ответы |

Множитель сложности применяется только к XP за ответы, не к бонусам:

```
hard quiz, 8/10 правильных:
  base   = 8 × 10 = 80 XP
  ×1.5   = 120 XP
  perfect? нет → +0
  streak? допустим да → +50
  итого  = 170 XP
```

### Схема БД

```sql
-- V5__create_xp_ledger.sql
-- Ledger (журнал) — каждое начисление XP как отдельная строка.
-- Итоговый XP = SUM(amount) по userId. Легко аудировать, идемпотентно по event_id.
CREATE TABLE statistics.xp_ledger (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    event_id    UUID        NOT NULL,            -- из AnswerSubmitted / SessionCompleted
    user_id     UUID        NOT NULL,
    amount      INT         NOT NULL,
    reason      VARCHAR(30) NOT NULL,            -- CORRECT_ANSWER | STREAK | PERFECT | DIFFICULTY
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_xp_ledger     PRIMARY KEY (id),
    CONSTRAINT uq_xp_event_id   UNIQUE (event_id, reason),  -- идемпотентность
    CONSTRAINT ck_xp_reason CHECK (
        reason IN ('CORRECT_ANSWER', 'STREAK_BONUS', 'PERFECT_BONUS', 'DIFFICULTY_MULTIPLIER')
    )
);

CREATE INDEX idx_xp_user_id   ON statistics.xp_ledger (user_id);
CREATE INDEX idx_xp_occurred  ON statistics.xp_ledger (occurred_at DESC);

-- Read model для лидерборда — обновляется триггером или Scheduled job
CREATE TABLE statistics.xp_totals (
    user_id      UUID NOT NULL,
    total_xp     BIGINT NOT NULL DEFAULT 0,
    last_updated TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_xp_totals PRIMARY KEY (user_id)
);
```

### Алгоритм (Java)

```java
// sm/selflearn/samskrtam/statistics/service/XpService.java
@Service
@RequiredArgsConstructor
public class XpService {

    private static final int XP_CORRECT       = 10;
    private static final int XP_STREAK_BONUS  = 50;
    private static final int XP_PERFECT_BONUS = 100;
    private static final double MULTIPLIER_HARD = 1.5;

    private final XpLedgerRepository ledgerRepository;
    private final XpTotalsRepository totalsRepository;
    private final UserStreakRepository streakRepository;

    @Transactional
    public void processSession(SessionCompleted event) {
        int baseXp = 0;

        // 1. XP за правильные ответы с учётом сложности
        double multiplier = event.difficulty() == Difficulty.HARD ? MULTIPLIER_HARD : 1.0;
        int answersXp = (int) Math.round(event.correctAnswers() * XP_CORRECT * multiplier);
        baseXp += answersXp;

        // 2. Perfect quiz бонус
        boolean isPerfect = event.correctAnswers() == event.totalQuestions();
        if (isPerfect) baseXp += XP_PERFECT_BONUS;

        // 3. Streak бонус — считается по AnswerRecord, не по сессии
        //    Streak обновляется в processAnswer(), здесь только читаем
        int currentStreak = streakRepository.getCurrentStreak(event.userId());
        int streakBonus = (currentStreak > 0 && currentStreak % 10 == 0) ? XP_STREAK_BONUS : 0;

        // 4. Записываем в ledger (ON CONFLICT DO NOTHING — идемпотентно)
        ledgerRepository.insertIfNotExists(event.eventId(), event.userId(),
                answersXp, "CORRECT_ANSWER", event.occurredAt());
        if (isPerfect)
            ledgerRepository.insertIfNotExists(event.eventId(), event.userId(),
                    XP_PERFECT_BONUS, "PERFECT_BONUS", event.occurredAt());
        if (streakBonus > 0)
            ledgerRepository.insertIfNotExists(event.eventId(), event.userId(),
                    streakBonus, "STREAK_BONUS", event.occurredAt());

        // 5. Обновляем read model
        totalsRepository.incrementXp(event.userId(), baseXp + streakBonus);
    }
}
```

### Лидерборд

```sql
-- Глобальный TOP XP
SELECT u.username, x.total_xp
FROM statistics.xp_totals x
JOIN users u ON u.id = x.user_id
ORDER BY x.total_xp DESC
LIMIT 50;

-- Внутри группы
SELECT u.username, x.total_xp
FROM statistics.xp_totals x
JOIN users u ON u.id = x.user_id
JOIN group_members gm ON gm.user_id = x.user_id AND gm.group_id = :groupId
ORDER BY x.total_xp DESC;
```

---

## 3. Вариант 2 — Elo Rating

Рейтинг по аналогии с шахматами. Пользователь "играет" против квиза.

### Параметры

| Сущность | Начальный рейтинг |
|---|---|
| Новый пользователь | 1000 |
| Easy quiz | 800 |
| Medium quiz | 1200 |
| Hard quiz | 1600 |

K-фактор (чувствительность рейтинга): `K = 32`.

### Формула

```
Ожидаемый результат пользователя:
  E = 1 / (1 + 10^((quizRating - userRating) / 400))

Фактический результат (нормализован в [0, 1]):
  S = correctAnswers / totalQuestions

Новый рейтинг:
  newRating = userRating + K × (S - E)
```

Пример:

```
userRating = 1000, quizRating = 1600 (hard)
E = 1 / (1 + 10^(600/400)) = 1 / (1 + 10^1.5) ≈ 0.030
S = 8/10 = 0.8
Δ = 32 × (0.8 - 0.030) = +24.6 → рейтинг: 1024

userRating = 1000, quizRating = 800 (easy)
E = 1 / (1 + 10^(-200/400)) ≈ 0.760
S = 4/10 = 0.4
Δ = 32 × (0.4 - 0.760) = -11.5 → рейтинг: 988
```

### Схема БД

```sql
-- V6__create_elo_ratings.sql
CREATE TABLE statistics.elo_ratings (
    user_id         UUID    NOT NULL,
    current_rating  INT     NOT NULL DEFAULT 1000,
    peak_rating     INT     NOT NULL DEFAULT 1000,
    total_games     INT     NOT NULL DEFAULT 0,
    last_updated    TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_elo_ratings PRIMARY KEY (user_id)
);

-- История изменений для графика динамики рейтинга
CREATE TABLE statistics.elo_history (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    event_id    UUID        NOT NULL,
    user_id     UUID        NOT NULL,
    delta       INT         NOT NULL,       -- может быть отрицательным
    rating_after INT        NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_elo_history   PRIMARY KEY (id),
    CONSTRAINT uq_elo_event_id  UNIQUE (event_id)
);

CREATE INDEX idx_elo_user ON statistics.elo_history (user_id, occurred_at DESC);
```

### Алгоритм (Java)

```java
// sm/selflearn/samskrtam/statistics/service/EloService.java
@Service
@RequiredArgsConstructor
public class EloService {

    private static final int    K              = 32;
    private static final int    DEFAULT_RATING = 1000;
    private static final Map<Difficulty, Integer> QUIZ_RATINGS = Map.of(
            Difficulty.BEGINNER,     800,
            Difficulty.INTERMEDIATE, 1200,
            Difficulty.ADVANCED,     1600
    );

    private final EloRatingRepository ratingRepository;
    private final EloHistoryRepository historyRepository;

    @Transactional
    public void processSession(SessionCompleted event) {
        int userRating = ratingRepository.findById(event.userId())
                .map(EloRating::getCurrentRating)
                .orElse(DEFAULT_RATING);

        int quizRating = QUIZ_RATINGS.get(event.difficulty());

        double expected = 1.0 / (1 + Math.pow(10, (quizRating - userRating) / 400.0));
        double actual   = (double) event.correctAnswers() / event.totalQuestions();
        int    delta    = (int) Math.round(K * (actual - expected));
        int    newRating = Math.max(100, userRating + delta); // пол рейтинга — 100

        ratingRepository.upsert(event.userId(), newRating);
        historyRepository.insertIfNotExists(event.eventId(), event.userId(),
                delta, newRating, event.occurredAt());
    }
}
```

### Лидерборд

```sql
-- Глобальный TOP Rating
SELECT u.username, e.current_rating, e.peak_rating
FROM statistics.elo_ratings e
JOIN users u ON u.id = e.user_id
WHERE e.total_games >= 5        -- скрываем пользователей без игр
ORDER BY e.current_rating DESC
LIMIT 50;
```

---

## 4. Вариант 3 — Accuracy Ranking

### Правила

```
accuracy = correct_answers / total_answers   (в [0.0, 1.0])
```

Минимальный порог для попадания в лидерборд: **500 ответов**.
Без порога один правильный ответ из одного даст 100% — бессмысленно.

### Схема БД

```sql
-- Агрегат: считается из answer_records, кэшируется для быстрой сортировки
CREATE TABLE statistics.accuracy_cache (
    user_id       UUID    NOT NULL,
    total_answers INT     NOT NULL DEFAULT 0,
    correct_count INT     NOT NULL DEFAULT 0,
    accuracy      NUMERIC(5,4) GENERATED ALWAYS AS  -- 0.0000–1.0000
        (CASE WHEN total_answers = 0 THEN 0
              ELSE correct_count::NUMERIC / total_answers END) STORED,
    last_updated  TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_accuracy_cache PRIMARY KEY (user_id)
);
```

### Лидерборд

```sql
-- Только пользователи с 500+ ответами
SELECT u.username,
       a.accuracy * 100 AS accuracy_pct,
       a.total_answers
FROM statistics.accuracy_cache a
JOIN users u ON u.id = a.user_id
WHERE a.total_answers >= 500
ORDER BY a.accuracy DESC
LIMIT 50;
```

### Ограничение

Accuracy Ranking не самостоятелен как главная метрика — рекомендуется использовать
как вторичную вкладку на странице лидерборда ("Самые точные") или как компонент
Composite Score (Вариант 7).

---

## 5. Вариант 4 — Streak Leaderboard

### Правила

`days_in_row` — количество календарных дней подряд, в которые пользователь
дал хотя бы один ответ. Стрик обнуляется если пользователь пропустил день.

Часовой пояс: UTC. Пользователь в UTC+3 может терять стрик если ответил в 23:00 по
местному времени и не ответил "вчера" по UTC. **Открытый вопрос:** хранить timezone в профиле.

### Схема БД

```sql
-- V7__create_streaks.sql
CREATE TABLE statistics.user_streaks (
    user_id          UUID    NOT NULL,
    current_streak   INT     NOT NULL DEFAULT 0,   -- дней подряд сейчас
    longest_streak   INT     NOT NULL DEFAULT 0,   -- исторический максимум
    last_activity_at DATE    NOT NULL,             -- последний день с ответом (UTC)
    CONSTRAINT pk_user_streaks PRIMARY KEY (user_id)
);
```

### Алгоритм обновления стрика

```java
// sm/selflearn/samskrtam/statistics/service/StreakService.java
@Service
@RequiredArgsConstructor
public class StreakService {

    private final UserStreakRepository streakRepository;

    @Transactional
    public void processAnswer(AnswerSubmitted event) {
        LocalDate today = event.occurredAt().atZone(ZoneOffset.UTC).toLocalDate();

        streakRepository.findById(event.userId()).ifPresentOrElse(streak -> {
            long daysSinceLast = ChronoUnit.DAYS.between(streak.getLastActivityAt(), today);

            if (daysSinceLast == 0) {
                // Уже отвечал сегодня — стрик не меняется
            } else if (daysSinceLast == 1) {
                // Следующий день подряд — увеличиваем
                streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                streak.setLongestStreak(
                    Math.max(streak.getLongestStreak(), streak.getCurrentStreak()));
                streak.setLastActivityAt(today);
                streakRepository.save(streak);
            } else {
                // Пропуск — обнуляем
                streak.setCurrentStreak(1);
                streak.setLastActivityAt(today);
                streakRepository.save(streak);
            }
        }, () -> {
            // Первый ответ пользователя
            streakRepository.save(new UserStreak(event.userId(), 1, 1, today));
        });
    }
}
```

### Лидерборд

```sql
-- Глобальный TOP Streak
SELECT u.username, s.current_streak, s.longest_streak
FROM statistics.user_streaks s
JOIN users u ON u.id = s.user_id
ORDER BY s.current_streak DESC
LIMIT 50;

-- Топ по историческому рекорду
SELECT u.username, s.longest_streak
FROM statistics.user_streaks s
JOIN users u ON u.id = s.user_id
ORDER BY s.longest_streak DESC
LIMIT 50;
```

---

## 6. Вариант 5 — Weekly / Monthly Leaderboard (CQRS Read Model)

Лидерборд за последние 7 / 30 / 365 дней. Позволяет новичкам войти в топ.
Это классический CQRS-паттерн: write side — `xp_ledger`, read side — денормализованные таблицы.

### Схема БД

```sql
-- V8__create_leaderboard_snapshots.sql
CREATE TABLE statistics.leaderboard_daily (
    user_id  UUID NOT NULL,
    day      DATE NOT NULL,
    xp_earned INT NOT NULL DEFAULT 0,
    CONSTRAINT pk_lb_daily PRIMARY KEY (user_id, day)
);

-- Weekly и Monthly — materialized views поверх daily, обновляются по расписанию
CREATE MATERIALIZED VIEW statistics.leaderboard_weekly AS
SELECT
    user_id,
    SUM(xp_earned) AS weekly_xp
FROM statistics.leaderboard_daily
WHERE day >= CURRENT_DATE - INTERVAL '7 days'
GROUP BY user_id;

CREATE UNIQUE INDEX ON statistics.leaderboard_weekly (user_id);

CREATE MATERIALIZED VIEW statistics.leaderboard_monthly AS
SELECT
    user_id,
    SUM(xp_earned) AS monthly_xp
FROM statistics.leaderboard_daily
WHERE day >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY user_id;

CREATE UNIQUE INDEX ON statistics.leaderboard_monthly (user_id);
```

### Обновление

Запись в `leaderboard_daily` происходит синхронно при обработке `SessionCompleted`
(вместе с `xp_ledger`). Materialized views обновляются раз в час через `@Scheduled`:

```java
// sm/selflearn/samskrtam/statistics/scheduler/LeaderboardRefreshScheduler.java
@Component
@RequiredArgsConstructor
public class LeaderboardRefreshScheduler {

    private final EntityManager em;

    // Раз в час — приемлемая задержка для лидерборда
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void refresh() {
        em.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY statistics.leaderboard_weekly").executeUpdate();
        em.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY statistics.leaderboard_monthly").executeUpdate();
    }
}
```

`CONCURRENTLY` — обновление без блокировки чтения, требует `UNIQUE INDEX` на view.

### Лидерборд

```sql
-- Weekly TOP (глобальный)
SELECT u.username, w.weekly_xp
FROM statistics.leaderboard_weekly w
JOIN users u ON u.id = w.user_id
ORDER BY w.weekly_xp DESC
LIMIT 50;

-- Weekly TOP (внутри группы)
SELECT u.username, w.weekly_xp
FROM statistics.leaderboard_weekly w
JOIN users u ON u.id = w.user_id
JOIN group_members gm ON gm.user_id = w.user_id AND gm.group_id = :groupId
ORDER BY w.weekly_xp DESC;
```

---

## 7. Вариант 6 — Skill Rating

Отдельный рейтинг по каждой категории санскрита. Идеально подходит для проекта:
в санскрите склонения, спряжения и словарь — принципиально разные навыки.

### Категории

| Категория | QuizType в системе |
|---|---|
| Declensions | `DECLENSIONS` |
| Conjugations | `CONJUGATIONS` |
| Vocabulary | `VOCABULARY` |

> Sandhi — можно добавить позже как отдельный `QuizType`.

### Схема БД

```sql
-- V9__create_skill_ratings.sql
CREATE TABLE statistics.skill_ratings (
    user_id             UUID NOT NULL,
    quiz_type           VARCHAR(20) NOT NULL,
    rating              INT  NOT NULL DEFAULT 1000,   -- Elo по категории
    total_sessions      INT  NOT NULL DEFAULT 0,
    accuracy            NUMERIC(5,4) NOT NULL DEFAULT 0,
    last_updated        TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_skill_ratings PRIMARY KEY (user_id, quiz_type),
    CONSTRAINT ck_skill_quiz_type CHECK (
        quiz_type IN ('DECLENSIONS', 'CONJUGATIONS', 'VOCABULARY')
    )
);
```

### Алгоритм

Skill Rating использует тот же Elo-алгоритм что и Вариант 2, но применяется
отдельно по каждому `QuizType`. `SessionCompleted` содержит `quizType` — этого
достаточно для маршрутизации в нужный рейтинг.

```java
// sm/selflearn/samskrtam/statistics/service/SkillRatingService.java
@Service
@RequiredArgsConstructor
public class SkillRatingService {

    private final SkillRatingRepository skillRepository;
    // Переиспользуем EloService для расчёта delta
    private final EloCalculator eloCalculator;   // чистая функция, без I/O

    @Transactional
    public void processSession(SessionCompleted event) {
        SkillRating skill = skillRepository
                .findByUserIdAndQuizType(event.userId(), event.quizType())
                .orElse(SkillRating.defaultFor(event.userId(), event.quizType()));

        int delta    = eloCalculator.delta(skill.getRating(),
                event.difficulty(), event.correctAnswers(), event.totalQuestions());
        int newRating = Math.max(100, skill.getRating() + delta);

        double newAccuracy = ((skill.getAccuracy() * skill.getTotalSessions())
                + ((double) event.correctAnswers() / event.totalQuestions()))
                / (skill.getTotalSessions() + 1);

        skill.setRating(newRating);
        skill.setAccuracy(newAccuracy);
        skill.setTotalSessions(skill.getTotalSessions() + 1);
        skill.setLastUpdated(Instant.now());

        skillRepository.save(skill);
    }
}
```

### Лидерборды по категориям

```sql
-- Top Declension Experts
SELECT u.username, s.rating, s.accuracy * 100 AS accuracy_pct
FROM statistics.skill_ratings s
JOIN users u ON u.id = s.user_id
WHERE s.quiz_type = 'DECLENSIONS'
  AND s.total_sessions >= 3
ORDER BY s.rating DESC
LIMIT 20;

-- Top Vocabulary Masters (внутри группы)
SELECT u.username, s.rating
FROM statistics.skill_ratings s
JOIN users u ON u.id = s.user_id
JOIN group_members gm ON gm.user_id = s.user_id AND gm.group_id = :groupId
WHERE s.quiz_type = 'VOCABULARY'
ORDER BY s.rating DESC;
```

---

## 8. Вариант 7 — Composite Score

Финальная формула объединяющая все метрики. Позволяет гибко балансировать.

### Формула

```
Score = XP
      + 50 × current_streak
      + difficulty_bonus
      + achievement_bonus
```

Где:

| Слагаемое | Описание |
|---|---|
| `XP` | Накопленные XP (Вариант 1) |
| `50 × current_streak` | Бонус за streak в днях (Вариант 4) |
| `difficulty_bonus` | +200 за каждый пройденный Hard квиз (score ≥ 70%) |
| `achievement_bonus` | +500 за каждую разблокированную ачивку (будущее) |

### Пример

```
XP            = 12 500
streak        = 14 дней  → 14 × 50 = 700
hard quizzes  = 5        → 5 × 200 = 1 000
achievements  = 3        → 3 × 500 = 1 500
──────────────────────────────────────────
Composite     =          15 700
```

### Схема БД

```sql
-- Composite Score — materialized view поверх уже существующих таблиц
CREATE MATERIALIZED VIEW statistics.composite_scores AS
SELECT
    x.user_id,
    x.total_xp
    + (s.current_streak * 50)
    + (COALESCE(h.hard_completions, 0) * 200)   AS composite_score
FROM statistics.xp_totals x
JOIN statistics.user_streaks   s ON s.user_id = x.user_id
LEFT JOIN (
    SELECT user_id, COUNT(*) AS hard_completions
    FROM statistics.session_records
    WHERE difficulty = 'ADVANCED'
      AND score::NUMERIC / total_questions >= 0.7
    GROUP BY user_id
) h ON h.user_id = x.user_id;

CREATE UNIQUE INDEX ON statistics.composite_scores (user_id);
```

Обновляется вместе с `leaderboard_weekly` в `LeaderboardRefreshScheduler`.

### Лидерборд

```sql
-- Глобальный Composite TOP
SELECT u.username, c.composite_score
FROM statistics.composite_scores c
JOIN users u ON u.id = c.user_id
ORDER BY c.composite_score DESC
LIMIT 50;
```

---

## 9. Группы — лидерборд внутри группы

Все алгоритмы поддерживают фильтрацию по группе через JOIN с `group_members`.
API принимает опциональный параметр `groupId`:

```
GET /api/v1/leaderboard?type=xp&period=weekly&groupId=<uuid>
GET /api/v1/leaderboard?type=skill&quizType=DECLENSIONS&groupId=<uuid>
GET /api/v1/leaderboard?type=streak
```

| Параметр | Значения | По умолчанию |
|---|---|---|
| `type` | `xp`, `elo`, `accuracy`, `streak`, `skill`, `composite` | `xp` |
| `period` | `all`, `weekly`, `monthly` | `all` |
| `quizType` | `DECLENSIONS`, `CONJUGATIONS`, `VOCABULARY` | — (обязателен для `type=skill`) |
| `groupId` | UUID группы | — (глобальный если не указан) |
| `limit` | 1–100 | 50 |

Доступ к лидерборду группы: только участники группы и администраторы.

### Response (унифицированный)

```json
{
  "type":     "xp",
  "period":   "weekly",
  "groupId":  "uuid-or-null",
  "entries": [
    {
      "rank":          1,
      "userId":        "...",
      "username":      "anna",
      "value":         4200,
      "valueLabel":    "4 200 XP",
      "isCurrentUser": false,
      "delta":         "+320 за день"
    }
  ],
  "currentUserEntry": {
    "rank": 7, "value": 1800, "valueLabel": "1 800 XP", "isCurrentUser": true
  }
}
```

`currentUserEntry` всегда возвращается отдельно — даже если текущий пользователь
не попал в `limit`. Так фронтенд всегда может показать "Ты на 47 месте".

---

## 10. Backend структура (дополнение к statistics-service)

```
sm/selflearn/samskrtam/statistics/
├── leaderboard/
│   ├── LeaderboardController.java      ← GET /api/v1/leaderboard
│   ├── LeaderboardService.java         ← роутит по type + period
│   ├── LeaderboardResponse.java        ← унифицированный DTO
│   └── LeaderboardEntry.java
│
├── algorithm/
│   ├── XpService.java
│   ├── EloService.java
│   ├── EloCalculator.java              ← чистая функция delta(), без I/O
│   ├── AccuracyService.java
│   ├── StreakService.java
│   ├── SkillRatingService.java
│   └── CompositeScoreService.java
│
└── scheduler/
    └── LeaderboardRefreshScheduler.java
```

`StatisticsService` (существующий) вызывает нужные алгоритмы при обработке событий:

```java
// StatisticsService.java
@Transactional
public void recordSession(SessionCompleted event) {
    // 1. Сырые данные (уже существует)
    sessionRepository.insertIfNotExists(...);

    // 2. Алгоритмы
    xpService.processSession(event);
    eloService.processSession(event);
    skillRatingService.processSession(event);
    // Streak обновляется в recordAnswer() по каждому ответу
}

@Transactional
public void recordAnswer(AnswerSubmitted event) {
    answerRepository.insertIfNotExists(...);
    streakService.processAnswer(event);
    accuracyService.processAnswer(event);
}
```

---

## 11. Flyway порядок миграций

| Версия | Файл | Что создаёт |
|---|---|---|
| V1 | `V1__create_schema.sql` | schema statistics |
| V2 | `V2__create_answer_records.sql` | answer_records |
| V3 | `V3__create_session_records.sql` | session_records |
| V4 | `V4__create_quiz_outbox.sql` | quiz.outbox_events (quiz-service) |
| V5 | `V5__create_xp_ledger.sql` | xp_ledger, xp_totals |
| V6 | `V6__create_elo_ratings.sql` | elo_ratings, elo_history |
| V7 | `V7__create_streaks.sql` | user_streaks |
| V8 | `V8__create_leaderboard_snapshots.sql` | leaderboard_daily, views |
| V9 | `V9__create_skill_ratings.sql` | skill_ratings |
| V10 | `V10__create_composite_view.sql` | composite_scores view |

---

## 12. Acceptance Criteria

### XP (Вариант 1)
- [ ] Правильный ответ начисляет 10 XP
- [ ] Hard quiz применяет множитель ×1.5 к XP за ответы
- [ ] Perfect quiz начисляет бонус +100 XP
- [ ] Streak 10 подряд начисляет бонус +50 XP
- [ ] Повторная обработка события не начисляет XP дважды (ON CONFLICT DO NOTHING по event_id + reason)

### Elo (Вариант 2)
- [ ] Новый пользователь начинает с рейтинга 1000
- [ ] Победа над Hard quiz (score ≥ 70%) даёт существенный прирост рейтинга
- [ ] Провал Easy quiz снижает рейтинг
- [ ] Рейтинг не опускается ниже 100
- [ ] История изменений рейтинга доступна через API

### Streak (Вариант 4)
- [ ] Ответ в новый день увеличивает стрик
- [ ] Пропуск дня (UTC) обнуляет текущий стрик, longest_streak не меняется
- [ ] Несколько ответов в один день не увеличивают стрик дважды

### Weekly (Вариант 5)
- [ ] leaderboard_daily обновляется синхронно при обработке SessionCompleted
- [ ] Materialized views обновляются раз в час без блокировки чтения
- [ ] Новичок с высоким результатом за неделю попадает в топ

### Skill Rating (Вариант 6)
- [ ] Рейтинг по DECLENSIONS не влияет на рейтинг по VOCABULARY
- [ ] Лидерборд "Top Declension Experts" показывает только пользователей с ≥3 сессиями

### API
- [ ] GET /api/v1/leaderboard возвращает `currentUserEntry` даже если пользователь не в топ-50
- [ ] Параметр `groupId` фильтрует лидерборд по участникам группы
- [ ] Доступ к лидерборду группы запрещён для не-участников (403)

---

## 13. Открытые вопросы

- [ ] Какой алгоритм(ы) включать в MVP? Рекомендация: XP + Weekly + Skill Rating
- [ ] Timezone для стрика: хранить в профиле пользователя или всегда UTC?
- [ ] Sandhi как отдельный QuizType для Skill Rating?
- [ ] Achievement bonus в Composite Score — когда реализовывать систему ачивок?
- [ ] Нужен ли Elo в MVP или достаточно XP?
- [ ] Отображение delta ("↑ +320 за сегодня") — хранить или вычислять на лету?
