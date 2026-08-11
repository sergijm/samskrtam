# Lexicon Content Pipeline — наполнение ~2000 базовых лемм

> Связанные файлы: [lexicon.md](./lexicon.md), [lexical-curriculum.md](./lexical-curriculum.md),
> [sangraha-service.md](./sangraha-service.md) (единственный источник сырья, см. §1).

> **Решение по задаче (зафиксировано):** источник 2000 лемм — только корпус
> sangraha-service (уже проанализированные, `status=ANALYZED`, стихи и их
> `VerseWord[]`), без внешних частотных списков (DCS и т.п.). Классификация —
> **без AI-enrichment**: только эвристики по данным, уже имеющимся в
> `VerseWord`/`VerseWordMorphology` (грамматика туда уже пришла от LLM-анализа
> sangraha-service на этапе `/verses/{id}/analyze`, повторный LLM-вызов здесь не
> нужен), плюс ручной ADMIN CRUD для донаполнения/исправления. Оба пункта — прямое
> изменение относительно черновой версии этого документа (там источником был
> внешний корпус + AI-enrichment).

---

## 1. Source data — только корпус sangraha-service

Кандидаты на 2000 лемм собираются исключительно из уже проанализированных
произведений sangraha-service — никакого стороннего частотного списка не
привлекается:

| Поле | Источник | Комментарий |
|---|---|---|
| `lemmaIast`/`lemmaDevanagari` | `sangraha.verse_words.lemma_iast` + `surface_devanagari` по всем `Verse.status = ANALYZED` (across все произведения, не только те, где кто-то нажал «Изучить» — см. §6 об отличии от текущего on-demand потока) | Читается напрямую из БД sangraha-service (batch export/внутренний REST, см. §2), не через LLM повторно |
| `frequencyRank` | Вычисляется в самом pipeline: `rank` = позиция леммы в списке, отсортированном по `COUNT(*)` вхождений `(lemmaIast, stem)` в `verse_words` по всему корпусу, по убыванию | Целиком производное от объёма уже загруженных текстов — при малом корпусе (мало произведений) полоса `EXTENDED` (`lexical-curriculum.md` §2) может остаться недонаселённой; это открытый вопрос §5, не блокер |
| `glossRu`/`glossEn` | `VerseWord.lemmaGlossRu/En` (уже заполнено LLM на этапе анализа стиха в sangraha-service, см. `verse-word-grammar.md`) — при нескольких встречаемостях одной леммы с разными глоссами берётся глосс представителя с наибольшим числом вхождений (§3, tie-break — первый по `verseId`/`position`) | Не генерируется заново — просто переносится |
| `pos`, `gender` | `VerseWord.pos`/`VerseWordMorphology.gender` (уже есть от LLM-анализа sangraha-service) — маппится в `lexicon.part_of_speech`/`gender` таблицей соответствий кодов (см. §2) | Эвристика только в маппинге кодов, не в определении значения |
| `morphology` (стем-класс/verb-класс) | Правило по `VerseWordMorphology.vowelType` (для именных, то же поле, что уже используется `sangraha-service.md` §9) / глагольному классу, если он есть в разборе; при отсутствии — правило по последней букве `stem` (тот же fallback-принцип, что и `sangraha-service.md` §9 для `vowelType`) | Полностью эвристика по уже размеченным данным, без обращения к LLM |
| `Source`/`SourceOccurrence` | 1:1 с уже загруженными `verse_words`: `source.externalSangrahaWorkSlug = workSlug`, `source_occurrence.locationRef = "{chapterSlug}.{verseOrderIndex}"`, `surfaceFormIast` = `VerseWord.surfaceIast` | Данные не парсятся заново — переиспользуются как есть, см. §2 шаг 1 |

**Явно вне периметра этой итерации:** внешние корпуса (DCS и т.п.), словарь
`dictionary-service` как источник (см. `lexicon.md` §0 п.1), любой LLM-вызов
внутри самого lexicon-pipeline (LLM-анализ уже произошёл раньше, в
sangraha-service, на этапе `/verses/{id}/analyze` — pipeline лексикона его не
повторяет и не проверяет заново).

---

## 2. Import — детерминированный batch-процесс (без AI)

