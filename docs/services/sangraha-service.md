# sangraha-service

> Домен: Sangraha (सङ्ग्रह — «собрание, свод») — санскритские произведения: иерархия
> книга → глава → стих, LLM-анализ стиха (транслитерация, перевод, сандхи, грамматика).
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/sangraha-service`
> Порт: `8089` (фиксирован, согласован с Агентом 5 DevOps)
> Схема БД: `sangraha`
> Status: **DRAFT**

---

## 1. Описание

Хранит санскритские тексты (произведения) в виде дерева **Work → Chapter → Verse** и
результаты их LLM-анализа: транслитерация IAST ⇄ devanagari, перевод (ru/en), разбор
сандхи, пословная грамматика.

Сервис **не хранит словарь**. Единственный канал наружу — синхронный REST-вызов
`content-service` (см. §6): по явному действию пользователя (кнопка «Изучить» на
VersePage, §7) sangraha-service отправляет слова конкретного стиха, `content-service`
синхронно строит из них лексический квиз этого стиха и возвращает `quizSlug` и `quizId` (UUID сущности `Lesson` в content-service — фронтенд стартует сессию по UUID напрямую, см. §7).
**ИЗМЕНЕНО:** раньше это был асинхронный обмен через Kafka (топик
`sangraha-vocabulary-events`), затем — автоматический синхронный REST-вызов сразу
после каждого анализа стиха через Transactional Outbox (ADR-006). Оба механизма
убраны: анализ стиха больше не инициирует никакой отправки в content-service —
вызов происходит лениво, по клику пользователя, без Outbox (см. ADR-009). Слово
попадает в content-service только если хотя бы один пользователь захотел изучить
слова этого стиха.
Сопоставление слов со словарными статьями `dictionary-service` **в текущей итерации не
делается** (см. §8).

Разделение ответственности:
- **sangraha-service** — тексты, их структура, LLM-анализ (грамматика стиха)
- **content-service** — лексика для VOCABULARY-квизов (получает слова синхронным REST-вызовом, см. §6)
- **dictionary-service** — полный словарь (MW/Frisch), не связан с sangraha в этой итерации

---

## 2. Сущности

**Work** (таблица works): id (UUID), slug (string, unique), titleRu, titleEn, titleSaIast, titleSaDevanagari, descriptionRu, descriptionEn, author (nullable), createdAt, deletedAt

**Chapter** (таблица chapters): id (UUID), workId (UUID), slug (string, unique в пределах work), orderIndex (int, nullable — backend вычисляет автоматически при создании, см. §5.3), titleRu, titleEn, titleSaIast, titleSaDevanagari, deletedAt

**Verse** (таблица verses): id (UUID), chapterId (UUID), orderIndex (int), rawText (VARCHAR, сырой ввод пользователя до определения письменности — см. §4, §7), textDevanagari (TEXT), textIast (TEXT), status (DRAFT|ANALYZING|ANALYZED|FAILED), createdAt, updatedAt, deletedAt

**VerseAnalysis** (1:1 с Verse, таблица verse_analyses): verseId (UUID, PK), translationRu (TEXT), translationEn (TEXT), sandhiSplits (JSONB), rawModelResponse (JSONB, опционально), modelName, **analyzerName (НОВОЕ, задача Агенту 2 — имя LLM-модели, фактически выполнившей анализ; заполняется тем же значением, что и modelName, отдельная колонка — задел на случай будущего расхождения «настроенная модель» vs «модель, реально ответившая в API»)**, analyzedAt

**VerseWord** (таблица verse_words, задача Агенту 2 — реляционная модель, **НЕ** JSONB) — расширена полным лексико-грамматическим разбором (formType, isFinite, lemmaGlossRu/En, contextGlossRu/En вместо glossRu/En, analysisConfidence, ambiguityNotes); морфология и словообразование вынесены в отдельные таблицы 1:1 — **VerseWordMorphology**, **VerseWordDerivation**. Детали — [sangraha-service/verse-word-grammar.md](./sangraha-service/verse-word-grammar.md).

---

## 3. Flyway Migrations

**ИЗМЕНЕНО (задача Агенту 2): история миграций сведена к одному файлу
`V1__create_schema.sql`** — проект без прод-данных, вместо очередной
миграции поверх пересоздаётся единственный исходный файл в целевом виде
(works/chapters/verses, verse_analyses, новая реляционная verse_words +
verse_word_morphology + verse_word_derivation — см. §2,
[verse-word-grammar.md](./sangraha-service/verse-word-grammar.md));
`outbox_events` не создаётся (ADR-009, см. §6). SQL прислан оркестратором
целиком, файлы V2–V10 удаляются.

**НОВОЕ (задача Агенту 2):** добавить `V2__verse_analyses_add_analyzer_name.sql`
— `ALTER TABLE sangraha.verse_analyses ADD COLUMN analyzer_name varchar(200)`,
NOT NULL с backfill `UPDATE ... SET analyzer_name = model_name` в той же
миграции (см. §2).

---

## 4. API

Права доступа: **весь write-контур — только `ADMIN`** (как в content-service). Отдельная
роль «редактор/переводчик» отложена на будущую итерацию (см. §8). Чтение доступно всем
аутентифицированным пользователям.

```
GET    /api/v1/sangraha/works                                  → плитки произведений
       ?id={workId} (опционально)                              → если id указан — дерево произведения по UUID
