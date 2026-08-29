# GrammarLessonPage (`/lessons/grammar/:type`)

> ⚠️ **Требует согласования:** этот документ описывает UI-контракт (`filterScope`, `statusFilter=REVIEW`, `lessonId`) в терминах старой модели прогресса quiz-service. Модель прогресса и API сессий переработаны — см. [services/quest-engine.md](../../services/quest-engine.md). Детали фронтенд-контракта в этом файле нуждаются в пересмотре под новый API (`questId`, статусы NEW/LEARNING/DUE/MASTERED, без ручного `filterScope`).

> Вынесено из [lesson-pages-spec.md](./lesson-pages-spec.md) §3 по правилу лимита 350 строк (conventions.md §9, паттерн «индекс + подпапка»).
> Связанные файлы: [lesson-pages-spec.md](./lesson-pages-spec.md) (общая концепция, VocabularyLessonPage, роутинг, типы, acceptance criteria) · [quest-engine.md](../../services/quest-engine.md) §3.4 (filterScope) · [quest-engine.md](../../services/quest-engine.md) §3 (statusFilter)
> Status: **DRAFT**

---

## 1. Назначение

Показывает список грамматических вопросов урока с правильными ответами и индивидуальной статистикой.

## 2. Элементы страницы

**Шапка урока:** заголовок + счётчик `total` из `statusSummary` (без `LessonStatsBadges`). Ниже шапки, **вне `TabView`** — `LessonStatsTab` (см. §2.1), это не отдельная вкладка (расхождение с более ранней версией этого документа — зафиксировано и исправлено; актуальный источник истины — `frontend/src/pages/lessons/GrammarLessonPage.tsx`).

**Вкладки (`TabView`), порядок слева направо:**

| # | Вкладка | Компонент | Содержимое |
|---|---|---|---|
| 1 | Парадигмы | `GrammarParadigmCarousel` + `DeclensionEndingsReferenceTable` | карусель реальных словоформ по стемам (падеж×число, см. §2.2) со статичной справочной таблицей окончаний над ней (кликабельна, см. §2.2б) |
| 2 | Примеры | `DeclensionExamplesTab` | реальные цитаты из проанализированных стихов на каждую ячейку парадигмы, см. §2.2а |
| 3 | Прогресс | `GrammarProgressGrid` | **НОВОЕ**, заменяет удалённые «По падежам»/«По числам»/«Подробно», см. §2.1а |

**УДАЛЕНО:** вкладки «По падежам» (`CaseAggregationTable`), «По числам» (`NumberAggregationTable`), «Подробно» (`GrammarDetailsTable`) и соответствующие им компоненты. Их функциональность (запуск квиза по падежу/числу) полностью покрывается новой вкладкой «Прогресс» (§2.1а); детальная таблица вопросов (бывшая «Подробно», с колонкой «Попытки» → `QuestionHistoryDialog`) в новом UI не воспроизводится — сознательное упрощение по требованию пользователя, а не забытая функция.

Статус кодируется цветом `ProgressBar` (переиспользуется в §2.1а):

| Статус (`WordStatus`) | Цвет `ProgressBar` |
|---|---|
| `NEW` | серый (`text-color-secondary` / нейтральный) |
| `LEARNING` | синий (`text-primary`) |
| `REVIEW` | жёлтый (`text-yellow-500`) |
| `MASTERED` | зелёный (`text-green-500`) |

Цвета переиспользуют ту же палитру, что и `WordStatusIcon` (см. `frontend/src/components/lesson/WordStatusIcon.tsx`), чтобы не заводить второй источник цветовой кодировки статуса. Реализация — через `PrimeReact ProgressBar` с кастомным CSS-классом на цвет заливки (`--progressbar-value-bg` / аналог, конкретный механизм окраски определяет Агент 3 при реализации, единственное требование — использовать один и тот же маппинг статус→цвет во всех трёх таблицах, вынесенный в общую утилиту, а не дублируемый в каждом компоненте).

