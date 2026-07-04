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

**Chapter** (таблица chapters): id (UUID), workId (UUID), slug (string, unique в пределах work), orderIndex (int), titleRu, titleEn, deletedAt

**Verse** (таблица verses): id (UUID), chapterId (UUID), orderIndex (int), textDevanagari (TEXT), textIast (TEXT), status (DRAFT|ANALYZING|ANALYZED|FAILED), createdAt, updatedAt, deletedAt

**VerseAnalysis** (1:1 с Verse, таблица verse_analyses): verseId (UUID, PK), translationRu (TEXT), translationEn (TEXT), sandhiSplits (JSONB), rawModelResponse (JSONB, опционально), modelName, analyzedAt

**VerseWord** (таблица verse_words): id (UUID), verseId (UUID), position (int), surfaceIast, surfaceDevanagari, lemmaIast, stem, root (опционально), pos, gender, caseType, numberType, person, tense, mood, voice, glossRu, glossEn

---

## 3. Flyway Migrations (эскиз)

7 миграций Flyway: `V1` — создание схемы sangraha; `V2` — таблица works; `V3` — chapters; `V4` — verses; `V5` — verse_analyses; `V6` — verse_words; `V7` — outbox_events (patent как в user-service/quiz-service, event_type: VERSE_VOCABULARY_EXTRACTED).

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

POST   /api/v1/sangraha/works/{workId}/chapters                 → добавить главу (ADMIN)
PUT    /api/v1/sangraha/chapters/{chapterId}                     → обновить главу (ADMIN)
DELETE /api/v1/sangraha/chapters/{chapterId}                     → soft delete (ADMIN)

POST   /api/v1/sangraha/chapters/{chapterId}/verses             → добавить стих (пустой, DRAFT) (ADMIN)
GET    /api/v1/sangraha/verses/{verseId}                         → стих: текст + (если ANALYZED) VerseAnalysis + VerseWord[]
PUT    /api/v1/sangraha/verses/{verseId}/text                    → сохранить текст (ADMIN), единое поле `text` — backend
                                                                     определяет письменность по Unicode-диапазону (наличие
                                                                     символов деванагари → textDevanagari, иначе → textIast)
POST   /api/v1/sangraha/verses/{verseId}/analyze                 → запустить LLM-анализ (ADMIN, см. §5); синхронный
                                                                     ответ или 202 + опрос статуса — решает Агент 2
DELETE /api/v1/sangraha/verses/{verseId}                         → soft delete (ADMIN)
```

Ответ `GET /works/{workSlug}` (и `GET /works?id={workId}`) — двухуровневое дерево для TreeGrid:
Ответ — двухуровневое дерево: { id, slug, titleRu, chapters[] { id, slug, titleRu, orderIndex, categoryCode, verses[] { id, orderIndex, textIastPreview, status } } }

---

## 5. LLM-интеграция

### 5.1 Анализ стиха (tool calling)

Конфигурация — только через env, без дефолтов в yml (см. конвенцию по секретам):

```
SANGRAHA_LLM_BASE_URL     # OpenAI-совместимый endpoint
SANGRAHA_LLM_API_KEY
SANGRAHA_LLM_MODEL        # например gpt-4.1 / другая OpenAI-совместимая модель
```

Backend вызывает `/chat/completions` (или `/responses`) с промптом (файл
[`../prompts/verse-analysis.md`](../prompts/verse-analysis.md)) и
**одним** объявленным tool — модель обязана вернуть результат через `tool_calls`,
а не свободным текстом.

Tool `submit_verse_analysis` с параметрами: textDevanagari, textIast, translationRu, translationEn, sandhiSplits (массив {surface, components[]}), words (массив: position, surfaceIast, surfaceDevanagari, lemmaIast, stem, root, pos, gender, caseType, numberType, person, tense, mood, voice, glossRu, glossEn)

Backend:
1. Валидирует `tool_calls[0].function.arguments` по этой схеме (например через JSON Schema validator, не доверяем модели).
2. В одной транзакции: обновляет `Verse.textDevanagari/textIast` (если не были заданы вручную), пишет `VerseAnalysis` (перезаписывая предыдущую — см. §8), пересоздаёт `VerseWord[]` (deleteAll + saveAll) для стиха.
2e. Статус `Verse.status → ANALYZED` — **последним**.

**Гарантии атомарности (контракт):**
- Любое исключение на шагах 2a–2d откатывает транзакцию целиком.
- Статус не становится ANALYZED, а возвращается в DRAFT (можно повторить).
- `OutboxEvent` не публикуется частично: если payload не сериализовался — транзакция откатывается полностью.
- Исключение пробрасывается наружу → HTTP 500.

**Вне транзакции:**
- Перед LLM → `ANALYZING` (блокировка повторов).
- Ошибка LLM/невалид → `FAILED`.
- Техническая ошибка → `DRAFT`.
- Успех → `ANALYZED`.
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

[`docs/prompts/work-analysis.md`](../prompts/work-analysis.md)) и **один**
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
    и том же поле, backend сам определяет письменность (см. `PUT /verses/{id}/text` в
    §4) и заполняет нужное из полей `textDevanagari`/`textIast`; отсутствующее
    представление достраивает LLM при анализе (§5.1).
  - `status=DRAFT` (или после нажатия «Редактировать» из ANALYZED — см. ниже) → поле
    ввода активно + кнопка «Анализ» → `POST /verses/{id}/analyze`.
  - `status=ANALYZING` → поле и кнопки заблокированы, индикатор загрузки.
  - `status=ANALYZED` → поле ввода **read-only** (показывает сохранённый
    `textDevanagari`/`textIast` — оба, если оба заполнены), и ниже обязательно
    отображаются результаты `GET /verses/{verseId}` (объект `analysis` +
    `words[]` из `VerseDetail`, см. `sangraha-schemas.yaml`):
    - **Перевод** — `translationRu` и `translationEn` (обе колонки/вкладки).
    - **Сандхи** — `sandhiSplits`: список `surface → components[]`.
    - **Грамматический разбор** — таблица `words[]` по `position`: поверхностная
      форма, лемма/основа, часть речи и морфологические признаки (падеж/число/род
      либо лицо/время/наклонение/залог — в зависимости от pos), `glossRu`/`glossEn`.
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