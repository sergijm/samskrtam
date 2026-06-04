# User & Groups Frontend Specification

> Модуль: `frontend/`
> Связанные сервисы: `user-service`
> Status: **DRAFT**

Этот файл описывает всё что касается пользователей и групп учащихся на фронтенде:
страницы, компоненты, TypeScript типы, React Query хуки и API клиент.
Остальные части фронтенда — в [frontend.md](frontend.md).

---

## 1. Роуты

| Path | Компонент | Auth | Role |
|---|---|---|---|
| `/settings` | SettingsPage | Да | STUDENT, ADMIN |
| `/settings/password` | ChangePasswordPage | Да | STUDENT, ADMIN |
| `/users/:id` | UserProfilePage | Да | STUDENT, ADMIN |
| `/groups` | GroupListPage | Да | ADMIN |
| `/groups/new` | GroupCreatePage | Да | ADMIN |
| `/groups/:id` | GroupPage | Да | STUDENT, ADMIN |
| `/groups/:id/edit` | GroupEditPage | Да | ADMIN, CURATOR |

Доступ к `/groups/:id` для студента — только если он состоит в этой группе или является куратором.
Переход через чип на `UserProfilePage` открывает `GroupPage` напрямую.

---

## 2. Структура файлов

```
src/
├── pages/
│   ├── SettingsPage.tsx
│   ├── ChangePasswordPage.tsx
│   ├── UserProfilePage.tsx         ← профиль любого пользователя
│   ├── GroupListPage.tsx           ← список групп (только ADMIN)
│   ├── GroupPage.tsx               ← страница группы
│   ├── GroupCreatePage.tsx         ← создание группы (только ADMIN)
│   └── GroupEditPage.tsx           ← редактирование названия (ADMIN / CURATOR)
│
├── components/
│   ├── user/
│   │   ├── UserGroupChips.tsx      ← горизонтальные чипсы групп
│   │   └── UserAvatar.tsx          ← аватар + имя
│   └── group/
│       ├── GroupMembersTable.tsx   ← таблица с сортировкой, пагинацией, фильтром
│       ├── GroupCuratorBadge.tsx   ← бейдж куратора в таблице
│       └── AddMemberDialog.tsx     ← диалог добавления пользователя
│
├── api/
│   └── userApi.ts                  ← все вызовы к user-service
│
├── hooks/
│   ├── useUser.ts
│   └── useGroups.ts
│
└── types/
    └── user.ts                     ← User, Group, GroupMember, GroupRole
```

---

## 3. TypeScript типы

```typescript
// types/user.ts

export interface User {
  id:       string;
  username: string;
  email:    string;
  roles:    ('STUDENT' | 'ADMIN')[]; // Изменено: теперь массив ролей
  locale:   'ru' | 'en';
  theme:    'light' | 'dark';
}

export type Theme  = 'light' | 'dark';
export type Locale = 'ru' | 'en';

export interface AuthTokens {
  accessToken:  string;
  refreshToken: string;
}

// Роль пользователя внутри группы (не путать с глобальной ролью ADMIN/STUDENT)
export type GroupRole = 'CURATOR' | 'MEMBER';

export interface Group {
  id:          string;
  name:        string;
  curatorId:   string;             // userId куратора
  curatorName: string;
  memberCount: number;
  createdAt:   string;             // ISO 8601
}

export interface GroupMember {
  userId:    string;
  username:  string;
  email:     string;
  groupRole: GroupRole;
  joinedAt:  string;               // ISO 8601
}

export interface GroupDetail extends Group {
  members: GroupMember[];
}

// Для UserProfilePage — чипсы групп
export interface UserGroupSummary {
  groupId:   string;
  groupName: string;
  groupRole: GroupRole;            // чтобы показать куратора особым чипом
}
```

---

## 4. API клиент