GET    /api/v1/sangraha/works/{workSlug}                        → ★ произведение + дерево chapters/verses по slug
                                                                   (основной эндпоинт для фронтенда /sangraha/:workSlug,
                                                                   возвращает id, slug, titleRu/En и chapters[].verses[])

GET    /api/v1/sangraha/verses/{verseId}                         → стих: текст + (если ANALYZED) VerseAnalysis + VerseWord[] +
                                                                     vocabularyQuizSlug/vocabularyQuizId (кэш кнопки «Изучить», null пока
                                                                     не нажата, см. §6, §7, sangraha-schemas.yaml#VerseDetail)
POST   /api/v1/sangraha/verses/{verseId}/vocabulary-quiz         → кнопка «Изучить»: вернуть кэш ({quizSlug, quizId, quizStatus:"EXISTING"})
                                                                     или синхронно создать/дозаполнить лексический квиз стиха в
                                                                     content-service ({quizSlug, quizId, quizStatus}), закэшировать
                                                                     quizSlug/quizId (см. §6, ADR-009)
POST   /api/v1/sangraha/verses/{verseId}/analyze                 → сохранить `text` и запустить LLM-анализ (ADMIN, см. §5); тело —
                                                                     единое поле `text` (обязательно, см. §7) — backend определяет
                                                                     письменность и заполняет textDevanagari/textIast
POST   /api/v1/sangraha/chapters/{chapterId}/verses/analyze-all  → ★ НОВОЕ: кнопка «Анализировать всё» на странице списка стихов главы
                                                                     (ADMIN, см. §5.2) — пакетный LLM-анализ всех стихов главы одним
                                                                     batch-вызовом (см. sangraha-schemas.yaml#AnalyzeAllVersesResponse)
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

**ИЗМЕНЕНО (задача Агенту 2):** tool переименован в `submit_verse_analyses`
(множественное число) — параметры: единственный параметр `verses`, массив
записей `{verseIndex, textDevanagari, textIast, translationRu, translationEn,
sandhiSplits[], words[]}`. Одиночный анализ (`POST /verses/{verseId}/analyze`)
теперь тоже вызывает этот tool, просто с `verses` из одного элемента
(`verseIndex: 0`) — единая кодовая база для одиночного и пакетного режимов,
см. §5.2. Полный список полей `words[]` и JPA-модель хранения —
[verse-word-grammar.md §1–§3](./sangraha-service/verse-word-grammar.md).
Промпт (`prompts/verse-analysis.md`) обновлён оркестратором под batch-схему —
готовый файл-приложение
[`docs/tasks/attachments/B-batch-verse-analysis.md`](../tasks/attachments/B-batch-verse-analysis.md).

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
1. Валидирует `tool_calls[0].function.arguments.verses[]` по этой схеме (например через JSON Schema validator, не доверяем модели).
2. Для каждого элемента `verses[]`, сопоставленного по `verseIndex` со своим стихом, в одной транзакции на стих: обновляет `Verse.textDevanagari/textIast` (если не были заданы вручную), пишет `VerseAnalysis` (перезаписывая предыдущую — см. §8, включая новую колонку `analyzerName`), пересоздаёт `VerseWord[]` для стиха, переводит `Verse.status → ANALYZED`.

**ИЗМЕНЕНО:** шаг записи `OutboxEvent(VERSE_VOCABULARY_EXTRACTED)` убран — анализ стиха больше не инициирует никакой синхронизации с content-service. Слова уходят в content-service только по явному действию пользователя — кнопка «Изучить» на VersePage, см. §6, §7.

Если пользователь ввёл текст только в одном представлении (только devanagari или только
iast) — второе представление также генерирует модель, и backend сохраняет оба.

`analyzerName` (см. §2) заполняется тем же значением, что и `modelName` —
именем модели, фактически ответившей в API (`ChatCompletion.model()`), а не
именем из конфигурации `SANGRAHA_LLM_MODEL` (провайдер может подставить
конкретную версию). См. также §5.2 (пакетный анализ — то же поле).

### 5.2 Пакетный анализ (analyze-all)

`POST /chapters/{chapterId}/verses/analyze-all` (кнопка «Анализировать всё» на
странице списка стихов главы, см. §7):
1. Backend выбирает стихи главы со статусом `DRAFT`/`FAILED` (не `ANALYZING`/
   `ANALYZED` — повторно не трогаем), переводит их в `ANALYZING` одной пачкой.
   Если подходящих стихов нет — `409`.
2. Строит единый batch-запрос к LLM: один system-промпт
   (`prompts/verse-analysis.md`), один user-промпт, перечисляющий тексты всех
   отобранных стихов с их `verseIndex` (см. приложение
   [B-batch-verse-analysis.md](../tasks/attachments/B-batch-verse-analysis.md)),
   один `tool_choice=submit_verse_analyses`.
3. Из ответа читает `arguments.verses[]`, по каждому элементу находит стих по
   `verseIndex` и сохраняет результат той же процедурой, что и одиночный анализ
   (§5.1, п.2) — отдельная транзакция на стих, так что сбой по одному стиху не
   откатывает остальные. Стих, для которого модель не вернула элемент
   `verses[]`, помечается `FAILED`.
4. Отвечает `202` с `AnalyzeAllVersesResponse` (`chapterId`, `verseIds` —
   список стихов, переведённых в `ANALYZING`).

Ограничение на размер batch (сколько стихов отправлять в одном вызове LLM,
разбивать ли главу на несколько batch-вызовов при большом числе стихов) —
открытый вопрос, см. §8.

### 5.3 Удалённые операции

Work/Chapter CRUD удалён. Произведения и главы создаются через импорт
(см. §1). Соответствующие эндпоинты и Java-сервисы удалены:
- `POST /works`, `PUT /works/{workSlug}`, `DELETE /works/{workSlug}`
- `POST /works/{workSlug}/chapters`, `PUT /chapters/{chapterId}`, `DELETE /chapters/{chapterId}`

---

## 6. On-demand REST: sangraha → content-service (по кнопке «Изучить»)

**ИЗМЕНЕНО (было Outbox после каждого анализа, стало on-demand по клику пользователя).**
Транзакционный Outbox (`outbox_events`, `OutboxRelayService`) убран целиком (ADR-009) —
он был нужен только для гарантированной доставки автоматической синхронизации после
каждого анализа стиха; теперь синхронизации после анализа вообще нет, вызов
происходит внутри HTTP-запроса на кнопку «Изучить» — не нужна ни персистентная очередь,
ни ретраи в фоне.

### VocabularyQuizController (новый, endpoint из §7 таблицы API)

`POST /api/v1/sangraha/verses/{verseId}/vocabulary-quiz` — обрабатывается синхронно,
в теле HTTP-запроса от фронтенда:

1. Загрузить `verse.vocabularyQuizSlug`/`vocabularyQuizId` и `VerseWord[]` этого стиха. Если квиз уже закэширован (`vocabularyQuizSlug`/`vocabularyQuizId` не пустые) **и** у каждого `VerseWord` этого стиха уже проставлен `vocabularyWordId` — вернуть `{ quizSlug, quizId, quizStatus: "EXISTING" }` немедленно, без обращения к content-service. Если квиз закэширован, но хотя бы у одного слова `vocabularyWordId` ещё не проставлен (например, слово добавилось при повторном анализе стиха после того, как квиз уже был создан) — не возвращать сразу, а продолжить с шага 2, чтобы дозаполнить недостающие маппинги.
2. Если квиз ещё не закэширован — проверить `verse.status == ANALYZED` и что `VerseWord[]` не пуст (иначе ошибка). Собрать `VerseWord[]` этого стиха, дедуплицировать по `(lemmaIast, stem)` **внутри стиха** (одно и то же слово, встретившееся в стихе дважды, не должно попасть в список дважды; для каждого уникального `(lemmaIast, stem)` в запрос идёт один представитель — первый по порядку `position`).
3. `POST {CONTENT_SERVICE_URL}/content/internal/sangraha/vocabulary-quiz` (синхронный HTTP-клиент — `RestClient`) с телом: `verseId`, `workSlug`, `workTitleRu/En`, `chapterSlug`, `chapterTitleRu/En`, `verseOrderIndex`, `words[]`. Каждый элемент `words[]` собирается из представителя дедупликации по словарным полям — `verseWordId = VerseWord.id`, `wordIast = lemmaIast`, `translationRu/En = lemmaGlossRu/En`, `wordDevanagari = surfaceDevanagari` (у `VerseWord` нет отдельного деванагари-написания леммы, см. `verse-word-grammar.md` §1 — используется деванагари той словоформы, в которой слово встретилось первым; для слов без сандхи/окончания на стыке совпадает с деванагари леммы). Точный контракт — `content-service.md` §11.
4. **Успех (2xx):** из ответа `{ quizSlug, quizId, quizStatus, wordMappings[] }` — если квиз не был закэширован на шаге 1, сохранить `quizSlug`/`quizId` в `verse.vocabularyQuizSlug`/`verse.vocabularyQuizId`. Независимо от того, был ли квиз уже закэширован, разложить `wordMappings[]` (`verseWordId → vocabularyWordId`, где `verseWordId` — id дедуплицированного представителя) обратно на **все** `VerseWord[]` стиха по ключу `(lemmaIast, stem)` — включая слова-дубли внутри стиха, которые не были представителем и не попали в запрос напрямую — и сохранить `vocabularyWordId` у каждого. Ответ фронтенду: `{ quizSlug, quizId, quizStatus }` — `quizStatus` берётся из ответа content-service, если квиз создавался/дозаполнялся в этом вызове, либо `"EXISTING"`, если был кэш-хит на шаге 1.
5. **Ошибка (4xx/5xx/timeout):** вернуть ошибку как есть фронтенду, ничего не сохранять. Повторный клик по кнопке безопасен и идемпотентен — `quizSlug`/`quizId` на стороне content-service детерминированы по `(workSlug, chapterSlug, verseId)` (см. `content-service.md` §11), повторный вызов не создаёт дублей.

Никакой персистентной очереди/ретраев в фоне не требуется — повтор при ошибке равен повторному клику пользователя.

---

## 7. Frontend (эскиз, детализирует Агент 3)

- **Страница произведений** (`/sangraha`) — плитки (`WorkCard`) со списком работ.
- **Страница произведения** (`/sangraha/{workSlug}`) — дерево глав/стихов. Read-only: без кнопок добавления/удаления.
- **Страница списка стихов главы** (`/sangraha/{workSlug}/chapters/{chapterId}`,
  компонент `ChapterPage`, данные — `GET /chapters/{chapterId}/verses`): список
  стихов со статус-иконкой (§4). **НОВОЕ:** кнопка «Анализировать всё» в шапке
  страницы → `POST /chapters/{chapterId}/verses/analyze-all` (§5.2). Кнопка
  видна только ADMIN; disabled, пока в списке нет ни одного стиха со статусом
  `DRAFT`/`FAILED` (все уже `ANALYZING`/`ANALYZED`) — проверяется локально на
  фронте по `verses[].status`. По ответу `202` — инвалидировать кэш списка
  стихов (те же query keys, что и у одиночного анализа), список сам обновит
  статус-иконки на `ANALYZING`/`ANALYZED` при поллинге/повторном заходе —
  отдельного WebSocket/поллинга под это не заводим (см. открытый вопрос §8 про
  live-обновление статуса).
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
      (sangraha-service, см. §6, ADR-009): если у стиха уже есть закэшированные
      `vocabularyQuizSlug`/`vocabularyQuizId` и все слова стиха уже связаны со
      словарными статьями — sangraha-service возвращает их сразу с
      `quizStatus="EXISTING"` (см. §6, шаг 1); иначе сам синхронно
      создаёт/дозаполняет лексический квиз в content-service и кэширует
      `quizSlug`/`quizId`.
    - Из ответа `{ quizSlug, quizId, quizStatus }` фронтенд **не** делает
      промежуточный `GET /api/v1/lessons/vocabulary/{slug}` — он не нужен для
      запуска сессии. Вместо этого сразу вызывает
      `POST /api/v1/quiz/vocabulary/sessions/start-or-resume?lessonId={quizId}&statusFilter={statusFilter}`
      (quiz-service, `quiz-generator-spec.md` §3-4) — по UUID, не по slug.
      `statusFilter` вычисляется на фронте из `quizStatus`: `quizStatus="CREATED"` →
      `statusFilter=NEW` (весь пул квиза — новые слова, обычный смешанный
      due/new/reserve-отбор не нужен, слов ещё никто не проходил); `quizStatus="EXISTING"`
      → `statusFilter` не передаётся (обычный смешанный отбор).
    - После успешного старта сессии — переход на `/quiz/vocabulary/{quizSlug}/{sessionId}`
      (существующий маршрут `QuizPage`, см. `frontend-state.md`/`AppRoutes`) с передачей
      результата через `navigate(url, { state: { sessionData } })` — тот же паттерн,
      что уже поддержан в `useQuizSession` (ветка 1 «navigation state»), просто
      раньше не имел рабочего вызывающего кода для VOCABULARY-квизов.
    - Квиз — на уровне **стиха**, а не произведения (см. ADR-009 — заменяет
      прежнее решение §8 «Quiz только на уровне произведения»). Уникальность слов
      внутри квиза стиха обеспечивает sangraha-service (дедуп по `(lemmaIast, stem)`
      перед отправкой, см. §6) — content-service дополнительно дедуплицирует
      `VocabularyWord` по `(wordIast, stem)` в рамках всего своего словаря (см.
      `content-service.md` §11), но это не влияет на состав слов конкретного квиза.

---

## 8. Открытые вопросы / отложено

- **Live-обновление статуса стиха на ChapterPage после «Анализировать всё»**: пока без поллинга/WebSocket — статус обновляется только при повторном заходе/ручном refresh страницы; поллинг раз в N секунд, пока в главе есть `ANALYZING`-стихи, можно добавить позже без изменения контракта.
- **Размер batch для «Анализировать всё»** (см. §5.2): пока один LLM-вызов на всю главу; лимит числа стихов на вызов и стратегия разбиения больших глав на несколько batch-вызовов — решает Агент 2 по факту реальных размеров глав/токен-лимитов провайдера.
- **Таблица соответствия IAST↔SLP1 для slug** (см. §5.3): конкретный набор правил
  транслитерации выбирает Агент 2 при реализации на основе общепринятых схем IAST/SLP1.
- **Роль «редактор/переводчик»**: пока весь write — `ADMIN`. Отдельная роль (может вводить/анализировать стихи, но не управлять произведениями/главами) — следующая итерация; когда будет готова модель ролей, добавить `SANGRAHA_EDITOR` и обновить §4.
- **Связь слов стиха со словарём** (`dictionary-service`, поиск по `slp1`): сознательно не делаем в этой итерации — только грамматика от LLM. Если понадобится — отдельным Kafka-каналом (sangraha публикует, dictionary-service асинхронно обогащает через ответное событие), без синхронных вызовов между сервисами.
- **Политика ретраев Outbox Relay** — снято: Outbox убран целиком (ADR-009), синхронизация теперь происходит внутри HTTP-запроса на кнопку «Изучить», отдельного ретрая в фоне нет — повтор равен повторному клику пользователя.
- **Quiz(VOCABULARY) — уровень квиза** — решено иначе, чем раньше: квиз теперь на уровне **стиха** (`slug = "{workSlug}.{chapterSlug}.verse-{verseId}"`), а не произведения — см. ADR-009, §6, §7. Прежнее решение «только на уровне произведения» отменено этим ADR.
- **Библиотека текстов (каталог, не курикулум)** — требования к странице библиотеки, стабильному адресу строфы (`произведение.глава.строфа`, используется как `source_ref` в `usage_examples`), двухсекционному поиску по корпусу (произведения + строфы) и полю `license`/`source_type` в модели произведения — см. [frontend/information-architecture.md §3.2 и §7](../frontend/information-architecture/02-catalog.md).