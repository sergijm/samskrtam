# Keycloak — Аутентификация и Identity

> Связанные файлы: [README.md](README.md) · [api-gateway.md](services/api-gateway.md)  
> Status: **DRAFT**

---

## 1. Концепция

Keycloak заменяет самописный Auth Service. Все микросервисы получают JWT от Keycloak и не знают как пользователь аутентифицировался — через Google, Mail.ru или локальный аккаунт.

```
Пользователь
    ↓ выбирает способ входа
Keycloak (self-hosted)
    ↓ брокерует через внешний провайдер или локальную БД
    ↓ выдаёт единый JWT
API Gateway
    ↓ валидирует JWT через JWKS endpoint
Микросервисы
    ↓ получают X-User-Id, X-User-Role в заголовках
```

---

## 2. Настройка клиентов (Clients) в Keycloak

Используется один клиент `samskrtam-frontend`. Все операции — логин, ROPC, OAuth2 Authorization Code, а также вызовы Admin REST API — выполняются auth-service от имени этого клиента.

### `samskrtam-frontend`

| Параметр | Значение |
|---|---|
| Client ID | `samskrtam-frontend` |
| Client authentication | `On` (confidential) |
| Direct Access Grants | `On` — ROPC (логин/пароль) |
| Standard Flow | `On` — Authorization Code (Google, Mail.ru) |
| Service accounts | `On` — для Admin REST API (регистрация, управление пользователями) |
| Valid redirect URIs | `http://localhost:3000/auth/callback`, `http://samskrtam.local/auth/callback` |
| Web origins | `http://localhost:3000`, `http://samskrtam.local` |

**Service Account roles** (вкладка Service Account Roles в Keycloak Admin):
```
realm-management → manage-users
realm-management → view-users
```

> **⚠️ После сохранения** перейдите на вкладку **Credentials** и скопируйте `Client secret`.
> Передаётся в auth-service через переменную окружения `KEYCLOAK_CLIENT_SECRET`.
> Ошибка `401 Unauthorized` при запросе к `/token` — почти всегда неверный секрет.

> **Зачем один клиент?** Direct Access Grants + Service Account в одном confidential клиенте — стандартная практика для backend-driven auth. auth-service инкапсулирует все Keycloak вызовы, фронтенд работает только с auth-service.

---

## 3. Realm конфигурация

| Параметр | Значение |
|---|---|
| Realm name | `samskrtam` |
| Регистрация | Через auth-service (Admin REST API) — самостоятельная регистрация поддержана |
| Язык по умолчанию | Russian |

---

## 4. Identity Providers

### Google (Gmail)
```
Тип:           OAuth2 / OIDC (встроенный в Keycloak)
Authorization: https://accounts.google.com/o/oauth2/auth
Token URL:     https://oauth2.googleapis.com/token
Client ID:     (из Google Cloud Console)
Client Secret: (из Google Cloud Console)
Scopes:        openid email profile
```

Настройка в Keycloak Admin Console:
```
Realm → Identity Providers → Add Provider → Google
→ Client ID / Client Secret из GCP
→ First Login Flow: автоматически создать аккаунт
```

### Mail.ru
```
Тип:           Generic OpenID Connect
Authorization: https://oauth.mail.ru/login
Token URL:     https://oauth.mail.ru/token
UserInfo URL:  https://oauth.mail.ru/userinfo
Client ID:     (из Mail.ru OAuth приложения)
Client Secret: (из Mail.ru OAuth приложения)
Scopes:        openid email
```

> ⚠️ Mail.ru не является стандартным провайдером в Keycloak.  
> Добавляется как Generic OpenID Connect Provider вручную.

### Локальный аккаунт
```
Тип:     Keycloak собственная БД пользователей
Создаёт: пользователь через форму регистрации → auth-service → Keycloak Admin REST API
Пароль:  задаётся при регистрации, смена через auth-service /change-password
```

---

## 5. Роли

```
samskrtam realm roles:
├── STUDENT   ← по умолчанию при создании аккаунта
└── ADMIN     ← назначается вручную через Keycloak Admin
```

---

## 6. JWT Claims

Токен который Keycloak выдаёт содержит:

```json
{
  "sub":             "uuid",
  "preferred_username": "ivan",
  "email":           "ivan@example.com",
  "realm_access": {
    "roles": ["STUDENT"]
  },
  "locale":          "ru",
  "iss":             "http://keycloak:8080/realms/samskrtam",
  "exp":             1234567890
}
```

Gateway извлекает из токена и передаёт downstream:
```
X-User-Id:    sub
X-User-Role:  realm_access.roles[0]
X-User-Locale: locale
```

---

## 7. Keycloak как код (realm export)

Конфигурация Keycloak хранится в репозитории как JSON и применяется при старте:

```
infrastructure/
└── keycloak/
    └── realm-export.json    ← экспорт realm из Keycloak Admin Console
```

```bash
# Экспорт текущего состояния realm
docker exec keycloak /opt/keycloak/bin/kc.sh export \
  --dir /tmp/export --realm samskrtam

# Применяется автоматически через docker-compose volume mount
```

---

## 8. Конфигурация: кто валидирует JWT

Только **API Gateway** валидирует JWT через JWKS. Остальные микросервисы JWT не видят — они получают уже проверенные данные в заголовках от Gateway.

```
API Gateway (единственный валидатор):
  spring.security.oauth2.resourceserver.jwt.jwk-set-uri:
    http://keycloak:8080/realms/samskrtam/protocol/openid-connect/certs

Все остальные сервисы (content, quiz, statistics, dictionary):
  # JWT не валидируется — доверяем заголовкам от Gateway
  # X-User-Id:     sub из JWT
  # X-User-Role:   realm_access.roles[0]
  # X-User-Locale: locale claim
```

Преимущества: единая точка верификации, нет лишних сетевых запросов к Keycloak JWKS из каждого сервиса, проще горизонтальное масштабирование сервисов.

---

## 9. Acceptance Criteria

- [ ] Пользователь входит через Google → получает JWT с ролью STUDENT
- [ ] Пользователь входит через Mail.ru → получает JWT с ролью STUDENT
- [ ] Пользователь входит через локальный аккаунт → получает JWT
- [ ] Все три способа входа дают одинаковый формат JWT
- [ ] ADMIN создаёт пользователей через Keycloak Admin Console
- [ ] realm-export.json в репозитории — достаточно для воспроизведения конфига

---

## 10. Открытые вопросы

- [ ] Mail.ru OIDC — проверить актуальность endpoints (могли измениться)
- [ ] Нужен ли Keycloak Account Console для смены пароля пользователем?
- [ ] First Login Flow для внешних провайдеров — автосоздание или ручное подтверждение?
