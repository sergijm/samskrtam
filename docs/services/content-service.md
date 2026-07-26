# content-service

> Домен: Lesson Content — настройки и содержание уроков (см. ADR-002)
> Язык: **Java 21 + Virtual Threads**
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

| LessonType | Описание |
|---|---|
| `DECLENSIONS` | Квиз по падежным формам санскрита |
| `CONJUGATIONS` | Квиз по спряжениям глаголов |
| `VOCABULARY` | Квиз по лексике (slug-based) |

---

## 3. Сущности

**Quiz** (таблица quizzes): id (UUID), slug (string, unique), titleRu, titleEn, descriptionRu, descriptionEn, lessonType (DECLENSIONS|CONJUGATIONS|VOCABULARY), difficulty (BEGINNER|INTERMEDIATE|ADVANCED), questionsPerSession (int, default 10), createdAt, deletedAt

**Question** (таблица questions): id (UUID), quizId (UUID), textRu (TEXT), textEn (TEXT), explanationRu (TEXT), explanationEn (TEXT), correctOptionId (UUID), deletedAt

**QuestionOption** (таблица question_options): id (UUID), questionId (UUID), textRu, textEn

**VocabularyWord** (таблица vocabulary_words, только для VOCABULARY квизов): id (UUID), quizId (UUID), word (IAST), wordDevanagari, translationRu, translationEn, partOfSpeech, example

**DeclensionStem** (таблица `content.declension_stems`, для DECLENSIONS квизов; **отсутствовала в этом документе — добавлено**): id (UUID), stemIast, stemDevanagari (колонка существует в БД, но не заполняется миграцией-сидом V2 — данных нет), vowelType (A_STEM|AA_STEM|I_STEM|II_STEM|U_STEM|UU_STEM|R_STEM|PRON_AHAM|PRON_TVAM|PRON_TAD|PRON_ETAD|PRON_IDAM|PRON_KIM|PRON_YAD — семь новых значений для местоимений, см. ADR-008), gender.
**NEW (местоимения, ADR-008):** `PRON_AHAM`/`PRON_TVAM` — `gender = UNSPECIFIED` (личные местоимения рода не различают, как i/u/ṛ-основы, ADR-004/005). `PRON_TAD`/`PRON_ETAD`/`PRON_IDAM`/`PRON_KIM`/`PRON_YAD` — по 3 стема на класс (MASCULINE/FEMININE/NEUTER), как a-основы одного рода. `case_endings.ending` для супплетивных форм (aham/tvam) хранит словоформу целиком, а не вычленяемый суффикс — генератор это переживает без изменений кода (см. ADR-008).
**NEW (задача Агенту 2, см. ниже):** добавить `translationRu`, `translationEn` — сейчас перевода основы нет вообще ни в БД, ни в entity.

**DeclensionForm** (таблица `content.declension_forms`): PK (declensionStemId, caseType, numberType), formIast, formDevanagari — уже заполнены (сид из `raw_data.sanskrit_declensions_enriched`).

---

## 4. Flyway Migrations

6 миграций Flyway: V1 — schema content; V2 — таблица quizzes; V3 — questions; V4 — question_options (FK на correct_option_id после создания); V5 — vocabulary_words; V6 — seed начальных VOCABULARY квизов (animals, numbers, body-parts, nature, 1, 2).

`declension_stems`/`declension_forms` создаются и заполняются отдельно, в `V2__init_grammar_quizzes.sql` (сид из `raw_data.sanskrit_declensions_enriched`), не входят в перечисление выше — **несоответствие в этом документе, зафиксировано, не исправляется в рамках текущей задачи**.

**NEW, требуется новая миграция (Агент 2):** `ALTER TABLE content.declension_stems ADD COLUMN translation_ru VARCHAR(255), ADD COLUMN translation_en VARCHAR(255);` + data-fix UPDATE для заполнения `stem_devanagari` и новых `translation_ru/en` по всем текущим строкам таблицы — данные предоставляет пользователь (см. §9).

**NEW, требуется миграция на удаление (Агент 2):** `DROP TABLE content.generated_questions; DROP TABLE content.generated_quiz_data;` — таблицы persist-хранения сгенерированных вопросов сессии удаляются, т.к. это дублировало `quiz.session_questions` (см. §3а и quiz-service.md §12). Порядок важен — сначала дочерняя таблица (FK на generated_quiz_data_id).

