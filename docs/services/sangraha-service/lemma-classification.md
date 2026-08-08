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
что уже в §3 старого pipeline, просто источник кандидатов теперь LLM, а не
пусто).

**Почему в sangraha-service, а не в curriculum-service:** классификация
работает на леммах, агрегированных по всему корпусу (частотность, вхождения),
а корпус (`VerseWord`, `Source`) физически живёт в sangraha-service — агрегация
и классификация ближе к сырью, не требует гонять весь корпус в
curriculum-service ради промежуточного шага. curriculum-service остаётся
потребителем уже готового результата (категория + перевод), как и раньше
получает occurrences (`lexicon-content-pipeline.md` §1) — просто теперь тем же
export-каналом приходит больше готовых полей, часть ручной работы ADMIN
curriculum-service переносится на ADMIN sangraha-service (по сути, тот же
человек/роль, просто интерфейс review — в sangraha-service, рядом с данными).

**Схемы классификации — расширяемая модель, не захардкоженная:**

| Схема | Статус | Что классифицирует |
|---|---|---|
| `CURRICULUM` | Реализуется в этом документе | Семантическая категория из таксономии `lexical-curriculum.md` §3 (42 листа) + перевод (glossRu/En) |
| `WORDNET` | Зарезервирована, **не реализуется в этой итерации** | Synset-based классификация (WordNet-подобная сеть значений) — расширенная альтернатива, для будущего |
| (другие) | Не определены | Модель `classification_scheme` — просто справочник, добавление новой схемы не требует миграции структуры `lemma_classification`, только новую строку схемы + свой промпт/tool |

---

## 1. Доменная модель

Новая схема/пакет в sangraha-service (не путать с `curriculum.lexeme` —
разные сервисы, разные таблицы; см. §5 про связь).

### 1.1 `Lemma` — агрегат по всему корпусу

Таблица `sangraha.lemma`:

id (UUID, PK), lemmaSlp1 (VARCHAR 100, NOT NULL), lemmaIast (VARCHAR 100, NOT
NULL), lemmaDevanagari (VARCHAR 100, NOT NULL), gender (VARCHAR 20, NULL —
`VerseWordMorphology.gender`, доминирующее значение среди occurrences, см.
§1.3), dominantPosCode (VARCHAR 30, NULL — доминирующий `VerseWord.pos` среди
occurrences), occurrenceCount (INTEGER, NOT NULL — денормализованный кэш,
пересчитывается при refresh, см. §1.3), frequencyRank (INTEGER, NULL —
позиция по убыванию `occurrenceCount`, пересчитывается вместе с ним),
createdAt / updatedAt.

`UNIQUE(lemmaSlp1, gender)` — тот же ключ уникальности, что уже согласован
для `curriculum.lexeme` (`lexicon.md` §1) — не совпадение: `Lemma` в
sangraha-service физически предшествует `curriculum.lexeme` в конвейере
(§5), ключ должен быть тем же, иначе сопоставление на стороне
curriculum-service ломается.

### 1.2 `LemmaOccurrenceRef` — не отдельная таблица

Не заводим новую таблицу вхождений — `Lemma.id` вычисляется группировкой
существующих `VerseWord` по `(lemmaSlp1, gender)` (тот же принцип, что уже
описан в `lexicon-content-pipeline.md` §2 шаг 2, только теперь выполняется
внутри sangraha-service, а не при экспорте). `VerseWord` получает опциональную
денормализованную ссылку `lemmaId` (FK → `lemma.id`, nullable, заполняется
процессом refresh, см. §1.3) — так occurrences остаются доступны без JOIN по
тексту леммы при последующих обращениях (batch-выборка примеров для
классификации, §3).

### 1.3 Refresh job — пересчёт `Lemma` из `VerseWord`

Отдельный процесс (ADMIN-триггер, `POST /sangraha/internal/lexicon/lemmas/refresh`):

1. Группировка всех `VerseWord` по стихам `status = ANALYZED` по `(lemmaSlp1, gender)`.
2. Upsert `Lemma`: новая группа → новая строка; существующая → обновить `occurrenceCount`, `dominantPosCode` (мода по группе), `frequencyRank` (пересчёт по всем строкам).
3. Проставить `VerseWord.lemmaId` для всех строк группы.
4. Идемпотентно, безопасно перезапускать по мере роста корпуса (тот же принцип, что `lexicon-content-pipeline.md` §2 шаг 6).

