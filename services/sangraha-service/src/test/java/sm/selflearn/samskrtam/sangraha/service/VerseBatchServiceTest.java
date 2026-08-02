package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.VerseBatchItemDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseBatchResponseDto;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseAnalysisRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты GET /api/v1/sangraha/verse (sangraha-service/batch-verse-review.md, B1):
 * отсутствующие id молча пропускаются, status отдаётся как есть (включая DRAFT),
 * порядок ответа — по порядку id в запросе.
 */
class VerseBatchServiceTest {

    private VerseRepository verseRepository;
    private ChapterRepository chapterRepository;
    private WorkRepository workRepository;
    private VerseBatchService service;

    private static UUID uuid(String suffix) {
        return UUID.fromString("00000000-0000-0000-0000-" + suffix);
    }

    @BeforeEach
    void setUp() {
        verseRepository = mock(VerseRepository.class);
        VerseAnalysisRepository verseAnalysisRepository = mock(VerseAnalysisRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        workRepository = mock(WorkRepository.class);
        service = new VerseBatchService(verseRepository, verseAnalysisRepository,
                chapterRepository, workRepository);
    }

    @Test
    void fetchBatchReview_existingAndMissingId_returnsOnlyExistingWithoutError() {
        UUID existingId = uuid("000000000001");
        UUID missingId = uuid("000000000002");
        UUID chapterId = uuid("000000000003");
        UUID workId = uuid("000000000004");

        Verse draft = Verse.builder()
                .id(existingId)
                .chapterId(chapterId)
                .orderIndex(7)
                .textIast("śrī bhagavān uvāca")
                .status(VerseStatus.DRAFT)
                .build();
        when(verseRepository.findAllByIdInAndDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(draft));
        when(chapterRepository.findAllById(List.of(chapterId)))
                .thenReturn(List.of(Chapter.builder()
                        .id(chapterId)
                        .workId(workId)
                        .slug("bhagavad-gita.1")
                        .titleRu("Первая глава")
                        .titleEn("Chapter 1")
                        .build()));
        when(workRepository.findAllById(List.of(workId)))
                .thenReturn(List.of(Work.builder()
                        .id(workId)
                        .slug("bhagavad-gita")
                        .titleRu("Бхагавад-гита")
                        .titleEn("Bhagavad-gita")
                        .build()));

        VerseBatchResponseDto response =
                service.fetchBatchReview(List.of(missingId, existingId));

        assertThat(response.verses()).hasSize(1);
        VerseBatchItemDto item = response.verses().get(0);
        assertThat(item.id()).isEqualTo(existingId);
        assertThat(item.workSlug()).isEqualTo("bhagavad-gita");
        assertThat(item.workTitleRu()).isEqualTo("Бхагавад-гита");
        assertThat(item.chapterSlug()).isEqualTo("bhagavad-gita.1");
        assertThat(item.verseOrderIndex()).isEqualTo(7);
        assertThat(item.textIastPreview()).isEqualTo("śrī bhagavān uvāca");
        assertThat(item.status()).isEqualTo(VerseStatus.DRAFT);
    }

    @Test
    void fetchBatchReview_draftWithoutTextIast_usesRawTextAsPreview() {
        UUID verseId = uuid("000000000020");
        UUID chapterId = uuid("000000000021");
        UUID workId = uuid("000000000022");
        String rawText = "karmaṇyevādhikāras te mā phaleṣu kadācana";

        Verse draft = Verse.builder()
                .id(verseId)
                .chapterId(chapterId)
                .orderIndex(1)
                .textIast(null)
                .rawText(rawText)
                .status(VerseStatus.DRAFT)
                .build();
        when(verseRepository.findAllByIdInAndDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(draft));
        when(chapterRepository.findAllById(List.of(chapterId)))
                .thenReturn(List.of(Chapter.builder().id(chapterId).workId(workId)
                        .slug("c").titleRu("C").titleEn("C").build()));
        when(workRepository.findAllById(List.of(workId)))
                .thenReturn(List.of(Work.builder().id(workId).slug("w")
                        .titleRu("W").titleEn("W").build()));

        VerseBatchResponseDto response = service.fetchBatchReview(List.of(verseId));

        assertThat(response.verses()).hasSize(1);
        assertThat(response.verses().get(0).textIastPreview()).isEqualTo(rawText);
    }

    @Test
    void fetchBatchReview_preservesRequestOrder() {
        UUID chapterId = uuid("000000000010");
        UUID workId = uuid("000000000011");
        UUID firstId = uuid("000000000001");
        UUID secondId = uuid("000000000002");

        Verse first = Verse.builder().id(firstId).chapterId(chapterId)
                .orderIndex(1).textIast("one").status(VerseStatus.ANALYZED).build();
        Verse second = Verse.builder().id(secondId).chapterId(chapterId)
                .orderIndex(2).textIast("two").status(VerseStatus.ANALYZED).build();
        // Репозиторий вернул в обратном порядке — сервис должен выстроить по запросу.
        when(verseRepository.findAllByIdInAndDeletedAtIsNull(anyCollection()))
                .thenReturn(List.of(second, first));
        when(chapterRepository.findAllById(List.of(chapterId)))
                .thenReturn(List.of(Chapter.builder().id(chapterId).workId(workId)
                        .slug("c").titleRu("C").titleEn("C").build()));
        when(workRepository.findAllById(List.of(workId)))
                .thenReturn(List.of(Work.builder().id(workId).slug("w")
                        .titleRu("W").titleEn("W").build()));

        VerseBatchResponseDto response =
                service.fetchBatchReview(List.of(secondId, firstId));

        assertThat(response.verses()).extracting(VerseBatchItemDto::id)
                .containsExactly(secondId, firstId);
    }

    @Test
    void fetchBatchReview_emptyIds_returnsEmptyVerses() {
        VerseBatchResponseDto response = service.fetchBatchReview(List.of());

        assertThat(response.verses()).isEmpty();
    }

    @Test
    void fetchBatchReview_nullIds_returnsEmptyVerses() {
        VerseBatchResponseDto response = service.fetchBatchReview(null);

        assertThat(response.verses()).isEmpty();
    }
}
