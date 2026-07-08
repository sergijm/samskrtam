# Lesson Pages — VocabularyLessonPage и GrammarLessonPage

> Связанные файлы: [frontend-overview.md](../frontend-overview.md) · [content-service.md](../../services/content-service.md) · [quiz-service.md](../../services/quiz-service.md) · [statistics-service.md](../../services/statistics-service.md)
> Status: **DRAFT**

---

## 1. Концепция

Вместо прямого старта квиза из QuizListPage пользователь сначала попадает на страницу урока — **LessonPage**. Там он видит содержание урока, свой прогресс по каждому элементу и запускает квиз осознанно.

**Изменение флоу:**
```
Было:  QuizListPage → [клик] → QuizPage (квиз стартует сразу)
Стало: QuizListPage → [клик] → LessonPage → [кнопка Начать квиз] → QuizPage
```

Два типа страниц урока:

| Страница | Путь | Для квизов |
|---|---|---|
| `VocabularyLessonPage` | `/lessons/vocabulary/:slug` | `VOCABULARY` |
| `GrammarLessonPage` | `/lessons/grammar/:type` | `DECLENSIONS`, `CONJUGATIONS` |

---

## 2. VocabularyLessonPage (`/lessons/vocabulary/:slug`)

### Назначение
Показывает список слов словарного урока с индивидуальной статистикой по каждому слову и общим прогрессом.

### Элементы страницы

**Шапка урока:**
- Название урока (`titleRu` / `titleEn`)
- Сложность (бейдж)
- Кнопка **«Начать квиз»** → `/quiz/vocabulary/:slug`

**LessonStatsBadges (отдельный компонент, подключается над `LessonHeader`):** заменяет прежний `ProgressBar` в шапке — три кликабельных бейджа, см. §2.1.

### 2.1. LessonStatsBadges

Общий компонент для `VocabularyLessonPage` и `GrammarLessonPage`, подключается сверху страницы урока (над `LessonHeader`, вместо удалённого `ProgressBar`). Строится из `statusSummary: LessonStatusSummary` (см. §7) — отдельного запроса не требует, дублирует агрегаты, уже отдаваемые в `VocabularyLesson`/`GrammarLesson`.

**Бейджи:**

| Бейдж | Значение | Клик запускает/резюмирует квиз |
|---|---|---|
| Изучено | `{statusSummary.mastered}/{statusSummary.total}` | `statusFilter=REVIEW` — сессия по MASTERED-элементам в стадии review (`reviewDue > 0`; см. ADR-007 «Обновление 2026-07») |
| Новые | `{statusSummary.newCount}` | `statusFilter=NEW` — сессия по неизученным элементам |
| В процессе | `{statusSummary.learning}` | `statusFilter=LEARNING` — сессия по начатым, но не изученным элементам |

**Поведение:**
- Каждый клик вызывает `POST /quiz/{slug}/sessions/start-or-resume?lessonId=...&statusFilter=<NEW|LEARNING|REVIEW>` (см. quiz-generator-spec.md §3/§4, ../openapi/quiz/parameters.yaml `StatusFilterParam`) и переходит на `/quiz/vocabulary/:slug` (или `/quiz/grammar/:type`) — квиз стартует или продолжается (resume) в зависимости от наличия IN_PROGRESS-сессии с тем же `statusFilter`.
- Бейдж с нулевым значением (`total === 0`, `newCount === 0`, `learning === 0`, либо для «Изучено» — `reviewDue === 0`) недоступен для клика (`disabled`), но остаётся видимым.
- Заменяет прежний placeholder `navigate('/quiz/vocabulary/:slug?filterScope=REVIEW_DUE')` из `VocabularyLessonPage`/`GrammarLessonPage` — параметр `filterScope` для этой цели **не используется** (он занят под фильтр падеж/число/род в `GrammarLessonPage`, см. quiz-declension.md §3.4); корректный параметр — `statusFilter`.
- Кнопка «Повторить» рядом с «Начать квиз» (см. §1) становится избыточной и удаляется — её функцию берёт на себя бейдж «Изучено».

**Таблица слов (`DataTable`):**

| Колонка | Содержимое |
|---|---|
| Статус | иконка: `pi-circle` (не начато) / `pi-spin pi-spinner` (в процессе) / `pi-check-circle` (изучено) |
| Слово | `word` в IAST + `wordDevanagari` мелким шрифтом |
| Перевод | `translationRu` или `translationEn` по локали |
| Попытки | кликабельный текст `{nSuccess}/{nAll}` |