Раз в LLM-вызовах внутри pipeline нет необходимости, наполнение — обычный
batch-job (ADMIN-инициируемый, синхронный или асинхронный — решает Агент 2 при
реализации), а не очередь review для AI-предложений:

1. **Экспорт сырья.** Новый internal-эндпоинт (по аналогии с уже существующими
   `sangraha-service.md` §9): `GET /sangraha/internal/content/verse-words/export`
   — отдаёт весь `VerseWord[]` по всем `Verse.status = ANALYZED`, постранично
   (курсор по `verseId`), с полями `lemmaIast`, `surfaceDevanagari`, `stem`,
   `pos`, `lemmaGlossRu/En`, `gender` (из `VerseWordMorphology`), `vowelType`,
   `workSlug`, `chapterSlug`, `verseOrderIndex`, `verseId`. Вызывается
   curriculum-service напрямую по `SANGRAHA_SERVICE_URL` (тот же принцип, что и
   `sangraha-service.md` §9 — не публичный, не через gateway).
2. **Группировка и подсчёт частоты.** curriculum-service группирует полученные
   строки по `(lemmaSlp1, gender)` (та же ключ уникальности, что `lexicon.md`
   §1), считает `COUNT(*)` на группу → это и есть базис для `frequencyRank`
   (сортировка по убыванию `COUNT(*)`, при равенстве — по алфавиту `lemmaSlp1`,
   детерминированно).
3. **Маппинг кодов (эвристика, не LLM).** Для каждой группы:
   - `pos` из `VerseWord.pos` → код `curriculum.part_of_speech` таблицей
     соответствий (например, sangraha `NOUN` → `noun`, `VERB_FINITE` →
     `finite-verb` — полный список соответствий фиксирует Агент 2 при
     реализации по факту значений `VerseWord.pos`, см. `verse-word-grammar.md`);
   - `gender` из `VerseWordMorphology.gender` — прямой перенос (те же значения
     enum, что уже согласованы между sangraha-service и curriculum-service, см.
     `sangraha-service.md` §9);
   - `morphologyClassCode` из `VerseWordMorphology.vowelType`/verb-класса —
     маппинг 1:1 по совпадающим кодам (`lexical-curriculum.md` §5), fallback —
     по последней букве `stem`, если `vowelType` не заполнен;
   - `semanticTopicId` — **не проставляется автоматически** (нет источника
     значения без LLM/словаря) — остаётся пустым при импорте, заполняется
     вручную ADMIN через `LexemeTaxonomyController` (`lexicon.md` §3.2,
     `task-curriculum-16`) отдельным шагом после импорта, батчами по теме
     (например, отфильтровать импортированные леммы по `posCode=noun` и
     разметить `Animals` вручную по списку) — это единственное поле, где ручной
     труд не заменяется эвристикой в этой версии.
4. **Создание/апдейт `Lexeme`.** Дедуп по `(lemmaSlp1, gender)` — если строка
   уже существует (например, от предыдущего batch-импорта) — обновить
   `frequencyRank`/пересчитать occurrences, не создавать дубль. Новая строка →
   `status = CANDIDATE` (не `DRAFT`: `DRAFT` резервируется за леммами, которые
   ADMIN начал вручную и не доимпортировал, `CANDIDATE` — специально за
   результатом этого батч-импорта, см. `lexicon.md` §1).
5. **`SourceOccurrence`.** Для каждого исходного `VerseWord` — строка
   `source_occurrence` (см. §1 таблицу), `Source` создаётся/находится по
   `externalSangrahaWorkSlug = workSlug` (dedup по этому полю). После батча —
   пересчёт `totalOccurrencesCache`/`uniqueLemmaCountCache` (`lexicon.md` §4).
6. **Идемпотентность.** Повторный запуск импорта (например, после того как в
   sangraha-service проанализировали новые произведения) — не создаёт дублей
   `Lexeme`/`SourceOccurrence` (дедуп по `(lemmaSlp1, gender)` и по
   `(sourceId, lexemeId, locationRef, surfaceFormIast)` соответственно),
   пересчитывает только частоты/кэши.

---

## 3. Validation & review — ручной ADMIN-гейт вместо AI-гейта

Раз строки приходят не от LLM, а детерминированной эвристикой, требования к
ревью — **не** «проверить, не ошиблась ли модель», а «проверить, что эвристика
дала осмысленный результат», набор проверок при этом похожий:

