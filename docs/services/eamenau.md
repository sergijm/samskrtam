# Eamenau — Спецификация функциональности

> Статус: реализовано в коде, не описано в документации  
> Домен: `eamenau` (отдельная PostgreSQL-схема)  
> Расположение кода: `services/content-service/src/main/java/sm/selflearn/samskrtam/eamenau/`  
> Фронтенд: `frontend/src/pages/eamenau/`, `frontend/src/components/eamenau/`

---

## Что это

Eamenau — учебный модуль на основе упражнений из книги **J.F. Staal «A Reader on the Sanskrit Grammarians»** (точнее, задачников по санскриту серии Eméneau). Модуль предназначен для отработки правил **сандхи** (sandhi) — фонетических изменений на стыке слов и морфем в санскрите.

Пользователь видит текстовую задачу (например, строку из санскритского текста с нераскрытыми сандхи), должен определить правила сандхи и сопоставить решение с эталонным.

---

## Предметная область

### Сандхи (Sandhi rules)
Правила фонетических изменений, по которым звуки меняются на стыке слов. Каждое правило имеет:
- **ruleNumber** — порядковый номер правила
- **ruleType** — тип (`external` / `internal`)
- **whitneyNumber** — номер по грамматике Уитни (стандартная ссылка)
- **shortDescription** — краткое описание (например, «a + i → e»)
- **fullText** — полная формулировка правила
- **iastExample** / **hkExample** — пример в транслитерации IAST и Harvard-Kyoto
- **notes** — примечания
- **sandhiRuleGroups** — принадлежность к группам (many-to-many)

### Группы правил (SandhiRuleGroup)
Тематические группы правил сандхи (например: «гласные + гласные», «конечные согласные»). Поля: `description`, `code`.

### Упражнения (Exercise)
Набор задач из учебника. Идентифицируются как «Упражнение 5» или «Упражнение 5a» (поля `exerciseNumber` + `exerciseLetter`). Содержат `instructionText` — текст задания.

### Задачи (Task)
Конкретный пример внутри упражнения. Имеет `taskNumber` и `taskText` — текст на санскрите для анализа.

### Решения (Solution)
К каждой задаче может быть несколько решений (правильных и неправильных). Поля:
- **solutionText** — правильная запись раскрытого сандхи
- **stepByStep** — пошаговое объяснение (редактируемое ADMIN-ом)
- **isCorrect** — признак эталонного решения
- **sandhiRules** (через `SolutionSandhiRule`) — список правил сандхи, применённых в этом решении

### Ответы (Answer)
Варианты ответа для задачи (связаны с `Task`). Используются для режима выбора из вариантов.

### Фонемная система (Phoneme)
Полная классификация санскритских фонем по артикуляционным признакам:
- **PlaceOfArticulation** — место артикуляции (велярный, палатальный, ретрофлексный, дентальный, лабиальный)
- **MannerOfArticulation** — способ артикуляции (смычный, фрикативный, носовой, аппроксимант)
- **Voicing** — звонкость (глухой, звонкий)
- **Aspiration** — придыхательность (непридыхательный, придыхательный)
- **Varga** — традиционная группа согласных (ка-варга, ча-варга и т.д.)

Каждая фонема хранится в трёх транслитерациях: IAST, Harvard-Kyoto, Devanagari.

---

## API Endpoints

Базовый путь: `/api/v1/eamenau`  
Сервис: `content-service` (порт 8081)  
Аутентификация: все endpoint'ы доступны аутентифицированным пользователям (STUDENT+)  
Исключение: `PUT /exercises/solutions/{id}` — только ADMIN

### Правила сандхи

```
GET /api/v1/eamenau/sandhi-rules
```
Возвращает все правила сандхи, отсортированные по `ruleNumber` по возрастанию.

**Ответ:** `List<SandhiRuleDto>`
```json
[
  {
    "id": 1,
    "ruleNumber": 1,
    "ruleType": "external",
    "shortDescription": "a/ā + a/ā → ā",
    "whitneyNumber": "§98",
    "iastExample": "ca api → cāpi",
    "hkExample": "ca api → cApi",
    "notes": null,
    "fullText": "Два гласных a или ā сливаются в долгий ā...",
    "sandhiRuleGroups": [
      { "id": 1, "description": "Гласные + гласные", "code": "vowel-vowel" }
    ]
  }
]
```

