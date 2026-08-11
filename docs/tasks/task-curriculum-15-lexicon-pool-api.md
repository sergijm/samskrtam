# Задача: curriculum-service — модуль lexicon: pool/resolve и UserLexemeProgress API

**Что:** Реализовать `GET /api/v2/lexicon/pool/resolve`, `GET`/`PATCH
/api/v2/lexicon/users/{userId}/progress...`.
**Зачем:** Единственная точка входа, которой пользуется quiz-service для
генерации lexical-сессий — см. `docs/services/lexical-quizzes.md` §0/§5.
**Важно:** пул отдаёт только `status = APPROVED` лексемы по умолчанию (см.
`lexicon-content-pipeline.md` §3) — жёсткий фильтр, не опциональный.

## Зависит от
task-curriculum-13-lexicon-schema-entities.md, task-curriculum-14-lexicon-sangraha-import.md

## Шаги

1. `PoolCriteria` DTO: `topicIds[]`, `frequencyRankMin/Max`, `posCodes[]`, `morphologyClassCodes[]`, `sourceId`, `sourceLocationPrefix`, `collectionId`, `excludeMasteredForUserId`, `poolLimit`.
2. `LexemePoolService.resolve(criteria)`, шаг 1: базовый запрос `status = APPROVED`, JOIN по каждому переданному измерению (AND между измерениями).
3. Шаг 2: внутри одного измерения — OR (например, несколько `topicIds` через `LexicalTopicBinding`).
4. Шаг 3: если `excludeMasteredForUserId` задан — LEFT JOIN `UserLexemeProgress`, исключить `masteryScore >= 90` (отсутствие строки прогресса не исключает лексему).
5. Шаг 4: квота на тему при `topicIds.size() > 1` — не более `ceil(poolLimit / topicIds.size()) + 2` лексем от одной темы (обрезка случайной выборкой внутри перепредставленной темы).
6. Шаг 5: финальный reshuffle — не более 2 подряд одного `posCode` после сортировки.
7. `GET /api/v2/lexicon/pool/resolve` контроллер — маппинг query-параметров, вызов сервиса, ответ `List<LexemeCandidateDto>` (лемма, глоссы, pos, gender, до 3 `wordForms`, если есть).
8. `GET /api/v2/lexicon/users/{userId}/progress?lexemeIds=...` — batch fetch, отсутствующие `lexemeId` просто не в ответе.
9. `PATCH /api/v2/lexicon/users/{userId}/progress/{lexemeId}` `{correct: boolean}`: создать строку, если её нет (`exposureCount=1`), иначе инкремент + пересчёт `masteryScore`/`nextReviewAt` по формуле ADR-007 (`lexical-quizzes.md` §6), `lastSeenAt = now()`.
10. Проверить, что `userId` в обоих эндпоинтах — реальный пользователь сессии (прокидывается quiz-service), не токен вызывающего сервиса — по существующему internal-контракту проекта.

## DoD
- [ ] `pool/resolve` с 2 `topicIds` возвращает слова из обеих тем, ни одна не превышает квоту
- [ ] `pool/resolve` никогда не возвращает `status != APPROVED`
- [ ] `excludeMasteredForUserId` исключает мастерные лексемы только для этого пользователя
- [ ] `PATCH .../progress/{lexemeId}` корректно создаёт и инкрементирует