Именно `Lemma`/`frequencyRank` (не сырой `VerseWord`) — единица работы для
классификации (§2) и для отбора батчей (§3).

### 1.4 `ClassificationScheme` — справочник схем

Таблица `sangraha.classification_scheme`:

code (VARCHAR 20, PK — `CURRICULUM`|`WORDNET`|…), titleRu (VARCHAR 100, NOT
NULL), isActive (BOOLEAN, NOT NULL, DEFAULT `true` — `WORDNET` сидируется
строкой с `isActive = false`, чтобы UI мог показать «скоро», не пряча схему
полностью, но запуск batch-классификации по неактивной схеме — 400).

### 1.5 `CurriculumSemanticTopic` — копия таксономии (только для схемы CURRICULUM)

Таблица `sangraha.curriculum_semantic_topic` — **редактируемая копия**, не FK
на curriculum-service (разные сервисы/БД, синхронный кросс-сервисный FK
невозможен, тот же принцип независимости схем, что уже применяется в проекте):

code (VARCHAR 40, PK — например `animals`, `plants`, `ritual-worship` — те же
коды, что в `curriculum.semantic_topic.code`, `lexical-curriculum.md` §3),
parentCode (VARCHAR 40, NULL, FK → сама на себя — для 9 корней/33 листьев
структуры), labelRu (VARCHAR 100, NOT NULL), labelEn (VARCHAR 100, NOT NULL),
description (TEXT, NULL — короткое пояснение, **включается в промпт LLM**, см.
§2, чтобы модель не угадывала объём категории по одному названию).

