package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.service.VerseInternalSandhiService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sangraha")
@RequiredArgsConstructor
public class VerseInternalSandhiController {

    private final VerseInternalSandhiService internalSandhiService;

    /**
     * ШАГ 2 для одного стиха (внутренние сандхи). Стих должен быть ANALYZED.
     */
    @PostMapping("/verses/{verseId}/internal-sandhi")
    public ResponseEntity<Void> analyzeVerse(
            @PathVariable UUID verseId) {
        internalSandhiService.analyze(verseId);
        return ResponseEntity.accepted().build();
    }

    /**
     * ШАГ 2 для всех ANALYZED-стихов главы (SAME_WORK, батч).
     */
    @PostMapping("/chapters/{chapterId}/verses/internal-sandhi")
    public ResponseEntity<?> analyzeChapter(@PathVariable UUID chapterId) {
        try {
            List<UUID> verseIds = internalSandhiService.analyzeChapter(chapterId);
            return ResponseEntity.accepted().body(
                    new InternalSandhiResponse(chapterId, verseIds));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErrorResponse(e.getMessage()));
        }
    }

    /**
     * ШАГ 2 для произвольного списка стихов (MIXED_WORKS, батч по id).
     */
    @PostMapping("/verse/internal-sandhi")
    public ResponseEntity<InternalSandhiResponse> analyzeVerses(
            @RequestBody InternalSandhiRequest request) {
        List<UUID> verseIds = request == null ? List.of() : request.verseIds();
        List<UUID> accepted = internalSandhiService.analyzeVerses(verseIds);
        return ResponseEntity.accepted().body(new InternalSandhiResponse(null, accepted));
    }

    public record InternalSandhiRequest(List<UUID> verseIds) {}

    public record InternalSandhiResponse(UUID chapterId, List<UUID> verseIds) {}

    public record ErrorResponse(String message) {}
}
