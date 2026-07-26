package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.VerseDetailDto;
import sm.selflearn.samskrtam.sangraha.mapper.VerseMapper;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseAnalysisRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

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
    private final WorkRepository workRepository;
    private final ChapterService chapterService;
    private final VerseMapper verseMapper;
    private final TransliterationService transliterationService;

    @Transactional(readOnly = true)
    public List<Verse> getVersesByChapterId(UUID chapterId) {
        chapterService.getChapterById(chapterId);
        return verseRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(chapterId);
    }

        @Transactional(readOnly = true)
    public VerseDetailDto getVerseDetail(UUID id) {
        Verse verse = getVerseById(id);
        Optional<VerseAnalysis> analysis = verseAnalysisRepository.findByVerseId(id);
        List<VerseWord> words = verseWordRepository.findAllByVerseIdOrderByPositionAsc(id);

        // Получаем workSlug через chapter → work для vocabularyQuizSlug
        Chapter chapter = chapterRepository.findByIdAndDeletedAtIsNull(verse.getChapterId())
                .orElseThrow(() -> new RuntimeException("Chapter not found: " + verse.getChapterId()));
        Work work = workRepository.findById(chapter.getWorkId())
                .orElseThrow(() -> new RuntimeException("Work not found: " + chapter.getWorkId()));
        String vocabularyQuizSlug = work.getSlug();

        // vocabularyQuizAvailable — есть ли хотя бы одно синхронизированное слово
        // на уровне произведения
        boolean vocabularyQuizAvailable = verseWordRepository
                .existsSyncedWordsByWorkId(work.getId());
        int uniqueWordCount = vocabularyQuizAvailable
                ? verseWordRepository.countDistinctSyncedWordsByWorkId(work.getId())
                : 0;

        return verseMapper.toDetailDto(verse, analysis.orElse(null),
                words.isEmpty() ? null : words,
                vocabularyQuizSlug, vocabularyQuizAvailable, uniqueWordCount);
    }

    @Transactional(readOnly = true)
    public Verse getVerseById(UUID id) {
        return verseRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Verse not found: " + id));
    }

    @Transactional(readOnly = true)
    public VerseAnalysis getVerseAnalysis(UUID verseId) {
        return verseAnalysisRepository.findByVerseId(verseId)
                .orElseThrow(() -> new RuntimeException("Verse analysis not found: " + verseId));
    }

    @Transactional(readOnly = true)
    public List<VerseWord> getVerseWords(UUID verseId) {
        return verseWordRepository.findAllByVerseIdOrderByPositionAsc(verseId);
    }

    @Transactional
    public Verse createVerse(UUID chapterId, Verse verse) {
        chapterService.getChapterById(chapterId);
        verse.setChapterId(chapterId);
        verse.setStatus(VerseStatus.DRAFT);
        verse.setCreatedAt(Instant.now());
        return verseRepository.save(verse);
    }

    /**
     * PUT /verses/{id}/text — единое поле text.
     * Backend определяет письменность по Unicode-диапазону деванагари (\u0900–\u097F)
     * и кладёт в textDevanagari либо textIast.
     */
    @Transactional
    public Verse updateVerseText(UUID id, String text) {
        Verse verse = getVerseById(id);
        String script = transliterationService.detectScript(text);
        if ("devanagari".equals(script)) {
            verse.setTextDevanagari(text);
            verse.setTextIast(null);
        } else {
            verse.setTextIast(text);
            verse.setTextDevanagari(null);
        }
        verse.setUpdatedAt(Instant.now());
        return verseRepository.save(verse);
    }

    @Transactional
    public void deleteVerse(UUID id) {
        Verse verse = getVerseById(id);
        verse.setDeletedAt(Instant.now());
        verseRepository.save(verse);
    }

    /**
     * PUT /verses/{id} — обновление orderIndex и rawText.
     * Если текст изменился (rawText не равен сохранённому) — очищает результаты анализа
     * и сбрасывает статус в DRAFT.
     */
    @Transactional
    public Verse updateVerse(UUID id, int orderIndex, String rawText) {
        Verse verse = getVerseById(id);
        verse.setOrderIndex(orderIndex);
        verse.setUpdatedAt(Instant.now());

        if (rawText != null) {
            boolean textChanged = !rawText.equals(verse.getRawText());
            verse.setRawText(rawText);
            if (textChanged) {
                // Очистить результаты анализа
                verseAnalysisRepository.deleteByVerseId(id);
                verseWordRepository.deleteAllByVerseId(id);
                verse.setTextDevanagari(null);
                verse.setTextIast(null);
                verse.setStatus(VerseStatus.DRAFT);
            }
        }

        return verseRepository.save(verse);
    }
}

