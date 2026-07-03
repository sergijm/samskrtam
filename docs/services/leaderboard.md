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

**Правила:** правильный ответ = +10 XP, неправильный = 0, стрик 10 подряд = +50, perfect quiz = +100, hard quiz = ×1.5 к XP за ответы.

**Таблицы:** `xp_ledger` (id, event_id, user_id, amount, reason, occurred_at), `xp_totals` (user_id PK, total_xp, last_updated).

**Алгоритм:** При `SessionCompleted`: 1) XP за ответы: correctAnswers × 10 (×1.5 если HARD); 2) Perfect бонус: +100 если correctAnswers = totalQuestions; 3) Streak бонус: +50 если streak % 10 == 0; 4) Запись в ledger идемпотентно; 5) Инкремент xp_totals.

### Лидерборд

Глобальный TOP XP: сортировка `xp_totals.total_xp DESC`, JOIN с `users`, LIMIT 50.
Внутри группы: дополнительный JOIN с `group_members` по `groupId`.

---

## 3. Elo Rating

Пользователь "играет" против квиза. Начальный рейтинг: 1000. Easy quiz = 800, Medium = 1200, Hard = 1600. K = 32.

**Формула:** E = 1 / (1 + 10^((quizRating - userRating) / 400)), S = correctAnswers / totalQuestions, delta = K × (S - E), newRating = max(100, userRating + delta).

**Таблицы:** `elo_ratings` (user_id PK, current_rating, peak_rating, total_games, last_updated), `elo_history` (id, event_id, user_id, delta, rating_after, occurred_at).

**Алгоритм:** При SessionCompleted: получить/создать рейтинг, определить quizRating, вычислить delta, обновить. Лидерборд: current_rating DESC, total_games >= 5, LIMIT 50.

---

## 4. Accuracy Ranking

accuracy = correct_answers / total_answers. Минимальный порог: 500 ответов.

**Таблица:** `accuracy_cache` (user_id PK, total_answers, correct_count, accuracy NUMERIC(5,4), last_updated).

**Лидерборд:** accuracy DESC, total_answers >= 500, LIMIT 50. Рекомендуется как вторичная метрика.

---

## 5. Streak Leaderboard

Количество календарных дней подряд с хотя бы одним ответом. Часовой пояс: UTC.

**Таблица:** `user_streaks` (user_id PK, current_streak, longest_streak, last_activity_at DATE).

**Алгоритм:** При AnswerSubmitted: today = UTC дата; если daysSinceLast == 0 — ничего; если == 1 — increment; если пропуск — currentStreak = 1. Лидерборд: current_streak DESC, LIMIT 50.

---

## 6. Weekly / Monthly Leaderboard (CQRS)

Лидерборд за 7/30/365 дней. Write side — xp_ledger, read side — денормализованные таблицы.

**Таблицы:** `leaderboard_daily` (user_id, day, xp_earned, PK: (user_id, day)), `leaderboard_weekly` (materialized view, weekly_xp), `leaderboard_monthly` (materialized view, monthly_xp).

**Обновление:** daily — синхронно при SessionCompleted; weekly/monthly — через @Scheduled раз в час с REFRESH MATERIALIZED VIEW CONCURRENTLY.

---

## 7. Skill Rating

Отдельный Elo-рейтинг по каждой категории санскрита: DECLENSIONS, CONJUGATIONS, VOCABULARY.

**Таблица:** `skill_ratings` (user_id, quiz_type, rating, total_sessions, accuracy, last_updated, PK: (user_id, quiz_type)).

**Алгоритм:** тот же Elo, но отдельно по каждому lessonType. При SessionCompleted: найти/создать запись, вычислить delta, обновить rating и accuracy (скользящее среднее). Лидерборды: rating DESC, total_sessions >= 3, LIMIT 20.

---

## 8. Composite Score

Score = XP + 50 × current_streak + difficulty_bonus (+200 за каждый Hard квиз с score ≥ 70%) + achievement_bonus (+500 за ачивку, будущее).

**Таблица:** materialized view `composite_scores` (user_id, composite_score). Обновляется раз в час вместе с leaderboard_weekly.

---

## 9. Группы — лидерборд внутри группы

Все алгоритмы поддерживают фильтрацию по группе через JOIN с group_members. API: `GET /api/v1/leaderboard?type=xp&period=weekly&groupId=<uuid>`. Параметры: type (xp|elo|accuracy|streak|skill|composite), period (all|weekly|monthly), lessonType (для skill), groupId, limit (1-100, default 50).

**Response:** унифицированный DTO: type, period, groupId, entries[{rank, userId, username, value, valueLabel, isCurrentUser, delta}], currentUserEntry (всегда возвращается, даже если не в топе).

---

## 10. Backend структура

Пакет `leaderboard/`: LeaderboardController, LeaderboardService, LeaderboardResponse, LeaderboardEntry.
Пакет `algorithm/`: XpService, EloService, EloCalculator, AccuracyService, StreakService, SkillRatingService, CompositeScoreService.
Пакет `scheduler/`: LeaderboardRefreshScheduler.

`recordSession(SessionCompleted)`: сохранить session_records, вызвать xpService, eloService, skillRatingService.
`recordAnswer(AnswerSubmitted)`: сохранить answer_records, вызвать streakService, accuracyService.

---

## 11. Flyway миграции: V1 — schema statistics, V2 — answer_records, V3 — session_records, V4 — quiz.outbox_events, V5 — xp_ledger/xp_totals, V6 — elo_ratings/elo_history, V7 — user_streaks, V8 — leaderboard_daily/views, V9 — skill_ratings, V10 — composite_scores view.

---

## 12. Acceptance Criteria

XP: правильный ответ = 10 XP, hard ×1.5, perfect +100, streak 10 +50, идемпотентность. Elo: старт 1000, не ниже 100, история доступна. Streak: инкремент при новом дне, обнуление при пропуске. Weekly: daily синхронно, views раз в час. Skill Rating: независимость категорий, порог ≥3 сессий. API: currentUserEntry всегда, groupId фильтр, 403 для не-участников.

---

## 13. Открытые вопросы

- Какой алгоритм(ы) в MVP? Рекомендация: XP + Weekly + Skill Rating.
- Timezone для стрика: хранить в профиле или UTC?
- Sandhi как отдельный LessonType для Skill Rating?
- Achievement bonus — когда система ачивок?
- Нужен ли Elo в MVP или достаточно XP?