### Упражнения

```
GET /api/v1/eamenau/exercises
```
Список всех упражнений, отсортированных по `exerciseNumber ASC, exerciseLetter ASC`.

**Ответ:** `List<EamenauExerciseDto>`
```json
[
  { "id": 1, "exerciseNumber": 1, "exerciseLetter": null, "instructionText": "Раскройте сандхи:" },
  { "id": 2, "exerciseNumber": 1, "exerciseLetter": "a", "instructionText": "Определите правила:" }
]
```

```
GET /api/v1/eamenau/exercises/{id}
```
Подробное упражнение со списком задач (без решений).

**Ответ:** `EamenauExerciseDetailDto`
```json
{
  "id": 1,
  "exerciseNumber": 1,
  "exerciseLetter": null,
  "instructionText": "Раскройте сандхи:",
  "tasks": [
    { "id": 1, "taskNumber": 1, "taskText": "rāma iti" },
    { "id": 2, "taskNumber": 2, "taskText": "deva atra" }
  ]
}
```

```
GET /api/v1/eamenau/exercises/{exerciseId}/sandhi-rules
```
Уникальные правила сандхи, задействованные в эталонных решениях всех задач упражнения. Используется для отображения «справочника правил» рядом с упражнением.

**Ответ:** `List<SandhiRuleInfo>`
```json
[
  { "ruleNumber": 1, "shortDescription": "a/ā + a/ā → ā" },
  { "ruleNumber": 7, "shortDescription": "a + i → e" }
]
```

### Решения

```
GET /api/v1/eamenau/exercises/tasks/{taskId}/solution
```
Эталонные решения (`isCorrect = true`) для конкретной задачи, включая пошаговое объяснение и применённые правила сандхи.

**Ответ:** `List<SolutionDto>`
```json
[
  {
    "id": 1,
    "solutionText": "rāma iti → rāmeti",
    "stepByStep": "1. rāma + iti\n2. a + i → e (правило №7)\n3. rāmeti",
    "sandhiRules": [
      { "ruleNumber": 7, "shortDescription": "a + i → e" }
    ]
  }
]
```

```
PUT /api/v1/eamenau/exercises/solutions/{solutionId}
```
Обновление пошагового объяснения и привязки правил сандхи. **Только ADMIN.**

**Тело запроса:** `SolutionUpdateRequestDto`
```json
{
  "stepByStep": "1. rāma + iti\n2. a + i → e (правило №7)\n3. rāmeti",
  "ruleNumbers": "7, 12"
}
```
Поле `ruleNumbers` — строка с номерами правил через запятую/пробел/точку с запятой. Сервис сам резолвит номера в ID и выполняет diff (добавляет новые, удаляет убранные).

---

## Схема данных (PostgreSQL schema: `eamenau`)

```
eamenau.sandhi_rules_group          ← группы правил
eamenau.sandhi_rules                ← правила сандхи
eamenau.sandhi_rules_group_map      ← many-to-many: правило ↔ группа

eamenau.exercises                   ← упражнения (exerciseNumber + exerciseLetter)
eamenau.tasks                       ← задачи внутри упражнения
eamenau.answers                     ← варианты ответов к задаче
eamenau.solutions                   ← эталонные решения задачи
eamenau.solution_sandhi_rules       ← many-to-many: решение ↔ правило сандхи

eamenau.phonemes                    ← санскритские фонемы
eamenau.place_of_articulation       ← место артикуляции
eamenau.manner_of_articulation      ← способ артикуляции
eamenau.voicing                     ← звонкость
eamenau.aspiration                  ← придыхательность
eamenau.varga                       ← традиционная группа (варга)
```

### ER-диаграмма (упрощённо)

```
Exercise ──< Task ──< Solution >── SolutionSandhiRule >── SandhiRule >── SandhiRuleGroup
                  └──< Answer

Phoneme >── PlaceOfArticulation
        >── MannerOfArticulation
        >── Voicing
        >── Aspiration
        >── Varga
```

---

## Фронтенд

