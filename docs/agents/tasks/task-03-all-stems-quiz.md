# Задача Агенту 3 (Frontend) — «Все основы»

> Поставлена Агентом 0 (Оркестратор) по шаблону `docs/task-templates/new-feature.md`.
> Полная спецификация страницы — `docs/frontend/pages/grammar-all-stems-page.md`, прочитать целиком
> перед реализацией. Контракт API — `docs/services/quiz-service/quiz-declension.md` §5.
> **Зависит от Агента 2** (см. `task-02-all-stems-quiz.md`) — эндпоинт `POST
> /quiz/declensions-all/sessions/start-or-resume` с `filterScope=ALL_STEMS` должен быть развёрнут хотя бы
> на dev-стенде до интеграционной проверки; вёрстку и состояние фильтров можно делать параллельно.

---

## Описание задачи

**Что:** новый пункт меню «Грамматика → Существительные → Все основы» и страница
`/lessons/grammar/declensions-all` с тремя мульти-select фильтрами (гласные/числа/роды) и кнопкой
«Начать квиз», без таблиц парадигм и без вкладок прогресса.

**Зачем:** пользователь хочет тренировать склонение по всем основам сразу, одной кнопкой.

## Контекст

**Milestone:** уточнить у Агента 0 при следующей синхронизации.
**Затронутые сервисы:** frontend (без изменений на бэкенде со стороны Агента 3).
**Инициатор:** прямой запрос пользователя.

## Входные данные

- [x] Спецификация страницы — `docs/frontend/pages/grammar-all-stems-page.md`.
- [x] Пункт в дереве курикулума описан — `docs/frontend/information-architecture/01-curriculum-vs-catalog.md`
      §2.1.1.
- [x] API-контракт — `openapi/quiz/quiz-sessions.yaml`, `openapi/quiz/parameters.yaml`
      (`FilterVowelTypesParam`, `FilterGendersParam`).

## Что нужно сделать (пошагово)

1. **`frontend/src/config/curriculumTree.ts`:** добавить узел `2.1.1.2` в массив `children` узла
   `2.1.1` (после `2.1.1.1`, перед `2.1.1.3` — порядок как в
   `01-curriculum-vs-catalog.md`): `titleKey: 'nouns.all_stems'`, `status: 'available'`,
   `route: '/lessons/grammar/declensions-all'`.
2. **Локализация:** добавить ключ `nouns.all_stems` в оба файла переводов (там же, где уже лежат
   `nouns.a_masc`, `nouns.aa_fem` и т.д.) — RU «Все основы», EN «All stems».
3. **Роутинг:** зарегистрировать маршрут `/lessons/grammar/declensions-all` — либо новый компонент
   `GrammarAllStemsPage`, либо (если роутинг сейчас общий для всех `/lessons/grammar/:type` и разводит
   по типу урока внутри одного компонента) — добавить ветвление по `slug === 'declensions-all'` в
   существующем компоненте, рендерящее упрощённую разметку вместо `GrammarLessonPage`. Выбор конкретного
   способа — на усмотрение Агента 3, критерий — не тянуть `TabView`/`DataTable`-инфраструктуру
   `GrammarLessonPage` туда, где она не нужна (см. §5 «Что сознательно не входит»
   в `grammar-all-stems-page.md`).
4. **Компонент фильтров:** три `MultiSelect` (или эквивалент, см. §3
   `grammar-all-stems-page.md`) с независимым `useState` каждый:
   - `selectedVowelTypes: string[]` — опции A_STEM/AA_STEM/I_STEM/II_STEM/U_STEM/UU_STEM/R_STEM,
     локализованные лейблы переиспользовать из существующих ключей уроков (см. §3
     `grammar-all-stems-page.md`).
   - `selectedNumberTypes: string[]` — опции SINGULAR/DUAL/PLURAL, лейблы из
     `utils/grammarAggregation.ts` (`NUMBER_TYPES`).
   - `selectedGenders: string[]` — опции MASCULINE/FEMININE/NEUTER/UNSPECIFIED, лейблы из того же
     справочника, что уже используют `CASE_COMBINATION`-опции (`genderRu`/`genderEn`).
5. **Кнопка «Начать квиз»:** без счётчика (см. §4 `grammar-all-stems-page.md`). По клику — вызов
   `POST /quiz/declensions-all/sessions/start-or-resume` с query-параметрами `filterScope=ALL_STEMS`,
   `filterVowelTypes`, `filterGenders`, `filterNumberTypes` (пустой массив → параметр не передавать вовсе
   или передавать пустой список — уточнить по факту поведения существующих `CASE_ONLY`/`NUMBER_ONLY`
   вызовов на фронте и повторить тот же паттерн сериализации, не изобретать новый).
6. **Обработка ошибок:** `SCOPE_FILTER_EMPTY` → `Toast`/inline-сообщение (см. §4 п.4
   `grammar-all-stems-page.md`), форма остаётся на экране.
7. **Успешный старт/резюм:** переход на `/quiz/grammar/declensions-all`, рендер вопросов — существующий
   компонент квиза склонений без изменений (`slug` берётся из ответа API как обычно).

## Критерии готовности (DoD)

- [ ] Реализация соответствует `docs/frontend/pages/grammar-all-stems-page.md`, включая раздел §5 «Что
      сознательно не входит» (важно не перенести туда лишние компоненты).
- [ ] Пункт меню «Все основы» отображается под «Существительные» и ведёт на нужный маршрут.
- [ ] Три независимых мульти-фильтра работают, пустой выбор = «все значения» (проверяется по факту, что
      бэкенд получает соответствующий сериализованный запрос — см. acceptance criteria в
      `grammar-all-stems-page.md` §6).
- [ ] Клик «Начать квиз» стартует/резюмирует сессию, ошибка `SCOPE_FILTER_EMPTY` показывается
      пользователю без падения страницы.
- [ ] i18n: строки не хардкожены, оба языка (ru/en) заполнены.
- [ ] PR прошёл CI (lint/build) + один code review.

## Дополнительно

- Не переиспользовать `GrammarParadigmTable`/`CaseAggregationTable`/`NumberAggregationTable`/
  `GrammarDetailsTable`/`LessonStatsTab` на этой странице — они рассчитаны на прогресс по одному
  `lessonId`, здесь урок виртуальный (см. `quiz-declension.md` §5.6).
- Если бэкенд Агента 2 ещё не готов на момент вёрстки — делать фильтры и состояние независимо, с mock
  вызовом `start-or-resume`, интеграцию проверить после готовности `task-02-all-stems-quiz.md`.
