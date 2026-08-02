# Architecture — топология, монорепозиторий, ключевые решения

> Связанные файлы: [README.md](README.md) · [conventions.md](conventions.md) · [infra/keycloak.md](infra/keycloak.md) · [services/api-gateway.md](services/api-gateway.md)

---

## 1. Соглашения по именованию

| Элемент | Паттерн | Пример |
|---|---|---|
| Base package | `sm.selflearn.samskrtam.<сервис>` | `sm.selflearn.samskrtam.dictionary` |
| Gradle group | `sm.selflearn` | — |
| artifactId | `samskrtam-<сервис>-service` | `samskrtam-dictionary-service` |
| Main class | `sm.selflearn.samskrtam.<сервис>.Application` | — |
| Docker image | `registry.gitlab.local/samskrtam/<сервис>-service` | — |

### Пакеты по сервисам

| Сервис | Язык / модель | Base package | Описание |
|---|---|---|---|
| api-gateway | Java 21, WebFlux | `sm.selflearn.samskrtam.gateway` | Единая точка входа, аутентификация, маршрутизация |
| user-service | Java 21, Virtual Threads | `sm.selflearn.samskrtam.user` | Профили, регистрация, Keycloak-прокси |
| content-service | Java 21, Virtual Threads | `sm.selflearn.samskrtam.content` | Настройки и содержание уроков/квизов |
| quiz-service | Java 21, WebFlux + R2DBC | `sm.selflearn.samskrtam.quiz` | Прохождение квизов, Outbox → Kafka |
| statistics-service | Java 21, Kafka Streams | `sm.selflearn.samskrtam.statistics` | Расчёт статистики и лидерборда |
| dictionary-service | Java 21, Virtual Threads | `sm.selflearn.samskrtam.dictionary` | Поиск по словарю, cache-aside |
| sangraha-service | Java 21, Virtual Threads | `sm.selflearn.samskrtam.sangraha` | Санскритские произведения, LLM-анализ стихов. См. [services/sangraha-service.md](services/sangraha-service.md) |
| shared/samskrtam-dtos | Java 21 | `sm.selflearn.samskrtam.quiz` | DTO и Kafka-события для квизов, контента, статистики (`QuizAnsweredEvent`, `QuizSessionStatusChangedEvent`, `StatisticEvent`) |
| shared/common-dto | Java 21 | `sm.selflearn.samskrtam.common` | Общие DTO, используемые всеми сервисами |

---

## 2. Физическая инфраструктура

```
┌─────────────────────────────────────────────────────────────┐
│                    Локальная сеть                            │
│                                                             │
│  ┌──────────────────┐     ┌──────────────────────────────┐  │
│  │   VM-1: GitLab   │     │   Kubernetes Cluster         │  │
│  │                  │     │                              │  │
│  │  GitLab CE       │     │  VM-2: control-plane         │  │
│  │  GitLab Runner   │◄────│  VM-3: worker-1              │  │
│  │  Container       │     │  VM-4: worker-2              │  │
│  │  Registry        │     │  VM-5: worker-3              │  │
│  │                  │     │                              │  │
│  │  gitlab.local    │     │  Portainer (управление)      │  │
│  └──────────────────┘     └──────────────────────────────┘  │
│                                                             │
│  Рабочая машина: Docker Compose (локальная разработка)      │
└─────────────────────────────────────────────────────────────┘
```

| VM | Роль | Сервисы |
|---|---|---|
| VM-1 | GitLab | GitLab CE, GitLab Runner, Container Registry |
| VM-2 | k8s control plane | kube-apiserver, etcd, scheduler, GitLab Agent |
| VM-3 | k8s worker-1 | рабочая нагрузка |
| VM-4 | k8s worker-2 | рабочая нагрузка |
| VM-5 | k8s worker-3 | рабочая нагрузка + Portainer Agent |

---

## 3. Ключевые архитектурные решения

Раздел фиксирует принятые проектные решения в их финальном виде — как основу для дальнейшей работы, а не как историю изменений.

### 3.1 Разделение аутентификации между Gateway и user-service

**Gateway** отвечает за протокол OAuth2/OIDC: login (ROPC), refresh, logout, редиректы Google/Mail.ru, Authorization Code flow. **user-service** отвечает за жизненный цикл аккаунта: регистрацию, восстановление и смену пароля, верификацию email, инвайты — через Keycloak Admin API. Граница: протокол OAuth2 — зона Gateway, бизнес-логика аккаунта — зона user-service.

### 3.2 Семантика Quiz / Lesson / Activity

**Lesson** — единица контента (склонение, словарный урок). **Quiz** — выборка вопросов из урока на сессию. **QuizSession** — прохождение квиза пользователем. **Activity** — будущая абстракция для типов активности за пределами квизов (после M5). В коде: `LessonRepository`/`LessonContentService` (а не `QuizRepository`), `lessonId` в статистике (а не `quizId`). Роут `/api/v1/quiz/` и имена Kafka-топиков не связаны с этим переименованием и не меняются.

