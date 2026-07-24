package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

public record ChapterTreeDto(
    UUID id,
    String slug,
    String titleRu,
    String titleEn,
    String titleIast,
    String titleDevanagari,
    int orderIndex,
    String categoryCode,
    List<VerseTreeDto> verses
) {}