| Проверка | Правило | Действие при нарушении |
|---|---|---|
| Дубликаты | `UNIQUE(lemmaSlp1, gender)` на уровне БД, обеспечивается уже на шаге импорта (§2 шаг 4) | Merge на этапе группировки, не отдельная очередь |
| Отсутствующий `semanticTopicId` | У всех свежеимпортированных `CANDIDATE`-лексем | Не блокирует `APPROVED` жёстко (иначе весь батч встанет), но `LexemeController` (`task-curriculum-16`) отдаёт отдельный фильтр `GET /lexemes?status=CANDIDATE&semanticTopicId=null` для приоритизации ручной разметки |
| POS/gender не промаппились (эвристика не нашла код) | `posCode`/`morphologyClassCode` = `null` после шага 3 | Флаг `needsReview`, реализуется как `status` остаётся `CANDIDATE` с пустым полем — видно в admin-фильтре, отдельной сущности не заводится (тот же принцип упрощения, что был в черновике) |
| Gender для NOMINAL POS | Для `posCode` из группы `NOMINAL` — `gender` обязателен бизнес-правилом (см. `lexicon.md` §1) | Отсутствие — блокирует переход `CANDIDATE → APPROVED` |
| Транслитерация | Автосверка `lemmaIast` ↔ `lemmaDevanagari` тем же конвертером, что уже используется в проекте (sangraha-service §5.2/dictionary-service) | Несовпадение — блокирует `APPROVED` |
| Порог по Topic | Как в черновике: лист `SemanticTopic`/`LexicalTopic` с итоговым наполнением < 10 лексем после ручной разметки — сигнал на объединение (`lexical-curriculum.md` §3) | Отчёт "Topics below threshold" в admin UI, не автоблокировка |

**Единственный обязательный человеческий шаг** — переход `CANDIDATE → APPROVED`
(`PATCH /lexemes/{id}/status`, `task-curriculum-16`); только `APPROVED`-лексемы
попадают в `pool/resolve` по умолчанию (`lexical-quizzes.md` §0) — это правило
не меняется относительно черновика, меняется только то, что предшествует
`CANDIDATE` (эвристика вместо LLM).

---

## 4. Порядок наполнения — по факту накопленного корпуса, не батчами по 200

В черновой версии предполагался батч-план по диапазонам `rank` (1–200,
201–400, …) — он был рассчитан на внешний частотный список, доступный сразу и
целиком. При источнике "только sangraha-corpus" **весь** объём леммы
определяется объёмом уже загруженных и проанализированных произведений — план
не по rank-диапазонам, а по этапам роста корпуса:

1. **Первый импорт** — после того как в sangraha-service проанализировано
   достаточно текста, чтобы `COUNT(DISTINCT lemmaSlp1)` перевалило за
   заметный порог (ориентир — несколько сотен уникальных лемм; конкретное
   число, при котором имеет смысл первый запуск batch-импорта, фиксирует
   Агент 2/ADMIN по факту, не заранее).
2. **Повторные импорты** — по мере анализа новых произведений в
   sangraha-service, идемпотентно (§2 шаг 6); `frequencyRank` пересчитывается
   каждый раз заново по актуальному `COUNT(*)`, старые ранги не «замораживаются».
3. **Цель 2000 уникальных лемм** — это ориентир объёма корпуса, а не
   гарантированный результат одного прогона: если проанализированного текста
   недостаточно для 2000 уникальных лемм, pipeline честно возвращает меньше
   (например, 800) — задача не требует искусственного добора до ровно 2000
   (в отличие от черновика, где внешний список это гарантировал).
4. **После каждого импорта:** отчёт "Topics below threshold" (§3), приоритетная
   очередь `CANDIDATE`-лексем без `semanticTopicId` для ручной разметки (§3),
   пересчёт `SourceOccurrence`-кэшей (`lexicon.md` §4).

---

## 5. Открытые вопросы

- **Достаточность объёма корпуса sangraha-service для 2000 уникальных лемм** —
  зависит от того, сколько произведений/стихов будет проанализировано к моменту
  запуска pipeline; если объём корпуса органически ограничивает результат
  меньшим числом лемм — решение принимается по факту (расширить корпус
  дополнительными произведениями в sangraha-service, либо принять меньший
  словарь), не в этом документе.
