package sm.selflearn.samskrtam.sangraha.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Строка списка на ревью (lemma-classification.md §4) и строка экспорта (§5).
 * gender приходит из строки классификации; dominantPosCode/occurrenceCount — из
 * статистики (lemma, gender) вместе с gender (решение 2026-08-09). frequencyRank
 * исключён: ранг жил на лемме, после расщепления на словарь+статистику не ведётся.
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
        String categoryCode,
        String glossRu,
        String glossEn,
        Short confidence,
        String status,
        String reviewedBy,
        Instant reviewedAt) {
}