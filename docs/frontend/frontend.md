# Frontend Specification — React + TypeScript

> Модуль: `frontend/`
> Язык: TypeScript 5
> Фреймворк: React 18
> Status: **DRAFT**

---

## 1. Стек

| Технология | Версия | Назначение |
|---|---|---|
| React | 18 | UI фреймворк |
| TypeScript | 5 | Язык |
| Vite | 5 | Сборщик |
| React Router | 6 | Маршрутизация |
| React Query (TanStack) | 5 | Server state, кэширование |
| Zustand | 4 | Client state (auth, locale) |
| i18next + react-i18next | latest | Интернационализация ru/en |
| PrimeReact ThemeProvider | — | Переключение светлой/тёмной темы |
| Axios | latest | HTTP клиент |
| **PrimeReact** | **10.x** | **UI компоненты** |
| **PrimeFlex** | **3.x** | **CSS утилиты (grid, spacing)** |
| **PrimeIcons** | **6.x** | **Иконки** |

### Почему PrimeReact

PrimeReact — React-версия PrimeFaces от той же компании (PrimeTek). API и набор компонентов намеренно похожи, что даёт знакомый опыт разработки.

### Маппинг компонентов PrimeFaces → PrimeReact

| PrimeFaces | PrimeReact | Где используется |
|---|---|---|
| `<p:dataTable>` | `<DataTable>` | Лидерборд, история сессий, админка |
| `<p:card>` | `<Card>` | QuizCard, WordCard, ScoreSummary |
| `<p:steps>` | `<Steps>` | Прогресс квиза (1/10 ... 10/10) |
| `<p:tabView>` | `<TabPanel>` | AdminPage вкладки |
| `<p:autoComplete>` | `<AutoComplete>` | Поиск в словаре |
| `<p:dialog>` | `<Dialog>` | Создание квиза в админке |
| `<p:button>` | `<Button>` | Везде |
| `<p:progressBar>` | `<ProgressBar>` | Прогресс сессии |
| `<p:chart>` | `<Chart>` | Тепловая карта ошибок (Chart.js) |

### Тема

PrimeReact поддерживает динамическую смену темы через `<link id="theme-link">` без перезагрузки страницы. Используются две темы из стандартного набора:

| Режим | Тема PrimeReact |
|---|---|
| Светлая (по умолчанию) | `lara-light-blue` |
| Тёмная | `lara-dark-blue` |

Вместо статического `import` в `main.tsx` тема подключается динамически — `themeStore` меняет `href` у тега `<link>`:

```typescript
// main.tsx — только базовые стили, без темы
import 'primereact/resources/primereact.min.css';
import 'primeicons/primeicons.css';
import 'primeflex/primeflex.css';
// Тема подключается динамически через themeStore при монтировании App
```

```html
<!-- index.html — placeholder для динамической темы -->
<link id="theme-link" rel="stylesheet" href="/themes/lara-light-blue/theme.css" />
```

---

## 2. Структура проекта

