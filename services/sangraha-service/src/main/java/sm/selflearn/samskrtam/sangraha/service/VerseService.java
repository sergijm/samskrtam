package sm.selflearn.samskrtam.sangraha.service;

import sm.selflearn.samskrtam.common.transliteration.TransliterationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.CreateVerseRequest;
import sm.selflearn.samskrtam.sangraha.dto.VerseDetailDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseTreeDto;
import sm.selflearn.samskrtam.sangraha.mapper.VerseMapper;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseAnalysisRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerseService {

            private final VerseRepository verseRepository;
    private final VerseAnalysisRepository verseAnalysisRepository;
    private final VerseWordRepository verseWordRepository;
    private final ChapterRepository chapterRepository;
    private final ChapterService chapterService;
    private final VerseMapper verseMapper;
    private final WorkService workService;
    private final TransliterationService transliterationService;
    private final VerseBatchPushService verseBatchPushService;

    @Transactional(readOnly = true)
    public List<Verse> getVersesByChapterId(UUID chapterId) {
        chapterService.getChapterById(chapterId);
        return verseRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(chapterId);
    }

    @Transactional(readOnly = true)
    public VerseDetailDto getVerseDetail(UUID id) {
        Verse verse = getVerseById(id);
        Optional<VerseAnalysis> analysis = verseAnalysisRepository.findByVerseId(id);
        List<VerseWord> words = verseWordRepository.findAllByVerse_IdOrderByPositionAsc(id);

        return verseMapper.toDetailDto(verse, analysis.orElse(null),
                words.isEmpty() ? null : words,
                resolveVerseTopicCode(verse));
    }

    /**
     * Код VERSE-урока (lexicon-content-pipeline.md §7). Для стиха в главе —
     * {@code "{workSlp1}_{chapterNumber}"}, для standalone-стиха (без главы) —
     * персональный {@code "user-{ownerId}"} (совпадает с VerseBatchPushService).
     */
    private String resolveVerseTopicCode(Verse verse) {
        if (verse.getChapterId() == null) {
            return verse.getOwnerId() == null ? null : "user-" + verse.getOwnerId();
        }
        Chapter chapter = chapterRepository.findById(verse.getChapterId()).orElse(null);
        if (chapter == null) {
            return null;
        }
        Work work = workService.getWorkById(chapter.getWorkId());
        String workSlp1 = transliterationService.iastToSlp1(work.getSlug());
        String base = workSlp1 == null || workSlp1.isBlank() ? "verse" : workSlp1;
        int chapterNumber = chapter.getOrderIndex() == null ? 0 : chapter.getOrderIndex();
        return base + "_" + chapterNumber;
    }

    @Transactional(readOnly = true)
    public Verse getVerseById(UUID id) {
        return verseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Verse not found: " + id));
    }

    // ── Write: создание стиха (DRAFT + rawText, ADMIN, см. §4/§5.2) ──

    @Transactional
    public VerseTreeDto createVerse(UUID chapterId, CreateVerseRequest req) {
        if (req.text() == null || req.text().isBlank()) {
            throw new IllegalArgumentException("Verse text must not be blank");
        }
        chapterService.getChapterById(chapterId);
        int orderIndex = verseRepository.countByChapterIdAndDeletedAtIsNull(chapterId) + 1;
        Instant now = Instant.now();
        Verse verse = Verse.builder()
                .chapterId(chapterId)
                .orderIndex(orderIndex)
                .rawText(req.text())
                .status(VerseStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
        verse = verseRepository.save(verse);
        return toTreeDto(verse);
    }

    public static VerseTreeDto toTreeDto(Verse v) {
        return new VerseTreeDto(
                v.getId(),
                v.getOrderIndex(),
                ChapterService.preview(v.getRawText(), 80),
                v.getRawText(),
                null,
                null,
                null,
                v.getStatus());
    }

    @Transactional(readOnly = true)
    public VerseAnalysis getVerseAnalysis(UUID verseId) {
        return verseAnalysisRepository.findByVerseId(verseId)
                .orElseThrow(() -> new RuntimeException("Verse analysis not found: " + verseId));
    }

    /**
     * On-demand экспорт пачки лемм стиха в curriculum-service (кнопка «Изучить»).
     * Идемпотентен — повторный вызов не создаёт дублей (см. VerseBatchPushService).
     * Возвращает код VERSE-урока, на который ведёт кнопка.
     */
    public String triggerStudyExport(UUID verseId) {
        Verse verse = getVerseById(verseId);
        if (verse.getStatus() != VerseStatus.ANALYZED) {
            throw new RuntimeException("Verse is not analyzed yet: " + verseId);
        }
        Chapter chapter = null;
        Work work = null;
        if (verse.getChapterId() != null) {
            chapter = chapterRepository.findByIdAndDeletedAtIsNull(verse.getChapterId()).orElse(null);
            if (chapter != null) {
                work = workService.getWorkById(chapter.getWorkId());
            }
        }
        List<VerseWord> words = verseWordRepository.findAllByVerse_IdOrderByPositionAsc(verseId);
        verseBatchPushService.push(verse, work, chapter, words);
        String code = resolveVerseTopicCode(verse);
        if (code == null) {
            throw new RuntimeException("Cannot resolve lesson code for verse: " + verseId);
        }
        return code;
    }

    @Transactional(readOnly = true)
    public List<VerseWord> getVerseWords(UUID verseId) {
        return verseWordRepository.findAllByVerse_IdOrderByPositionAsc(verseId);
    }
}