**Таблица вопросов (вкладка «Подробно», `DataTable`):**

| Колонка | Содержимое |
|---|---|
| Вопрос | `textRu` / `textEn` по локали |
| Правильный ответ | текст правильного варианта (всегда виден) |
| Попытки | кликабельный `{nSuccess}/{nAll}` |

**Клик на `{nSuccess}/{nAll}`** → открывает `QuestionHistoryDialog` — аналог WordHistoryDialog для грамматических вопросов.

## 2.1а. GrammarProgressGrid + GrammarProgressTagSets (вкладка «Прогресс»)

**НОВОЕ.** Вкладка состоит из двух компонентов: `GrammarProgressGrid` (сводная таблица падеж × число) и `GrammarProgressTagSets` (срезы-строки по падежам, числам и парам падежей) под ней. Заменяет удалённые `CaseAggregationTable`/`NumberAggregationTable`/`GrammarDetailsTable`.

**Данные.** Агрегация прогресса **вычисляется на бэкенде** (`GrammarProgressAggregationService` в quiz-service) и приходит в ответе `GET /api/v2/lessons/grammar/{topicCode}` четырьмя массивами `GrammarLesson`:
- `caseAggregations: CaseAggregation[]` — по падежам (для заголовков строк и строк срезов);
- `numberAggregations: NumberAggregation[]` — по числам (заголовки столбцов и строки срезов);
- `grid: CaseNumberAggregation[]` — по парам `(caseType, numberType)` (ячейки сетки);
- `pairAggregations: PairAggregation[]` — по парам падежей (`setId` = `progressTagSetId`, `caseRuA/caseRuB` — локализованные названия пары).

`lesson.questions` / `GrammarQuestionProgress` клиенту больше не отдаются; фронтовая агрегация (`aggregateByCaseAndNumber` и пр.) удалена из `utils/grammarAggregation.ts` — там остались только константы `CASE_TYPES`/`NUMBER_TYPES`.

Семантика агрегатов (см. OpenAPI `schemas/grammar.yaml`): `aggregatedProgress = Math.round(avg(score))` по вопросам группы, `learnedCombinations` — вопросы с `score >= MASTERY_THRESHOLD (90)`, `status` — `NEW` при `avg <= 0`, `LEARNING` при `< 90`, иначе `MASTERED` (`REVIEW` на уровне агрегата не используется). Порядок падежей/чисел/пар фиксирован на бэкенде, совпадает с бывшими фронтовыми `CASE_TYPES`×`NUMBER_TYPES` и порядком `CASE_PAIRS` (GEN_LOC, GEN_ABL, DAT_ACC, INS_ABL, INS_LOC, ACC_LOC, DAT_GEN, ABL_LOC).

**Разметка `GrammarProgressGrid`:**
- Заголовок первой колонки пуст (угловая ячейка).
- Первая строка — заголовки столбцов: `numberRu`/`numberEn` по локали из `numberAggregations`. **Кликабельно** → запускает/резюмирует квиз `filterScope=NUMBER_ONLY&filterNumberTypes=<numberType>` (контракт бывшей `NumberAggregationTable` не меняется, см. `quest-engine.md` §3.4).
- Первая колонка каждой строки — `caseRu`/`caseEn` из `caseAggregations`. **Кликабельно** → `filterScope=CASE_ONLY&filterCaseType=<caseType>` (контракт бывшей `CaseAggregationTable`, не меняется).
- Ячейки (падеж×число) — `MiniProgressBar` со `value=aggregatedProgress`, `status` для цвета, **без `onClick`** (клик по самой ячейке ничего не запускает; запуск квиза описывается осями заголовков). Ячейка без данных (нет агрегата для этой пары в `grid`) рендерится прочерком.
- `GrammarLessonPage` ищет ячейку по ключу `${caseType}:${numberType}` в массиве `grid`.

