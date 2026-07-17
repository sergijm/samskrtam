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
| 3 | По падежам | `CaseAggregationTable` | агрегация вопросов по падежу |
| 4 | Подробно | `GrammarDetailsTable` | таблица вопросов, см. ниже |

**Таблица вопросов (вкладка «Подробно», `DataTable`):**

| Колонка | Содержимое |
|---|---|
| Статус | та же иконка что в VocabularyLessonPage |
| Вопрос | `textRu` / `textEn` по локали |
| Правильный ответ | текст правильного варианта (всегда виден) |
| Попытки | кликабельный `{nSuccess}/{nAll}` |

**Клик на `{nSuccess}/{nAll}`** → открывает `QuestionHistoryDialog` — аналог WordHistoryDialog для грамматических вопросов.

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

**Источник данных:** тот же `lesson.questions: GrammarQuestionProgress[]`, что и у `GrammarDetailsTable` (см. lesson-pages-spec.md §7) — отдельного запроса не требует. Поле `caseEnding` (уже опционально присутствует в типе) используется как содержимое ячейки; если для конкретной комбинации `caseEnding` не задан — отображается `correctAnswerRu`/`correctAnswerEn` по локали.

**Построение таблицы:**
- Строки — 8 падежей (`CASE_TYPES`, см. `utils/grammarAggregation.ts`), в фиксированном порядке.
- Столбцы — значения `numberType`, встречающиеся в `lesson.questions` (обычно SINGULAR/DUAL/PLURAL; набор варьируется по уроку).
- Если урок покрывает несколько родов одновременно (ADR-004 «уроки с двумя родами») — на вкладке рендерится отдельная таблица на каждый род, с подзаголовком (`genderRu`/`genderEn`), а не общая таблица с примешиванием рода в ячейку.
- Ячейка пуста (`—`), если для данной комбинации падеж×число(×род) в уроке нет вопроса (не все комбинации обязаны присутствовать в каждом уроке).

**Клик по ячейке** (когда есть форма) — запускает или резюмирует квиз, отфильтрованный именно на эту комбинацию: `POST /quiz/{slug}/sessions/start-or-resume?...&filterScope=CASE_NUMBER_GENDER&filterCombinations=<caseType>:<numberType>:<gender>` (тот же контракт, что уже используют `filterScope`-фильтры declension-квиза, см. quiz-declension.md §3.4 — в отличие от `statusFilter`, эта ветка **реализована** на бэкенде, доп. работы у Агента 2 не требуется), затем переход на `/quiz/grammar/:type`.
