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
`curriculum-service` (см. §6, [architecture.md §3.5](../architecture.md#35-sangraha-service-произведения-llm-анализ-стихов-синхронизация-лексики-через-rest)): после успешного анализа sangraha-service отправляет инкрементальную пачку лемм стиха, а кнопка «Изучить» на VersePage (§7) делает тот же push on-demand (идемпотентно) и возвращает код VERSE-урока для перехода в curriculum-service.
Анализ стиха сохраняется независимо от успеха отправки: пачка уходит из транзакции анализа, любой сбой curriculum-service только логируется (см. §6, без фонового Outbox/relay).
Сопоставление слов со словарными статьями `dictionary-service` **в текущей итерации не
делается** (см. §8).

Разделение ответственности:
- **sangraha-service** — тексты, их структура, LLM-анализ (грамматика стиха)
- **curriculum-service** — лексика лексикона и VERSE-уроки (получает пачки лемм синхронным REST-вызовом, см. §6)
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
                                                                     verseTopicCode (код VERSE-урока кнопки «Изучить», см. §6, §7,
                                                                     sangraha-schemas.yaml#VerseDetail)
GET    /api/v1/sangraha/verses/{verseId}/analysis                → сырая сущность VerseAnalysis (отладка/выгрузка; основной путь —
                                                                     GET /verses/{verseId}, вложенный analysis)
GET    /api/v1/sangraha/verses/{verseId}/words                   → сырые сущности VerseWord[] стиха (отладка/выгрузка; основной
                                                                     путь — GET /verses/{verseId}, вложенные words)
POST   /api/v1/sangraha/verses/{verseId}/study                   → кнопка «Изучить»: идемпотентный on-demand push пачки лемм стиха
                                                                     в curriculum-service и возврат { verseTopicCode } для перехода
                                                                     на VERSE-урок (см. §6)
POST   /api/v1/sangraha/verses/{verseId}/analyze                 → сохранить `text` (тело опционально: пустое — переанализ по уже
                                                                     сохранённому rawText) и запустить LLM-анализ (ADMIN, см. §5);
                                                                     202 без тела — backend определяет письменность и запускает анализ
POST   /api/v1/sangraha/chapters/{chapterId}/verses/analyze-all  → батч-анализ всех DRAFT/FAILED стихов главы (ADMIN, реализовано,
                                                                     )
POST   /api/v1/sangraha/verse                                 → произвольный список стихов + status каждого
                                                                      (тело — { verseIds: UUID[] }), см. sangraha-service/batch-verse-review.md
POST   /api/v1/sangraha/verse/analysis                           → батч-анализ произвольного списка verseId (ADMIN,
                                                                        безусловный повтор), см. sangraha-service/batch-verse-review.md
POST   /api/v1/sangraha/verses/{verseId}/internal-sandhi         → ШАГ 2 (внутренние сандхи) для одного стиха:
                                                                        перевызывает LLM, дописывает words[].formationRuleNumbers
                                                                        в существующие VerseWord (стих должен быть ANALYZED). 202,
                                                                        не стартует автоматически после ШАГА 1 (ADMIN)
POST   /api/v1/sangraha/chapters/{chapterId}/verses/internal-sandhi → ШАГ 2 для всех ANALYZED-стихов главы
                                                                        (SAME_WORK, батч; ADMIN)
POST   /api/v1/sangraha/verse/internal-sandhi                    → ШАГ 2 для произвольного списка verseId в теле
                                                                        { verseIds: [...] } (MIXED_WORKS, батч; ADMIN)
POST   /api/v1/sangraha/analysis                                 → страница /analysis: создать standalone-стих (chapter_id = null,
                                                                      owner_id = X-User-Id) и сразу запустить LLM-анализ; каждое
                                                                      нажатие «Анализировать» создаёт новую запись (любой авторизованный,
                                                                      персональные стихи)
GET    /api/v1/sangraha/analysis                                 → список standalone-стихов текущего пользователя (новые сверху,
                                                                      только превью/статус/createdAt — без контекста произведения/главы)
DELETE /api/v1/sangraha/analysis/{verseId}                       → мягкое удаление standalone-стиха (только владелец)
POST   /api/v1/sangraha/words/examples                           → примеры стихов по точной словоформе (тело — { surfaceIasts,
                                                                       limitPerForm? }); до limitPerForm стихов на форму, перевод
                                                                       (translationRu/En) может отсутствовать, форма без совпадений —
                                                                       пустой verses (колонка «примеры из санграхи» урока склонений)
POST   /api/v1/sangraha/words/examples-by-lemma                   → примеры стихов по лемме (словарной форме, lemmaIast); тело —
                                                                       { lemmas, limitPerLemma? } (по умолчанию 5 стихов на лемму);
                                                                       раскрываемые строки таблицы слов лексического урока, лемма без
                                                                       совпадений — пустой verses
```

Ответ `GET /works/{workSlug}` (и `GET /works?id={workId}`) — двухуровневое дерево для TreeGrid:
Ответ — двухуровневое дерево: { id, slug, titleRu, chapters[] { id, slug, titleRu, orderIndex, categoryCode, verses[] { id, orderIndex, textIastPreview, status } } }

---

## 5. LLM-интеграция

### 5.1 Анализ стиха (tool calling)

**Сохранение текста перед анализом.** `POST /verses/{verseId}/analyze` принимает
**опциональное** тело с полем `text`: фронтенд отправляет текущее значение
единственного поля ввода при первом анализе; пустое тело — переанализ по уже
сохранённому `rawText`. Backend сохраняет `text` (если передан) как есть в
`Verse.rawText`, затем детектирует письменность по
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

Анализ разбит на два последовательных шага (каждый — отдельный tool-calling вызов):

- **ШАГ 1 (активен сейчас):** translation + external sandhi + полный
  лексико-морфологический разбор слов. Промпт —
  [`prompts/2/step1-translation-external-sandhi.md`](./prompts/2/step1-translation-external-sandhi.md),
  tool `submit_verse_analyses_step1`. **Без** `words[].formationRuleNumbers`
  (внутренние сандхи — это ШАГ 2). Режим `BATCH_CONTEXT_MODE` выбирается backend-ом по
  точке входа: «Анализировать все» из страницы главы → `SAME_WORK` (все стихи одного
  произведения, в промпт добавляется `workTitle`); страница списка стихов
  (`/sangraha/verses`) → `MIXED_WORKS`.
- **ШАГ 2 (реализован, НЕ автоматический):** внутренние сандхи (formationRuleNumbers,
  правила 1–40), потребляет `words[]`, сохранённые ШАГОМ 1. Промпт —
  [`prompts/2/step2-internal-sandhi.md`](./prompts/2/step2-internal-sandhi.md),
  tool `submit_word_formations` (плоский массив `words` с `verseIndex`/`position` для
  джойна к записи ШАГА 1). На ШАГЕ 1 поле `formationRuleNumbers` в `VerseWord`
  остаётся `NULL` — это маркер «требуется ШАГ 2». ШАГ 2 **не запускается автоматически**
  после ШАГА 1: он вызывается только явным эндпоинтом (см. ниже) и лишь дописывает
  `formationRuleNumbers` (плюс при необходимости уточняет `derivation`) в уже
  существующие `VerseWord`; статус стиха (`ANALYZED`) не меняется. Пустой
  `formationRuleNumbers: []` после ШАГА 2 отличает «внутренних сандхи нет» от
  «ШАГ 2 ещё не запускался» (поле остаётся `NULL`).

  Эндпоинты ШАГА 2 (ADMIN, как и анализ):
  - `POST /api/v1/sangraha/verses/{verseId}/internal-sandhi` — один стих (должен быть `ANALYZED`).
  - `POST /api/v1/sangraha/chapters/{chapterId}/verses/internal-sandhi` — все `ANALYZED`-стихи главы (`SAME_WORK`).
  - `POST /api/v1/sangraha/verse/internal-sandhi` — произвольный список `verseId` в теле `{ verseIds: [...] }` (`MIXED_WORKS`).

Backend вызывает `/chat/completions` (или `/responses`) с промптом шага 1 и
**одним** объявленным tool (`submit_verse_analyses_step1`) — модель обязана
вернуть результат через `tool_calls`, а не свободным текстом.

Tool `submit_verse_analyses_step1` с параметрами: textIast, translationRu,
translationEn, sandhiSplits (массив {surface, components[], ruleNumbers[]}),
words — полный лексико-грамматический разбор каждого слова (без
formationRuleNumbers), точный список полей и JPA-модель хранения —
[verse-word-grammar.md §1–§3](./sangraha-service/verse-word-grammar.md).

**Письменность:** везде, кроме `textDevanagari` и `surfaceDevanagari`, — только IAST.
В частности, `sandhiSplits.surface`/`components`, `lemmaIast`, `stem`, `root` — IAST,
даже если исходный текст стиха был введён только в деванагари. Это нужно проверять
при валидации ответа LLM (наличие символов деванагари в этих полях — признак ошибки
модели).

Справочник правил сандхи (нумерация 1–71, из Эмено, с глоссарием фонетических
терминов) разбит на два файла:
[`prompts/2/emenau-sandhi-rules-external.json`](./prompts/2/emenau-sandhi-rules-external.json)
(41–71, внешние) и
[`prompts/2/emenau-sandhi-rules-internal.json`](./prompts/2/emenau-sandhi-rules-internal.json)
(1–40, внутренние). На ШАГЕ 1 backend передаёт модели **только внешние** правила
(41–71); внутренние правила задействуются на ШАГЕ 2.

Правила `applicability=external` (41–71) — граница между словами, цитируются в
`sandhiSplits.ruleNumbers`. Правила `applicability=internal` (1–40) — как образована
сама словоформа из корня/основы (морфофонемные изменения при словообразовании),
цитируются в `words[].formationRuleNumbers` на ШАГЕ 2; **не путать поля местами**.
Если сандхи на стыке слов нет — граница не попадает в `sandhiSplits`; если словоформа
не требует объяснения через внутренние сандхи — `formationRuleNumbers: []`. Если
правило неясно в любом из двух случаев — пустой массив, не угадывать номер.

Backend:
1. Валидирует `tool_calls[0].function.arguments` по этой схеме (например через JSON Schema validator, не доверяем модели).
2. В одной транзакции: обновляет `Verse.textDevanagari/textIast` (если не были заданы вручную), пишет `VerseAnalysis` (перезаписывая предыдущую — см. §8), пересоздаёт `VerseWord[]` для стиха, переводит `Verse.status → ANALYZED`.

Анализ стиха **инициирует** синхронизацию лексики с curriculum-service: после
успешного сохранения результатов sangraha отправляет инкрементальную пачку лемм
стиха (`VerseBatchPushService.push`, вне транзакции, сбои только логируются — анализ
не откатывается). Кнопка «Изучить» на VersePage дополнительно делает тот же push
on-demand (идемпотентно) и возвращает код VERSE-урока (см. §6).

Если пользователь ввёл текст только в одном представлении (только devanagari или только
iast) — второе представление также генерирует модель, и backend сохраняет оба.

### 5.2 Удалённые операции

Work/Chapter CRUD удалён. Произведения и главы создаются через импорт
(см. §1). Соответствующие эндпоинты и Java-сервисы удалены:
- `POST /works`, `PUT /works/{workSlug}`, `DELETE /works/{workSlug}`
- `POST /works/{workSlug}/chapters`, `PUT /chapters/{chapterId}`, `DELETE /chapters/{chapterId}`

---

## 6. Push пачек лемм в curriculum-service и кнопка «Изучить» (VERSE-урок)

Лексика стиха уходит в curriculum-service **маленькими пачками по стихам**,
направление «наоборот» (разовый первичный импорт «curriculum тянет весь экспорт»
остаётся, см. `curriculum-service/lexicon-content-pipeline.md` §2; этот раздел — §7
того же документа). Старый on-demand поток «кнопка → `POST
/content/internal/sangraha/vocabulary-quiz` → `content.vocabulary_words`»
**удалён** вместе со своей таблицей (`lexicon-content-pipeline.md` §5): его место
заняли инкрементальные пачки в `curriculum.lexeme` и VERSE-урок как `Topic домен=VERSE`.

### Push после успешного анализа (фоновый, не блокирует анализ)

После успешного `analysisSaver.saveResults(...)` (см. §5.1)
`VerseBatchPushService.push` отправляет инкрементальную пачку лемм стиха в
`POST /api/v2/lexicon/import/verse-batch` (контракт `VerseLemmaBatch`; слова —
дедуп по `(lemmaSlp1, gender)` внутри стиха). Вызов — **вне** транзакции анализа и
**не блокирует** его: любой сбой — недоступность curriculum-service,
`app.curriculum-service.url` не задан (push отключается) — только логируется,
анализ стиха уже сохранён и не откатывается. Повторная отправка той же пачки
(переанализ стиха) идемпотентна: дубли не создаются.

### Кнопка «Изучить» — `POST /api/v1/sangraha/verses/{verseId}/study`

Обрабатывается синхронно в теле запроса фронтенда (`VerseService.triggerStudyExport`):

1. Загрузить стих; если `status != ANALYZED` — `409`.
2. Если у стиха заполнен `chapterId` — найти `Chapter`/`Work` (нужны только для
   кода урока; для standalone-стиха контекста нет).
3. Повторный on-demand push пачки лемм этого стиха (`VerseBatchPushService.push`) —
   идемпотентен, сбой не валит запрос (только лог).
4. Вернуть код VERSE-урока `{ verseTopicCode }`:
   - стих в главе — `"{workSlp1}_{chapterNumber}"` (slug произведения в SLP1 +
     номер главы `orderIndex`); тот же код строит curriculum-service при создании
     урока пачки (`lexicon-content-pipeline.md` §7, шаг 3);
   - standalone-стих (без главы) — персональный `"user-{ownerId}"` (совпадает с
     фоновым push).
   Если урок не резолвится (стих без главы и без владельца) — `409`.

Обработка ошибок: недоступность curriculum-service при push не влияет на ответ —
вернётся только код урока; кнопку можно жать повторно (идемпотентно). Отдельная
очередь/ретраи в фоне не нужны.

Тот же код лежит в `VerseDetail.verseTopicCode` (см. §7) — фронтенд показывает
состояние урока (`useVocabularyLesson(verseTopicCode)`) ещё до клика
через `GET /lessons/vocabulary/{code}`.

---

## 7. Frontend (эскиз, детализирует Агент 3)

- **Страница произведений** (`/sangraha`) — плитки (`WorkCard`) со списком работ.
- **Страница произведения** (`/sangraha/{workSlug}`) — дерево глав/стихов. Read-only: без кнопок добавления/удаления.
- **Страница массового просмотра/анализа** (`/sangraha/verses`, ADMIN-only, `id` из query-параметров либо из localStorage `sangraha.verseBatchIds`) — см. `sangraha-service/batch-verse-review.md`.
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
    - Иконка кнопки отражает прогресс VERSE-урока из `useVocabularyLesson(verseTopicCode)`
      (`statusSummary`: mastery/learning/reviewDue/total из `GET /lessons/vocabulary/{code}`):
      всё замастерено — `pi-check-circle`, есть learning/reviewDue — `pi-caret-right`,
      иначе `pi-book`.
    - По клику — `POST /api/v1/sangraha/verses/{verseId}/study` (sangraha-service,
      см. §6): sangraha идемпотентно пушит пачку лемм стиха в curriculum-service и
      возвращает `{ verseTopicCode }`.
    - Из ответа берётся код VERSE-урока и выполняется переход на
      `/lessons/vocabulary/{code}` — страницу лексического урока в curriculum-service,
      без промежуточных запросов к quiz/content-service (старый поток `vocabulary-quiz`
      с `quizSlug`/`quizId`/`quizStatus` удалён, см. `lexicon-content-pipeline.md` §5).
    - Урок создаётся на уровне **пары «произведение, глава»** (не на произведение):
      код `"{slp1_work}_{chapterNumber}"`, связь лексем накапливается по стихам главы
      (`lexicon-content-pipeline.md` §7, шаг 3). Уникальность слов в уроке обеспечивает
      sangraha-дедуп по `(lemmaSlp1, gender)` перед пачкой, а curriculum-service
      дополнительно дедуплицирует `Lexeme` по значению по всему словарю.

---

## 8. Открытые вопросы / отложено

- **Таблица соответствия IAST↔SLP1 для slug** (см. §5.2): конкретный набор правил
  транслитерации выбирает Агент 2 при реализации на основе общепринятых схем IAST/SLP1.
- **Роль «редактор/переводчик»**: пока весь write — `ADMIN`. Отдельная роль (может вводить/анализировать стихи, но не управлять произведениями/главами) — следующая итерация; когда будет готова модель ролей, добавить `SANGRAHA_EDITOR` и обновить §4.
- **Связь слов стиха со словарём** (`dictionary-service`, поиск по `slp1`): сознательно не делаем в этой итерации — только грамматика от LLM. Если понадобится — отдельным REST-каналом (sangraha отправляет, dictionary-service обогащает), без синхронных вызовов между сервисами.
- **Политика ретраев** — не требуется: синхронизация происходит внутри HTTP-запроса на кнопку «Изучить», отдельного фонового Outbox/relay нет, повтор равен повторному клику пользователя.
- **VERSE-урок лексики** — на уровне пары «произведение, глава» в curriculum-service:
  `code = "{slp1_work}_{chapterNumber}"`, связь лексем накапливается по стихам главы;
  для standalone-стихов — персональный `user-{ownerId}` (см. §6,
  `lexicon-content-pipeline.md` §7).

## 9. Примеры склонений: публичный поиск и internal-эндпоинты

Вкладка «Примеры» на странице урока склонений делает **один** запрос на урок —
`POST /api/v1/sangraha/verses/examples` (публичный, через api-gateway): sangraha сам
агрегирует примеры по всей парадигме (по ячейкам `(caseType, numberType)`) и отдаёт
текст/перевод стихов. Фронтенд не ходит в sangraha за каждой ячейкой и не склеивает
результат (раньше эту агрегацию делал quiz-service — вызов
`GET /api/v1/content/public/lessons/{slug}/examples`, удалён).

Агрегация внутри sangraha — `VerseWordSearchService.searchExamples`: весь поиск — **один**
запрос к БД `findDeclensionExampleCells`: кортежи `[stemClass, gender, caseType, numberType]`
из `grammar_info.tuples` служат только фильтром стихов-кандидатов (через `@>`), а ячейки
парадигмы стиха берутся из именованных полей `grammar_info.caseType`/`grammar_info.numberType`;
SQL раздаёт до `limitPerGroup` стихов на ячейку через
`ROW_NUMBER() PARTITION BY (caseType, numberType)`, затем один батч-запрос текстов через
`VerseBatchService`. `gender`/`caseType`/`numberType` из запроса опциональны: не заполнены — фильтр
по соответствующей оси не применяется (стихи с любым родом/падежом/числом). Ответ: `groups[]` —
по группе на каждую непустую ячейку (`caseType`, `numberType`, `examples[]` — с
`textIast`/`textDevanagari`/`translationRu`/`translationEn` и атрибуцией стиха); стих,
покрывающий несколько ячеек, попадает в каждую, в батч идёт один раз (distinct).

Запрос (тело):

```json
{
  "vowelType": "A_STEM",
  "gender": "MASCULINE",
  "caseType": "NOMINATIVE",
  "numberType": "SINGULAR",
  "limitPerGroup": 3
}
```

`gender`/`caseType`/`numberType` — значения тех же enum'ов, что в `VerseWordMorphology` (`Gender`, `GrammaticalCase`, `NumberType`, см. `verse-word-grammar.md` §1); имена значений совпадают с одноимёнными enum'ами curriculum-service (`content.Gender/CaseType/NumberType`) один в один, поэтому маппинг — только сериализация имени enum. `vowelType` — значения = `declension_stems.vowel_type` curriculum-service (`ck_vowel_type`, см. `curriculum-service` V13-миграцию): 7 регулярных классов основы (`A_STEM`, `AA_STEM`, `I_STEM`, `II_STEM`, `U_STEM`, `UU_STEM`, `R_STEM`) + 8 местоимённых (`PRON_AHAM`, `PRON_TVAM`, `PRON_TAD`, `PRON_ETAD`, `PRON_IDAM`, `PRON_KIM`, `PRON_YAD`, `PRON_REFLEXIVE`).

Для всех классов (регулярных и `PRON_*`) поиск идёт одним и тем же запросом `findDeclensionExampleCells` по предвычисленному полю `tuples` в `verse_statistics.grammar_info` (`[stemClass, gender, caseType, numberType]` на слово, только фильтр): `stemClass` — из `nominal_lemmas` (join по тексту `lemma_iast`); для `PRON_*` в `stemClass` лежит соответствующее значение `PRON_*`. Отбор (`≤ limitPerGroup`) и ранжирование — детерминированные, на стороне SQL (по `word_count`, при равенстве — по `verse_id`), чтобы повторный запрос с тем же `limitPerGroup` возвращал тот же набор, а не случайную выборку.

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

### Предвычисленная статистика стихов (`verse_statistics`)

Два вкладчика семантики стиха, пересчитываемые на `POST /sangraha/internal/lexicon/lemmas/refresh-statistics` (upsert по PK `verse_id`, см. `lemma-classification.md` §1.4):

- `word_count` — число слов стиха (COUNT `verse_words`), без COUNT при каждом поиске (§9, фильтр `< maxPhraseWords`);
- `grammar_info` — JSON с грамматическим составом стиха:
  ```json
  { "pos": ["NOUN", "VERB"], "formType": ["FINITE", "PARTICIPLE"],
    "numberType": ["SINGULAR", "PLURAL"], "caseType": ["NOMINATIVE", "ACCUSATIVE"],
    "gender": ["MASCULINE", "NEUTER"],
    "tuples": [["A_STEM","MASCULINE","NOMINATIVE","SINGULAR"],
               ["U_STEM","NEUTER","ACCUSATIVE","SINGULAR"]] }
  ```
  Поля `pos`–`gender` — distinct-массивы признаков по всему стиху. Поле `tuples` — массив кортежей `[stemClass, gender, caseType, numberType]` (один кортеж на слово, distinct), только для фильтрации стихов через `@>`, без join на `verse_words`/`verse_word_morphology`/`nominal_lemmas`; ячейки парадигмы стиха берутся из именованных полей `caseType`/`numberType`.
  Источник — `verse_words` (`pos`, `form_type`), `verse_word_morphology` (`number_type`, `case_type`, `gender`) и `nominal_lemmas` (`stem_class`, join по `verse_words.lemma_iast`); `NULL`-значения не попадают в массивы. GIN-индекс `idx_verse_statistics_grammar_info`: поиск — `grammar_info @> '{"tuples": [["A_STEM","MASCULINE","NOMINATIVE","SINGULAR"]]}'`.

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