**Разметка `GrammarProgressTagSets`:** две колонки — слева строки по падежам и числам, справа по парам падежей. Каждая строка: название (`caseRu/caseEn`, `numberRu/numberEn`, `caseRuA ↔ caseRuB` по локали), `MiniProgressBar` (`aggregatedProgress`/`status`), кнопка запуска квиза → `?progressTagSetId=<setId>` (для падежей/чисел `setId` = `caseType`/`numberType`). Компонент рендерится из `caseAggregations`/`numberAggregations`/`pairAggregations`; при пустых массивах не отображается.

- Детальная таблица вопросов (бывшая «Подробно», клик `{nSuccess}/{nAll}` → `QuestionHistoryDialog`) не воспроизводится — при удалении `GrammarDetailsTable` удалены и состояния `selectedCaseType`/`selectedNumberType`/`selectedGender`/`questionHistoryDialogVisible`/`sortField`/`sortOrder`, существовавшие только ради неё.



## 2.1. LessonStatsTab (вкладка «Статистика»)

Заменяет `LessonStatsBadges` только на `GrammarLessonPage` — в шапке урока больше не отображается. `VocabularyLessonPage` продолжает использовать `LessonStatsBadges` в шапке без изменений (см. lesson-pages-spec.md §2.1); унификация вынесена в открытые вопросы (lesson-pages-spec.md §9).

Строится из того же `statusSummary: LessonStatusSummary`, что и `LessonStatsBadges` (см. lesson-pages-spec.md §7), отдельного запроса не требует. Строки расположены вертикально, каждая — название, значение (кроме «Всего» — со знаменателем `total`), и кнопка запуска/резюме квиза (кроме «Всего»):

| Строка | Значение | Кнопка | Клик запускает/резюмирует квиз |
|---|---|---|---|
| Всего | `{statusSummary.total}` | — | — |
| Не изучено | `{statusSummary.newCount}` | «Изучить» | `statusFilter=NEW` |
| В процессе | `{statusSummary.learning}` | «Продолжить» | `statusFilter=LEARNING` |
| Изучено | `{statusSummary.mastered}` | «Повторить» | `statusFilter=REVIEW` (доступно при `reviewDue > 0`, см. architecture.md §3.6) |

**Поведение:** идентично `LessonStatsBadges` — клик по кнопке вызывает `POST /quiz/{slug}/sessions/start-or-resume?...&statusFilter=<NEW|LEARNING|REVIEW>` и переходит на `/quiz/grammar/:type`, квиз стартует или резюмируется в зависимости от наличия IN_PROGRESS-сессии с тем же `statusFilter`. Кнопка строки с нулевым значением (`newCount === 0`, `learning === 0`, либо для «Изучено» — `reviewDue === 0`) недоступна (`disabled`), строка остаётся видимой.

Кнопки этой вкладки готовы и выполняют полезное действие — `statusFilter` реализован на бэкенде (см. [quest-engine.md §3](../../services/quest-engine.md) и `quiz-generator-spec.md` §3/§7 п.5).

## 2.2. GrammarParadigmTable (вкладка «Парадигмы»)

Справочная таблица словоформ — классическая грамматическая парадигма (падеж × число), в отличие от §2.1/«По падежам»/«Подробно» не про прогресс пользователя, а про содержание урока: какие формы вообще есть и как они выглядят.

**Источник данных и навигация (карусель, не всё сразу).** Урок обычно содержит несколько стемов-примеров (слов) одного грамматического типа — грузить формы всех сразу не нужно и вредно (лишний трафик, пользователю нужен один пример за раз). Компонент хранит локальный `currentIndex` (React `useState`, старт — 0, не персистится между заходами на вкладку) и на каждое его изменение делает **новый** запрос — `GET /api/v1/content/public/lessons/{slug}/declension-paradigms?index={currentIndex}` → `DeclensionParadigmPageDto` (см. `services/curriculum-service.md` §5а, `openapi/content/content-api.yaml`). Хук `useDeclensionParadigm(slug, index)` (React Query, ключ `['declension-paradigm', slug, index]`, данные статичны — без auto-refetch); соседний индекс (`currentIndex ± 1`) можно prefetch-ить в фоне при простое, чтобы клик по стрелке не показывал спиннер — не обязательно для первой версии.