```
frontend/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
│
└── src/
    ├── main.tsx                    ← точка входа
    ├── App.tsx                     ← роутер + провайдеры
    │
    ├── pages/                      ← страницы (один файл = один роут)
    │   ├── LoginPage.tsx
    │   ├── RegisterPage.tsx
    │   ├── ForgotPasswordPage.tsx
    │   ├── AuthCallbackPage.tsx
    │   ├── ChangePasswordPage.tsx  ← спецификация в user-frontend.md
    │   ├── SettingsPage.tsx        ← спецификация в user-frontend.md
    │   ├── UserProfilePage.tsx     ← спецификация в user-frontend.md
    │   ├── GroupListPage.tsx       ← спецификация в user-frontend.md
    │   ├── GroupPage.tsx           ← спецификация в user-frontend.md
    │   ├── GroupCreatePage.tsx     ← спецификация в user-frontend.md
    │   ├── GroupEditPage.tsx       ← спецификация в user-frontend.md
    │   ├── DashboardPage.tsx
    │   ├── QuizListPage.tsx
    │   ├── QuizPage.tsx
    │   ├── ResultPage.tsx
    │   ├── DictionaryPage.tsx
    │   ├── StatisticsPage.tsx
    │   ├── LeaderboardPage.tsx
    │   └── AdminPage.tsx
    │
    ├── components/                 ← переиспользуемые компоненты
    │   ├── layout/
    │   │   ├── AppLayout.tsx       ← шапка + навигация + контент
    │   │   ├── Header.tsx
    │   │   └── Sidebar.tsx
    │   ├── auth/
    │   │   └── ProtectedRoute.tsx  ← HOC для защищённых роутов
    │   ├── quiz/
    │   │   ├── QuizCard.tsx        ← карточка квиза в списке
    │   │   ├── QuestionCard.tsx    ← вопрос + варианты ответа
    │   │   ├── OptionButton.tsx    ← кнопка варианта ответа
    │   │   ├── FeedbackPanel.tsx   ← правильно/нет + объяснение
    │   │   └── ProgressBar.tsx     ← прогресс 3/10
    │   ├── statistics/
    │   │   ├── ScoreSummary.tsx    ← итог сессии
    │   │   ├── AnswerReview.tsx    ← разбор вопросов
    │   │   ├── HeatmapChart.tsx    ← тепловая карта ошибок
    │   │   └── LeaderboardTable.tsx
    │   ├── dictionary/
    │   │   ├── WordCard.tsx        ← статья словаря
    │   │   └── SearchInput.tsx     ← поиск с автодополнением
    │   └── common/
    │       ├── LocaleSwitcher.tsx  ← переключатель ru/en
    │       ├── ThemeSwitcher.tsx   ← переключатель светлая/тёмная
    │       ├── LoadingSpinner.tsx
    │       └── ErrorMessage.tsx
    │   ├── user/                   ← компоненты профиля (user-frontend.md)
    │   │   ├── UserGroupChips.tsx
    │   │   └── UserAvatar.tsx
    │   └── group/                  ← компоненты групп (user-frontend.md)
    │       ├── GroupMembersTable.tsx
    │       ├── GroupCuratorBadge.tsx
    │       └── AddMemberDialog.tsx
    │
    ├── api/                        ← HTTP клиенты по доменам
    │   ├── axios.ts                ← настройка axios + interceptors
    │   ├── authApi.ts
    │   ├── userApi.ts              ← пользователи и группы (user-frontend.md)
    │   ├── quizApi.ts
    │   ├── dictionaryApi.ts
    │   └── statisticsApi.ts
    │
    ├── store/                      ← Zustand stores
    │   ├── authStore.ts            ← токены, текущий пользователь
    │   ├── localeStore.ts          ← текущий язык
    │   └── themeStore.ts           ← текущая тема
    │
    ├── hooks/                      ← React Query хуки
    │   ├── useUser.ts              ← пользователи (user-frontend.md)
    │   ├── useGroups.ts            ← группы (user-frontend.md)
    │   ├── useQuizzes.ts
    │   ├── useQuizSession.ts
    │   ├── useDictionary.ts
    │   └── useStatistics.ts
    │
    ├── types/                      ← TypeScript типы
    │   ├── user.ts                 ← User, Group, GroupMember (user-frontend.md)
    │   ├── quiz.ts
    │   ├── dictionary.ts
    │   └── statistics.ts
    │
    └── i18n/
        ├── index.ts                ← настройка i18next
        ├── ru.json                 ← русские переводы
        └── en.json                 ← английские переводы
```

---

## 3. Роуты

