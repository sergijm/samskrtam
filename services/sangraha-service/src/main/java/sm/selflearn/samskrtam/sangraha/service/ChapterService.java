package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.ChapterSummaryDto;
import sm.selflearn.samskrtam.sangraha.dto.ChapterVersesDto;
import sm.selflearn.samskrtam.sangraha.dto.CreateChapterRequest;
import sm.selflearn.samskrtam.sangraha.dto.UpdateChapterRequest;
import sm.selflearn.samskrtam.sangraha.dto.VerseTreeDto;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseAnalysisRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChapterService {

    private final ChapterRepository chapterRepository;
    private final WorkService workService;
    private final VerseRepository verseRepository;
    private final VerseAnalysisRepository verseAnalysisRepository;

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

    // ── Write: создание/обновление главы (ADMIN, см. §4/§5.2) ──

    @Transactional
    public Chapter createChapter(UUID workId, CreateChapterRequest req) {
        if (req.titleRu() == null || req.titleRu().isBlank()) {
            throw new IllegalArgumentException("titleRu must not be blank");
        }
        workService.getWorkById(workId);
        String slug = SlugUtils.uniqueSlug(
                req.titleRu(),
                candidate -> chapterRepository.existsByWorkIdAndSlug(workId, candidate));
        int orderIndex = nextOrderIndex(workId);
        Chapter chapter = Chapter.builder()
                .workId(workId)
                .slug(slug)
                .orderIndex(orderIndex)
                .titleRu(req.titleRu())
                .titleEn(req.titleEn() == null ? req.titleRu() : req.titleEn())
                .titleSaIast(req.titleSaIast())
                .titleSaDevanagari(req.titleSaDevanagari())
                .build();
        return chapterRepository.save(chapter);
    }

    @Transactional
    public Chapter updateChapter(UUID chapterId, UpdateChapterRequest req) {
        Chapter chapter = getChapterById(chapterId);
        if (req.titleRu() != null) {
            chapter.setTitleRu(req.titleRu());
        }
        if (req.titleEn() != null) {
            chapter.setTitleEn(req.titleEn());
        }
        if (req.titleSaIast() != null) {
            chapter.setTitleSaIast(req.titleSaIast());
        }
        if (req.titleSaDevanagari() != null) {
            chapter.setTitleSaDevanagari(req.titleSaDevanagari());
        }
        return chapterRepository.save(chapter);
    }

    private int nextOrderIndex(UUID workId) {
        return getChaptersByWorkId(workId).stream()
                .map(Chapter::getOrderIndex)
                .filter(o -> o != null)
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0) + 1;
    }

    public static ChapterSummaryDto toSummary(Chapter ch, int verseCount) {
        return new ChapterSummaryDto(
                ch.getId(),
                ch.getSlug(),
                ch.getTitleRu(),
                ch.getTitleEn(),
                ch.getTitleSaIast(),
                ch.getTitleSaDevanagari(),
                ch.getOrderIndex() == null ? 0 : ch.getOrderIndex(),
                ch.getSlug(),
                verseCount
        );
    }

    // ── NEW: chapter summaries without verses (for WorkPage tree) ──

    @Transactional(readOnly = true)
    public List<ChapterSummaryDto> getChapterSummaryByWorkId(UUID workId) {
        List<Chapter> chapters = getChaptersByWorkId(workId);
        return chapters.stream()
                .map(ch -> new ChapterSummaryDto(
                        ch.getId(),
                        ch.getSlug(),
                        ch.getTitleRu(),
                        ch.getTitleEn(),
                        ch.getTitleSaIast(),
                        ch.getTitleSaDevanagari(),
                        ch.getOrderIndex(),
                        ch.getSlug(), // categoryCode = slug (legacy)
                        verseRepository.countByChapterIdAndDeletedAtIsNull(ch.getId())
                ))
                .toList();
    }

    // ── NEW: single chapter with its verses (for ChapterPage) ──

    @Transactional(readOnly = true)
    public ChapterVersesDto getChapterVersesByChapterId(UUID chapterId) {
        Chapter ch = getChapterById(chapterId);
        var verses = verseRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(ch.getId())
                .stream()
                .map(v -> {
                    Optional<VerseAnalysis> analysis =
                            verseAnalysisRepository.findByVerseId(v.getId());
                    return new VerseTreeDto(
                            v.getId(),
                            v.getOrderIndex(),
                            preview(v.getTextIast(), 80),
                            Optional.ofNullable(v.getTextIast()).orElse(v.getRawText()),
                            v.getTextDevanagari(),
                            analysis.map(VerseAnalysis::getTranslationRu).orElse(null),
                            analysis.map(VerseAnalysis::getTranslationEn).orElse(null),
                            v.getStatus());
                })
                .toList();
        return new ChapterVersesDto(
                ch.getId(),
                ch.getSlug(),
                ch.getTitleRu(),
                ch.getTitleEn(),
                ch.getTitleSaIast(),
                ch.getTitleSaDevanagari(),
                ch.getOrderIndex(),
                ch.getSlug(),
                verses
        );
    }

    // ── DEPRECATED: old tree method (kept for backward compat, will be removed) ──
    // Replaced by getChapterSummaryByWorkId() + getChapterVersesByChapterId()

    /**
     * Обрезка текста для превью. Используется деревом главы и батч-списком стихов
     * (VerseBatchService.fetchBatchReview), чтобы не плодить две копии утилиты.
     */
    public static String preview(String text, int max) {
        if (text == null) return null;
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