**Элементы вкладки:**
- Над таблицей — панель навигации: стрелка «←», счётчик `{index + 1} / {totalCount}`, стрелка «→». Стрелка «←» задизейблена при `index === 0`, «→» — при `index === totalCount - 1`.
- Одна таблица на экран — парадигма ровно одного стема (`paradigm` из ответа), падеж×число (×подзаголовок с родом текущего стема — `genderRu`/`genderEn`, без отдельной логики «два рода = две таблицы»: раз стемы разных родов урока уже линейно перечислены в общем списке карусели, каждая страница карусели просто показывает *свой* род).
- Заголовок таблицы — `stemIast` (крупно) + `stemDevanagari` мелким шрифтом + перевод (`translationRu`/`translationEn` по локали).
- Строки — 8 падежей (`CASE_TYPES`, см. `utils/grammarAggregation.ts`), в фиксированном порядке.
- Столбцы — `numberType`, присутствующие среди `forms` текущего стема (обычно SINGULAR/DUAL/PLURAL).
- Ячейка — `formIast` (крупно) + `formDevanagari` мелким шрифтом, ищется в `forms` по (`caseType`, `numberType`) текущей строки/столбца; пусто (`—`), если записи нет (неполный сид, см. `curriculum-service.md` §3 про `sanskrit_declensions_enriched`).
- Пока не выполнена миграция `translation_ru/translation_en`/data-fix `stem_devanagari` (задача Агенту 2, `curriculum-service.md` §4/§5а) — эти поля приходят `null`, заголовок таблицы деградирует до одного `stemIast`; это временное, не блокирующее рендер состояние, а не ошибка.

## 2.2б. DeclensionEndingsReferenceTable — клик по ячейке окончания (вкладка «Парадигмы»)

**НОВОЕ.** `DeclensionEndingsReferenceTable` (см. `frontend/src/components/lesson/DeclensionEndingsReferenceTable.tsx`, данные — `data/aStemEndingsTable.ts`) — статичная справочная таблица абстрактных окончаний (падеж × число×род, столбцы вида `sgM`/`sgN`/`duMN`/`plM`/`plN`), рендерится над `GrammarParadigmCarousel` (§2.2), пока не кликабельна. Теперь её ячейки становятся кликабельными.

**Разбор ячейки.** Каждый столбец `EndingsTableData.columns[].key` кодирует число + (опционально) род: `sgM`→(`SINGULAR`,`MASCULINE`), `sgN`→(`SINGULAR`,`NEUTER`), `duMN`→(`DUAL`, род не фильтруется — совпадают формы `MASCULINE`/`NEUTER`), `plM`→(`PLURAL`,`MASCULINE`), `plN`→(`PLURAL`,`NEUTER`); конкретный набор столбцов зависит от `vowelType` (см. `vowelTypeToEndingsTable`), маппинг «столбец → (numberType, gender|undefined)» — новая константа `ENDINGS_COLUMN_TO_NUMBER_GENDER` рядом с `aStemEndingsTable.ts`. `EndingsRow.caseKey` (нижний регистр, например `'nominative'`) маппится на `CASE_TYPES` (верхний регистр, `'NOMINATIVE'`) — новая функция `caseKeyToCaseType` там же.

**Состояние.** На странице `GrammarLessonPage.tsx` (вкладка «Парадигмы») — новое состояние `selectedEndingCell: { caseType: string; numberType: string; gender?: string } | null` (изначально `null`). Клик по непустой ячейке `DeclensionEndingsReferenceTable` (пустая/`isIdentity`-ячейка, например «= основа» у Vocative, не кликабельна) устанавливает `selectedEndingCell` в разобранные `(caseType, numberType, gender)`. Пока `selectedEndingCell !== null`: `GrammarParadigmCarousel` скрывается, вместо него рендерится новый компонент `DeclensionEndingWordsTable`.

