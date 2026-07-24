package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.ChapterTreeDto;
import sm.selflearn.samskrtam.sangraha.dto.CreateChapterRequest;
import sm.selflearn.samskrtam.sangraha.dto.UpdateChapterRequest;
import sm.selflearn.samskrtam.sangraha.dto.VerseTreeDto;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseAnalysisRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final WorkService workService;
    private final VerseRepository verseRepository;
    private final VerseAnalysisRepository verseAnalysisRepository;
    private final ChapterTitleService chapterTitleService;
    @Transactional(readOnly = true)
    public List<Chapter> getChaptersByWorkId(UUID workId) {
        workService.getWorkById(workId);
        return chapterRepository.findAllByWorkIdAndDeletedAtIsNullOrderByOrderIndexAsc(workId);
    }

    @Transactional(readOnly = true)
    public Chapter getChapterById(UUID id) {
        return chapterRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ChapterTreeDto> getChapterTreeByWorkId(UUID workId) {
        List<Chapter> chapters = getChaptersByWorkId(workId);
        return chapters.stream()
                .map(ch -> new ChapterTreeDto(
                        ch.getId(),
                        ch.getSlug(),
                        ch.getTitleRu(),
                        ch.getTitleEn(),
                        ch.getTitleSaIast(),
                        ch.getTitleSaDevanagari(),
                        ch.getOrderIndex(),
                        ch.getSlug(),
                        verseRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(ch.getId())
                                .stream()
                                .map(v -> {
                                    Optional<VerseAnalysis> analysis =
                                            verseAnalysisRepository.findByVerseId(v.getId());
                                    return new VerseTreeDto(
                                            v.getId(),
                                            v.getOrderIndex(),
                                            preview(v.getTextIast(), 80),
                                            v.getTextIast(),
                                            v.getTextDevanagari(),
                                            analysis.map(VerseAnalysis::getTranslationRu).orElse(null),
                                            analysis.map(VerseAnalysis::getTranslationEn).orElse(null),
                                            v.getStatus());
                                })
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public Chapter createChapterBySlug(String workSlug, CreateChapterRequest request) {
        Work work = workService.getWorkBySlug(workSlug);
        return createChapterFromTitle(work.getId(), request);
    }

    /**
     * Создание главы из сырого заголовка — делегирует {@link ChapterTitleService#createFromTitle}.
     */
    @Transactional
    public Chapter createChapterFromTitle(UUID workId, CreateChapterRequest request) {
        return chapterTitleService.createFromTitle(workId, request);
    }
    /**
     * Обновление главы через title — делегирует {@link ChapterTitleService#updateFromTitle}.
     * Если title не передан — обновляется только orderIndex.
     */
    @Transactional
    public Chapter updateChapterFromTitle(UUID chapterId, UpdateChapterRequest request) {
        Chapter chapter = getChapterById(chapterId);
        return chapterTitleService.updateFromTitle(chapterId, request, chapter);
    }

    /**
     * @deprecated — заменён на createChapterFromTitle. Сохранён для обратной совместимости
     * до полного перехода на DTO в контроллере.
     */
    @Deprecated
    @Transactional
    public Chapter createChapter(UUID workId, Chapter chapter) {
        workService.getWorkById(workId);
        if (chapterRepository.existsByWorkIdAndSlug(workId, chapter.getSlug())) {
            throw new RuntimeException("Chapter with slug '" + chapter.getSlug() + "' already exists in this work");
        }
        chapter.setWorkId(workId);
        return chapterRepository.save(chapter);
    }

    @Transactional
    public Chapter updateChapter(UUID id, Chapter update) {
        Chapter chapter = getChapterById(id);
        chapter.setTitleRu(update.getTitleRu());
        chapter.setTitleEn(update.getTitleEn());
        chapter.setOrderIndex(update.getOrderIndex());
        return chapterRepository.save(chapter);
    }

    @Transactional
    public void deleteChapter(UUID id) {
        Chapter chapter = getChapterById(id);
        chapter.setDeletedAt(Instant.now());
        chapterRepository.save(chapter);
    }

    private static String preview(String text, int max) {
        if (text == null) return null;
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}