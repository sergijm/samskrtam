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

**Work** (таблица works): id (UUID), slug (string, unique), titleRu, titleEn, descriptionRu, descriptionEn, author, createdAt, deletedAt

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
POST   /api/v1/sangraha/works                                   → создать произведение (ADMIN)
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
PUT    /api/v1/sangraha/verses/{verseId}/text                    → сохранить введённый текст (devanagari и/или iast) (ADMIN)
POST   /api/v1/sangraha/verses/{verseId}/analyze                 → запустить LLM-анализ (ADMIN, см. §5); синхронный
                                                                     ответ или 202 + опрос статуса — решает Агент 2
DELETE /api/v1/sangraha/verses/{verseId}                         → soft delete (ADMIN)
```

Ответ `GET /works/{workSlug}` (и `GET /works?id={workId}`) — двухуровневое дерево для TreeGrid:
Ответ — двухуровневое дерево: { id, slug, titleRu, chapters[] { id, slug, titleRu, orderIndex, categoryCode, verses[] { id, orderIndex, textIastPreview, status } } }

---

## 5. LLM-анализ стиха (tool calling)

Конфигурация — только через env, без дефолтов в yml (см. конвенцию по секретам):

```
SANGRAHA_LLM_BASE_URL     # OpenAI-совместимый endpoint
SANGRAHA_LLM_API_KEY
SANGRAHA_LLM_MODEL        # например gpt-4.1 / другая OpenAI-совместимая модель
```

Backend вызывает `/chat/completions` (или `/responses`) с промптом (транслитерировать,
перевести на ru/en, разобрать сандхи, дать пословную грамматику) и **одним** объявленным
tool — модель обязана вернуть результат через `tool_calls`, а не свободным текстом:

Tool `submit_verse_analysis` с параметрами: textDevanagari, textIast, translationRu, translationEn, sandhiSplits (массив {surface, components[]}), words (массив: position, surfaceIast, surfaceDevanagari, lemmaIast, stem, root, pos, gender, caseType, numberType, person, tense, mood, voice, glossRu, glossEn)

Backend:
1. Валидирует `tool_calls[0].function.arguments` по этой схеме (например через JSON Schema validator, не доверяем модели).
2. В одной транзакции: обновляет `Verse.textDevanagari/textIast` (если не были заданы вручную), пишет `VerseAnalysis` (перезаписывая предыдущую — см. §8), пересоздаёт `VerseWord[]` для стиха, переводит `Verse.status → ANALYZED`.
3. Пишет `OutboxEvent(VERSE_VOCABULARY_EXTRACTED)` в той же транзакции (transactional outbox).

Если пользователь ввёл текст только в одном представлении (только devanagari или только
iast) — второе представление также генерирует модель, и backend сохраняет оба.

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
  - `status=DRAFT` → textarea для ввода devanagari/iast + кнопка «Анализ» → `POST /verses/{id}/analyze`.
  - `status=ANALYZED` → read-only: devanagari, iast, перевод ru/en, сандхи, таблица слов с грамматикой; кнопка «Редактировать» возвращает к textarea и повторяет анализ (перезапись, см. §8).

---

## 8. Открытые вопросы / отложено

- **Роль «редактор/переводчик»**: пока весь write — `ADMIN`. Отдельная роль (может вводить/анализировать стихи, но не управлять произведениями/главами) — следующая итерация; когда будет готова модель ролей, добавить `SANGRAHA_EDITOR` и обновить §4.
- **Связь слов стиха со словарём** (`dictionary-service`, поиск по `slp1`): сознательно не делаем в этой итерации — только грамматика от LLM. Если понадобится — отдельным Kafka-каналом (sangraha публикует, dictionary-service асинхронно обогащает через ответное событие), без синхронных вызовов между сервисами.
- **Quiz(VOCABULARY) — только на уровне произведения**: §6.3 решён — Quiz заводится только с `slug = workSlug`. Главы не получают отдельного Quiz, т.к. агрегация слов по поддереву категорий (`VocabularyService.getVocabularyWordsForQuiz`) уже поддерживает фильтрацию по `categoryCode = "{workSlug}.{chapterSlug}"` через дерево категорий. Отдельный Quiz на главу создал бы дублирование.