---

## 5. API


> Полная OpenAPI спецификация для Lesson Pages: [lesson-aggregation-openapi.yaml](../openapi/lesson-aggregation-openapi.yaml)
> OpenAPI спецификация внутреннего API (generate-quiz-data, включая scope-фильтр): [openapi/content/content-service.yaml](../openapi/content/content-service.yaml)
> OpenAPI спецификация остального API content-service (реверс-инжиниринг по коду, все остальные эндпоинты): [openapi/content/content-api.yaml](../openapi/content/content-api.yaml) — см. там же зафиксированное расхождение: ADMIN CRUD из §5 ниже в коде не реализован.

### Управление уроками (ADMIN)

```
GET    /api/v1/content/quizzes                          → список уроков (фильтр по lessonType)
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

### 5а. Публичное API для вкладки «Парадигмы» (STUDENT)

**NEW.** До сих пор `declension_stems`/`declension_forms` были доступны только двумя ADMIN-only эндпоинтами (`GET /content/lessons/{slug}/declension-stems`, `GET /content/declension-stems/{stemId}/forms` — реверс-инжинирены в `content-api.yaml`, используются только `quiz-service.ContentClient` для генерации дистракторов, роль ADMIN согласно таблице маршрутов агент-1-gateway.md, т.е. **фронтенд их вызвать не может**). Это блокировало задачу «показать реальные парадигмы (не абстрактные окончания) на вкладке «Парадигмы»» — см. `frontend/pages/grammar-lesson-page.md` §2.2 и `quiz-service/quiz-declension.md` §3.1.

Добавлен один публичный эндпоинт с постраничной выдачей:

```
GET /api/v1/content/public/lessons/{slug}/declension-paradigms?index=N   → DeclensionParadigmPageDto
```

Возвращает **одну** парадигму (стем + все его формы) за раз, по 0-based `index`, плюс `totalCount` — для навигации «вперёд/назад» по стемам урока на фронтенде (карусель, одна таблица падеж×число на экран, см. `frontend/pages/grammar-lesson-page.md` §2.2). Урок склонений обычно содержит несколько стемов-примеров, поэтому грузить формы всех сразу нецелесообразно. Полная схема — `openapi/content/content-api.yaml` + `schemas/content.yaml#DeclensionParadigmPageDto`/`#DeclensionParadigmDto`.

**Задача Агенту 2 (реализация):**
- Контроллер: новый метод в существующем `LessonContentController` (или отдельный `DeclensionParadigmController`, если контроллер уже перегружен) под путём `/content/public/lessons/{slug}/declension-paradigms`, роль STUDENT, query-параметр `index` (см. `parameters.yaml#ParadigmIndexQueryParam`).
- Данные — из уже существующих `DeclensionStemRepository`/`DeclensionFormRepository`: сначала стабильно отсортированный список `stemId` урока (по `stemId`/дате создания — зафиксировать выбор постфактум), взять `totalCount` = размер списка, `index`-й элемент → подгрузить его формы. Не грузить формы остальных стемов.
- `index` вне диапазона `[0, totalCount)` → 404 (тем же `ErrorResponse`, что и для «урок не найден»).
- **Обязательная зависимость:** сначала выполнить миграцию из §4 выше (`stemIast` уже есть в БД и entity, но добавить `stemIast` в `DeclensionStemMapper`/DTO — см. расхождение, зафиксированное в `content.yaml#DeclensionStemDto`) и миграцию `translation_ru/translation_en` + data-fix `stem_devanagari` (уже запланирована, см. §4/§9) — без них `stemIast/translationRu/translationEn/stemDevanagari` внутри `DeclensionParadigmDto` будут пустыми и вкладка «Парадигмы» не даст выигрыша относительно текущего состояния.
- 404, если урок не найден или `lessonType != DECLENSIONS` (переиспользовать существующий паттерн ошибки, как в `getDeclensionStemsForLesson`).

### Внутреннее API для quiz-service

