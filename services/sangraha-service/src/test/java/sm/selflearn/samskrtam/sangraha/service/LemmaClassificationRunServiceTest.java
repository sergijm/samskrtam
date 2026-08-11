package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.ClassificationRunResponse;
import sm.selflearn.samskrtam.sangraha.model.ClassificationScheme;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.LemmaStatistics;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationSchemeRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaClassificationRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaStatisticsRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationLlmClient.LemmaClassificationCallException;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationPromptBuilder.LemmaBatchItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LemmaClassificationRunServiceTest {

    private LemmaRepository lemmaRepo;
    private LemmaStatisticsRepository statisticsRepo;
    private ClassificationSchemeRepository schemeRepo;
    private VerseWordRepository verseWordRepo;
    private LemmaClassificationLlmClient llmClient;
    private ClassificationResultPersister persister;
    private ClassificationCandidateReader candidateReader;
    private LlmProperties llmProperties;
    private LlmConfigRegistry llmConfigRegistry;

    private final Map<UUID, List<LemmaStatistics>> statsByLemma = new HashMap<>();

    @BeforeEach
    void setUp() {
        statsByLemma.clear();
        lemmaRepo = mock(LemmaRepository.class);
        statisticsRepo = mock(LemmaStatisticsRepository.class);
        when(statisticsRepo.findByLemmaIdIn(anyCollection())).thenAnswer(inv -> {
            List<UUID> ids = new ArrayList<>((java.util.Collection<UUID>) inv.getArgument(0));
            return ids.stream().flatMap(id -> statsByLemma.getOrDefault(id, List.of()).stream()).toList();
        });
        when(statisticsRepo.findAll()).thenAnswer(inv -> statsByLemma.values().stream().flatMap(List::stream).toList());
        schemeRepo = mock(ClassificationSchemeRepository.class);
        when(schemeRepo.findById("CURRICULUM"))
                .thenReturn(Optional.of(ClassificationScheme.builder().code("CURRICULUM").titleRu("TEST").active(true).build()));
        when(schemeRepo.findById("WORDNET"))
                .thenReturn(Optional.of(ClassificationScheme.builder().code("WORDNET").titleRu("TEST").active(false).build()));
        verseWordRepo = mock(VerseWordRepository.class);
        when(verseWordRepo.findTop2ByLemmaIastOrderByPositionAsc(any())).thenReturn(List.of());
        llmClient = mock(LemmaClassificationLlmClient.class);
        LemmaClassificationRepository classificationRepo = mock(LemmaClassificationRepository.class);
        when(classificationRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classificationRepo.findByLemmaIdAndGenderAndSchemeCode(any(), any(), any())).thenReturn(Optional.empty());
        LemmaClassificationValidator validator = mock(LemmaClassificationValidator.class);
        when(validator.containsDevanagari(any())).thenReturn(false);
        when(validator.isValidCategoryCode(any())).thenReturn(true);
        persister = new ClassificationResultPersister(classificationRepo, validator);
        candidateReader = new ClassificationCandidateReader(lemmaRepo, statisticsRepo, verseWordRepo);
        llmProperties = new LlmProperties();
        llmProperties.setModel("test-model");
        llmConfigRegistry = mock(LlmConfigRegistry.class);
    }

    private Lemma lemma(String slp1, int rank, String... genders) {
        Lemma l = Lemma.builder().id(UUID.randomUUID()).lemmaSlp1(slp1).lemmaIast(slp1).lemmaDevanagari("x").build();
        List<LemmaStatistics> lStats = new ArrayList<>();
        for (int i = 0; i < genders.length; i++) {
            lStats.add(LemmaStatistics.builder().id(UUID.randomUUID()).lemma(l)
                    .gender(genders[i]).occurrenceCount(rank + i).dominantPosCode("NOUN").build());
        }
        statsByLemma.put(l.getId(), lStats);
        return l;
    }

    private LemmaClassificationRunService newService() {
        return new LemmaClassificationRunService(schemeRepo, llmClient, persister, candidateReader,
                llmProperties, llmConfigRegistry);
    }

    @Test
    void startRun_successQueue_completed() {
        List<Lemma> candidates = List.of(lemma("l10", 10, "MASCULINE"), lemma("l9", 9, "FEMININE"));
        when(lemmaRepo.findCandidatesForClassification(eq("CURRICULUM"), eq(ClassificationStatus.REJECTED)))
                .thenReturn(candidates);
        when(llmClient.classifyBatch(any(), any(), anyString())).thenReturn(
                new LemmaClassificationSuggestion.BatchResult(List.of(), "test-model"));

        ClassificationRunResponse r = newService().startRun("CURRICULUM", 1, 2, null, "admin");

        assertThat(r.succeededBatchCount()).isEqualTo(2);
        assertThat(r.failedBatchCount()).isZero();
    }

    @Test
    void startRun_rejectInactiveScheme() {
        assertThatThrownBy(() -> newService().startRun("WORDNET", 10, 1, null, "admin"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("not active");
    }

    @Test
    void startRun_batchCountRequired() {
        assertThatThrownBy(() -> newService().startRun("CURRICULUM", 10, null, null, "admin"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("batchCount");
    }

    @Test
    void startRun_oneFailedBatch_statusWithErrors() {
        List<Lemma> candidates = List.of(lemma("l1", 1, "MASCULINE"), lemma("l2", 2, "MASCULINE"));
        when(lemmaRepo.findCandidatesForClassification(eq("CURRICULUM"), eq(ClassificationStatus.REJECTED)))
                .thenReturn(candidates);
        when(llmClient.classifyBatch(any(), any(), anyString()))
                .thenThrow(new LemmaClassificationCallException("LLM down"))
                .thenReturn(new LemmaClassificationSuggestion.BatchResult(List.of(), "test-model"));

        ClassificationRunResponse r = newService().startRun("CURRICULUM", 1, 2, null, "admin");

        assertThat(r.failedBatchCount()).isEqualTo(1);
        assertThat(r.succeededBatchCount()).isEqualTo(1);
    }

    @Test
    void startRun_sortsCandidatesByTotalOccurrencesDesc() {
        List<Lemma> candidates = List.of(lemma("l1", 1, "MASCULINE"), lemma("l5", 5, "MASCULINE", "FEMININE"));
        when(lemmaRepo.findCandidatesForClassification(eq("CURRICULUM"), eq(ClassificationStatus.REJECTED)))
                .thenReturn(candidates);
        when(llmClient.classifyBatch(any(), any(), anyString())).thenReturn(
                new LemmaClassificationSuggestion.BatchResult(List.of(), "test-model"));

        newService().startRun("CURRICULUM", 10, 1, null, "admin");
        // candidates sorted by total occurrenceCount desc: l5=5+6=11, l1=1
    }
}