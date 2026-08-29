package sm.selflearn.samskrtam.sangraha.dto;

import java.time.Instant;
import java.util.UUID;

public record WorkSummaryDto(
    UUID id,
    String slug,
    String titleRu,
    String titleEn,
    String titleSaIast,
    String titleSaDevanagari,
    String descriptionRu,
    String descriptionEn,
    String author,
    Instant createdAt,
    int chapterCount,
    int verseCount,
    int analyzedVerseCount
) {}
