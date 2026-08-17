package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExamplesRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExamplesResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchResponseDto.VerseDto;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository.SurfaceVerseRank;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Юнит-тесты VerseWordExamplesService: самая короткая строка с глаголом на форму,
 * форма без совпадений получает пустой verses, порядок форм из запроса сохраняется.
 */
class VerseWordExamplesServiceTest {

    private VerseWordRepository verseWordRepository;
    private VerseBatchService verseBatchService;
    private VerseWordExamplesService service;

    private static UUID id(String suffix) {
        return UUID.fromString("00000000-0000-0000-0000-" + suffix);
    }

    private static SurfaceVerseRank rank(String form, UUID verseId, int wordCount) {
        return new SurfaceVerseRank() {
            @Override
            public String getSurfaceIast() {
                return form;
            }

            @Override
            public UUID getVerseId() {
                return verseId;
            }

            @Override
            public int getWordCount() {
                return wordCount;
            }
        };
    }

    private static VerseDto verse(UUID verseId, String textIast, String translationRu) {
        return new VerseDto(
                verseId, "work", textIast, textIast,
                translationRu, null, "Work", "Work", "Chapter", "Chapter", 1);
    }

    @BeforeEach
    void setUp() {
        verseWordRepository = mock(VerseWordRepository.class);
        verseBatchService = mock(VerseBatchService.class);
        service = new VerseWordExamplesService(verseWordRepository, verseBatchService);
    }

    @Test
    void findExamples_shortestVersePerForm_returnsOneVerseEach() {
        UUID v1 = id("000000000001");
        UUID v2 = id("000000000002");
        when(verseWordRepository.findShortestSurfaceVerseWithVerb(List.of("devaḥ", "devam"), 3, 7))
                .thenReturn(List.of(
                        rank("devaḥ", v1, 4),
                        rank("devam", v2, 5)
                ));
        when(verseBatchService.fetchBatch(any()))
                .thenReturn(new VersesBatchResponseDto(List.of(
                        verse(v1, "devaḥ ...", "первый"),
                        verse(v2, "... devam ...", "второй")
                )));

        VerseWordExamplesResponseDto response = service.findExamples(
                new VerseWordExamplesRequestDto(List.of("devaḥ", "devam")));

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).surfaceIast()).isEqualTo("devaḥ");
        assertThat(response.results().get(0).verses()).extracting(VerseDto::verseId)
                .containsExactly(v1);
        assertThat(response.results().get(1).surfaceIast()).isEqualTo("devam");
        assertThat(response.results().get(1).verses()).extracting(VerseDto::verseId)
                .containsExactly(v2);
    }

    @Test
    void findExamples_formWithoutSuitableVerse_returnsEmptyVerses() {
        when(verseWordRepository.findShortestSurfaceVerseWithVerb(List.of("devaḥ", "devasya"), 3, 7))
                .thenReturn(List.of(rank("devaḥ", id("000000000001"), 5)));
        when(verseBatchService.fetchBatch(any()))
                .thenReturn(new VersesBatchResponseDto(List.of(
                        verse(id("000000000001"), "devaḥ ...", null)
                )));

        VerseWordExamplesResponseDto response = service.findExamples(
                new VerseWordExamplesRequestDto(List.of("devaḥ", "devasya")));

        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).verses()).hasSize(1);
        assertThat(response.results().get(1).surfaceIast()).isEqualTo("devasya");
        assertThat(response.results().get(1).verses()).isEmpty();
    }

    @Test
    void findExamples_emptyRequest_returnsNoResults() {
        VerseWordExamplesResponseDto response = service.findExamples(
                new VerseWordExamplesRequestDto(List.of()));

        assertThat(response.results()).isEmpty();
    }

    @Test
    void findExamples_blankForms_filteredOut() {
        when(verseWordRepository.findShortestSurfaceVerseWithVerb(List.of("devaḥ"), 3, 7))
                .thenReturn(List.of(rank("devaḥ", id("000000000001"), 4)));
        when(verseBatchService.fetchBatch(any()))
                .thenReturn(new VersesBatchResponseDto(List.of(
                        verse(id("000000000001"), "devaḥ ...", null)
                )));

        VerseWordExamplesResponseDto response = service.findExamples(
                new VerseWordExamplesRequestDto(List.of("  ", "devaḥ")));

        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).surfaceIast()).isEqualTo("devaḥ");
    }
}
