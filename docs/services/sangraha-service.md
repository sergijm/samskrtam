# sangraha-service

> Домен: Sangraha (सङ्ग्रह — «собрание, свод») — санскритские произведения: иерархия
> книга → глава → стих, LLM-анализ стиха (транслитерация, перевод, сандхи, грамматика).
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/sangraha-service`
> Порт: `8089` (фиксированный, из env)
> Схема БД: `sangraha`
> Status: **DRAFT**

---

## 1. Описание

Хранит санскритские тексты (произведения) в виде дерева **Work → Chapter → Verse** и
результаты их LLM-анализа: транслитерация IAST ⇄ devanagari, перевод (ru/en), разбор
сандхи, пословная грамматика.

Сервис **не хранит словарь**. Единственный канал наружу — синхронный REST-вызов
`curriculum-service` (см. §6, [architecture.md §3.5](../architecture.md#35-sangraha-service-произведения-llm-анализ-стихов-синхронизация-лексики-через-rest)): по явному действию пользователя (кнопка «Изучить» на
VersePage, §7) sangraha-service отправляет слова конкретного стиха, `curriculum-service`
синхронно строит из них лексический квиз этого стиха и возвращает `quizSlug` и `quizId` (UUID сущности `Lesson` в curriculum-service — фронтенд стартует сессию по UUID напрямую, см. §7).
Анализ стиха сам по себе не инициирует никакой отправки в curriculum-service — вызов
происходит лениво, по клику пользователя, без фонового Outbox/relay. Слово попадает
в curriculum-service только если хотя бы один пользователь захотел изучить слова этого
стиха.
Сопоставление слов со словарными статьями `dictionary-service` **в текущей итерации не
делается** (см. §8).

Разделение ответственности:
- **sangraha-service** — тексты, их структура, LLM-анализ (грамматика стиха)
- **curriculum-service** — лексика для VOCABULARY-квизов (получает слова синхронным REST-вызовом, см. §6)
- **dictionary-service** — полный словарь (MW/Frisch), не связан с sangraha в этой итерации

---

## 2. Сущности

**Work** (таблица works): id (UUID), slug (string, unique), titleRu, titleEn, titleSaIast, titleSaDevanagari, descriptionRu, descriptionEn, author (nullable), sourceId (UUID FK → sources), createdAt, deletedAt

**WorksClass** (таблица works_class): id (UUID), parentId (UUID, self-ref, nullable), classification (string, имя классификатора — GENRE/ERA/…), code (string, unique), titleSaIast, titleSaDeva, titleRu, titleEn, sortOrder (int)

**WorksWorkClass** (таблица works_work_class): workId (UUID FK → works, CASCADE), classId (UUID FK → works_class, CASCADE), композитный PK (work_id, class_id) — many-to-many связь произведения с категориями классификатора

**Chapter** (таблица chapters): id (UUID), workId (UUID), slug (string, unique в пределах work), orderIndex (int, nullable — backend вычисляет автоматически при создании, см. §5.3), titleRu, titleEn, titleSaIast, titleSaDevanagari, deletedAt

**Verse** (таблица verses): id (UUID), chapterId (UUID), orderIndex (int), rawText (VARCHAR, сырой ввод пользователя до определения письменности — см. §4, §7), textDevanagari (TEXT), textIast (TEXT), status (DRAFT|ANALYZING|ANALYZED|FAILED), createdAt, updatedAt, deletedAt

**VerseAnalysis** (1:1 с Verse, таблица verse_analyses): verseId (UUID, PK), translationRu (TEXT), translationEn (TEXT), sandhiSplits (JSONB), rawModelResponse (JSONB, опционально), modelName, analyzedAt

**VerseWord** (таблица verse_words, задача Агенту 2 — реляционная модель, **НЕ** JSONB) — расширена полным лексико-грамматическим разбором (formType, isFinite, lemmaGlossRu/En, contextGlossRu/En вместо glossRu/En, analysisConfidence, ambiguityNotes); морфология и словообразование вынесены в отдельные таблицы 1:1 — **VerseWordMorphology**, **VerseWordDerivation**. Детали — [sangraha-service/verse-word-grammar.md](./sangraha-service/verse-word-grammar.md).

---

## 3. Flyway Migrations

Схема БД описывается одним файлом `V1__create_schema.sql`, содержащим целевую структуру
(works/chapters/verses, verse_analyses, реляционные verse_words + verse_word_morphology +
verse_word_derivation — см. §2, [verse-word-grammar.md](./sangraha-service/verse-word-grammar.md));
проект без прод-данных, поэтому история миграций не накапливается — при изменении схемы
единственный исходный файл переиздаётся в целевом виде. Таблица `outbox_events` не создаётся
(см. §6): доставка обеспечивается синхронным HTTP-вызовом по клику пользователя, а не фоновым
relay.

---

## 4. API

Права доступа: **весь write-контур — только `ADMIN`** (как в curriculum-service). Отдельная
роль «редактор/переводчик» отложена на будущую итерацию (см. §8). Чтение доступно всем
аутентифицированным пользователям.

```
GET    /api/v1/sangraha/works                                  → плитки произведений
       ?classId={uuid}&classId={uuid}...                         → фильтрация по категориям классификатора произведений
                                                                    (множественный выбор, см. GET /works/classes);
                                                                    произведение подходит, если связано хотя бы с одной
                                                                    категорией из запроса (включая все подкатегории)
