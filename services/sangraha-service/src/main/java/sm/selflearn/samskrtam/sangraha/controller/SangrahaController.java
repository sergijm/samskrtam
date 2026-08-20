package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.dto.ChapterVersesDto;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesSearchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseBatchResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseDetailDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExamplesRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExamplesResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.WorksClassGroupDto;
import sm.selflearn.samskrtam.sangraha.dto.WorkTreeDto;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.service.ChapterService;
import sm.selflearn.samskrtam.sangraha.service.VerseBatchService;
import sm.selflearn.samskrtam.sangraha.service.VerseService;
import sm.selflearn.samskrtam.sangraha.service.VerseWordExamplesService;
import sm.selflearn.samskrtam.sangraha.service.VerseWordSearchService;
import sm.selflearn.samskrtam.sangraha.service.WorkService;
import sm.selflearn.samskrtam.sangraha.service.WorkTreeService;
import sm.selflearn.samskrtam.sangraha.service.WorksClassService;

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
    private final VerseBatchService verseBatchService;
    private final WorksClassService worksClassService;
    private final VerseWordExamplesService verseWordExamplesService;
    private final VerseWordSearchService verseWordSearchService;

    // ── Works (read-only) ───────────────────────────────────────────

    @GetMapping("/works")
    public ResponseEntity<List<Work>> getAllWorks(
            @RequestParam(value = "classId", required = false) List<UUID> classIds) {
        return ResponseEntity.ok(worksClassService.filterWorks(classIds));
    }

    @GetMapping("/works/classes")
    public ResponseEntity<List<WorksClassGroupDto>> getWorksClasses() {
        return ResponseEntity.ok(worksClassService.getClassGroups());
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

    // ── Verses: произвольный список id (batch-verse-review.md) ─────

    @PostMapping("/verse")
    public ResponseEntity<VerseBatchResponseDto> getVersesByIds(
            @RequestBody VersesBatchRequestDto request) {
        return ResponseEntity.ok(verseBatchService.fetchBatchReview(request.verseIds()));
    }

    // ── Verses (read-only) ──────────────────────────────────────────
    @GetMapping("/verses/{verseId}")
    public ResponseEntity<VerseDetailDto> getVerse(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseDetail(verseId));
    }

    // ── «Изучить»: экспорт пачки лемм стиха + код урока ─────────────
    @PostMapping("/verses/{verseId}/study")
    public ResponseEntity<StudyVerseResponse> studyVerse(@PathVariable UUID verseId) {
        return ResponseEntity.ok(new StudyVerseResponse(verseService.triggerStudyExport(verseId)));
    }

    public record StudyVerseResponse(String verseTopicCode) {}

    // ── Verse Analysis (read-only) ────────────────────────────────

    @GetMapping("/verses/{verseId}/analysis")
    public ResponseEntity<VerseAnalysis> getVerseAnalysis(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseAnalysis(verseId));
    }

    @GetMapping("/verses/{verseId}/words")
    public ResponseEntity<List<VerseWord>> getVerseWords(@PathVariable UUID verseId) {
        return ResponseEntity.ok(verseService.getVerseWords(verseId));
    }

    // ── Словоформы: примеры стихов по точной surfaceIast (урок склонений) ──
    @PostMapping("/words/examples")
    public ResponseEntity<VerseWordExamplesResponseDto> getWordExamples(
            @RequestBody VerseWordExamplesRequestDto request) {
        return ResponseEntity.ok(verseWordExamplesService.findExamples(request));
    }

    // ── Примеры склонений по словоизменительному классу (вкладка «Примеры») ──
    @PostMapping("/verses/examples")
    public ResponseEntity<DeclensionExamplesResponseDto> getExamplesByStemClass(
            @RequestBody DeclensionExamplesSearchRequestDto request) {
        return ResponseEntity.ok(verseWordSearchService.searchExamples(request));
    }
}

