package sm.selflearn.samskrtam.sangraha.dto;

/**
 * Ответ POST /sangraha/internal/lexicon/lemmas/refresh-statistics
 * (lemma-classification.md §1.3, решение 2026-08-09).
 */
public record LemmaRefreshResponse(
        int lemmaCount,
        int newLemmaCount,
        int updatedLemmaCount,
        int statisticsCount,
        int newStatisticsCount,
        int updatedStatisticsCount) {
}