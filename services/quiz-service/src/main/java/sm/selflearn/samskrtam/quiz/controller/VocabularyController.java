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
import sm.selflearn.samskrtam.quiz.dto.StartSessionResponse;
import sm.selflearn.samskrtam.quiz.service.SessionService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quiz/vocabulary/{slug}/sessions")
@Tag(name = "Vocabulary Quiz", description = "APIs for vocabulary quiz sessions")
@RequiredArgsConstructor
public class VocabularyController {

    private final SessionService sessionService;

    @PostMapping("/start")
    @Operation(summary = "Start a new vocabulary quiz session")
    @ApiResponse(responseCode = "200", description = "Session started successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public Mono<StartSessionResponse> startSession(
            @PathVariable String slug, // Slug is part of the path for vocabulary quizzes
            @RequestParam UUID quizId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale) {
        return sessionService.startSession(quizId, userId, userLocale);
    }

    @GetMapping("/{sessionId}/resume")
    @Operation(summary = "Resume an existing vocabulary quiz session")
    @ApiResponse(responseCode = "200", description = "Session resumed successfully")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<ResumeSessionResponse> resumeSession(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale) {
        return sessionService.resumeSession(sessionId, userId, userLocale);
    }

    @PostMapping("/{sessionId}/answer")
    @Operation(summary = "Submit an answer for a question in a vocabulary quiz session")
    @ApiResponse(responseCode = "200", description = "Answer submitted successfully")
    @ApiResponse(responseCode = "404", description = "Session or question not found")
    @ApiResponse(responseCode = "409", description = "Question already answered")
    public Mono<AnswerResponse> submitAnswer(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale,
            @RequestBody AnswerRequest request) {
        return sessionService.submitAnswer(sessionId, userId, request, userLocale);
    }

    @PostMapping("/{sessionId}/complete")
    @Operation(summary = "Complete a vocabulary quiz session")
    @ApiResponse(responseCode = "200", description = "Session completed successfully")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<CompleteSessionResponse> completeSession(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return sessionService.completeSession(sessionId, userId);
    }
}
