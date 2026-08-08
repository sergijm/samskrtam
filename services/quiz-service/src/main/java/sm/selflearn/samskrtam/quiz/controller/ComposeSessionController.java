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
import sm.selflearn.samskrtam.quiz.dto.QuestComposeRequest;
import sm.selflearn.samskrtam.quiz.service.QuestComposeService;

import java.util.UUID;

/**
 * Universal (curriculum-driven) quiz sessions: the caller specifies topics and a question
 * count per topic (mixed grammar + lexical topics allowed); curriculum-service renders the
 * materialized questions with options, quiz-service persists the session and serves it.
 *
 * <p>Contract-first: see docs/services/curriculum-session-composition.md.
 */
@RestController
@RequestMapping("/api/v2/quiz")
@RequiredArgsConstructor
@Tag(name = "Quest Compose Sessions", description = "Universal curriculum-driven quiz sessions (topics instead of lessons)")
public class ComposeSessionController {

    private final QuestComposeService questComposeService;

    @PostMapping("/compose")
    @Operation(summary = "Compose and start a curriculum-driven quiz session from topics",
            description = "Requests topics + counts, gets materialized questions from curriculum-service, "
                    + "persists the session with options fixed at start, returns the first question set")
    @ApiResponse(responseCode = "200", description = "Session composed and started")
    @ApiResponse(responseCode = "400", description = "Empty topics or a topic with no materialized questions")
    @ApiResponse(responseCode = "404", description = "Unknown topic code")
    public Mono<ComposeQuizResponse> compose(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Locale", required = false) String userLocale,
            @RequestBody QuestComposeRequest request) {
        return questComposeService.compose(userId, request);
    }
}
