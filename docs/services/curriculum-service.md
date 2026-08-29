# curriculum-service

> Домен: Учебный план — атомарные темы (`Topic`, ~70 шт.), мягкие зависимости
> между ними (`TopicPrerequisite`), рекомендуемый уровень освоения
> (`LearningLevel` L0–L6) и учебные темы
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/curriculum-service`
> Порт: `8091`
> Схема БД: `curriculum` (собственная, независимая от `content`/`quiz`)
> Пакет: `sm.selflearn.samskrtam.curriculum`
> Status: **DRAFT**

> Источник требований: [curriculum.md](curriculum-service/curriculum.md) §1–§2 + пересмотр модели
> (70 атомарных Topic, L0–L6 — см. историю обсуждения ниже, §8).
> Связанные файлы: [quest-catalog.md](curriculum-service/quest-catalog.md), [learning-materials.md](learning-materials.md),
> [curriculum-session-composition.md](curriculum-service/curriculum-session-composition.md), [conventions.md](../conventions.md).

---

## 1. Описание

Независимый сервис учебного плана. Хранит **две независимые классификации**
одного и того же набора атомарных тем — без наполнения (текста теории,
вопросов, квизов):

1. **Topic graph** — зависимости знаний (`TopicPrerequisite`, мягкие, не блокирующие);
2. **Learning Level** (`L0`…`L6`) — рекомендуемый уровень первого знакомства с темой, хранимое поле, не вычисляемое;

**Явно вне периметра этой версии:**
- наполнение тем (`LearningMaterial` — текст теории, живёт в **content-service**, см. §5);
- проверка prerequisite перед стартом занятия — её нет и не будет на уровне API (curriculum.md §1, «Принципиально»);
- пересчёт «next recommended topic» по прогрессу пользователя — open question, отдельная задача Dashboard.

**Начиная с API v2 — исключение из правила «без квестов»:** генерация и хранение готовых
`QuestItem` для семейства `DECLENSION_FORM` переехали под `/api/v2/curriculum/quest-items`
(см. [curriculum-quest-items.md](curriculum-service/curriculum-quest-items.md)). Архитектурное
решение вынесено в единый раздел «Архитектурные решения (ADR)» (ADR-3).

---

## 2. Почему две независимые классификации, а не одна иерархия

_Архитектурное решение вынесено в единый раздел «Архитектурные решения (ADR)» файла curriculum-service.md (ADR-1)._

---

## 3. Сущности: Topic / TopicPrerequisite

**Topic** (таблица `curriculum.topic`):
id (UUID, PK)
code (VARCHAR 80, UNIQUE, NOT NULL — стабильный slug, например `a-stem-masculine`)
titleRu (VARCHAR 200, NOT NULL)
titleEn (VARCHAR 200, NOT NULL)
learningLevel (VARCHAR 2, NOT NULL — `L0`…`L6`, ровно одно значение на тему, авторская классификация первого введения темы, см. §2)
isEvergreen (BOOLEAN, NOT NULL, DEFAULT false — тема вне уровней и вне графа: `Mixed review`, `Error correction`)
displayOrder (SMALLINT, NULL — ручной tie-break порядка отображения внутри одного `learningLevel`)
createdAt / updatedAt (TIMESTAMPTZ, NOT NULL)

Ожидаемый объём (не enforced в схеме, справочно из curriculum.md): ~70 строк,
неравномерно по уровням — от 8 (`L0`) до 12 (`L3`).

**TopicPrerequisite** (таблица `curriculum.topic_prerequisite`) — без изменений
относительно первой версии: `topicId`, `prerequisiteTopicId`, `strength`
(`RECOMMENDED`|`HELPFUL`), защита от циклов и self-loop — см. §6.

**Важно:** `questType` по-прежнему не хранится на Topic (см. обоснование в
истории обсуждения, §8) — тема остаётся чисто структурной единицей учебного
плана.

---

## 4. LearningMaterial — принадлежит content-service

Как и в первой версии: `LearningMaterial` — 1:N от материала к теме
(`LearningMaterial.topicId → Topic.id` по значению, без физического FK между
БД разных сервисов), **физически принадлежит content-service** (см.
[learning-materials.md](learning-materials.md) §1 и `content-service.md`). curriculum-service
ссылается на материал только по `id` и не хранит, не валидирует и не зависит от этой
сущности — в коде и схеме curriculum-service нет ни одного поля `LearningMaterial`.

---

## 5. Бизнес-логика графа prerequisite (без изменений)

Проверка циклов при добавлении `TopicPrerequisite` и вычисление топологических
слоёв для `/graph` — как в первой версии (DFS-проверка на запись, Кан-алгоритм
на чтение). **Важное уточнение после пересмотра модели:** вычисленный
топологический «layer» — это не то же самое, что `learningLevel`. `/graph`
остаётся как вспомогательный/диагностический инструмент (проверить, что
граф prerequisite не противоречит авторской раскладке по уровням — например,
подсветить на фронте случай, когда у Topic уровня `L2` есть prerequisite с
`learningLevel = L4`, что является ошибкой авторского наполнения, а не
техническим сбоем). Основная навигационная иерархия для UI — `learningLevel`
(эндпоинт `/levels`), не `/graph`.

---

## 6. API (OpenAPI v2)

Полная спецификация: [openapi/api/v2/curriculum/curriculum-service.yaml](../openapi/api/v2/curriculum/curriculum-service.yaml).

Темы:
GET /api/v2/curriculum/topics — плоский список (без appearsInLevels)
GET /api/v2/curriculum/topics/{id} — тема + прямые prerequisite + appearsInLevels
POST /api/v2/curriculum/topics — создать (ADMIN)
PUT /api/v2/curriculum/topics/{id} — обновить (ADMIN)
DELETE /api/v2/curriculum/topics/{id} — удалить, каскад по prerequisite (ADMIN)
GET/POST/DELETE /api/v2/curriculum/topics/{id}/prerequisites... — как в первой версии

Уровни:
GET /api/v2/curriculum/levels — L0…L6 со счётчиком тем каждого уровня
GET /api/v2/curriculum/levels/{level}/topics — темы конкретного уровня (для рендера «L2 — 10 Topics»)

Граф (диагностика, не основная навигация — см. §6):
GET /api/v2/curriculum/graph

### Прочие группы эндпоинтов v2 (кратко)

Полные контракты — в OpenAPI и в профильных документах. Никаких эндпоинтов вида
`/sessions/**` или `POST …/sessions/compose` в curriculum-service **нет** (состав и
жизненный цикл сессии — задача quiz-service, см.
[curriculum-session-composition.md](curriculum-service/curriculum-session-composition.md)):

- **Quest Items** (`/api/v2/curriculum/quest-items`, DECLENSION_FORM family) —
  `GET ?topicId&itemType&limit` (любой аутентифицированный, случайная выборка готовых
  `QuestItem`), `POST /select` (тело `QuestItemSelectionRequest` + `topicCode` query —
  выборка по прогресс-тегам/типу/режиму), `POST /regenerate` (ADMIN, 202 —
  перегенерация офлайн-батчем). Подробно:
  [curriculum-quest-items.md](curriculum-service/curriculum-quest-items.md).
- **Sandhi rules** — `GET /api/v2/curriculum/sandhi-rules`,
  `GET /api/v2/curriculum/sandhi-rules/{topicCode}`.
- **Learn graph** — `GET /api/v2/curriculum/learn-graph` (опциональный заголовок
  `X-User-Id`).
- **Lexicon dashboard** — `GET /api/v2/curriculum/lexicon`.
- **Lingua / case endings** — `GET /api/v2/curriculum/lingua/case-endings`.
- **Lexicon references (CRUD, ADMIN)** — `/lexicon`:
  `GET /semantic-classes/tree`, `POST /semantic-classes`, `PUT /semantic-classes/{id}`,
  `DELETE /semantic-classes/{id}`, `GET /pos`, `PUT /pos`, `DELETE /pos/{code}`,
  `GET /morphology-classes`, `PUT /morphology-classes`, `DELETE /morphology-classes/{code}`,
  `GET /frequency-bands`, `PUT /frequency-bands`, `DELETE /frequency-bands/{code}`.
- **Lexicon import** — `POST /api/v2/lexicon/import/verse-batch` (приём пачки лемм от
  sangraha-service; внутренний ADMIN-триггер импорта). Подробно:
  [lexicon-content-pipeline.md](curriculum-service/lexicon-content-pipeline.md).
- **Vocabulary quiz definitions** — `/api/v2/lexicon/vocabulary-quiz-definitions`:
  `GET ?kind`, `GET /{id}`, `POST` (ADMIN), `PUT /{id}` (ADMIN), `DELETE /{id}` (ADMIN).
- **Paradigms** — `GET /api/v2/curriculum/topics/{topicCode}/declension-paradigms` (`?index`),
  `GET /api/v2/curriculum/topics/{topicCode}/conjugation-paradigms` (`?index`,`?voice`).

---

## 7. Открытые вопросы / follow-up

- Gateway-маршрут `/api/v2/curriculum/**` и NetworkPolicy — задача Агента 1, не выполнялась.
- Наполнение конкретных 70 Topic реальными `code`/`titleRu`/`titleEn`/`learningLevel` из предложенной раскладки (L0–L6) — это данные, а не схема; загружаются либо seed-миграцией, либо через `POST /topics` вручную ADMIN — решение о способе загрузки не принято, вне периметра этой итерации.
- `questionCountHint` — чисто декоративное поле; если позже появится реальный подсчёт вопросов по темам в curriculum-service, имеет смысл сделать его вычисляемым на уровне API-агрегации, а не хранимым — сейчас осознанно упрощено до ручного числа.

---

## 8. Модуль lexicon — учебная лексика (NEW)

_Архитектурное решение вынесено в единый раздел «Архитектурные решения (ADR)» файла curriculum-service.md (ADR-2)._

Полная спецификация — [lexicon.md](lexicon.md) (доменная модель),
[lexical-curriculum.md](curriculum-service/lexical-curriculum.md) (таксономии, 68 Lexical Topics),
[lexical-quizzes.md](lexical-quizzes.md) (типы квизов, adaptive selection),
[lexicon-content-pipeline.md](curriculum-service/lexicon-content-pipeline.md) (импорт лемм из
корпуса sangraha-service, без AI-enrichment — только эвристики и ручной
ADMIN-review; точечная догрузка экзотических лемм из внешнего словаря — будущая
задача).

**Явно не заменяет** существующий per-verse поток (`content.vocabulary_words`,
отдельный сервис, см. `content-service.md` §11) — это два параллельных
механизма над одним и тем же сырьём sangraha-service; их слияние — отдельная
будущая задача (`lexicon.md` §0 п.2, `lexicon-content-pipeline.md` §5).

---

## Архитектурные решения (ADR)

Консолидированный раздел ключевых архитектурных решений curriculum-service. Ранее
эти блоки были разбросаны по отдельным документам; здесь собраны все в одном месте.

**ADR-1 — Две независимые классификации вместо одной иерархии.** Topic graph (DAG
prerequisite) и Learning Level (L0…L6, авторская классификация первого введения)
отвечают на разные вопросы и не обязаны совпадать. Обоснование — §2.

**ADR-2 — Lexicon живёт в curriculum-service как компромисс.** Учебная лексика (~2000
базовых лемм + таксономии frequency/semantic/POS/morphology + прогресс) хранится в той
же схеме `curriculum` тем же сервисом — решение принято против выделения отдельного
`lexicon-service` (обоснование — `lexicon.md` §0 п.3). Домен независим от
`Topic` по данным, но переиспользует `Topic.domain=LEXICON`.
Явно не заменяет per-verse поток `content.vocabulary_words`.

**ADR-3 — Исключение из правила «curriculum-service без квестов» (версионное).**
`curriculum-service` (API v1) не меняется и не удаляется (deprecated-not-removed). Новый
функционал 4 типов квестов склонения реализуется только в curriculum-service под
`/api/v2/curriculum/quest-items`, без изменений старого кода (§1 этого файла,
curriculum-quest-items.md §0).

**ADR-4 — Материализация Quest Items офлайн-batch вместо генерации «на лету».** Никакой
генерации по запросу через `QuestItemGenerator.generate(ctx)`; вместо этого batch-генератор
один раз проходит по `Lexeme` нужного `morphologyClass`, строит словоформы и материализует
весь набор `QuestItem` (с дистракторами) в `curriculum.quest_item`. Источник лемм —
`curriculum.lexeme` + `curriculum.morphology_class` (curriculum-quest-items.md §0).

**ADR-5 — Разделение ответственности сессии квиза.** curriculum-service = «что спросить»
(готовые материализованные вопросы, рендер, дистракторы через `/quest-items`); quiz-service =
«как проходит пользователь» (отбор с учётом прогресса, жизненный цикл сессии,
`quiz_item_score`, outbox). Никаких `/sessions/**` эндпоинтов в curriculum-service нет —
compose/lifecycle реализованы в quiz-service (`POST /api/v2/quiz/compose`).

**ADR-6 — ItemType НЕ расширяется для прогресса.** Quest-единицы пишутся как
`(ItemType.DECLENSION_FORM | VOCABULARY_WORD, quest_item.id)` через `QuestProgressTypes`;
пространства ref-id (`case_ending_id`/`vocabulary_word_id` vs `quest_item.id`) не
пересекаются. Чистый ключ с кодами `QuestItemType` — отложенный рефакторинг
`itemType`→String (curriculum-session-composition.md §4/§6).

**ADR-7 — LexicalTopic не заводит свою таблицу.** Регистрируется как обычная строка
`curriculum.topic` с дискриминатором `domain=LEXICON`, переиспользует graph/`learningLevel`.
Композиция Lexeme↔LexicalTopic — отдельная таблица `curriculum.lexeme_lexical_topic`

**ADR-8 — Импорт лексики: объём корпуса и разметка.** Достаточность объёма корпуса
sangraha-service для ~2000 лемм откладывается (решается по факту). Ручная разметка
`semanticClasses` — bottleneck пайплайна. Триггер импорта — ручной ADMIN:
`POST /api/v2/lexicon/import/verse-batch` (lexicon-content-pipeline.md §4).
