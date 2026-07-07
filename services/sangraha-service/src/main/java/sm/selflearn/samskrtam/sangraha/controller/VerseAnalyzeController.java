package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.service.VerseAnalysisService;
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
}
