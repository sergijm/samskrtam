# Lemma Classification — классификация лексем через LLM (sangraha-service)

> Связанные файлы: [sangraha-service.md](../sangraha-service.md) §5 (существующая
> LLM-интеграция, конвенции переиспользуются), §10 (точка входа), §9 (export для
> curriculum-service — здесь дополняется), [lexical-curriculum.md](../lexical-curriculum.md)
> §3 (таксономия — источник копии, см. §1), [lexicon-content-pipeline.md](../lexicon-content-pipeline.md)
> (потребитель результата на стороне curriculum-service).

---

## 0. Зачем и где

Раньше (`lexicon-content-pipeline.md` §2–§3, первая версия) `semanticTopicId` и
проверка перевода были единственным полем без эвристики — целиком ручной труд
ADMIN на стороне curriculum-service. Этот документ закрывает именно этот
пробел: **классификация лексем по семантике + перевод — отдельный модуль
внутри sangraha-service**, использующий внешнюю LLM батчами, с ручным
ADMIN-review результата (не автоматическое доверие модели — тот же принцип,
что уже в §3 старого pipeline).

**Почему в sangraha-service, а не в curriculum-service:** классификация
работает на леммах, агрегированных по всему корпусу (частотность, вхождения),
а корпус (`VerseWord`, `Source`) физически живёт в sangraha-service — агрегация
и классификация ближе к сырью. curriculum-service остаётся потребителем уже
готового результата (категория + перевод), как и раньше получает occurrences
(`lexicon-content-pipeline.md` §1) — просто теперь тем же export-каналом приходит
больше готовых полей, и часть ручной работы ADMIN переносится в review рядом с
данными.

**Схемы классификации — расширяемая модель, не захардкоженная:**

| Схема | Статус | Что классифицирует |
|---|---|---|
| `CURRICULUM` | Реализуется в этом документе | Семантическая категория из таксономии `lexical-curriculum.md` §3 (42 листа) + перевод (glossRu/En) |
| `WORDNET` | Зарезервирована, **не реализуется в этой итерации** | Synset-based классификация — расширенная альтернатива, для будущего |
| (другие) | Не определены | `classification_scheme` — простой справочник; новая схема добавляется строкой + своим промпт/tool, без ALTER `lemma_classification` |

---

## 1. Доменная модель

Новая схема/пакет в sangraha-service (не путать с `curriculum.lexeme` —
разные сервисы/БД, см. §7 про связь). С 2026-08-09 лемма расщеплена на
**словарь** и **статистику**: словарь уникален по `lemmaSlp1`, а частотность и
распределение по родам живут в отдельной таблице (см. §1.2).

### 1.1 `Lemma` — словарь лексем

Таблица `sangraha.lemma` (как в прошлом разделе, но теперь чисто словарь):

id (UUID, PK), lemmaSlp1 (VARCHAR 100, NOT NULL), lemmaIast (VARCHAR 100, NOT
NULL), lemmaDevanagari (VARCHAR 100, NOT NULL), createdAt / updatedAt.

`UNIQUE(lemmaSlp1)` — одна строка на лексему. Никакой статистики: gender,
dominantPosCode, occurrenceCount, frequencyRank **удалены** и переехали в
`lemma_statistics`.

### 1.2 `LemmaStatistics` — статистика по (lemma, gender)

Числа по вхождениям леммы в корпус (lemma-classification.md §1.3) — новая таблица
`sangraha.lemma_statistics`:

id (UUID, PK), lemmaId (UUID, FK → lemma.id, ON DELETE CASCADE), gender (VARCHAR
20, NULL — `VerseWordMorphology.gender` вхождения; null, если у ряда нет gender),
occurrenceCount (INTEGER, NOT NULL — число вхождений группы), dominantPosCode
(VARCHAR 30, NULL — мода по `VerseWord.pos` в группе), updatedAt.

`UNIQUE(lemmaId, gender)` — одна строка на (лемма, род).

### 1.3 `LemmaOccurrenceRef` — ссылка из `VerseWord`, не отдельная таблица