| Path | Компонент | Auth | Role |
|---|---|---|---|
| `/login` | LoginPage | Нет | — |
| `/register` | RegisterPage | Нет | — |
| `/forgot-password` | ForgotPasswordPage | Нет | — |
| `/auth/callback` | AuthCallbackPage | Нет | — |
| `/` | DashboardPage | Да | STUDENT |
| `/quizzes` | QuizListPage | Да | STUDENT |
| `/quiz/grammar/:type` | QuizPage | Да | STUDENT |
| `/quiz/vocabulary/:slug` | QuizPage | Да | STUDENT |
| `/quiz/*/result/:attemptId` | ResultPage | Да | STUDENT |
| `/dictionary` | DictionaryPage | Да | STUDENT |
| `/statistics` | StatisticsPage | Да | STUDENT |
| `/leaderboard` | LeaderboardPage | Да | STUDENT |
| `/settings/password` | ChangePasswordPage | Да | STUDENT, ADMIN |
| `/settings` | SettingsPage | Да | STUDENT, ADMIN |
| `/users/:id` | UserProfilePage | Да | STUDENT, ADMIN |
| `/groups` | GroupListPage | Да | ADMIN |
| `/groups/new` | GroupCreatePage | Да | ADMIN |
| `/groups/:id` | GroupPage | Да | STUDENT, ADMIN |
| `/groups/:id/edit` | GroupEditPage | Да | ADMIN, CURATOR |
| `/admin` | AdminPage | Да | ADMIN |

Роуты `/settings`, `/users/:id`, `/groups/*` — подробная спецификация в [user-frontend.md](user-frontend.md).

---

## 4. Страницы

### LoginPage (`/login`)

**Назначение:** вход в систему.

**Элементы:**
- Логотип / название SamskrtamApp
- Кнопка "Войти через Google"
- Кнопка "Войти через Mail.ru"
- Форма: email + пароль + кнопка "Войти" (локальный аккаунт)
- Переключатель языка (ru/en) — доступен без авторизации
- Переключатель темы (светлая/тёмная) — доступен без авторизации

**Поведение:**
- Форма логин/пароль → POST /api/v1/auth/login → ROPC через user-service
- Кнопка Google → GET /api/v1/auth/oauth2/google → редирект через Keycloak
- Кнопка Mail.ru → GET /api/v1/auth/oauth2/mailru → редирект через Keycloak
- После успешного входа → редирект на `/`
- Ошибка входа → сообщение под формой, без уточнения что именно неверно
- Токены сохраняются в `authStore` (не в localStorage)
- Ссылки: "Зарегистрироваться" → `/register`, "Забыли пароль?" → `/forgot-password`

---

### RegisterPage (`/register`)

**Назначение:** регистрация нового пользователя.

**Элементы:**
- Поле username
- Поле email
- Поле пароль + подтверждение пароля
- Кнопка "Зарегистрироваться"
- Ссылка "Уже есть аккаунт? Войти" → `/login`

**Поведение:**
- POST /api/v1/auth/register → user-service → Keycloak Admin API
- Успех → страница "Проверьте email для подтверждения"
- Ошибка (email занят) → сообщение под полем email

---

### ForgotPasswordPage (`/forgot-password`)

**Назначение:** запрос восстановления пароля.

**Элементы:**
- Поле email
- Кнопка "Отправить письмо"
- Ссылка "Вернуться к входу" → `/login`

**Поведение:**
- POST /api/v1/auth/forgot-password → user-service → Keycloak Email Flow
- Всегда показывает "Если email зарегистрирован — письмо отправлено"
  (не раскрываем существование аккаунта)

---

### AuthCallbackPage (`/auth/callback`)

**Назначение:** обработка возврата от Google/Mail.ru после OAuth2 редиректа.

**Элементы:**
- Только спиннер загрузки — пользователь видит эту страницу долю секунды

**Поведение:**
- Читает `?code=...` из URL
- POST /api/v1/auth/callback { code } → user-service → обменивает на токены
- Успех → сохраняет токены в authStore → редирект на `/`
- Ошибка → редирект на `/login` с сообщением об ошибке

