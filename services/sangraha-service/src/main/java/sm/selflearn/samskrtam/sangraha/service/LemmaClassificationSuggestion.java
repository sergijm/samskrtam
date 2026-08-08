package sm.selflearn.samskrtam.sangraha.service;

import java.util.List;
import java.util.UUID;

/**
 * Результат классификации одной леммы, извлечённый из tool-ответа LLM
 * (lemma-classification.md §2.3).
 */
public record LemmaClassificationSuggestion(
        UUID lemmaId,
        String categoryCode,
        String glossRu,
        String glossEn,
        Short confidence) {

    public record BatchResult(List<LemmaClassificationSuggestion> items, String llmModel) {
    }
}