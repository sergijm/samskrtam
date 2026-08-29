package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Ответ примерами стихов по словоизменительному классу (публичный endpoint
 * POST /api/v1/sangraha/verses/examples) для вкладки «Примеры» урока склонений:
 * ровно одна группа на каждую ячейку {@code (caseType, numberType)} парадигмы,
 * в группе — реальные стихи (текст, перевод, атрибуция).
 */
public record DeclensionExamplesResponseDto(
        List<GroupDto> groups
) {
    public record GroupDto(
            String caseType,
            String numberType,
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