| Компонент | Путь | Описание |
|---|---|---|
| `EmeneauExercisesPage` | `/eamenau` или `/exercises` | Список всех упражнений |
| `EmeneauExerciseDetailPage` | `/eamenau/exercises/:id` | Упражнение с задачами и решениями |
| `SolutionPanel` | компонент | Отображение решения с пошаговым разбором и правилами сандхи |
| `VocabularyEamenauPage` | `/vocabulary/eamenau` | Словарная страница, связанная с Eamenau-контентом |

> ⚠️ Обратите внимание: в именах файлов используется написание «Emeneau» (через `e`), в именах Java-классов и URL — «Eamenau». Это историческая непоследовательность, которую стоит унифицировать.

---

## Фонемная система — статус

Модели `Phoneme`, `PlaceOfArticulation`, `MannerOfArticulation`, `Voicing`, `Aspiration`, `Varga` и их репозитории реализованы, но **не имеют ни одного API endpoint'а**. Данные хранятся в БД и могут использоваться для:

- Отображения таблицы фонем (алфавита) с артикуляционными характеристиками
- Фильтрации и поиска правил сандхи по типу фонем
- Будущего модуля произношения

**Статус: данные есть, API нет — открытый вопрос.**

---

## Open Questions

- [ ] Нужен ли API для фонемной системы (`GET /api/v1/eamenau/phonemes`)? Если да — кто реализует (Backend Domain Agent, Агент 2)?
- [ ] Унифицировать написание: `Eamenau` или `Emeneau`? Рекомендация: `Eamenau` (как в Java-коде)
- [ ] Нужна ли пагинация для `GET /eamenau/exercises`? Сейчас возвращает все упражнения без limit
- [ ] `Answer` (варианты ответа к задаче) — реализован в модели и репозитории, но не используется ни в одном endpoint'е и не отображается на фронтенде. Планируется ли режим теста с выбором варианта?
- [ ] Должен ли `PUT /solutions/{id}` проверять роль ADMIN через `@PreAuthorize`? Сейчас проверки нет

---

## Связанные файлы

### Backend (content-service)

**Модели** (`services/content-service/src/main/java/sm/selflearn/samskrtam/eamenau/model/`)
- `Exercise.java` — упражнение
- `Task.java` — задача внутри упражнения
- `Answer.java` — варианты ответа (не используется в API)
- `Solution.java` — эталонное решение с пошаговым разбором
- `SolutionSandhiRule.java` — связка решение ↔ правило сандхи
- `SandhiRule.java` — правило сандхи (с Whitney-номером и примерами)
- `SandhiRuleGroup.java` — тематическая группа правил
- `Phoneme.java` — санскритская фонема (не используется в API)
- `Varga.java` — традиционная группа согласных
- `PlaceOfArticulation.java`, `MannerOfArticulation.java`, `Voicing.java`, `Aspiration.java` — артикуляционные характеристики

**Сервисы** (`services/content-service/src/main/java/sm/selflearn/samskrtam/content/service/`)
- `EamenauService.java` — работа с правилами сандхи
- `EamenauExerciseService.java` — упражнения, задачи, решения; логика diff для `SolutionSandhiRule`

**Контроллеры** (`services/content-service/src/main/java/sm/selflearn/samskrtam/content/controller/`)
- `EamenauController.java` — `GET /api/v1/eamenau/sandhi-rules`
- `EamenauExerciseController.java` — все `/api/v1/eamenau/exercises/**`

**Shared DTOs** (`shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/content/dto/`)
- `EamenauExerciseDto.java`
- `EamenauExerciseDetailDto.java`
- `EamenauTaskDto.java`
- `SandhiRuleDto.java`, `SandhiRuleGroupDto.java`, `SandhiRuleInfo.java`
- `SolutionDto.java`, `SolutionUpdateRequestDto.java`

**Миграции** (`services/content-service/src/main/resources/db/migration/`)
- `V2__create_eamenau_schema_and_sandhi_rules_table.sql` — создание схемы `eamenau` и всех таблиц

### Frontend

- `frontend/src/pages/eamenau/EmeneauExercisesPage.tsx`
- `frontend/src/pages/eamenau/EmeneauExerciseDetailPage.tsx`
- `frontend/src/components/eamenau/SolutionPanel.tsx`
- `frontend/src/pages/vocabulary/VocabularyEamenauPage.tsx`