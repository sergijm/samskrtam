# Curriculum Quest Items (v2) — DECLENSION_FORM family

> Домен: MORPHOLOGY · Сервис: curriculum-service · API-версия: **v2** (`/api/v2/curriculum/**`)
> Связанные файлы: [quest-item-model.md](../quest-item-model.md) · [quest-catalog.md](quest-catalog.md) ·
> [quest-engine.md](../quest-engine.md) · [curriculum-service.md](../curriculum-service.md) ·
> [quests/morphology/declension/](../quests/morphology/declension/)

---

## 0. Решение и границы

`curriculum-service` (API v1) **не изменяется** — старая реализация `DECLENSION_FORM` там
остаётся как есть (либо считается deprecated и постепенно выводится из использования,
решение о выводе — отдельная будущая задача, не эта). Новый функционал 4 типов квестов
склонения реализуется **только** в curriculum-service, под новым путём `/api/v2/curriculum/quest-items`,
без единой строки изменений в curriculum-service.

**Ключевое архитектурное отличие от старой модели** (`quest-item-model.md` §2): никакой
генерации «на лету» через `QuestItemGenerator.generate(ctx)` по запросу. Вместо этого —
**batch-генератор**, офлайн-процесс (Spring `@Scheduled`/CLI-команда ADMIN, см. §4), который
один раз проходит по всем `Lexeme` нужного `morphologyClass`, строит словоформы по таблице
окончаний (переиспользуя парадигмы, которыми ранее оперировал curriculum-service, см.
`architecture.md §3.3`) и **материализует** весь набор `QuestItem` (включая дистракторы) в
таблицу `curriculum.quest_item`. Эндпоинт чтения — простой отбор уже готовых строк, без
вычислений в рантайме.

Источник лемм — уже существующие таблицы curriculum-service: `curriculum.lexeme` (лемма,
`gender`) и `curriculum.morphology_class` (`a-stem-masc`, `i-stem` и т.д., см. миграцию
lexicon-schema). Наполнение `Lexeme` реальными данными для склонения — отдельная будущая
задача (сейчас в БД есть таксономия, но не гарантирован объём лемм на класс основы).

---

## 1. Таблица `curriculum.quest_item` (единая на все будущие типы квестов)

Одна generic-таблица обслуживает не только 4 типа склонения, но и любые будущие типы
(грамматика и лексика, см. `quest-types-overview.md`) — типоспецифичные данные живут в
`payload`/`distractors` (`jsonb`), не в отдельных колонках/таблицах на тип.

| Колонка | Тип | Смысл |
|---|---|---|
| id | UUID PK | — |
| topic_id | UUID, FK → curriculum.topic | тема, к которой относится вопрос (`domain=GRAMMAR`) |
| item_type | VARCHAR(40) | код `QuestItemType` (`DECLENSION_FORM`, `DECLENSION_FORM_CHOICE`, `CASE_RECOGNITION`, `DECLENSION_MATCH`, …) |
| answer_mode | VARCHAR(20) | `FREE_TEXT` \| `SINGLE_CHOICE` \| `MATCHING` \| … (см. `quest-item-model.md`) |
| prompt | TEXT | что показываем пользователю |
| correct_answer | TEXT, NULL | эталонный ответ; NULL для `MATCHING` (ответ целиком в payload) |
| distractors | JSONB, NOT NULL DEFAULT '[]' | неверные варианты для `SINGLE_CHOICE`; пусто для `FREE_TEXT`/`MATCHING` |
| payload | JSONB, NOT NULL | типоспецифичные данные, см. §3 |
| generator_source | VARCHAR(60) | код batch-генератора, которым создана строка (для трассировки/повторной генерации) |
| created_at | TIMESTAMPTZ | — |

Индексы: `(topic_id, item_type)` — основной паттерн выборки при старте сессии; `(item_type)` —
для админ-обслуживания/пересчёта одного типа сразу по всем темам.

**Идемпотентность генерации:** повторный запуск batch-генератора для той же `Lexeme` +
`item_type` + `topic_id` не должен плодить дубликаты — уникальный индекс
`(topic_id, item_type, lexeme_id, case_type, number_type)` через выражение по `payload`
(`jsonb` extract) либо отдельная служебная таблица `curriculum.quest_item_generation_key`
(решает Агент 2 при реализации, зафиксировать в PR).

