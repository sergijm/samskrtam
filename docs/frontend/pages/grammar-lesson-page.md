# GrammarLessonPage (`/lessons/grammar/:type`)

> Вынесено из [lesson-pages-spec.md](./lesson-pages-spec.md) §3 по правилу лимита 350 строк (conventions.md §9, паттерн «индекс + подпапка»).
> Связанные файлы: [lesson-pages-spec.md](./lesson-pages-spec.md) (общая концепция, VocabularyLessonPage, роутинг, типы, acceptance criteria) · [quiz-declension.md](../../../services/quiz-service/quiz-declension.md) §3.4 (filterScope) · [quiz-generator-spec.md](../../../services/quiz-service/quiz-generator-spec.md) §3 (statusFilter)
> Status: **DRAFT**

---

## 1. Назначение

Показывает список грамматических вопросов урока с правильными ответами и индивидуальной статистикой.

## 2. Элементы страницы

**Шапка урока:** отличается от VocabularyLessonPage — заголовок слева, справа только кнопка «Начать квиз» (без `LessonStatsBadges`; панель статистики перенесена во вкладку «Статистика», см. §2.1).

**Вкладки (`TabView`), порядок слева направо:**

| # | Вкладка | Компонент | Содержимое |
|---|---|---|---|
| 1 | Статистика | `LessonStatsTab` | статистика урока по статусам, см. §2.1 |
| 2 | Парадигмы | `GrammarParadigmTable` | справочная таблица словоформ падеж×число (×род), см. §2.2 |
| 3 | Примеры | `DeclensionExamplesTab` | реальные цитаты из проанализированных стихов на каждую ячейку парадигмы, см. §2.2а |
| 4 | По падежам | `CaseAggregationTable` | агрегация вопросов по падежу |
| 5 | По числам | `NumberAggregationTable` | агрегация вопросов по числу (SINGULAR/DUAL/PLURAL), см. §2.1а |
| 6 | Подробно | `GrammarDetailsTable` | таблица вопросов, см. ниже |

**ИЗМЕНЕНО:** отдельной колонки «Статус» больше нет ни в одной из таблиц вкладок (`CaseAggregationTable`, `NumberAggregationTable`, `GrammarDetailsTable`) — тот же паттерн, что уже применён для вкладок «По падежам»/«По числам»/«Подробно» на стартовой странице квиза склонений (см. `quiz-declension.md` §3.1). Вместо неё статус кодируется цветом `ProgressBar` в колонке «Изучено»:

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

## 2.1а. NumberAggregationTable (вкладка «По числам»)

Зеркальная копия `CaseAggregationTable` (см. таблицу вкладок выше), но группировка — по `numberType` вместо `caseType`:
- Источник данных и агрегация — тот же `lesson.questions: GrammarQuestionProgress[]`, новая функция `aggregateByNumber(questions)` в `utils/grammarAggregation.ts` (по аналогии с существующей `aggregateByCase`), константа `NUMBER_TYPES = ['SINGULAR', 'DUAL', 'PLURAL']` (фиксированный порядок строк, аналогично `CASE_TYPES`).
- Колонки: **Число** (`numberRu`/`numberEn` по локали), **Изучено** (`ProgressBar` + процент, цвет по статусу — см. §2 выше), кнопка запуска квиза.
- Строка таблицы отсутствует, если для данного `numberType` нет вопросов в уроке (как и в `CaseAggregationTable` для падежа без вопросов).
- Клик по строке/кнопке запускает квиз с фильтром: `POST /quiz/{slug}/sessions/start-or-resume?...&filterScope=NUMBER_ONLY&filterNumberTypes=<numberType>` (те же `filterScope`-контракты, что уже реализованы на бэкенде для `CASE_ONLY`, см. `quiz-declension.md` §3.4 — дополнительных изменений на бэкенде не требуется, `NUMBER_ONLY` уже поддержан).



## 2.1. LessonStatsTab (вкладка «Статистика»)

Заменяет `LessonStatsBadges` только на `GrammarLessonPage` — в шапке урока больше не отображается. `VocabularyLessonPage` продолжает использовать `LessonStatsBadges` в шапке без изменений (см. lesson-pages-spec.md §2.1); унификация вынесена в открытые вопросы (lesson-pages-spec.md §9).

