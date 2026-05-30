# auth-service

> Домен: Authentication — Keycloak proxy
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/auth-service`
> Порт: 8087
> Status: **DRAFT**

---

## 1. Описание

Инкапсулирует все обращения к Keycloak API. Фронтенд никогда не обращается к Keycloak напрямую — только через auth-service. Сервис обрабатывает три категории задач:

- **Логин** — ROPC и OAuth2 Authorization Code (Google, Mail.ru)
- **Управление токенами** — refresh, logout
- **Управление аккаунтом** — регистрация, смена пароля, восстановление пароля

Остальные микросервисы не валидируют JWT — доверяют заголовкам `X-User-*` от Gateway. auth-service сам тоже не выдаёт JWT — это делает Keycloak; auth-service только проксирует запросы к нему.

---

## 2. Keycloak client

Используется один confidential клиент `samskrtam-frontend`. Он совмещает все необходимые grant types:

```
Client ID:            samskrtam-frontend
Client authentication: On (confidential)
Direct Access Grants: Enabled   ← ROPC (логин/пароль)
Standard Flow:        Enabled   ← Authorization Code (Google, Mail.ru)
Service Accounts:     Enabled   ← Admin REST API (регистрация, управление)
Valid Redirect URIs:  http://localhost:3000/auth/callback
                      http://samskrtam.local/auth/callback
Web Origins:          http://localhost:3000
                      http://samskrtam.local
Service Account Roles: realm-management → manage-users
                       realm-management → view-users
```

Получение admin-токена для Admin REST API (Client Credentials flow):
```
POST /realms/samskrtam/protocol/openid-connect/token
  grant_type=client_credentials
  client_id=samskrtam-frontend
  client_secret=${KEYCLOAK_CLIENT_SECRET}
```

> ⚠️ `KEYCLOAK_CLIENT_SECRET` передаётся через переменную окружения — никогда не хранится в коде. В Kubernetes: Secret ресурс → env variable в Deployment.

---

## 3. API

```
POST /api/v1/auth/login              → ROPC логин (email + password)
POST /api/v1/auth/refresh            → обновление access token
POST /api/v1/auth/logout             → инвалидация refresh token

GET  /api/v1/auth/oauth2/google      → редирект на Google через Keycloak
GET  /api/v1/auth/oauth2/mailru      → редирект на Mail.ru через Keycloak
POST /api/v1/auth/callback           → обмен authorization_code на токены

POST /api/v1/auth/register           → регистрация нового пользователя
POST /api/v1/auth/forgot-password    → запрос восстановления пароля
POST /api/v1/auth/change-password    → смена пароля (требует JWT)
```

> Все эти маршруты открыты на Gateway без валидации JWT (кроме `/change-password`). Логика маршрутизации — в [api-gateway.md](./api-gateway.md).

### POST /api/v1/auth/login

Request:
```json
{ "email": "ivan@example.com", "password": "secret" }
```

Response:
```json
{
  "accessToken":  "eyJ...",
  "refreshToken": "eyJ...",
  "expiresIn":    300,
  "user": {
    "id":       "uuid",
    "username": "ivan",
    "email":    "ivan@example.com",
    "role":     "STUDENT",
    "locale":   "ru"
  }
}
```

### POST /api/v1/auth/register

Request:
```json
{ "username": "ivan", "email": "ivan@example.com", "password": "secret" }
```

Response `201 Created` — Keycloak отправляет письмо верификации.
Response `409 Conflict` — пользователь с таким email уже существует.

### POST /api/v1/auth/callback

Request:
```json
{ "code": "...", "redirectUri": "http://localhost:3000/auth/callback" }
```

> ⚠️ `redirectUri` должен точно совпадать со значением из исходного запроса авторизации. Несовпадение → Keycloak возвращает `invalid_grant`.

---

## 4. Флоу аутентификации

### Логин/пароль (ROPC)

```
React LoginPage
  ↓ POST /api/v1/auth/login { email, password }
auth-service
  ↓ POST keycloak/realms/samskrtam/protocol/openid-connect/token
      grant_type=password&client_id=samskrtam-frontend&client_secret=...&...
