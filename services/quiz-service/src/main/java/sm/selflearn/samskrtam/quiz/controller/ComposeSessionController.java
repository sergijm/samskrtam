package sm.selflearn.samskrtam.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.ComposeQuizResponse;
import sm.selflearn.samskrtam.quiz.model.ProgressTagSetId;
import sm.selflearn.samskrtam.quiz.service.QuizComposeService;

import java.util.UUID;

/**
 * Universal (curriculum-driven) quiz sessions: selects one quest item per
 * (progress_tag, item_type, answer_mode) group using a window-function query,
 * then composes and persists the session.
 */
@RestController
@RequestMapping("/api/v2/quiz")
@RequiredArgsConstructor
@Tag(name = "Quest Compose Sessions", description = "Universal curriculum-driven quiz sessions")
public class ComposeSessionController {

    private final QuizComposeService quizComposeService;

    public record ComposeRequest(
            String topicCode,
            ProgressTagSetId progressTagSetId,
            String itemType,
            String answerMode,
            int limit
    ) {}

    @PostMapping("/compose")
    @Operation(summary = "Compose and start a curriculum-driven quiz session",
            description = "Selects one item per (tag,type,mode) via window function, persists the session, returns questions")
    @ApiResponse(responseCode = "200", description = "Session composed and started")
    @ApiResponse(responseCode = "400", description = "Empty selection or unknown topic")
    public Mono<ComposeQuizResponse> compose(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Locale", required = false) String userLocale,
            @RequestBody ComposeRequest request) {
        return quizComposeService.compose(
                userId,
                request.topicCode(),
                request.progressTagSetId(),
                request.itemType(),
                request.answerMode(),
                request.limit());
    }
}
