# user-service

> Домен: Identity — профили пользователей, управление аккаунтом
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/user-service`
> Порт: 8087
> Status: **DRAFT**

> **Переименован из auth-service.** Логин, refresh, logout, OAuth2 flow перенесены в api-gateway (ADR-001).

---

## 1. Описание

Управляет жизненным циклом аккаунта и профилем пользователя. Хранит профили в собственной схеме БД, синхронизирует изменения с Keycloak через Outbox Pattern. Управляет файлами (аватарки) через MinIO. Реализован механизм восстановления пароля с отправкой ссылки по SMTP.

Разделение: api-gateway — OAuth2/OIDC, user-service — бизнес-логика аккаунта.
---

## 2. Keycloak Service Account

Client ID: `samskrtam-frontend`. Service Accounts: Enabled. Роли: `manage-users`, `view-users`. Токен получается через Client Credentials grant.
---

## 3. Outbox Pattern

Атомарность: в одной транзакции обновляется профиль в БД и создаётся `outbox_events` (PENDING). `OutboxProcessor` (@Scheduled) читает PENDING, вызывает Keycloak Admin API, при успехе → PROCESSED, при ошибке → retry до 5 раз, затем FAILED.

**Алгоритм OutboxProcessor:**
1. Читает все записи со статусом PENDING
2. Для каждой: вызывает KeycloakAdminService.apply(event)
3. При успехе: status = PROCESSED, processedAt = now
4. При ошибке: retryCount++, если ≥ MAX_RETRIES → FAILED, иначе WARN лог

**Алгоритм KeycloakAdminService.apply():**
- USER_REGISTERED: POST /users с полями username, email, firstName, lastName, password, enabled=true
- PROFILE_UPDATED: PUT /users/{id} только с изменёнными полями (firstName, lastName, username)
- USER_BLOCKED: PUT /users/{id} с enabled=false
- USER_UNBLOCKED: PUT /users/{id} с enabled=true
---

## 11. Сущности

### UserProfile
- id (UUID, PK)
- username (varchar, unique)
- email (varchar, unique)
- firstName (varchar)
- lastName (varchar)
- avatarUrl (varchar, nullable)
- roles (массив UserRole)
- blocked (boolean)
- passwordResetToken (varchar, nullable)
- passwordResetTokenExpiry (timestamp, nullable)
- createdAt (timestamp)
- updatedAt (timestamp)

### OutboxEvent
- id (UUID, PK)
- aggregateId (UUID)
- eventType (OutboxEventType enum)
- payload (jsonb)
- status (OutboxStatus enum: PENDING, PROCESSED, FAILED)
- retryCount (int)
- errorMessage (varchar, nullable)
- createdAt (timestamp)
- processedAt (timestamp, nullable)

### OutboxEventType (enum)
- USER_REGISTERED
- PROFILE_UPDATED
- USER_BLOCKED
- USER_UNBLOCKED

### OutboxStatus (enum)
- PENDING
- PROCESSED
- FAILED

### UserRole (enum)
- STUDENT
- ADMIN
---

## 12. DTO

### UserProfileResponse
- id (UUID)
- username (string)
- email (string)
- firstName (string)
- lastName (string)
- avatarUrl (string, nullable)
- roles (массив UserRole)
- blocked (boolean)
- createdAt (datetime)

### PublicProfileResponse
- id (UUID)
- username (string)
- firstName (string)
- lastName (string)
- avatarUrl (string, nullable)

### UpdateProfileRequest
- firstName (string, optional)
- lastName (string, optional)
- username (string, optional)

### RegisterRequest
- username (string)
- email (string)
- password (string)
- firstName (string)
- lastName (string)

### ChangePasswordRequest
- currentPassword (string)
- newPassword (string)

### ForgotPasswordRequest
- email (string)

### ResetPasswordRequest
- token (string)
- newPassword (string)

### UploadUrlResponse
- uploadUrl (string)
- objectKey (string)

### AvatarConfirmResponse
- avatarUrl (string)

### BlockUserResponse
- id (UUID)
- blocked (boolean)
- updatedAt (datetime)

### AdminUserListResponse
- content (массив UserProfileResponse)
- totalPages (int)
- totalElements (long)
- page (int)
- size (int)

