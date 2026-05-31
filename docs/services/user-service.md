# user-service

> Домен: Identity — профили пользователей, управление аккаунтом
> Язык: **Java 21 + Virtual Threads**
> Модуль: `services/user-service`
> Порт: 8087
> Status: **DRAFT**

> **Переименован из auth-service.** Логин, refresh, logout, OAuth2 flow перенесены в api-gateway (ADR-001).

---

## 1. Описание

Управляет жизненным циклом аккаунта и профилем пользователя. Хранит профили в собственной схеме БД, синхронизирует изменения с Keycloak через Outbox Pattern. Управляет файлами (аватарки) через MinIO.

Разделение ответственности:
- **api-gateway** — OAuth2/OIDC протокол (токены, редиректы, сессии)
- **user-service** — бизнес-логика аккаунта (регистрация, профиль, пароль, блокировка)

---

## 2. Keycloak Service Account

```
Client ID:             samskrtam-frontend
Service Accounts:      Enabled
Service Account Roles: realm-management → manage-users
                       realm-management → view-users
```

Получение admin-токена (Client Credentials):
```
POST /realms/samskrtam/protocol/openid-connect/token
  grant_type=client_credentials
  client_id=samskrtam-frontend
  client_secret=${KEYCLOAK_CLIENT_SECRET}
```

> ⚠️ `KEYCLOAK_CLIENT_SECRET` — только через переменную окружения, никогда в коде.

---

## 3. Сущности

```java
// sm/selflearn/samskrtam/user/model/UserProfile.java
@Entity
@Table(name = "user_profiles", schema = "users")
public class UserProfile {

    @Id
    private UUID id;                      // совпадает с Keycloak sub

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;                 // только для чтения после регистрации

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;             // публичный URL в MinIO (avatars/)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;                // STUDENT, ADMIN

    @Column(name = "blocked", nullable = false)
    private boolean blocked = false;      // дублируется из Keycloak для поиска/фильтрации

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}

// sm/selflearn/samskrtam/user/model/OutboxEvent.java
@Entity
@Table(name = "outbox_events", schema = "users")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;             // userId

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;    // USER_REGISTERED, PROFILE_UPDATED, USER_BLOCKED, USER_UNBLOCKED

    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private String payload;               // JSON с данными для Keycloak

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;          // PENDING, PROCESSED, FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;
}
```

---

## 4. Flyway Migrations

```sql
-- V1__create_schema.sql
CREATE SCHEMA IF NOT EXISTS users;

-- V2__create_user_profiles.sql
CREATE TABLE users.user_profiles (
    id          UUID         NOT NULL,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100),
    last_name   VARCHAR(100),
    avatar_url  VARCHAR(500),
    role        VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
    blocked     BOOLEAN      NOT NULL DEFAULT false,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_user_profiles   PRIMARY KEY (id),
    CONSTRAINT uq_username        UNIQUE (username),
    CONSTRAINT uq_email           UNIQUE (email),
    CONSTRAINT ck_role            CHECK (role IN ('STUDENT', 'ADMIN'))
);

CREATE INDEX idx_user_profiles_username ON users.user_profiles (username);
CREATE INDEX idx_user_profiles_email    ON users.user_profiles (email);
CREATE INDEX idx_user_profiles_blocked  ON users.user_profiles (blocked);
CREATE INDEX idx_user_profiles_role     ON users.user_profiles (role);

-- V3__create_outbox_events.sql
CREATE TABLE users.outbox_events (
    id            UUID        NOT NULL DEFAULT gen_random_uuid(),
    aggregate_id  UUID        NOT NULL,
    event_type    VARCHAR(50) NOT NULL,
    payload       JSONB       NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at  TIMESTAMPTZ,
    retry_count   INT         NOT NULL DEFAULT 0,
    error_message TEXT,

    CONSTRAINT pk_outbox_events PRIMARY KEY (id),
    CONSTRAINT ck_event_type    CHECK (event_type IN (
        'USER_REGISTERED', 'PROFILE_UPDATED', 'USER_BLOCKED', 'USER_UNBLOCKED'
    )),
    CONSTRAINT ck_status        CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED'))
);

CREATE INDEX idx_outbox_pending ON users.outbox_events (status, created_at)
    WHERE status = 'PENDING';
```