Не заводим новую таблицу вхождений — `Lemma.id`/статистика вычисляются
группировкой существующих `VerseWord` по `(lemmaSlp1, gender)`.
`VerseWord` получает опциональную денормализованную ссылку `lemmaId` (FK →
`lemma.id`, nullable, заполняется процессом refresh, §1.4) — так occurrences
доступны без JOIN по тексту леммы (выборка примеров для классификации §2.2).
Текстовая копия `verse_words.lemma_iast` сохраняется (часть корпуса сидится
внешними скриптами напрямую в текст, скрипты будут переписаны отдельно).

### 1.4 Refresh job — пересчёт словаря и статистики

ADMIN-триггер, `POST /sangraha/internal/lexicon/lemmas/refresh-statistics`:

1. Группировка всех `VerseWord` всех не удалённых стихов (без фильтра статуса:
   часть корпуса загружена внешним скриптом в обход штатного флоу анализа) по
   `lemmaSlp1` → upsert `Lemma` (заполнить `lemmaIast`/`lemmaDevanagari` по
   первому вхождению группы).
2. Внутри каждой леммы — группировка по gender → upsert `LemmaStatistics`
   (`occurrenceCount`, `dominantPosCode` = мода по группе, §1.2). Устаревшие
   строки статистики (рода, которых больше нет в корпусе) удаляются.
3. Проставить `VerseWord.lemmaId` для всех строк группы.
4. Идемпотентно, безопасно перезапускать по мере роста корпуса.

Ответ: `{ lemmaCount, newLemmaCount, updatedLemmaCount, statisticsCount,
newStatisticsCount, updatedStatisticsCount }`.

Именно `LemmaStatistics`/частотность (не сырой `VerseWord`) — ресурс для
классификации (§2) и отбора батчей (§3).

### 1.5 `ClassificationScheme` — справочник схем

Таблица `sangraha.classification_scheme`:

code (VARCHAR 20, PK — `CURRICULUM`|`WORDNET`|…), titleRu (VARCHAR 100, NOT
NULL), isActive (BOOLEAN, NOT NULL, DEFAULT `true` — `WORDNET` сидируется
строкой с `isActive = false`; запуск batch-классификации по неактивной схеме —
400).

### 1.6 `CurriculumSemanticTopic` — копия таксономии (только CURRICULUM)

Таблица `sangraha.curriculum_semantic_topic` — **редактируемая копия**, не FK
на curriculum-service (разные БД, синхронный кросс-сервисный FK невозможен):

code (VARCHAR 40, PK — `animals`, `plants`, `ritual-worship`, те же коды, что в
`curriculum.semantic_topic.code`), parentCode (VARCHAR 40, NULL, FK на саму себя
— для 9 корней/33 листьев), labelRu / labelEn (VARCHAR 100, NOT NULL),
description (TEXT, NULL — включён в промпт LLM, §2).

Сидируется миграцией из полного списка `lexical-curriculum.md` §3 (42 строки).
Синхронизация с эталоном — ручная (правки таксономии редки, §3 помечена как
«итоговая»).

### 1.7 `LemmaClassification` — результат по паре (лемма, род) по одной схеме

Таблица `sangraha.lemma_classification`:

id (UUID, PK), lemmaId (UUID, FK → lemma.id, ON DELETE CASCADE), **gender (VARCHAR 20,
NULL — род из статистики пары, классифицируемой в батче, §3)**, schemeCode (VARCHAR
20, FK), categoryCode (VARCHAR 40, NULL — для `CURRICULUM` FK-по-значению на
`curriculum_semantic_topic.code`; проверяется в сервисном слое, не БД-констрейнтом),
glossRu (VARCHAR 200, NULL), glossEn (VARCHAR 200, NULL), confidence (SMALLINT,
NULL — 0–100, если модель вернула), status (VARCHAR 20, NOT NULL, DEFAULT
`CANDIDATE` — `CANDIDATE`|`APPROVED`|`REJECTED`), llmModel (VARCHAR 100, NOT
NULL), batchId (UUID, FK), reviewedBy (VARCHAR, NULL), reviewedAt (TIMESTAMPTZ,
NULL), createdAt / updatedAt.

