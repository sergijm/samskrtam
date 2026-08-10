# Lexicon Content Pipeline — наполнение лемм из корпуса sangraha

> Связанные файлы: [lexicon.md](./lexicon.md), [lexical-curriculum.md](./lexical-curriculum.md),
> [sangraha-service.md](./sangraha-service.md) (источник сырья),
> [lemma-classification.md](./sangraha-service/lemma-classification.md) (LLM-классификация).

> **Источник:** только корпус sangraha-service (проанализированные стихи, `status=ANALYZED`).
> Без внешних частотных списков, без AI в самом pipeline.
> Классификация — отдельный модуль LLM в sangraha-service с ручным ADMIN-ревью.

---

## 1. Архитектура: агрегация на стороне sangraha

Вся агрегация данных происходит в sangraha-service. curriculum-service получает готовые
строки и делает простой upsert — не группирует, не ранжирует, не маппит коды.

### Источники данных (один JOIN в LemmaExportService)

| Поле | Источник в sangraha |
|---|---|
| `lemmaSlp1` / `lemmaIast` / `lemmaDevanagari` | `sangraha.lemma` |
| `gender` | `sangraha.lemma_statistics` |
| `occurrenceCount` | `sangraha.lemma_statistics` (COUNT verse_words по lemma+gender) |
| `dominantPosCode` | `sangraha.lemma_statistics` (наиболее частотный POS в группе) |
| `categoryCodes` | `sangraha.lemma_classification` (APPROVED, scheme=CURRICULUM) — список всех категорий для пары (lemma, gender) |
| `glossRu` / `glossEn` | `sangraha.lemma_classification` (APPROVED) |
| `vowelType` | `sangraha.nominal_lemmas.stem_class` (если есть, иначе null) |

### Экспортный эндпоинт

`GET /sangraha/internal/lexicon/lemmas/export` — курсорная пагинация по `lemma_statistics.id`,
сортировка по `occurrenceCount DESC`. Одна строка = одна пара `(lemma, gender)`.
Все JOIN выполняются на стороне sangraha — curriculum-service не делает дополнительных
запросов.

### Импорт в curriculum-service

`POST /api/v2/lexicon/import/from-sangraha` (ADMIN):

1. `SangrahaExportClient.fetchLemmaExport()` — постраничная загрузка из `lemmas/export`.
2. Для каждой строки — upsert `Lexeme` по ключу `(lemmaSlp1, gender)`.
3. Присваивается `frequencyRank` = позиция в отсортированном списке (1..N).
4. Привязываются `partOfSpeech`, `morphologyClass` (из `vowelType`), `semanticTopic` (все из `categoryCodes`).
5. Глоссы берутся из классификации; если классификации нет — поле остаётся пустым
   (ADMIN заполнит позже).
6. Статус новых лексем: `CANDIDATE`. Переход в `APPROVED` — ручной ADMIN-шаг.

Повторный запуск идемпотентен — дубли не создаются.

---

## 2. Порядок наполнения

1. **Подготовка в sangraha-service:**
   - `POST /sangraha/internal/lexicon/lemmas/refresh-statistics` — собрать уникальные леммы
     из `verse_words` и пересчитать `lemma_statistics`.
   - `POST /sangraha/internal/lexicon/classification/runs` — запустить LLM-классификацию
     лемм по таксономии `CURRICULUM`.
   - ADMIN-ревью классификаций: `PATCH /sangraha/internal/lexicon/classifications/{id}`
     со статусом `APPROVED`.

2. **Импорт в curriculum-service:**
   - `POST /api/v2/lexicon/import/from-sangraha` — batch-импорт всех лемм,
     для которых есть `lemma_statistics` (независимо от наличия классификации).
   - Леммы без APPROVED-классификации получают пустые `semanticTopic` и `gloss`
     — ADMIN заполняет вручную через `LexemeTaxonomyController`.

3. **Повторные импорты** — по мере анализа новых произведений в sangraha-service.
   `frequencyRank` пересчитывается заново по актуальному `occurrenceCount`.

4. **Стандартные определения квизов:**
   Миграция `V12__seed_vocabulary_quiz_definitions.sql` создаёт строки
   `VocabularyQuizDefinition` для частотных полос: Core Vocabulary 1–5
   с `kind=FREQUENCY_BAND`, `frequencyRankMax = 100/250/500/1000/2000`.

---

## 3. Validation — ручной ADMIN-гейт

| Проверка | Правило | Действие при нарушении |
|---|---|---|
| Отсутствующий `semanticTopic` | У всех свежеимпортированных `CANDIDATE`-лексем | Не блокирует `APPROVED`; фильтр `GET /lexemes?status=CANDIDATE&semanticTopicId=null` для приоритизации ручной разметки |
| Отсутствующий `gloss` | `glossRu` И `glossEn` пусты | Блокирует переход `CANDIDATE → APPROVED` (нельзя построить вопрос без перевода) |
| Gender для NOMINAL POS | `posCode` из группы `NOMINAL` — `gender` обязателен | Блокирует `APPROVED` |
| Порог по Topic | Тема с < 10 лексем после разметки | Отчёт "Topics below threshold" в admin UI |

**Обязательный человеческий шаг:** переход `CANDIDATE → APPROVED` (`PATCH /lexemes/{id}/status`).
Только `APPROVED`-лексемы попадают в `pool/resolve` лексических квизов.

---

## 4. Отличие от on-demand потока sangraha «кнопка Изучить»

Batch-pipeline **не заменяет** существующий поток «кнопка «Изучить» →
`POST /content/internal/sangraha/vocabulary-quiz` → `content.vocabulary_words».
Это два независимых механизма:

- **on-demand** — по клику, один стих, попадает в `content.vocabulary_words`, создаёт
  per-verse `VOCABULARY`-квиз.
- **batch-pipeline** — весь корпус разом, попадает в `curriculum.lexeme`,
  питает lexical-квизы нового поколения.

Слияние в одну таблицу —задача за периметром текущей итерации.

---

## 5. Открытые вопросы

- **Достаточность объёма корпуса для 2000 уникальных лемм** — зависит от количества
  проанализированных произведений в sangraha-service.
- **Омонимы с одинаковым gender** — по-прежнему не разделяются на разные `Lexeme`.
- **Формат запуска импорта** — ручная кнопка ADMIN (`POST /api/v2/lexicon/import/from-sangraha`),
  не cron.