---

## 5. Outbox Pattern

Проблема: нужно атомарно сохранить изменения в своей БД **и** синхронизировать с Keycloak. Если сначала записать в БД, потом вызвать Keycloak — при сбое между операциями данные расходятся.

Решение — Outbox Pattern:

```
┌─────────────────────────────────────────────┐
│           @Transactional                     │
│  1. UPDATE users.user_profiles               │
│  2. INSERT users.outbox_events (PENDING)     │
└──────────────────┬──────────────────────────┘
                   │ одна транзакция — атомарно
                   ▼
         OutboxProcessor (@Scheduled)
                   │
                   ├─ читает PENDING события
                   ├─ вызывает Keycloak Admin API
                   ├─ при успехе → status = PROCESSED
                   └─ при ошибке → retry_count++, status = FAILED после N попыток
```

```java
// sm/selflearn/samskrtam/user/service/UserProfileService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final OutboxEventRepository outboxRepository;

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.trace("updateProfile: userId={}", userId);

        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setUsername(request.username());
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);

        // Атомарно с сохранением профиля — в одной транзакции
        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(userId)
                .eventType(OutboxEventType.PROFILE_UPDATED)
                .payload(toJson(Map.of(
                        "firstName", request.firstName(),
                        "lastName",  request.lastName(),
                        "username",  request.username()
                )))
                .status(OutboxStatus.PENDING)
                .build());

        log.debug("Profile updated and outbox event created: userId={}", userId);
        return UserProfileResponse.from(profile);
    }
}

// sm/selflearn/samskrtam/user/outbox/OutboxProcessor.java
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxRepository;
    private final KeycloakAdminService keycloakAdminService;

    @Scheduled(fixedDelayString = "${outbox.processor.interval-ms}")
    @Transactional
    public void process() {
        List<OutboxEvent> pending = outboxRepository.findPendingEvents();
        if (pending.isEmpty()) return;

        log.debug("Processing {} outbox events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                keycloakAdminService.apply(event);
                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                log.debug("Outbox event processed: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event failed after {} retries: id={}, type={}",
                            MAX_RETRIES, event.getId(), event.getEventType(), e);
                } else {
                    log.warn("Outbox event retry {}/{}: id={}, type={}",
                            event.getRetryCount(), MAX_RETRIES, event.getId(), event.getEventType());
                }
            }
            outboxRepository.save(event);
        }
    }
}
```

---

## 6. MinIO — управление файлами

### Бакеты

| Бакет | Политика | Назначение |
|---|---|---|
| `avatars` | **публичный** (read-only) | Аватарки пользователей |
| `documents` | приватный (presigned URL) | Будущие файлы |

### Upload flow (аватарка)

```
1. Браузер → POST /api/v1/users/me/avatar/upload-url
   user-service → генерирует presigned PUT URL (MinIO SDK, TTL 5 минут)
   → { uploadUrl, objectKey }

2. Браузер → PUT {uploadUrl} (напрямую в MinIO, бинарные данные)
   MinIO → 200 OK

3. Браузер → POST /api/v1/users/me/avatar/confirm { objectKey }
   user-service → проверяет что объект существует в MinIO
   user-service → обновляет avatar_url в user_profiles
   → { avatarUrl }  ← публичный URL для чтения
```