---

### ChangePasswordPage и SettingsPage

Спецификации вынесены в [user-frontend.md](user-frontend.md) (раздел 6).

---

### DashboardPage (`/`)

**Назначение:** главная страница после входа.

**Элементы:**
- Приветствие с именем пользователя
- Карточки доступных квизов (QuizCard × N)
- Блок "Мой прогресс" — краткая статистика (всего попыток, средний %)
- Блок "Лидерборд" — топ-3 участников
- Кнопка "Словарь"

---

### QuizListPage (`/quizzes`)

**Назначение:** список всех доступных квизов, сгруппированных по категориям.

**Группы квизов (v1):**

```
Группа 1 — Грамматика
  ├── Склонения существительных   → /quiz/grammar/declensions
  └── Спряжения глаголов          → /quiz/grammar/conjugations

Группа 2 — Лексика
  ├── Животные                    → /quiz/vocabulary/animals
  ├── Числа                       → /quiz/vocabulary/numbers
  ├── Части тела                  → /quiz/vocabulary/body-parts
  ├── Природа                     → /quiz/vocabulary/nature
  ├── Базовая лексика             → /quiz/vocabulary/1
  └── Средний уровень             → /quiz/vocabulary/2
```

**Элементы:**
- Секция "Грамматика" с QuizCard для каждого грамматического квиза
- Секция "Лексика" с QuizCard для каждого словарного квиза
- Фильтр по сложности (Beginner / Intermediate / Advanced)

**QuizCard содержит:**
- Название квиза (на текущем языке)
- Slug или тип (отображается как бейдж)
- Сложность
- Лучший результат пользователя (если есть)
- Кнопка "Начать"

**Роутинг по группам:**

```typescript
// Грамматические квизы
<QuizCard href={`/quiz/grammar/${quiz.type}`} />

// Словарные квизы — slug может быть текстовым или числовым
<QuizCard href={`/quiz/vocabulary/${quiz.slug}`} />
// например: /quiz/vocabulary/animals
//           /quiz/vocabulary/1
```

---

### QuizPage (`/quiz/grammar/:type` и `/quiz/vocabulary/:slug`)

**Назначение:** прохождение квиза. Один компонент для всех типов квизов —
параметры маршрута определяют какой сервис вызывается.

```typescript
// App.tsx
<Route path="/quiz/grammar/:type"      element={<QuizPage group="grammar" />} />
<Route path="/quiz/vocabulary/:slug"   element={<QuizPage group="vocabulary" />} />

// QuizPage.tsx — определяет endpoint по группе
const apiPath = group === "grammar"
  ? `/api/v1/quiz/${type}/sessions/start`
  : `/api/v1/quiz/vocabulary/${slug}/sessions/start`;
```

**Состояния страницы:**
```
LOADING    → загрузка сессии
QUESTION   → показ вопроса
FEEDBACK   → показ результата ответа
COMPLETED  → редирект на ResultPage
```

**Элементы в состоянии QUESTION:**
- ProgressBar (вопрос 3 из 10)
- Текст вопроса
- 4 кнопки OptionButton
- Нельзя изменить ответ после выбора

**Элементы в состоянии FEEDBACK:**
- Индикатор правильно / неправильно
- Правильный ответ (выделен)
- Объяснение (explanationRu или explanationEn по локали)
- Кнопка "Следующий вопрос"

---

### ResultPage (`/quizzes/:id/result/:attemptId`)

**Назначение:** итоги сессии.

**Элементы:**
- ScoreSummary: score, %, время
- Место в группе: "Ты на 2 месте из 4"
- AnswerReview: список всех вопросов с правильными/неправильными ответами
- Кнопка "Пройти ещё раз"
- Кнопка "На главную"

---

### DictionaryPage (`/dictionary`)

**Назначение:** поиск слов санскрита через Monier-Williams (CSL API).

