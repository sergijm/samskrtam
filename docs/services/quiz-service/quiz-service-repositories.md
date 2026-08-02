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
- quiz_id (UUID)
- quiz_type (VARCHAR)
- total_questions (INT)
- answered_questions (INT)
- score (INT)
- status (VARCHAR: IN_PROGRESS / COMPLETED)
- started_at (TIMESTAMP WITH TIME ZONE)
- completed_at (TIMESTAMP WITH TIME ZONE)
- vocabulary_words_json (TEXT)
- generated_quiz_data_id (UUID, внутренний group-id quiz-service, не внешний FK)

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

selected_option_id — id варианта ответа, присвоенный на лету, не ссылается на персистентную таблицу опций. Проверка правильности идёт по строковому сравнению selected_form_iast / correct_form_iast.

Репозиторий содержит нативные @Query-методы с JOIN на session_questions / quiz_session для статистики:
- findByWordIdAndUserIdAndLessonId
- countByWordIdAndUserIdAndLessonId
- calculateWordScore
- findGrammarHistory
- countGrammarHistory

## 4. SessionQuestionRepository

Единственное персистентное хранилище вопросов сессии во всей системе. Пишется на start (из ответа generate-quiz-data), читается на resume/answer/complete, а также через JOIN из QuizAnswerRepository для статистики.

Поля:
- id (UUID)
- session_id (UUID, FK на quiz_session)
- question_id (UUID)
- question_number (INT)
- text (TEXT)
- explanation_ru (TEXT)
- explanation_en (TEXT)
- declension_stem_id (UUID)
- stem_devanagari (VARCHAR, из content-service DeclensionStem.stemDevanagari)
- stem_translation_ru (VARCHAR, из content-service DeclensionStem.translationRu)
- stem_translation_en (VARCHAR)
- target_case (VARCHAR)
- target_number (VARCHAR)
- target_gender (VARCHAR)
- correct_form_iast (VARCHAR)
- correct_form_devanagari (VARCHAR)
- vocabulary_word_id (UUID)
- question_source_language (VARCHAR)
- question_target_language (VARCHAR)
- correct_translation_ru (VARCHAR)
- correct_translation_en (VARCHAR)

## 5. OutboxEventRepository

См. quiz-service-kafka.md.

## 6. Генерация вариантов ответа (дистракторы) — generate-on-read, не персистится

DeclensionOptionGeneratorService (для DECLENSIONS/CONJUGATIONS) и LexicalOptionGeneratorService (для VOCABULARY) генерируют варианты ответа заново при каждом обращении к вопросу (start/resume): запрашивают у content-service все формы основы (getDeclensionForms) или слова урока, выбирают до 3 дистракторов, шафлят и присваивают UUID. Ничего не сохраняется.

Последствие: при повторном рендере ещё не отвеченного вопроса набор неправильных вариантов может отличаться от предыдущего показа. Открытый вопрос: сидировать Random от questionId для стабильности.

## 7. Word Score Calculation (On-the-fly) — только для колонки «Попытки»

Отдельная таблица word_statistics удалена. Score слова вычисляется на лету через SQL-запрос с JOIN на quiz_answers, quiz_session, session_questions:

SELECT COUNT(*), SUM(CASE WHEN is_correct THEN 1 ELSE 0 END), MAX(answered_at) FROM quiz.quiz_answers qa JOIN quiz.quiz_session qs ON qa.session_id = qs.id JOIN quiz.session_questions sq ON qa.question_id = sq.id WHERE qs.user_id = :userId AND qs.quiz_id = :quizId AND sq.vocabulary_word_id = :wordId

Запрос выполняется асинхронно через R2DBC @Query в UserSessionService. Используется в LessonServiceImpl только для nSuccess/nAll/successRate (колонка «Попытки», WordHistoryDialog) — **не** для вычисления статуса NEW/LEARNING/MASTERED/REVIEW (см. §8).

## 8. QuizItemScoreRepository — источник статуса и сводки LessonPage (architecture.md §3.6)

Единая таблица `quiz.quiz_item_score` (составной ключ userId+itemType+externalRefId, поле score 0–100, nextReviewAt) — единственный источник статуса QuizItem и `LessonStatusSummary` на LessonPage. Заменяет удалённые word_score/grammar_form_score и связанные с ними WordScoreRepository/GrammarFormScoreRepository как источник статуса (successRate-модель отменена, см. architecture.md §3.6).

Метод `findByUserIdAndItemTypeAndExternalRefIdIn` — джойн списка externalRefId урока с прогрессом пользователя одним запросом (для отображения статуса каждого QuizItem). Метод `countLearnedItems(userId, itemType, minScore=90)` — используется для `learnedWords`/`learnedQuestions` на плитке урока (LessonItemDto) и для поля `mastered` в LessonStatusSummary.

Правила бакетов — единый порог `masteredLowerThreshold=90` для VOCABULARY_WORD и DECLENSION_FORM:
- нет строки → NEW
- score < 90 → LEARNING
- score >= 90 и nextReviewAt > now → MASTERED
- score >= 90 и nextReviewAt <= now → REVIEW (подмножество MASTERED по времени, не по score; см. openapi/schemas/vocabulary.yaml#WordStatus)

Подробности формулы score/stability — [quiz-generator-spec.md](../../quizzes/quiz-generator-spec.md) §2.5.