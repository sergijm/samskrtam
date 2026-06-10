package sm.selflearn.samskrtam.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;
import sm.selflearn.samskrtam.quiz.dto.AnswerResponse;
import sm.selflearn.samskrtam.quiz.dto.CompleteSessionResponse;
import sm.selflearn.samskrtam.quiz.dto.ResumeSessionResponse;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse; // Import new DTO
import sm.selflearn.samskrtam.quiz.dto.StartSessionResponse;
import sm.selflearn.samskrtam.quiz.service.QuizSessionService; // Changed from GrammarSessionService

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quiz/{slug}/sessions")
@Tag(name = "Quiz Sessions", description = "APIs for all quiz sessions (vocabulary, declensions, conjugations)")
@RequiredArgsConstructor
public class QuizSessionController {

    private final QuizSessionService quizSessionService; // Changed from GrammarSessionService

    @PostMapping("/start")
    @Operation(summary = "Start a new quiz session (or resume if in progress)")
    @ApiResponse(responseCode = "200", description = "Session started or resumed successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public Mono<StartOrResumeResponse> startSession( // Changed return type to StartOrResumeResponse
            @PathVariable String slug,
            @RequestParam UUID quizId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale) {
        // This endpoint now behaves like start-or-resume to simplify client logic
        return quizSessionService.startOrResumeSession(quizId, userId, userLocale); // Changed service call
    }

    @PostMapping("/start-or-resume")
    @Operation(summary = "Start a new quiz session or resume the latest in-progress session for a given quiz")
    @ApiResponse(responseCode = "200", description = "Session started or resumed successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public Mono<StartOrResumeResponse> startOrResumeSession(
            @PathVariable String slug,
            @RequestParam UUID quizId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale) {
        return quizSessionService.startOrResumeSession(quizId, userId, userLocale); // Changed service call
    }

    @GetMapping("/{sessionId}/resume")
    @Operation(summary = "Resume an existing quiz session")
    @ApiResponse(responseCode = "200", description = "Session resumed successfully")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<ResumeSessionResponse> resumeSession(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale) {
        // This method is now primarily for direct resume by sessionId,
        // while start-or-resume handles the logic of finding the latest in-progress session.
        // It's kept for backward compatibility or specific use cases.
        return quizSessionService.resumeSession(sessionId, userId, userLocale); // Changed service call
    }

    @PostMapping("/{sessionId}/answer")
    @Operation(summary = "Submit an answer for a question in a quiz session")
    @ApiResponse(responseCode = "200", description = "Answer submitted successfully")
    @ApiResponse(responseCode = "404", description = "Session or question not found")
    @ApiResponse(responseCode = "409", description = "Question already answered")
    public Mono<AnswerResponse> submitAnswer(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale,
            @RequestBody AnswerRequest request) {
        return quizSessionService.submitAnswer(sessionId, userId, request, userLocale); // Changed service call
    }

    @PostMapping("/{sessionId}/complete")
    @Operation(summary = "Complete a quiz session")
    @ApiResponse(responseCode = "200", description = "Session completed successfully")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<CompleteSessionResponse> completeSession(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return quizSessionService.completeSession(sessionId, userId); // Changed service call
    }
}
