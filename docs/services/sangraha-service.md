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

Сервис **не хранит словарь и не ходит синхронно ни в `content-service`, ни в
`dictionary-service`**. Единственный канал наружу — Kafka: после анализа стиха
sangraha-service публикует слова этого стиха, `content-service` асинхронно строит из
них категории лексики и словарные квизы (см. §6). Сопоставление слов со словарными
статьями `dictionary-service` **в текущей итерации не делается** (см. §8).

Разделение ответственности:
- **sangraha-service** — тексты, их структура, LLM-анализ (грамматика стиха)
- **content-service** — лексика для VOCABULARY-квизов (получает слова из Kafka)
- **dictionary-service** — полный словарь (MW/Frisch), не связан с sangraha в этой итерации

---

## 2. Сущности

**Work** (таблица works): id (UUID), slug (string, unique), titleRu, titleEn, titleSaIast, titleSaDevanagari, descriptionRu, descriptionEn, author (nullable), createdAt, deletedAt

**Chapter** (таблица chapters): id (UUID), workId (UUID), slug (string, unique в пределах work), orderIndex (int, nullable — backend вычисляет автоматически при создании, см. §5.3), titleRu, titleEn, titleSaIast, titleSaDevanagari, deletedAt

**Verse** (таблица verses): id (UUID), chapterId (UUID), orderIndex (int), rawText (VARCHAR, сырой ввод пользователя до определения письменности — см. §4, §7), textDevanagari (TEXT), textIast (TEXT), status (DRAFT|ANALYZING|ANALYZED|FAILED), createdAt, updatedAt, deletedAt

**VerseAnalysis** (1:1 с Verse, таблица verse_analyses): verseId (UUID, PK), translationRu (TEXT), translationEn (TEXT), sandhiSplits (JSONB), rawModelResponse (JSONB, опционально), modelName, analyzedAt

**VerseWord** (таблица verse_words): id (UUID), verseId (UUID), position (int), surfaceIast, surfaceDevanagari, lemmaIast, stem, root (опционально), pos, gender, caseType, numberType, person, tense, mood, voice, glossRu, glossEn

---

## 3. Flyway Migrations

4 миграции: V1 — вся схема sangraha (works, chapters, verses, verse_analyses, verse_words, outbox_events); V2 — санскритские названия (titleSaIast, titleSaDevanagari) для works; V3 — санскритские названия для chapters + orderIndex nullable; V4 — поле formation_rule_numbers в verse_words; V5 — поле raw_text (VARCHAR) в verses.

---

## 4. API

Права доступа: **весь write-контур — только `ADMIN`** (как в content-service). Отдельная
роль «редактор/переводчик» отложена на будущую итерацию (см. §8). Чтение доступно всем
аутентифицированным пользователям.