GET    /api/v1/sangraha/works/classes                           → дерево классификатора произведений (works_class),
                                                                    сгруппированное по classification: один дропдаун
                                                                    TreeSelect с checkbox-мультивыбором на группу
       ?id={workId} (опционально)                              → если id указан — дерево произведения по UUID
GET    /api/v1/sangraha/works/{workSlug}                        → ★ произведение + дерево chapters/verses по slug
                                                                   (основной эндпоинт для фронтенда /sangraha/:workSlug,
                                                                   возвращает id, slug, titleRu/En и chapters[].verses[])

GET    /api/v1/sangraha/verses/{verseId}                         → стих: текст + (если ANALYZED) VerseAnalysis + VerseWord[] +
                                                                     vocabularyQuizSlug/vocabularyQuizId (кэш кнопки «Изучить», null пока
                                                                     не нажата, см. §6, §7, sangraha-schemas.yaml#VerseDetail)
POST   /api/v1/sangraha/verses/{verseId}/vocabulary-quiz         → кнопка «Изучить»: вернуть кэш ({quizSlug, quizId, quizStatus:"EXISTING"})
                                                                     или синхронно создать/дозаполнить лексический квиз стиха в
                                                                     curriculum-service ({quizSlug, quizId, quizStatus}), закэшировать
                                                                     quizSlug/quizId (см. §6)
POST   /api/v1/sangraha/verses/{verseId}/analyze                 → сохранить `text` и запустить LLM-анализ (ADMIN, см. §5); тело —
                                                                     единое поле `text` (обязательно, см. §7) — backend определяет
                                                                     письменность и заполняет textDevanagari/textIast
POST   /api/v1/sangraha/chapters/{chapterId}/verses/analyze-all  → батч-анализ всех DRAFT/FAILED стихов главы (ADMIN, реализовано,
                                                                     )
GET    /api/v1/sangraha/verse?id={uuid}&id={uuid}...             → произвольный список стихов + status каждого
                                                                      (не только ANALYZED), см. sangraha-service/batch-verse-review.md
POST   /api/v1/sangraha/verse/analysis                           → батч-анализ произвольного списка verseId (ADMIN,
                                                                      безусловный повтор), см. sangraha-service/batch-verse-review.md
POST   /api/v1/sangraha/analysis                                 → страница /analysis: создать standalone-стих (chapter_id = null,
                                                                      owner_id = X-User-Id) и сразу запустить LLM-анализ; каждое
                                                                      нажатие «Анализировать» создаёт новую запись (любой авторизованный,
                                                                      персональные стихи)
GET    /api/v1/sangraha/analysis                                 → список standalone-стихов текущего пользователя (новые сверху,
                                                                      только превью/статус/createdAt — без контекста произведения/главы)
DELETE /api/v1/sangraha/analysis/{verseId}                       → мягкое удаление standalone-стиха (только владелец)
```

Ответ `GET /works/{workSlug}` (и `GET /works?id={workId}`) — двухуровневое дерево для TreeGrid:
Ответ — двухуровневое дерево: { id, slug, titleRu, chapters[] { id, slug, titleRu, orderIndex, categoryCode, verses[] { id, orderIndex, textIastPreview, status } } }

---

## 5. LLM-интеграция

### 5.1 Анализ стиха (tool calling)

**Сохранение текста перед анализом.** `POST /verses/{verseId}/analyze` принимает обязательное
тело с полем `text` — фронтенд всегда отправляет текущее значение единственного поля ввода.
Backend сохраняет `text` как есть в `Verse.rawText`, затем детектирует письменность по
Unicode-диапазону деванагари и заполняет `textDevanagari` либо `textIast` — до перехода
статуса в `ANALYZING` и до вызова LLM. Это единственная точка сохранения текста стиха
(Verse CRUD удалён — стихи создаются через импорт, текст сохраняется здесь,
см. §7).

Конфигурация — только через env, без дефолтов в yml (см. конвенцию по секретам):

```
SANGRAHA_LLM_BASE_URL     # OpenAI-совместимый endpoint
SANGRAHA_LLM_API_KEY
SANGRAHA_LLM_MODEL        # например gpt-4.1 / другая OpenAI-совместимая модель
```

Backend вызывает `/chat/completions` (или `/responses`) с промптом (файл
[`prompts/verse-analysis.md`](./prompts/verse-analysis.md)) и
**одним** объявленным tool — модель обязана вернуть результат через `tool_calls`,
а не свободным текстом.

Tool `submit_verse_analysis` с параметрами: textDevanagari, textIast,
translationRu, translationEn, sandhiSplits (массив {surface, components[],
ruleNumbers[]}), words — полный лексико-грамматический разбор
каждого слова, точный список полей и JPA-модель хранения —
[verse-word-grammar.md §1–§3](./sangraha-service/verse-word-grammar.md).

**Письменность:** везде, кроме `textDevanagari` и `surfaceDevanagari`, — только IAST.
В частности, `sandhiSplits.surface`/`components`, `lemmaIast`, `stem`, `root` — IAST,
даже если исходный текст стиха был введён только в деванагари. Это нужно проверять
при валидации ответа LLM (наличие символов деванагари в этих полях — признак ошибки
модели).

Справочник правил сандхи (нумерация 1–71, внутренние + внешние, из Эмено, с
глоссарием фонетических терминов) —
[`prompts/emenau-sandhi-rules.json`](./prompts/emenau-sandhi-rules.json).
Правила `applicability=external` (41–71) — граница между словами, цитируются в
`sandhiSplits.ruleNumbers`. Правила `applicability=internal` (1–40) — как образована
сама словоформа из корня/основы (морфофонемные изменения при словообразовании),
цитируются в `words[].formationRuleNumbers`; **не путать поля местами**. Если сандхи
на стыке слов нет — граница не попадает в `sandhiSplits`; если словоформа не требует
объяснения через внутренние сандхи — `formationRuleNumbers: []`. Если правило неясно
в любом из двух случаев — пустой массив, не угадывать номер.

Backend:
1. Валидирует `tool_calls[0].function.arguments` по этой схеме (например через JSON Schema validator, не доверяем модели).
2. В одной транзакции: обновляет `Verse.textDevanagari/textIast` (если не были заданы вручную), пишет `VerseAnalysis` (перезаписывая предыдущую — см. §8), пересоздаёт `VerseWord[]` для стиха, переводит `Verse.status → ANALYZED`.

Анализ стиха не инициирует никакой синхронизации с curriculum-service — слова уходят в curriculum-service только по явному действию пользователя, кнопка «Изучить» на VersePage (см. §6, §7).

Если пользователь ввёл текст только в одном представлении (только devanagari или только
iast) — второе представление также генерирует модель, и backend сохраняет оба.

### 5.2 Удалённые операции

Work/Chapter CRUD удалён. Произведения и главы создаются через импорт
(см. §1). Соответствующие эндпоинты и Java-сервисы удалены:
- `POST /works`, `PUT /works/{workSlug}`, `DELETE /works/{workSlug}`
- `POST /works/{workSlug}/chapters`, `PUT /chapters/{chapterId}`, `DELETE /chapters/{chapterId}`

---

## 6. On-demand REST: sangraha → curriculum-service (по кнопке «Изучить»)

Синхронизация выполняется on-demand, по клику пользователя, без транзакционного Outbox:
анализ стиха не запускает никакой синхронизации с curriculum-service сам по себе, вызов
происходит внутри HTTP-запроса на кнопку «Изучить» — персистентная очередь и фоновые
ретраи не нужны.

### VocabularyQuizController (endpoint из §7 таблицы API)

`POST /api/v1/sangraha/verses/{verseId}/vocabulary-quiz` — обрабатывается синхронно,
в теле HTTP-запроса от фронтенда:

1. Загрузить `verse.vocabularyQuizSlug`/`vocabularyQuizId` и `VerseWord[]` этого стиха. Если квиз уже закэширован (`vocabularyQuizSlug`/`vocabularyQuizId` не пустые) **и** у каждого `VerseWord` этого стиха уже проставлен `vocabularyWordId` — вернуть `{ quizSlug, quizId, quizStatus: "EXISTING" }` немедленно, без обращения к curriculum-service. Если квиз закэширован, но хотя бы у одного слова `vocabularyWordId` ещё не проставлен (например, слово добавилось при повторном анализе стиха после того, как квиз уже был создан) — не возвращать сразу, а продолжить с шага 2, чтобы дозаполнить недостающие маппинги.
2. Если квиз ещё не закэширован — проверить `verse.status == ANALYZED` и что `VerseWord[]` не пуст (иначе ошибка). Собрать `VerseWord[]` этого стиха, дедуплицировать по `(lemmaIast, stem)` **внутри стиха** (одно и то же слово, встретившееся в стихе дважды, не должно попасть в список дважды; для каждого уникального `(lemmaIast, stem)` в запрос идёт один представитель — первый по порядку `position`).
3. `POST {CONTENT_SERVICE_URL}/content/internal/sangraha/vocabulary-quiz` (синхронный HTTP-клиент — `RestClient`) с телом: `verseId`, `workSlug`, `workTitleRu/En`, `chapterSlug`, `chapterTitleRu/En`, `verseOrderIndex`, `words[]`. Каждый элемент `words[]` собирается из представителя дедупликации по словарным полям — `verseWordId = VerseWord.id`, `wordIast = lemmaIast`, `translationRu/En = lemmaGlossRu/En`, `wordDevanagari = surfaceDevanagari` (у `VerseWord` нет отдельного деванагари-написания леммы, см. `verse-word-grammar.md` §1 — используется деванагари той словоформы, в которой слово встретилось первым; для слов без сандхи/окончания на стыке совпадает с деванагари леммы). Точный контракт — `curriculum-service.md` §11.
4. **Успех (2xx):** из ответа `{ quizSlug, quizId, quizStatus, wordMappings[] }` — если квиз не был закэширован на шаге 1, сохранить `quizSlug`/`quizId` в `verse.vocabularyQuizSlug`/`verse.vocabularyQuizId`. Независимо от того, был ли квиз уже закэширован, разложить `wordMappings[]` (`verseWordId → vocabularyWordId`, где `verseWordId` — id дедуплицированного представителя) обратно на **все** `VerseWord[]` стиха по ключу `(lemmaIast, stem)` — включая слова-дубли внутри стиха, которые не были представителем и не попали в запрос напрямую — и сохранить `vocabularyWordId` у каждого. Ответ фронтенду: `{ quizSlug, quizId, quizStatus }` — `quizStatus` берётся из ответа curriculum-service, если квиз создавался/дозаполнялся в этом вызове, либо `"EXISTING"`, если был кэш-хит на шаге 1.
5. **Ошибка (4xx/5xx/timeout):** вернуть ошибку как есть фронтенду, ничего не сохранять. Повторный клик по кнопке безопасен и идемпотентен — `quizSlug`/`quizId` на стороне curriculum-service детерминированы по `(workSlug, chapterSlug, verseId)` (см. `curriculum-service.md` §11), повторный вызов не создаёт дублей.

Никакой персистентной очереди/ретраев в фоне не требуется — повтор при ошибке равен повторному клику пользователя.

---

## 7. Frontend (эскиз, детализирует Агент 3)

- **Страница произведений** (`/sangraha`) — плитки (`WorkCard`) со списком работ.
- **Страница произведения** (`/sangraha/{workSlug}`) — дерево глав/стихов. Read-only: без кнопок добавления/удаления.
- **Страница массового просмотра/анализа** (`/sangraha/verses`, ADMIN-only, `id` из query-параметров) — см. `sangraha-service/batch-verse-review.md`.
- **Страница стиха** (`/sangraha/{workSlug}/verses/{verseId}`):
  - Поле ввода текста — **одно** (не два раздельных для devanagari/iast). Пользователь
    может печатать в нём как деванагари, так и IAST — оба варианта допустимы в одном
    и том же поле; в режиме редактирования поле показывает сохранённое `rawText`
    (а не `textDevanagari`/`textIast` — они актуальны только после анализа, см. ниже).
    - `status=DRAFT` (или `FAILED`) → поле ввода активно, кнопка
    **только одна** — «Анализ» → `POST /verses/{id}/analyze`
    с телом `{ text }`. Backend сохраняет введённый текст в `rawText`
    и определяет письменность (`textDevanagari`/`textIast`,
    §4/§5.1) в рамках того же запроса, до LLM-анализа.
  - `status=ANALYZING` → поле и кнопка заблокированы, индикатор загрузки.
  - `status=ANALYZED` → поле ввода **read-only** (показывает сохранённые
    `textDevanagari`/`textIast` — оба, если оба заполнены, а не `rawText`), и ниже обязательно
    отображаются результаты `GET /verses/{verseId}` (объект `analysis` +
    `words[]` из `VerseDetail`, см. `sangraha-schemas.yaml`):
    - **Перевод** — `translationRu` и `translationEn` (обе колонки/вкладки).
    - **Сандхи** — `sandhiSplits`: список `surface → components[]`, с номером(-ами)
      правила `ruleNumbers` (по справочнику `prompts/emenau-sandhi-rules.json`,
      §5.1) — показывать как краткую подпись/тултип у каждого перехода; если
      `ruleNumbers` пуст — просто не показывать номер, без плейсхолдера-ошибки (это
      штатный случай «модель не уверена», не баг).
    - **Грамматический разбор** — таблица `words[]` по `position`: поверхностная
      форма, лемма/основа, часть речи и морфологические признаки (падеж/число/род
      либо лицо/время/наклонение/залог — в зависимости от pos), `glossRu`/`glossEn`,
      и номер(-а) внутреннего правила `formationRuleNumbers` (тот же справочник,
      §5.1) — как краткая подпись/тултип «как образована форма»; пустой массив —
      штатно, без плейсхолдера-ошибки. **НОВОЕ:** строка слова разворачивается
      по клику в панель с полным разбором — без новых колонок в таблице, см.
      [verse-word-grammar.md §5](./sangraha-service/verse-word-grammar.md).
    -     Если `status=ANALYZED`, но `analysis`/`words` не пришли (пустой ответ backend) —
      фронтенд должен показать явную ошибку/плейсхолдер, а не пустой блок молча.
  - Кнопка **«Изучить»** — рядом со списком слов (таблица `words[]`, см. выше), видна
    только при `status=ANALYZED` и непустом `words[]`; `disabled`, если слов нет
    (проверяется локально на фронте, без обращения к бэкенду).
    - По клику — синхронный REST-вызов `POST /verses/{verseId}/vocabulary-quiz`
      (sangraha-service, см. §6): если у стиха уже есть закэшированные
      `vocabularyQuizSlug`/`vocabularyQuizId` и все слова стиха уже связаны со
      словарными статьями — sangraha-service возвращает их сразу с
      `quizStatus="EXISTING"` (см. §6, шаг 1); иначе сам синхронно
      создаёт/дозаполняет лексический квиз в curriculum-service и кэширует
      `quizSlug`/`quizId`.
    - Из ответа `{ quizSlug, quizId, quizStatus }` фронтенд **не** делает
      промежуточный `GET /api/v1/lessons/vocabulary/{slug}` — он не нужен для
      запуска сессии. Вместо этого сразу вызывает
      `POST /api/v1/quiz/vocabulary/sessions/start-or-resume?lessonId={quizId}&statusFilter={statusFilter}`
      (quiz-service, `quest-engine.md` §3-4) — по UUID, не по slug.
      `statusFilter` вычисляется на фронте из `quizStatus`: `quizStatus="CREATED"` →
      `statusFilter=NEW` (весь пул квиза — новые слова, обычный смешанный
      due/new/reserve-отбор не нужен, слов ещё никто не проходил); `quizStatus="EXISTING"`
      → `statusFilter` не передаётся (обычный смешанный отбор).
    - После успешного старта сессии — переход на `/quiz/vocabulary/{quizSlug}/{sessionId}`
      (существующий маршрут `QuizPage`, см. `frontend-state.md`/`AppRoutes`) с передачей
      результата через `navigate(url, { state: { sessionData } })` — паттерн,
      уже поддержанный в `useQuizSession` (ветка 1 «navigation state»).
    - Квиз создаётся на уровне **стиха**, а не произведения. Уникальность слов
      внутри квиза стиха обеспечивает sangraha-service (дедуп по `(lemmaIast, stem)`
      перед отправкой, см. §6) — curriculum-service дополнительно дедуплицирует
      `VocabularyWord` по `(wordIast, stem)` в рамках всего своего словаря (см.
      `curriculum-service.md` §11), но это не влияет на состав слов конкретного квиза.

---

## 8. Открытые вопросы / отложено

- **Таблица соответствия IAST↔SLP1 для slug** (см. §5.2): конкретный набор правил
  транслитерации выбирает Агент 2 при реализации на основе общепринятых схем IAST/SLP1.
- **Роль «редактор/переводчик»**: пока весь write — `ADMIN`. Отдельная роль (может вводить/анализировать стихи, но не управлять произведениями/главами) — следующая итерация; когда будет готова модель ролей, добавить `SANGRAHA_EDITOR` и обновить §4.
- **Связь слов стиха со словарём** (`dictionary-service`, поиск по `slp1`): сознательно не делаем в этой итерации — только грамматика от LLM. Если понадобится — отдельным Kafka-каналом (sangraha публикует, dictionary-service асинхронно обогащает через ответное событие), без синхронных вызовов между сервисами.
- **Политика ретраев** — не требуется: синхронизация происходит внутри HTTP-запроса на кнопку «Изучить», отдельного фонового Outbox/relay нет, повтор равен повторному клику пользователя.
- **Уровень Quiz(VOCABULARY)** — стих, а не произведение: `slug = "{workSlug}.{chapterSlug}.verse-{verseId}"` (см. §6, §7).

## 9. Internal REST: примеры склонений для curriculum-service

Два service-to-service эндпоинта для вкладки «Примеры» на странице шага склонений (`curriculum-service.md` §12) — не публичные, не через api-gateway, вызываются напрямую по адресу sangraha-service (env `SANGRAHA_SERVICE_URL` у curriculum-service, по аналогии с `CONTENT_SERVICE_URL` у sangraha-service, §6).

### `POST /sangraha/internal/content/declension-examples`

Ищет примеры словоформ по словоизменительному классу и возвращает их сгруппированными по ячейке `(caseType, numberType)`, не сами цитаты — только `verseId[]` (за текстом/переводом curriculum-service идёт отдельно, см. следующий эндпоинт).

```json
{
  "vowelType": "A_STEM",
  "gender": "MASCULINE",
  "limitPerGroup": 3,
  "cells": [
    { "caseType": "NOMINATIVE", "numberType": "SINGULAR" },
    { "caseType": "INSTRUMENTAL", "numberType": "SINGULAR" }
  ]
}
```

`gender`/`caseType`/`numberType` — значения тех же enum'ов, что в `VerseWordMorphology` (`Gender`, `GrammaticalCase`, `NumberType`, см. `verse-word-grammar.md` §1); имена значений совпадают с одноимёнными enum'ами curriculum-service (`content.Gender/CaseType/NumberType`) один в один, поэтому маппинг на стороне curriculum-service — только сериализация имени enum, без таблицы соответствий. `vowelType` — значения = `declension_stems.vowel_type` curriculum-service (`ck_vowel_type`, см. `curriculum-service` V13-миграцию): 7 регулярных классов основы (`A_STEM`, `AA_STEM`, `I_STEM`, `II_STEM`, `U_STEM`, `UU_STEM`, `R_STEM`) + 8 местоимённых (`PRON_AHAM`, `PRON_TVAM`, `PRON_TAD`, `PRON_ETAD`, `PRON_IDAM`, `PRON_KIM`, `PRON_YAD`, `PRON_REFLEXIVE`). Для `PRON_*` классов сопоставление по основе в принципе не работает (местоимённые парадигмы супплетивны — например, `aham` → `mayā` в творительном, разные корни) — поиск для `PRON_*` идёт по фиксированному соответствию `vowelType → lemmaIast` (`PRON_AHAM` → `asmad`, `PRON_TVAM` → `yuṣmad`, `PRON_TAD` → `tad`, и т.д.) — таблица соответствий не в этом документе, фиксирует Агент 2 при реализации по словарю местоимений.

Для 7 регулярных классов основной источник `vowelType` слова — таблица `NominalLemma` (`nominal_lemmas`, одна строка на лемму, см. `verse-word-grammar.md` §1б): у слова-кандидата берётся `VerseWord.lemmaIast`, по нему batch-лукап `NominalLemma` (`findByLemmaIastIn`, без физической FK-связи — join по тексту леммы), `stemClass` найденной строки трактуется как `vowelType`. Если для леммы нет строки в `nominal_lemmas` (или `stemClass` в ней `null`) — fallback на прежнюю эвристику: `vowelType` определяется на лету по последней букве `VerseWord.stem` (`A_STEM` → `stem` оканчивается на краткое `a`, `AA_STEM` → на `ā`, и т.д. по остальным пяти регулярным классам). `noun_stems` (см. `verse-word-grammar.md` §1а, deprecated) поиском больше не используется. Приоритет `nominal_lemmas` над эвристикой — по умолчанию; полная замена эвристики (её удаление из кода) — отдельное решение после того, как `nominal_lemmas` будет заполнена для большей части корпуса.

Ответ:

```json
{
  "groups": [
    { "caseType": "NOMINATIVE", "numberType": "SINGULAR", "verseIds": ["uuid1", "uuid2", "uuid3"] },
    { "caseType": "INSTRUMENTAL", "numberType": "SINGULAR", "verseIds": [] }
  ]
}
```

По каждой запрошенной ячейке — ровно одна группа в ответе, даже если `verseIds` пуст (curriculum-service кэширует пустой результат как значимый, см. `curriculum-service.md` §12) — группа не пропускается молча. Отбор внутри группы (`≤ limitPerGroup` штук) — детерминированный (например, по `verse_id`), чтобы повторный запрос с тем же `limitPerGroup` возвращал тот же набор, а не случайную выборку.

### `POST /sangraha/internal/content/verses/batch`

```json
{ "verseIds": ["uuid1", "uuid2"] }
```

```json
{
  "verses": [
    {
      "verseId": "uuid1",
      "workSlug": "bhagavad-gita",
      "textIast": "...", "textDevanagari": "...",
      "translationRu": "...", "translationEn": "...",
      "workTitleRu": "Бхагавад-гита", "workTitleEn": "Bhagavad Gita",
      "chapterTitleRu": "Глава 1", "chapterTitleEn": "Chapter 1",
      "verseOrderIndex": 1
    }
  ]
}
```

Только стихи со `status = ANALYZED` (только тогда есть перевод и оба варианта письменности, см. §5.1). `verseId` из запроса, для которого стиха нет, стих не `ANALYZED`, или он мягко удалён (`deletedAt != null`) — просто отсутствует в `verses[]` ответа, без ошибки и без указания причины (curriculum-service не обязан и не должен различать «не найден»/«не проанализирован»/«удалён» — во всех случаях цитата для вкладки «Примеры» одинаково недоступна, см. `curriculum-service.md` §12, шаг 4).

---

## 10. Модуль классификации лексем (Lemma / LemmaClassification)

Полная спецификация — [lemma-classification.md](./sangraha-service/lemma-classification.md).
Кратко: sangraha-service агрегирует уникальные леммы по всему корпусу
(`Lemma`, вне контекста конкретного стиха), классифицирует их по одной или
нескольким **схемам** (`ClassificationScheme` — сейчас только `CURRICULUM`,
`WORDNET` зарезервирована на будущее) через batch-вызовы внешней LLM с
одновременным переводом (наиболее вероятный глосс), батчами по частотности
употребления, с ручным ADMIN-review результата. Результат (категория +
перевод) отдаётся curriculum-service через export-эндпоинт и используется
пайплайном наполнения лексикона (`lexicon-content-pipeline.md`) вместо ручной
разметки `semanticClasses`/эвристического перевода.

### `GET /sangraha/internal/content/verse-words/export`

Третий internal-эндпоинт, используется batch-импортом лексикона curriculum-service
(`lexicon-content-pipeline.md` §2) — постраничная выгрузка всех `VerseWord` по
всем стихам `status = ANALYZED`, курсор по `verseId`:

```
GET /sangraha/internal/content/verse-words/export?cursor={verseId}&limit=500
```

```json
{
  "items": [
    { "verseId": "uuid", "workSlug": "bhagavad-gita", "chapterSlug": "1", "verseOrderIndex": 1,
      "lemmaIast": "dhṛtarāṣṭra", "stem": "dhṛtarāṣṭra",
      "surfaceIast": "dhṛtarāṣṭraḥ", "surfaceDevanagari": "धृतराष्ट्रः",
      "pos": "NOUN", "lemmaGlossRu": "...", "lemmaGlossEn": "...",
      "gender": "MASCULINE", "vowelType": "A_STEM" }
  ],
  "nextCursor": "uuid-or-null"
}
```

`gender`/`vowelType` — из `VerseWordMorphology` (nullable, если разбор не дал
значения). `nextCursor = null`, когда строк больше нет. Это единственное место,
где весь корпус читается разом (в отличие от §6/§7, где обработка идёт по
одному стиху) — используется исключительно batch-задачей, не публичным API,
не через gateway.
- **Библиотека текстов (каталог, не курикулум)** — требования к странице библиотеки, стабильному адресу строфы (`произведение.глава.строфа`, используется как `source_ref` в `usage_examples`), двухсекционному поиску по корпусу (произведения + строфы) и полю `license`/`source_type` в модели произведения — см. [frontend/information-architecture.md §3.2 и §7](../frontend/information-architecture/02-catalog.md).