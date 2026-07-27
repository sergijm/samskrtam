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

## ADR-006: sangraha-service — произведения, LLM-анализ стихов, синхронизация лексики через REST (было — через Kafka)

Новый сервис `sangraha-service` (Java 21, Virtual Threads, схема `sangraha`). LLM-анализ стиха (OpenAI-совместимый) — строго через tool calling (`submit_verse_analysis`), без парсинга свободного текста. **ИЗМЕНЕНО:** синхронизация лексики с content-service изначально была спроектирована только через Kafka (topic `sangraha-vocabulary-events`, transactional outbox, без синхронных HTTP-вызовов) — по факту эксплуатации признано избыточным для канала «один producer, один consumer» и заменено на прямой синхронный REST-вызов `POST content-service/content/internal/sangraha/vocabulary`. Transactional Outbox в sangraha-service **сохранён** как паттерн надёжной доставки (retry/backoff) — изменился только транспорт Relay (HTTP вместо Kafka producer), см. `sangraha-service.md` §6, `content-service.md` §11. Иерархия work.slug → chapter.slug маппится на VocabularyCategory.code в content-service для бесплатного VOCABULARY-квиза. Дедупликация слов по (wordIast, stem). Версионирование analysis не хранится (перезапись). Права: write — только ADMIN. Порт: 8089.

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

## ADR-008: Местоимения — через существующий itemType DECLENSION_FORM, без нового PRONOUN_FORM

**Контекст:** курикулум (`information-architecture/01-curriculum-vs-catalog.md` §2.1.3) требует пять уроков местоимений (личные, указательные, вопросительные, относительные, возвратное). `quiz-generator-spec.md` §2.1 заранее резервировал `PRONOUN_FORM` как возможное новое значение itemType под эту задачу.

**Решение:** новый itemType не заводится. Местоимения реализуются как дополнительные значения `vowelType` (`PRON_AHAM`, `PRON_TVAM`, `PRON_TAD`, `PRON_ETAD`, `PRON_IDAM`, `PRON_KIM`, `PRON_YAD`) в уже существующих таблицах `content.declension_stems`/`content.declension_forms`/`content.case_endings`, itemType остаётся `DECLENSION_FORM`, external_ref_id — по-прежнему `case_endings.id` (см. ADR-007).

**Обоснование:**
- `declension_forms` хранит готовые словоформы (не суффикс+основа), поэтому супплетивные парадигмы (aham→mama→mahyam, tad/etad/idam/kim/yad) укладываются в модель без изменений.
- Личные местоимения (aham/tvam) не различают род — `gender = UNSPECIFIED`, тот же паттерн, что уже принят для i/u/ṛ-основ (ADR-004/005).
- Указательные/вопросительные/относительные (tad/etad/idam/kim/yad) различают 3 рода — тот же паттерн, что a-основы одного рода (ADR-004), просто с тремя стемами вместо одного на класс.
- Для форм без вычленяемого окончания (aham/tvam) `case_endings.ending` хранит форму целиком; при единственной форме в группе омонимии вес `ENDING_MATCH` обнуляется автоматически существующим алгоритмом (`quiz-declension.md` §4.5, шаг 2) — отдельной ветки кода не требуется.
- Соблюдается инвариант §5 `quiz-generator-spec.md`: алгоритм отбора и таблица `quiz.quiz_item_score` не меняются, новый материал — это только данные в content-service.

**Явно вне контракта первой итерации:** энклитические формы личных местоимений (me/te/nau/vaḥ и т.д.) и несклоняемое `svayam` — не входят в 24 стандартные словоформы DECLENSION_FORM, квизом не покрываются, статические заметки на странице урока (реализация — Агент 2/3).

**Статус:** Принято.

**Последствия:**
- `openapi/content/schemas/content.yaml#VowelType` расширен семью значениями.
- Новая Flyway-миграция в content-service: расширение enum + сид `declension_stems`/`declension_forms`/`case_endings` + 5 новых `Quiz(lessonType=DECLENSIONS)` (`pronouns-personal`, `pronouns-demonstrative`, `pronouns-interrogative`, `pronouns-relative`, `pronouns-reflexive`) — задача Агента 2.
- `pronouns-reflexive` (ātman) дублирует парадигму существующего `declensions-a-masc` под отдельным slug — сознательный выбор в пользу независимости уроков, а не переиспользования/связи между Quiz.
- quiz-service, генератор вопросов, quiz_item_score — без изменений.

