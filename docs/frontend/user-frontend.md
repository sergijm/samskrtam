# User & Groups Frontend Specification

> Модуль: `frontend/`
> Связанные сервисы: `user-service`
> Status: **DRAFT**

Этот файл описывает всё что касается пользователей и групп учащихся на фронтенде:
страницы, компоненты, TypeScript типы, React Query хуки и API клиент.
Остальные части фронтенда — в [frontend-overview.md](frontend-overview.md).

---

## 1. Роуты

| Path | Компонент | Auth | Role |
|---|---|---|---|
| `/settings` | SettingsPage | Да | STUDENT, ADMIN |
| `/settings/password` | ChangePasswordPage | Да | STUDENT, ADMIN |
| `/users/:id` | UserProfilePage | Да | STUDENT, ADMIN |
| `/groups` | AdminGroupsPage | Да | ADMIN | // Изменено: теперь ведет на AdminGroupsPage
| `/groups/new` | GroupCreatePage | Да | ADMIN |
| `/groups/:id` | GroupPage | Да | STUDENT, ADMIN |
| `/groups/:id/edit` | GroupEditPage | Да | ADMIN, CURATOR |

Доступ к `/groups/:id` для студента — только если он состоит в этой группе или является куратором.
Переход через чип на `UserProfilePage` открывает `GroupPage` напрямую.

---

## 2. Структура файлов

`pages/`: SettingsPage, ChangePasswordPage, UserProfilePage, AdminGroupsPage, GroupPage, GroupCreatePage, GroupEditPage

`components/user/`: UserGroupChips, UserAvatar

`components/group/`: GroupMembersTable, GroupCuratorBadge, AddMemberDialog

`api/userApi.ts` — все вызовы к user-service

`hooks/`: useUser.ts, useGroups.ts

`types/user.ts` — User, Group, GroupMember, GroupRole

---

## 3. TypeScript типы

Сущности и их поля:

**User:**
- id: string — UUID
- username: string — логин
- email: string — email
- roles: ('STUDENT' | 'ADMIN')[] — массив глобальных ролей
- locale: 'ru' | 'en'
- theme: 'light' | 'dark'

**AuthTokens:**
- accessToken: string
- refreshToken: string

**GroupRole:** 'CURATOR' | 'MEMBER' — роль внутри группы

**Group:**
- id: string
- name: string — название группы
- curatorId: string — UUID куратора
- curatorName: string — имя куратора
- memberCount: number
- createdAt: string (ISO 8601)

**GroupMember:**
- userId: string
- username: string
- email: string
- groupRole: GroupRole
- joinedAt: string (ISO 8601)

**GroupDetail** extends Group:
- members: GroupMember[]

**UserGroupSummary** (для чипсов):
- groupId: string
- groupName: string
- groupRole: GroupRole

---

## 4. API клиент

Эндпоинты к user-service:

**Профиль:**
- `GET /api/v1/users/me` — получить текущего пользователя
- `PATCH /api/v1/users/me` — обновить locale/theme
- `POST /api/v1/auth/change-password` — сменить пароль (currentPassword, newPassword)
- `GET /api/v1/users/{userId}` — получить пользователя по ID
- `GET /api/v1/users/{userId}/groups` — получить группы пользователя

**Группы:**
- `GET /api/v1/groups` — список всех групп
- `GET /api/v1/groups/{groupId}` — группа с участниками
- `POST /api/v1/groups` — создать группу (name)
- `PATCH /api/v1/groups/{groupId}` — переименовать (name)

**Участники:**
- `POST /api/v1/groups/{groupId}/members` — добавить участника (userId)
- `DELETE /api/v1/groups/{groupId}/members/{userId}` — удалить участника

**Куратор:**
- `PUT /api/v1/groups/{groupId}/curator` — назначить куратора (userId) — ADMIN или текущий CURATOR

---

## 5. React Query хуки

Описание хуков (каждый использует api из userApi.ts, инвалидирует кэш при мутациях):

**useUser.ts:**
- `useMe()` — GET /api/v1/users/me, queryKey: `['users', 'me']`
- `useUser(userId)` — GET /api/v1/users/{userId}, enabled: !!userId
- `useUserGroups(userId)` — GET /api/v1/users/{userId}/groups, enabled: !!userId
- `useUpdateMe()` — мутация PATCH /api/v1/users/me, на успехе инвалидирует `['users', 'me']`

**useGroups.ts:**
- `useGroups()` — GET /api/v1/groups, queryKey: `['groups']`
- `useGroup(groupId)` — GET /api/v1/groups/{groupId}, enabled: !!groupId
- `useCreateGroup()` — мутация POST /api/v1/groups, инвалидирует `['groups']`
- `useRenameGroup(groupId)` — мутация PATCH /api/v1/groups/{groupId}, инвалидирует `['groups', groupId]`
- `useAddMember(groupId)` — мутация POST /api/v1/groups/{groupId}/members, инвалидирует `['groups', groupId]`
- `useRemoveMember(groupId)` — мутация DELETE /api/v1/groups/{groupId}/members/{userId}, инвалидирует `['groups', groupId]`
- `useSetCurator(groupId)` — мутация PUT /api/v1/groups/{groupId}/curator, инвалидирует `['groups', groupId]`

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

При выборе темы/языка — мгновенное применение через themeStore/localeStore. При сохранении — PATCH /api/v1/users/me.

**Восстановление при логине:**
При загрузке App.tsx, если пользователь авторизован (useAuthStore), вызывается useEffect, который устанавливает locale и тему из профиля пользователя. Для неавторизованных (LoginPage) — настройки берутся из `localStorage`.

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

Компонент: загружает группы пользователя через `useUserGroups(userId)`, отображает их в виде чипсов. Для куратора — иконка pi-star и стиль warning. Клик → переход на `/groups/{groupId}`. Если групп нет — текст-заглушка.

---

### AdminGroupsPage (`/admin/groups`)

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

"GroupMembersTable использует PrimeReact DataTable с колонками: username, email, groupRole, joinedAt. Поддерживает сортировку, пагинацию (20 строк), глобальный фильтр. Для куратора — GroupCuratorBadge. В actionsTemplate — кнопки "Удалить" и "Сделать куратором"."

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
Диалог с AutoComplete для поиска пользователей по username/email (GET /api/v1/users?search=...). При выборе и нажатии "Добавить" → POST /api/v1/groups/:id/members.

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

AdminPage использует TabView с четырьмя вкладками: quizzes, questions, users, groups. Вкладка "Группы" содержит кнопку "Создать группу" и таблицу групп с переходом на /groups/:id.

Вкладка "Группы" в AdminPage — это облегчённая версия GroupListPage встроенная в таблицу (без отдельного роута). Полный функционал управления — на `/groups/:id`.

---

## 8. i18n ключи (дополнение к frontend.md)

Добавлены i18n ключи в разделе groups: title, createGroup, groupName, noGroups, addMember, removeMember, setCurator, curator, member, searchUser, table (name, role, joined), confirm (remove, setCurator). В admin.tabs добавлена вкладка groups. В admin.groups — description.

---



