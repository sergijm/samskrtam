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
| Светлая (по умолчанию) | `lara-light-amber` |
| Тёмная | `lara-dark-amber` |

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
<link id="theme-link" rel="stylesheet" href="/themes/lara-light-amber/theme.css" />
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
    │   ├── HomePage.tsx            ← публичная landing для незалогиненных
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
    │   │   ├── AppLayout.tsx       ← шапка + навигация + контент (только для залогиненных)
    │   │   ├── Header.tsx          ← шапка залогиненного: лого + навигация + ThemeSwitcher + LocaleSwitcher + кнопка Выйти
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
| `/` | HomePage | Нет | — |
| `/login` | LoginPage | Нет | — |
| `/register` | RegisterPage | Нет | — |
| `/forgot-password` | ForgotPasswordPage | Нет | — |
| `/auth/callback` | AuthCallbackPage | Нет | — |
| `/dashboard` | DashboardPage | Да | STUDENT, ADMIN |
| `/quizzes/:category` | QuizListPage | Да | STUDENT |
| `/quiz/grammar/:slug` | QuizPage | Да | STUDENT |
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
| `/admin/flags` | FeatureFlagsPage | Да | ADMIN |
| `/admin/flags/:name/history` | FlagHistoryPage | Да | ADMIN |

> Маршрут `/` отображает `HomePage` для неаутентифицированных пользователей и перенаправляет на `/dashboard` для аутентифицированных.

Роуты `/settings`, `/users/:id`, `/groups/*` — подробная спецификация в [user-frontend.md](user-frontend.md).
Роуты `/admin/flags/*` — подробная спецификация в [feature-flags-frontend.md](feature-flags-frontend.md).

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
- Форма логин/пароль → POST /api/v1/auth/login → user-service. После получения токенов, вызывается GET /api/v1/users/me для получения полных данных пользователя.
- Кнопка Google → GET /api/v1/auth/oauth2/google → редирект через Keycloak
- Кнопка Mail.ru → GET /api/v1/auth/oauth2/mailru → редирект через Keycloak
- После успешного входа → редирект на `/dashboard`
- Ошибка входа → сообщение под формой, без уточнения что именно неверно
- Токены и объект пользователя сохраняются в `authStore` (не в localStorage)
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
- Читает `access_token` и `refresh_token` из URL-фрагмента.
- Временно сохраняет токены в localStorage.
- Вызывает GET /api/v1/users/me для получения полных данных пользователя.
- Успех → сохраняет токены и объект пользователя в authStore → редирект на `/dashboard`
- Ошибка → редирект на `/login` с сообщением об ошибке

---

### ChangePasswordPage и SettingsPage

Спецификации вынесены в [user-frontend.md](user-frontend.md) (раздел 6).

---

### Header (залогиненный пользователь)

**Позиция:** фиксированная шапка, правый верхний угол.

**Элементы (слева → справа):**
- Логотип / название "SamskrtamApp" → ссылка на `/`
- `ThemeSwitcher`
- `LocaleSwitcher`
- **Поле пользователя (аватар с именем):**
    - **Кликабельно:** При клике перенаправляет на страницу настроек (`/settings`).
    - **Отображение:**
        - Первая строка: `firstName` + `lastName` (если доступны, иначе `username`).
        - Вторая строка: `email`.
- **Кнопка "Выйти"** (крайняя справа, иконка `pi-sign-out`)

```typescript
// components/layout/Header.tsx
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { Button } from 'primereact/button';
import { LocaleSwitcher } from '../common/LocaleSwitcher';
import { ThemeSwitcher } from '../common/ThemeSwitcher';
import UserAvatar from '../user/UserAvatar';
import { useTranslation } from 'react-i18next';

const Header = () => {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const { t } = useTranslation();

  const handleLogout = () => {
    logout();
    navigate('/');
  };

  return (
    <div className="layout-topbar flex justify-content-between align-items-center px-4 py-2">
      <Link to="/" className="layout-topbar-logo no-underline text-xl font-bold">
        <span>SamskrtamApp</span>
      </Link>
      <div className="layout-topbar-menu flex align-items-center gap-3">
        <ThemeSwitcher />
        <LocaleSwitcher />
        {user && (
          <Link to="/settings" className="no-underline text-color">
            <UserAvatar
              username={user.username}
              firstName={user.firstName}
              lastName={user.lastName}
              email={user.email}
              avatarUrl={user.avatarUrl}
            />
          </Link>
        )}
        {user && (
          <Button icon="pi pi-sign-out" className="p-button-text" onClick={handleLogout} label={t('auth.logout')} />
        )}
      </div>
    </div>
  );
};

export default Header;
```

