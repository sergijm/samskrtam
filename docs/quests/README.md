# Quests — юзер-стори по разделам грамматики и лексики

> Структура повторяет домены из [services/quest-catalog.md](../services/quest-catalog.md).
> Каждая подпапка — один тип квеста; юзер-стори внутри описывают конкретный сценарий
> использования этого типа с точки зрения ученика.

На этом этапе написаны **31 юзер-стори** — по каждому типу из
[quest-types-overview.md](../services/quest-types-overview.md), кроме уже реализованного
`VOCABULARY_WORD` (прямое направление, слово→перевод — сделано, M4).

## Структура

### Morphology

| Подпапка | Тип | Юзер-стори |
|---|---|---|
| `morphology/declension/` | DECLENSION_FORM | [user-story-case-drill.md](morphology/declension/user-story-case-drill.md) |
| `morphology/conjugation/` | CONJUGATION_FORM (parasmaipada, laṭ/loṭ/vidhiliṅ) | [user-story-verb-drill.md](morphology/conjugation/user-story-verb-drill.md) |
| `morphology/conjugation-atmanepada/` | CONJUGATION_FORM (ātmanepada) | [user-story-atmanepada-drill.md](morphology/conjugation-atmanepada/user-story-atmanepada-drill.md) |
| `morphology/conjugation-secondary-tenses/` | CONJUGATION_FORM (laṅ, lṛṭ) | [user-story-past-future-drill.md](morphology/conjugation-secondary-tenses/user-story-past-future-drill.md) |
| `morphology/participles/` | PARTICIPLE_FORM | [user-story-present-participle.md](morphology/participles/user-story-present-participle.md) |
| `morphology/absolutives/` | ABSOLUTIVE_FORM | [user-story-absolutive-recognition.md](morphology/absolutives/user-story-absolutive-recognition.md) |
| `morphology/infinitives/` | INFINITIVE_FORM | [user-story-infinitive-recognition.md](morphology/infinitives/user-story-infinitive-recognition.md) |
| `morphology/secondary-stems/` | SECONDARY_STEM_FORM | [user-story-causative-drill.md](morphology/secondary-stems/user-story-causative-drill.md) |
| `morphology/numerals/` | NUMERAL_FORM | [user-story-numerals-1-4.md](morphology/numerals/user-story-numerals-1-4.md) |

### Phonology

| Подпапка | Тип | Юзер-стори |
|---|---|---|
| `phonology/sandhi/` | SANDHI_SPLIT (внешнее) | [user-story-sandhi-split.md](phonology/sandhi/user-story-sandhi-split.md) |
| `phonology/sandhi-join/` | SANDHI_JOIN (внешнее) | [user-story-sandhi-join.md](phonology/sandhi-join/user-story-sandhi-join.md) |
| `phonology/internal-sandhi/` | SANDHI_SPLIT (внутреннее) | [user-story-internal-sandhi.md](phonology/internal-sandhi/user-story-internal-sandhi.md) |

### Syntax

| Подпапка | Тип | Юзер-стори |
|---|---|---|
| `syntax/karaka/` | KARAKA_CASE_CHOICE | [user-story-karaka-choice.md](syntax/karaka/user-story-karaka-choice.md) |
| `syntax/agreement/` | AGREEMENT_CHECK | [user-story-agreement-check.md](syntax/agreement/user-story-agreement-check.md) |
| `syntax/relative-clause/` | RELATIVE_CLAUSE | [user-story-relative-correlative.md](syntax/relative-clause/user-story-relative-correlative.md) |
| `syntax/participle-clause/` | PARTICIPLE_CLAUSE | [user-story-participle-clause.md](syntax/participle-clause/user-story-participle-clause.md) |

### Lexicon

| Подпапка | Тип | Юзер-стори |
|---|---|---|
| `lexicon/vocabulary/` | VOCABULARY_WORD (обратное направление) | [user-story-reverse-recall.md](lexicon/vocabulary/user-story-reverse-recall.md) |
| `lexicon/synonyms/` | VOCABULARY_SYNONYM | [user-story-synonym-match.md](lexicon/synonyms/user-story-synonym-match.md) |
| `lexicon/antonyms/` | VOCABULARY_ANTONYM | [user-story-antonym-match.md](lexicon/antonyms/user-story-antonym-match.md) |
| `lexicon/roots/` | VOCABULARY_ROOT | [user-story-root-recognition.md](lexicon/roots/user-story-root-recognition.md) |
| `lexicon/semantic-fields/` | VOCABULARY_SEMANTIC_FIELD | [user-story-semantic-field.md](lexicon/semantic-fields/user-story-semantic-field.md) |
| `lexicon/gender/` | VOCABULARY_GENDER | [user-story-gender-quiz.md](lexicon/gender/user-story-gender-quiz.md) |
| `lexicon/word-formation/` | WORD_FORMATION | [user-story-taddhita-formation.md](lexicon/word-formation/user-story-taddhita-formation.md) |
| `lexicon/idioms/` | IDIOM_MATCH | [user-story-idiom-match.md](lexicon/idioms/user-story-idiom-match.md) |

### Prosody

| Подпапка | Тип | Юзер-стори |
|---|---|---|
| `prosody/chandas/` | CHANDAS_IDENTIFICATION | [user-story-sloka-recognition.md](prosody/chandas/user-story-sloka-recognition.md) |
| `prosody/syllable-weight/` | SYLLABLE_WEIGHT | [user-story-syllable-weight.md](prosody/syllable-weight/user-story-syllable-weight.md) |

### Compounds

| Подпапка | Тип | Юзер-стори |
|---|---|---|
| `compounds/split/` | COMPOUND_SPLIT | [user-story-compound-split.md](compounds/split/user-story-compound-split.md) |
| `compounds/type/` | COMPOUND_TYPE | [user-story-compound-type.md](compounds/type/user-story-compound-type.md) |

### Meta

| Подпапка | Тип | Юзер-стори |
|---|---|---|
| `meta/mixed-review/` | MIXED_REVIEW | [user-story-mixed-review.md](meta/mixed-review/user-story-mixed-review.md) |
| `meta/error-correction/` | ERROR_CORRECTION | [user-story-error-correction.md](meta/error-correction/user-story-error-correction.md) |
| `meta/sentence-translation/` | SENTENCE_TRANSLATION | [user-story-sentence-translation.md](meta/sentence-translation/user-story-sentence-translation.md) |