**Источник данных `DeclensionEndingWordsTable`.** Никакого нового backend-эндпоинта не требуется (см. решение по вопросу 6/8 задачи) — компонент переиспользует уже показываемые в карусели данные: все страницы `GET /content/public/lessons/{slug}/declension-paradigms?index=0..totalCount-1`. Новый хук `useAllDeclensionParadigms(slug, totalCount, enabled)` (React Query `useQueries`, тот же `queryFn`/`queryKey`-паттерн, что `useDeclensionParadigm`, `enabled = selectedEndingCell !== null && totalCount > 0`) параллельно запрашивает все индексы (дедуплицируется с уже закэшированными страницами карусели по тому же `queryKey`, повторных сетевых запросов для уже открытых стемов не будет). `totalCount` берётся из уже загруженной страницы карусели (поднимается в состояние `GrammarLessonPage`, либо `GrammarParadigmCarousel` получает его как проп сверху — на усмотрение реализации, лишь бы не дублировался запрос ради самого числа).

**Таблица слов.** Для каждого `DeclensionParadigmDto` из всех загруженных страниц, где среди `forms` есть запись с `(caseType, numberType)` из `selectedEndingCell` (и `gender`, если он задан в `selectedEndingCell`; для `duMN` — оба рода) — строка: девангари (`form.formDevanagari` слова целиком, не только основы), IAST (`form.formIast`), перевод (`translationRu`/`translationEn` стема по локали). Внутри `formIast`/`formDevanagari` каждой строки суффикс, совпадающий с текстом кликнутой ячейки (`EndingsCell.text`, без ведущего дефиса), оборачивается в `<span>` с цветовым выделением (то же цветовое решение, что и статус-цвета — фон/текст акцентного цвета темы, конкретный класс на усмотрение Агента 3). Стемы без формы для этой ячейки (неполный сид) в таблицу не попадают.

**Возврат к карусели.** Заголовок таблицы — те же оси, что у справочной таблицы: первая колонка «Падеж», первая строка «Число/Род» (или объединённый заголовок, аналогично `DeclensionEndingsReferenceTable`). Клик по названию падежа (первая колонка) или по названию числа/рода (первая строка) внутри `DeclensionEndingWordsTable` сбрасывает `selectedEndingCell` в `null` → `GrammarParadigmCarousel` показывается снова, `DeclensionEndingWordsTable` скрывается. Клик по обычной ячейке таблицы слов (не заголовку) — не определён, действия не выполняет.

**Что не делает эта вкладка (открытый вопрос п.7 отклонён пользователем):** запуск/резюм квиза по клику на ячейку `DeclensionEndingsReferenceTable` или по строке `DeclensionEndingWordsTable` не реализуется — такого поведения нет и не требуется.

## 2.2а. DeclensionExamplesTab (вкладка «Примеры»)