```java
// sm/selflearn/samskrtam/user/service/AvatarService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class AvatarService {

    private final MinioClient minioClient;
    private final UserProfileRepository profileRepository;

    @Value("${minio.bucket.avatars}")
    private String avatarsBucket;

    @Value("${minio.public-url}")
    private String minioPublicUrl;

    public UploadUrlResponse generateUploadUrl(UUID userId, String contentType) {
        log.trace("generateUploadUrl: userId={}", userId);
        validateImageContentType(contentType);

        String objectKey = "avatars/" + userId + "/" + UUID.randomUUID();

        String uploadUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.PUT)
                        .bucket(avatarsBucket)
                        .object(objectKey)
                        .expiry(5, TimeUnit.MINUTES)
                        .extraQueryParams(Map.of("Content-Type", contentType))
                        .build()
        );

        return new UploadUrlResponse(uploadUrl, objectKey);
    }

    @Transactional
    public AvatarConfirmResponse confirmUpload(UUID userId, String objectKey) {
        log.trace("confirmUpload: userId={}, objectKey={}", userId, objectKey);

        // Проверяем что объект действительно загружен
        minioClient.statObject(StatObjectArgs.builder()
                .bucket(avatarsBucket)
                .object(objectKey)
                .build());   // бросает исключение если не существует

        String avatarUrl = minioPublicUrl + "/" + avatarsBucket + "/" + objectKey;

        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        profile.setAvatarUrl(avatarUrl);
        profileRepository.save(profile);

        log.debug("Avatar confirmed: userId={}, url={}", userId, avatarUrl);
        return new AvatarConfirmResponse(avatarUrl);
    }

    private void validateImageContentType(String contentType) {
        if (!List.of("image/jpeg", "image/png", "image/webp").contains(contentType)) {
            throw new InvalidFileTypeException(contentType);
        }
    }
}
```

---

## 7. API

### Текущий пользователь

```
GET    /api/v1/users/me                      → профиль текущего пользователя
PUT    /api/v1/users/me                      → обновить профиль (username, firstName, lastName)
POST   /api/v1/users/me/avatar/upload-url    → получить presigned URL для загрузки аватарки
POST   /api/v1/users/me/avatar/confirm       → подтвердить загрузку аватарки
POST   /api/v1/users/me/change-password      → смена пароля (требует currentPassword)
```

### Публичные профили

```
GET    /api/v1/users/{id}                    → публичный профиль пользователя (STUDENT видит)
```

### Регистрация и восстановление пароля

```
POST   /api/v1/users/register                → регистрация нового пользователя
POST   /api/v1/users/forgot-password         → запрос восстановления пароля (email)
```

### Административные (только ADMIN)

```
GET    /api/v1/admin/users                   → список пользователей (фильтрация, сортировка, пагинация)
GET    /api/v1/admin/users/{id}              → профиль пользователя (полный, с флагом blocked)
PUT    /api/v1/admin/users/{id}              → редактировать профиль (username, firstName, lastName, avatarUrl)
POST   /api/v1/admin/users/{id}/avatar/upload-url  → presigned URL для аватарки пользователя
POST   /api/v1/admin/users/{id}/avatar/confirm     → подтвердить загрузку
POST   /api/v1/admin/users/{id}/block        → заблокировать пользователя
POST   /api/v1/admin/users/{id}/unblock      → разблокировать пользователя
```

### GET /api/v1/users/me — Response

```json
{
  "id":        "uuid",
  "username":  "ivan_petrov",
  "email":     "ivan@example.com",
  "firstName": "Иван",
  "lastName":  "Петров",
  "avatarUrl": "http://minio:9000/avatars/uuid/uuid",
  "role":      "STUDENT",
  "blocked":   false,
  "createdAt": "2025-01-01T00:00:00Z"
}
```

### GET /api/v1/admin/users — Request params

```
page     int     0-based (default 0)
size     int     default 20, max 100
sort     string  createdAt,desc | username,asc | lastName,asc
search   string  поиск по username, firstName, lastName, email (ILIKE)
role     string  STUDENT | ADMIN
blocked  boolean фильтр по флагу блокировки
```

### POST /api/v1/admin/users/{id}/block — Response

```json
{ "id": "uuid", "blocked": true, "updatedAt": "2025-05-30T12:00:00Z" }
```

---

## 8. Блокировка пользователя

Блокировка атомарна через тот же Outbox Pattern:

```java
@Transactional
public void blockUser(UUID targetId, UUID adminId) {
    log.trace("blockUser: targetId={}, adminId={}", targetId, adminId);

    UserProfile profile = profileRepository.findById(targetId)
            .orElseThrow(() -> new UserNotFoundException(targetId));

    if (profile.isBlocked()) {
        throw new UserAlreadyBlockedException(targetId);
    }

    profile.setBlocked(true);
    profile.setUpdatedAt(Instant.now());
    profileRepository.save(profile);

    outboxRepository.save(OutboxEvent.builder()
            .aggregateId(targetId)
            .eventType(OutboxEventType.USER_BLOCKED)
            .payload(toJson(Map.of("enabled", false)))
            .status(OutboxStatus.PENDING)
            .build());

    log.debug("User blocked: targetId={}, by adminId={}", targetId, adminId);
}
```

