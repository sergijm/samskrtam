# Handoff: split lemma into dictionary + statistics

Session state and verified plan. Date: 2026-08-09.

## State

**Implemented 2026-08-09** (see "Components changed" below). All decisions 1–9
done. Build: `sangraha-service` compiles, all tests green.

## Goal

Refactor sangraha lemma-classification module: `sangraha.lemma` becomes a pure
dictionary (unique by `lemma_slp1`), per-gender corpus stats move to a new
`sangraha.lemma_statistics`, and `lemma_classification` gets a `gender` column.

## Verified decisions (from conversation recap)

1. `lemma` = dictionary only:
   id, lemma_slp1 (UNIQUE), lemma_iast, lemma_devanagari, created_at, updated_at.
   Remove: gender, dominant_pos_code, occurrence_count, frequency_rank.
2. New table `lemma_statistics`:
   id, lemma_id (FK -> lemma.id, ON DELETE CASCADE), gender, occurrence_count,
   dominant_pos_code, updated_at. `UNIQUE(lemma_id, gender)`.
3. `lemma_classification`: add `gender` column.
   `UNIQUE(lemma_id, gender, scheme_code)`.
4. Export (lemma-classifications/export): one row per lemma. Pick the
   (lemma, gender) statistics row with max occurrence_count; include gender + 
   occurrenceCount from that row.
5. Candidates for classification run: sort by sum of occurrence_count over all
   genders of the lemma (most frequent first).
6. LLM prompt item: lemma + gender (from statistics).
7. verse_words: `lemma_id` stays as FK link to `sangraha.lemma`. The current
   `verse_words.lemma_iast` TEXT copy is kept for now (imported verses were
   seeded via external python script using text; script will be reworked later).
   Java `VerseWord` keeps `lemmaIast` text field mapped to that column.
   No changes needed to verse_words schema.
8. Endpoint rename: `/sangraha/internal/lexicon/lemmas/refresh` ->
   `/sangraha/internal/lexicon/lemmas/refresh-statistics`.
9. Migration V10: drop `sangraha.lemma` and recreate as dictionary; create
   `sangraha.lemma_statistics`; alter `lemma_classification` to add gender +
   new unique constraint.

## Files already partially changed in working tree (DO NOT clobber)

- `docs/services/sangraha-service/lemma-classification.md` (edited)
- `services/sangraha-service/.../service/LemmaRefreshService.java` (edited:
  removed status filter, aggregates ALL non-deleted verses)
- `services/sangraha-service/.../repository/VerseRepository.java`
  (added `findAllByDeletedAtIsNullAndIdGreaterThan`)
- `services/sangraha-service/.../service/LemmaRefreshServiceTest.java`

## Components to change

- `model/Lemma.java` - strip stats fields.
- NEW `model/LemmaStatistics.java` + `repository/LemmaStatisticsRepository.java`.
- `model/LemmaClassification.java` - add `gender`.
- `repository/LemmaRepository.java` - candidates query orders by stats sum.
- `repository/LemmaClassificationRepository.java` - `findByLemmaIdAndGenderAndSchemeCode`;
  review/export ordering via statistics sum.
- `service/LemmaRefreshService.java` - rewrite: upsert Lemma (dictionary, by
  lemma via translit), recalc LemmaStatistics per (lemma, gender); response
  counts become statistics-oriented; rename class? Keep name, change endpoint.
- `service/LemmaClassificationRunService.java` - candidates selection via stats;
  picks dominant (lemma, gender) for prompt; uses lemmaId -> gender row.
- `service/LemmaClassificationPromptBuilder.java` - item lemma+gender.
- `service/LemmaClassificationReviewService.java` + `dto/LemmaClassificationItemDto.java`
  - return gender + occurrenceCount from statistics; keep API shape (occurrenceCount
  from stats instead of lemma).
- `controller/LexiconController.java` - endpoint rename.
- migration V9 file = new `V10__lemmas_split_dictionary_statistics.sql` in
  `services/sangraha-service/src/main/resources/db/migration/` (V9 exists as
  `V9__create_works_class.sql`).

## Not changed (out of scope per decision 7/imports)

- `etcetera/python/classify/classify_nominal_lemmas.py`, import scripts:
  they will be reused/rewritten later separately.

## Components changed (implemented 2026-08-09)

- `model/Lemma.java` — dictionary-only (id, lemmaSlp1 UNIQUE, lemmaIast,
  lemmaDevanagari, createdAt/updatedAt). gender/dominantPosCode/occurrenceCount/
  frequencyRank removed.