### 3.3 Хранение окончаний склонений

Таблица `content.case_endings (vowel_type, gender, case_type, number_type, ending)` — эталон падежных окончаний, ключ `(vowel_type, gender, case_type, number_type)`. Для уроков без родового различия (`declensions-i/u/r`) `gender = UNSPECIFIED`. quiz-service читает окончание по ключу при генерации вопроса и сравнивает ответ напрямую.

Уроки с двумя родами (`declensions-i/u/r`, одинаковые окончания у обоих родов) состоят из 24 вопросов (8 caseType × 3 numberType), `gender = UNSPECIFIED`. Уроки с одним родом (`declensions-a-masc/neut/fem`, `declensions-i-long/u-long`) также содержат 24 вопроса, но поле `gender` обязательно.

Для `vowel_type` из набора `I/I_LONG/U/U_LONG/R` окончания одинаковы для всех родов, поэтому хранятся с `gender = UNSPECIFIED`; прогресс при этом агрегируется раздельно по фактическому роду слова.

### 3.4 Местоимения — через существующий itemType DECLENSION_FORM

Местоимения реализованы как дополнительные значения `vowelType` (`PRON_AHAM`, `PRON_TVAM`, `PRON_TAD`, `PRON_ETAD`, `PRON_IDAM`, `PRON_KIM`, `PRON_YAD`) в существующих таблицах `content.declension_stems`/`content.declension_forms`/`content.case_endings`; отдельный itemType не заводится, `external_ref_id` по-прежнему ссылается на `case_endings.id`.

- `declension_forms` хранит готовые словоформы (не суффикс + основа), поэтому супплетивные парадигмы (`aham → mama → mahyam`, `tad/etad/idam/kim/yad`) укладываются в модель без изменений схемы.
- Личные местоимения (`aham`/`tvam`) не различают род — `gender = UNSPECIFIED`, по аналогии с i/u/ṛ-основами (§3.3).
- Указательные/вопросительные/относительные (`tad/etad/idam/kim/yad`) различают три рода — по три стема на класс, как a-основы одного рода.
- Для форм без вычленяемого окончания (`aham`/`tvam`) `case_endings.ending` хранит словоформу целиком; вес `ENDING_MATCH` для единственной формы в группе омонимии обнуляется существующим алгоритмом без дополнительного кода.
- Уроки местоимений: `pronouns-personal`, `pronouns-demonstrative`, `pronouns-interrogative`, `pronouns-relative`, `pronouns-reflexive`. `pronouns-reflexive` (ātman) сознательно дублирует парадигму `declensions-a-masc` под отдельным slug — независимость уроков предпочтена переиспользованию.
- Вне контракта: энклитические формы личных местоимений (`me/te/nau/vaḥ` и т.д.) и несклоняемое `svayam` — не входят в 24 стандартные словоформы `DECLENSION_FORM`, квизом не покрываются, представлены как статические заметки на странице урока.

### 3.5 sangraha-service: произведения, LLM-анализ стихов, синхронизация лексики через REST

Сервис `sangraha-service` (Java 21, Virtual Threads, схема `sangraha`) хранит иерархию Work → Chapter → Verse и выполняет LLM-анализ стиха (OpenAI-совместимый) строго через tool calling (`submit_verse_analysis`), без парсинга свободного текста.

Синхронизация лексики с content-service выполняется синхронным REST-вызовом `POST content-service/content/internal/sangraha/vocabulary-quiz` — канал «один producer, один consumer» не требует Kafka. Иерархия `work.slug → chapter.slug` маппится на `VocabularyCategory.code` в content-service для тематической группировки лексики. Слова дедуплицируются по `(wordIast, stem)`; версионирование анализа не хранится (перезапись). Запись — только для роли ADMIN. Порт — из env (см. §6 `services/sangraha-service.md`).

Синхронизация лексики со стихом инициируется явным действием пользователя, а не автоматически при анализе стиха:

- Кнопка «Изучить» на VersePage вызывает `POST /verses/{verseId}/vocabulary-quiz`. Если по стиху уже был клик — возвращается закэшированный `verse.vocabularyQuizSlug` без обращения к content-service; иначе sangraha-service синхронно, в рамках того же HTTP-запроса, вызывает content-service, получает `quizSlug`/`quizId`/`quizStatus` и кэширует их.
- Квиз создаётся на уровне **стиха**, а не произведения: `Quiz.slug = "{workSlug}.{chapterSlug}.verse-{verseId}"`, slug детерминирован, что даёт идемпотентность без ретраев.
- `VocabularyCategory` (work → chapter) остаётся общим механизмом тематической классификации лексики и используется независимо от квиза по стиху: слово одновременно входит и в свой квиз-по-стиху (`VocabularyWord` ↔ `Quiz`), и в тематическую категорию произведения/главы (`VocabularyWord` ↔ `VocabularyCategory` через `VocabularyWordCategory`) — это ортогональные связи.
- `quizStatus` (`CREATED`/`EXISTING`) — единственный сигнал, которым content-service может обозначить, нужен ли фильтр `statusFilter=NEW` при первом старте сессии: `CREATED` → `NEW`, `EXISTING`/кэш-хит → без фильтра. Фронтенд стартует сессию сразу по `quizId` через `POST /quiz/vocabulary/sessions/start-or-resume?lessonId={quizId}&statusFilter=...`.
- Надёжная доставка обеспечивается на уровне HTTP-запроса, инициированного пользователем: отдельного фонового Outbox/relay для этого потока нет, повтор равен повторному клику.

