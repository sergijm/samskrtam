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

```typescript
// types/featureFlag.ts

export interface FeatureFlag {
  name:        string;           // 'RATE_LIMITING_ENABLED'
  enabled:     boolean;
  description: string;
  updatedAt:   string;           // ISO 8601
  updatedBy:   string;           // email администратора
}

export interface FlagHistoryEntry {
  changedAt:  string;            // ISO 8601
  changedBy:  string;
  oldValue:   boolean;
  newValue:   boolean;
  reason:     string | null;
}

export interface FlagUpdateRequest {
  enabled: boolean;
}
```

---

## 5. API клиент

```typescript
// api/featureFlagApi.ts

export const featureFlagApi = {

  getAll: () =>
    api.get<FeatureFlag[]>('/api/v1/flags'),

  get: (name: string) =>
    api.get<FeatureFlag>(`/api/v1/flags/${name}`),

  update: (name: string, enabled: boolean) =>
    api.patch<FeatureFlag>(`/api/v1/flags/${name}`, { enabled }),

  getHistory: (name: string) =>
    api.get<FlagHistoryEntry[]>(`/api/v1/flags/${name}/history`),
};
```

---

## 6. React Query хуки

```typescript
// hooks/useFeatureFlags.ts

export const useFeatureFlags = () =>
  useQuery({
    queryKey: ['feature-flags'],
    queryFn:  featureFlagApi.getAll,
    // Не кешируем долго — состояние флагов должно быть актуальным
    staleTime: 10_000,
  });

export const useFlagHistory = (name: string) =>
  useQuery({
    queryKey: ['feature-flags', name, 'history'],
    queryFn:  () => featureFlagApi.getHistory(name),
    enabled:  !!name,
  });

export const useUpdateFlag = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ name, enabled }: { name: string; enabled: boolean }) =>
      featureFlagApi.update(name, enabled),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['feature-flags'] });
    },
  });
};
```

---

## 7. Страницы

### FeatureFlagsPage (`/admin/flags`)

**Назначение:** управление всеми feature flags. Только для ADMIN.

