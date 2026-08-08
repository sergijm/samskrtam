# Задача: curriculum-service — модуль lexicon: admin CRUD

**Что:** REST CRUD для `Lexeme`, справочников (`SemanticTopic`, `PartOfSpeech`,
`MorphologyClass`, `FrequencyBand`), `Source`, `UserCollection`,
`LexicalTopicBinding`, `VocabularyQuizDefinition`.
**Зачем:** Ручной этап pipeline — донаполнение `semanticTopicId` и перевод
`CANDIDATE → APPROVED` (см. `lexicon-content-pipeline.md` §3).

## Зависит от
task-curriculum-13-lexicon-schema-entities.md

## Шаги

1. `LexemeController` `GET /api/v2/lexicon/lexemes` — список с фильтрами `status`, `posCode`, `semanticTopicId` (в т.ч. `semanticTopicId=null` — для очереди «без темы», см. `lexicon-content-pipeline.md` §3), обязательная пагинация (2000+ строк).
2. `GET /api/v2/lexicon/lexemes/{id}` — лексема + резолвленные таксономии.
3. `POST`/`PUT /api/v2/lexicon/lexemes` (ADMIN) — статус по умолчанию `DRAFT` при ручном создании.
4. `PATCH /api/v2/lexicon/lexemes/{id}/status` (ADMIN) — переход `DRAFT|CANDIDATE → APPROVED|REJECTED`; заблокировать переход в `APPROVED`, если `gender` не заполнен у `NOMINAL` POS или `lemmaIast`/`lemmaDevanagari` не проходят сверку транслитерации (`lexicon-content-pipeline.md` §3) — вернуть 422 с описанием причины.
5. `LexemeTaxonomyController`: `PUT /lexemes/{id}/semantic-topics`, `PUT /lexemes/{id}/pos`, `PUT /lexemes/{id}/morphology` — полная идемпотентная замена набора (ADMIN).
6. Справочники `SemanticTopic`/`PartOfSpeech`/`MorphologyClass`/`FrequencyBand` — CRUD (ADMIN на запись, публичный `GET`), `GET /semantic-topics/tree` — дерево.
7. `SourceController`: CRUD (ADMIN), `GET /{id}` с кэш-полями, `POST /{id}/occurrences/refresh-cache` (пересчёт `COUNT`/`COUNT DISTINCT`).
8. `UserCollectionController` (не ADMIN, свои + `SHARED` чужие): `GET`, `POST`, `PUT /{id}`, `DELETE /{id}`, `POST /{id}/items`, `DELETE /{id}/items/{lexemeId}`.
9. `LexicalTopicController /api/v2/lexicon/topics/{topicId}/binding` (ADMIN, проверка `Topic.domain == LEXICON`, иначе 400): `GET`, `PUT` (полная замена), `POST`/`DELETE` по одному `lexemeId`.
10. `VocabularyQuizDefinitionController` (ADMIN на запись, публичный `GET`) — CRUD, валидация «ровно одно из 4 взаимоисключающих полей заполнено» (422 при нарушении).

## DoD
- [ ] Пагинация на `GET /lexemes` корректна на 2000+ строках
- [ ] `PATCH /status` блокирует `APPROVED` при незаполненном обязательном `gender`/несверенной транслитерации
- [ ] `PUT /lexemes/{id}/semantic-topics` — идемпотентная замена, без накопления дублей
- [ ] `refresh-cache` даёт числа, совпадающие с прямым `COUNT`
- [ ] `VocabularyQuizDefinition` отклоняет создание с >1 заполненным взаимоисключающим полем (422)
