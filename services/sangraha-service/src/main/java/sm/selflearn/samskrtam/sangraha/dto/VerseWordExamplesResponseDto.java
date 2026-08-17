package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;

/**
 * Ответ на поиск примеров стихов по словоформам. Порядок results повторяет порядок
 * surfaceIasts в запросе; форма без найденных стихов получает пустой verses.
 */
public record VerseWordExamplesResponseDto(
        List<ResultDto> results
) {
    public record ResultDto(
            String surfaceIast,
            List<VersesBatchResponseDto.VerseDto> verses
    ) {}
}
