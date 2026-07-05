package sm.selflearn.samskrtam.sangraha.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.dto.CreateWorkRequest;
import sm.selflearn.samskrtam.sangraha.dto.VerseDetailDto;
import sm.selflearn.samskrtam.sangraha.dto.WorkTreeDto;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Verse;
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
    private final ChapterService chapterService;
    private final VerseService verseService;
    private final WorkTreeService workTreeService;

    // ── Works ─────────────────────────────────────────────────────

    @GetMapping("/works")
    public ResponseEntity<List<Work>> getAllWorks() {
        return ResponseEntity.ok(workService.getAllWorks());
    }

    @PostMapping("/works")
    public ResponseEntity<Work> createWork(@Valid @RequestBody CreateWorkRequest request) {
        Work work = workService.createWorkFromTitle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(work);
    }

    @GetMapping("/works/{workSlug}")
    public ResponseEntity<WorkTreeDto> getWorkTree(@PathVariable String workSlug) {
        return ResponseEntity.ok(workTreeService.getWorkTreeBySlug(workSlug));
    }

    @PutMapping("/works/{workSlug}")
    public ResponseEntity<Work> updateWork(@PathVariable String workSlug, @RequestBody Work work) {
        return ResponseEntity.ok(workService.updateWorkBySlug(workSlug, work));
    }

    @DeleteMapping("/works/{workSlug}")
    public ResponseEntity<Void> deleteWork(@PathVariable String workSlug) {
        workService.deleteWorkBySlug(workSlug);
        return ResponseEntity.noContent().build();
    }

    // ── Chapters ──────────────────────────────────────────────────

    @PostMapping("/works/{workSlug}/chapters")
    public ResponseEntity<Chapter> createChapter(@PathVariable String workSlug, @RequestBody Chapter chapter) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chapterService.createChapterBySlug(workSlug, chapter));
    }

    @PutMapping("/chapters/{chapterId}")
    public ResponseEntity<Chapter> updateChapter(@PathVariable UUID chapterId, @RequestBody Chapter chapter) {
        return ResponseEntity.ok(chapterService.updateChapter(chapterId, chapter));
    }

    @DeleteMapping("/chapters/{chapterId}")
    public ResponseEntity<Void> deleteChapter(@PathVariable UUID chapterId) {
        chapterService.deleteChapter(chapterId);
        return ResponseEntity.noContent().build();
    }

    // ── Verses ────────────────────────────────────────────────────

    @PostMapping("/chapters/{chapterId}/verses")
    public ResponseEntity<Verse> createVerse(@PathVariable UUID chapterId, @RequestBody Verse verse) {
        return ResponseEntity.status(HttpStatus.CREATED).body(verseService.createVerse(chapterId, verse));
    }

    @GetMapping("/verses/{verseId}")
    public ResponseEntity<VerseDetailDto> getVerse(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseDetail(verseId));
    }

    /**
     * PUT /verses/{id}/text — единое поле text, backend определяет письменность
     * по Unicode-диапазону деванагари (\u0900–\u097F) и кладёт в textDevanagari
     * либо textIast соответственно.
     */
    @PutMapping("/verses/{verseId}/text")
    public ResponseEntity<Verse> updateVerseText(
            @PathVariable UUID verseId,
            @RequestBody VerseTextRequest request) {
        return ResponseEntity.ok(verseService.updateVerseText(verseId, request.text()));
    }

    @DeleteMapping("/verses/{verseId}")
    public ResponseEntity<Void> deleteVerse(@PathVariable UUID verseId) {
        verseService.deleteVerse(verseId);
        return ResponseEntity.noContent().build();
    }

    // ── Verse Analysis ────────────────────────────────────────────

    @GetMapping("/verses/{verseId}/analysis")
    public ResponseEntity<VerseAnalysis> getVerseAnalysis(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseAnalysis(verseId));
    }

    @GetMapping("/verses/{verseId}/words")
    public ResponseEntity<List<VerseWord>> getVerseWords(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseWords(verseId));
    }
}