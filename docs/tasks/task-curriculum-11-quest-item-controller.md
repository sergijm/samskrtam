# Задача: curriculum-service — REST-контроллер QuestItem (API v2)

**Что:** `QuestItemController` — чтение готовых заданий и админский запуск регенерации.
**Зачем:** См. `docs/services/curriculum-quest-items.md` §6.

## Зависит от
task-curriculum-09-quest-item-entities.md, task-curriculum-10-declension-generator.md

## Шаги

1. DTO (пакет `sm.selflearn.samskrtam.curriculum.questitem.dto`): `QuestItemDto(UUID id, String itemType, String answerMode, String prompt, String correctAnswer, List<String> distractors, Object payload)` — `correctAnswer` возвращать `null` в ответе клиенту, если answerMode = MATCHING (не отдавать ответ вперёд, но payload с парами отдаём — сопоставление проверяется на бэкенде отдельным эндпоинтом вне периметра этой задачи, см. открытые вопросы `curriculum-quest-items.md` §7 — на этом шаге просто не включать `correctAnswer` в сериализацию для MATCHING).
2. `QuestItemMapper` (MapStruct) `QuestItem → QuestItemDto`, `distractors`/`payload` — десериализация из jsonb-строки в `List<String>`/`Object` через `ObjectMapper` (внедрить бин `ObjectMapper`, если его ещё нет в контексте curriculum-service).
3. `@RestController @RequestMapping("/api/v2/curriculum/quest-items")`.
4. `GET /` (`?topicId=&itemType=&limit=`, `limit` default 20, max 100): валидация — `topicId` и `itemType` обязательны (`400`, если нет); если темы не существует — `404`; иначе — случайная выборка `limit` строк (`questItemRepository.findByTopicIdAndItemType(topicId, itemType, PageRequest.of(0, limit, Sort.by("id")))` — для настоящей случайности предпочтительно `ORDER BY random() LIMIT :limit` через `@Query(nativeQuery = true)`, реализовать нативным запросом в `QuestItemRepository`, не через `Pageable` с сортировкой по id).
5. `POST /regenerate` (`?topicId=&itemType=`) — `@PreAuthorize("hasRole('ADMIN')")`; если `itemType` — одно из 4 типов склонения, вызвать `questItemRepository.deleteByTopicIdAndItemType(...)` + соответствующий метод `DeclensionQuestItemBatchGenerator` (по `itemType` определить, какие из 3 групп методов генератора вызывать — `DECLENSION_FORM`+`DECLENSION_FORM_CHOICE` генерируются вместе одним проходом, см. task-10 шаги 4–5); тему найти по `topicId`, `morphologyClassCode` определить по `topic.code` (соглашение: код темы совпадает с кодом класса основ, например тема `a-stem-masc` ↔ `morphology_class.code = 'a-stem-masc'` — если это не так для каких-то тем, потребуется отдельное маппинг-поле, зафиксировать как открытый вопрос в PR, не выдумывать по ходу); вернуть `202 Accepted` с телом `{ "generated": <int> }`.
6. Добавить обработку `EntityNotFoundException → 404`, `IllegalArgumentException` (неизвестный `itemType`) `→ 400` в существующий `@RestControllerAdvice` curriculum-service.

## Критерии готовности (DoD)
- [ ] `GET /api/v2/curriculum/quest-items?topicId=&itemType=DECLENSION_FORM&limit=5` возвращает не более 5 строк, каждый раз в разном порядке при повторном вызове (не детерминированная сортировка)
- [ ] Ответ для `itemType=DECLENSION_MATCH` не содержит поля `correctAnswer` со значением (проверить сериализованный JSON)
- [ ] `POST /regenerate` без роли ADMIN — `403`