---

### HomePage (`/` — незалогиненный пользователь)

**Назначение:** публичная landing для пользователей не прошедших авторизацию.
Если пользователь залогинен — роут `/` перенаправляет на `/dashboard`.

```typescript
// App.tsx
<Route path="/" element={<HomePage />} />
```

**Элементы:**

**Шапка:** Встроенная в `HomePage` кнопка "Вход" в правом верхнем углу.

**Hero секция:**
- Фоновое изображение: `/bk-samskrtam.jpg`
- В правом верхнем углу кнопка "Вход" (ведет на `/login`).
- Заголовок: "SamskrtamApp"
- Подзаголовок: "Learn Sanskrit with interactive quizzes and tools."

```typescript
// pages/HomePage.tsx
import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore';

const HomePage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
    if (isAuthenticated) {
      navigate('/dashboard', { replace: true });
    }
  }, [isAuthenticated, navigate]);

  if (isAuthenticated) {
    return null; // Render nothing while redirecting
  }

  return (
    <div
      className="relative min-h-screen bg-cover bg-center"
      style={{ backgroundImage: 'url(/bk-samskrtam.jpg)' }}
    >
      <div className="absolute top-0 right-0 p-4">
        <Link to="/login">
          <Button label={t('auth.login')} className="p-button-primary" />
        </Link>
      </div>
      <div className="flex flex-column align-items-center justify-content-center min-h-screen text-white">
        <h1 className="text-6xl font-bold mb-3">SamskrtamApp</h1>
        <p className="text-xl mb-5">Learn Sanskrit with interactive quizzes and tools.</p>
      </div>
    </div>
  );
};

export default HomePage;
```

---

### DashboardPage (`/dashboard` — залогиненный пользователь)

**Назначение:** главная страница после входа.

**Шапка:** `Header` с кнопкой "Выйти" в правом верхнем углу.

**Элементы:**
- Приветствие с именем пользователя: "Добро пожаловать, {{name}}"
- Плитка "Грамматика" (ведет на `/quizzes/grammar`)
- Плитка "Лексика" (ведет на `/quizzes/vocabulary`)
- Плитка "Словарь" (ведет на `/dictionary`)
- Плитка "Статистика" (ведет на `/statistics`)
- Плитка "Лидерборд" (ведет на `/leaderboard`)
- Плитка "Администрирование" (ведет на `/admin/users`, только для ADMIN)

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
import { create } from 'zustand';
import { User, AuthTokens } from '../types/user';

interface AuthState {
  user:         User | null;
  accessToken:  string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;

  login:  (tokens: AuthTokens, user: User) => void;
  logout: () => void;
  setAccessToken: (token: string) => void;
}

const getInitialState = () => {
  const accessToken = localStorage.getItem('accessToken');
  const refreshToken = localStorage.getItem('refreshToken');
  const userString = localStorage.getItem('user');
  let user: User | null = null;
  try {
    if (userString) {
      user = JSON.parse(userString);
    }
  } catch (e) {
    console.error("Failed to parse user from localStorage", e);
    localStorage.removeItem('user');
  }

  return {
    user,
    accessToken,
    refreshToken,
    isAuthenticated: !!accessToken && !!refreshToken,
  };
};

export const useAuthStore = create<AuthState>((set) => ({
  ...getInitialState(),

  login: (tokens, user) => {
    localStorage.setItem('accessToken', tokens.accessToken);
    localStorage.setItem('refreshToken', tokens.refreshToken);
    localStorage.setItem('user', JSON.stringify(user));
    set({
      user,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
      isAuthenticated: true,
    });
  },

  logout: () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    set({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,
    });
  },

  setAccessToken: (token) => {
    localStorage.setItem('accessToken', token);
    set({ accessToken: token });
  },
}));
```

---

## 8. Theme Store и Locale Store (Zustand)

```typescript
// store/themeStore.ts
type Theme = 'light' | 'dark';