Сидируется миграцией из полного списка `lexical-curriculum.md` §3 (9 корней +
33 листа = 42 строки). **Синхронизация с эталоном** (`curriculum.semantic_topic`
в curriculum-service) — ручная (правки таксономии редки, `lexical-curriculum.md`
§3 сама помечена как «итоговая» после ревизии) — при необходимости добавить
категорию в будущем правится в обоих местах одновременно, отдельного
механизма авто-синхронизации в этой итерации не строим (тот же уровень
допустимого дублирования, что уже есть между `sangraha` и `dictionary-service`
enum'ами в §9 текущего документа).

### 1.6 `LemmaClassification` — результат по одной лемме по одной схеме

Таблица `sangraha.lemma_classification`:

id (UUID, PK), lemmaId (UUID, FK → lemma.id, ON DELETE CASCADE), schemeCode
(VARCHAR 20, FK → classification_scheme.code), categoryCode (VARCHAR 40, NULL
— для `CURRICULUM` это FK-по-значению на `curriculum_semantic_topic.code`,
проверяется в сервисном слое против списка допустимых кодов, не БД-констрейнтом,
т.к. для будущих схем формат `categoryCode` будет другим, например synset-id
для `WORDNET` — единая VARCHAR-колонка держит любую схему без ALTER TABLE),
glossRu (VARCHAR 200, NULL — наиболее вероятный перевод, заполняется вместе с
категорией, см. §2), glossEn (VARCHAR 200, NULL), confidence (SMALLINT, NULL —
0–100, если модель вернула — не обязательное поле tool'а, см. §2), status
(VARCHAR 20, NOT NULL, DEFAULT `CANDIDATE` — `CANDIDATE`|`APPROVED`|`REJECTED`;
без `AI_ENRICHED`/промежуточных статусов — LLM-результат сразу `CANDIDATE`,
человеческое решение переводит в `APPROVED`/`REJECTED`, тот же принцип
двухстатусного review, что `lexicon-content-pipeline.md` §3), llmModel
(VARCHAR 100, NOT NULL — значение `SANGRAHA_LLM_MODEL` на момент вызова, для
воспроизводимости/аудита), batchId (UUID, FK → classification_batch.id),
reviewedBy (VARCHAR, NULL — `X-User-Id` ADMIN, при переходе из `CANDIDATE`),
reviewedAt (TIMESTAMPTZ, NULL), createdAt / updatedAt.

`UNIQUE(lemmaId, schemeCode)` — одна классификация на лемму на схему
(повторный batch-прогон по уже классифицированной лемме — апдейт существующей
строки, не дубль, см. §3 шаг 1 «только неклассифицированные»).

### 1.7 `ClassificationBatch` — метаданные одного LLM-вызова

Таблица `sangraha.classification_batch`:

id (UUID, PK), schemeCode (VARCHAR 20, FK), runId (UUID, FK →
classification_run.id), lemmaCount (SMALLINT, NOT NULL), status (VARCHAR 20,
NOT NULL — `PENDING`|`SUCCESS`|`FAILED`), errorMessage (TEXT, NULL),
llmModel (VARCHAR 100, NOT NULL), createdAt / completedAt.

`ClassificationRun` (таблица `sangraha.classification_run`): id (UUID, PK),
schemeCode, requestedBatchCount (SMALLINT, NOT NULL — ADMIN-лимит, см. §3),
completedBatchCount (SMALLINT, NOT NULL, DEFAULT 0), status (`RUNNING`|
`COMPLETED`|`COMPLETED_WITH_ERRORS`), requestedBy (X-User-Id), createdAt /
completedAt — один запуск (§3) может состоять из нескольких `ClassificationBatch`,
каждый батч — ровно один LLM-вызов; отдельный `Batch` нужен, чтобы неудача
одного batch (LLM недоступна/невалидный ответ) не откатывала весь run —
остальные батчи обрабатываются независимо, `FAILED`-батч просто оставляет свои
леммы неклассифицированными до следующего run (retry — просто следующий запуск,
т.к. отбор §3 шаг 1 берёт только неклассифицированные).

---

## 2. LLM-вызов — batch-классификация + перевод

Переиспользуются конвенции §5 текущего документа (`sangraha-service.md`):
OpenAI-совместимый `/chat/completions`, **tool calling**, env
`SANGRAHA_LLM_BASE_URL`/`SANGRAHA_LLM_API_KEY`/`SANGRAHA_LLM_MODEL` (тот же
клиент/конфигурация, что уже используется для анализа стиха — не заводим
второй набор env-переменных ради второго типа LLM-вызова).

### 2.1 Промпт

Новый файл [`prompts/lemma-classification.md`](./prompts/lemma-classification.md)
(структура файла — по образцу уже существующего `prompts/verse-analysis.md`).
Промпт на каждый вызов включает:
1. Полный список 42 категорий `CURRICULUM` (code + labelRu/En + description,
   §1.5) — закрытый список, модель обязана выбрать `categoryCode` **только**
   из него (не придумывать новые коды).
2. Инструкцию на перевод: «одно наиболее вероятное значение» — не список
   вариантов, не выбор по контексту конкретного стиха (лемма классифицируется
   вне контекста, см. §0) — то же упрощение, что уже было в старом pipeline
   для `glossRu`/`glossEn` (`lexicon-content-pipeline.md` §1, «представитель с
   наибольшим числом вхождений» больше не нужен как источник — теперь перевод
   даёт LLM, а не берётся от случайного occurrence).
3. Список лемм батча (см. §2.2 — что именно передаётся на лемму).

### 2.2 Вход батча — что передаётся на одну лемму

Не только голая лемма — модели нужен минимальный контекст, чтобы не гадать
вслепую (аналогично тому, как разбор стиха в §5.1 видит текст целиком, не
изолированное слово):

- `lemmaIast`, `lemmaDevanagari`;
- `dominantPosCode`, `gender` (если есть, из `Lemma`, §1.1) — сужает
  пространство значений (например, для явного `finite-verb` LLM не будет
  путать с омонимичным существительным);
- до 2 примеров реального употребления — `surfaceIast` + короткий фрагмент
  контекста (соседние слова стиха, если доступны дёшево; при отсутствии —
  просто `surfaceIast` без контекста, не блокирует классификацию).

### 2.3 Tool `submit_lemma_classification`

Параметры — массив (batch, не одна лемма на вызов, см. §3):

```json
{
  "classifications": [
    {
      "lemmaId": "uuid",
      "categoryCode": "animals",
      "glossRu": "слон",
      "glossEn": "elephant",
      "confidence": 85
    }
  ]
}
```

`confidence` — необязательное поле (LLM может не уметь его честно оценивать,
не блокируем схему из-за этого); если передано — сохраняется как есть, ничего
не считает сервер. `categoryCode` — модель обязана выбрать **ровно один** из
переданных в промпте 42 кодов; сервер **валидирует** ответ против списка
`curriculum_semantic_topic` (§1.5) до записи в БД — неизвестный код это ошибка
ответа модели, не тихо принимается: строка такой лексемы помечается
`status = CANDIDATE` с `categoryCode = null` (не отбрасывается целиком — перевод
`glossRu`/`glossEn`, если он валиден, всё равно сохраняется) и flag'ится для
ручного разбора (тот же принцип, что уже был для непромаппленных
POS/morphology-кодов в старом pipeline, `lexicon-content-pipeline.md` §3
«POS/gender не промаппились» — тут аналогичная категория ошибки, только для
классификации).

### 2.4 Валидация письменности/перевода

Тот же принцип, что §5.1: `glossRu`/`glossEn` не должны содержать деванагари
(признак ошибки модели, отклонить строку, оставить лемму неклассифицированной
для следующего run).

---

## 3. Отбор батчей — по частотности, с ADMIN-лимитом на прогон

`POST /sangraha/internal/lexicon/classification/runs` (ADMIN):

```json
{ "schemeCode": "CURRICULUM", "batchSize": 50, "batchCount": 10 }
```

1. **Кандидаты.** `Lemma`, у которых **нет** строки `LemmaClassification` с
   `schemeCode = CURRICULUM` и `status != REJECTED` (т.е. ранее отклонённые
   ADMIN леммы не подставляются автоматически повторно — если ADMIN отклонил
   результат, значит эвристика/LLM ошиблась специфическим образом, повторный
   автопрогон без изменений скорее всего даст тот же неверный результат;
   переклассификация отклонённых — отдельное ручное действие ADMIN, не часть
   обычного run, см. §4), отсортированные по `frequencyRank` (по возрастанию —
   сначала самые частотные, см. заголовок задачи).
2. **Лимит прогона.** Берутся первые `batchSize × batchCount` кандидатов
   (дефолты — `batchSize = 50`, `batchCount` **обязателен** в запросе, без
   дефолта — явный ручной ADMIN-лимит на объём одного прогона, как
   зафиксировано в решении; типичное значение подсказывается фронтендом,
   не хардкодится сервером).
3. **Батчинг.** Кандидаты разбиваются на группы по `batchSize` подряд по
   уже отсортированному списку (не случайно — детерминированность повторного
   просмотра прогона).
4. **Выполнение.** Батчи обрабатываются последовательно (не параллельно —
   sangraha-service и так однопоточно синхронен для LLM-вызовов анализа стиха,
   тот же паттерн; распараллеливание — возможная будущая оптимизация,
   не блокирует текущий дизайн) — на каждый батч: создать `ClassificationBatch`
   (`status = PENDING`) → вызов LLM (§2) → валидация ответа (§2.3–2.4) →
   upsert `LemmaClassification` (`status = CANDIDATE`) на каждую валидную
   строку → `ClassificationBatch.status = SUCCESS`/`FAILED`.
5. **Ошибка батча** (LLM недоступна, невалидный JSON, tool не вызван) — весь
   батч помечается `FAILED` с `errorMessage`, run продолжает со следующим
   батчем (не откатывается целиком, см. §1.7). Леммы неудачного батча остаются
   неклассифицированными и попадут в следующий run автоматически (шаг 1 их
   снова выберет — строки `LemmaClassification` для них не создано).
6. **Ответ:** `{ runId, requestedBatchCount, completedBatchCount, succeededBatchCount, failedBatchCount, classifiedLemmaCount }`.

`GET /sangraha/internal/lexicon/classification/runs/{runId}` — статус
прогона (для UI, если run асинхронный — решение о sync/async вызова, как и в
`lexicon-content-pipeline.md` §4 п.4, оставлено реализации; при синхронном —
этот эндпоинт просто отдаёт финальный результат сразу).

---

## 4. Admin review

`GET /sangraha/internal/lexicon/classifications?status=CANDIDATE&schemeCode=CURRICULUM`
— список на ревью (пагинация, сортировка по `frequencyRank` — частотные слова
приоритетнее для ручной проверки).

`PATCH /sangraha/internal/lexicon/classifications/{id}` (ADMIN):
```json
{ "status": "APPROVED", "categoryCode": "animals", "glossRu": "слон" }
```
Позволяет одновременно исправить поля и подтвердить (частый случай — LLM
почти угадала, ADMIN правит один код/перевод и сразу апрувит, не два отдельных
запроса). `status = REJECTED` без правки полей — просто отклонение (лемма не
экспортируется, см. §5, и не переклассифицируется автоматически, §3 шаг 1).

---

## 5. Экспорт в curriculum-service

Дополняет (не заменяет) `GET /sangraha/internal/content/verse-words/export`
(`sangraha-service.md` §9) новым эндпоинтом уровня леммы:

`GET /sangraha/internal/lexicon/lemma-classifications/export?schemeCode=CURRICULUM&status=APPROVED&cursor={lemmaId}&limit=500`

```json
{
  "items": [
    {
      "lemmaId": "uuid", "lemmaSlp1": "gaja", "lemmaIast": "gaja", "lemmaDevanagari": "गज",
      "gender": "MASCULINE", "dominantPosCode": "NOUN", "occurrenceCount": 42, "frequencyRank": 187,
      "categoryCode": "animals", "glossRu": "слон", "glossEn": "elephant"
    }
  ],
  "nextCursor": "uuid-or-null"
}
```

Только `status = APPROVED` — тот же жёсткий фильтр принципа, что уже применён
к `pool/resolve` в curriculum-service (`task-curriculum-15` DoD). Импорт
curriculum-service (`task-curriculum-14`) при наличии готовой `APPROVED`
CURRICULUM-классификации для леммы использует `categoryCode` → `semanticTopicId`
и `glossRu`/`glossEn` напрямую из этого экспорта вместо шага 3
`lexicon-content-pipeline.md` (эвристический перевод по представителю с
наибольшим числом вхождений) и вместо ручной разметки `semanticTopicId`
(`lexicon-content-pipeline.md` §3) — этот шаг пайплайна de facto заменяется
результатом текущего документа, см. правку `lexicon-content-pipeline.md` §7
(новую).

---

## 6. Открытые вопросы

- **Синхронизация таксономии** (§1.5) между sangraha-service (копия) и
  curriculum-service (эталон, `curriculum.semantic_topic`) — ручная в этой
  итерации; при рассинхронизации (`categoryCode` из экспорта не найден в
  `curriculum.semantic_topic`) — импорт curriculum-service должен явно упасть
  на этой строке, а не тихо пропустить (детали проверки — `task-curriculum-14`
  при реализации, не в этом документе).
- **`WORDNET`-схема** — таблицы `classification_scheme`/`lemma_classification`
  спроектированы так, чтобы принять её без ALTER (свободный `categoryCode`),
  но сам промпт/tool/таксономия synset'ов, а также то, откуда brать synset ID
  (WordNet для санскрита — не общеизвестный готовый ресурс) — не определены,
  реализация вне периметра этой итерации.
- **Повторная классификация после расширения корпуса** — новая партия лемм с
  ростом корпуса (`Lemma refresh`, §1.3) естественно попадает в следующий run
  (шаг 1 §3 всегда берёт неклассифицированные); но `frequencyRank` уже
  классифицированных лемм от роста корпуса тоже меняется — не триггерит
  переклассификацию (категория/перевод леммы не зависят от её ранга) — это
  осознанное упрощение, не ошибка.
- **Параллельные LLM-вызовы для ускорения batch-прогона** — текущий дизайн
  последовательный (§3 шаг 4); если объём (2000 лемм ÷ 50 = 40 батчей)
  окажется медленным при последовательных вызовах — распараллеливание
  batch'ей (не лемм внутри батча) — решение при реализации, не блокирует
  текущий дизайн.
