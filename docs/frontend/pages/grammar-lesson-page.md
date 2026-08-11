# GrammarLessonPage (`/lessons/grammar/:type`)

> Актуализирован под модель прогресс-сетов (ProgressTagSet) — см. [services/quest-engine.md](../../services/quest-engine.md). Запуск квиза — по стабильному `progressTagSetId`, без ручных фильтров.

> Вынесено из [lesson-pages-spec.md](./lesson-pages-spec.md) §3 по правилу лимита 350 строк (conventions.md §9, паттерн «индекс + подпапка»).
> Связанные файлы: [lesson-pages-spec.md](./lesson-pages-spec.md) (общая концепция, VocabularyLessonPage, роутинг, типы, acceptance criteria) · [quest-engine.md](../../services/quest-engine.md) §3 (прогресс-сеты)
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
| `MASTERED` | зелёный (`text-green-500`) |
| `DIFFICULT` (ортогональная ось) | красный (`text-red-500`) |

Цвета переиспользуют ту же палитру, что и `WordStatusIcon` (см. `frontend/src/components/lesson/WordStatusIcon.tsx`), чтобы не заводить второй источник цветовой кодировки статуса. Реализация — через `PrimeReact ProgressBar` с кастомным CSS-классом на цвет заливки (`--progressbar-value-bg` / аналог, конкретный механизм окраски определяет Агент 3 при реализации, единственное требование — использовать один и тот же маппинг статус→цвет во всех трёх таблицах, вынесенный в общую утилиту, а не дублируемый в каждом компоненте).

**Таблица вопросов (вкладка «Подробно», `DataTable`):**

| Колонка | Содержимое |
|---|---|
| Вопрос | `textRu` / `textEn` по локали |
| Правильный ответ | текст правильного варианта (всегда виден) |
| Попытки | кликабельный `{nSuccess}/{nAll}` |

**Клик на `{nSuccess}/{nAll}`** → открывает `QuestionHistoryDialog` — аналог WordHistoryDialog для грамматических вопросов.

## 2.1а. GrammarProgressGrid (вкладка «Прогресс»)

**НОВОЕ.** Заменяет вкладки «По падежам» (`CaseAggregationTable`), «По числам» (`NumberAggregationTable`) и «Подробно» (`GrammarDetailsTable`) одной сводной таблицей: строки — падежи (`CASE_TYPES`, фиксированный порядок), столбцы — числа (`NUMBER_TYPES = ['SINGULAR','DUAL','PLURAL']`, фиксированный порядок). Никаких новых бэкенд-запросов не требуется — источник данных тот же `lesson.questions: GrammarQuestionProgress[]`, что и у старых `CaseAggregationTable`/`NumberAggregationTable`/`GrammarDetailsTable`.

**Агрегация.** Новая функция `aggregateByCaseAndNumber(questions: GrammarQuestionProgress[])` в `utils/grammarAggregation.ts` (рядом с существующими `aggregateByCase`/`aggregateByNumber`, которые остаются — переиспользуются для расчёта заголовков строк/столбцов, см. ниже): группировка вопросов по паре `(caseType, numberType)`, для каждой непустой пары — `{ caseType, numberType, aggregatedProgress: Math.round(learned/total*100), status: aggregatedProgress >= MASTERY_THRESHOLD ? 'MASTERED' : 'LEARNING', totalCombinations, learnedCombinations }` (`learned` — количество вопросов с `score >= MASTERY_THRESHOLD`, идентично существующим `aggregateByCase`/`aggregateByNumber`). Возвращает `Map`/массив, по которому компонент ищет ячейку по ключу `${caseType}:${numberType}`.