const THEME_HREFS: Record<Theme, string> = {
  light: '/themes/lara-light-amber/theme.css',
  dark:  '/themes/lara-dark-amber/theme.css',
};

interface ThemeState {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

export const useThemeStore = create<ThemeState>()(
  persist(
    (set) => ({
      theme: (localStorage.getItem('theme') as Theme) ?? 'light',

      setTheme: (theme) => {
        if (theme && THEME_HREFS[theme]) {
          const link = document.getElementById('theme-link') as HTMLLinkElement;
          if (link) {
            link.href = THEME_HREFS[theme];
          }
        } else {
          console.warn(`Attempted to set an invalid theme: ${theme}. Defaulting to 'light'.`);
          theme = 'light';
          const link = document.getElementById('theme-link') as HTMLLinkElement;
          if (link) {
            link.href = THEME_HREFS[theme];
          }
        }
        document.documentElement.setAttribute('data-theme', theme);
        set({ theme });
      },
    }),
    {
      name: 'theme-storage',
    }
  )
);

// store/localeStore.ts
type Locale = 'ru' | 'en';

interface LocaleState {
  locale: Locale;
  setLocale: (locale: Locale) => void;
}

export const useLocaleStore = create<LocaleState>()(
  persist(
    (set) => ({
      locale: (localStorage.getItem('locale') as Locale) ?? 'ru',

      setLocale: (locale) => {
        i18next.changeLanguage(locale);
        set({ locale });
      },
    }),
    {
      name: 'locale-storage',
    }
  )
);
```

```typescript
// components/common/ThemeSwitcher.tsx
import React from 'react';
import { InputSwitch } from 'primereact/inputswitch';
import { useThemeStore } from '../../store/themeStore';
import { useTranslation } from 'react-i18next';

export const ThemeSwitcher = () => {
  const { theme, setTheme } = useThemeStore();
  const { t } = useTranslation();

  return (
    <div className="flex align-items-center gap-2">
      <i className="pi pi-sun" />
      <InputSwitch
        checked={theme === 'dark'}
        onChange={(e) => setTheme(e.value ? 'dark' : 'light')}
        aria-label={t('settings.toggleTheme')}
      />
      <i className="pi pi-moon" />
    </div>
  );
};

// components/common/LocaleSwitcher.tsx
import React from 'react';
import { SelectButton } from 'primereact/selectbutton';
import { useLocaleStore } from '../../store/localeStore';

export const LocaleSwitcher = () => {
  const { locale, setLocale } = useLocaleStore();

  const options = [
    { label: 'RU', value: 'ru' },
    { label: 'EN', value: 'en' },
  ];

  return (
    <SelectButton
      value={locale}
      onChange={(e) => e.value && setLocale(e.value)}
      options={options}
      aria-label="Language"
    />
  );
};
```

`ThemeSwitcher` и `LocaleSwitcher` размещаются в `Header.tsx` (для авторизованных пользователей) и на `LoginPage` и `HomePage` (для всех остальных).

---

```typescript
// api/authApi.ts — все вызовы к user-service
import api from './axios';
import { AuthTokens, User } from '../types/user';

const API_URL = import.meta.env.VITE_API_URL;

interface AuthResponse extends AuthTokens {
  // The user object is now fetched via getMe, but AuthResponse still defines tokens
  access_token: string; // Keycloak specific
  refresh_token: string; // Keycloak specific
}

export const authApi = {

  // ROPC — логин через форму
  login: (username: string, password: string) =>
    api.post<AuthResponse>('/api/v1/auth/login', { username, password }),

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
import axios from 'axios';
import { useAuthStore } from '../store/authStore';
import { authApi } from './authApi';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
});

// Request interceptor to add the Bearer token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor for automatic token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry && !originalRequest.url.includes('/auth/refresh')) {
      originalRequest._retry = true;
      const { refreshToken, setAccessToken, logout } = useAuthStore.getState();

      if (refreshToken) {
        try {
          const { data } = await authApi.refresh(refreshToken);
          setAccessToken(data.accessToken);
          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
          return api(originalRequest);
        } catch (refreshError) {
          logout();
          return Promise.reject(refreshError);
        }
      } else {
        logout();
      }
    }
    return Promise.reject(error);
  }
);

