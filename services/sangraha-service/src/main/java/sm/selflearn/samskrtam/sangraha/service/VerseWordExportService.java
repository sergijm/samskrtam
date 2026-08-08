package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Постраничный экспорт всех VerseWord из ANALYZED стихов для batch-импорта лексики
 * (lexicon-content-pipeline.md §2). Одна страница — {@code limit} стихов с их словами,
 * курсор — последний обработанный verseId.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerseWordExportService {

    private final VerseRepository verseRepository;
    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;

    @Transactional(readOnly = true)
    public VerseWordExportPageDto export(UUID cursor, int limit) {
        List<Verse> verses = verseRepository.findAllByStatusAndDeletedAtIsNullAndIdGreaterThan(
                VerseStatus.ANALYZED, cursor, PageRequest.of(0, limit));

        if (verses.isEmpty()) {
            return new VerseWordExportPageDto(List.of(), null);
        }

        Map<UUID, Chapter> chapters = loadChapters(verses);
        Map<UUID, Work> works = loadWorks(chapters.values());

        List<VerseWordExportItemDto> items = new ArrayList<>();
        for (Verse verse : verses) {
            Chapter chapter = chapters.get(verse.getChapterId());
            if (chapter == null) continue;
            Work work = works.get(chapter.getWorkId());
            if (work == null) continue;

            for (VerseWord word : verse.getVerseWords()) {
                items.add(new VerseWordExportItemDto(
                        verse.getId(),
                        work.getSlug(),
                        chapter.getSlug(),
                        verse.getOrderIndex(),
                        word.getLemmaIast(),
                        word.getStem(),
                        word.getSurfaceIast(),
                        word.getSurfaceDevanagari(),
                        word.getPos() == null ? null : word.getPos().name(),
                        word.getLemmaGlossRu(),
                        word.getLemmaGlossEn(),
                        word.getMorphology() == null || word.getMorphology().getGender() == null
                                ? null : word.getMorphology().getGender().name(),
                        null
                ));
            }
        }

        UUID nextCursor = verses.get(verses.size() - 1).getId();
        return new VerseWordExportPageDto(items, nextCursor);
    }

    private Map<UUID, Chapter> loadChapters(List<Verse> verses) {
        List<UUID> chapterIds = verses.stream()
                .map(Verse::getChapterId)
                .distinct()
                .toList();
        return chapterRepository.findAllById(chapterIds)
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Function.identity()));
    }

    private Map<UUID, Work> loadWorks(java.util.Collection<Chapter> chapters) {
        List<UUID> workIds = chapters.stream()
                .map(Chapter::getWorkId)
                .distinct()
                .toList();
        return workRepository.findAllById(workIds)
                .stream()
                .collect(Collectors.toMap(Work::getId, Function.identity()));
    }
}