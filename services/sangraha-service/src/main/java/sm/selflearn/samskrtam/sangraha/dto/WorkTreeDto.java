package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

public record WorkTreeDto(
    UUID id,
    String slug,
    String titleRu,
    String titleEn,
    String descriptionRu,
    String descriptionEn,
    String author,
    List<ChapterTreeDto> chapters
) {}