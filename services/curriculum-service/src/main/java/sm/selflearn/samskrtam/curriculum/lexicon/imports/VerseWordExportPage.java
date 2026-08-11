package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import java.util.List;
import java.util.UUID;

/**
 * Страница экспорта VerseWord[] из sangraha-service (verse-words/export).
 */
public record VerseWordExportPage(
        List<VerseWordExportItem> items,
        UUID nextCursor
) {
}