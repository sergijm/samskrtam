package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import java.util.UUID;

/**
 * Одна лемма из экспорта sangraha-service (lemmas/export).
 * Соответствует {@code LemmaExportItemDto}.
 */
public record LemmaExportItem(
        UUID lemmaId,
        String lemmaSlp1,
        String lemmaIast,
        String lemmaDevanagari,
        String gender,
        String dominantPosCode,
        int occurrenceCount,
        java.util.List<String> categoryCodes,
        String glossRu,
        String glossEn,
        String vowelType
) {
}