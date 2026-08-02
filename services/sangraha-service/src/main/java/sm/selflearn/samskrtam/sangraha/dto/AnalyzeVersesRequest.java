package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Тело POST /api/v1/sangraha/verse/analysis (sangraha-service/batch-verse-review.md).
 */
public record AnalyzeVersesRequest(
        List<UUID> verseIds
) {}