```
GET    /api/v1/sangraha/works                                  → плитки произведений
       ?id={workId} (опционально)                              → если id указан — дерево произведения по UUID
POST   /api/v1/sangraha/works                                   → создать произведение (ADMIN), см. §5.2
GET    /api/v1/sangraha/works/{workSlug}                        → ★ произведение + дерево chapters/verses по slug
                                                                   (основной эндпоинт для фронтенда /sangraha/:workSlug,
                                                                   возвращает id, slug, titleRu/En и chapters[].verses[])
PUT    /api/v1/sangraha/works/{workId}                          → обновить метаданные (ADMIN)
DELETE /api/v1/sangraha/works/{workId}                          → soft delete (ADMIN)

POST   /api/v1/sangraha/works/{workSlug}/chapters                → добавить главу (ADMIN), см. §5.3
       Body: {"title": "...", "orderIndex": 123}  — title обязателен, orderIndex опционален
PUT    /api/v1/sangraha/chapters/{chapterId}                     → обновить главу (ADMIN), см. §5.3
       Body: {"title": "...", "orderIndex": 123}  — оба поля опциональны
DELETE /api/v1/sangraha/chapters/{chapterId}                     → soft delete (ADMIN)

POST   /api/v1/sangraha/chapters/{chapterId}/verses             → добавить стих (пустой, DRAFT) (ADMIN)
GET    /api/v1/sangraha/verses/{verseId}                         → стих: текст + (если ANALYZED) VerseAnalysis + VerseWord[]
POST   /api/v1/sangraha/verses/{verseId}/analyze                 → сохранить `raw_text` и запустить LLM-анализ (ADMIN, см. §5); тело —
                                                                     единое поле `text` (обязательно, см. §7) — backend определяет
                                                                     письменность по Unicode-диапазону (наличие символов деванагари →
                                                                     textDevanagari, иначе → textIast); синхронный ответ или 202 +
                                                                     опрос статуса — решает Агент 2
DELETE /api/v1/sangraha/verses/{verseId}                         → soft delete (ADMIN)
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
(кнопка «Сохранить» и отдельный эндпоинт `PUT /verses/{id}/text` удалены — write-контур
текста стиха теперь состоит из одного эндпоинта, см. §7).

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

Tool `submit_verse_analysis` с параметрами: textDevanagari, textIast, translationRu, translationEn, sandhiSplits (массив {surface, components[], ruleNumbers[]}), words (массив: position, surfaceIast, surfaceDevanagari, lemmaIast, stem, root, pos, gender, caseType, numberType, person, tense, mood, voice, glossRu, glossEn, formationRuleNumbers[])

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
3. Пишет `OutboxEvent(VERSE_VOCABULARY_EXTRACTED)` в той же транзакции (transactional outbox).

Если пользователь ввёл текст только в одном представлении (только devanagari или только
iast) — второе представление также генерирует модель, и backend сохраняет оба.

### 5.2 Создание произведения: авто-детекция языка, перевод, генерация метаданных

`POST /api/v1/sangraha/works` принимает только `title` (сырой ввод пользователя на любом
из трёх языков) и опционально `description`. Все остальные поля Work заполняются
автоматически, синхронно, в рамках одного HTTP-запроса.

**Шаг 1 — детекция языка (без LLM, по алфавиту первого значимого символа `title`):**
Devanagari-диапазон Unicode → `SANSKRIT`; кириллица → `RU`; латиница → `EN`.

**Шаг 2 — LLM tool calling.** Один вызов `/chat/completions` с промптом (файл
[`prompts/work-metadata.md`](./prompts/work-metadata.md)) и **один**
объявленный tool — `submit_work_metadata`, модель обязана вернуть результат через
`tool_calls`, а не свободным текстом.

Параметры tool `submit_work_metadata`: titleRu, titleEn, titleSaIast, titleSaDevanagari, descriptionRu
(nullable), descriptionEn (nullable), author (nullable — если LLM не уверена в
авторстве, возвращает `null`, поле остаётся пустым, не выдумывается).

Backend валидирует `tool_calls[0].function.arguments` по JSON Schema (не доверяем
модели, как и в §5.1). Поле языка, указанное пользователем (`detectedLanguage`),
никогда не перезаписывается ответом модели — LLM только дополняет два оставшихся
языковых представления и (опционально) описание/автора.

Если пользователь передал `description` — она считается описанием на языке
`detectedLanguage` и подставляется в соответствующее поле (`descriptionRu` или
`descriptionEn`); модель в этом случае дополняет только оставшееся из двух полей
описания переводом. Санскритское описание не хранится (только `descriptionRu`/`descriptionEn`).

**Шаг 3 — slug.** Вычисляется **детерминированно, без LLM** — транслитерация
`titleSaIast → SLP1` по фиксированной таблице соответствия IAST↔SLP1 (чистая функция
в Agent 2, не LLM-задача: идентификатор не должен зависеть от недетерминированного
вывода модели). Диакритика и пробелы/апострофы IAST превращаются в ASCII-набор SLP1;
результат приводится к `^[a-z0-9][a-z0-9-]*$` (дефисы вместо пробелов, нижний регистр).
При коллизии `slug` — backend добавляет числовой суффикс (`-2`, `-3`, ...).

**Ошибки:** если LLM недоступна/вернула невалидный `tool_calls` — `POST /works`
завершается ошибкой (5xx), Work не создаётся (никаких частично заполненных записей).

### 5.3 Создание/редактирование главы: авто-перевод названия

`POST /works/{workSlug}/chapters` принимает только `title` (сырой ввод пользователя на любом
из трёх языков) и опционально `orderIndex`. Все остальные поля Chapter (`titleRu`, `titleEn`,
`titleSaIast`, `titleSaDevanagari`, `slug`) заполняются автоматически, синхронно, в рамках
одного HTTP-запроса — по тому же паттерну, что и §5.2 для Work.

**Шаг 1 — детекция языка (без LLM, по алфавиту первого значимого символа `title`):**
Devanagari-диапазон Unicode → `SANSKRIT`; кириллица → `RU`; латиница → `EN`.

**Шаг 2 — LLM tool calling.** Один вызов `/chat/completions` с промптом (файл
[`prompts/chapter-metadata.md`](./prompts/chapter-metadata.md)) и **один**
объявленный tool — `submit_chapter_metadata`, модель обязана вернуть результат через
`tool_calls`, а не свободным текстом.

Параметры tool `submit_chapter_metadata`: titleRu, titleEn, titleSaIast, titleSaDevanagari.
У главы нет author/description — эти поля отсутствуют.

Backend валидирует `tool_calls[0].function.arguments` по JSON Schema (не доверяем
модели, как и в §5.1). Поле языка, указанное пользователем (`detectedLanguage`),
никогда не перезаписывается ответом модели — LLM только дополняет два оставшихся
языковых представления.

**Шаг 3 — slug.** Вычисляется **детерминированно, без LLM** — транслитерация
`titleSaIast → SLP1` по той же схеме, что и для Work (см. §5.2). При коллизии в
пределах workId (уникальность `(work_id, slug)`, а не глобально) — backend добавляет
числовой суффикс (`-2`, `-3`, ...).

**Шаг 4 — orderIndex.** Если `orderIndex` не передан в запросе — backend вычисляет его
как `max(orderIndex) + 1` среди активных (не удалённых) глав того же произведения.

**PUT /chapters/{chapterId}** принимает те же поля — `title` и опционально `orderIndex`.
Оба поля опциональны: если `title` передан — выполняется LLM-перевод и обновляются
все четыре title-поля; если `orderIndex` передан — обновляется только он.

**Ошибки:** если LLM недоступна/вернула невалидный `tool_calls` — запрос завершается
ошибкой (5xx), Chapter не создаётся/не обновляется (никаких частично заполненных записей).

---

## 6. Kafka: sangraha → content-service

```
topic: sangraha-vocabulary-events
key:   verseId
```

Публикуется **на каждый проанализированный стих** (не батчами по главе).

Событие содержит: eventType (VERSE_VOCABULARY_EXTRACTED), verseId, workSlug, workTitleRu, workTitleEn, chapterSlug, chapterTitleRu, chapterTitleEn, words[] (wordIast, wordDevanagari, stem, root, gender, translationRu, translationEn, explanationRu, explanationEn)

Consumer в `content-service` (новый `@KafkaListener`, первый консьюмер в этом сервисе):
1. `upsert VocabularyCategory(code = workSlug)` (root, если не существует — создать по workTitleRu/En).
2. `upsert VocabularyCategory(code = "{workSlug}.{chapterSlug}", parentId = root.id)`.

3. `upsert Quiz(type = VOCABULARY, slug = workSlug)` — **только на уровне произведения**. Quiz на уровне главы не создаётся: агрегация слов по поддереву категорий (`VocabularyService.getVocabularyWordsForQuiz`) уже поддерживает фильтрацию по `categoryCode = "{workSlug}.{chapterSlug}"` через дерево категорий. Отдельный Quiz на главу создал бы дублирование набора слов.
4. Для каждого слова: dedup по `(wordIast, stem)` — если `VocabularyWord` уже существует, не создавать заново, только добавить `VocabularyWordCategory(wordId, categoryId=chapter.id)`, если связи ещё нет.

Payload/типы события переиспользуются как shared DTO — `shared/samskrtam-dtos` содержит пакет `sangraha` с `SangrahaVocabularyEvent`. Решение Агента 6: заводим shared DTO, т.к. событие используется двумя сервисами (producer + consumer), локальный DTO создал бы дублирование и риск рассинхронизации.

---

## 7. Frontend (эскиз, детализирует Агент 3)

- **Страница произведений** (`/sangraha`) — плитки (`WorkCard`) со списком работ + кнопка «Добавить произведение» (ADMIN).
- **Страница произведения** (`/sangraha/{workSlug}`) — TreeGrid (PrimeReact TreeTable, по аналогии с остальным фронтом): колонка 1 — дерево «глава → стих (textIastPreview)», колонка 2 — иконка/ссылка на VOCABULARY-квиз `slug = categoryCode`. Кнопки «Добавить главу», «Добавить стих» (ADMIN).
- **Страница стиха** (`/sangraha/{workSlug}/verses/{verseId}`):
  - Поле ввода текста — **одно** (не два раздельных для devanagari/iast). Пользователь
    может печатать в нём как деванагари, так и IAST — оба варианта допустимы в одном
    и том же поле; в режиме редактирования поле показывает сохранённое `rawText`
    (а не `textDevanagari`/`textIast` — они актуальны только после анализа, см. ниже).
  - `status=DRAFT` (или после нажатия «Редактировать» из ANALYZED/FAILED — см. ниже) →
    поле ввода активно, кнопка **только одна** — «Анализ» → `POST /verses/{id}/analyze`
    с телом `{ text }`. Отдельной кнопки «Сохранить» на странице нет: backend сохраняет
    введённый текст в `rawText` и определяет письменность (`textDevanagari`/`textIast`,
    §4/§5.1) в рамках того же запроса, до начала LLM-анализа.
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
      штатно, без плейсхолдера-ошибки.
    - Если `status=ANALYZED`, но `analysis`/`words` не пришли (пустой ответ backend) —
      фронтенд должен показать явную ошибку/плейсхолдер, а не пустой блок молча.
  - Кнопка **«Редактировать»** видна только при `status=ANALYZED`: возвращает поле
    ввода в редактируемое состояние (значение — как для DRAFT), сохраняет исходный
    текст доступным для правки; повторное нажатие «Анализ» перезаписывает
    `VerseAnalysis` и `VerseWord[]` (см. §8, версионирование анализа не хранится).

---

## 8. Открытые вопросы / отложено

- **Таблица соответствия IAST↔SLP1 для slug** (см. §5.2): конкретный набор правил
  транслитерации выбирает Агент 2 при реализации на основе общепринятых схем IAST/SLP1.
- **Роль «редактор/переводчик»**: пока весь write — `ADMIN`. Отдельная роль (может вводить/анализировать стихи, но не управлять произведениями/главами) — следующая итерация; когда будет готова модель ролей, добавить `SANGRAHA_EDITOR` и обновить §4.
- **Связь слов стиха со словарём** (`dictionary-service`, поиск по `slp1`): сознательно не делаем в этой итерации — только грамматика от LLM. Если понадобится — отдельным Kafka-каналом (sangraha публикует, dictionary-service асинхронно обогащает через ответное событие), без синхронных вызовов между сервисами.
- **Quiz(VOCABULARY) — только на уровне произведения**: §6.3 решён — Quiz заводится только с `slug = workSlug`. Главы не получают отдельного Quiz, т.к. агрегация слов по поддереву категорий (`VocabularyService.getVocabularyWordsForQuiz`) уже поддерживает фильтрацию по `categoryCode = "{workSlug}.{chapterSlug}"` через дерево категорий. Отдельный Quiz на главу создал бы дублирование.
- **Библиотека текстов (каталог, не курикулум)** — требования к странице библиотеки, стабильному адресу строфы (`произведение.глава.строфа`, используется как `source_ref` в `usage_examples`), двухсекционному поиску по корпусу (произведения + строфы) и полю `license`/`source_type` в модели произведения — см. [frontend/information-architecture.md §3.2 и §7](../frontend/information-architecture/02-catalog.md).