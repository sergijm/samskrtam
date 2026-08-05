# Задача: curriculum-service — модуль lexicon: pool/resolve и UserLexemeProgress API

**Что:** Реализовать контракт `docs/openapi/curriculum/lexicon-api.yaml` — `GET /lexicon/pool/resolve`, `GET`/`PATCH /lexicon/users/{userId}/progress...`.
**Зачем:** Это единственная точка входа, которой пользуется quiz-service для генерации lexical-сессий, см. `docs/services/lexical-quizzes.md` §0/§5.

## Зависит от
task-curriculum-08-lexicon-entities.md

## Шаги

### Pool resolution
1. `LexemePoolService.resolve(PoolCriteria criteria)`:
   - построить JPA/Criteria-запрос по всем переданным измерениям (`topicIds` через `LexicalTopicBinding`, `frequencyRankMin/Max` через `LexemeFrequency`, `posCodes` через `LexemePos`, `morphologyClassCodes` через `LexemeMorphology`, `sourceId`/`sourceLocationPrefix` через `SourceOccurrence`, `collectionId` через `UserCollectionItem`) — все условия AND между измерениями, OR внутри одного измерения (`lexical-quizzes.md` §3);
   - если `excludeMasteredForUserId` задан — исключить лексемы с `UserLexemeProgress.masteryScore >= 90` для этого пользователя (LEFT JOIN + фильтр, отсутствие строки прогресса не исключает лексему);
   - применить квоту на тему при `topicIds.size() > 1` (не более `ceil(poolLimit / topicIds.size()) + 2` от одной темы — если после общего запроса какая-то тема перепредставлена, обрезать её выборку до квоты, оставив случайные);
   - финальный reshuffle с ограничением «не более 2 подряд одного `posCode`» (после квоты).
2. `GET /api/v2/lexicon/pool/resolve` контроллер — маппинг query-параметров, вызов `LexemePoolService`, ответ `List<LexemeCandidateDto>` (поля — по `lexicon-api.yaml` `LexemeCandidate`, включая до 3 `wordForms` на лексему, если есть).

### Progress
3. `GET /api/v2/lexicon/users/{userId}/progress?lexemeIds=...` — batch fetch, отсутствующие `lexemeId` просто не попадают в ответ (не 404, не создаётся пустая строка).
4. `PATCH /api/v2/lexicon/users/{userId}/progress/{lexemeId}` body `{correct: boolean}`:
   - если строки `UserLexemeProgress` нет — создать (`exposureCount=1`, `correctCount`/`incorrectCount` по `correct`);
   - если есть — `exposureCount++`, `correctCount++`/`incorrectCount++`, пересчитать `masteryScore`/`nextReviewAt` по формуле ADR-007 (переиспользовать ту же логику/константы, что quiz-service использует для `grammar_form_score` — согласовать точные константы с реализацией quiz-service при интеграции, см. `lexical-quizzes.md` §6);
   - `lastSeenAt = now()`.
5. Оба эндпоинта требуют, чтобы вызывающий сервис (quiz-service) прокидывал реальный `userId` пользователя, от чьего лица идёт сессия — не текущего вызывающего сервисного токена; способ передачи (service-to-service auth) — по существующему внутреннему контракту проекта (см. `content-service.md` internal REST раздел как образец), не переизобретается здесь.

## Критерии готовности (DoD)
- [ ] Интеграционный тест: `pool/resolve` с 2 `topicIds` возвращает слова из обеих тем, ни одна не занимает > квоты
- [ ] Интеграционный тест: `excludeMasteredForUserId` реально исключает мастерные лексемы, не влияя на пул для другого пользователя
- [ ] `PATCH .../progress/{lexemeId}` корректно создаёт первую строку и инкрементирует существующую
- [ ] Ответы соответствуют `docs/openapi/curriculum/lexicon-api.yaml`
