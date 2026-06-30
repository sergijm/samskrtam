"# Frontend State — Типы, API-хуки, Сторы

> Модуль: `frontend/`
> Связанные файлы: [frontend.md](frontend.md) (индекс) · [frontend-overview.md](frontend-overview.md) · [frontend-pages.md](frontend-pages.md) · [frontend-conventions.md](frontend-conventions.md)
> Status: **DRAFT** (выделен из frontend.md)

---

## 5. TypeScript типы

Типы пользователей и групп (`User`, `Group`, `GroupMember`, `GroupRole`, `AuthTokens`, `Theme`, `Locale`) вынесены в [user-frontend.md](user-frontend.md) раздел 3 (`types/user.ts`).

```typescript
// types/quiz.ts
export interface QuizSummary {
  id:         string;
  titleRu:    string;
  titleEn:    string;
  lessonType:   LessonType;
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

export type LessonType   = 'DECLENSIONS' | 'CONJUGATIONS' | 'VOCABULARY';
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
  byQuizType:        Record<LessonType, QuizTypeStats>;
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

    // 2. Синхронизируем <html data-theme=\"...\"> для кастомных CSS переменных
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
    <div className=\"flex align-items-center gap-2\">
      <i className=\"pi pi-sun\" aria-hidden=\"true\" />
      <InputSwitch
        checked={theme === 'dark'}
        onChange={(e) => setTheme(e.value ? 'dark' : 'light')}
        aria-label={t('settings.toggleTheme')}
      />
      <i className=\"pi pi-moon\" aria-hidden=\"true\" />
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
      aria-label=\"Язык / Language\"
    />
  );
};
```

`ThemeSwitcher` и `LocaleSwitcher` размещаются в `Header.tsx` (для авторизованных пользователей) и на `LoginPage` (для всех остальных)."