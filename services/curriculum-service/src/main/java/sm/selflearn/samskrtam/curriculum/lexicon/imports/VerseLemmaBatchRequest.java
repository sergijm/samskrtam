package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import java.util.List;
import java.util.UUID;

/**
 * Пачка лемм одного стиха, которую sangraha-service присылает по мере анализа
 * (lexicon-content-pipeline.md §7). Один урок (Topic.domain = VERSE) на пару
 * (произведение, глава): code = "{workSlp1}_{chapterNumber}", лексемы пачки
 * привязываются через lexeme_lexical_topic.
 */
public record VerseLemmaBatchRequest(
        UUID verseId,
        UUID ownerId,
        String workSlug,
        String workSlp1,
        int chapterNumber,
        String chapterSlug,
        String workTitleRu,
        String workTitleEn,
        List<LemmaExportItem> words
) {
}