> **ИЗМЕНЕНО (архитектурное решение):** ранее `generate-quiz-data` генерировал вопросы **и**
> сохранял их в content-service (`content.generated_quiz_data`/`generated_questions`), а
> quiz-service на resume/answer/complete перезапрашивал их обратно по id
> (`GET /generated-quiz-data/{id}`, `GET /generated-questions/{id}`). Решено: это дублирование
> не нужно, т.к. quiz-service всё равно обязан хранить сгенерированные вопросы у себя (для
> SQL-статистики/истории, `quiz.session_questions`, см. quiz-service.md §12). Поэтому
> content-service теперь **не хранит ничего** — генерирует и сразу возвращает результат.
> Единственный вызываемый quiz-service эндпоинт для генерации вопросов сессии:

```
POST /api/v1/content/lessons/{quizId}/generate-quiz-data   → генерирует вопросы сессии и
                                                               ВОЗВРАЩАЕТ их, ничего не
                                                               сохраняя; вызывается один раз,
                                                               на старте сессии
```

Эндпоинты `GET /generated-quiz-data/{id}` и `GET /generated-questions/{questionId}` —
**удалены** вместе с их персистентным слоем (см. §3а). quiz-service самостоятельно хранит
результат этого вызова в `quiz.session_questions` и оттуда же читает на resume/answer/complete
(см. quiz-service.md §3, §5, §12) — content-service для этого больше не нужен.

Ответ `generate-quiz-data` — `GeneratedQuizData`: `{ lessonId, lessonType,
questionsPerSession, generatedQuestions[...], vocabularyWords (null для не-VOCABULARY) }`.
Поле `generatedQuizDataId` в DTO больше не нужно как внешний идентификатор для повторного
запроса к content-service — quiz-service при желании может сохранить какой-то свой
внутренний group-id, но это уже его внутреннее дело (см. quiz-service.md §12).

**ИЗМЕНЕНО (перенос scope pre-filter из quiz-service, см. quiz-declension.md §3.4):**
`generate-quiz-data` принимает опциональные query-параметры фильтрации, ранее применявшиеся
на стороне quiz-service (`SessionCreationService.applyScopeFilter`) уже после получения
полного списка вопросов от content-service:

- `filterScope` — `CASE_ONLY` / `NUMBER_ONLY` / `CASE_NUMBER_GENDER` (те же значения, что и у
  одноимённого параметра `quiz-sessions.yaml`/`FilterScopeParam`; в content-service
  представлено обычной строкой, не связано с enum `FilterScope` из quiz-service — сервисы не
  шарят доменные enum через HTTP-контракт).
- `filterCaseTypes` — CSV/JSON-массив значений `CaseType`, используется при `CASE_ONLY`.
- `filterNumberTypes` — CSV/JSON-массив значений `NumberType`, используется при `NUMBER_ONLY`.
- `filterCombinations` — JSON-массив троек `{caseType,numberType,gender}`, используется при
  `CASE_NUMBER_GENDER`.

Если `filterScope` не передан — поведение не меняется, возвращаются все сгенерированные
вопросы (как раньше). Если передан — content-service фильтрует `generatedQuestions[]` по тем
же правилам, что ранее были в `applyScopeFilter` (сравнение `targetCase`/`targetNumber`/
`gender` вопроса с разрешённым множеством), и возвращает уже отфильтрованный список;
**критично:** фильтр обязан применяться к пулу кандидатов (комбинациям основа×падеж×число) **до**
обрезки по `questionsPerSession` внутри `DeclensionQuizGeneratorService`/`QuestionGenerationService`,
а не после генерации фиксированных `questionsPerSession` вопросов — иначе после фильтрации
может остаться 0–1 вопрос вместо полного набора (это и есть баг, ради которого фильтр
переносится из quiz-service: перенос сам по себе бесполезен, если порядок «генерация → фильтр
→ обрезка» не изменить на «фильтр кандидатов → генерация ровно `questionsPerSession` вопросов
из отфильтрованного пула, комбинируя разные вопросы»);
`vocabularyWords` фильтр не затрагивает. Пустой результат фильтрации — не ошибка на уровне
content-service (пустой список); обработку `SCOPE_FILTER_EMPTY` (бизнес-ошибка) по-прежнему
выполняет quiz-service после получения ответа (см. quiz-service.md §6, quiz-service-sessions.md).
Парсинг CSV/JSON-параметров — новая внутренняя утилита content-service, независимая от
`QuizFilterJsonHelper` quiz-service (тот остаётся в quiz-service — используется для
канонизации множеств `QuizSession.filterCaseTypes/filterNumberTypes/filterCombinations` при
поиске сессии для резюма, что вне ответственности content-service).

