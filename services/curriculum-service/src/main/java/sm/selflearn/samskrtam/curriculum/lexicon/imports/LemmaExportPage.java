package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import java.util.List;
import java.util.UUID;

/**
 * Страница экспорта лемм из sangraha-service (lemmas/export).
 * Соответствует {@code LemmaExportPageDto}.
 */
public record LemmaExportPage(
        List<LemmaExportItem> items,
        UUID nextCursor
) {
}