**Правила статуса слова (модель ADR-007, `quiz.quiz_item_score`, не successRate):**
- **NEW** — нет строки `quiz_item_score` для `(userId, itemType, externalRefId)`
- **LEARNING** — есть строка, `score < 90`
- **MASTERED** — `score >= 90`
- **REVIEW** (частный случай MASTERED, см. ADR-007 «Обновление 2026-07») — `score >= 90` и `nextReviewAt <= now`; отображается вместо MASTERED, включает кнопку «Повторить»

`nSuccess`/`nAll`/`successRate` в таблице попыток остаются (on-the-fly агрегация по `quiz_answers`, не влияют на статус) — используются только в колонке «Попытки» и `WordHistoryDialog`.

**LessonStatusSummary (шапка урока, под `LessonHeader`):** счётчики TOTAL / NEW / LEARNING / MASTERED (REVIEW складывается в MASTERED в этой сводке, но именно наличие REVIEW>0 включает кнопку «Повторить» рядом с «Начать квиз» — целевой запуск сессии с приоритетом due-элементов). Считается на бэкенде, отдаётся полем `statusSummary` в `VocabularyLesson`/`GrammarLesson` (см. §7).

**Клик на `{nSuccess}/{nAll}`** → открывает `WordHistoryDialog` (диалог, не отдельная страница).

### WordHistoryDialog

`<Dialog>` с историей ответов на данное слово в данном уроке.

Содержит:
- Заголовок: «История: {word}»
- Таблица попыток: дата · правильный ответ · ответ пользователя · ✓/✗
- Пагинация если попыток > 10

> **Важно:** история фильтруется по `quizId` (урок), потому что одно слово может встречаться в разных уроках с разными контекстами.

---

## 3. GrammarLessonPage (`/lessons/grammar/:type`)

### Назначение
Показывает список грамматических вопросов урока с правильными ответами и индивидуальной статистикой.

### Элементы страницы

**Шапка урока:** аналогична VocabularyLessonPage.

**Таблица вопросов (`DataTable`):**

| Колонка | Содержимое |
|---|---|
| Статус | та же иконка что в VocabularyLessonPage |
| Вопрос | `textRu` / `textEn` по локали |
| Правильный ответ | текст правильного варианта (всегда виден) |
| Попытки | кликабельный `{nSuccess}/{nAll}` |

**Клик на `{nSuccess}/{nAll}`** → открывает `QuestionHistoryDialog` — аналог WordHistoryDialog для грамматических вопросов.

---

## 4. Изменения в роутинге (frontend-overview.md раздел 3)

Добавить маршруты:

| Path | Компонент | Auth | Role |
|---|---|---|---|
| `/lessons/vocabulary/:slug` | `VocabularyLessonPage` | Да | STUDENT |
| `/lessons/grammar/:type` | `GrammarLessonPage` | Да | STUDENT |

Изменить маршрутизацию из `QuizListPage`:

```typescript
// Было:
<QuizCard href={`/quiz/grammar/${quiz.type}`} />
<QuizCard href={`/quiz/vocabulary/${quiz.slug}`} />

// Стало:
<QuizCard href={`/lessons/grammar/${quiz.type}`} />
<QuizCard href={`/lessons/vocabulary/${quiz.slug}`} />
```

---

## 5. Новые компоненты

```
src/
├── pages/
│   ├── VocabularyLessonPage.tsx
│   └── GrammarLessonPage.tsx
└── components/
    └── lesson/
        ├── LessonStatsBadges.tsx     ← НОВЫЙ: три кликабельных бейджа (см. §2.1), заменяет ProgressBar и LessonStatusSummary
        ├── LessonHeader.tsx          ← общая шапка (без прогресс-бара), с кнопкой «Начать квиз»
        ├── WordStatusIcon.tsx        ← иконка статуса слова/вопроса
        ├── WordHistoryDialog.tsx     ← история ответов на слово
        └── QuestionHistoryDialog.tsx ← история ответов на вопрос
```

`LessonStatusSummary.tsx` и `ProgressBar` из `LessonHeader.tsx` удаляются (их обязанности переходят к `LessonStatsBadges`).

---

## 6. Новые API хуки

