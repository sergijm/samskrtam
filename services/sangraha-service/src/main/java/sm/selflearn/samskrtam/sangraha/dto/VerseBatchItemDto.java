package sm.selflearn.samskrtam.sangraha.dto;

import sm.selflearn.samskrtam.sangraha.model.VerseStatus;

import java.util.UUID;

/**
 * Элемент ответа GET /api/v1/sangraha/verse (sangraha-service/batch-verse-review.md).
 * В отличие от внутреннего /sangraha/internal/content/verses/batch — отдаёт id,
 * слаги и status любого стиха (без фильтра по ANALYZED).
 */
public record VerseBatchItemDto(
        UUID id,
        String workSlug,
        String workTitleRu,
        String workTitleEn,
        String chapterSlug,
        String chapterTitleRu,
        String chapterTitleEn,
        int verseOrderIndex,
        String textIastPreview,
        VerseStatus status
) {}
