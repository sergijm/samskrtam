package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Ответ POST /api/v1/sangraha/verse/analysis (sangraha-service/batch-verse-review.md),
 * 202 Accepted. Список verseId, реально принятых к анализу (не найденные/удалённые
 * исключены молча).
 */
public record AnalyzeVersesResponse(
        List<UUID> verseIds
) {}
