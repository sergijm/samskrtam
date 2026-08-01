package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

public record VersesBatchResponseDto(
        List<VerseDto> verses
) {
    public record VerseDto(
            UUID verseId,
            String textIast,
            String textDevanagari,
            String translationRu,
            String translationEn,
            String workTitleRu,
            String workTitleEn,
            String chapterTitleRu,
            String chapterTitleEn,
            int verseOrderIndex
    ) {}
}