---

## 2. Четыре типа заданий

Все четыре читают один и тот же исходный материал (лемма + класс основы + парадигма
окончаний), различие — только в форме вопроса/ответа. Количество вопросов каждого типа,
запрашиваемых при старте сессии, задаётся конфигурацией quiz-service (см. §5) — не
хранится в curriculum-service как настройка по теме.

### 2.1 DECLENSION_FORM_CHOICE — тип 1 (простой, выбор)

Дана лемма (+ запрашиваемые падеж/число) → выбрать верную словоформу из вариантов.
`answerMode = SINGLE_CHOICE`. `distractors` — 2–3 похожие, но неверные словоформы (другой
падеж той же основы либо типичная ошибка окончания).

### 2.2 DECLENSION_FORM — тип 2 (простой, ввод)

Дана лемма (+ запрашиваемые падеж/число) → ввести словоформу свободным текстом.
`answerMode = FREE_TEXT`. `distractors` пусто. Это прямой аналог уже существующего типа —
переносится 1:1, меняется только место хранения/генерации (curriculum-service вместо
curriculum-service).

Payload обоих типов (1 и 2) — общий `DeclensionFormPayload`:

lemmaIast, lemmaDevanagari, morphologyClassCode, gender, caseType, numberType, correctFormIast, correctFormDevanagari

### 2.3 CASE_RECOGNITION — тип 3 (комплексный, определить падеж)

Дана готовая словоформа → определить падеж, число и (только если форма грамматически
неоднозначна без рода — например, часть окончаний i-/u-основ и местоимений совпадает у
разных родов) род. Ответ — **один составной вариант** из списка (`SINGLE_CHOICE`), например
`"Instrumental Plural"` или `"Instrumental Plural Neuter"`, не три отдельных поля.

Payload — `CaseRecognitionPayload`:

wordFormIast, wordFormDevanagari, lemmaIast, morphologyClassCode, correctCaseType, correctNumberType, correctGender (nullable), genderRequired (boolean — включать ли род в текст правильного варианта и дистракторов), distractorCombinations (список альтернативных «падеж+число[+род]», грамматически валидных для других основ — типичная путаница)

`genderRequired` вычисляется генератором по конкретной форме (проверка: совпадает ли эта
словоформа буквально с формой того же падежа/числа у основы другого рода в пределах одной
темы/класса основ — если да, род обязателен в ответе, иначе не нужен, чтобы не усложнять
вопрос без необходимости).

### 2.4 DECLENSION_MATCH — тип 4 (комплексный, сопоставление)

Слева — список словоформ одной лексемы (разных падежей/чисел), справа — список подписей
падеж+число в перемешанном порядке → соединить пары. `answerMode = MATCHING`.

Payload — `DeclensionMatchPayload`:

lemmaIast, morphologyClassCode, pairs: список { pairId, wordFormIast, wordFormDevanagari, caseType, numberType }

Число пар в одном задании — конфигурируется в `application.yaml` curriculum-service (не
хранится в БД по теме), см. §4.

---

## 3. Типы payload — сводка

Все четыре payload — `record`, реализуют `QuestItemPayload` (`quest-item-model.md` §1),
живут в `shared/samskrtam-dtos`, пакет `sm.selflearn.samskrtam.quest.declension`:

- `DeclensionFormPayload` — для DECLENSION_FORM_CHOICE и DECLENSION_FORM (§2.1–2.2)
- `CaseRecognitionPayload` — для CASE_RECOGNITION (§2.3)
- `DeclensionMatchPayload` — для DECLENSION_MATCH (§2.4)

Каждый payload несёт общий список `highlights` — `List<HighlightToken(text, textRu)>`:
слова промпта, которые фронтенд выделяет жирным (для вопросов со санскритским словом —
IAST-часть леммы/словоформы). quiz-service прокидывает `highlights` из payload в
`QuestionDto` без типизированного разбора (`ComposedQuestionMapper.parseHighlights`);
фронт сплитит текст промпта по токенам и оборачивает совпадения в `strong`.

`AnswerChecker` — три реализации (по одной на уникальный контракт проверки: точное
совпадение строки для FREE_TEXT/SINGLE_CHOICE, полное совпадение всех пар для MATCHING),
каждая — Spring-бин, ключ реестра — `QuestItemType.code()` (см. `quest-item-model.md` §2).

