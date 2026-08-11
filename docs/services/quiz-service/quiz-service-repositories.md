# quiz-service: Репозитории и хранение данных

> Часть спецификации quiz-service. Основной файл: `docs/services/quiz-service.md`

---

## 1. Общая схема хранения

PostgreSQL с R2DBC, схема `quiz`. Все репозитории — ReactiveCrudRepository.

## 2. QuizSessionRepository

Хранит активные и завершённые сессии.

Поля:
- id (UUID)
- user_id (UUID)
- lesson_id (UUID, null для composed)
- lesson_type (VARCHAR, null для composed)
- total_questions (INT)
- answered_questions (INT)
- score (INT)
- status (VARCHAR: IN_PROGRESS / COMPLETED)
- started_at (TIMESTAMP WITH TIME ZONE)
- completed_at (TIMESTAMP WITH TIME ZONE)
- vocabulary_words_json (TEXT, null для composed)
- progress_tag_set_id (VARCHAR, null)

## 3. QuizAnswerRepository

Хранит ответы пользователя на вопросы.

Поля:
- id (UUID)
- session_id (UUID, FK на quiz_session)
- question_id (UUID)
- selected_option_id (UUID)
- is_correct (BOOLEAN)
- response_time_ms (INT)
- answered_at (TIMESTAMP WITH TIME ZONE)
- selected_form_iast (VARCHAR)
- correct_form_iast (VARCHAR)

selected_option_id — id варианта ответа с детерминированным id, присвоенным при старте. Для MATCHING — проверка через matchSubmissions, а не через selected_option_id.

## 4. SessionQuestionRepository

Единственное персистентное хранилище вопросов сессии. Пишется на start (из ответа curriculum compose), читается на resume/answer/complete.

Поля:
- id (UUID)
- session_id (UUID, FK на quiz_session)
- question_id (UUID)
- question_number (INT)
- text (TEXT)
- item_type (VARCHAR — DECLENSION_FORM, VOCABULARY_WORD, etc.)
- answer_mode (VARCHAR — FREE_TEXT, SINGLE_CHOICE, MATCHING)
- correct_answer (VARCHAR, null для MATCHING)
- options (JSON — зафиксированный при старте список опций)
- payload (JSON — типоспецифичные данные, например MATCHING пары)
- topic_code (VARCHAR)
- progress_tag (VARCHAR — caseType|numberType|gender или formIast)
- question_type (VARCHAR — MULTIPLE_CHOICE, MATCHING, FREE_TEXT)

## 5. OutboxEventRepository

См. quiz-service-kafka.md.

## 6. QuizItemScoreRepository — источник статуса и сводки LessonPage (architecture.md §3.6)

Единая таблица `quiz.quiz_item_score` (составной ключ userId+itemType+progressTag, поле score 0–100, nextReviewAt) — единственный источник статуса QuizItem и `LessonStatusSummary` на LessonPage.

Правила статусов — единый порог `masteredLowerThreshold=90` для VOCABULARY_WORD и DECLENSION_FORM:
- нет строки → NEW
- score < 90 → LEARNING
- score >= 90 → MASTERED (DUE — внутренний атрибут отбора внутри сета MASTERED по
  `nextReviewAt <= now`, статусом не показывается)
- DIFFICULT — ортогональная ось, независимая от статуса: `consecutiveMistakes >= 2` ИЛИ
  `score <= difficultUpperThreshold`; выход с гистерезисом `difficultExitMargin`

Подробности формулы score/stability — [quiz-generator-spec.md](quiz-generator-spec.md) §2.5.