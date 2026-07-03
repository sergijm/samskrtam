# Feature Flags — Frontend Specification

> Модуль: `frontend/`
> Связанный сервис: `feature-flag-service`
> Status: **DRAFT**

---

## 1. Отдельная страница или вкладка в AdminPage?

**Решение: отдельная страница `/admin/flags`**, не вкладка в AdminPage.

Аргументы:

| | Вкладка в AdminPage | Отдельная страница |
|---|---|---|
| Доступ | Только через AdminPage | Прямая ссылка, bookmarkable |
| Контекст | Перемешан с квизами и пользователями | Изолирован — фокус на флагах |
| При инциденте | Нужно открыть AdminPage → найти вкладку | Открыл закладку → сразу на месте |
| Навигация | TabView — нет глубоких ссылок (нет `/admin?tab=flags`) | Полноценный роут с историей |
| Расширяемость | Вкладки AdminPage уже перегружены | Страница легко растёт (история, фильтры) |

AdminPage получает только **пункт в сайдбаре** с иконкой и ссылкой на `/admin/flags`.
Это же паттерн используют LaunchDarkly, Unleash, PostHog — Feature Flags всегда отдельный раздел.

---

## 2. Роуты

| Path | Компонент | Auth | Role |
|---|---|---|---|
| `/admin/flags` | FeatureFlagsPage | Да | ADMIN |
| `/admin/flags/:name/history` | FlagHistoryPage | Да | ADMIN |

Оба роута защищены `ProtectedRoute allowedRoles={['ADMIN']}`. STUDENT при попытке доступа получает редирект на `/`.

---

## 3. Структура файлов

```
src/
├── pages/
│   ├── FeatureFlagsPage.tsx         ← список всех флагов с тоглами
│   └── FlagHistoryPage.tsx          ← история изменений одного флага
│
├── components/
│   └── featureFlags/
│       ├── FlagCard.tsx             ← карточка одного флага
│       ├── FlagToggle.tsx           ← InputSwitch + подтверждение
│       └── FlagHistoryTable.tsx     ← таблица изменений
│
├── api/
│   └── featureFlagApi.ts            ← вызовы к feature-flag-service
│
├── hooks/
│   └── useFeatureFlags.ts           ← React Query хуки
│
└── types/
    └── featureFlag.ts               ← FeatureFlag, FlagHistoryEntry
```

---

## 4. TypeScript типы

**FeatureFlag:** name (string), enabled (boolean), description, updatedAt (ISO 8601), updatedBy (email)

**FlagHistoryEntry:** changedAt, changedBy, oldValue, newValue, reason (string|null)

**FlagUpdateRequest:** enabled (boolean)

---

## 5. API клиент

featureFlagApi.getAll() → GET /api/v1/flags; .get(name) → GET /api/v1/flags/{name}; .update(name, enabled) → PATCH /api/v1/flags/{name}; .getHistory(name) → GET /api/v1/flags/{name}/history

---

## 6. React Query хуки

**useFeatureFlags:** queryKey ['feature-flags'], staleTime 10s. **useFlagHistory(name):** queryKey ['feature-flags', name, 'history'], enabled: !!name. **useUpdateFlag:** mutation, onSuccess invalidates ['feature-flags'].

---

## 7. Страницы

### FeatureFlagsPage (`/admin/flags`)

**Назначение:** управление всеми feature flags. Только для ADMIN.

**FlagCard:** название (читаемое), описание, FlagToggle (InputSwitch с ConfirmDialog), время изменения, автор, кнопка "История →". Маппинг имён: FLAG_LABELS (RATE_LIMITING_ENABLED → 'Rate Limiting' и т.д.).

**FlagToggle:** InputSwitch, при изменении — ConfirmDialog (подтверждение: включить/выключить, иконка, цвет кнопки). Причина: переключение влияет на всех пользователей, случайное нажатие — production инцидент.

**FlagHistoryPage:** таблица PrimeReact DataTable с колонками: Когда (форматированная дата, сортировка DESC), Кто (email), Было → Стало (цветные бейджи Tag: success/secondary с pi-check/pi-times). Пагинация 20 строк.

---

## 8. Навигация — AdminPage сайдбар

AdminPage получает пункт меню "Feature Flags" в сайдбаре (не вкладку).
Иконка `pi-sliders-h` — стандартная иконка для настроек/тоглов.

```typescript
// pages/AdminPage.tsx — пункт в сайдбаре
const adminMenuItems = [
  { label: t('admin.nav.quizzes'),    icon: 'pi pi-book',      url: '/admin/quizzes' },
  { label: t('admin.nav.users'),      icon: 'pi pi-users',     url: '/admin/users' },
  { label: t('admin.nav.groups'),     icon: 'pi pi-sitemap',   url: '/admin/groups' },
  { label: t('admin.nav.flags'),      icon: 'pi pi-sliders-h', url: '/admin/flags' },
];
```

---

## 9. i18n ключи

Раздел flags: title, subtitle, enabled, disabled, neverChanged, lastChanged ({{time}}), changedBy ({{email}}), historyLink, confirmHeader, confirmEnable ({{name}}), confirmDisable ({{name}}), history.title ({{name}}), history.when, history.who, history.change. Ключи admin.nav: quizzes, users, groups, flags.

---

## 10. Роуты

Добавить в общую таблицу: /admin/flags → FeatureFlagsPage (ADMIN), /admin/flags/:name/history → FlagHistoryPage (ADMIN).

---

## 11. Acceptance Criteria

- [ ] `/admin/flags` доступна только ADMIN — STUDENT получает редирект на `/`
- [ ] Список флагов загружается с текущим состоянием каждого
- [ ] Переключение флага требует подтверждения через ConfirmDialog
- [ ] После подтверждения — состояние обновляется немедленно (optimistic update или invalidate)
- [ ] При ошибке обновления — Toast с сообщением об ошибке, состояние откатывается
- [ ] Кнопка "История" ведёт на `/admin/flags/:name/history`
- [ ] FlagHistoryPage показывает таблицу с сортировкой по дате DESC
- [ ] Бейджи "Включён/Выключен" цветные: зелёный/серый
- [ ] AdminPage сайдбар содержит пункт "Feature Flags" с иконкой `pi-sliders-h`
- [ ] Названия флагов отображаются читаемо (не `SCREAMING_SNAKE_CASE`)
