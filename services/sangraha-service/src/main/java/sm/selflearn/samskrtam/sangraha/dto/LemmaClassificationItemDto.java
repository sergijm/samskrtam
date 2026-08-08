package sm.selflearn.samskrtam.sangraha.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка списка на ревью (lemma-classification.md §4) и строка экспорта (§5).
 */
public record LemmaClassificationItemDto(
        UUID id,
        UUID lemmaId,
        String lemmaSlp1,
        String lemmaIast,
        String lemmaDevanagari,
        String gender,
        String dominantPosCode,
        Integer occurrenceCount,
        Integer frequencyRank,
        String categoryCode,
        String glossRu,
        String glossEn,
        Short confidence,
        String status,
        String reviewedBy,
        Instant reviewedAt) {
}