```typescript
// api/userApi.ts

export const userApi = {

  // ── Профиль ──────────────────────────────────────────────

  getMe: () =>
    api.get<User>('/api/v1/users/me'),

  updateMe: (data: { locale?: Locale; theme?: Theme }) =>
    api.patch<User>('/api/v1/users/me', data),

  changePassword: (currentPassword: string, newPassword: string) =>
    api.post('/api/v1/auth/change-password', { currentPassword, newPassword }),

  getUser: (userId: string) =>
    api.get<User>(`/api/v1/users/${userId}`),

  getUserGroups: (userId: string) =>
    api.get<UserGroupSummary[]>(`/api/v1/users/${userId}/groups`),

  // ── Группы ───────────────────────────────────────────────

  getGroups: () =>
    api.get<Group[]>('/api/v1/groups'),

  getGroup: (groupId: string) =>
    api.get<GroupDetail>(`/api/v1/groups/${groupId}`),

  createGroup: (name: string) =>
    api.post<Group>('/api/v1/groups', { name }),

  renameGroup: (groupId: string, name: string) =>
    api.patch<Group>(`/api/v1/groups/${groupId}`, { name }),

  // ── Участники ────────────────────────────────────────────

  addMember: (groupId: string, userId: string) =>
    api.post(`/api/v1/groups/${groupId}/members`, { userId }),

  removeMember: (groupId: string, userId: string) =>
    api.delete(`/api/v1/groups/${groupId}/members/${userId}`),

  // ── Куратор ──────────────────────────────────────────────

  // ADMIN или текущий CURATOR назначает нового куратора из участников группы
  setCurator: (groupId: string, userId: string) =>
    api.put(`/api/v1/groups/${groupId}/curator`, { userId }),
};
```

---

## 5. React Query хуки

```typescript
// hooks/useUser.ts

export const useMe = () =>
  useQuery({
    queryKey: ['users', 'me'],
    queryFn:  () => userApi.getMe(),
  });

export const useUser = (userId: string) =>
  useQuery({
    queryKey: ['users', userId],
    queryFn:  () => userApi.getUser(userId),
    enabled:  !!userId,
  });

export const useUserGroups = (userId: string) =>
  useQuery({
    queryKey: ['users', userId, 'groups'],
    queryFn:  () => userApi.getUserGroups(userId),
    enabled:  !!userId,
  });

export const useUpdateMe = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: { locale?: Locale; theme?: Theme }) =>
      userApi.updateMe(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users', 'me'] });
    },
  });
};

// hooks/useGroups.ts

export const useGroups = () =>
  useQuery({
    queryKey: ['groups'],
    queryFn:  () => userApi.getGroups(),
  });

export const useGroup = (groupId: string) =>
  useQuery({
    queryKey: ['groups', groupId],
    queryFn:  () => userApi.getGroup(groupId),
    enabled:  !!groupId,
  });

export const useCreateGroup = () => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => userApi.createGroup(name),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['groups'] }),
  });
};

export const useRenameGroup = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (name: string) => userApi.renameGroup(groupId, name),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['groups', groupId] }),
  });
};

export const useAddMember = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => userApi.addMember(groupId, userId),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['groups', groupId] }),
  });
};

export const useRemoveMember = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => userApi.removeMember(groupId, userId),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['groups', groupId] }),
  });
};

export const useSetCurator = (groupId: string) => {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (userId: string) => userApi.setCurator(groupId, userId),
    onSuccess:  () => queryClient.invalidateQueries({ queryKey: ['groups', groupId] }),
  });
};
```

---

## 6. Страницы

### SettingsPage (`/settings`)

**Назначение:** настройки внешнего вида и языка интерфейса.

**Элементы:**
- Секция "Язык": RadioButton группа — Русский / English
- Секция "Тема": RadioButton группа — Светлая / Тёмная
- Кнопка "Сохранить"
- Ссылка "Сменить пароль" → `/settings/password`

**Поведение:**

Настройки применяются немедленно при выборе (предпросмотр), кнопка "Сохранить" синхронизирует с профилем:

```
Пользователь выбирает "Тёмная"
  → themeStore.setTheme('dark')           [мгновенно]
  → DOM: #theme-link href меняется на lara-dark-blue/theme.css

Пользователь нажимает "Сохранить"
  → PATCH /api/v1/users/me { locale, theme }
  → при следующем входе настройки восстанавливаются из профиля
```

**Восстановление при логине:**
```typescript
// App.tsx
const { user } = useAuthStore();
useEffect(() => {
  if (user) {
    localeStore.setLocale(user.locale);
    themeStore.setTheme(user.theme);
  }
}, [user]);
```

Для неавторизованных (LoginPage) — берётся из `localStorage`.

---

### ChangePasswordPage (`/settings/password`)

**Назначение:** смена пароля.

**Элементы:**
- Поле "Текущий пароль"
- Поле "Новый пароль"
- Поле "Подтверждение нового пароля"
- Кнопка "Изменить пароль"

**Поведение:**
- POST /api/v1/auth/change-password `{ currentPassword, newPassword }`
- Успех → Toast "Пароль изменён"
- Ошибка → сообщение под полем "Текущий пароль"

---

### UserProfilePage (`/users/:id`)

**Назначение:** просмотр профиля пользователя. Доступна любому авторизованному пользователю.

