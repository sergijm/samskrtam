# quiz-service: Миграции базы данных

> Часть спецификации quiz-service. Основной файл: `docs/services/quiz-service.md`

Все миграции объединены в один стартовый файл V1__combined_schema.sql. Ниже — описание каждой таблицы схемы `quiz`.

## 1. quiz.quiz_session

- id (UUID, PK)
- user_id (UUID)
- quiz_id (UUID)
- quiz_type (VARCHAR(50))
- total_questions (INT)
- answered_questions (INT)
- score (INT)
- status (VARCHAR(50))
- started_at (TIMESTAMP WITH TIME ZONE)
- completed_at (TIMESTAMP WITH TIME ZONE)
- vocabulary_words_json (TEXT)
- generated_quiz_data_id (UUID, внутренний group-id, не внешний FK)

## 2. quiz.quiz_answers

- id (UUID, PK)
- session_id (UUID, FK на quiz.quiz_session)
- question_id (UUID)
- selected_option_id (UUID)
- is_correct (BOOLEAN)
- response_time_ms (INT)
- answered_at (TIMESTAMP WITH TIME ZONE)
- selected_form_iast (VARCHAR(255))
- correct_form_iast (VARCHAR(255))

selected_option_id — временный id, присвоенный на лету, не FK. Проверка правильности по selected_form_iast/correct_form_iast.

## 3. quiz.session_questions

Единственное персистентное хранилище вопросов сессии во всей системе (итоговая версия после нескольких итераций).

- id (UUID, PK)
- session_id (UUID, FK на quiz.quiz_session)
- question_id (UUID)
- question_number (INT)
- text (TEXT)
- explanation_ru (TEXT)
- explanation_en (TEXT)
- declension_stem_id (UUID)
- stem_devanagari (VARCHAR(255), из curriculum-service DeclensionStem.stemDevanagari)
- stem_translation_ru (VARCHAR(255), из curriculum-service DeclensionStem.translationRu)
- stem_translation_en (VARCHAR(255))
- target_case (VARCHAR(50))
- target_number (VARCHAR(50))
- target_gender (VARCHAR(50))
- correct_form_iast (VARCHAR(255))
- correct_form_devanagari (VARCHAR(255))
- vocabulary_word_id (UUID)
- question_source_language (VARCHAR(50))
- question_target_language (VARCHAR(50))
- correct_translation_ru (VARCHAR(255))
- correct_translation_en (VARCHAR(255))

Новые поля (добавлены в V2): stem_devanagari, stem_translation_ru, stem_translation_en.

## 4. quiz.outbox_events

- id (UUID, PK)
- aggregate_type (VARCHAR(255))
- aggregate_id (VARCHAR(255))
- event_type (VARCHAR(255))
- payload (TEXT)
- created_at (TIMESTAMP WITH TIME ZONE)
- status (VARCHAR(255))
- error_message (TEXT)
- retry_count (INTEGER)
- processed_at (TIMESTAMP WITH TIME ZONE)