**Элементы:**
- `SearchInput` — поле ввода слова (латиница, SLP1 транслитерация)
- Кнопка "Найти" — запускает поиск списка
- `SearchResultsList` — горизонтальный кликабельный список найденных слов
- `WordCard` — полная словарная статья выбранного слова
- `LoadingSpinner` — пока идёт запрос к внешнему API

**Двухэтапный флоу:**

```
Этап 1 — поиск списка:
  Пользователь вводит "deva" → нажимает "Найти"
  GET /api/v1/dictionary/search?q=deva
  ↓
  Горизонтальный список: [deva] [devaka] [devī] [devadatta] ...
  каждый элемент кликабелен

Этап 2 — загрузка статьи:
  Пользователь кликает на "deva"
  GET /api/v1/dictionary/entry?key=deva
  ↓
  WordCard с полной статьёй
  (индикатор загрузки если запрос к внешнему API)
```

**WordCard содержит:**
- Слово в IAST транслитерации + деванагари (если есть)
- Грамматические характеристики: часть речи, род, корень глагола
- Список значений
- Бейдж источника: LOCAL / MONIER_WILLIAMS
- Бейдж "из кэша" если слово уже было в БД

**Состояния страницы:**
```
IDLE       → только строка поиска и кнопка
SEARCHING  → спиннер под кнопкой (запрос списка)
LIST       → горизонтальный список слов
LOADING    → спиннер под списком (запрос статьи, внешний API)
ENTRY      → WordCard со статьёй
ERROR      → сообщение об ошибке (API недоступен и т.д.)
```

---

### StatisticsPage (`/statistics`)

**Назначение:** личная статистика.

**Элементы:**
- Общая статистика: всего сессий, средний %, лучший %
- Разбивка по типам квизов (таблица)
- HeatmapChart: тепловая карта ошибок по вопросам
- История последних 20 сессий

---

### LeaderboardPage (`/leaderboard`)

**Назначение:** групповой рейтинг.

**Элементы:**
- LeaderboardTable: место, имя, очки, сессии
- Текущий пользователь выделен
- Время последнего обновления

---

### AdminPage (`/admin`)

**Назначение:** управление контентом.

**Элементы (вкладки):**
- Вкладка "Квизы" — список + создание/редактирование/удаление
- Вкладка "Вопросы" — редактор вопросов выбранного квиза
- Вкладка "Пользователи" — список пользователей + создание
- Вкладка "Группы" — список групп + кнопка создания (детали в [user-frontend.md](user-frontend.md) раздел 7)

---

## 5. TypeScript типы

Типы пользователей и групп (`User`, `Group`, `GroupMember`, `GroupRole`, `AuthTokens`, `Theme`, `Locale`) вынесены в [user-frontend.md](user-frontend.md) раздел 3 (`types/user.ts`).

```typescript
// types/quiz.ts
export interface QuizSummary {
  id:         string;
  titleRu:    string;
  titleEn:    string;
  quizType:   QuizType;
  difficulty: Difficulty;
  bestScore?: number;
}

export interface QuizSession {
  sessionId:  string;
  quizId:     string;
  questions:  SessionQuestion[];
}

export interface SessionQuestion {
  id:      string;
  text:    string;           // уже на нужном языке (локаль из заголовка)
  options: SessionOption[];
}

export interface SessionOption {
  id:   string;
  text: string;
}

export interface AnswerResult {
  isCorrect:       boolean;
  correctOptionId: string;
  explanation:     string;
  questionNumber:  number;
  totalQuestions:  number;
}

export type QuizType   = 'DECLENSIONS' | 'CONJUGATIONS' | 'VOCABULARY';
export type Difficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

// types/dictionary.ts
export interface DictionaryEntry {
  word:           string;
  wordDevanagari: string | null;
  meanings:       string[];
  partOfSpeech:   string | null;
  source:         'LOCAL' | 'SANSKRIT_HERITAGE' | 'MONIER_WILLIAMS';
  cached:         boolean;
}

// types/statistics.ts
export interface LeaderboardEntry {
  rank:          number;
  userId:        string;
  username:      string;
  totalPoints:   number;
  totalSessions: number;
  isCurrentUser: boolean;
}

export interface PersonalStats {
  totalSessions:     number;
  averagePercentage: number;
  bestPercentage:    number;
  byQuizType:        Record<QuizType, QuizTypeStats>;
}
```

