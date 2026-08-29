package sm.selflearn.samskrtam.quiz.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.BulkProgressRequest;
import sm.selflearn.samskrtam.quiz.dto.BulkProgressResponse;
import sm.selflearn.samskrtam.quiz.dto.ProgressSummaryDto;
import sm.selflearn.samskrtam.quiz.service.ProgressSummaryService;

import java.util.UUID;

/**
 * Реальный прогресс пользователя. Все вычисления идут по таблице
 * {@code quiz.quiz_item_score} (quiz-service — система записи прогресса).
 */
@RestController
@RequestMapping("/api/v2/quiz/progress")
@RequiredArgsConstructor
@Slf4j
public class ProgressSummaryController {

    private final ProgressSummaryService progressSummaryService;

    @GetMapping("/summary")
    public Mono<ResponseEntity<ProgressSummaryDto>> summary(
            @RequestParam(defaultValue = "learn-graph") String scope,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("v2 GET /quiz/progress/summary?scope={} — X-User-Id={}", scope, userId);
        return progressSummaryService.summarize(scope, userId).map(ResponseEntity::ok);
    }

    @PostMapping("/bulk")
    public Mono<ResponseEntity<BulkProgressResponse>> bulk(
            @RequestBody BulkProgressRequest request,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return progressSummaryService
                .bulkScores(userId, ProgressSummaryService.parseItemType(request.itemType()), request.progressTags())
                .map(ResponseEntity::ok);
    }
}