Строится из того же `statusSummary: LessonStatusSummary`, что и `LessonStatsBadges` (см. lesson-pages-spec.md §7), отдельного запроса не требует. Строки расположены вертикально, каждая — название, значение (кроме «Всего» — со знаменателем `total`), и кнопка запуска/резюме квиза (кроме «Всего»):

| Строка | Значение | Кнопка | Клик запускает/резюмирует квиз |
|---|---|---|---|
| Всего | `{statusSummary.total}` | — | — |
| Не изучено | `{statusSummary.newCount}` | «Изучить» | `statusFilter=NEW` |
| В процессе | `{statusSummary.learning}` | «Продолжить» | `statusFilter=LEARNING` |
| Изучено | `{statusSummary.mastered}` | «Повторить» | `statusFilter=REVIEW` (доступно при `reviewDue > 0`, см. ADR-007 «Обновление 2026-07») |

**Поведение:** идентично `LessonStatsBadges` — клик по кнопке вызывает `POST /quiz/{slug}/sessions/start-or-resume?...&statusFilter=<NEW|LEARNING|REVIEW>` и переходит на `/quiz/grammar/:type`, квиз стартует или резюмируется в зависимости от наличия IN_PROGRESS-сессии с тем же `statusFilter`. Кнопка строки с нулевым значением (`newCount === 0`, `learning === 0`, либо для «Изучено» — `reviewDue === 0`) недоступна (`disabled`), строка остаётся видимой.

> ⚠️ **`statusFilter` не реализован на бэкенде** (расхождение контракт↔реализация, зафиксировано Агентом 6 — см. [quiz-generator-spec.md §3](../../../services/quiz-service/quiz-generator-spec.md), предупреждение, и §7 п.5). Кнопки этой вкладки визуально готовы, но до реализации Агентом 2 не выполняют полезного действия (бэкенд игнорирует параметр).

## 2.2. GrammarParadigmTable (вкладка «Парадигмы»)

Справочная таблица словоформ — классическая грамматическая парадигма (падеж × число), в отличие от §2.1/«По падежам»/«Подробно» не про прогресс пользователя, а про содержание урока: какие формы вообще есть и как они выглядят.

**Источник данных и навигация (карусель, не всё сразу).** Урок обычно содержит несколько стемов-примеров (слов) одного грамматического типа — грузить формы всех сразу не нужно и вредно (лишний трафик, пользователю нужен один пример за раз). Компонент хранит локальный `currentIndex` (React `useState`, старт — 0, не персистится между заходами на вкладку) и на каждое его изменение делает **новый** запрос — `GET /api/v1/content/public/lessons/{slug}/declension-paradigms?index={currentIndex}` → `DeclensionParadigmPageDto` (см. `services/content-service.md` §5а, `openapi/content/content-api.yaml`). Хук `useDeclensionParadigm(slug, index)` (React Query, ключ `['declension-paradigm', slug, index]`, данные статичны — без auto-refetch); соседний индекс (`currentIndex ± 1`) можно prefetch-ить в фоне при простое, чтобы клик по стрелке не показывал спиннер — не обязательно для первой версии.

**Элементы вкладки:**
- Над таблицей — панель навигации: стрелка «←», счётчик `{index + 1} / {totalCount}`, стрелка «→». Стрелка «←» задизейблена при `index === 0`, «→» — при `index === totalCount - 1`.
- Одна таблица на экран — парадигма ровно одного стема (`paradigm` из ответа), падеж×число (×подзаголовок с родом текущего стема — `genderRu`/`genderEn`, без отдельной логики «два рода = две таблицы»: раз стемы разных родов урока уже линейно перечислены в общем списке карусели, каждая страница карусели просто показывает *свой* род).
- Заголовок таблицы — `stemIast` (крупно) + `stemDevanagari` мелким шрифтом + перевод (`translationRu`/`translationEn` по локали).
- Строки — 8 падежей (`CASE_TYPES`, см. `utils/grammarAggregation.ts`), в фиксированном порядке.
- Столбцы — `numberType`, присутствующие среди `forms` текущего стема (обычно SINGULAR/DUAL/PLURAL).
- Ячейка — `formIast` (крупно) + `formDevanagari` мелким шрифтом, ищется в `forms` по (`caseType`, `numberType`) текущей строки/столбца; пусто (`—`), если записи нет (неполный сид, см. `content-service.md` §3 про `sanskrit_declensions_enriched`).
- Пока не выполнена миграция `translation_ru/translation_en`/data-fix `stem_devanagari` (задача Агенту 2, `content-service.md` §4/§5а) — эти поля приходят `null`, заголовок таблицы деградирует до одного `stemIast`; это временное, не блокирующее рендер состояние, а не ошибка.

