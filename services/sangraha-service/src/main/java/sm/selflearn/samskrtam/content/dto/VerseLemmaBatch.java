package sm.selflearn.samskrtam.content.dto;

import java.util.List;
import java.util.UUID;

/**
 * Пачка лемм одного стиха, отправляемая в curriculum-service после анализа стиха
 * (lexicon-content-pipeline.md §7). Слова — тот же формат {@code LemmaExportItem},
 * что в lemmas/export: поля записей совпадают поимённо, curriculum десериализует их
 * в свой {@code LemmaExportItem}.
 */
public record VerseLemmaBatch(
        UUID verseId,
        UUID ownerId,
        String workSlug,
        String workSlp1,
        int chapterNumber,
        String chapterSlug,
        String workTitleRu,
        String workTitleEn,
        List<Word> words
) {

    public record Word(
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
}