Для DECLENSIONS/CONJUGATIONS `generatedQuestions[]` — это `GeneratedQuizQuestionDto`:
`{id, quizId, questionNumber, text, explanationRu, explanationEn,
declensionStemId, targetCase, targetNumber, correctFormIast, correctFormDevanagari,
vocabularyWordId, questionSourceLanguage, questionTargetLanguage, correctTranslationRu,
correctTranslationEn, userLocale, stem, caseType, numberType, gender}`.

**NEW (задача Агенту 2):** добавить в `QuestionResponse`/`GeneratedQuizQuestionDto` поля
`stemDevanagari`, `stemTranslationRu`, `stemTranslationEn`, заполняемые из
`DeclensionStem.stemDevanagari/translationRu/translationEn` в
`DeclensionQuizGeneratorService.generateSingleQuestion(...)`.

**Дистракторы (варианты ответа) НЕ входят в этот ответ и не хранятся здесь** — они
генерируются в quiz-service на лету при каждом обращении к вопросу, в т.ч. на resume, для чего
content-service всё равно продолжает быть нужен через отдельный, независимый от этого,
эндпоинт `getDeclensionForms` (см. quiz-service.md §5а) — то есть **полной развязки
quiz-service от content-service на resume нет**, изменилось только то, что именно
content-service отдаёт по запросу.

---

## 3а. Генерация вопросов сессии — теперь без сохранения (было пропущено в этом документе)

Ранее в этом разделе описывались персистентные таблицы `content.generated_quiz_data`/
`content.generated_questions`. **Они удалены** вместе с `GeneratedQuizDataRecordRepository`,
`GeneratedQuestionRepository` и entity `GeneratedQuizDataRecord`/`GeneratedQuestion` — решено,
что per-session сгенерированный вопрос хранит только quiz-service (`quiz.session_questions`,
см. quiz-service.md §12), а content-service — чистый генератор без побочных эффектов записи.

`DeclensionQuizGeneratorService`/`QuestionGenerationService` остаются, но их персистентная
часть (`GenerateQuizService` в части сохранения) удаляется — они теперь только строят и
возвращают `List<QuestionResponse>`/`GeneratedQuizData`, ничего не пишут в БД
content-service.

---

## 6. Backend структура

Пакет `controller/`: LessonController, QuestionController, VocabularyController, LessonContentController (только `generate-quiz-data` — единственный внутренний эндпоинт для quiz-service; ранее здесь ошибочно упоминался несуществующий `internal/SessionDataController`, а также два уже удалённых read-эндпоинта, см. §5).
Пакет `service/`: LessonService, QuestionService, VocabularyService, DeclensionQuizGeneratorService, QuestionGenerationService (только генерация, персистентная часть удалена, см. §3а), GenerateQuizService (оркестрирует генерацию + применяет scope pre-filter к `generatedQuestions[]`, см. §3), QuizScopeFilterService (новый — перенесённый из quiz-service `applyScopeFilter`/`parseCombinationsJson`, чистая функция без состояния).
Пакет `repository/`: LessonRepository, QuestionRepository, QuestionOptionRepository, VocabularyWordRepository, DeclensionStemRepository, DeclensionFormRepository (`GeneratedQuizDataRecordRepository`/`GeneratedQuestionRepository` — удалены, см. §3а).
Пакет `model/`: Quiz, Question, QuestionOption, VocabularyWord, LessonType, Difficulty, DeclensionStem, DeclensionForm (`GeneratedQuizDataRecord`/`GeneratedQuestion` — удалены, см. §3а).
Пакет `dto/`: CreateQuizRequest, CreateQuestionRequest, QuizDetailResponse, VocabularyWordRequest, QuestionResponse, GeneratedQuizData, GeneratedQuizQuestionDto, DeclensionStemDto, DeclensionFormDto (DTO остаются — это транспортный формат ответа `generate-quiz-data`, просто больше не persist-ится).

