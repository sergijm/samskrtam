package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;

/**
 * Ответ на поиск примеров стихов по леммам. Порядок results повторяет порядок
 * lemmas в запросе; лемма без найденных стихов получает пустой verses.
 */
public record LemmaExamplesResponseDto(
        List<ResultDto> results
) {
    public record ResultDto(
            String lemmaIast,
            List<VersesBatchResponseDto.VerseDto> verses
    ) {}
}