**Layout:**
```
┌─────────────────────────────────────────────────────┐
│  ⚑ Feature Flags                                    │
│  Управляйте поведением системы без перезапуска      │
├─────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────┐   │
│  │  Rate Limiting                              │   │
│  │  Включает Redis Rate Limiting в api-gateway │   │
│  │                                             │   │
│  │  ● Включён     [  ●  ]    Изменён: 2 ч назад│   │
│  │  Администратор: admin@samskrtam.local        │   │
│  │                              [История →]    │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │  Email Notifications                        │   │
│  │  Отправка email (регистрация, сброс пароля) │   │
│  │                                             │   │
│  │  ○ Выключен   [○    ]    Никогда не менялся │   │
│  │  Значение по умолчанию                      │   │
│  │                              [История →]    │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

**Компонент FlagCard:**
- Название флага (человекочитаемое, не `SCREAMING_SNAKE_CASE`)
- Описание из БД
- `FlagToggle` — InputSwitch с визуальным состоянием включён/выключен
- Время последнего изменения (relative: "2 ч назад") и автор
- Кнопка "История →" → `/admin/flags/:name/history`

**Маппинг имён флагов для отображения** (локализован):
```typescript
// Читаемые названия вместо SCREAMING_SNAKE_CASE
const FLAG_LABELS: Record<string, string> = {
  RATE_LIMITING_ENABLED:       'Rate Limiting',
  EMAIL_NOTIFICATIONS_ENABLED: 'Email Notifications',
};
```

---

### FlagToggle — подтверждение перед изменением

Изменение флага — потенциально опасное действие (можно сломать production).
Поэтому переключение требует подтверждения через `ConfirmDialog`:

```typescript
// components/featureFlags/FlagToggle.tsx
export const FlagToggle = ({ flag }: { flag: FeatureFlag }) => {
  const { mutate: updateFlag, isPending } = useUpdateFlag();
  const { t } = useTranslation();

  const handleChange = (newValue: boolean) => {
    confirmDialog({
      message: newValue
        ? t('flags.confirmEnable',  { name: FLAG_LABELS[flag.name] })
        : t('flags.confirmDisable', { name: FLAG_LABELS[flag.name] }),
      header:  t('flags.confirmHeader'),
      icon:    newValue ? 'pi pi-check-circle' : 'pi pi-exclamation-triangle',
      acceptClassName: newValue ? 'p-button-success' : 'p-button-danger',
      accept:  () => updateFlag({ name: flag.name, enabled: newValue }),
    });
  };

  return (
    <div className="flex align-items-center gap-3">
      <span className={flag.enabled ? 'text-green-500' : 'text-color-secondary'}>
        {flag.enabled ? t('flags.enabled') : t('flags.disabled')}
      </span>
      <InputSwitch
        checked={flag.enabled}
        onChange={e => handleChange(e.value)}
        disabled={isPending}
      />
    </div>
  );
};
```

**Почему ConfirmDialog а не просто Toast:**
- Переключение флага применяется немедленно и влияет на всех пользователей
- `EMAIL_NOTIFICATIONS_ENABLED: false` → все письма перестают уходить
- `RATE_LIMITING_ENABLED: false` → система открыта для flood-атак
- Одно случайное нажатие без подтверждения — Production инцидент

---

### FlagHistoryPage (`/admin/flags/:name/history`)

**Назначение:** аудит изменений конкретного флага.

**Layout:**
```
┌──────────────────────────────────────────────────────────────┐
│  ← Назад к флагам                                           │
│                                                              │
│  История: Rate Limiting                                      │
│  Текущее состояние: ● Включён                               │
├──────────────────────────────────────────────────────────────┤
│  Когда           Кто                  Было  →  Стало        │
│  01.05.24 12:00  admin@...local       ○ выкл   ● вкл        │
│  30.04.24 09:15  admin@...local       ● вкл    ○ выкл       │
│  29.04.24 15:00  admin@...local       ○ выкл   ● вкл        │
└──────────────────────────────────────────────────────────────┘
```

**FlagHistoryTable** использует PrimeReact DataTable:
- Колонка "Когда" — форматированная дата + время, сортировка DESC
- Колонка "Кто" — email из `changedBy`
- Колонка "Было → Стало" — цветные бейджи: зелёный `●` включён, серый `○` выключен
- Пагинация: 20 строк

```typescript
// components/featureFlags/FlagHistoryTable.tsx
const statusTemplate = (value: boolean) => (
  <Tag
    value={value ? t('flags.enabled') : t('flags.disabled')}
    severity={value ? 'success' : 'secondary'}
    icon={value ? 'pi pi-check' : 'pi pi-times'}
  />
);

<DataTable value={history} paginator rows={20}
           defaultSortOrder={-1} sortField="changedAt">
  <Column field="changedAt"  header={t('flags.history.when')}
          body={row => formatDateTime(row.changedAt)} sortable />
  <Column field="changedBy"  header={t('flags.history.who')} />
  <Column header={t('flags.history.change')} body={row => (
    <div className="flex align-items-center gap-2">
      {statusTemplate(row.oldValue)}
      <i className="pi pi-arrow-right text-color-secondary" />
      {statusTemplate(row.newValue)}
    </div>
  )} />
</DataTable>
```

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

```json
// i18n/ru.json — раздел flags
{
  "flags": {
    "title":          "Feature Flags",
    "subtitle":       "Управляйте поведением системы без перезапуска",
    "enabled":        "Включён",
    "disabled":       "Выключен",
    "neverChanged":   "Никогда не менялся",
    "lastChanged":    "Изменён {{time}}",
    "changedBy":      "Администратор: {{email}}",
    "historyLink":    "История",
    "confirmHeader":  "Подтвердите изменение",
    "confirmEnable":  "Включить {{name}}? Изменение применится немедленно для всех пользователей.",
    "confirmDisable": "Выключить {{name}}? Изменение применится немедленно для всех пользователей.",
    "history": {
      "title":  "История: {{name}}",
      "when":   "Когда",
      "who":    "Кто изменил",
      "change": "Изменение"
    }
  },
  "admin": {
    "nav": {
      "quizzes":  "Квизы",
      "users":    "Пользователи",
      "groups":   "Группы",
      "flags":    "Feature Flags"
    }
  }
}
```

---

## 10. Роуты — обновление frontend.md

Добавить в общую таблицу роутов:

| Path | Компонент | Auth | Role |
|---|---|---|---|
| `/admin/flags` | FeatureFlagsPage | Да | ADMIN |
| `/admin/flags/:name/history` | FlagHistoryPage | Да | ADMIN |

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
