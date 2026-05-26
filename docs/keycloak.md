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

## 2. Realm конфигурация

| Параметр | Значение |
|---|---|
| Realm name | `samskrtam` |
| Регистрация | Отключена (только Admin создаёт пользователей) |
| Язык по умолчанию | Russian |

---

## 3. Identity Providers

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
Создаёт: только ADMIN через Keycloak Admin Console
Пароль:  задаётся при создании, смена через Keycloak Account Console
```

---

## 4. Роли

```
samskrtam realm roles:
├── STUDENT   ← по умолчанию при создании аккаунта
└── ADMIN     ← назначается вручную через Keycloak Admin
```

---

## 5. JWT Claims

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

## 6. Keycloak как код (realm export)

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

## 7. Конфигурация микросервисов

Каждый микросервис добавляет в `application.yml`:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://keycloak:8080/realms/samskrtam/protocol/openid-connect/certs
```

И Spring Security автоматически:
- Валидирует подпись JWT через JWKS
- Проверяет expiration
- Заполняет SecurityContext

---

## 8. Acceptance Criteria

- [ ] Пользователь входит через Google → получает JWT с ролью STUDENT
- [ ] Пользователь входит через Mail.ru → получает JWT с ролью STUDENT
- [ ] Пользователь входит через локальный аккаунт → получает JWT
- [ ] Все три способа входа дают одинаковый формат JWT
- [ ] ADMIN создаёт пользователей через Keycloak Admin Console
- [ ] realm-export.json в репозитории — достаточно для воспроизведения конфига

---

## 9. Открытые вопросы

- [ ] Mail.ru OIDC — проверить актуальность endpoints (могли измениться)
- [ ] Нужен ли Keycloak Account Console для смены пароля пользователем?
- [ ] First Login Flow для внешних провайдеров — автосоздание или ручное подтверждение?