## 2.2а. DeclensionExamplesTab (вкладка «Примеры»)

Отдельная вкладка верхнего уровня (см. таблицу вкладок в §2, позиция #3), не часть `GrammarParadigmTable` (§2.2) и не переключатель внутри неё — независимый компонент, без карусели.

**Данные загружаются одним запросом на весь урок, без постраничной навигации** (в отличие от §2.2 — эндпоинт адресуется только по `slug`, без `index`, потому что примеры группируются по словоизменительному классу `(vowelType, gender)` урока в целом, а не по конкретному стему-шагу карусели «Парадигмы», см. `services/content-service/declension-examples.md`). Запрос — `GET /content/public/lessons/{slug}/examples` → `DeclensionExamplesResponseDto` (см. `services/content-service/declension-examples.md`, `openapi/content/content-api.yaml`). Хук `useDeclensionExamples(slug)` (React Query, ключ `['declension-examples', slug]`, без auto-refetch — данные статичны); запрос делается лениво — только пока вкладка «Примеры» активна (`enabled: activeTab === 'examples'`), переключение на другую вкладку и обратно не шлёт новый запрос повторно (React Query кеш по тому же ключу).

**Элементы вкладки:**
- Заголовок вкладки — название урока/класса (`titleRu`/`titleEn` урока, тот же источник, что заголовок страницы урока) — без указания конкретного стема, т.к. ответ `DeclensionExamplesResponseDto` относится ко всему словоизменительному классу урока, а не к одному стему (`groups[]` содержит только `caseType`/`numberType`/`examples`, без `stem`).
- Список групп в порядке падежей `CASE_TYPES`, внутри падежа — по числам `NUMBER_TYPES` (`utils/grammarAggregation.ts`); заголовок группы — `{caseRu}, {numberRu}` (или `caseEn`/`numberEn` по локали). Внутри группы — карточка на каждый пример из `examples[]`: `textIast` крупно, `textDevanagari` мелким шрифтом под ним, `translationRu`/`translationEn` по локали, внизу мелкая атрибуция `«{workTitleRu}, {chapterTitleRu}, стих {verseOrderIndex}»` (аналогично `workTitleEn`/`chapterTitleEn`).
- Группа с пустым `examples[]` не рендерится вовсе (не показывать «нет примеров» отдельным блоком на каждую из ~14 ячеек — визуальный шум; просто пропустить). Если у ответа вообще нет ни одной непустой группы — один общий текст-заглушка на всю вкладку («Примеров пока нет», без иконки-ошибки — ожидаемое состояние для редких/местоимённых классов, не сбой).

**Что не делает эта вкладка:** карточки не кликабельны — примеры не запускают квиз (в отличие от ячеек таблицы §2.2), это чисто справочная иллюстрация; ссылок на страницу стиха (`VersePage`) в первой версии тоже нет (открытый вопрос ниже).

### Открытые вопросы (для Агента 3 при реализации)

- Клик по карточке примера → переход на `VersePage` конкретного стиха (`verseId` есть в каждом примере) — полезно, но не специфицировано детально (какой роут, открывается ли на стихе с подсветкой нужного слова); не блокирует первую версию, можно отложить.
- Источник `totalCount` для панели навигации этой вкладки (см. выше) — решает Агент 3, единственное требование — не делать отдельный полновесный запрос ради одного числа, если оно уже есть в кэше от вкладки «Парадигмы».



**Клик по ячейке** (когда есть форма) — запускает или резюмирует квиз, отфильтрованный именно на эту комбинацию: `POST /quiz/{slug}/sessions/start-or-resume?...&filterScope=CASE_NUMBER_GENDER&filterCombinations=<caseType>:<numberType>:<gender>` (тот же контракт, что уже используют `filterScope`-фильтры declension-квиза, см. quiz-declension.md §3.4 — в отличие от `statusFilter`, эта ветка **реализована** на бэкенде, доп. работы у Агента 2 не требуется), затем переход на `/quiz/grammar/:type`.