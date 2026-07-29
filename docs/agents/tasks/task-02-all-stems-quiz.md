# Задача Агенту 2 (Backend: Domain Services) — «Все основы»

> Поставлена Агентом 0 (Оркестратор) по шаблону `docs/task-templates/new-feature.md`.
> Контракт — Агент 6, см. `docs/services/quiz-service/quiz-declension.md` §5 (обязательное чтение перед
> реализацией). Не начинать реализацию, не прочитав §5 целиком.

---

## Описание задачи

**Что:** новый виртуальный урок `declensions-all` (slug) и параметр `filterScope=ALL_STEMS`,
позволяющий сгенерировать квиз-сессию по стемам сразу из всех существующих уроков склонения гласных
основ, отфильтрованным по гласной/числу/роду.

**Зачем:** пользователь хочет тренировать все склонения вперемешку, не переключаясь между 5 уроками.

## Контекст

**Milestone:** уточнить у Агента 0 при следующей синхронизации (не привязано к текущему M).
**Затронутые сервисы:** content-service, quiz-service.
**Инициатор:** прямой запрос пользователя.

## Входные данные

- [x] Спецификация в docs/ существует — `services/quiz-service/quiz-declension.md` §5.
- [x] Контракт параметров существует — `openapi/quiz/parameters.yaml`
      (`FilterVowelTypesParam`, `FilterGendersParam`), `openapi/quiz/schemas/session.yaml`
      (`FilterInfo.filterVowelTypes/filterGenders`, `FilterScope` enum += `ALL_STEMS`),
      `openapi/quiz/quiz-sessions.yaml` (оба параметра добавлены в `start`/`start-or-resume`).
- [ ] Миграция Flyway для новой строки `content.quizzes` (`slug=declensions-all`) — не создана,
      сделать в рамках этой задачи.

## Что нужно сделать (пошагово)

1. **content-service, миграция Flyway:** добавить строку в `content.quizzes` —
   `slug = "declensions-all"`, `lessonType = DECLENSIONS`, `titleRu = "Все основы"`,
   `titleEn = "All stems"`, `descriptionRu`/`descriptionEn` — по одному предложению,
   `questionsPerSession` — то же значение по умолчанию, что и у остальных `DECLENSIONS`-уроков
   (10, если не изменялось).
2. **content-service, `QuizScopeFilterService` (или аналог, применяемый в
   `QuestionGenerationService`/`DeclensionQuizGeneratorService` — см. `quiz-declension.md` §3.4,
   абзац про перенос `applyScopeFilter`):** добавить ветку для `filterScope=ALL_STEMS`. Ключевое
   отличие от `CASE_ONLY`/`NUMBER_ONLY`/`CASE_NUMBER_GENDER` — выборка `declension_stems` не
   ограничивается `lesson_id` текущего запрошенного `slug`, а идёт по всей таблице
   `content.declension_stems`, отфильтрованной условием: `vowelType IN (filterVowelTypes или все 7
   значений A_STEM/AA_STEM/I_STEM/II_STEM/U_STEM/UU_STEM/R_STEM, если пусто)` И
   `gender IN (filterGenders или все 4 значения, если пусто)`. Пустой `filterNumberTypes`
   раскрывается в SINGULAR/DUAL/PLURAL как и в `NUMBER_ONLY` (уже реализованная логика, переиспользовать).
   `caseType` не фильтруется — всегда все 8 падежей.
3. **content-service:** убедиться, что генератор question-у ровно так же вызывает
   `DeclensionOptionGeneratorService`/эквивалент для дистракторов, что и для обычных уроков — эта
   часть не меняется, различие только в шаге пре-фильтрации (шаг 2 выше).
4. **quiz-service:** `QuizSession` — новые поля `filterVowelTypes`/`filterGenders` (аналогично уже
   существующим `filterCaseTypes`/`filterNumberTypes`/`filterCombinations`, см. `quiz-declension.md`
   §3.4 — формат хранения множеств выбрать так же, как для существующих полей, для консистентности).
   Прокинуть значения как query-параметры в `ContentClient.generateQuizData(...)`.
5. **quiz-service:** резюм-сравнение — искать `IN_PROGRESS`-сессию с `filterScope=ALL_STEMS` и
   совпадающими множествами `filterVowelTypes`/`filterGenders`/`filterNumberTypes` (без учёта порядка),
   тот же алгоритм, что и для остальных scope.
6. **quiz-service:** если после пре-фильтрации content-service вернул пустой список вопросов — ответить
   `SCOPE_FILTER_EMPTY` (код ошибки уже существует, переиспользовать).
7. Зафиксировать в `quiz-declension.md` §5.2 постфактум, как именно реализована выборка без `lesson_id`
   (одно-два предложения, без кода — по конвенции Агента 6, §9.118-121 `samskrtam-agents-spec.md`).

## Критерии готовности (DoD)

- [ ] Реализация соответствует `quiz-declension.md` §5.
- [ ] Unit-тесты: пре-фильтрация ALL_STEMS (пустой фильтр → полный пул; частичный фильтр → только
      нужные vowelType/gender/numberType; несуществующая комбинация вроде AA_STEM+NEUTER → пустой
      результат, не exception).
- [ ] Интеграционный тест: `POST /quiz/declensions-all/sessions/start-or-resume` → 200, вопросы из
      разных vowelType присутствуют при пустом фильтре.
- [ ] Интеграционный тест: повторный вызов с тем же набором фильтров резюмирует ту же сессию.
- [ ] Checkstyle и SpotBugs не падают.
- [ ] OpenAPI уже обновлён Агентом 6 — сверить, что реализация не разошлась с контрактом.
- [ ] `.env.example` — без изменений (новых переменных окружения не требуется).
- [ ] PR прошёл CI + один code review.

## Дополнительно

- Не трогать существующие 5 уроков склонения и их `lesson_id`-scoped выборку — `ALL_STEMS` веткуется
  строго отдельно, без риска регресса остальных `filterScope`.
- Открытый вопрос §5.6 (`quiz-declension.md`) про live-счётчик N — в эту задачу не входит, фронтенд его
  не запрашивает в этой итерации.