`UNIQUE(lemmaId, gender, schemeCode)` — одна классификация на пару (лемма, род)
по схеме; повторный run апдейтит строку.

### 1.8 `ClassificationBatch` / `ClassificationRun`

`classification_batch` (одна строка = один LLM-вызов): id (UUID, PK), schemeCode,
runId (FK → classification_run.id ON DELETE CASCADE), lemmaCount (SMALLINT, NOT
NULL), status (`PENDING`|`SUCCESS`|`FAILED`), errorMessage (TEXT, NULL),
llmModel (VARCHAR 100, NOT NULL), createdAt / completedAt.

`classification_run`: id (UUID, PK), schemeCode, requestedBatchCount (SMALLINT,
NOT NULL — ADMIN-лимит, §3), completedBatchCount (SMALLINT, NOT NULL, DEFAULT 0),
status (`RUNNING`|`COMPLETED`|`COMPLETED_WITH_ERRORS`), requestedBy, createdAt /
completedAt. Один run — несколько батчей; неудача одного батча (LLM недоступна/
ответ невалиден) не откатывает весь run: остальные батчи обрабатываются
независимо, FAILED-батч оставляет свои пары неклассифицированными до следующего
run (retry = просто повторный запуск, §3 шаг 1 снова берёт неклассифицированные).

---

## 2. LLM-вызов — batch-классификация + перевод

Переиспользуются конвенции §5 текущего документа (`sangraha-service.md`):
OpenAI-совместимый `/chat/completions`, tool calling, env
`SANGRAHA_LLM_BASE_URL`/`SANGRAHA_LLM_API_KEY`/`SANGRAHA_LLM_MODEL`.

### 2.1 Промпт

Файл [`prompts/lemma-classification.md`](./prompts/lemma-classification.md), на
каждый вызов: 1) полный список 42 категорий (code + label + description, §1.6) —
закрытый список, модель обязана выбрать `categoryCode` только из него; 2) инструкцию
на перевод «одно наиболее вероятное значение» (лема классифицируется вне контекста);
3) список лемм батча (§2.2).

### 2.2 Вход батча — что передаётся на одну лемм

Для пары (лемма, gender) из статистики отбирается **доминирующая** строка
(максимум `occurrenceCount`, tie-break по алфавиту gender) — её род и
`dominantPosCode` попадают в промпт, а род этой пары фиксируется затем в
`lemma_classification.gender`:

- `lemmaIast`, `lemmaDevanagari`;
- `dominantPosCode`, `gender` (если есть) — сужает пространство значений;
- до 2 примеров реального употребления — `surfaceIast` + короткий фрагмент
  контекста (если доступны дёшево; иначе просто `surfaceIast`).

### 2.3 Tool `submit_lemma_classification`

Параметры — массив:

```json
{
  "classifications": [
    { "lemmaId": "uuid", "categoryCode": "animals", "glossRu": "слон",
      "glossEn": "elephant", "confidence": 85 }
  ]
}
```

`confidence` — необязательное поле (сохраняется как есть, сервер не считает).
`categoryCode` — модель обязана выбрать один из 42 кодов; сервер **валидирует**
ответ против §1.6 до записи в БД: неизвестный код → `status = CANDIDATE`, но
`categoryCode = null` (перевод, если валиден, сохраняется) и строка flagится для
ручного разбора.

### 2.4 Валидация письменности/перевода

`glossRu`/`glossEn` не должны содержать деванагари (признак ошибки модели,
отклонить строку, оставить лемм неклассифицированным).

---

## 3. Отбор батчей — по частотности, с ADMIN-лимитом на прогон

`POST /sangraha/internal/lexicon/classification/runs` (ADMIN):

1. **Кандидаты.** `Lemma`, у которых есть хотя бы одна пара (леммы, gender) в
   `lemma_statistics` БЕЗ строки `LemmaClassification` по схеме с
   `status != REJECTED` (ранее отклонённые не подставляются автоматически).
   Сортировка — по **сумме `occurrenceCount`** по всем gender леммы (по убыванию,
   самые частотные — первыми, решение 2026-08-09).
