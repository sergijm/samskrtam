# Архитектурные решения (ADR)

> Все ADR проекта SamskrtamApp, вынесенные из [conventions.md](./conventions.md) для соблюдения лимита 350 строк.
> Связанные файлы: [conventions.md](./conventions.md) · [quiz-generator-spec.md](./quizzes/quiz-generator-spec.md)

---

## ADR-001: Разделение auth между Gateway и user-service

**Gateway** → OAuth2/OIDC (login ROPC, refresh, logout, Google/Mail.ru редиректы, Authorization Code flow). **user-service** → жизненный цикл аккаунта (регистрация, восстановление/смена пароля, верификация email, invite). Граница: OAuth2 протокол — Gateway, бизнес-логика аккаунта — user-service через Keycloak Admin API.

## ADR-002: Семантика Quiz vs Lesson vs Activity

**Lesson** = единица контента (склонение, словарный урок). **Quiz** = выборка вопросов из урока на сессию. **QuizSession** = прохождение квиза (не переименовывается). **Activity** = будущая абстракция (M5+). Следствие: `QuizRepository`/`QuizContentService` → `LessonRepository`/`LessonContentService`; `quizId` в статистике → `lessonId`; Kafka-топики и роут `/api/v1/quiz/` не меняются; `QuizListItemResponse` удалён.

## ADR-003: Хранение окончаний склонений в БД

Таблица `case_endings (vowel_type, gender, case_type, number_type, ending)` — эталон падежных окончаний. Ключ: (vowel_type, gender, case_type, number_type). Для уроков без родового различия (declensions-i/u/r) gender = UNSPECIFIED. Quiz-service читает окончание по ключу при генерации вопроса; проверка ответа — прямое сравнение.

## ADR-004: Формирование вопросов для уроков с двумя родами

Уроки declensions-i/u/r (два рода, одинаковые окончания) — 24 вопроса (8 caseType × 3 numberType), gender = UNSPECIFIED. Уроки с одним родом (declensions-a-masc/neut/fem, declensions-i-long/u-long) — поля gender обязательно, тоже 24 вопроса.

## ADR-005: Единство окончаний для основ -i, -u, -ṛ независимо от рода

Для vowel_type I/I_LONG/U/U_LONG/R окончания одинаковы для всех родов. Прогресс агрегируется раздельно по gender (разные основы/слова). Хранение: либо одна запись с gender = UNSPECIFIED, либо дублирующие строки с разным gender — на усмотрение Агента 2. Агент 3 получает одинаковый caseEnding, но агрегирует прогресс раздельно.

## ADR-006: sangraha-service — произведения, LLM-анализ стихов, синхронизация лексики через Kafka

Новый сервис `sangraha-service` (Java 21, Virtual Threads, схема `sangraha`). LLM-анализ стиха (OpenAI-совместимый) — строго через tool calling (`submit_verse_analysis`), без парсинга свободного текста. Никаких синхронных HTTP-вызовов к content-service/dictionary-service — только Kafka, topic `sangraha-vocabulary-events` (transactional outbox). Иерархия work.slug → chapter.slug маппится на VocabularyCategory.code в content-service для бесплатного VOCABULARY-квиза. Дедупликация слов по (wordIast, stem). Версионирование analysis не хранится (перезапись). Права: write — только ADMIN. Порт: 8089.

## ADR-007: Единая таблица прогресса quiz_item_score, отсутствие FK на content, производный статус без time-decay

**Контекст:** На момент принятия решения прогресс по квизам хранился в двух отдельных таблицах:
- `quiz.word_score` (user_id, word_id, lesson_id → score) — для лексических квизов
- `quiz.grammar_form_score` (user_id, lesson_id, gender, case_type, number_type → score) — для грамматических квизов

Обе таблицы не поддерживали SRS-планирование (nextReviewAt) и не имели единого алгоритма отбора вопросов. Необходим универсальный генератор, инвариантный к типу квиза.

**Решение:**