**Разметка таблицы:**
- Заголовок первой колонки пуст (угловая ячейка).
- Первая строка — заголовки столбцов: название числа (`numberRu`/`numberEn` по локали, берётся из `aggregateByNumber(questions)` — переиспользуется только как источник локализованных названий, не для агрегации). **Кликабельно** → запускает/резюмирует квиз `progressTagSetId=<SINGULAR|DUAL|PLURAL>` по срезу соответствующего числа (см. quest-engine.md §2.4).
- Первая колонка каждой строки — название падежа (`caseRu`/`caseEn`, аналогично из `aggregateByCase`). **Кликабельно** → `progressTagSetId=<ACC_LOC|INS_ABL|GEN_LOC|DAT_ACC>` — квиз по паре падежей, в которую входит данный (омонимичные окончания, architecture.md §3.3; если падеж не входит ни в одну пару — по этому одному падежу).
- Остальные ячейки (падеж×число) — `MiniProgressBar` (см. `components/common/MiniProgressBar.tsx`, уже используется в старых `CaseAggregationTable`/`NumberAggregationTable`) со `value=aggregatedProgress`, `status` для цвета, **без `onClick`** — по прямому решению пользователя клик по самой ячейке ничего не запускает (там нет отдельного смысла «выбрать»: запуск квиза целиком описывается осями заголовков). Ячейка без данных (нет вопросов для этой пары падеж×число в уроке) рендерится пустой/прочерком, без `MiniProgressBar`.
- Детальная таблица вопросов (бывшая «Подробно», клик `{nSuccess}/{nAll}` → `QuestionHistoryDialog`) в этой вкладке не воспроизводится — при удалении `GrammarDetailsTable` из страницы удаляется и вызов `QuestionHistoryDialog` вместе с состояниями `selectedCaseType`/`selectedNumberType`/`selectedGender`/`questionHistoryDialogVisible`/`sortField`/`sortOrder`, которые существовали только ради неё.



## 2.1. LessonStatsTab (вкладка «Статистика»)

Заменяет `LessonStatsBadges` только на `GrammarLessonPage` — в шапке урока больше не отображается. `VocabularyLessonPage` продолжает использовать `LessonStatsBadges` в шапке без изменений (см. lesson-pages-spec.md §2.1); унификация вынесена в открытые вопросы (lesson-pages-spec.md §9).

Строится из того же `statusSummary: LessonStatusSummary`, что и `LessonStatsBadges` (см. lesson-pages-spec.md §7), отдельного запроса не требует. Строки расположены вертикально, каждая — название, значение (кроме «Всего» — со знаменателем `total`), и кнопка запуска/резюме квиза (кроме «Всего»):

| Строка | Значение | Кнопка | Клик запускает/резюмирует квиз |
|---|---|---|---|
| Всего | `{statusSummary.total}` | — | — |
| Не изучено | `{statusSummary.newCount}` | «Изучить» | `progressTagSetId=NEW` |
| В процессе | `{statusSummary.learning}` | «Продолжить» | `progressTagSetId=LEARNING` |
| Изучено | `{statusSummary.mastered}` | «Повторить» | `progressTagSetId=MASTERED` (внутри сета отбор деталей — см. quest-engine.md §2.4) |

**Поведение:** идентично `LessonStatsBadges` — клик по кнопке вызывает `POST /quiz/{slug}/sessions/start-or-resume?progressTagSetId=<NEW|LEARNING|MASTERED>` и переходит на `/quiz/grammar/:type`, квиз стартует или резюмируется в зависимости от наличия IN_PROGRESS-сессии с тем же `progressTagSetId`. Кнопка строки с нулевым значением (`newCount === 0`, `learning === 0`, `mastered === 0`) недоступна (`disabled`), строка остаётся видимой.

Кнопки этой вкладки готовы и выполняют полезное действие — `progressTagSetId` реализован на бэкенде (см. [quest-engine.md §2.4](../../services/quest-engine.md) и `quiz-generator-spec.md` §3/§7 п.5).

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

**Данные загружаются одним запросом на весь урок, без постраничной навигации** (в отличие от §2.2 — эндпоинт адресуется только по `slug`, без `index`, потому что примеры группируются по словоизменительному классу `(vowelType, gender)` урока в целом, а не по конкретному стему-шагу карусели «Парадигмы», см. `services/curriculum-service/declension-examples.md`). Запрос — `GET /content/public/lessons/{slug}/examples` → `DeclensionExamplesResponseDto` (см. `services/curriculum-service/declension-examples.md`, `openapi/content/content-api.yaml`). Хук `useDeclensionExamples(slug)` (React Query, ключ `['declension-examples', slug]`, без auto-refetch — данные статичны); запрос делается лениво — только пока вкладка «Примеры» активна (`enabled: activeTab === 'examples'`), переключение на другую вкладку и обратно не шлёт новый запрос повторно (React Query кеш по тому же ключу).

