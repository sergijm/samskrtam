# Frontend Overview — Стек, Структура, Роуты

> Модуль: `frontend/`
> Связанные файлы: [frontend.md](frontend.md) (индекс) · [frontend-pages.md](frontend-pages.md) · [frontend-state.md](frontend-state.md) · [frontend-conventions.md](frontend-conventions.md)
> Status: **DRAFT** (выделен из frontend.md)

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
    │   ├── HomePage.tsx            ← публичная landing для незалогиненных (новое)
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
    │   ├── VocabularyLessonPage.tsx  ← спецификация в lesson-pages-spec.md
    │   ├── GrammarLessonPage.tsx     ← спецификация в lesson-pages-spec.md
    │   ├── ResultPage.tsx
    │   ├── DictionaryPage.tsx
    │   ├── StatisticsPage.tsx
    │   ├── LeaderboardPage.tsx
    │   └── AdminPage.tsx
    │
    ├── components/                 ← переиспользуемые компоненты
    │   ├── layout/
    │   │   ├── AppLayout.tsx       ← шапка + навигация + контент (только для залогиненных)
    │   │   ├── PublicLayout.tsx    ← шапка для незалогиненных (кнопка Войти)
    │   │   ├── Header.tsx          ← шапка залогиненного: лого + навигация + ThemeSwitcher + LocaleSwitcher + кнопка Выйти
    │   │   ├── PublicHeader.tsx    ← шапка публичная: лого + ThemeSwitcher + LocaleSwitcher + кнопка Войти
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
    │   ├── lesson/                   ← компоненты уроков (lesson-pages-spec.md)
    │   │   ├── LessonHeader.tsx
    │   │   ├── WordStatusIcon.tsx
    │   │   ├── WordHistoryDialog.tsx
    │   │   └── QuestionHistoryDialog.tsx
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
    │   ├── lessonApi.ts              ← lesson-pages-spec.md раздел 6
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
    │   ├── useLessons.ts             ← lesson-pages-spec.md раздел 6
    │   ├── useQuizSession.ts
    │   ├── useDictionary.ts
    │   └── useStatistics.ts
    │
    ├── types/                      ← TypeScript типы
    │   ├── user.ts                 ← User, Group, GroupMember (user-frontend.md)
    │   ├── quiz.ts
    │   ├── lesson.ts                 ← lesson-pages-spec.md раздел 7
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
| `/` | **HomePage** (не залогинен) / **DashboardPage** (залогинен) | Нет | — |
| `/quizzes` | QuizListPage | Да | STUDENT |
| `/lessons/vocabulary/:slug` | VocabularyLessonPage | Да | STUDENT |
| `/lessons/grammar/:type` | GrammarLessonPage | Да | STUDENT |
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

> Маршрут `/` — единственный с условным рендером: `isAuthenticated ? <DashboardPage/> : <HomePage/>`.
> HomePage не требует авторизации и доступна по прямой ссылке.

Роуты `/settings`, `/users/:id`, `/groups/*` — подробная спецификация в [user-frontend.md](user-frontend.md).

Роуты `/lessons/*` — подробная спецификация в [lesson-pages-spec.md](../frontend/pages/lesson-pages-spec.md). OpenAPI: [lesson-aggregation-openapi.yaml](../openapi/lesson-aggregation-openapi.yaml).
    ├──► API Contract Agent (5)          ← первый: контракты
    │         │
    │         ▼
    ├──► Gateway & Infra Agent (1)        ← параллельно с Domain Agent
    │         │
    │         ▼
    ├──► Domain Services Agent (2)        ← после контрактов
    │         │
    │         ├──► Frontend Agent (3)     ← после domain API
    │         │
    │         └──► Testing Agent (4)      ← параллельно с frontend
```

**Критические зависимости:** Frontend (3) ждёт от Contract (5) OpenAPI и от Domain (2) endpoints; Testing (4) ждёт Domain (2); Gateway (1) ждёт Contract (5).

---

## Соглашения для агентов

**Именование веток:** feat/fix/test/chore(<scope>): description. Scope: quiz-service, gateway, curriculum-service, frontend, shared.

**Конфигурация:** без дефолтов в application.yml — только ${ENV_VAR}, секреты только через env, .env.example актуален.

**Definition of Done:** 1) реализация соответствует docs/; 2) тесты + покрытие ≥ 80% сервисного слоя; 3) Checkstyle и SpotBugs чисты; 4) OpenAPI обновлён; 5) PR прошёл code review.
