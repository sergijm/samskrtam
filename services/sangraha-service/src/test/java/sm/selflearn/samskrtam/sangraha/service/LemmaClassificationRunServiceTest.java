package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sm.selflearn.samskrtam.sangraha.dto.ClassificationRunResponse;
import sm.selflearn.samskrtam.sangraha.model.ClassificationBatch;
import sm.selflearn.samskrtam.sangraha.model.ClassificationRun;
import sm.selflearn.samskrtam.sangraha.model.ClassificationScheme;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.LemmaClassification;
import sm.selflearn.samskrtam.sangraha.model.LemmaStatistics;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationBatchRepository;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationRunRepository;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationSchemeRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaClassificationRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaStatisticsRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationLlmClient.LemmaClassificationCallException;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LemmaClassificationRunServiceTest {

    private final Map<UUID, ClassificationRun> runs = new HashMap<>();
    private final Map<UUID, ClassificationBatch> batches = new HashMap<>();

    private LemmaRepository lemmaRepo;
    private LemmaStatisticsRepository statisticsRepo;
    private ClassificationRunRepository runRepo;
    private ClassificationBatchRepository batchRepo;
    private ClassificationSchemeRepository schemeRepo;
    private LemmaClassificationRepository classificationRepo;
    private final ArgumentCaptor<LemmaClassification> captor = ArgumentCaptor.forClass(LemmaClassification.class);
    private VerseWordRepository verseWordRepo;
    private LemmaClassificationLlmClient llmClient;
    private LemmaClassificationValidator validator;
    private LlmProperties llmProperties;

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
        runRepo = mock(ClassificationRunRepository.class);
        when(runRepo.save(any())).thenAnswer(inv -> {
            ClassificationRun r = inv.getArgument(0);
            if (r.getId() == null) r.setId(UUID.randomUUID());
            runs.put(r.getId(), r);
            return r;
        });
        when(runRepo.findById(any())).thenAnswer(inv -> Optional.ofNullable(runs.get(inv.getArgument(0))));
        batchRepo = mock(ClassificationBatchRepository.class);
        when(batchRepo.save(any())).thenAnswer(inv -> {
            ClassificationBatch b = inv.getArgument(0);
            if (b.getId() == null) b.setId(UUID.randomUUID());
            batches.put(b.getId(), b);
            return b;
        });
        when(batchRepo.findByRunId(any())).thenAnswer(inv ->
                batches.values().stream().filter(b -> b.getRunId().equals(inv.getArgument(0))).toList());
        schemeRepo = mock(ClassificationSchemeRepository.class);
        when(schemeRepo.findById("CURRICULUM"))
                .thenReturn(Optional.of(ClassificationScheme.builder().code("CURRICULUM").titleRu("TEST").active(true).build()));
        when(schemeRepo.findById("WORDNET"))
                .thenReturn(Optional.of(ClassificationScheme.builder().code("WORDNET").titleRu("TEST").active(false).build()));
        when(schemeRepo.findById("UNKNOWN")).thenReturn(Optional.empty());
        classificationRepo = mock(LemmaClassificationRepository.class);
        when(classificationRepo.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(classificationRepo.findByLemmaIdAndGenderAndSchemeCode(any(), any(), any())).thenReturn(Optional.empty());
        verseWordRepo = mock(VerseWordRepository.class);
        when(verseWordRepo.findTop2ByLemmaIastOrderByPositionAsc(any())).thenReturn(List.of());
        llmClient = mock(LemmaClassificationLlmClient.class);
        validator = mock(LemmaClassificationValidator.class);
        when(validator.containsDevanagari(any())).thenReturn(false);
        when(validator.isValidCategoryCode(any())).thenReturn(true);
        llmProperties = new LlmProperties();
        llmProperties.setModel("test-model");
    }

    private Lemma lemma(int rank, String... genders) {
        Lemma lemma = Lemma.builder().id(UUID.randomUUID()).lemmaSlp1("lemma" + rank).lemmaIast("lema" + rank)
                .lemmaDevanagari("lemma").build();
        List<LemmaStatistics> lemmaStats = new ArrayList<>();
        for (int i = 0; i < genders.length; i++) {
            lemmaStats.add(LemmaStatistics.builder()
                    .id(UUID.randomUUID())
                    .lemma(lemma)
                    .gender(genders[i])
                    .occurrenceCount(rank + i)
                    .dominantPosCode("NOUN")
                    .build());
        }
        statsByLemma.put(lemma.getId(), lemmaStats);
        return lemma;
    }

    private LemmaClassificationRunService newService() {
        return new LemmaClassificationRunService(lemmaRepo, statisticsRepo, runRepo, batchRepo, schemeRepo,
                classificationRepo, verseWordRepo, llmClient, validator, llmProperties);
    }

    @Test
    void startRun_successQueue_completed() {
        List<Lemma> candidates = List.of(lemma(10, "MASCULINE"), lemma(9, "FEMININE"));
        when(lemmaRepo.findCandidatesForClassification(eq("CURRICULUM"), eq(ClassificationStatus.REJECTED)))
                .thenReturn(candidates);
        when(llmClient.classifyBatch(any())).thenReturn(
                new LemmaClassificationSuggestion.BatchResult(List.of(), "test-model"));

        ClassificationRunResponse response = newService().startRun("CURRICULUM", 1, 2, "admin");

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.completedBatchCount()).isEqualTo(2);
        assertThat(response.succeededBatchCount()).isEqualTo(2);
        assertThat(response.failedBatchCount()).isZero();
    }

    @Test
    void startRun_rejectInactiveScheme() {
        assertThatThrownBy(() -> newService().startRun("WORDNET", 10, 1, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void startRun_batchCountRequired() {
        assertThatThrownBy(() -> newService().startRun("CURRICULUM", 10, null, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchCount");
    }

    @Test
    void startRun_oneFailedBatch_otherSucceeds_statusWithErrors() {
        List<Lemma> candidates = List.of(lemma(1, "MASCULINE"), lemma(2, "MASCULINE"));
        when(lemmaRepo.findCandidatesForClassification(eq("CURRICULUM"), eq(ClassificationStatus.REJECTED)))
                .thenReturn(candidates);
        when(llmClient.classifyBatch(any()))
                .thenThrow(new LemmaClassificationCallException("LLM down"))
                .thenReturn(new LemmaClassificationSuggestion.BatchResult(List.of(), "test-model"));
        when(lemmaRepo.findById(any())).thenAnswer(inv -> candidates.stream()
                .filter(l -> l.getId().equals(inv.getArgument(0))).findFirst());

        ClassificationRunResponse response = newService().startRun("CURRICULUM", 1, 2, "admin");

        assertThat(response.status()).isEqualTo("COMPLETED_WITH_ERRORS");
        assertThat(response.failedBatchCount()).isEqualTo(1);
        assertThat(response.succeededBatchCount()).isEqualTo(1);
    }

    @Test
    void getRun_whenNone_runNotFound() {
        assertThatThrownBy(() -> newService().getRun(UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Run not found");
    }

    @Test
    void unknownCategoryCode_savesRowWithNullCategory_butKeepsGloss() {
        List<Lemma> candidates = List.of(lemma(1, "MASCULINE"));
        when(lemmaRepo.findCandidatesForClassification(eq("CURRICULUM"), eq(ClassificationStatus.REJECTED)))
                .thenReturn(candidates);
        when(lemmaRepo.findById(any())).thenAnswer(inv -> candidates.stream()
                .filter(l -> l.getId().equals(inv.getArgument(0))).findFirst());
        when(validator.isValidCategoryCode(any())).thenReturn(false);
        when(llmClient.classifyBatch(any())).thenReturn(new LemmaClassificationSuggestion.BatchResult(List.of(
                new LemmaClassificationSuggestion(candidates.get(0).getId(), "bogus", "слон", "elephant", (short) 80)),
                "test-model"));

        ClassificationRunResponse response = newService().startRun("CURRICULUM", 1, 1, "admin");

        assertThat(response.classifiedLemmaCount()).isEqualTo(1);
        LemmaClassification saved = lastSaved();
        assertThat(saved.getCategoryCode()).isNull();
        assertThat(saved.getGlossRu()).isEqualTo("слон");
        assertThat(saved.getGlossEn()).isEqualTo("elephant");
        assertThat(saved.getGender()).isEqualTo("MASCULINE");
    }

    @Test
    void devanagariInGloss_discardsRow() {
        List<Lemma> candidates = List.of(lemma(1, "MASCULINE"));
        when(lemmaRepo.findCandidatesForClassification(eq("CURRICULUM"), eq(ClassificationStatus.REJECTED)))
                .thenReturn(candidates);
        when(validator.containsDevanagari(any())).thenAnswer(inv -> {
            String text = (String) inv.getArgument(0);
            return text != null && text.matches(".*\\p{InDevanagari}.*");
        });
        when(llmClient.classifyBatch(any())).thenReturn(new LemmaClassificationSuggestion.BatchResult(List.of(
                new LemmaClassificationSuggestion(candidates.get(0).getId(), "animals", "हाथी", "elephant", null)),
                "test-model"));

        ClassificationRunResponse response = newService().startRun("CURRICULUM", 1, 1, "admin");

        assertThat(response.classifiedLemmaCount()).isZero();
    }

    @Test
    void startRun_sortsCandidatesByTotalOccurrencesDesc() {
        List<Lemma> candidates = List.of(lemma(1, "MASCULINE"), lemma(5, "MASCULINE", "FEMININE"));
        when(lemmaRepo.findCandidatesForClassification(eq("CURRICULUM"), eq(ClassificationStatus.REJECTED)))
                .thenReturn(candidates);

        ArgumentCaptor<List> itemCaptor = ArgumentCaptor.forClass(List.class);
        when(llmClient.classifyBatch(itemCaptor.capture())).thenReturn(
                new LemmaClassificationSuggestion.BatchResult(List.of(), "test-model"));

        newService().startRun("CURRICULUM", 10, 1, "admin");

        List<LemmaClassificationPromptBuilder.LemmaBatchItem> items =
                ((List<LemmaClassificationPromptBuilder.LemmaBatchItem>) itemCaptor.getValue());
        assertThat(items).hasSize(2);
        // lemma5 total = 5+6=11, lemma1 total = 1 → самая частотная первая.
        assertThat(items.get(0).lemma().getLemmaSlp1()).isEqualTo("lemma5");
    }

    private LemmaClassification lastSaved() {
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }
}