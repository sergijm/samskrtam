package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.dto.AnalyzeVersesRequest;
import sm.selflearn.samskrtam.sangraha.dto.AnalyzeVersesResponse;
import sm.selflearn.samskrtam.sangraha.service.VerseAnalysisService;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sangraha")
@RequiredArgsConstructor
public class VerseAnalyzeController {

    private final VerseAnalysisService verseAnalysisService;

    @PostMapping("/verses/{verseId}/analyze")
    public ResponseEntity<Void> analyzeVerse(
            @PathVariable UUID verseId,
            @RequestBody(required = false) VerseTextRequest request) {
        String rawText = (request != null) ? request.text() : null;
        verseAnalysisService.analyze(verseId, rawText);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/chapters/{chapterId}/verses/analyze-all")
    public ResponseEntity<?> analyzeAllVerses(@PathVariable UUID chapterId) {
        try {
            List<UUID> verseIds = verseAnalysisService.analyzeChapter(chapterId);
            return ResponseEntity.accepted().body(
                    new AnalyzeAllVersesResponse(chapterId, verseIds));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    @PostMapping("/verse/analysis")
    public ResponseEntity<AnalyzeVersesResponse> analyzeVerses(
            @RequestBody AnalyzeVersesRequest request) {
        List<UUID> verseIds = request == null ? List.of() : request.verseIds();
        List<UUID> accepted = verseAnalysisService.analyzeVerses(verseIds);
        return ResponseEntity.accepted().body(new AnalyzeVersesResponse(accepted));
    }

    public record AnalyzeAllVersesResponse(UUID chapterId, List<UUID> verseIds) {}

    public record ErrorResponse(String message) {}
}