**Элементы:**
- `UserAvatar` — имя + email
- Глобальные роли (бейджи STUDENT / ADMIN)
- **Секция "Группы"**: горизонтальный ряд `UserGroupChips`
  - Каждый чип — название группы
  - Чип куратора — с иконкой `pi-star` и другим цветом (`severity="warning"`)
  - Клик на чип → `navigate('/groups/:groupId')`
  - Если групп нет — текст "Не состоит ни в одной группе"

```typescript
// components/user/UserGroupChips.tsx
export const UserGroupChips = ({ userId }: { userId: string }) => {
  const { data: groups } = useUserGroups(userId);
  const navigate = useNavigate();

  return (
    <div className="flex flex-wrap gap-2">
      {groups?.map(g => (
        <Chip
          key={g.groupId}
          label={g.groupName}
          icon={g.groupRole === 'CURATOR' ? 'pi pi-star' : undefined}
          className={g.groupRole === 'CURATOR' ? 'p-chip-warning' : undefined}
          onClick={() => navigate(`/groups/${g.groupId}`)}
          style={{ cursor: 'pointer' }}
        />
      ))}
      {groups?.length === 0 && (
        <span className="text-color-secondary">{t('groups.noGroups')}</span>
      )}
    </div>
  );
};
```

---

### GroupListPage (`/groups`)

**Назначение:** список всех групп. Только для ADMIN.

**Элементы:**
- Таблица групп: название, куратор, кол-во участников, дата создания
- Кнопка "Создать группу" → `/groups/new`
- Клик на строку → `/groups/:id`

---

### GroupCreatePage (`/groups/new`)

**Назначение:** создание новой группы. Только для ADMIN.

**Элементы:**
- Поле "Название группы"
- Кнопка "Создать"
- Кнопка "Отмена" → `/groups`

**Поведение:**
- POST /api/v1/groups `{ name }`
- Успех → редирект на `/groups/:newId`
- Куратор изначально не назначен (назначается на GroupPage)

---

### GroupPage (`/groups/:id`)

**Назначение:** страница группы — основной экран работы с группой.

**Элементы:**

**Шапка:**
- Название группы (inline-редактирование для ADMIN и CURATOR — `InputText` с кнопкой сохранить)
- Бейдж куратора: имя + ссылка на `/users/:curatorId`
- Кнопка "Добавить участника" (ADMIN и CURATOR) → `AddMemberDialog`
- Кнопка "Назначить куратора" (ADMIN и CURATOR) → выбор из текущих участников

**Таблица участников (`GroupMembersTable`):**

```typescript
// components/group/GroupMembersTable.tsx
// Использует PrimeReact DataTable
<DataTable
  value={members}
  paginator rows={20}
  sortField="username" sortOrder={1}
  filters={filters}
  filterDisplay="row"
  globalFilterFields={['username', 'email']}
>
  <Column field="username"  header={t('groups.table.name')}   sortable filter />
  <Column field="email"     header="Email"                     sortable filter />
  <Column field="groupRole" header={t('groups.table.role')}   body={roleTemplate} />
  <Column field="joinedAt"  header={t('groups.table.joined')} sortable
          body={row => formatDate(row.joinedAt)} />
  <Column body={actionsTemplate} />  {/* Удалить / Назначить куратором */}
</DataTable>
```

- Сортировка: по имени, email, дате вступления
- Пагинация: 20 строк на страницу
- Фильтрация: глобальный поиск по имени и email + фильтр по роли в группе
- Строка куратора отмечена `GroupCuratorBadge` (`pi-star` + `severity="warning"`)
- Кнопка "Удалить" в строке — для ADMIN и CURATOR (нельзя удалить самого себя, если куратор)
- Кнопка "Сделать куратором" в строке — для ADMIN и CURATOR (только для MEMBER-строк)

**Логика прав в UI:**

| Действие | ADMIN | CURATOR | MEMBER |
|---|---|---|---|
| Переименовать группу | ✓ | ✓ | — |
| Добавить участника | ✓ | ✓ | — |
| Удалить участника | ✓ | ✓ | — |
| Назначить куратора | ✓ | ✓ (только себя заменить) | — |
| Видеть страницу | ✓ | ✓ | ✓ |

> Фронтенд скрывает кнопки согласно роли, но авторизацию проверяет user-service.