Отдельная вкладка верхнего уровня (см. таблицу вкладок в §2, позиция #3), не часть `GrammarParadigmTable` (§2.2) и не переключатель внутри неё — независимый компонент, без карусели.

**Данные загружаются одним запросом на весь урок, без постраничной навигации** (в отличие от §2.2 — эндпоинт адресуется по словоизменительному классу `(vowelType, gender)` урока в целом, а не по конкретному стему-шагу карусели «Парадигмы»). Запрос — `POST /api/v1/sangraha/verses/examples` с телом `{ vowelType, gender, limitPerGroup }` → `DeclensionExamplesResponseDto` (`groups[]` — по группе на ячейку `(caseType, numberType)`, агрегирует sangraha сам, см. `sangraha-service.md` §9). Хук `useDeclensionExamples(slug, vowelType, gender, enabled)` (React Query, ключ `['declension-examples', slug, vowelType, gender]`, без auto-refetch — данные статичны); запрос делается лениво — только пока вкладка «Примеры» активна (`enabled`), переключение на другую вкладку и обратно не шлёт новый запрос повторно (React Query кеш по тому же ключу).

**Элементы вкладки:**
- Заголовок вкладки — название урока/класса (`titleRu`/`titleEn` урока, тот же источник, что заголовок страницы урока) — без указания конкретного стема, т.к. ответ `DeclensionExamplesResponseDto` относится ко всему словоизменительному классу урока, а не к одному стему (`groups[]` содержит только `caseType`/`numberType`/`examples`, без `stem`).
- Список групп в порядке падежей `CASE_TYPES`, внутри падежа — по числам `NUMBER_TYPES` (`utils/grammarAggregation.ts`); заголовок группы — `{caseRu}, {numberRu}` (или `caseEn`/`numberEn` по локали). Внутри группы — карточка на каждый пример из `examples[]`: `textIast` крупно, `textDevanagari` мелким шрифтом под ним, `translationRu`/`translationEn` по локали, внизу мелкая атрибуция `«{workTitleRu}, {chapterTitleRu}, стих {verseOrderIndex}»` (аналогично `workTitleEn`/`chapterTitleEn`).
- Группа с пустым `examples[]` не рендерится вовсе (не показывать «нет примеров» отдельным блоком на каждую из ~14 ячеек — визуальный шум; просто пропустить). Если у ответа вообще нет ни одной непустой группы — один общий текст-заглушка на всю вкладку («Примеров пока нет», без иконки-ошибки — ожидаемое состояние для редких/местоимённых классов, не сбой).

**Кнопка «Открыть все стихи» (только ADMIN).** Над списком групп — кнопка `«Открыть все стихи ({n})»`, где `n` — число уникальных `verseId` в `examples[]` (всех групп). По клику — переход (обычный `navigate`) на `/sangraha/verses` **без query-параметров**: список `verseId` кладётся в localStorage по ключу `sangraha.verseBatchIds` (JSON-массив строк, хелперы `frontend/src/utils/verseBatchIds.ts`), а страница `/sangraha/verses` при отсутствии параметра `id` в URL читает его оттуда (см. `sangraha-service/batch-verse-review.md`). Раньше также была кнопка «Проанализировать недостающие примеры» (поле `missingVerseIds`) — вместе с quiz-service агрегацией этот функционал удалён.

**Что не делает эта вкладка:** карточки не кликабельны — примеры не запускают квиз (в отличие от ячеек таблицы §2.2), это чисто справочная иллюстрация; ссылок на страницу стиха (`VersePage`) в первой версии тоже нет (открытый вопрос ниже).

### Открытые вопросы (для Агента 3 при реализации)

- Клик по карточке примера → переход на `VersePage` конкретного стиха (`verseId` есть в каждом примере) — полезно, но не специфицировано детально (какой роут, открывается ли на стихе с подсветкой нужного слова); не блокирует первую версию, можно отложить.
- Источник `totalCount` для панели навигации этой вкладки (см. выше) — решает Агент 3, единственное требование — не делать отдельный полновесный запрос ради одного числа, если оно уже есть в кэше от вкладки «Парадигмы».



**Клик по ячейке** (когда есть форма) — запускает или резюмирует квиз, отфильтрованный именно на эту комбинацию: `POST /quiz/{slug}/sessions/start-or-resume?...&filterScope=CASE_NUMBER_GENDER&filterCombinations=<caseType>:<numberType>:<gender>` (тот же контракт, что уже используют `filterScope`-фильтры declension-квиза, см. quest-engine.md §3.4 — в отличие от `statusFilter`, эта ветка **реализована** на бэкенде, доп. работы у Агента 2 не требуется), затем переход на `/quiz/grammar/:type`.