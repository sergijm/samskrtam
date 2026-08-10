package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Одна лемма для экспорта в curriculum-service (lemmas/export).
 * Содержит данные из lemma + lemma_statistics + lemma_classification (APPROVED) +
 * nominal_lemmas (stemClass → vowelType).
 */
public record LemmaExportItemDto(
        UUID lemmaId,
        String lemmaSlp1,
        String lemmaIast,
        String lemmaDevanagari,
        String gender,
        String dominantPosCode,
        int occurrenceCount,
        List<String> categoryCodes,
        String glossRu,
        String glossEn,
        String vowelType
) {
}
