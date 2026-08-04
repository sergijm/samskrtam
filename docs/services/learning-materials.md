# Learning Materials — теория, литература, сканы, видео

> Связанные файлы: [curriculum.md](./curriculum.md) · [quest-catalog.md](./quest-catalog.md) · [content-service/eamenau.md](./content-service/eamenau.md)

Квест (`Quest`) — только тренажёр (см. [quest-engine.md](./quest-engine.md)). Теория,
ссылки на литературу, сканы страниц учебников и видео — отдельная сущность, привязанная
к `Topic` (см. [curriculum.md §1](./curriculum.md#1-модель)), а не к отдельному `Quest`,
чтобы не дублировать объяснение одной темы под каждый из нескольких `Quest` внутри неё.

---

## 1. Модель

```java
public enum MaterialType { TEXT, EXTERNAL_LINK, SCANNED_PAGE, VIDEO_LINK }

public record SourceCitation(
    String author, String title, String publisher, String year, String pages,
    boolean isPublicDomain
) { }

public record LearningMaterial(
    UUID id, UUID topicId, MaterialType type,
    String titleRu, String titleEn,
    String body,              // markdown — для TEXT, и перевод/расшифровка для SCANNED_PAGE
    String url,                // внешняя ссылка — для EXTERNAL_LINK и VIDEO_LINK
    String mediaObjectKey,     // ключ в MinIO — для SCANNED_PAGE
    SourceCitation citation,
    int sortOrder
) { }
```

| Тип | Что хранит | Где физически |
|---|---|---|
| **TEXT** | Markdown-объяснение (правило, парадигма, комментарий) | текст в БД content-service |
| **EXTERNAL_LINK** | Ссылка на литературу/статью + библиография | только `url` + `citation`, физически ничего не храним |
| **SCANNED_PAGE** | Скан страницы учебника + перевод/расшифровка рядом | изображение в MinIO (`mediaObjectKey`, presigned URL — по аналогии с аватарками в user-service), перевод — `body` того же материала |
| **VIDEO_LINK** | Ссылка на внешнее видео (YouTube и т.п.) | только `url`, эмбед на фронте |

Собственный хостинг видео (`VIDEO_UPLOAD`) в первую версию не входит — транскодирование
и раздача видео требуют отдельной инфраструктуры (CDN), решение отложено до появления
конкретной необходимости.

---

## 2. Авторское право сканов

`SourceCitation.isPublicDomain` — обязательное поле для `SCANNED_PAGE`. Большинство
изданий по санскриту всё ещё под авторским правом; поле определяет видимость материала
(публично или только для собственного использования) на уровне продукта, а не только
формальная атрибуция. Решение о фактическом уровне доступа (`accessLevel`) для не-public-domain
сканов — открытый вопрос §5.

---

## 3. API

```
GET /api/v1/content/topics/{topicId}            → Topic { materials[], questIds[] }
GET /api/v1/content/materials/{materialId}       → полная карточка материала
                                                     (для SCANNED_PAGE — presigned URL
                                                     полноразмерного изображения)
```

quiz-service не участвует — материалы вне его домена, прогресс/повторение по ним не
считается. Опциональная лёгкая пометка «прочитано» (`materialViewed(userId, materialId)`)
— не через алгоритм повторения (`quest-engine.md §3`), простой факт без интервалов.

---

## 4. UI

`TopicPage` — два таба:

- **«Теория»** — материалы по `sortOrder`: markdown как есть; `SCANNED_PAGE` — просмотрщик
  изображения (zoom) с переводом рядом/под ним; `EXTERNAL_LINK` — карточка с библиографией;
  `VIDEO_LINK` — embed-плеер.
- **«Тренировка»** — список `Quest` этой темы (текущий `LessonPage`).

Материал открывается независимо от прогресса — не гейтится тренировкой.

---

## 5. Порядок внедрения и открытые вопросы

Первые кандидаты — темы, где объяснение уже необходимо для осмысленной тренировки:
`KARAKA_CASE_CHOICE` (без теории роли непонятны) и `SANDHI_SPLIT` — там уже есть готовый
источник, учебник Eméneau (`content-service/eamenau.md`), не с нуля.

- Конкретный `accessLevel` для не-public-domain сканов (публично / только владельцу /
  по подписке) — решение на уровне продукта, не архитектуры.
- Нужен ли self-hosted видео (`VIDEO_UPLOAD`) — отложено до конкретного запроса.
- Трекинг «прочитал теорию» (`materialViewed`) — детали API и связь с картой прогресса
  (`curriculum.md §4`) не проработаны, будет уточнено при реализации Dashboard.
