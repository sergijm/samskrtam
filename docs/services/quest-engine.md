# Quest Engine — движок квестов по грамматике и лексике

> Заменяет: `services/quiz-service.md`, `services/quiz-service/quiz-generator-spec.md`,
> `services/quiz-service/quiz-declension.md`, `services/quiz-service/quiz-service-repositories.md`.
> Связанные файлы: [README.md](../README.md) · [architecture.md](../architecture.md) · [curriculum-service.md](./curriculum-service.md) · [quest-catalog.md](curriculum-service/quest-catalog.md) · [quest-item-model.md](./quest-item-model.md)

---

## 1. Идея

**quiz-service** остаётся тем же сервисом (порт, роут `/api/v1/quiz/`, схема БД `quiz` — не меняются, см. [architecture.md §3.2](../architecture.md#32-семантика-quiz--lesson--activity)), но внутренняя модель прохождения и прогресса переработана с нуля. Вся сложность предыдущей версии (произвольные пороги score, отдельный «производный» REVIEW-статус, ручная фильтрация по scope, разные веса дистракторов для разных типов вопросов) заменена одним алгоритмом планирования повторений (spaced repetition, вариант SM-2) и единой моделью данных для любого типа задания.

**Термины:**

| Термин | Значение |
|---|---|
| **QuestItem** | Атомарная обучаемая единица: словарное слово или грамматическая форма. Живёт в curriculum-service. |
| **Quest** | Именованный набор QuestItem одной темы (было — Lesson/Quiz). Пример: «declensions-a-masc», «vocabulary-sangraha-verse-42». |
| **QuestSession** | Один заход пользователя в Quest: последовательность попыток по подмножеству его QuestItem. |
| **Attempt** | Одна попытка ответа на один QuestItem в рамках сессии. |
| **Progress** | Состояние обучения пользователя по одному QuestItem (across-session, не привязано к конкретной сессии). |

---

## 2. Данные

### 2.1 curriculum-service — что это за items

`QuestItem`: id (UUID), questId, type (`VOCABULARY_WORD` | `DECLENSION_FORM` | …, открытое перечисление), prompt (что показываем — слово/форма для перевода или основа+падеж), answer (эталонный ответ), distractors (0–3 неверных варианта, если задание — выбор из вариантов; для free-text заданий пусто).

Формирование `prompt`/`answer`/`distractors` — полностью ответственность curriculum-service, per-type генераторы (declension, vocabulary, в будущем — conjugation и др.) живут там же, каждый в своём файле рядом с `curriculum-service.md`. quiz-service о внутреннем устройстве типов ничего не знает — работает с уже готовыми QuestItem через один контракт.

### 2.2 quiz-service — прогресс

Одна таблица `quest.progress`:

| Поле | Тип | Смысл |
|---|---|---|
| user_id | UUID | — |
| item_id | UUID | ссылка на `QuestItem.id` (без физического FK, см. §5) |
| repetitions | int | сколько раз подряд отвечено верно с текущего «сброса» |
| ease_factor | decimal | множитель интервала (SM-2), стартует с 2.5, не ниже 1.3 |
| interval_days | int | текущий интервал до следующего показа |
| due_at | timestamp | когда единица снова становится доступной для повторения |
| last_result | bool | последний ответ верный/неверный |
| updated_at | timestamp | — |

Строки создаются лениво — при первой попытке ответа. Нет строки → единица `NEW`.

### 2.3 Статус (для UI)

Статус всегда выводится из полей `progress`, отдельно нигде не хранится:

- **NEW** — строки нет.
- **LEARNING** — строка есть, `repetitions < 3`.
- **DUE** — `repetitions ≥ 3` и `due_at ≤ now` (пора повторить).
- **MASTERED** — `repetitions ≥ 3` и `due_at > now`.

Никакого отдельного «REVIEW как частный случай MASTERED» — DUE и есть то самое «пора повторить», без двойного смысла одного бакета.

---

## 3. Алгоритм повторения (SM-2, упрощённый)

После каждого ответа:

1. Если ответ **неверный** — `repetitions = 0`, `interval_days = 1`, `ease_factor` уменьшается на 0.2 (не ниже 1.3).
2. Если ответ **верный**:
   - `repetitions += 1`
   - `interval_days`: 1 → 1 день (repetitions=1), 2 → 3 дня (repetitions=2), далее `interval_days = round(interval_days_прошлый × ease_factor)`
   - `ease_factor` увеличивается на 0.1 (не выше 2.8)
3. `due_at = now + interval_days`

Один алгоритм для всех типов QuestItem — никаких отдельных формул score/threshold на тип, никакой калибровки «черновых» констант под каждый itemType по отдельности.

---

## 4. Сессия

### 4.1 Старт

`POST /api/v1/quiz/sessions/start?questId={id}`

Пул кандидатов = все QuestItem этого Quest. Отбор для сессии (размер — параметр `size`, по умолчанию 20):

1. Сначала — все с статусом **DUE** (сортировка по `due_at` — самые просроченные первыми).
2. Затем — **NEW** (случайный порядок).
3. Если пул всё ещё не заполнен — **LEARNING** (по `repetitions` возрастанию — сложнее сначала).

Никакой ручной фильтрации по грамматическому scope (падеж/число/род) на уровне сессии —
если пользователь хочет тренировать конкретный срез, это отдельный Quest с этим срезом
(содержательная фильтрация переносится на этап формирования контента, а не на выполнение
запроса каждый раз).

### 4.2 Ответ

`POST /api/v1/quiz/sessions/{sessionId}/attempts` — `{ itemId, answer }` → `{ correct, correctAnswer }`. Обновляет `progress` по правилам §3. Сессия хранит только список item_id и порядок — сами Attempt не персистятся отдельной таблицей, только агрегат `progress`.

### 4.3 Завершение

`POST /api/v1/quiz/sessions/{sessionId}/complete` → сводка `{ total, correct }`. Публикует `QuestSessionCompletedEvent` в Kafka (Outbox Pattern, как раньше) для statistics-service.

---

## 5. Границы с curriculum-service и curriculum-service

`item_id` в `quest.progress` — UUID без физического FK на схему источника (разные БД, разные
сервисы). Существование проверяется прикладным кодом при старте сессии; soft-delete в
источнике — предпочтительный способ не терять прогресс при архивации контента.

Источник `QuestItem` зависит от типа: для `DECLENSION_FORM`-семейства (4 типа, см.
[curriculum-quest-items.md](curriculum-service/curriculum-quest-items.md)) quiz-service обращается к
**curriculum-service (API v2)** через `CurriculumClient`; для остальных типов (`VOCABULARY_*`
и т.д.) — по-прежнему к **curriculum-service (API v1)** через `ContentClient`, без изменений.
Оба клиента реализуют один и тот же внутренний контракт получения списка `QuestItem` —
выбор клиента по `itemType`, без ветвления в алгоритме сессии/прогресса (§3–4 этого файла).

---

## 6. Что сознательно не переносится из старой версии

- **filterScope/filterCaseTypes/filterNumberTypes/filterCombinations** — убраны целиком. Нужен урок только с одним падежом — заводится отдельный Quest с нужным подмножеством QuestItem на этапе создания контента, а не параметром запроса на каждый старт сессии.
- **Двойной статус MASTERED/REVIEW** — заменён на плоский список статусов NEW/LEARNING/DUE/MASTERED без пересечений и «частных случаев».
- **Отдельные пороги (difficultUpperThreshold/masteredLowerThreshold) на каждый itemType** — заменены одним универсальным алгоритмом интервалов (SM-2), без калибровки под тип.
- **Ручные веса дистракторов (ENDING_MATCH и т.п.)** — генерация дистракторов остаётся в зоне ответственности curriculum-service per-type, но как деталь генератора, не завязанная на прогресс.

---

## 7. Открытые вопросы

- Стартовые константы SM-2 (ease_factor=2.5, интервалы 1/3 дня) — стандартные значения алгоритма, калибровка на реальных данных — задача после запуска.
- Формат хранения `distractors` в curriculum-service (JSON-массив в QuestItem vs отдельная таблица) — решает Агент 2 при реализации.
- Нужен ли лимит на количество NEW-единиц в одной сессии (чтобы не заваливать новыми словами) — открыто, предлагается стартовое значение 5 из `size`.
