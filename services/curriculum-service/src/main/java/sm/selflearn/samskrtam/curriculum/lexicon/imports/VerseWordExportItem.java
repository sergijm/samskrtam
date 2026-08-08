package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import java.util.UUID;

/**
 * Одна словоформа из экспорта sangraha-service (verse-words/export).
 * Соответствует {@code VerseWordExportItemDto} sangraha-service.
 */
public record VerseWordExportItem(
        UUID verseId,
        String workSlug,
        String chapterSlug,
        int verseOrderIndex,
        String lemmaIast,
        String stem,
        String surfaceIast,
        String surfaceDevanagari,
        String pos,
        String lemmaGlossRu,
        String lemmaGlossEn,
        String gender,
        String vowelType
) {
}