Keycloak → { access_token, refresh_token, expires_in }
auth-service → { accessToken, refreshToken, user }
React → сохраняет в authStore
```

### Google / Mail.ru (Authorization Code)

```
React → GET /api/v1/auth/oauth2/google
auth-service → редирект на Keycloak /auth?kc_idp_hint=google&...
Keycloak → редирект на Google
Google → пользователь вошёл → редирект на Keycloak с кодом
Keycloak → редирект на фронт: /auth/callback?code=...
React CallbackPage → POST /api/v1/auth/callback { code, redirectUri }
auth-service → POST keycloak/.../token (grant_type=authorization_code)
Keycloak → { access_token, refresh_token }
auth-service → { accessToken, refreshToken, user }
React → сохраняет в authStore
```

### Регистрация

```
React RegisterPage
  ↓ POST /api/v1/auth/register { username, email, password }
auth-service
  ↓ получает service account token (Client Credentials от samskrtam-frontend)
  ↓ POST keycloak/admin/realms/samskrtam/users
      Authorization: Bearer <service account token>
Keycloak создаёт пользователя → отправляет verification email
  ↓ 201 Created
React → "Проверьте email"
```

### Восстановление пароля

```
React ForgotPasswordPage
  ↓ POST /api/v1/auth/forgot-password { email }
auth-service
  ↓ GET keycloak/admin/realms/samskrtam/users?email=...  (найти userId)
  ↓ PUT keycloak/admin/realms/samskrtam/users/{id}/execute-actions-email
      ["UPDATE_PASSWORD"]
Keycloak отправляет email со ссылкой
  ↓ 204 No Content
React → "Письмо отправлено"
```

### Смена пароля

```
React ChangePasswordPage (требует авторизации)
  ↓ POST /api/v1/auth/change-password { currentPassword, newPassword }
auth-service
  ↓ верифицирует currentPassword через ROPC (если невалиден → 401)
  ↓ PUT keycloak/admin/realms/samskrtam/users/{id}/reset-password
      { type: "password", value: newPassword, temporary: false }
  ↓ 204 No Content
React → "Пароль изменён"
```

---

## 5. Backend структура

```
sm/selflearn/samskrtam/auth/
├── Application.java
├── controller/
│   └── AuthController.java
├── service/
│   ├── TokenService.java          ← логин, refresh, logout, callback
│   ├── KeycloakAdminService.java  ← регистрация, смена/восстановление пароля
│   └── AdminTokenProvider.java    ← получение и кэш admin token
├── client/
│   └── KeycloakClient.java        ← HTTP-клиент к Keycloak (RestClient)
└── dto/
    ├── LoginRequest.java
    ├── LoginResponse.java
    ├── RegisterRequest.java
    ├── CallbackRequest.java
    ├── RefreshRequest.java
    ├── ForgotPasswordRequest.java
    └── ChangePasswordRequest.java
```

---

## 6. application.yml

```yaml
server:
  port: 8087

spring:
  application:
    name: auth-service
  threads:
    virtual:
      enabled: true

keycloak:
  url:            ${KEYCLOAK_URL:http://keycloak:8080}
  realm:          ${KEYCLOAK_REALM:samskrtam}
  client-id:      samskrtam-frontend
  
  client-secret:  ${KEYCLOAK_CLIENT_SECRET}
```

---

## 7. Acceptance Criteria

- [ ] Логин через форму → JWT в authStore, редирект на `/`
- [ ] Неверные credentials → 401, без уточнения что именно неверно
- [ ] Кнопка Google → редирект → возврат с токеном
- [ ] Кнопка Mail.ru → редирект → возврат с токеном
- [ ] Регистрация → письмо верификации на email, 201
- [ ] Регистрация с существующим email → 409
- [ ] Восстановление пароля → письмо со ссылкой, 204
- [ ] Смена пароля с неверным текущим паролем → 401
- [ ] Refresh token → новый access token без участия пользователя
- [ ] Logout → refresh token инвалидируется на сервере Keycloak

---

## 8. Открытые вопросы

- [ ] Mail.ru OIDC endpoints — проверить актуальность
- [ ] SMTP для Keycloak — какой сервер отправки email?
- [ ] TTL verification email — сколько часов действительна ссылка?
- [ ] Блокировать аккаунт после N неудачных попыток входа?
- [ ] Ротация `KEYCLOAK_CLIENT_SECRET` — как часто?
- [ ] `redirectUri` для production — вынести в конфиг (сейчас localhost)
