# Frontend Specification — React + TypeScript

> Модуль: `frontend/`
> Язык: TypeScript 5
> Фреймворк: React 18
> Status: **DRAFT**

---

## Содержание

Фронтенд-спецификация разбита на 4 части для удобства поддержки:

| Файл | Содержание |
|---|---|
| [frontend-overview.md](frontend-overview.md) | §1 Стек, §2 Структура проекта, §3 Роуты |
| [frontend-pages.md](frontend-pages.md) | §4 Страницы (LoginPage … AdminPage) |
| [frontend-state.md](frontend-state.md) | §5 TS-типы, §6 API-хуки, §7 Auth Store, §8 Theme/Locale Store |
| [frontend-conventions.md](frontend-conventions.md) | §9 i18n, §10 env, §11 Coding conventions, §12 Acceptance Criteria, §13 Открытые вопросы |

Связанные спецификации:
- [user-frontend.md](user-frontend.md) — пользователи, группы, настройки
- [lesson-pages-spec.md](lesson-pages-spec.md) — страницы уроков (VocabularyLessonPage, GrammarLessonPage)
- [lesson-aggregation-openapi.yaml](../openapi/lesson-aggregation-openapi.yaml) — OpenAPI для lesson-страниц
---

## 1. Стек и структура проекта

### Стек
- TypeScript 5
- React 18
- Vite для сборки (https://vitejs.dev/)
- React Query для управления состоянием приложения (https://tanstack.com/query/v4)
- Axios для HTTP запросов (https://axios-http.com/docs/intro)
- i18next для локализации (https://www.i18next.com/)

### Структура проекта

