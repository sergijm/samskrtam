package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExportItemDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExportPageDto;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerseWordExportServiceTest {

    private Verse verse(UUID id, VerseStatus status) {
        Verse v = new Verse();
        v.setId(id);
        v.setStatus(status);
        v.setChapterId(UUID.fromString("00000000-0000-0000-0000-00000000000a"));
        return v;
    }

    @Test
    void export_onlyAnalyzedVerses_areWalked() {
        VerseRepository verseRepo = mock(VerseRepository.class);
        ChapterRepository chapterRepo = mock(ChapterRepository.class);
        WorkRepository workRepo = mock(WorkRepository.class);

        UUID analyzedId = UUID.randomUUID();
        Verse analyzed = verse(analyzedId, VerseStatus.ANALYZED);
        VerseWord word = new VerseWord();
        word.setLemmaIast("nara");
        word.setStem("nara");
        word.setSurfaceIast("naraḥ");
        word.setSurfaceDevanagari("नरः");
        analyzed.setVerseWords(List.of(word));

        UUID chapterId = UUID.randomUUID();
        UUID workId = UUID.randomUUID();
        analyzed.setChapterId(chapterId);

        when(verseRepo.findAllByStatusAndDeletedAtIsNullAndIdGreaterThan(
                eq(VerseStatus.ANALYZED), any(), any()))
                .thenReturn(List.of(analyzed));

        Work work = new Work();
        work.setId(workId);
        work.setSlug("gita");
        Chapter chapter = new Chapter();
        chapter.setId(chapterId);
        chapter.setWorkId(workId);
        chapter.setSlug("ch1");
        when(chapterRepo.findAllById(any())).thenReturn(List.of(chapter));
        when(workRepo.findAllById(any())).thenReturn(List.of(work));

        VerseWordExportService service =
                new VerseWordExportService(verseRepo, chapterRepo, workRepo);

        VerseWordExportPageDto page = service.export(null, 500);

        assertThat(page.nextCursor()).isEqualTo(analyzedId);
        assertThat(page.items()).hasSize(1);
        VerseWordExportItemDto item = page.items().get(0);
        assertThat(item.lemmaIast()).isEqualTo("nara");
        assertThat(item.workSlug()).isEqualTo("gita");
        assertThat(item.chapterSlug()).isEqualTo("ch1");
    }

    @Test
    void export_noVerse_returnsNullCursor() {
        VerseRepository verseRepo = mock(VerseRepository.class);
        when(verseRepo.findAllByStatusAndDeletedAtIsNullAndIdGreaterThan(
                eq(VerseStatus.ANALYZED), any(), any())).thenReturn(List.of());

        VerseWordExportService service =
                new VerseWordExportService(verseRepo, mock(ChapterRepository.class), mock(WorkRepository.class));

        VerseWordExportPageDto page = service.export(UUID.randomUUID(), 500);

        assertThat(page.items()).isEmpty();
        assertThat(page.nextCursor()).isNull();
    }
}