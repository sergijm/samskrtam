# Quest Engine — движок квестов по грамматике и лексике

> Заменяет: `services/quiz-service.md`, `services/quiz-service/quiz-generator-spec.md`,
> `services/quiz-service/quiz-declension.md`, `services/quiz-service/quiz-service-repositories.md`.
> Связанные файлы: [README.md](../README.md) · [architecture.md](../architecture.md) · [curriculum-service.md](./curriculum-service.md) · [quest-catalog.md](./quest-catalog.md) · [quest-item-model.md](./quest-item-model.md)

---

## 1. Идея

**quiz-service** остаётся тем же сервисом (порт, роут `/api/v1/quiz/`, схема БД `quiz` — не меняются, см. [architecture.md §3.2](../architecture.md#32-семантика-quiz--lesson--activity)), и внутренняя модель прохождения и прогресса — единый алгоритм планирования повторений (spaced repetition, вариант SM-2) плюс именованные прогресс-сеты (§2.4) для любой единицы данных и любого типа задания.

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
- **MASTERED** — `repetitions ≥ 3`.
- **DIFFICULT** — ортогональная ось к перечисленным (независимо от статуса): последний ответ
  неверный серией (`consecutiveMistakes ≥ 2`) или низкий score (`≤ difficultUpperThreshold`);
  выход из сета с гистерезисом `difficultExitMargin` (см. §2.4).

`DUE` (пора повторить: `repetitions ≥ 3`, `due_at ≤ now`) — **внутренний атрибут отбора**, а не
отображаемый статус: из него выбираются детали внутри сета `MASTERED`, отдельным статусом в UI
не показывается.

### 2.4 Прогресс-сеты (ProgressTagSet)

Урок = набор `progressTag`: атомарных признаков в ключе агрегации прогресса (склонения —
`(caseType, numberType, gender)`; лексика — `formIast` слова). `ProgressTagSet` — именованное
подмножество progressTag урока; прогресс сета — средний score по входящим в него тегам.

Запуск/резюм сессии по срезу — через стабильный `progressTagSetId` (параметр `start`/`start-or-resume`,
вместо ручных фильтров по бакету или грамматическому scope). Состав фиксирован дизайн-таблицей:

| setId | Состав | Когда нужен |
|---|---|---|
| `NEW` | теги без строки прогресса | «Изучить»/бейдж «Новые» |
| `LEARNING` | начато, но не MASTERED | «Продолжить»/бейдж «В процессе» |
| `MASTERED` | score ≥ masteredLowerThreshold (внутри — отбор по `DUE`/`due_at`) | «Повторить»/бейдж «Изучено» |
| `DIFFICULT` | `consecutiveMistakes ≥ 2` ИЛИ `score ≤ difficultUpperThreshold` (с гистерезисом выхода) | упражнения на ошибки |
| `SINGULAR`/`DUAL`/`PLURAL` | теги по числу (только declension) | заголовки «Прогресса», §2.1а |
| `ACC_LOC`/`INS_ABL`/`GEN_LOC`/`DAT_ACC` | теги пары падежей с омонимичными окончаниями (declension) | заголовки «Прогресса»/ячейки парадигм |

Сессия по сету — по всему срезу (всем тегам сета), без ограничений размера среза; существующий
`progressTagSetId` сохраняется в запросе на старте, резюм — по равенству id (`quiz_session`.
`progress_tag_set_id`).

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

По умолчанию отбор — обычный смешанный due/new/LEARNING. Грамматический срез внутри урока
задаётся именованным `progressTagSetId` (§2.4: SINGULAR/DUAL/PLURAL, ACC_LOC/INS_ABL/GEN_LOC/DAT_ACC) —
это набор progressTag урока, а не отдельный Quest; содержательный срез за пределами именованных
сетов — отдельный Quest с нужным подмножеством QuestItem на этапе формирования контента.

### 4.2 Ответ

`POST /api/v1/quiz/sessions/{sessionId}/attempts` — `{ itemId, answer }` → `{ correct, correctAnswer }`. Обновляет `progress` по правилам §3. Сессия хранит только список item_id и порядок — сами Attempt не персистятся отдельной таблицей, только агрегат `progress`.

### 4.3 Завершение

`POST /api/v1/quiz/sessions/{sessionId}/complete` → сводка `{ total, correct }`. Публикует `QuestSessionCompletedEvent` в Kafka (Outbox Pattern, как раньше) для statistics-service.

---

## 5. Границы с curriculum-service

`item_id` в `quest.progress` — UUID без физического FK на схему источника (разные БД, разные
сервисы). Существование проверяется прикладным кодом при старте сессии; soft-delete в
источнике — предпочтительный способ не терять прогресс при архивации контента.

Источник `QuestItem` — **curriculum-service (API v2)** через `CurriculumClient` для всех
типов. quiz-service получает пул топика (id, itemType, progressTag), выполняет прогресс-отбор
и запрашивает материализацию отобранных вопросов через compose-эндпоинт.

---

## 6. Что сознательно не переносится из старой версии

- **Ручные грамматические фильтры** — на уровне сессии содержимое среза задаётся одним
  `progressTagSetId` (§2.4), а не комбинацией параметров scope. Нужен урок с одним падежом —
  это именованный сет (или отдельный Quest с нужным подмножеством QuestItem на этапе формирования
  контента), а не параметр запроса на каждый старт сессии.
- **Двойной статус MASTERED/REVIEW** — статусы плоские: NEW/LEARNING/MASTERED для отображения;
  `DUE` — внутренний атрибут отбора внутри сета MASTERED, отдельным статусом не показывается (§2.3).
- **Отдельные пороги (difficultUpperThreshold/masteredLowerThreshold) на каждый itemType** — заменены одним универсальным алгоритмом интервалов (SM-2), без калибровки под тип.
- **Ручные веса дистракторов (ENDING_MATCH и т.п.)** — генерация дистракторов остаётся в зоне ответственности curriculum-service per-type, но как деталь генератора, не завязанная на прогресс.

---

## 7. Открытые вопросы

- Стартовые константы SM-2 (ease_factor=2.5, интервалы 1/3 дня) — стандартные значения алгоритма, калибровка на реальных данных — задача после запуска.
- Формат хранения `distractors` в curriculum-service (JSON-массив в QuestItem vs отдельная таблица) — решает Агент 2 при реализации.
- Нужен ли лимит на количество NEW-единиц в одной сессии (чтобы не заваливать новыми словами) — открыто, предлагается стартовое значение 5 из `size`.