- **Ручная разметка `semanticTopicId`** — самое трудоёмкое место pipeline
  теперь (единственное поле без эвристики); возможное будущее облегчение —
  словарь ключевых слов/сопоставление по корню для грубой авто-подсказки перед
  ручным подтверждением, вне периметра текущей итерации.
- Омонимы с одинаковым `gender`, но разным значением — по-прежнему не
  разделяются на разные `Lexeme` (см. `lexicon.md` §1) — то же упрощение, что
  в черновике, решение не пересматривалось.
- Формат/периодичность запуска импорта (ручная кнопка ADMIN vs cron) — вопрос
  реализации, не архитектуры; в этой итерации — ручной запуск ADMIN
  (`POST /api/v2/lexicon/import/from-sangraha`, см. `task-curriculum-14`).

---

## 6. Отличие от существующего on-demand потока (§6/§7 `sangraha-service.md`)

Этот batch-pipeline **не заменяет и не трогает** существующий поток «кнопка
«Изучить» → `POST /content/internal/sangraha/vocabulary-quiz` →
`content.vocabulary_words`» (`sangraha-service.md` §6, `curriculum-service.md`
§11, они по-прежнему пишут в старую, отдельную таблицу `content.vocabulary_words`,
не в `curriculum.lexeme`) — это два независимых механизма, читающих один и тот
же первичный источник (`sangraha.verse_words`) по-разному:
- on-demand поток — по клику, один стих, попадает в `content.vocabulary_words`,
  создаёт per-verse `VOCABULARY`-квиз (`Lesson`);
- этот batch-pipeline — весь корпус разом, попадает в `curriculum.lexeme`,
  питает lexical-квизы нового поколения (`lexical-quizzes.md`).

Слияние этих двух механизмов в один (чтобы `content.vocabulary_words` и
`curriculum.lexeme` не были двумя параллельными таблицами с частично
пересекающимися данными) — миграционная задача, явно вынесенная за периметр
текущей итерации (см. `lexicon.md` §0 п.2, тот же открытый вопрос, что и в
черновике).

---

## 7. Источник `semanticTopicId`/перевода — обновление (см. `sangraha-service/lemma-classification.md`)

Пункты §2 шаг 3 («`semanticTopicId` не проставляется автоматически») и §2 шаг
3 («выбор glossRu/glossEn от представителя с наибольшим числом вхождений»)
**заменяются** отдельным модулем классификации на стороне sangraha-service
(`sangraha-service/lemma-classification.md`, схема `CURRICULUM`) — LLM
батчами по частотности классифицирует леммы по 42-листовой таксономии
(`lexical-curriculum.md` §3) и одновременно даёт перевод, с ручным
ADMIN-review на стороне sangraha-service.

**Актуальный порядок шага 3 импорта:**
1. Перед группировкой (§2 шаг 2) curriculum-service запрашивает
   `GET /sangraha/internal/lexicon/lemma-classifications/export?schemeCode=CURRICULUM&status=APPROVED`
   (постранично, `sangraha-service/lemma-classification.md` §5).
2. Если для группы `(lemmaSlp1, gender)` есть `APPROVED`-классификация —
   `semanticTopicId` (по `categoryCode`) и `glossRu`/`glossEn` берутся из неё
   напрямую, шаг 3 старого §2 для этой группы не выполняется.
3. Если классификации нет (ещё не прогнан run в sangraha-service, или лемма
   не набрала `APPROVED`-статус) — поведение как раньше: `semanticTopicId`
   пустой, `glossRu`/`glossEn` — от представителя с наибольшим числом
   вхождений (fallback, не удалён, см. §1 текущего документа) — импорт не
   блокируется отсутствием классификации, просто такая лексема остаётся в
   очереди ручной разметки (§3), пока классификация не появится.

Таким образом «единственное поле без эвристики» (§3, было верно для первой
версии этого документа) больше не совсем так — теперь для большинства лемм
есть LLM-кандидат, отревьюженный ADMIN на стороне sangraha-service; ручная
работа ADMIN curriculum-service (§3) остаётся страховкой для лемм, не
дошедших до `APPROVED` в sangraha-service к моменту импорта.