**Элементы вкладки:**
- Заголовок вкладки — название урока/класса (`titleRu`/`titleEn` урока, тот же источник, что заголовок страницы урока) — без указания конкретного стема, т.к. ответ `DeclensionExamplesResponseDto` относится ко всему словоизменительному классу урока, а не к одному стему (`groups[]` содержит только `caseType`/`numberType`/`examples`, без `stem`).
- Список групп в порядке падежей `CASE_TYPES`, внутри падежа — по числам `NUMBER_TYPES` (`utils/grammarAggregation.ts`); заголовок группы — `{caseRu}, {numberRu}` (или `caseEn`/`numberEn` по локали). Внутри группы — карточка на каждый пример из `examples[]`: `textIast` крупно, `textDevanagari` мелким шрифтом под ним, `translationRu`/`translationEn` по локали, внизу мелкая атрибуция `«{workTitleRu}, {chapterTitleRu}, стих {verseOrderIndex}»` (аналогично `workTitleEn`/`chapterTitleEn`).
- Группа с пустым `examples[]` не рендерится вовсе (не показывать «нет примеров» отдельным блоком на каждую из ~14 ячеек — визуальный шум; просто пропустить). Если у ответа вообще нет ни одной непустой группы — один общий текст-заглушка на всю вкладку («Примеров пока нет», без иконки-ошибки — ожидаемое состояние для редких/местоимённых классов, не сбой).

**Кнопка «Проанализировать недостающие примеры» (только ADMIN).** `DeclensionExamplesResponseDto` для роли `ADMIN` дополнительно содержит `missingVerseIds: UUID[]` — список стихов, которые были найдены sangraha-service как грамматические кандидаты (см. `sangraha-service.md` §9, поиск без фильтра по `ANALYZED`), но не попали в `examples[]`, потому что не прошли фильтр `ANALYZED` на шаге `POST .../verses/batch` (см. `services/curriculum-service/declension-examples.md`, шаг 4) — то есть реально существуют в базе sangraha, но ещё не проанализированы LLM, поэтому невидимы ученику. Для `STUDENT`/анонимного пользователя поле не передаётся (`null`/отсутствует) — список внутренних `verseId` и сам факт «есть непроанализированные кандидаты» не предназначен для обычных пользователей. Если `missingVerseIds` непусто — над списком групп кнопка `«Проанализировать недостающие примеры ({missingVerseIds.length})»`, по клику — переход (обычный `navigate`, тот же SPA, не новая вкладка) на `/sangraha/verses?id={id1}&id={id2}&...` (новая страница sangraha-service, см. `sangraha-service/batch-verse-review.md`) с полным списком `missingVerseIds` в query-параметрах.

**Что не делает эта вкладка:** карточки не кликабельны — примеры не запускают квиз (в отличие от ячеек таблицы §2.2), это чисто справочная иллюстрация; ссылок на страницу стиха (`VersePage`) в первой версии тоже нет (открытый вопрос ниже).

### Открытые вопросы (для Агента 3 при реализации)

- Клик по карточке примера → переход на `VersePage` конкретного стиха (`verseId` есть в каждом примере) — полезно, но не специфицировано детально (какой роут, открывается ли на стихе с подсветкой нужного слова); не блокирует первую версию, можно отложить.
- Источник `totalCount` для панели навигации этой вкладки (см. выше) — решает Агент 3, единственное требование — не делать отдельный полновесный запрос ради одного числа, если оно уже есть в кэше от вкладки «Парадигмы».



**Клик по ячейке** (когда есть форма) — запускает или резюмирует квиз, отфильтрованный именно на эту комбинацию: `POST /quiz/{slug}/sessions/start-or-resume?progressTagSetId=<ACC_LOC|INS_ABL|GEN_LOC|DAT_ACC|SINGULAR|DUAL|PLURAL>` по ближайшему именованному срезу, покрывающему ячейку (тот же контракт, что используют заголовки §2.1а и бейджи LessonStatsTab — см. quest-engine.md §2.4, ветка **реализована** на бэкенде, доп. работы у Агента 2 не требуется), затем переход на `/quiz/grammar/:type`.