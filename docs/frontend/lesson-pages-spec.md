# Lesson Pages — VocabularyLessonPage и GrammarLessonPage

> Связанные файлы: [frontend-overview.md](../../../../Users/sm/AppData/Local/Temp/frontend-overview.md) · [content-service.md](../services/content-service.md) · [quiz-service.md](../services/quiz-service.md) · [statistics-service.md](../services/statistics-service.md)
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
- `ProgressBar` — процент изученных слов (слово считается изученным при `successRate >= 80%` минимум за 3 попытки)
- Кнопка **«Начать квиз»** → `/quiz/vocabulary/:slug`

**Таблица слов (`DataTable`):**

| Колонка | Содержимое |
|---|---|
| Статус | иконка: `pi-circle` (не начато) / `pi-spin pi-spinner` (в процессе) / `pi-check-circle` (изучено) |
| Слово | `word` в IAST + `wordDevanagari` мелким шрифтом |
| Перевод | `translationRu` или `translationEn` по локали |
| Попытки | кликабельный текст `{nSuccess}/{nAll}` |

**Правила статуса слова:**
- **Не начато** — `nAll = 0`
- **В процессе** — `nAll > 0` и `successRate < 80%`
- **Изучено** — `nAll >= 3` и `successRate >= 80%`

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
        ├── LessonHeader.tsx          ← общая шапка с прогрессом и кнопкой квиза
        ├── WordStatusIcon.tsx        ← иконка статуса слова/вопроса
        ├── WordHistoryDialog.tsx     ← история ответов на слово
        └── QuestionHistoryDialog.tsx ← история ответов на вопрос
```

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
  successRate:  number;   // 0-100
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
  successRate:    number;
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

export interface VocabularyLesson {
  quizId:           string;
  slug:             string;
  titleRu:          string;
  titleEn:          string;
  difficulty:       string;
  totalWords:       number;
  learnedWords:     number;
  progressPercent:  number;
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
- [ ] Прогресс-бар отражает реальный процент изученных слов/вопросов
- [ ] Иконки статуса корректно отображаются для всех трёх состояний
- [ ] Клик на `{nSuccess}/{nAll}` открывает диалог с историей
- [ ] История фильтруется по `quizId` — слово из другого урока не попадает в историю
- [ ] Кнопка «Начать квиз» ведёт на QuizPage и стартует сессию
- [ ] Таблица поддерживает пагинацию при большом количестве слов/вопросов
- [ ] Правильный ответ в GrammarLessonPage всегда виден (не скрыт)
- [ ] Страницы корректно отображаются при `nAll = 0` (нет истории)

---

## 9. Открытые вопросы

- [ ] Нужна ли сортировка в таблице (по статусу, по алфавиту)?
- [ ] Показывать ли слова/вопросы которые не вошли в последнюю сессию (если `questionsPerSession < totalWords`)?
- [ ] Нужна ли кнопка «Повторить только ошибки»?

