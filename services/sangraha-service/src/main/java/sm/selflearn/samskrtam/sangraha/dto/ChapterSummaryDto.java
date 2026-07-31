package sm.selflearn.samskrtam.sangraha.dto;

import java.util.UUID;

public record ChapterSummaryDto(
    UUID id,
    String slug,
    String titleRu,
    String titleEn,
    String titleIast,
    String titleDevanagari,
    int orderIndex,
    String categoryCode,
    int verseCount
) {}
