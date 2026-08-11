# Задача: frontend — компоненты вопросов для 4 типов declension-квестов

**Что:** Компонент-диспетчер по `answerMode` + 3 недостающих рендерера вопроса
(`SINGLE_CHOICE` уже реализован для текущего квиза — переиспользуется, не переписывать).
**Зачем:** См. `docs/services/curriculum-quest-items.md` §2, `docs/frontend/frontend-state.md` §5а.

## Контекст
**Затронутые сервисы:** frontend
**Зависит от:** backend-эндпоинты `task-curriculum-11`/`task-curriculum-12` (для реального
API), но верстать и тестировать можно на моках — не блокируется бэкендом.

> ⚠️ Перед началом — найти актуальный компонент текущей страницы прохождения квиза
> (роут `/quiz/grammar/:type`, см. `frontend-overview.md` §3) в `frontend/src/pages/`. Его
> точное имя/путь в предоставленных доках не зафиксировано (расхождение документации и кода,
> как и в других местах этого репозитория, см. предупреждения в `grammar-lesson-page.md`) —
> актуальный источник истины — сам код, не додумывать структуру.

## Шаги

1. В `frontend/src/types/quiz.ts` — применить расширение `SessionQuestion`/`AnswerMode`/`MatchingPayload`/`MatchingItem`/`MatchingAnswerPayload`/`AnswerResult` из `docs/frontend/frontend-state.md` §5а (скопировать типы как есть, поправив только импорты под реальную структуру файла).
2. Создать `frontend/src/components/quiz/QuestionRenderer.tsx` — тонкий диспетчер: пропсы `{ question: SessionQuestion; onSubmit: (answer: unknown) => void }`, `switch (question.answerMode)` → рендерит один из 4 компонентов ниже, передавая `question` и `onSubmit`. Никакой бизнес-логики проверки ответа здесь — только выбор компонента.
3. `frontend/src/components/quiz/SingleChoiceQuestion.tsx` — используется для `DECLENSION_FORM_CHOICE` и `CASE_RECOGNITION` (оба `SINGLE_CHOICE`, различий в верстке нет — `question.options` уже готовый список строк для обоих типов, `CASE_RECOGNITION` не требует какого-то отдельного рендера падежа/числа/рода, это просто текст варианта). Если аналогичный компонент уже существует в проекте (текущий квиз наверняка уже рендерит SINGLE_CHOICE-вопросы) — переиспользовать его, только убедиться, что пропсы совместимы с новым `SessionQuestion` (поле `answerMode` добавилось, но `options`/`id`/`text` не меняли форму).
4. `frontend/src/components/quiz/FreeTextQuestion.tsx` — для `DECLENSION_FORM` (`answerMode === 'FREE_TEXT'`): `InputText` (PrimeReact, единый стиль с остальными формами приложения, см. `frontend-conventions.md` §11) + кнопка отправки, `onSubmit(trimmedValue)`; Enter в поле — тоже отправка. Валидация на фронте — только «не пустая строка», сравнение с правильным ответом (регистр/диакритика IAST) — исключительно на бэкенде, фронт не решает, верен ли ответ.
5. `frontend/src/components/quiz/MatchingQuestion.tsx` — для `DECLENSION_MATCH`: два столбца (`question.matching.left` слева, `question.matching.right` справа), выбор пары — клик по элементу слева, затем клик по элементу справа (простейший UX без drag-and-drop в первой версии — drag-and-drop можно добавить позже отдельной задачей, не блокирует эту). Локальное состояние — `Record<string, string>` (`leftId → rightId`), после того как все `left`-элементы сопоставлены — кнопка «Проверить» становится активной и вызывает `onSubmit({ matches: Object.entries(state).map(([leftId, rightId]) => ({ leftId, rightId })) } satisfies MatchingAnswerPayload)`. Уже сопоставленные элементы визуально помечаются (например, приглушаются/получают галочку), повторный клик по уже сопоставленному left-элементу — сбрасывает его пару (даёт переназначить).
6. Во всех 4 компонентах после получения `AnswerResult` (родитель — существующий контейнер сессии) — показ обратной связи: для `MATCHING` подсветить каждую пару зелёным/красным по `result.correctMatches` (сравнить с локальным state), а не просто общим «верно/неверно» на весь вопрос — иначе ученик не поймёт, какую именно пару перепутал.
7. i18n: добавить в `frontend/src/i18n/ru.json` и `en.json` (секция `quiz`, рядом с существующими ключами, см. `frontend-conventions.md` §9) ключи `quiz.matching.checkButton` («Проверить» / «Check»), `quiz.matching.resetHint` («Нажмите ещё раз, чтобы отменить выбор» / «Click again to undo»), `quiz.freeText.placeholder` («Введите словоформу…» / «Type the word form…»).

## Критерии готовности (DoD)
- [ ] Все 4 компонента рендерятся изолированно в Storybook/dev-песочнице (если она есть в проекте) либо покрыты unit-тестом на моковом `SessionQuestion` каждого `answerMode`
- [ ] `MatchingQuestion`: кнопка «Проверить» недоступна, пока не сопоставлены все left-элементы; повторный клик по сопоставленному элементу снимает пару
- [ ] `QuestionRenderer` не содержит логики проверки ответа — только выбор компонента по `answerMode`
- [ ] Существующие вопросы других типов (не declension) продолжают рендериться как раньше — регресс не сломан
