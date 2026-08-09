package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.StandaloneVerseItemDto;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тесты standalone-стихов пользователя (страница /analysis): verse.chapter_id = null,
 * персональное владение, создание + анализ одним действием.
 */
class StandaloneVerseServiceTest {

    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private VerseRepository verseRepository;
    private VerseAnalysisService verseAnalysisService;
    private StandaloneVerseService service;

    private Verse verse(UUID id) {
        return Verse.builder()
                .id(id)
                .chapterId(null)
                .ownerId(OWNER)
                .orderIndex(0)
                .rawText("अहं गच्छामि")
                .status(VerseStatus.DRAFT)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    @BeforeEach
    void setUp() {
        verseRepository = mock(VerseRepository.class);
        verseAnalysisService = mock(VerseAnalysisService.class);
        service = new StandaloneVerseService(verseRepository, verseAnalysisService);
    }

    @Test
    void createAndAnalyze_validText_createsStandaloneVerseWithOwnerAndRunsAnalysis() {
        UUID createdId = UUID.randomUUID();
        when(verseRepository.save(any(Verse.class))).thenAnswer(inv -> {
            Verse v = inv.getArgument(0);
            v.setId(createdId);
            return v;
        });

        Verse result = service.createAndAnalyze("अहं गच्छामि", OWNER);

        assertThat(result.getId()).isEqualTo(createdId);
        assertThat(result.getChapterId()).isNull();
        assertThat(result.getOwnerId()).isEqualTo(OWNER);
        assertThat(result.getOrderIndex()).isZero();
        assertThat(result.getRawText()).isEqualTo("अहं गच्छामि");
        assertThat(result.getStatus()).isEqualTo(VerseStatus.DRAFT);
        verify(verseAnalysisService).analyze(eq(createdId), eq("अहं गच्छामि"));
    }

    @Test
    void createAndAnalyze_blankText_throwsWithoutSaving() {
        assertThatThrownBy(() -> service.createAndAnalyze("   ", OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
        verify(verseRepository, never()).save(any());
    }

    @Test
    void list_ownerVerses_mapsToItemsNewestFirst() {
        Verse a = verse(UUID.randomUUID());
        Verse b = verse(UUID.randomUUID());
        b.setTextIast("aham gacchāmi");
        when(verseRepository.findAllByChapterIdIsNullAndOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(OWNER))
                .thenReturn(List.of(a, b));

        List<StandaloneVerseItemDto> items = service.list(OWNER);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).preview().length()).isLessThanOrEqualTo(120);
        assertThat(items.get(0).status()).isEqualTo(VerseStatus.DRAFT);
        assertThat(items.get(1).preview()).isEqualTo("aham gacchāmi");
    }

    @Test
    void delete_ownedStandaloneVerse_softDeletes() {
        UUID verseId = UUID.randomUUID();
        Verse v = verse(verseId);
        when(verseRepository.findByIdAndDeletedAtIsNull(verseId))
                .thenReturn(Optional.of(v));

        service.delete(verseId, OWNER);

        assertThat(v.getDeletedAt()).isNotNull();
        verify(verseRepository).save(v);
    }

    @Test
    void delete_notOwner_throwsAndDoesNotDelete() {
        UUID verseId = UUID.randomUUID();
        Verse v = verse(verseId);
        when(verseRepository.findByIdAndDeletedAtIsNull(verseId))
                .thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.delete(verseId, UUID.randomUUID()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong");
        assertThat(v.getDeletedAt()).isNull();
        verify(verseRepository, never()).save(v);
    }

    @Test
    void delete_verseOfCorpus_throws() {
        UUID verseId = UUID.randomUUID();
        Verse v = verse(verseId);
        v.setChapterId(UUID.randomUUID());
        when(verseRepository.findByIdAndDeletedAtIsNull(verseId))
                .thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.delete(verseId, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not standalone");
    }
}