`KeycloakAdminService` обрабатывает все типы событий через единый метод `apply()`:

```java
// sm/selflearn/samskrtam/user/outbox/KeycloakAdminService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final AdminTokenProvider tokenProvider;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    public void apply(OutboxEvent event) {
        log.trace("apply: eventType={}, aggregateId={}", event.getEventType(), event.getAggregateId());

        String userId = event.getAggregateId().toString();
        Map<String, Object> payload = parsePayload(event.getPayload());

        switch (event.getEventType()) {
            case USER_REGISTERED -> createKeycloakUser(payload);
            case PROFILE_UPDATED -> updateKeycloakUser(userId, payload);
            case USER_BLOCKED    -> setEnabled(userId, false);
            case USER_UNBLOCKED  -> setEnabled(userId, true);
        }
    }

    // USER_REGISTERED — создание пользователя в Keycloak
    private void createKeycloakUser(Map<String, Object> payload) {
        // payload: { username, email, firstName, lastName, password }
        Map<String, Object> keycloakUser = Map.of(
                "username",      payload.get("username"),
                "email",         payload.get("email"),
                "firstName",     payload.get("firstName"),
                "lastName",      payload.get("lastName"),
                "enabled",       true,
                "emailVerified", false,
                "credentials",   List.of(Map.of(
                        "type",      "password",
                        "value",     payload.get("password"),
                        "temporary", false
                ))
        );

        restClient.post()
                .uri(adminUrl() + "/users")
                .header("Authorization", "Bearer " + tokenProvider.getToken())
                .body(keycloakUser)
                .retrieve()
                .toBodilessEntity();
    }

    // PROFILE_UPDATED — синхронизация имени, фамилии, username
    private void updateKeycloakUser(String userId, Map<String, Object> payload) {
        // payload: { firstName, lastName, username }
        // Keycloak PATCH не поддерживается — используем PUT с частичными полями
        Map<String, Object> representation = new HashMap<>();
        if (payload.containsKey("firstName")) representation.put("firstName", payload.get("firstName"));
        if (payload.containsKey("lastName"))  representation.put("lastName",  payload.get("lastName"));
        if (payload.containsKey("username"))  representation.put("username",  payload.get("username"));

        restClient.put()
                .uri(adminUrl() + "/users/" + userId)
                .header("Authorization", "Bearer " + tokenProvider.getToken())
                .body(representation)
                .retrieve()
                .toBodilessEntity();

        log.debug("Keycloak user updated: userId={}, fields={}",
                userId, representation.keySet());
    }

    // USER_BLOCKED / USER_UNBLOCKED — включение/отключение аккаунта
    private void setEnabled(String userId, boolean enabled) {
        restClient.put()
                .uri(adminUrl() + "/users/" + userId)
                .header("Authorization", "Bearer " + tokenProvider.getToken())
                .body(Map.of("enabled", enabled))
                .retrieve()
                .toBodilessEntity();

        log.debug("Keycloak user enabled={}: userId={}", enabled, userId);
    }

    private String adminUrl() {
        return keycloakUrl + "/admin/realms/" + realm;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new OutboxProcessingException("Failed to parse payload: " + json, e);
        }
    }
}
```

> **Замечание по Keycloak Admin API:** Keycloak не поддерживает PATCH для пользователей — только PUT. При PUT незаполненные поля сбрасываются в null. Поэтому `updateKeycloakUser` передаёт только те поля, которые есть в payload, а не весь объект пользователя. Это безопасно для частичного обновления.

Заблокированный пользователь не может получить новый токен — Keycloak отклонит логин. Уже выданные токены действуют до истечения TTL (обычно 5 минут).

---

## 9. Backend структура

