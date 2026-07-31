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

**VerseAnalysis** (1:1 с Verse, таблица verse_analyses): verseId (UUID, PK), translationRu (TEXT), translationEn (TEXT), sandhiSplits (JSONB), rawModelResponse (JSONB, опционально), modelName, analyzedAt

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
POST   /api/v1/sangraha/verses/{verseId}/vocabulary-quiz         → кнопка «Изучить»: вернуть кэш ({quizSlug, quizId}, без quizStatus) или
                                                                     синхронно создать лексический квиз стиха в content-service
                                                                     ({quizSlug, quizId, quizStatus}), закэшировать quizSlug/quizId
                                                                     (см. §6, ADR-009)
POST   /api/v1/sangraha/verses/{verseId}/analyze                 → сохранить `text` и запустить LLM-анализ (ADMIN, см. §5); тело —
                                                                     единое поле `text` (обязательно, см. §7) — backend определяет
                                                                     письменность и заполняет textDevanagari/textIast
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
ruleNumbers[]}), words — **ИЗМЕНЕНО**, полный лексико-грамматический разбор
каждого слова, точный список полей и JPA-модель хранения —
[verse-word-grammar.md §1–§3](./sangraha-service/verse-word-grammar.md).
Промпт (`prompts/verse-analysis.md` §4) обновлён оркестратором под эту модель.

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

**ИЗМЕНЕНО:** шаг записи `OutboxEvent(VERSE_VOCABULARY_EXTRACTED)` убран — анализ стиха больше не инициирует никакой синхронизации с content-service. Слова уходят в content-service только по явному действию пользователя — кнопка «Изучить» на VersePage, см. §6, §7.

Если пользователь ввёл текст только в одном представлении (только devanagari или только
iast) — второе представление также генерирует модель, и backend сохраняет оба.

### 5.2 Удалённые операции

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

1. Если `verse.vocabularyQuizSlug != null` — вернуть `{ quizSlug: verse.vocabularyQuizSlug, quizId: verse.vocabularyQuizId }` немедленно, никаких вызовов наружу. **`quizStatus` не возвращается на кэш-хите** — content-service не переспрашивается, а значит не может подтвердить/опровергнуть «только что создан»; фронтенд трактует отсутствие `quizStatus` как «обычный смешанный отбор» (см. §7).
2. Иначе — собрать `VerseWord[]` этого стиха, дедуплицировать по `(lemmaIast, stem)` **внутри стиха** (одно и то же слово, встретившееся в стихе дважды, не должно попасть в список дважды).
3. `POST {CONTENT_SERVICE_URL}/content/internal/sangraha/vocabulary-quiz` (синхронный HTTP-клиент — `RestClient`, как и раньше) с телом: `verseId`, `workSlug`, `workTitleRu/En`, `chapterSlug`, `chapterTitleRu/En`, `verseOrderIndex`, `words[]`. Точный контракт — `content-service.md` §11.
4. **Успех (2xx):** из ответа `{ quizSlug, quizId, quizStatus }` — сохранить `quizSlug`/`quizId` в `verse.vocabularyQuizSlug`/`verse.vocabularyQuizId` (`quizStatus` — транзитное поле, не кэшируется на `Verse`, прокидывается на фронт как есть только в этом первом ответе).
5. **Ошибка (4xx/5xx/timeout):** вернуть ошибку как есть фронтенду, ничего не сохранять. Повторный клик по кнопке безопасен и идемпотентен — `quizSlug`/`quizId` на стороне content-service детерминированы по `verseId` (см. `content-service.md` §11), повторный вызов не создаёт дублей.

Никакой персистентной очереди/ретраев в фоне не требуется — повтор при ошибке равен повторному клику пользователя.

---

## 7. Frontend (эскиз, детализирует Агент 3)

- **Страница произведений** (`/sangraha`) — плитки (`WorkCard`) со списком работ.
- **Страница произведения** (`/sangraha/{workSlug}`) — дерево глав/стихов. Read-only: без кнопок добавления/удаления.
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
      `vocabularyQuizSlug`/`vocabularyQuizId` — sangraha-service возвращает их сразу
      (`quizStatus` в этом случае не возвращается, см. §6); иначе — сам синхронно
      создаёт лексический квиз в content-service и кэширует оба поля.
    - **ИЗМЕНЕНО:** из ответа `{ quizSlug, quizId, quizStatus? }` фронтенд **не**
      делает промежуточный `GET /api/v1/lessons/vocabulary/{slug}` — он и раньше
      был не нужен для запуска сессии (лишний шаг, оставался в первой версии этой
      задачи по инерции с `VocabularyLessonPage`). Вместо этого сразу вызывает
      `POST /api/v1/quiz/vocabulary/sessions/start-or-resume?lessonId={quizId}&statusFilter={statusFilter}`
      (quiz-service, `quiz-generator-spec.md` §3-4) — по UUID, не по slug.
      `statusFilter` вычисляется на фронте из `quizStatus`: `quizStatus="CREATED"` →
      `statusFilter=NEW` (весь пул квиза — новые слова, обычный смешанный
      due/new/reserve-отбор не нужен, слов ещё никто не проходил); `quizStatus="EXISTING"`
      либо не пришёл (кэш-хит) → `statusFilter` не передаётся (обычный смешанный отбор).
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

- **Таблица соответствия IAST↔SLP1 для slug** (см. §5.2): конкретный набор правил
  транслитерации выбирает Агент 2 при реализации на основе общепринятых схем IAST/SLP1.
- **Роль «редактор/переводчик»**: пока весь write — `ADMIN`. Отдельная роль (может вводить/анализировать стихи, но не управлять произведениями/главами) — следующая итерация; когда будет готова модель ролей, добавить `SANGRAHA_EDITOR` и обновить §4.
- **Связь слов стиха со словарём** (`dictionary-service`, поиск по `slp1`): сознательно не делаем в этой итерации — только грамматика от LLM. Если понадобится — отдельным Kafka-каналом (sangraha публикует, dictionary-service асинхронно обогащает через ответное событие), без синхронных вызовов между сервисами.
- **Политика ретраев Outbox Relay** — снято: Outbox убран целиком (ADR-009), синхронизация теперь происходит внутри HTTP-запроса на кнопку «Изучить», отдельного ретрая в фоне нет — повтор равен повторному клику пользователя.
- **Quiz(VOCABULARY) — уровень квиза** — решено иначе, чем раньше: квиз теперь на уровне **стиха** (`slug = "sangraha-verse-{verseId}"`), а не произведения — см. ADR-009, §6, §7. Прежнее решение «только на уровне произведения» отменено этим ADR.
- **Библиотека текстов (каталог, не курикулум)** — требования к странице библиотеки, стабильному адресу строфы (`произведение.глава.строфа`, используется как `source_ref` в `usage_examples`), двухсекционному поиску по корпусу (произведения + строфы) и полю `license`/`source_type` в модели произведения — см. [frontend/information-architecture.md §3.2 и §7](../frontend/information-architecture/02-catalog.md).