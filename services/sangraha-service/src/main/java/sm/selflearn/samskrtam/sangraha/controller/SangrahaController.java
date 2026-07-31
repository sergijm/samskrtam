package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.dto.ChapterVersesDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseDetailDto;
import sm.selflearn.samskrtam.sangraha.dto.VocabularyQuizResponse;
import sm.selflearn.samskrtam.sangraha.dto.WorkTreeDto;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.service.ChapterService;
import sm.selflearn.samskrtam.sangraha.service.VerseService;
import sm.selflearn.samskrtam.sangraha.service.WorkService;
import sm.selflearn.samskrtam.sangraha.service.WorkTreeService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sangraha")
@RequiredArgsConstructor
public class SangrahaController {

        private final WorkService workService;
    private final VerseService verseService;
    private final WorkTreeService workTreeService;
    private final ChapterService chapterService;

    // ── Works (read-only) ───────────────────────────────────────────

    @GetMapping("/works")
    public ResponseEntity<List<Work>> getAllWorks() {
        return ResponseEntity.ok(workService.getAllWorks());
    }

    @GetMapping("/works/{workSlug}")
    public ResponseEntity<WorkTreeDto> getWorkTree(@PathVariable String workSlug) {
        return ResponseEntity.ok(workTreeService.getWorkTreeBySlug(workSlug));
    }

    // ── Chapters: single chapter with verses (NEW) ─────────────────

    @GetMapping("/chapters/{chapterId}/verses")
    public ResponseEntity<ChapterVersesDto> getChapterVerses(@PathVariable UUID chapterId) {
        return ResponseEntity.ok(chapterService.getChapterVersesByChapterId(chapterId));
    }
    // ── Verses (read-only + vocabulary-quiz) ──────────────────────
    @GetMapping("/verses/{verseId}")
    public ResponseEntity<VerseDetailDto> getVerse(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseDetail(verseId));
    }

    @PostMapping("/verses/{verseId}/vocabulary-quiz")
    public ResponseEntity<VocabularyQuizResponse> getOrCreateVocabularyQuiz(@PathVariable UUID verseId) {
        VocabularyQuizResponse response = verseService.getOrCreateVocabularyQuiz(verseId);
        return ResponseEntity.ok(response);
    }

    // ── Verse Analysis (read-only) ────────────────────────────────

    @GetMapping("/verses/{verseId}/analysis")
    public ResponseEntity<VerseAnalysis> getVerseAnalysis(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseAnalysis(verseId));
    }

    @GetMapping("/verses/{verseId}/words")
    public ResponseEntity<List<VerseWord>> getVerseWords(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseWords(verseId));
    }
}

