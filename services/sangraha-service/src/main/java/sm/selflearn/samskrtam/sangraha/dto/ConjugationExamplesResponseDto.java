package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Ответ примерами стихов по спряжению (вкладка «Примеры» урока спряжений):
 * одна группа на каждую ячейку {@code (tense, mood)} парадигмы.
 */
public record ConjugationExamplesResponseDto(
        List<GroupDto> groups
) {
    public record GroupDto(
            String tense,
            String mood,
            List<ExampleDto> examples
    ) {}

    public record ExampleDto(
            UUID verseId,
            String workSlug,
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