### 3.6 Единая таблица прогресса quiz_item_score

Прогресс по всем типам квизов хранится в одной таблице `quiz.quiz_item_score` с составным ключом `(user_id, item_type, external_ref_id)`. `item_type` — открытое для расширения перечисление (`VOCABULARY_WORD`, `DECLENSION_FORM` и др.). `external_ref_id` — UUID сущности в content-service, без физического FK. Поля таблицы: `id`, `user_id`, `item_type`, `external_ref_id`, `score` (0–100), `stability`, `last_answered_at`, `last_mistake_at`, `consecutive_mistakes`, `next_review_at`, `updated_at`.

Физические внешние ключи между схемами `quiz` и `content` не используются, поскольку это разные микросервисы с разными базами данных: целостность ссылок обеспечивается на уровне приложения (`ContentClient` проверяет существование `external_ref_id`) и эвентуально — предпочтительный механизм для content-service — soft-delete.

Статус единицы (`NEW`/`LEARNING`/`DIFFICULT`/`MASTERED`) не хранится, а вычисляется из текущего `score` при чтении: нет строки → `NEW`; `score ≤ difficultUpperThreshold` → `DIFFICULT`; между порогами → `LEARNING`; `score ≥ masteredLowerThreshold` → `MASTERED`. Time-decay не реализуется: `score` не убывает со временем, забывание проявляется через формулу score при следующей ошибке после контрольного показа (см. §2.5 `quiz-generator-spec.md`). `masteredLowerThreshold = 90` для обоих `itemType`; `difficultUpperThreshold` остаётся открытым параметром калибровки (см. §6 `quiz-generator-spec.md`).

Единственное исключение из «без time-decay»: единица со статусом `MASTERED` (`score ≥ 90`), у которой `nextReviewAt ≤ now` (просрочен контрольный показ, см. `masteredCooldown`, §3 `quiz-generator-spec.md`), отображается на LessonPage как бакет **REVIEW** вместо `MASTERED` — это чисто отображаемый статус, для генератора и алгоритма отбора единица остаётся в бакете `MASTERED`. Наличие ≥1 единицы в REVIEW включает кнопку «Повторить» на LessonPage.

Прогресс склонений общий для всех основ с одинаковым `(vowel_type, gender, case_type, number_type)`: `external_ref_id` для `DECLENSION_FORM` ссылается на `content.case_endings.id` — эталонную связку параметров, а не на конкретную основу, поэтому прогресс не дублируется там, где окончания совпадают (см. §3.3).

Ответы пользователя хранятся отдельно, в `quiz.quiz_answers` (агрегация nSuccess/nAll — оттуда); `quiz_item_score` — единственное хранилище именно прогресса. Алгоритм отбора вопросов (`generate()`) единый для всех `itemType`, без ветвлений по типу. Планирование `next_review_at` — отдельная формула SRS-интервалов (открытый вопрос, см. §4).

---

## 4. Открытые вопросы

- Secrets management (K8s Secrets vs Vault)
- CHANGELOG (ручной vs semantic-release)
- Grafana dashboards (JSON в репозитории vs ручная настройка)
- ArchUnit-тесты: shared/arch-rules vs дублирование в каждом сервисе
- Testcontainers reuse mode
- Формула SRS-интервалов для `next_review_at` (§3.6)
- Конкретные значения `difficultUpperThreshold` (по умолчанию 45) и `masteredLowerThreshold` (по умолчанию 80 для нового материала, 90 — финальное значение для расчёта REVIEW) калибруются на реальных данных
- Столбец `du. (m/n)` в справочной таблице окончаний a-основ (`frontend/src/data/aStemEndingsTable.ts`, `DeclensionEndingsReferenceTable`) объединяет мужской и средний род в одну колонку; корректно для instrumental/dative/ablative/genitive/locative (формы дв.ч. совпадают у обоих родов), но для nominative/accusative/vocative формы различаются по роду (муж. `-au`, ср. `-e`) — см. `docs/tasks/task-fix-astem-endings-table.md`. Решить: разбивать колонку на `du.m`/`du.n` для этих трёх строк или документировать ограничение в UI.
