# Задача: sangraha-service — модуль lexicon-classification: Lemma и справочники

**Что:** Таблицы/сущности `Lemma`, `ClassificationScheme`,
`CurriculumSemanticTopic`, refresh-job агрегации `Lemma` из `VerseWord`.
**Зачем:** См. `docs/services/sangraha-service/lemma-classification.md` §1.

## Зависит от
Существующие миграции/сущности `VerseWord`/`VerseWordMorphology`
(`verse-word-grammar.md`).

## Шаги

1. Миграция: таблица `sangraha.lemma` — поля по §1.1 (`lemmaSlp1`, `lemmaIast`, `lemmaDevanagari`, `gender`, `dominantPosCode`, `occurrenceCount`, `frequencyRank`), `UNIQUE(lemma_slp1, gender)`.
2. Миграция: `ALTER TABLE sangraha.verse_word ADD COLUMN lemma_id UUID NULL REFERENCES sangraha.lemma(id)`.
3. Миграция: таблица `sangraha.classification_scheme` (`code` PK, `title_ru`, `is_active`), seed-данные: строка `CURRICULUM` (`is_active=true`), строка `WORDNET` (`is_active=false`).
4. Миграция: таблица `sangraha.curriculum_semantic_topic` (`code` PK, `parent_code` self-FK nullable, `label_ru`, `label_en`, `description`).
5. Seed-миграция: 42 строки таксономии (9 корней + 33 листа) — скопировать состав из `lexical-curriculum.md` §3 (коды придумать в kebab-case по названию листа, например `animals`, `plants`, `ritual-worship`, `speech-acts`).
6. Entity `Lemma`, `ClassificationScheme`, `CurriculumSemanticTopic` (JPA, self-FK у `CurriculumSemanticTopic` через `parentCode`).
7. `LemmaRefreshService.refresh()`: группировка `VerseWord` (только стихи `status=ANALYZED`) по `(lemmaSlp1, gender)` — `lemmaSlp1` через существующий IAST→SLP1 конвертер.
8. Шаг 7 продолжение: upsert `Lemma` (новая группа → новая строка; существующая → обновить `occurrenceCount`/`dominantPosCode`/пересчитать `frequencyRank` по всем строкам разом).
9. Проставить `VerseWord.lemmaId` для каждой строки соответствующей группы (batch update).
10. `POST /sangraha/internal/lexicon/lemmas/refresh` (ADMIN) — синхронный вызов шагов 7–9, ответ `{ lemmaCount, newLemmaCount, updatedLemmaCount }`.
11. Идемпотентность: повторный вызов на тех же данных не создаёт дублей `Lemma`, `newLemmaCount = 0`.

## DoD
- [ ] Refresh корректно группирует по `(lemmaSlp1, gender)`, `frequencyRank` детерминирован (алфавитный tie-break)
- [ ] Повторный refresh идемпотентен (шаг 11)
- [ ] Seed-таксономия — ровно 42 строки, дерево `parentCode` валидно (9 корней без родителя, 33 листа с родителем)
