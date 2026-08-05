# Задача: curriculum-service — модуль lexicon: admin CRUD для таксономий/Source/UserCollection

**Что:** REST CRUD для `Lexeme`, справочников (`SemanticTopic`, `PartOfSpeech`, `MorphologyClass`, `FrequencyBand`), `Source`/`SourceOccurrence`, `UserCollection`, `LexicalTopicBinding`, `VocabularyQuizDefinition`.
**Зачем:** Наполнение и курирование данных — см. `docs/services/lexicon-content-pipeline.md` (наполнение), `docs/services/lexical-curriculum.md` §1 (binding), `docs/services/lexical-quizzes.md` §2 (VocabularyQuizDefinition).

## Зависит от
task-curriculum-08-lexicon-entities.md

## Шаги
1. `LexemeController` (`/api/v2/lexicon/lexemes`): `GET` (список с фильтрами по `status`/`posCode`/`semanticTopicId`, пагинация — каталог на 2000+ строк, в отличие от `Topic`, здесь пагинация обязательна), `GET /{id}` (с резолвленными таксономиями), `POST`/`PUT` (ADMIN, статус по умолчанию `DRAFT`), `PATCH /{id}/status` (ADMIN, переход `DRAFT→AI_ENRICHED→APPROVED|REJECTED`, см. content pipeline).
2. `LexemeTaxonomyController` — управление M:N связями лексемы: `PUT /lexemes/{id}/semantic-topics`, `PUT /lexemes/{id}/pos`, `PUT /lexemes/{id}/morphology` (полная замена набора, ADMIN).
3. Справочники (`SemanticTopic`/`PartOfSpeech`/`MorphologyClass`/`FrequencyBand`) — простой CRUD, ADMIN-only на запись, публичный `GET` (списком, дерево для `SemanticTopic` — `GET /semantic-topics/tree`).
4. `SourceController` (`/api/v2/lexicon/sources`): CRUD (ADMIN), `GET /{id}` возвращает `totalOccurrencesCache`/`uniqueLemmaCountCache`; `POST /{id}/occurrences/refresh-cache` (ADMIN, пересчитывает кэш через `COUNT`/`COUNT DISTINCT` по `source_occurrence`, см. `lexicon.md` §4); `POST /{id}/occurrences/batch` (ADMIN, батч-загрузка occurrences, используется content pipeline).
5. `UserCollectionController` (`/api/v2/lexicon/collections`, не ADMIN — обычный пользователь управляет своими): `GET` (только свои + `SHARED` чужие), `POST`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/items` (`{lexemeId, addedVia}`), `DELETE /{id}/items/{lexemeId}`.
6. `LexicalTopicController` (`/api/v2/lexicon/topics/{topicId}/binding`, ADMIN) — работает поверх уже существующего `Topic` (проверка `domain == LEXICON`, иначе 400): `GET` (список привязанных Lexeme), `PUT` (полная замена набора `lexemeIds`), `POST`/`DELETE` по одному `lexemeId`.
7. `VocabularyQuizDefinitionController` (`/api/v2/lexicon/quiz-definitions`, ADMIN на запись, публичный `GET`) — CRUD, валидация «ровно одно из `topicId`/`complexQuizId`/`frequencyRankMax`/`sourceId` заполнено» в зависимости от `kind` (`lexical-quizzes.md` §2).

## Критерии готовности (DoD)
- [ ] Пагинация на `GET /lexemes` работает корректно на 2000+ строках (проверить план запроса, индекс по `status`)
- [ ] `PUT /lexemes/{id}/semantic-topics` — идемпотентная полная замена, не накопление дублей
- [ ] `refresh-cache` даёт числа, совпадающие с прямым `COUNT`/`COUNT DISTINCT` по `source_occurrence`
- [ ] `VocabularyQuizDefinition` отклоняет создание с более чем одним заполненным из 4 взаимоисключающих полей (422)