```typescript
// hooks/useLessons.ts

// Урок по словарю — содержание + статистика пользователя
export const useVocabularyLesson = (slug: string) =>
  useQuery({
    queryKey: ['lesson', 'vocabulary', slug],
    queryFn: () => lessonApi.getVocabularyLesson(slug),
  });

// Урок по грамматике — содержание + статистика пользователя
export const useGrammarLesson = (type: string) =>
  useQuery({
    queryKey: ['lesson', 'grammar', type],
    queryFn: () => lessonApi.getGrammarLesson(type),
  });

// История ответов на конкретное слово в уроке
export const useWordHistory = (quizId: string, wordId: string) =>
  useQuery({
    queryKey: ['word-history', quizId, wordId],
    queryFn: () => lessonApi.getWordHistory(quizId, wordId),
    enabled: !!wordId,
  });

// История ответов на конкретный вопрос в уроке
export const useQuestionHistory = (quizId: string, questionId: string) =>
  useQuery({
    queryKey: ['question-history', quizId, questionId],
    queryFn: () => lessonApi.getQuestionHistory(quizId, questionId),
    enabled: !!questionId,
  });
```

---

## 7. TypeScript типы

```typescript
// types/lesson.ts

export interface VocabularyWordProgress {
  wordId:       string;
  word:         string;
  wordDevanagari: string | null;
  translationRu: string;
  translationEn: string;
    nSuccess:     number;
  nAll:         number;
  score:        number;   // 0-100, exponential score
  status:       WordStatus;
}

export interface GrammarQuestionProgress {
  questionId:     string;
  textRu:         string;
  textEn:         string;
  correctAnswerRu: string;
  correctAnswerEn: string;
  nSuccess:       number;
  nAll:           number;
  score:          number;
  status:         WordStatus;
  caseType:       string;
  caseRu:         string;
  caseEn:         string;
  numberType:     string;
  numberRu:       string;
  numberEn:       string;
  gender:         string;
  genderRu:       string;
  genderEn:       string;
}

export interface LessonStatusSummary {
  total:        number;
  newCount:     number;   // JSON: "new" — зарезервированное слово в TS/JS
  learning:     number;
  mastered:     number;   // включает REVIEW (score >= 90)
  reviewDue:    number;   // подмножество mastered с nextReviewAt <= now; >0 → показать кнопку "Повторить"
}

export interface VocabularyLesson {
  quizId:           string;
  slug:             string;
  titleRu:          string;
  titleEn:          string;
  difficulty:       string;
  totalWords:       number;
  learnedWords:     number;
  progressPercent:  number;
  statusSummary:    LessonStatusSummary;
  words:            VocabularyWordProgress[];
}

export interface GrammarLesson {
  quizId:           string;
  type:             string;
  titleRu:          string;
  titleEn:          string;
  difficulty:       string;
  totalQuestions:   number;
  learnedQuestions: number;
  progressPercent:  number;
  statusSummary:    LessonStatusSummary;
  questions:        GrammarQuestionProgress[];
}

export interface AnswerHistoryEntry {
  answeredAt:      string;   // ISO datetime
  correctAnswer:   string;
  userAnswer:      string;
  isCorrect:       boolean;
}

export interface WordAnswerHistory {
  wordId:   string;
  word:     string;
  quizId:   string;
  entries:  AnswerHistoryEntry[];
  page:     number;
  total:    number;
}
```

---

## 8. Acceptance Criteria

- [ ] Клик на карточку квиза в QuizListPage ведёт на LessonPage, не на QuizPage
- [ ] `LessonStatsBadges` отображает три бейджа (mastered/total, new, learning) над `LessonHeader`; `ProgressBar` в шапке урока отсутствует
- [ ] Клик по каждому из трёх бейджей запускает или резюмирует квиз с соответствующим `statusFilter` (REVIEW/NEW/LEARNING) и переходит на страницу квиза
- [ ] Бейдж с нулевым значением недоступен для клика
- [ ] Иконки статуса корректно отображаются для всех трёх состояний
- [ ] Клик на `{nSuccess}/{nAll}` открывает диалог с историей
- [ ] История фильтруется по `quizId` — слово из другого урока не попадает в историю
- [ ] Кнопка «Начать квиз» ведёт на QuizPage и стартует сессию без фильтра
- [ ] Таблица поддерживает пагинацию при большом количестве слов/вопросов
- [ ] Правильный ответ в GrammarLessonPage всегда виден (не скрыт)
- [ ] Страницы корректно отображаются при `nAll = 0` (нет истории)

---

## 9. Открытые вопросы

- [ ] Нужна ли сортировка в таблице (по статусу, по алфавиту)?
- [ ] Показывать ли слова/вопросы которые не вошли в последнюю сессию (если `questionsPerSession < totalWords`)?
- [ ] Нужна ли кнопка «Повторить только ошибки»?