## ADR-009: Лексический квиз по стиху — on-demand по кнопке «Изучить», без Outbox/автосинхронизации

**Контекст:** ADR-006 ввёл синхронный REST-вызов `sangraha-service → content-service` вместо Kafka, но сохранил Transactional Outbox — синхронизация лексики происходила автоматически после **каждого** анализа стиха, независимо от того, нужен ли этот квиз кому-либо из пользователей. Квиз при этом заводился на уровне произведения (`slug = workSlug`), агрегируя слова всех стихов через `VocabularyCategory`.

**Решение:**
- **Outbox убран целиком.** Таблица `outbox_events` и `OutboxRelayService` в sangraha-service удаляются (новая миграция `DROP TABLE`, см. `sangraha-service.md` §3). Анализ стиха (`§5.1`) больше не пишет `OutboxEvent` и вообще не инициирует никакого обращения к content-service.
- **Синхронизация — по явному клику пользователя.** Кнопка «Изучить» на VersePage вызывает `POST /verses/{verseId}/vocabulary-quiz` (sangraha-service). Если по этому стиху уже был клик — возвращается закэшированный `verse.vocabularyQuizSlug` без обращения к content-service. Иначе sangraha-service синхронно (в рамках того же HTTP-запроса) вызывает `content-service`, получает `quizSlug` и кэширует его.
- **Квиз — на уровне стиха, а не произведения.** `Quiz.slug = "sangraha-verse-{verseId}"`, детерминирован (идемпотентность вместо ретраев). Это отменяет прежнее решение «Quiz только на уровне произведения» (было в `sangraha-service.md` §8 до этого ADR).
- **`VocabularyCategory` (work→chapter) сохраняется без изменений** — это общий механизм тематической классификации лексики (см. `information-architecture.md` §2.3 «Лексика»), не специфичный для sangraha; произведения по-прежнему используют его для группировки слов по темам «произведение/глава», независимо от того, в каком именно квизе (по стиху) физически находится слово.
- Слово может одновременно входить и в свой квиз-по-стиху, и в тематическую категорию произведения/главы — это ортогональные связи (`VocabularyWord` ↔ `Quiz` через прямую привязку, `VocabularyWord` ↔ `VocabularyCategory` через `VocabularyWordCategory`).

**Статус:** Принято.

**Последствия:**
- `sangraha-service`: новая колонка `verses.vocabulary_quiz_slug`, новая миграция `DROP TABLE outbox_events`, удаление `OutboxRelayService`, новый эндпоинт `POST /verses/{verseId}/vocabulary-quiz`.
- `content-service`: эндпоинт `§11` переименован (`/vocabulary` → `/vocabulary-quiz`), контракт запроса/ответа изменён (ответ — `{ quizSlug }` вместо списка `vocabularyWordId`), `VocabularyCategory` не меняется.
- Открытые вопросы «квиз на уровне работы/главы/обоих» и «политика ретраев Outbox» (были в `sangraha-service.md` §8 и `content-service.md` §11) закрыты этим ADR — сняты, а не решены в старой постановке.

**Обновление (текущая итерация):** ответ content-service дополнен `quizId` (UUID `Lesson`) и `quizStatus` (`CREATED`/`EXISTING`). Фронтенд стартует сессию квиза сразу по `quizId` через `POST /quiz/vocabulary/sessions/start-or-resume?lessonId={quizId}&statusFilter=...` — убран промежуточный `GET /lessons/vocabulary/{slug}`, который был не нужен для запуска сессии и оставался только по инерции с `VocabularyLessonPage`. `quizStatus` определяет `statusFilter` при первом старте (`CREATED` → `NEW`, `EXISTING`/кэш-хит → без фильтра) — единственный сигнал, который content-service может дать, не имея доступа к пользовательскому прогрессу (тот принадлежит quiz-service). `quizSlug` сохраняется в ответе и кэше `Verse` — нужен для построения URL `/quiz/vocabulary/{quizSlug}/{sessionId}` после старта, сам по себе для запуска сессии больше не используется.