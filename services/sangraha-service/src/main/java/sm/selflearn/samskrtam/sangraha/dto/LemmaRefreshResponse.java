package sm.selflearn.samskrtam.sangraha.dto;

/**
 * Ответ POST /sangraha/internal/lexicon/lemmas/refresh
 * (lemma-classification.md §1.3, task-sangraha-17 шаг 10).
 */
public record LemmaRefreshResponse(
        int lemmaCount,
        int newLemmaCount,
        int updatedLemmaCount) {
}