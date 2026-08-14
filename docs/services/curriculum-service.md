# curriculum-service

> Домен: Учебный план — атомарные темы (`Topic`, ~70 шт.), мягкие зависимости
> между ними (`TopicPrerequisite`), рекомендуемый уровень освоения
> (`LearningLevel` L0–L6) и комплексные подборки тем (`ComplexQuiz`)
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/curriculum-service`
> Порт: `8091`
> Схема БД: `curriculum` (собственная, независимая от `content`/`quiz`)
> Пакет: `sm.selflearn.samskrtam.curriculum`
> Status: **DRAFT**

> Источник требований: [curriculum.md](./curriculum.md) §1–§2 + пересмотр модели
> (70 атомарных Topic, L0–L6, ComplexQuiz — см. историю обсуждения ниже, §8).
> Связанные файлы: [quest-catalog.md](./quest-catalog.md), [learning-materials.md](./learning-materials.md),
> [curriculum-session-composition.md](./curriculum-session-composition.md), [conventions.md](../conventions.md).

---

## 1. Описание

Независимый сервис учебного плана. Хранит **три независимые классификации**
одного и того же набора атомарных тем — без наполнения (текста теории,
вопросов, квизов):

1. **Topic graph** — зависимости знаний (`TopicPrerequisite`, мягкие, не блокирующие);
2. **Learning Level** (`L0`…`L6`) — рекомендуемый уровень первого знакомства с темой, хранимое поле, не вычисляемое;
3. **ComplexQuiz** — произвольная комбинация 2–7 тем для интегрированной практики (`Mixed Practice` / `Level Assessment`), не привязанная жёстко к одному уровню темы.

**Явно вне периметра этой версии:**
- наполнение тем (`LearningMaterial` — текст теории, живёт в curriculum-service, см. §5);
- состав `ComplexQuiz` (сервис хранит только *состав* — какие Topic входят и сколько вопросов ожидается) — сами задания внутри подборки по-прежнему не генерируются здесь;
- проверка prerequisite перед стартом занятия — её нет и не будет на уровне API (curriculum.md §1, «Принципиально»);
- пересчёт «next recommended topic» по прогрессу пользователя — open question, отдельная задача Dashboard.

**Начиная с API v2 — исключение из правила «без квестов»:** генерация и хранение готовых
`QuestItem` для семейства `DECLENSION_FORM` (4 подтипа — выбор, ввод, определение падежа,
сопоставление) переехали именно сюда, под `/api/v2/curriculum/quest-items`, см.
[curriculum-quest-items.md](./curriculum-quest-items.md). Решение версионное: `curriculum-service`
(API v1) не меняется и не удаляется, новый функционал живёт только в curriculum-service —
см. `curriculum-quest-items.md` §0.

---

## 2. Почему три независимые классификации, а не одна иерархия

Раньше предполагалось, что «слой» (layer) — это и есть группировка тем для UI.
Пересмотр модели развёл это на три сущности, потому что они отвечают на разные
вопросы и не обязаны совпадать:

- **Topic graph** отвечает на вопрос «что нужно знать раньше» — чистая
  DAG-структура, применяется только для UI-подсказок (см. curriculum-service.md
  первой версии, §3);
- **Learning Level** отвечает на вопрос «когда тема **впервые** вводится» —
  это авторская классификация (проставляется вручную при заполнении учебного
  плана, не выводится из графа prerequisite), ровно 7 значений `L0`…`L6`, у
  каждой Topic — ровно одно значение;
- **ComplexQuiz** отвечает на вопрос «тема **повторно всплывает** в
  интегрированной практике более высокого уровня» — одна и та же Topic может
  входить в `ComplexQuiz` уровня `L1`, `L2` и `L4` одновременно (пример:
  «Personal pronouns» введена на `L1`, но участвует в комплексной практике
  `L2` и `L3`). Это и есть «appears_in» — **не хранимое поле**, а производный
  список, вычисляемый join'ом по `complex_quiz_topic` (см. §4).

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

## 4. Сущности: ComplexQuiz / ComplexQuizTopic

**ComplexQuiz** (таблица `curriculum.complex_quiz`):
id (UUID, PK)
type (VARCHAR 20, NOT NULL — `MIXED_PRACTICE` | `LEVEL_ASSESSMENT`)
learningLevel (VARCHAR 2, NOT NULL — `L0`…`L6`, уровень, к которому привязана подборка на UI, независимо от `learningLevel` входящих в неё Topic)
titleRu / titleEn (VARCHAR 200, NOT NULL)
questionCountHint (SMALLINT, NULL — ориентировочное число заданий для отображения на карточке, например «15 questions»; это **не** реальное число сгенерированных вопросов — квесты этот сервис не хранит и не генерирует, поле чисто информационное, проставляется вручную ADMIN)
createdAt / updatedAt (TIMESTAMPTZ, NOT NULL)

**ComplexQuizTopic** (таблица `curriculum.complex_quiz_topic`, join-таблица):
complexQuizId (UUID, FK → complex_quiz.id, ON DELETE CASCADE)
topicId (UUID, FK → topic.id, ON DELETE CASCADE)
PRIMARY KEY (complexQuizId, topicId)

**Ограничение по количеству тем (валидируется в сервисном слое, не в БД):**
- `MIXED_PRACTICE` — от 2 до 4 `Topic`;
- `LEVEL_ASSESSMENT` — от 5 до 7 `Topic`.

Попытка создать/обновить `ComplexQuiz` с числом тем вне диапазона своего типа —
`422 Unprocessable Entity`.

`Topic.appearsInLevels` (производное, вычисляемое поле в `TopicDto`, не
колонка БД) — отсортированный список различных `ComplexQuiz.learningLevel`,
в подборки которых входит эта тема (плюс её собственный `learningLevel`,
всегда первым). Вычисляется одним JOIN-запросом при чтении конкретной темы
(`GET /topics/{id}`), не при листинге (дорого при большом каталоге ComplexQuiz
— для списка тем `appearsInLevels` не возвращается).

---

## 5. LearningMaterial — не в этом сервисе

Как и в первой версии: `LearningMaterial` — 1:N от материала к теме
(`LearningMaterial.topicId → Topic.id` по значению, без физического FK между
БД разных сервисов), физически живёт в curriculum-service, см.
[learning-materials.md](./learning-materials.md) §1. curriculum-service не
хранит и не валидирует эту связь.

---

## 6. Бизнес-логика графа prerequisite (без изменений)

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

## 7. API (OpenAPI v2)

Полная спецификация: [openapi/curriculum/curriculum-service.yaml](../openapi/curriculum/curriculum-service.yaml).

Темы:
GET /api/v2/curriculum/topics — плоский список (без appearsInLevels)
GET /api/v2/curriculum/topics/{id} — тема + прямые prerequisite + appearsInLevels
POST /api/v2/curriculum/topics — создать (ADMIN)
PUT /api/v2/curriculum/topics/{id} — обновить (ADMIN)
DELETE /api/v2/curriculum/topics/{id} — удалить, каскад по prerequisite и complex_quiz_topic (ADMIN)
GET/POST/DELETE /api/v2/curriculum/topics/{id}/prerequisites... — как в первой версии

Уровни:
GET /api/v2/curriculum/levels — L0…L6 со счётчиком тем каждого уровня
GET /api/v2/curriculum/levels/{level}/topics — темы конкретного уровня (для рендера «L2 — 10 Topics»)

Граф (диагностика, не основная навигация — см. §6):
GET /api/v2/curriculum/graph

Комплексные подборки:
GET /api/v2/curriculum/complex-quizzes?level=&type= — список с фильтрами
GET /api/v2/curriculum/complex-quizzes/{id} — подборка + резолвленные Topic
POST /api/v2/curriculum/complex-quizzes — создать (ADMIN, 422 при неверном числе тем для типа)
PUT /api/v2/curriculum/complex-quizzes/{id} — обновить состав/метаданные (ADMIN, та же валидация)
DELETE /api/v2/curriculum/complex-quizzes/{id} — удалить (ADMIN)

Доступ: чтение — любой аутентифицированный пользователь; запись — `ADMIN`
(проверка на Gateway по JWT, как у curriculum-service).

Quest Items (v2, DECLENSION_FORM family) — отдельный раздел API, см.
[curriculum-quest-items.md §6](./curriculum-quest-items.md#6-api-v2-новые-эндпоинты-curriculum-service).

---

## 8. Открытые вопросы / follow-up

- Gateway-маршрут `/api/v2/curriculum/**` и NetworkPolicy — задача Агента 1, не выполнялась.
- Наполнение конкретных 70 Topic реальными `code`/`titleRu`/`titleEn`/`learningLevel` из предложенной раскладки (L0–L6) — это данные, а не схема; загружаются либо seed-миграцией, либо через `POST /topics` вручную ADMIN — решение о способе загрузки не принято, вне периметра этой итерации.
- Реальная генерация вопросов внутри `ComplexQuiz` (quiz-service должен уметь взять `ComplexQuiz.topics`, найти относящиеся к ним Quest в curriculum-service через будущее поле `topicId` там, и собрать сессию) — отдельная будущая интеграционная задача, вне curriculum-service.
- `questionCountHint` — чисто декоративное поле; если позже появится реальный подсчёт вопросов по темам в curriculum-service, имеет смысл сделать его вычисляемым на уровне API-агрегации, а не хранимым — сейчас осознанно упрощено до ручного числа.

---

## 9. Модуль lexicon — учебная лексика (NEW)

Начиная с этой итерации curriculum-service дополнительно хранит учебную лексику
(до 2000 базовых лемм + таксономии frequency/semantic/POS/morphology +
пользовательский прогресс) в той же схеме `curriculum`, тем же сервисом —
решение принято как компромисс против выделения отдельного `lexicon-service`
(обоснование — `lexicon.md` §0 п.3). Домен независим от `Topic`/`ComplexQuiz`
по данным, но переиспользует `Topic.domain=LEXICON` для `LexicalTopic` и
`ComplexQuiz` для интегрированной лексической практики.

Полная спецификация — [lexicon.md](./lexicon.md) (доменная модель),
[lexical-curriculum.md](./lexical-curriculum.md) (таксономии, 68 Lexical Topics),
[lexical-quizzes.md](./lexical-quizzes.md) (типы квизов, adaptive selection),
[lexicon-content-pipeline.md](./lexicon-content-pipeline.md) (импорт лемм из
корпуса sangraha-service, без AI-enrichment — только эвристики и ручной
ADMIN-review; точечная догрузка экзотических лемм из внешнего словаря — будущая
задача).

**Явно не заменяет** существующий per-verse поток (`content.vocabulary_words`,
отдельный сервис, см. `content-service.md` §11) — это два параллельных
механизма над одним и тем же сырьём sangraha-service; их слияние — отдельная
будущая задача (`lexicon.md` §0 п.2, `lexicon-content-pipeline.md` §5).