---

## 6. API хуки (React Query)

```typescript
// hooks/useQuizzes.ts

// Список грамматических квизов
export const useGrammarQuizzes = () =>
  useQuery({
    queryKey: ['quizzes', 'grammar'],
    queryFn:  () => quizApi.getGrammarQuizzes(),
  });

// Список словарных квизов
export const useVocabularyQuizzes = () =>
  useQuery({
    queryKey: ['quizzes', 'vocabulary'],
    queryFn:  () => quizApi.getVocabularyQuizzes(),
  });

// Старт сессии — path зависит от группы
export const useQuizSession = (group: 'grammar' | 'vocabulary', id: string) =>
  useQuery({
    queryKey: ['quiz-session', group, id],
    queryFn:  () => group === 'grammar'
      ? quizApi.startGrammarSession(id)      // id = type (declensions/conjugations)
      : quizApi.startVocabularySession(id),  // id = slug (animals/1/2)
    staleTime: Infinity,
  });

export const useSubmitAnswer = () =>
  useMutation({
    mutationFn: ({ sessionId, questionId, selectedOptionId }: AnswerPayload) =>
      quizApi.submitAnswer(sessionId, questionId, selectedOptionId),
  });

// hooks/useDictionary.ts

// Шаг 1 — поиск списка слов (запускается по кнопке, не автоматически)
export const useDictionarySearch = () =>
  useMutation({
    mutationFn: (query: string) => dictionaryApi.search(query),
  });

// Шаг 2 — загрузка статьи по ключу (запускается по клику на слово)
export const useDictionaryEntry = (key: string | null) =>
  useQuery({
    queryKey: ['dictionary', 'entry', key],
    queryFn:  () => dictionaryApi.getEntry(key!),
    enabled:  key !== null,
    staleTime: Infinity,    // статья не меняется — кэшируем навсегда
  });
```

---

## 7. Auth Store (Zustand)

```typescript
// store/authStore.ts
interface AuthState {
  user:         User | null;
  accessToken:  string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;

  login:  (tokens: AuthTokens, user: User) => void;
  logout: () => void;
  setAccessToken: (token: string) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user:            null,
  accessToken:     null,
  refreshToken:    null,
  isAuthenticated: false,

  login: (tokens, user) => set({
    ...tokens,
    user,
    isAuthenticated: true,
  }),

  logout: () => set({
    user: null, accessToken: null,
    refreshToken: null, isAuthenticated: false,
  }),

  setAccessToken: (token) => set({ accessToken: token }),
}));
```

---

## 8. Theme Store и Locale Store (Zustand)

```typescript
// store/themeStore.ts
type Theme = 'light' | 'dark';

const THEME_HREFS: Record<Theme, string> = {
  light: '/themes/lara-light-blue/theme.css',
  dark:  '/themes/lara-dark-blue/theme.css',
};

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

export const useThemeStore = create<ThemeState>((set) => ({
  // Начальное значение: из localStorage (для неавторизованных)
  theme: (localStorage.getItem('theme') as Theme) ?? 'light',

  setTheme: (theme) => {
    // 1. Применяем немедленно — меняем href тега <link>
    const link = document.getElementById('theme-link') as HTMLLinkElement;
    if (link) link.href = THEME_HREFS[theme];

    // 2. Синхронизируем <html data-theme="..."> для кастомных CSS переменных
    document.documentElement.setAttribute('data-theme', theme);

    // 3. Сохраняем в localStorage для неавторизованных / до логина
    localStorage.setItem('theme', theme);

    set({ theme });
  },
}));

// store/localeStore.ts
type Locale = 'ru' | 'en';

interface LocaleState {
  locale: Locale;
  setLocale: (locale: Locale) => void;
}

export const useLocaleStore = create<LocaleState>((set) => ({
  locale: (localStorage.getItem('locale') as Locale) ?? 'ru',

  setLocale: (locale) => {
    i18next.changeLanguage(locale);       // применяется немедленно
    localStorage.setItem('locale', locale);
    set({ locale });
  },
}));
```

