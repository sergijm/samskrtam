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
        return verseMapper.toDetailDto(verse, analysis.orElse(null), words.isEmpty() ? null : words);
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
}