---

## 7. application.yml

Порт 8081, virtual threads enabled, datasource через env, ddl-auto: validate, default_schema: content, flyway schemas: content.

---

## 8. Acceptance Criteria

- [ ] Только ADMIN получает доступ к write-операциям (403 для STUDENT)
- [ ] `generate-quiz-data` доступен без роли ADMIN (для quiz-service) — единственный внутренний эндпоинт, не пишет в БД
- [ ] `generate-quiz-data` c `filterScope`/`filterCaseTypes`/`filterNumberTypes`/`filterCombinations` возвращает только вопросы, соответствующие фильтру (логика перенесена из quiz-service `applyScopeFilter`, см. §2 выше); без `filterScope` — поведение не меняется
- [ ] Нельзя сохранить вопрос без ровно 1 правильного варианта
- [ ] Удаление квиза и вопроса — soft delete
- [ ] `vocabulary_words` возвращаются только для квизов с `quiz_type = VOCABULARY`
- [ ] Slug уникален и соответствует паттерну `^[a-z0-9][a-z0-9-]*$`

---

## 9. Открытые вопросы

- [ ] Импорт вопросов и слов из CSV для массового добавления?
- [ ] Кэшировать ли ответ `generate-quiz-data`/данные для дистракторов (`getDeclensionForms`) — актуально только для распределения нагрузки на content-service при большом числе одновременных `start`/`resume`; ответ `generate-quiz-data` теперь не переиспользуется (вызывается один раз на старте, дальше quiz-service хранит сам). Отложено до появления реальной нагрузки (см. quiz-service.md §3).
- [ ] **Личные списки слов** (не реализовано) — новые сущности
  `content.user_word_lists (id, userId, title, createdAt)` и
  `content.user_word_list_items (listId, stemId|conjugationId, addedAt)`,
  плюс режим выбора основ квиза "из списка пользователя" в
  `DeclensionQuizGeneratorService`. Требования и открытые вопросы по
  "сырым" элементам списка (слово без связи с `declension_stems`) —
  см. [frontend/information-architecture.md §3.1](../frontend/information-architecture/02-catalog.md).

---

## 10. Домен Eamenau

Модуль упражнений по правилам сандхи санскрита. Полная спецификация: [services/content-service/eamenau.md](./content-service/eamenau.md).

Структура: модели (13 классов) в eamenau/model/, репозитории (12 интерфейсов) в eamenau/repository/, сервисы (EamenauService, EamenauExerciseService), контроллеры (EamenauController, EamenauExerciseController). Shared DTOs — в shared/samskrtam-dtos. Миграция: V2 — schema eamenau. Фронтенд: pages/eamenau/, components/eamenau/.

**Endpoints:** GET/PUT /api/v1/eamenau/sandhi-rules, exercises, solutions (ADMIN).

**Известные проблемы:** PUT /solutions/{id} без авторизации, Answer не используется, Phoneme без API.

---

## 11. Internal REST: приём словаря из sangraha-service

**ИЗМЕНЕНО (было автосинхронизация через Outbox после каждого анализа, стало on-demand по кнопке «Изучить»).** Раньше `content-service` принимал слова автоматически после каждого анализа стиха (через Transactional Outbox Relay в sangraha-service, см. историю в ADR-006). Теперь sangraha-service вызывает этот эндпоинт **только** по явному клику пользователя на кнопку «Изучить» на VersePage (см. `sangraha-service.md` §6/§7, ADR-009) — Outbox в sangraha-service убран целиком, вызов происходит синхронно внутри HTTP-запроса на кнопку.

```
POST /content/internal/sangraha/vocabulary-quiz
```

**Не публичный и не ADMIN-эндпоинт** — чистый service-to-service вызов, без роли (аутентификация — по внутреннему сетевому периметру/service-to-service секрету, как решит Агент 1). Вызывается напрямую по адресу content-service (env `CONTENT_SERVICE_URL` у sangraha-service, минуя gateway), не через `/api/v1/**`.

### Request