2. **Лимит прогона.** Первые `batchSize × batchCount` кандидатов (дефолт
   `batchSize = 50`, `batchCount` **обязателен** — явный ручной ADMIN-лимит).
3. **Батчинг.** Кандидаты разбиваются на группы по `batchSize` подряд по
   отсортированному списку (детерминированно).
4. **Выполнение.** Батча последовательно: `ClassificationBatch(PENDING)` → LLM
   (§2) → валидация (§2.3–2.4) → upsert `LemmaClassification` (`status =
   CANDIDATE`, gender пары из §2.2) → `SUCCESS`/`FAILED`.
5. **Ошибка** одного батная — `FAILED` с `errorMessage`, run продолжает со
   следующими (леммы неклассифицированы, попадут в следующий run).
6. **Ответ:** `{ runId, requestedBatchCount, completedBatchCount,
   succeededBatchCount, failedBatchCount, classifiedLemmaCount }`.

`GET /sangraha/internal/lexicon/classification/runs/{runId}` — статус прогона
(при синхронном вызове просто отдаёт итог).

---

## 4. Admin review

`GET /sangraha/internal/lexicon/classifications?status=CANDIDATE&schemeCode=CURRICULUM`
— список на ревью (пагинация, сортировка по убыванию `occurrenceCount`
статистики пары — частотные приоритетнее).

`PATCH /sangraha/internal/lexicon/classifications/{id}` (ADMIN):
```json
{ "status": "APPROVED", "categoryCode": "animals", "glossRu": "слон" }
```
Позволяет одновременно исправить поля и подтвердить. `status = REJECTED` без
правки полей — просто отклонение (не экспортируется, не переклассифицируется).

---

## 5. Экспорт в curriculum-service

Отдельный эндпоинт для экспорта APPROVED-классификаций:

`GET /sangraha/internal/lexicon/lemma-classifications/export?schemeCode=CURRICULUM&status=APPROVED&cursor={lemmaId}&limit=500`

В основном потоке импорта curriculum-service не используется — все данные
(лемма + статистика + классификация) уже включены в `lemmas/export`
(`sangraha-service.md` §10). Этот эндпоинт оставлен для отладки и ручной проверки.

Одна строка: классификация + статистика `(lemma, gender)` из `lemma_statistics`.

```json
{
  "items": [
    { "lemmaId": "uuid", "lemmaSlp1": "gaja", "lemmaIast": "gaja",
      "lemmaDevanagari": "गज", "gender": "MASCULINE",
      "dominantPosCode": "NOUN", "occurrenceCount": 42,
      "categoryCode": "animals", "glossRu": "слон", "glossEn": "elephant" }
  ],
  "nextCursor": "uuid-or-null"
}
```

Только `status = APPROVED`. Импорт curriculum-service (`task-curriculum-14`) при
наличии готовой APPROVED-классификации использует `categoryCode` →
`semanticTopicId` и gloss'ы напрямую вместо эвристики по представителю с
наибольшим числом вхождений — de facto заменяет шаг ручной разметки
`lexicon-content-pipeline.md` §3 (правка §7).

---

## 6. Открытые вопросы

- **Синхронизация таксономии** (§1.6) между sangraha (копия) и curriculum
  (эталон) — ручная; при рассинхронизации импорт curriculum должен явно упасть
  на строке, а не тихо пропустить.
- **`WORDNET`-схема** — модели/промпт/tool синеё, а также источник synset ID
  (WordNet для санскрита — не готовый ресурс) — не определены, вне этой
  итерации.
- **Повторная классификация при росте корпуса** — новые лемы естественно
  попадают в следующий run (§3 шаг 1), но переклассификации уже
  классифицированных из-за смены частотности не происходит (категория/перевод
  не зависят от ранга) — осознанное упрощение.
- **Параллельные LLM-вызовы** для ускорения прогона — текущий дизайн
  последовательный (§3 шаг 4); распараллеливание батчей вне блокера.