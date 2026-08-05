# Задача: curriculum-service — применить seed 70 grammar-тем

**Что:** Подключить `V5__seed_grammar_topics.sql` (72 строки `Topic` с `domain=GRAMMAR`: 70 атомарных тем L0–L6 + 2 evergreen, 82 prerequisite-ребра).
**Зачем:** См. `docs/services/curriculum.md` §2 — раздробление с ~20 крупных тем до 60–80 атомарных.

## Зависит от
task-curriculum-02-migration.md, task-curriculum-08-lexicon-entities.md (миграция V5 применяется после V1–V4)

## Шаги
1. Файл `V5__seed_grammar_topics.sql` уже подготовлен — проверить, что применяется после `V1`–`V4` без конфликтов (не пересекается с lexicon-таблицами `V3`/`V4`, только `curriculum.topic`/`curriculum.topic_prerequisite`).
2. Прогнать на чистой БД, убедиться: 72 строки в `topic` (70 с `is_evergreen=false` + 2 `mixed-review`/`error-correction` с `is_evergreen=true`), 82 строки в `topic_prerequisite`, отсутствие циклов (`GET /api/v2/curriculum/graph` должен успешно вернуть 7 слоёв без ошибки).
3. **Известное упрощение, требующее последующей правки контентной командой:** `title_en` в этой миграции временно продублирован из `title_ru` для тем, где не было явно задано отдельное английское название (пометка в комментарии файла) — потребуется отдельный проход по переводу перед публикацией на английской локали; не блокирует работу схемы/API.
4. Сверить, что `code` каждой темы соответствует таблицам `curriculum.md` §2 (использовать как ключ для будущей привязки `LearningMaterial.topicId`, `learning-materials.md`).

## Критерии готовности (DoD)
- [ ] 72 темы, 82 ребра применились без ошибок
- [ ] `GET /api/v2/curriculum/graph` возвращает 7 непустых слоёв (L0 без входящих рёбер — корень)
- [ ] `GET /api/v2/curriculum/levels` возвращает `topicCount` по каждому уровню, совпадающий с таблицами §2 `curriculum.md` (8/11/10/12/11/10/8)