**Единая таблица `quiz.quiz_item_score`:** Вместо двух отдельных таблиц — одна таблица с составным ключом (user_id, item_type, external_ref_id). item_type — перечисление (VOCABULARY_WORD, DECLENSION_FORM и др., открытое для расширения). external_ref_id — UUID сущности в content-service без физического FK. Таблица включает поля: id, user_id, item_type, external_ref_id, score (0–100), stability, last_answered_at, last_mistake_at, consecutive_mistakes, next_review_at, updated_at.

**Отсутствие физических FK на content:** quiz-service и content-service — разные микросервисы с разными схемами БД. Физические FK между схемами quiz и content запрещены. Целостность внешних ссылок обеспечивается на уровне приложения (ContentClient проверяет существование external_ref_id) и эвентуально (soft-delete в content-service как предпочтительный механизм; событие удаления — альтернатива, открытый вопрос).

**Производный статус без time-decay:** Статус (NEW/LEARNING/DIFFICULT/MASTERED) не хранится в таблице — вычисляется из текущего score при чтении. Правила: нет строки → NEW; score ≤ difficultUpperThreshold → DIFFICULT; между difficultUpperThreshold и masteredLowerThreshold → LEARNING; score ≥ masteredLowerThreshold → MASTERED. Time-decay не реализуется — score не "тает" со временем. Забывание проявляется через формулу score (§2.5 quiz-generator-spec.md) при следующей ошибке после контрольного показа.

**Обновление 2026-07 (LessonPage status summary):** старая successRate-модель статуса (`ProgressConstants.MASTERY_THRESHOLD`/`GRAMMAR_LEARNING_THRESHOLD`, nSuccess/nAll) удаляется из quiz-service; статус слов/вопросов урока вычисляется только из `quiz_item_score.score`. Единое финальное значение `masteredLowerThreshold = 90` для обоих itemType (VOCABULARY_WORD и DECLENSION_FORM) — калибровочная оговорка "черновые значения" снята для этого порога. `difficultUpperThreshold` остаётся открытым (см. §6 quiz-generator-spec.md, бакет DIFFICULT не задействован в сводке LessonPage — см. ниже про REVIEW).

**Бакет REVIEW (только для отображения на LessonPage, не заменяет DIFFICULT):** единица со статусом MASTERED (score ≥ 90), у которой `nextReviewAt ≤ now` (просрочен контрольный показ, см. `masteredCooldown` §3 quiz-generator-spec.md), отображается пользователю как REVIEW вместо MASTERED. Это единственное место, где производный статус зависит от времени — точечное исключение из "без time-decay" (§2.4), затрагивающее только UI-статус, не сам score/бакет генератора: для генератора и для алгоритма отбора (§4 quiz-generator-spec.md) единица остаётся в бакете MASTERED (due-проверка по masteredCooldown уже существует независимо). Наличие ≥1 единицы в REVIEW включает кнопку «Повторить» на LessonPage.

**Прогресс склонений общий для всех основ с одинаковым (vowel_type, gender, case_type, number_type):** external_ref_id для DECLENSION_FORM ссылается на content.case_endings.id (эталонную связку vowel_type+gender+case_type+number_type), а не на конкретную основу. Все основы с одинаковым сочетанием vowel_type/gender/case_type/number_type разделяют один прогресс. Это консолидирует решение ADR-005: прогресс не дублируется по gender там, где окончания одинаковы.

**Статус:** Принято.

**Последствия:**
- Старые таблицы quiz.word_score и quiz.grammar_form_score удаляются, данные мигрируются в quiz.quiz_item_score. Миграция — отдельная задача Агента 2.
- Таблица quiz.quiz_item_score — единственное хранилище прогресса в quiz-service; ответы пользователя по-прежнему в quiz.quiz_answers (агрегация nSuccess/nAll — оттуда).
- Алгоритм отбора вопросов (generate()) — единый для всех itemType, без ветвлений по типу.
- Планирование next_review_at — отдельная формула SRS-интервалов, не спроектирована (открытый вопрос, не блокирует принятие ADR).
- Конкретные значения difficultUpperThreshold (по умолчанию 45) и masteredLowerThreshold (по умолчанию 80) — черновые, калибруются на реальных данных.