---

## 4. Batch-генератор

Класс `DeclensionQuestItemBatchGenerator` (пакет `sm.selflearn.samskrtam.curriculum.questgen`),
CLI-triggered (административный REST-эндпоинт `POST /api/v2/curriculum/quest-items/regenerate?topicId=&itemType=`,
ADMIN-only) — не выполняется автоматически по расписанию в первой версии (нет расписания —
запуск только вручную ADMIN после наполнения `Lexeme`).

Конфигурация количества пар для DECLENSION_MATCH — `application.yaml`:

```
curriculum:
  quest-items:
    declension-match:
      pairs-per-item: 5
```

Алгоритм на один `Topic` (тема связана с одним или несколькими `morphologyClassCode` через общий
маппинг `DeclensionClassMapper.topicToClassCodes` — одна тема может покрывать несколько классов
основы, например `i-u-stems` → `i-stem` + `u-stem`; при 1:1 код темы совпадает с классом, например
`a-stem-masc`):
1. Выбрать все `Lexeme`, у которых есть связь с этим `morphologyClassCode` (`curriculum.lexeme_morphology`).
2. Для каждой леммы и каждой пары (падеж, число) из парадигмы класса основы — построить
   словоформу (переиспользуя правила окончаний, см. `architecture.md §3.3`).
3. Сгенерировать `DECLENSION_FORM_CHOICE` и `DECLENSION_FORM` — по одной строке
   `quest_item` на (лемма, падеж, число), дистракторы для choice — словоформы того же
   класса основы, но другого падежа/числа (2–3 шт, случайная выборка).
4. Сгенерировать `CASE_RECOGNITION` — по одной строке на (лемма, падеж, число), с проверкой
   омонимии для `genderRequired` в рамках всех Lexeme этой же темы.
5. Сгенерировать `DECLENSION_MATCH` — группировать словоформы леммы блоками по
   `pairs-per-item` (§4), одна строка `quest_item` на блок.

---

## 5. Интеграция с quiz-service

Замена части `ContentClient` (v1, curriculum-service) на новый `CurriculumClient` (v2,
curriculum-service) **только** для типов из этого документа — остальные типы (vocabulary
и др.) продолжают идти через curriculum-service v1, см. `quest-engine.md` §5 (обновлено).

Количество вопросов каждого типа при старте сессии — конфигурация quiz-service
(`application.yaml`, не параметр запроса и не настройка в curriculum-service):

```
quiz:
  declension-session:
    single-choice-count: 5
    free-text-count: 5
    case-recognition-count: 3
    match-count: 2
```

`CurriculumClient.fetchQuestItems(topicId, itemType, count)` → `GET /api/v2/curriculum/quest-items?topicId=&itemType=&limit=` —
возвращает случайную выборку из уже сгенерированных строк (`ORDER BY random() LIMIT :count`
либо офсет по хэшу для стабильности пагинации — решает Агент 2). Прогресс (`quest.progress`
в quiz-service) по-прежнему ссылается на `item_id` без физического FK — как и раньше, item_id
теперь просто указывает на `curriculum.quest_item.id` вместо `content.quest_item.id`.

---

## 6. API v2 (новые эндпоинты curriculum-service)

```
GET  /api/v2/curriculum/quest-items?topicId=&itemType=&limit=   — выборка готовых QuestItem (любой аутентифицированный)
POST /api/v2/curriculum/quest-items/regenerate?topicId=&itemType=  — ADMIN, перегенерировать (удалить старые строки по ключу, создать заново)
```

Полная OpenAPI-схема — задача Агента 5 (Contract & Documentation Agent), не входит в этот
документ; здесь зафиксирован только контракт для написания задач Агенту 2.

---

## 7. Открытые вопросы

- Уникальный ключ идемпотентности генерации (§1) — jsonb-индекс vs служебная таблица, решает Агент 2.
- Наполнение `curriculum.lexeme`/`lexeme_morphology` реальными данными для склоняемых лемм — вне периметра этой задачи, отдельный content pipeline.
- Судьба старой реализации `DECLENSION_FORM` в curriculum-service v1 (deprecate/удалить) — не решается сейчас.