```
sm/selflearn/samskrtam/user/
├── Application.java
├── controller/
│   ├── UserController.java          ← /users/me, /users/{id}
│   ├── RegistrationController.java  ← /users/register, /users/forgot-password
│   └── AdminUserController.java     ← /admin/users/**
├── service/
│   ├── UserProfileService.java      ← CRUD профиля + Outbox
│   ├── RegistrationService.java     ← регистрация через Keycloak Admin API
│   ├── PasswordService.java         ← смена/восстановление пароля
│   ├── UserBlockService.java        ← блокировка/разблокировка + Outbox
│   └── AvatarService.java           ← MinIO presigned URL, confirm
├── outbox/
│   ├── OutboxProcessor.java         ← @Scheduled, читает PENDING события
│   └── KeycloakAdminService.java    ← применяет события к Keycloak Admin API
├── repository/
│   ├── UserProfileRepository.java
│   └── OutboxEventRepository.java
├── model/
│   ├── UserProfile.java
│   ├── OutboxEvent.java
│   ├── OutboxEventType.java
│   ├── OutboxStatus.java
│   └── UserRole.java
└── dto/
    ├── UserProfileResponse.java
    ├── PublicProfileResponse.java
    ├── UpdateProfileRequest.java
    ├── RegisterRequest.java
    ├── ChangePasswordRequest.java
    ├── ForgotPasswordRequest.java
    ├── UploadUrlResponse.java
    ├── AvatarConfirmResponse.java
    ├── BlockUserResponse.java
    └── AdminUserListResponse.java
```

---

## 10. application.yml

```yaml
server:
  port: ${USER_SERVICE_PORT}

spring:
  application:
    name: user-service
  threads:
    virtual:
      enabled: true
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_POOL_MAX_SIZE}
      minimum-idle: ${DB_POOL_MIN_IDLE}
      connection-timeout: ${DB_POOL_CONNECTION_TIMEOUT_MS}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: users
  flyway:
    schemas: users

keycloak:
  url:           ${KEYCLOAK_URL}
  realm:         ${KEYCLOAK_REALM}
  client-id:     ${KEYCLOAK_CLIENT_ID}
  client-secret: ${KEYCLOAK_CLIENT_SECRET}

minio:
  url:        ${MINIO_URL}
  access-key: ${MINIO_ACCESS_KEY}
  secret-key: ${MINIO_SECRET_KEY}
  public-url: ${MINIO_PUBLIC_URL}
  bucket:
    avatars:    ${MINIO_BUCKET_AVATARS}
    documents:  ${MINIO_BUCKET_DOCUMENTS}

outbox:
  processor:
    interval-ms: ${OUTBOX_PROCESSOR_INTERVAL_MS}

# JWT не валидируется — сервис доверяет заголовкам X-User-* от Gateway.
```

---

## 11. Acceptance Criteria

- [ ] `GET /users/me` возвращает профиль текущего пользователя по `X-User-Id`
- [ ] STUDENT не может изменить email
- [ ] STUDENT видит публичный профиль другого пользователя (`GET /users/{id}`)
- [ ] STUDENT не имеет доступа к `/admin/**` → 403
- [ ] Загрузка аватарки: presigned URL → PUT в MinIO → confirm → avatar_url обновлён
- [ ] Невалидный content-type при запросе upload-url → 400
- [ ] Блокировка через ADMIN: `blocked=true` в БД + `enabled=false` в Keycloak (через Outbox)
- [ ] Заблокированный пользователь не может залогиниться (Keycloak отклоняет)
- [ ] Outbox: при недоступном Keycloak — retry до 5 раз, затем FAILED + ERROR лог
- [ ] Outbox: данные в БД и Keycloak консистентны после восстановления Keycloak
- [ ] Список пользователей: пагинация, сортировка, поиск по username/email/имени
- [ ] Логин возможен по email и по username (настройка Keycloak realm)
- [ ] Смена username — синхронизируется в Keycloak через Outbox
- [ ] `GET /users/me` вызывается после логина — содержит avatarUrl, firstName, lastName для header

---

## 12. Открытые вопросы

- [ ] Максимальный размер аватарки — ограничивать на уровне presigned URL или MinIO политики?
- [ ] Удаление старой аватарки из MinIO при загрузке новой?
- [ ] Outbox processor — отдельный поток или `@Scheduled` достаточно?
- [ ] TTL для FAILED outbox событий — когда очищать?
- [ ] При блокировке — инвалидировать уже выданные токены немедленно (Keycloak session logout) или ждать истечения TTL?
- [ ] Поле `search` в списке пользователей — полнотекстовый поиск PostgreSQL или ILIKE достаточно?