export default api;
```

---

## 9. i18n структура

```json
// i18n/ru.json
{
  "nav": {
    "dashboard": "Главная",
    "quizzes": "Квизы",
    "dictionary": "Словарь",
    "statistics": "Статистика",
    "leaderboard": "Рейтинг",
    "admin": "Администрирование",
    "grammar": "Грамматика",
    "vocabulary": "Лексика"
  },
  "settings": {
    "title": "Настройки",
    "language": "Язык",
    "theme": "Тема",
    "themeLight": "Светлая",
    "themeDark": "Тёмная",
    "toggleTheme": "Переключить тему",
    "save": "Сохранить",
    "saved": "Настройки сохранены",
    "saveError": "Не удалось сохранить настройки.",
    "languageRu": "Русский",
    "languageEn": "Английский",
    "username": "Имя пользователя",
    "firstName": "Имя",
    "lastName": "Фамилия",
    "avatar": {
      "title": "Аватар",
      "upload": "Загрузить новый аватар",
      "uploaded": "Аватар успешно загружен!",
      "invalidFileType": "Недопустимый тип файла. Разрешены только изображения JPEG, PNG, WEBP.",
      "uploadError": "Не удалось загрузить аватар."
    }
  },
  "quiz": {
    "start": "Начать квиз",
    "next": "Следующий вопрос",
    "correct": "Верно!",
    "incorrect": "Неверно",
    "question": "Вопрос {{current}} из {{total}}",
    "explanation": "Объяснение",
    "title": "Квизы",
    "fetchError": "Не удалось загрузить квизы: {{message}}",
    "totalQuestions": "Всего вопросов: {{count}}",
    "noQuizzesFound": "Квизы не найдены."
  },
  "quizzes": {
    "title": "Список квизов"
  },
  "auth": {
    "login": "Войти",
    "logout": "Выйти",
    "email": "Email",
    "emailOrLogin": "Email или логин",
    "password": "Пароль",
    "google": "Войти через Google",
    "mailru": "Войти через Mail.ru",
    "error": "Неверный email или пароль",
    "register": "Регистрация",
    "registerLink": "Зарегистрироваться",
    "forgotPassword": "Забыли пароль?",
    "forgotPasswordLink": "Забыли пароль?",
    "sendResetLink": "Отправить ссылку для сброса",
    "forgotPasswordSuccess": "Если ваш email зарегистрирован, вы получите ссылку для сброса пароля.",
    "username": "Имя пользователя",
    "confirmPassword": "Подтвердите пароль",
    "changePassword": "Сменить пароль",
    "currentPassword": "Текущий пароль",
    "newPassword": "Новый пароль",
    "passwordChanged": "Пароль успешно изменен",
    "registerError": "Ошибка регистрации."
  },
  "groups": {
    "title": "Группы",
    "createGroup": "Создать группу",
    "editGroup": "Редактировать группу",
    "groupName": "Название группы",
    "noGroups": "Не состоит ни в одной группе",
    "addMember": "Добавить участника",
    "removeMember": "Удалить из группы",
    "setCurator": "Назначить куратором",
    "curator": "Куратор",
    "member": "Участник",
    "memberCount": "Участников",
    "searchUser": "Поиск по имени или email",
    "table": {
      "name": "Имя",
      "role": "Роль в группе",
      "joined": "Дата вступления"
    },
    "confirm": {
      "remove": "Удалить {{username}} из группы?",
      "setCurator": "Назначить {{username}} куратором группы?"
    }
  },
  "admin": {
    "tabs": {
      "quizzes": "Квизы",
      "questions": "Вопросы",
      "users": "Пользователи",
      "groups": "Группы"
    },
    "users": {
      "title": "Управление пользователями",
      "fetchError": "Не удалось получить список пользователей.",
      "searchPlaceholder": "Поиск по логину, email, имени...",
      "filterByRole": "Фильтр по роли",
      "filterByStatus": "Фильтр по статусу",
      "noUsersFound": "Пользователи не найдены.",
      "role": "Роль",
      "status": "Статус",
      "blocked": "Заблокирован",
      "active": "Активен",
      "role": {
        "all": "Все роли",
        "student": "Студент",
        "admin": "Администратор"
      },
      "blocked": {
        "all": "Все статусы",
        "true": "Заблокирован",
        "false": "Активен"
      }
    }
  },
  "common": {
      "search": "Поиск",
      "edit": "Редактировать",
      "create": "Создать",
      "save": "Сохранить",
      "cancel": "Отмена",
      "add": "Добавить",
      "createdAt": "Дата создания",
      "or": "или",
      "go": "Перейти"
  },
  "validation": {
    "usernameRequired": "Требуется Email или логин.",
    "emailRequired": "Требуется Email.",
    "invalidEmail": "Неверный формат Email.",
    "passwordRequired": "Требуется пароль.",
    "confirmPasswordRequired": "Пожалуйста, подтвердите пароль.",
    "passwordsDoNotMatch": "Пароли не совпадают.",
    "currentPasswordRequired": "Требуется текущий пароль.",
    "newPasswordRequired": "Требуется новый пароль.",
    "confirmNewPasswordRequired": "Пожалуйста, подтвердите новый пароль."
  },
  "dashboard": {
    "quizzesDescription": "Проверьте свои знания с помощью интерактивных квизов.",
    "dictionaryDescription": "Изучайте новые слова на санскрите.",
    "statisticsDescription": "Отслеживайте свой прогресс и достижения.",
    "leaderboardDescription": "Соревнуйтесь с другими и смотрите свой рейтинг.",
    "settingsDescription": "Управляйте своим профилем и настройками приложения.",
    "adminDescription": "Доступ к административным инструментам и функциям.",
    "grammarDescription": "Квизы по грамматике: склонения и спряжения.",
    "vocabularyDescription": "Квизы на знание лексики."
  },
  "home": {
    "title": "SamskrtamApp",
    "subtitle": "Learn Sanskrit with interactive quizzes and tools.",
    "paniniDescription": "Панини (IV–III вв. до н.э.) — великий санскритский грамматист, автор «Аштадхьяи» — одной из самых совершенных грамматик в истории языкознания. Его труд до сих пор остаётся эталоном лингвистического анализа.",
    "paniniAlt": "Панини — грамматист санскрита",
    "startLearning": "Начать обучение",
    "learnMore": "Узнать больше",
    "footerTagline": "Платформа изучения санскрита",
    "features": {
      "quizzes": { "title": "Квизы по грамматике и лексике", "description": "Склонения, спряжения, словарный запас — всё в интерактивном формате с разбором ошибок." },
      "dictionary": { "title": "Словарь санскрита", "description": "Поиск по словам, транслитерация, примеры употребления." },
      "leaderboard": { "title": "Лидерборд", "description": "Соревнуйтесь с другими учащимися — глобальный рейтинг и рейтинг внутри группы." }
    }
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

### Навигация и аутентификация
- [ ] `/` для незалогиненного пользователя → HomePage с кнопкой "Вход"
- [ ] `/` для залогиненного пользователя → автоматический редирект на `/dashboard`
- [ ] Header: кнопка "Выйти" в правом верхнем углу — очищает authStore и редиректит на `/`
- [ ] Header: при клике на поле пользователя (аватар/имя) происходит переход на `/settings`
- [ ] После logout — пользователь видит HomePage (не DashboardPage)
- [ ] Кнопка "Вход" на HomePage ведёт на `/login`
- [ ] Плитка "Настройки" удалена с DashboardPage.
- [ ] Пункт "Настройки" удален из Sidebar.

### Auth
- [ ] Логин через форму → ROPC → получение токенов → вызов `/api/v1/users/me` → сохранение токенов и User в authStore → редирект на /dashboard
- [ ] Кнопка Google → редирект → /auth/callback → получение токенов → вызов `/api/v1/users/me` → сохранение токенов и User в authStore → редирект на /dashboard
- [ ] Кнопка Mail.ru → редирект → /auth/callback → получение токенов → вызов `/api/v1/users/me` → сохранение токенов и User в authStore → редирект на /dashboard
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

- [x] Тема — обе с переключателем: `lara-light-amber` (по умолчанию) и `lara-dark-amber`, динамическое переключение через `#theme-link`
- [ ] Шрифт для деванагари — Noto Sans Devanagari?
- [ ] Keycloak JS adapter или самописный OAuth2 флоу?
- [ ] PWA (оффлайн режим для словаря)?



## Localization Rule

All user-facing messages, validation messages and notifications must use i18n keys.

## Dashboard

Responsive card layout is the default navigation mechanism.

## Avatar Workflow

Settings -> Upload URL -> Direct MinIO Upload -> Confirm Upload