```json
{
  "verseId": "uuid",
  "workSlug": "bhagavad-gita",
  "workTitleRu": "Бхагавад-гита", "workTitleEn": "Bhagavad Gita",
  "chapterSlug": "1",
  "chapterTitleRu": "Глава 1", "chapterTitleEn": "Chapter 1",
  "verseOrderIndex": 1,
  "words": [
    {
      "wordIast": "dhṛtarāṣṭraḥ", "wordDevanagari": "धृतराष्ट्रः",
      "stem": "dhṛtarāṣṭra", "root": null, "gender": "MASCULINE",
      "translationRu": "Дхритараштра", "translationEn": "Dhritarashtra",
      "explanationRu": "...", "explanationEn": "..."
    }
  ]
}
```

Список `words[]` уже дедуплицирован sangraha-service по `(lemmaIast, stem)` в рамках стиха (см. `sangraha-service.md` §6) — content-service не обязан ожидать дублей внутри одного запроса, но и не полагается на это (см. шаг 4 ниже — дедуп всё равно по `(wordIast, stem)`, для защиты от повторной отправки одного и того же слова в разных стихах).

### Response

```json
{ "quizSlug": "sangraha-verse-<verseId>" }
```

Один квиз на стих, не на слово — `vocabularyWordId` каждого слова больше не возвращается (не нужен, `verse_words.vocabulary_word_id` из старого плана отменён вместе с Outbox, см. `sangraha-service.md` §3).

### Обработка (`SangrahaVocabularyController` → `VocabularyQuizSyncService`, синхронно, в теле HTTP-запроса)

Идемпотентно (sangraha-service может повторить вызов при таймауте на своей стороне — не гарантия at-least-once через Outbox, как раньше, а просто «пользователь нажал кнопку ещё раз», см. `sangraha-service.md` §6, шаг 5):

1. `VocabularyCategory` root: `findByCodeIgnoreCase(workSlug)`, если нет — создать (`nameRu = workTitleRu`, `nameEn = workTitleEn`, `parentId = null`) — **без изменений относительно прежней логики**, категория остаётся общим механизмом тематической классификации лексики (см. `information-architecture.md` §2.3), не специфичным для этого флоу.
2. `VocabularyCategory` chapter: `findByCodeIgnoreCase("{workSlug}.{chapterSlug}")`, если нет — создать с `parentId = root.id`.
3. `Quiz` — **на уровне стиха**, не категории: `slug = "sangraha-verse-{verseId}"` (детерминирован, не рандом — повтор вызова не создаёт дубль), `titleRu/En` — шаблон, например `"{workTitleRu}, стих {verseOrderIndex}"` (точный текст — на усмотрение Агента 2). `upsert` — если `Quiz` с таким slug уже есть, вернуть его же (идемпотентность), новый не создавать.
4. Для каждого слова из `words[]`: dedup по `(wordIast, stem)` в рамках всего словаря content-service (`findByWordIastAndStem`), как и раньше. Если найдено — не создавать новый `VocabularyWord`; связать существующий `wordId` и с `Quiz` этого стиха (если связи ещё нет), и с `chapterCategory` (`VocabularyWordCategory`, если связи ещё нет) — слово может одновременно входить в квиз конкретного стиха **и** в тематическую категорию произведения/главы, это независимые связи. Если не найдено — создать `VocabularyWord` (`wordIast`, `wordDevanagari`, `stem`, `root`, `gender`, `translationRu/En`, `explanationRu/En`) и сразу связать и с квизом стиха, и с категорией главы.
5. **Ошибки:** невалидный payload/ошибка БД → HTTP 4xx/5xx с телом ошибки (`ErrorResponse`), транзакция отменяется целиком. Повтор — ответственность sangraha-service (кнопка «Изучить» просто не закэширует slug при ошибке, пользователь может нажать снова, см. `sangraha-service.md` §6, шаг 5); DLQ/Outbox-ретраи здесь не нужны — это был механизм для автосинхронизации, которой больше нет.

### Открытые вопросы (для Агента 2 при реализации)

- Точный шаблон `titleRu/En` генерируемого квиза (см. шаг 3) — решает Агент 2, зафиксировать постфактум в этом разделе.
- `gender = null` от sangraha (для indeclinable-слов) — как мапится в `VocabularyWord.gender` (там `nullable = false`)? Вероятно `UNSPECIFIED` — подтвердить при реализации.