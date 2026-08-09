package sm.selflearn.samskrtam.sangraha.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.sangraha.dto.ClassificationRunResponse;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationItemDto;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationPageDto;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationReviewRequest;
import sm.selflearn.samskrtam.sangraha.dto.LemmaRefreshResponse;
import sm.selflearn.samskrtam.sangraha.dto.StartClassificationRunRequest;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationReviewService;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationRunService;
import sm.selflearn.samskrtam.sangraha.service.LemmaRefreshService;

import java.util.UUID;

/**
 * Internal endpoints модуля lexicon-classification (lemma-classification.md,
 * task-sangraha-17..19). Все требуют {@code X-User-Id} администратора.
 */
@Slf4j
@RestController
@RequestMapping("/sangraha/internal/lexicon")
@RequiredArgsConstructor
public class LexiconController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final LemmaRefreshService lemmaRefreshService;
    private final LemmaClassificationRunService runService;
    private final LemmaClassificationReviewService reviewService;

    @PostMapping("/lemmas/refresh-statistics")
    public ResponseEntity<LemmaRefreshResponse> refreshStatistics(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userId) {
        LemmaRefreshResponse response = lemmaRefreshService.refresh();
        log.debug("Lemma refresh-statistics requested by {}", userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/classification/runs")
    public ResponseEntity<ClassificationRunResponse> startRun(
            @RequestBody StartClassificationRunRequest request,
            @RequestHeader(value = USER_ID_HEADER, required = false) String userId) {
        ClassificationRunResponse response = runService.startRun(
                request.schemeCode(), request.batchSize(), request.batchCount(), userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/classification/runs/{runId}")
    public ResponseEntity<ClassificationRunResponse> getRun(@PathVariable UUID runId) {
        return ResponseEntity.ok(runService.getRun(runId));
    }

    @GetMapping("/classifications")
    public ResponseEntity<LemmaClassificationPageDto> listForReview(
            @RequestParam(defaultValue = "CURRICULUM") String schemeCode,
            @RequestParam(defaultValue = "CANDIDATE") String status,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "100") int limit) {
        ClassificationStatus st = ClassificationStatus.valueOf(status.toUpperCase());
        return ResponseEntity.ok(reviewService.listForReview(schemeCode, st, cursor, limit));
    }

    @PatchMapping("/classifications/{id}")
    public ResponseEntity<LemmaClassificationItemDto> review(
            @PathVariable UUID id,
            @RequestBody LemmaClassificationReviewRequest request,
            @RequestHeader(value = USER_ID_HEADER, required = false) String userId) {
        return ResponseEntity.ok(reviewService.review(id, null, request, userId));
    }

    @GetMapping("/lemma-classifications/export")
    public ResponseEntity<LemmaClassificationPageDto> exportApproved(
            @RequestParam(defaultValue = "CURRICULUM") String schemeCode,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(defaultValue = "500") int limit) {
        return ResponseEntity.ok(reviewService.exportApproved(schemeCode, cursor, limit));
    }
}