package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.sangraha.model.NominalLemma;
import sm.selflearn.samskrtam.sangraha.service.VerseWordSearchService.Candidate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты сортировки/отбора кандидатов по правилу sangraha-service.md §9 (B4)
 * и резолва vowelType через nominal_lemmas с fallback-эвристикой (B5).
 */
class VerseWordSearchServiceTest {

    private static UUID id(String s) {
        return UUID.fromString("00000000-0000-0000-0000-" + s);
    }

    private static NominalLemma lemma(VowelType stemClass) {
        return NominalLemma.builder().stemClass(stemClass).build();
    }

    @Test
    void rankAndSelect_primaryCandidatesEnough_reserveNotUsed() {
        List<Candidate> candidates = List.of(
                new Candidate(id("000000000003"), 1L),
                new Candidate(id("000000000004"), 2L),
                new Candidate(id("000000000005"), 3L),
                new Candidate(id("000000000001"), 5L),
                new Candidate(id("000000000002"), 4L)
        );

        List<UUID> result = VerseWordSearchService.rankAndSelect(candidates, 2);

        assertThat(result).containsExactly(id("000000000005"), id("000000000002"));
    }

    @Test
    void rankAndSelect_primaryFewerThanLimit_filledFromReserve() {
        List<Candidate> candidates = List.of(
                new Candidate(id("000000000004"), 2L),
                new Candidate(id("000000000005"), 3L),
                new Candidate(id("000000000001"), 4L),
                new Candidate(id("000000000003"), 1L)
        );

        List<UUID> result = VerseWordSearchService.rankAndSelect(candidates, 3);

        assertThat(result).containsExactly(id("000000000005"), id("000000000001"), id("000000000003"));
    }

    @Test
    void rankAndSelect_fewerCandidatesThanLimit_returnsAll() {
        List<Candidate> candidates = List.of(
                new Candidate(id("000000000004"), 2L),
                new Candidate(id("000000000003"), 1L)
        );

        List<UUID> result = VerseWordSearchService.rankAndSelect(candidates, 5);

        assertThat(result).containsExactly(id("000000000003"), id("000000000004"));
    }

    @Test
    void rankAndSelect_equalWordCount_orderStableByVerseId() {
        UUID shortFirst = id("000000000002");
        UUID shortSecond = id("000000000001");
        List<Candidate> candidates = List.of(
                new Candidate(shortFirst, 2L),
                new Candidate(shortSecond, 2L),
                new Candidate(id("000000000004"), 2L),
                new Candidate(id("000000000003"), 2L)
        );

        List<UUID> first = VerseWordSearchService.rankAndSelect(candidates, 4);
        List<UUID> second = VerseWordSearchService.rankAndSelect(candidates, 4);

        assertThat(first).containsExactly(
                id("000000000001"), id("000000000002"), id("000000000003"), id("000000000004"));
        assertThat(second).isEqualTo(first);
    }

    @Test
    void rankAndSelect_emptyCandidates_emptyResult() {
        List<UUID> result = VerseWordSearchService.rankAndSelect(List.of(), 3);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveVowelType_noLemma_fallsBackToStemHeuristic() {
        assertThat(VerseWordSearchService.resolveVowelType(null, "deva")).isEqualTo(VowelType.A_STEM);
    }

    @Test
    void resolveVowelType_noLemma_unclassifiableStem_returnsNull() {
        assertThat(VerseWordSearchService.resolveVowelType(null, "vrkṣas")).isNull();
    }

    @Test
    void resolveVowelType_lemmaStemClassNull_fallsBackToStemHeuristic() {
        assertThat(VerseWordSearchService.resolveVowelType(lemma(null), "deva")).isEqualTo(VowelType.A_STEM);
    }

    @Test
    void resolveVowelType_lemmaPresent_nominalLemmasWinOverHeuristic() {
        assertThat(VerseWordSearchService.resolveVowelType(lemma(VowelType.U_STEM), "deva")).isEqualTo(VowelType.U_STEM);
    }
}