**`AddMemberDialog`:**
```typescript
// components/group/AddMemberDialog.tsx
// AutoComplete по username/email → POST /api/v1/groups/:id/members
<Dialog header={t('groups.addMember')} visible={visible} onHide={onHide}>
  <AutoComplete
    value={query}
    suggestions={suggestions}
    completeMethod={searchUsers}   // GET /api/v1/users?search=...
    field="username"
    onChange={e => setQuery(e.value)}
    placeholder={t('groups.searchUser')}
  />
  <Button label={t('common.add')} onClick={handleAdd} disabled={!selected} />
</Dialog>
```

---

### GroupEditPage (`/groups/:id/edit`)

**Назначение:** отдельная страница редактирования названия группы (fallback для мобильных, если inline не удобен).

**Элементы:**
- Поле "Название группы" (предзаполнено текущим)
- Кнопка "Сохранить"
- Кнопка "Отмена" → `/groups/:id`

**Поведение:**
- PATCH /api/v1/groups/:id `{ name }`
- Успех → редирект на `/groups/:id`

---

## 7. Обновления AdminPage

`AdminPage` (`/admin`) получает новую вкладку "Группы":

```typescript
// pages/AdminPage.tsx
<TabView>
  <TabPanel header={t('admin.tabs.quizzes')}>   {/* существующая */} </TabPanel>
  <TabPanel header={t('admin.tabs.questions')}>  {/* существующая */} </TabPanel>
  <TabPanel header={t('admin.tabs.users')}>      {/* существующая */} </TabPanel>
  <TabPanel header={t('admin.tabs.groups')}>
    {/* Кнопка "Создать группу" + таблица групп с переходом на /groups/:id */}
  </TabPanel>
</TabView>
```

Вкладка "Группы" в AdminPage — это облегчённая версия GroupListPage встроенная в таблицу (без отдельного роута). Полный функционал управления — на `/groups/:id`.

---

## 8. i18n ключи (дополнение к frontend.md)

```json
// i18n/ru.json — раздел groups
{
  "groups": {
    "title":         "Группы",
    "createGroup":   "Создать группу",
    "groupName":     "Название группы",
    "noGroups":      "Не состоит ни в одной группе",
    "addMember":     "Добавить участника",
    "removeMember":  "Удалить из группы",
    "setCurator":    "Назначить куратором",
    "curator":       "Куратор",
    "member":        "Участник",
    "searchUser":    "Поиск по имени или email",
    "table": {
      "name":        "Имя",
      "role":        "Роль в группе",
      "joined":      "Дата вступления"
    },
    "confirm": {
      "remove":      "Удалить {{username}} из группы?",
      "setCurator":  "Назначить {{username}} куратором группы?"
    }
  },
  "admin": {
    "tabs": {
      "quizzes":   "Квизы",
      "questions": "Вопросы",
      "users":     "Пользователи",
      "groups":    "Группы"
    }
  }
}
```

---

## 9. Acceptance Criteria

### Пользователи
- [ ] UserProfilePage показывает имя, email, глобальные роли
- [ ] Секция "Группы" отображает горизонтальные чипсы групп пользователя
- [ ] Чип куратора отличается визуально (иконка `pi-star`, другой цвет)
- [ ] Клик на чип открывает `/groups/:groupId`
- [ ] Если групп нет — отображается текст-заглушка

### Группы
- [ ] ADMIN может создать группу через `/groups/new`
- [ ] GroupPage: таблица поддерживает сортировку по имени, email, дате
- [ ] GroupPage: пагинация 20 строк на страницу
- [ ] GroupPage: фильтрация по имени / email / роли работает без перезагрузки
- [ ] Название группы редактируется inline для ADMIN и CURATOR
- [ ] ADMIN и CURATOR могут добавить участника через `AddMemberDialog` с поиском
- [ ] ADMIN и CURATOR могут удалить участника (с подтверждением через `ConfirmDialog`)
- [ ] ADMIN может назначить любого участника куратором
- [ ] CURATOR может назначить куратором другого участника (себя заменяет)
- [ ] После смены куратора — старый куратор становится MEMBER, новый — CURATOR
- [ ] Кнопки управления скрыты для MEMBER
- [ ] GroupListPage доступна только ADMIN (`ProtectedRoute allowedRoles={['ADMIN']}`)
- [ ] GroupPage доступна STUDENT только если он является участником или куратором

### Настройки
- [ ] SettingsPage: тема применяется мгновенно (без Save)
- [ ] SettingsPage: язык применяется мгновенно (без Save)
- [ ] Кнопка "Сохранить" делает PATCH /api/v1/users/me и показывает Toast
- [ ] После логина тема и язык восстанавливаются из профиля
- [ ] ChangePasswordPage: успех показывает Toast, поля очищаются