```typescript
// components/common/ThemeSwitcher.tsx
export const ThemeSwitcher = () => {
  const { theme, setTheme } = useThemeStore();
  const { t } = useTranslation();

  return (
    <div className="flex align-items-center gap-2">
      <i className="pi pi-sun" aria-hidden="true" />
      <InputSwitch
        checked={theme === 'dark'}
        onChange={(e) => setTheme(e.value ? 'dark' : 'light')}
        aria-label={t('settings.toggleTheme')}
      />
      <i className="pi pi-moon" aria-hidden="true" />
    </div>
  );
};

// components/common/LocaleSwitcher.tsx
export const LocaleSwitcher = () => {
  const { locale, setLocale } = useLocaleStore();

  return (
    <SelectButton
      value={locale}
      onChange={(e) => setLocale(e.value)}
      options={[
        { label: 'RU', value: 'ru' },
        { label: 'EN', value: 'en' },
      ]}
      aria-label="Язык / Language"
    />
  );
};
```

`ThemeSwitcher` и `LocaleSwitcher` размещаются в `Header.tsx` (для авторизованных пользователей) и на `LoginPage` (для всех остальных).

---

```typescript
// api/authApi.ts — все вызовы к user-service
export const authApi = {

  // ROPC — логин через форму
  login: (email: string, password: string) =>
    api.post<AuthResponse>('/api/v1/auth/login', { email, password }),

  // OAuth2 редиректы — просто открываем URL в браузере
  loginWithGoogle: () => {
    window.location.href = `${API_URL}/api/v1/auth/oauth2/google`;
  },

  loginWithMailRu: () => {
    window.location.href = `${API_URL}/api/v1/auth/oauth2/mailru`;
  },

  // Обмен authorization code на токены (после редиректа)
  callback: (code: string) =>
    api.post<AuthResponse>('/api/v1/auth/callback', { code }),

  // Регистрация
  register: (username: string, email: string, password: string) =>
    api.post('/api/v1/auth/register', { username, email, password }),

  // Восстановление пароля
  forgotPassword: (email: string) =>
    api.post('/api/v1/auth/forgot-password', { email }),

  // Смена пароля (требует JWT)
  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/api/v1/auth/change-password', { currentPassword, newPassword }),

  // Обновление access token
  refresh: (refreshToken: string) =>
    api.post<{ accessToken: string }>('/api/v1/auth/refresh', { refreshToken }),

  // Logout
  logout: (refreshToken: string) =>
    api.post('/api/v1/auth/logout', { refreshToken }),
};

// api/axios.ts
// Request interceptor — добавляет Bearer токен
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Response interceptor — автоматический refresh при 401
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const { refreshToken } = useAuthStore.getState();
      if (refreshToken) {
        const { data } = await authApi.refresh(refreshToken);
        useAuthStore.getState().setAccessToken(data.accessToken);
        return api.request(error.config);   // повторяем запрос
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 9. i18n структура

```json
// i18n/ru.json
{
  "nav": {
    "dashboard":   "Главная",
    "quizzes":     "Квизы",
    "dictionary":  "Словарь",
    "statistics":  "Статистика",
    "leaderboard": "Рейтинг",
    "settings":    "Настройки",
    "admin":       "Администрирование"
  },
  "settings": {
    "title":         "Настройки",
    "language":      "Язык",
    "theme":         "Тема",
    "themeLight":    "Светлая",
    "themeDark":     "Тёмная",
    "toggleTheme":   "Переключить тему",
    "save":          "Сохранить",
    "saved":         "Настройки сохранены"
  },
  "quiz": {
    "start":       "Начать квиз",
    "next":        "Следующий вопрос",
    "correct":     "Верно!",
    "incorrect":   "Неверно",
    "question":    "Вопрос {{current}} из {{total}}",
    "explanation": "Объяснение"
  },
  "auth": {
    "login":       "Войти",
    "logout":      "Выйти",
    "email":       "Email",
    "password":    "Пароль",
    "google":      "Войти через Google",
    "mailru":      "Войти через Mail.ru",
    "error":       "Неверный email или пароль"
  }
}
```

---

## 10. Переменные окружения

```
# frontend/.env
VITE_API_URL=http://localhost:8090
VITE_KEYCLOAK_URL=http://localhost:8080
VITE_KEYCLOAK_REALM=samskrtam
VITE_KEYCLOAK_CLIENT_ID=samskrtam-frontend
```

---

## 11. Coding conventions

> Все примеры ниже — соглашения для генерации, не готовый код.

- Функциональные компоненты везде, никаких class components
- Пропсы через `interface`, не `type`
- Именование: компоненты `PascalCase`, хуки `useCamelCase`, файлы совпадают с именем компонента
- Каждый компонент в отдельном файле
- `React.FC` не используется — явные типы пропсов
- Нет `any` — `unknown` если тип неизвестен
- React Query для всех серверных данных — никакого `useEffect` + `fetch`

---

## 12. Acceptance Criteria

### Auth
- [ ] Логин через форму → ROPC → JWT в authStore → редирект на /
- [ ] Кнопка Google → редирект → /auth/callback → JWT → редирект на /
- [ ] Кнопка Mail.ru → редирект → /auth/callback → JWT → редирект на /
- [ ] Регистрация → письмо верификации → страница подтверждения
- [ ] Восстановление пароля → всегда показывает одно сообщение (без раскрытия)
- [ ] Смена пароля → требует текущий пароль → успех/ошибка
- [ ] После истечения токена — автоматический refresh без участия пользователя
- [ ] Защищённые роуты недоступны без авторизации → редирект на /login
- [ ] `/admin` недоступен для STUDENT → редирект на /

### Quiz
- [ ] Варианты ответов перемешаны при каждом показе
- [ ] После выбора ответ нельзя изменить
- [ ] Фидбек показывается немедленно после выбора
- [ ] После 10 вопросов — автоматический переход на ResultPage

### Dictionary
- [ ] Поиск запускается по кнопке "Найти" (не автоматически)
- [ ] После поиска — горизонтальный список ранжированных слов
- [ ] Клик на слово → загрузка статьи
- [ ] Если статья уже в локальной БД — ответ мгновенный (без спиннера)
- [ ] Если статья запрашивается впервые — спиннер пока идёт внешний запрос
- [ ] WordCard показывает грамматические характеристики (род, часть речи)

### i18n и тема
- [ ] Переключение языка работает без перезагрузки страницы
- [ ] Переключение темы применяется мгновенно (смена href у #theme-link)
- [ ] Все тексты через i18next, нет захардкоженных строк
- [ ] После логина тема и язык восстанавливаются из профиля

Полные критерии по настройкам, профилю и группам — в [user-frontend.md](user-frontend.md) раздел 9.

---

## 13. Открытые вопросы

- [x] Тема — обе с переключателем: `lara-light-blue` (по умолчанию) и `lara-dark-blue`, динамическое переключение через `#theme-link`
- [ ] Шрифт для деванагари — Noto Sans Devanagari?
- [ ] Keycloak JS adapter или самописный OAuth2 флоу?
- [ ] PWA (оффлайн режим для словаря)?
