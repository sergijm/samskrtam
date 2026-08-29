package sm.selflearn.samskrtam.sangraha.service;

import sm.selflearn.samskrtam.common.transliteration.TransliterationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.LemmaRefreshResponse;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaStatisticsRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseStatisticsRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LemmaRefreshServiceTest {

    private final Map<String, Lemma> lemmaStore = new HashMap<>();
    private LemmaRepository lemmaRepo;
    private LemmaStatisticsRepository statsRepo;
    private VerseStatisticsRepository verseStatsRepo;
    private VerseWordRepository verseWordRepo;

    @BeforeEach
    void setUp() {
        lemmaStore.clear();

        statsRepo = mock(LemmaStatisticsRepository.class);
        when(statsRepo.refreshStatistics(org.mockito.ArgumentMatchers.<UUID[]>any())).thenReturn(7);

        verseStatsRepo = mock(VerseStatisticsRepository.class);
        when(verseStatsRepo.refreshStatistics()).thenReturn(42);

        lemmaRepo = mock(LemmaRepository.class);
        when(lemmaRepo.saveAll(any())).thenAnswer(inv -> {
            Iterable<Lemma> lemmas = inv.getArgument(0);
            for (Lemma l : lemmas) {
                if (l.getId() == null) {
                    l.setId(UUID.randomUUID());
                }
                lemmaStore.put(l.getLemmaSlp1(), l);
            }
            return new ArrayList<>(lemmaStore.values());
        });

        verseWordRepo = mock(VerseWordRepository.class);
        when(verseWordRepo.findDistinctLemmaIast()).thenReturn(List.of());
    }

    private LemmaRefreshService service(String... distinctIast) {
        when(verseWordRepo.findDistinctLemmaIast()).thenAnswer(inv -> java.util.Arrays
                .stream(distinctIast)
                .filter(iast -> lemmaStore.values().stream().noneMatch(l -> l.getLemmaIast().equals(iast)))
                .toList());
        return new LemmaRefreshService(verseWordRepo, lemmaRepo, statsRepo, verseStatsRepo, new TransliterationService());
    }

    @Test
    void refresh_addsNewLeumasToDictionary() {
        LemmaRefreshResponse response = service("gaja", "nadi").refresh();

        assertThat(response.lemmaCount()).isEqualTo(2);
        assertThat(response.newLemmaCount()).isEqualTo(2);
        assertThat(response.statisticsCount()).isEqualTo(7);
        assertThat(response.verseStatisticsCount()).isEqualTo(42);

        Lemma gaja = lemmaStore.get("gaja");
        assertThat(gaja).isNotNull();
        assertThat(gaja.getLemmaSlp1()).isEqualTo("gaja");
        assertThat(gaja.getLemmaIast()).isEqualTo("gaja");
        assertThat(gaja.getLemmaDevanagari()).isNotBlank();
        assertThat(lemmaStore.get("nadi")).isNotNull();
    }

    @Test
    void refresh_skipsBlankIast() {
        LemmaRefreshResponse response = service("gaja", "", "  ").refresh();

        assertThat(response.lemmaCount()).isEqualTo(1);
        assertThat(lemmaStore).containsOnlyKeys("gaja");
    }

    @Test
    void refresh_secondRunIsIdempotent_newCountZero() {
        service("gaja").refresh();

        LemmaRefreshResponse second = service("gaja").refresh();

        assertThat(second.newLemmaCount()).isZero();
        assertThat(second.lemmaCount()).isZero();
        assertThat(lemmaStore).hasSize(1);
    }
}