- NEW `model/LemmaStatistics.java` + `repository/LemmaStatisticsRepository.java`
  (findByLemmaIdAndGender, findByLemmaId, findByLemmaIdIn).
- `model/LemmaClassification.java` — added `gender` column.
- `repository/LemmaRepository.java` — removed findByLemmaSlp1AndGender;
  candidates: exists-stats row without non-REJECTED (lemma, gender, scheme)
  classification; ordering by total occurrence sum in service layer.
- `repository/LemmaClassificationRepository.java` —
  `findByLemmaIdAndGenderAndSchemeCode`; `findForReview` orders by
  `ls.occurrenceCount DESC` (JOIN LemmaStatistics ON lemma+gender).
- `service/LemmaRefreshService.java` — dictionary upsert by lemmaSlp1 + stats
  per (lemma, gender), stale stats pruned, lemmaId linked.
- `dto/LemmaRefreshResponse.java` — {lemmaCount, newLemmaCount, updatedLemmaCount,
  statisticsCount, newStatisticsCount, updatedStatisticsCount, verseStatisticsCount}.
- `service/LemmaClassificationRunService.java` — candidates by total occurrence
  sum; batch item = dominant (gender, dominantPosCode) from stats; upsert via
  (lemmaId, gender, schemeCode).
- `service/LemmaClassificationPromptBuilder.java` — `LemmaBatchItem(Lemma, gender,
  dominantPosCode, examples)`.
- `service/LemmaClassificationReviewService.java` + `dto/LemmaClassificationItemDto.java`
  — gender + dominantPosCode + occurrenceCount from statistics; `frequencyRank`
  dropped from DTO.
- `controller/LexiconController.java` — `/lemmas/refresh` ->
  `/lemmas/refresh-statistics`.
- Migration `V10__lemmas_split_dictionary_statistics.sql` — fixed (original had
  missing `fk_lemma_classification_lemma` restore, non-null stale
  `verse_words.lemma_id` would break FK re-add, index without columns). Now:
  explicit FK drops, `verse_words.lemma_id`/`lemma_classification` cleared,
  FK restored with CASCADE, gender column + new UNIQUE.
- Tests: `LemmaRefreshServiceTest`, `LemmaClassificationRunServiceTest`,
  `LemmaClassificationReviewServiceTest` updated; build green.
- Docs updated: `lemma-classification.md` (293 lines, new model §1.1–§1.4),
  postman collection (endpoint + sort description), this handoff.

## Still open / not touched

- External python seed/import scripts: out of scope (decision 7), to be
  rewritten separately.
- Applying V10 to an existing DB: run flyway (dev DB rebuild) then
  `POST /lemmas/refresh-statistics` to populate the dictionary (only new lemmas)
  + statistics. `verse_words.lemma_id` linkage no longer applies (dropped).

## Dictionary final shape (2026-08-09 follow-up)

`verse_words.lemma_id` linkage is dropped entirely (decision revised: no id link
from corpus words to the dictionary). `JdbcTemplate`/bulk-ON-CONFLICT removed.

- `repository/VerseWordRepository.java` — `findDistinctLemmaIast()`: returns only
  DISTINCT `lemma_iast` from the corpus that are NOT yet in `sangraha.lemma`
  (filter: `NOT EXISTS (SELECT 1 FROM Lemma l WHERE l.lemmaIast = vw.lemmaIast)`),
  `length(lemmaIast) > 0`.
- `repository/LemmaRepository.java` — `findByLemmaSlp1In(Collection<String>)`
  (stats step resolves lemmaSlp1 → Lemma by IN, no full `findAll`).
- Migration `V11__lemma_iast_unique.sql` — `UNIQUE` constraint on `lemma_iast`
  (dictionary key stays `lemma_slp1`; uniqueness by IAST text is an extra guard).
- `service/LemmaRefreshService.java` — `refreshDictionary()`: distinct new
  `lemma_iast` → transliterate to SLP1/devanagari → one `lemmaRepository.saveAll`
  + `flush`. No corpus walk, no loops over verses.
  Statistics is DISABLED (response stats counts = 0); it will be added later via
  a single native SQL query.
  `LemmaDictionarySummary` is a record `(total, newCount, updatedCount)`.
- `LemmaRefreshServiceTest` — reworked around `LemmaRepository.saveAll` mock
  (in-memory dictionary store populated from saved lemmas); no JdbcTemplate.
  6 tests green; whole `sangraha-service` test suite green.