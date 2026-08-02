package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты POST /api/v1/sangraha/verse/analysis (sangraha-service/batch-verse-review.md, B2/B3):
 * чанкинг списка произвольного размера и безусловный повторный анализ (включая ANALYZED).
 */
class VerseAnalysisServiceTest {

    private VerseRepository verseRepository;
    private ChapterRepository chapterRepository;
    private WorkRepository workRepository;
    private LlmClient llmClient;
    private VerseAnalysisSaver analysisSaver;
    private VerseAnalysisResponseNormalizer responseNormalizer;
    private VerseAnalysisService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int ANALYSIS_CHUNK_SIZE = 20;

    private static UUID uuid(String suffix) {
        return UUID.fromString("00000000-0000-0000-0000-" + suffix);
    }

    @BeforeEach
    void setUp() {
        verseRepository = mock(VerseRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        workRepository = mock(WorkRepository.class);
        llmClient = mock(LlmClient.class);
        analysisSaver = mock(VerseAnalysisSaver.class);
        responseNormalizer = mock(VerseAnalysisResponseNormalizer.class);
        ToolCallValidator toolCallValidator = mock(ToolCallValidator.class);
        JsonSchemas jsonSchemas = mock(JsonSchemas.class);
        LlmProperties llmProperties = mock(LlmProperties.class);
        service = new VerseAnalysisService(
                verseRepository, chapterRepository, workRepository, llmClient,
                analysisSaver, responseNormalizer, toolCallValidator, jsonSchemas,
                llmProperties, objectMapper);
    }

    @Test
    void analyzeVerses_overChunkSize_splitsIntoSeveralSequentialChunks() {
        int total = (int) (2.5 * ANALYSIS_CHUNK_SIZE); // 50 → 20 + 20 + 10
        List<UUID> ids = IntStream.range(0, total)
                .mapToObj(i -> uuid(String.format("%012d", i)))
                .toList();
        List<Verse> verses = ids.stream()
                .map(id -> Verse.builder().id(id).build())
                .toList();
        when(verseRepository.findAllByIdInAndDeletedAtIsNull(anyCollection()))
                .thenReturn(verses);
        when(llmClient.call(anyList())).thenReturn(objectMapper.createObjectNode());
        when(responseNormalizer.normalizeToVersesArray(any(JsonNode.class)))
                .thenReturn(objectMapper.createArrayNode());
        when(llmClient.extractModelName(any(JsonNode.class))).thenReturn("test-model");

        List<UUID> accepted = service.analyzeVerses(ids);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<Verse>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(llmClient, times(3)).call(captor.capture());

        List<List<Verse>> chunks = captor.getAllValues();
        assertThat(chunks).hasSize(3);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.size()).isLessThanOrEqualTo(ANALYSIS_CHUNK_SIZE));
        assertThat(chunks.stream().mapToInt(List::size).sum()).isEqualTo(total);
        assertThat(chunks.stream().flatMap(List::stream).map(Verse::getId).toList())
                .containsExactlyInAnyOrder(ids.toArray(UUID[]::new));

        assertThat(accepted).containsExactlyInAnyOrder(ids.toArray(UUID[]::new));
    }

    @Test
    void analyzeVerses_analyzedVerseInList_notFilteredAndReanalyzed() {
        UUID analyzedId = uuid("000000000001");
        UUID draftId = uuid("000000000002");
        UUID chapterId = uuid("000000000003");
        UUID workId = uuid("000000000004");

        Verse analyzed = Verse.builder().id(analyzedId).chapterId(chapterId)
                .orderIndex(1).textIast("old").status(VerseStatus.ANALYZED).build();
        Verse draft = Verse.builder().id(draftId).chapterId(chapterId)
                .orderIndex(2).textIast("new").status(VerseStatus.DRAFT).build();

        when(verseRepository.findAllByIdInAndDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(analyzed, draft));
        when(llmClient.call(anyList())).thenReturn(validLlmResponse(2));
        when(responseNormalizer.normalizeToVersesArray(any(JsonNode.class)))
                .thenReturn(validVersesArray(2));
        when(llmClient.extractModelName(any(JsonNode.class))).thenReturn("test-model");
        when(chapterRepository.findByIdAndDeletedAtIsNull(any(UUID.class)))
                .thenReturn(java.util.Optional.of(Chapter.builder().id(chapterId).workId(workId).build()));
        when(workRepository.findById(any(UUID.class)))
                .thenReturn(java.util.Optional.of(Work.builder().id(workId).build()));

        List<UUID> accepted = service.analyzeVerses(List.of(analyzedId, draftId));

        assertThat(accepted).containsExactly(analyzedId, draftId);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<Verse>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(llmClient, times(1)).call(captor.capture());
        List<Verse> submitted = captor.getValue();
        assertThat(submitted).extracting(Verse::getId).containsExactly(analyzedId, draftId);

        // Перезапись существующего VerseAnalysis: saveResults вызывается и для ANALYZED стиха.
        verify(analysisSaver, times(2)).saveResults(
                any(Verse.class), any(Work.class), any(Chapter.class),
                any(String.class), any(String.class), any(String.class), any(String.class),
                any(JsonNode.class), any(JsonNode.class),
                any(String.class), eq("test-model"), eq("test-model"));
    }

    @Test
    void analyzeVerses_missingAndDeletedIds_skippedSilently() {
        UUID existingId = uuid("000000000001");
        UUID missingId = uuid("000000000002");
        Verse verse = Verse.builder().id(existingId).build();
        when(verseRepository.findAllByIdInAndDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(verse));
        when(llmClient.call(anyList())).thenReturn(objectMapper.createObjectNode());
        when(responseNormalizer.normalizeToVersesArray(any(JsonNode.class)))
                .thenReturn(objectMapper.createArrayNode());
        when(llmClient.extractModelName(any(JsonNode.class))).thenReturn("test-model");

        List<UUID> accepted = service.analyzeVerses(List.of(missingId, existingId));

        assertThat(accepted).containsExactly(existingId);
        verify(llmClient, times(1)).call(anyList());
    }

    @Test
    void analyzeVerses_emptyList_returnsEmptyWithoutLlmCall() {
        List<UUID> accepted = service.analyzeVerses(List.of());

        assertThat(accepted).isEmpty();
        verify(llmClient, times(0)).call(anyList());
    }

    private JsonNode validLlmResponse(int verseCount) {
        ObjectNode response = objectMapper.createObjectNode();
        response.set("verses", validVersesArray(verseCount));
        return response;
    }

    private ArrayNode validVersesArray(int verseCount) {
        ArrayNode verses = objectMapper.createArrayNode();
        for (int i = 0; i < verseCount; i++) {
            ObjectNode entry = objectMapper.createObjectNode();
            entry.put("verseIndex", i);
            entry.put("textDevanagari", "ॐ");
            entry.put("textIast", "oṁ");
            entry.put("translationRu", "ом");
            entry.put("translationEn", "om");
            entry.set("sandhiSplits", objectMapper.createArrayNode());
            entry.set("words", objectMapper.createArrayNode());
            verses.add(entry);
        }
        return verses;
    }
}
