package sm.selflearn.samskrtam.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.service.QuizSessionService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v2/quiz/sessions")
@RequiredArgsConstructor
@Tag(name = "Quiz Sessions V2", description = "Session lifecycle: resume, answer, complete, retake")
public class QuizSessionV2Controller {

    private final QuizSessionService quizSessionService;

    @GetMapping("/{sessionId}/resume")
    @Operation(summary = "Resume a composed quiz session")
    @ApiResponse(responseCode = "200", description = "Session resumed")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<StartOrResumeResponse> resumeSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Locale", required = false) String userLocale) {
        return quizSessionService.resumeSession(sessionId, userId,
                userLocale != null ? userLocale : "en");
    }

    @PostMapping("/{sessionId}/answer")
    @Operation(summary = "Submit an answer")
    @ApiResponse(responseCode = "200", description = "Answer submitted")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<AnswerResponse> submitAnswer(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Locale", required = false) String userLocale,
            @RequestBody AnswerRequest request) {
        return quizSessionService.submitAnswer(sessionId, userId, request,
                userLocale != null ? userLocale : "en");
    }

    @PostMapping("/{sessionId}/complete")
    @Operation(summary = "Complete a session")
    @ApiResponse(responseCode = "200", description = "Session completed")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<CompleteSessionResponse> completeSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return quizSessionService.completeSession(sessionId, userId);
    }

    @PostMapping("/{sessionId}/retake")
    @Operation(summary = "Retake a session")
    @ApiResponse(responseCode = "200", description = "Session reset for retake")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<StartOrResumeResponse> retakeSession(
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader(value = "X-User-Locale", required = false) String userLocale) {
        return quizSessionService.retakeSession(sessionId, userId,
                userLocale != null ? userLocale : "en");
    }
}