package sm.selflearn.samskrtam.sangraha.dto;

import java.util.UUID;

/**
 * Одна словоформа для экспорта лексики (lexicon-content-pipeline.md §2).
 * Род и класс основы берутся из {@code verse_word_morphology} и могут отсутствовать.
 */
public record VerseWordExportItemDto(
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