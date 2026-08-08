# Задача: sangraha-service — модуль lexicon-classification: admin review и export

**Что:** CRUD/review эндпоинты для `LemmaClassification` + internal
export-эндпоинт для curriculum-service.
**Зачем:** См. `docs/services/sangraha-service/lemma-classification.md` §4–§5.

## Зависит от
task-sangraha-18-llm-batch-classification.md

## Шаги

1. `GET /sangraha/internal/lexicon/classifications?schemeCode=&status=&cursor=&limit=` — пагинация курсором, сортировка по `Lemma.frequencyRank ASC` (частотные — первыми, §4).
2. Ответ строки: `lemmaId`, `lemmaIast`, `lemmaDevanagari`, `gender`, `dominantPosCode`, `frequencyRank`, `categoryCode`, `glossRu`, `glossEn`, `confidence`, `status`, `llmModel`.
3. `PATCH /sangraha/internal/lexicon/classifications/{id}` (ADMIN) `{status?, categoryCode?, glossRu?, glossEn?}` — частичное обновление полей + смена статуса одним запросом (§4); при смене на `APPROVED`/`REJECTED` — заполнить `reviewedBy` (`X-User-Id`), `reviewedAt = now()`.
4. Валидация на шаге 3: если `status=APPROVED` — `categoryCode` обязателен и должен существовать в `curriculum_semantic_topic` (после возможной правки в этом же запросе), иначе 422.
5. `GET /sangraha/internal/lexicon/lemma-classifications/export?schemeCode=CURRICULUM&status=APPROVED&cursor={lemmaId}&limit=500` — постраничный экспорт по §5, JOIN `Lemma`+`LemmaClassification`, только `status=APPROVED`.
6. Формат строки экспорта — по §5 (`lemmaId`, `lemmaSlp1`, `lemmaIast`, `lemmaDevanagari`, `gender`, `dominantPosCode`, `occurrenceCount`, `frequencyRank`, `categoryCode`, `glossRu`, `glossEn`), `nextCursor` по аналогии с `verse-words/export` (`sangraha-service.md` §9).
7. Не публичный, не через api-gateway (тот же принцип internal-эндпоинтов, что везде в проекте).
8. Интеграционный тест: 5 лемм с разными статусами (`CANDIDATE`/`APPROVED`/`REJECTED`) — экспорт возвращает только `APPROVED`, а `GET /classifications?status=CANDIDATE` возвращает оставшиеся.

## DoD
- [ ] `PATCH` не позволяет `APPROVED` без валидного `categoryCode` (шаг 4)
- [ ] Export отдаёт только `APPROVED`, постранично, без дублей между страницами
- [ ] `reviewedBy`/`reviewedAt` заполняются только при реальной смене статуса ADMIN, не при обычном апдейте полей без смены `status`
