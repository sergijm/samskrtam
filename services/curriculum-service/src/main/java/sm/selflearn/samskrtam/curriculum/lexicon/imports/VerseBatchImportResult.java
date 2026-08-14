package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import java.util.UUID;

/**
 * Итог обработки одной пачки стиха (lexicon-content-pipeline.md §7).
 */
public record VerseBatchImportResult(
        int importedCount,
        int updatedCount,
        UUID topicId,
        String topicCode
) {
}
