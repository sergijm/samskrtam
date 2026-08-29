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
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.model.ProgressTagSetId;
import sm.selflearn.samskrtam.quiz.service.QuizSessionService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quiz/{slug}/sessions")
@Tag(name = "Quiz Sessions", description = "APIs for all quiz sessions (vocabulary, declensions, conjugations)")
@RequiredArgsConstructor
public class QuizSessionController {

    private final QuizSessionService quizSessionService;

    @PostMapping("/start")
    @Operation(summary = "Start a new quiz session (or resume if in progress)")
    @ApiResponse(responseCode = "200", description = "Session started or resumed successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public Mono<StartOrResumeResponse> startSession(
            @PathVariable String slug,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) String topicCode,
            @RequestParam(defaultValue = "10") int count,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale,
            @RequestParam(required = false) ProgressTagSetId progressTagSetId,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String answerMode) {
        if (topicCode != null && !topicCode.isBlank()) {
            return quizSessionService.startOrResumeSessionByTopic(
                    topicCode, count, userId, userLocale, progressTagSetId, itemType, answerMode);
        }
        return quizSessionService.startOrResumeSession(lessonId, userId, userLocale);
    }

    @PostMapping("/start-or-resume")
    @Operation(summary = "Start a new quiz session or resume the latest in-progress session for a given quiz")
    @ApiResponse(responseCode = "200", description = "Session started or resumed successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public Mono<StartOrResumeResponse> startOrResumeSession(
            @PathVariable String slug,
            @RequestParam(required = false) UUID lessonId,
            @RequestParam(required = false) String topicCode,
            @RequestParam(defaultValue = "10") int count,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale,
            @RequestParam(required = false) ProgressTagSetId progressTagSetId,
            @RequestParam(required = false) String itemType,
            @RequestParam(required = false) String answerMode) {
        if (topicCode != null && !topicCode.isBlank()) {
            return quizSessionService.startOrResumeSessionByTopic(
                    topicCode, count, userId, userLocale, progressTagSetId, itemType, answerMode);
        }
        return quizSessionService.startOrResumeSession(lessonId, userId, userLocale);
    }

    @GetMapping("/{sessionId}/resume")
    @Operation(summary = "Resume an existing quiz session")
    @ApiResponse(responseCode = "200", description = "Session resumed successfully")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<StartOrResumeResponse> resumeSession(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale) {
        return quizSessionService.resumeSession(sessionId, userId, userLocale);
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
        return quizSessionService.submitAnswer(sessionId, userId, request, userLocale);
    }

    @PostMapping("/{sessionId}/complete")
    @Operation(summary = "Complete a quiz session")
    @ApiResponse(responseCode = "200", description = "Session completed successfully")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<CompleteSessionResponse> completeSession(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId) {
        return quizSessionService.completeSession(sessionId, userId);
    }

    @PostMapping("/{sessionId}/retake")
    @Operation(summary = "Retake an existing quiz session, clearing answers and resetting progress")
    @ApiResponse(responseCode = "200", description = "Session reset for retake")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<StartOrResumeResponse> retakeSession(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale) {
        return quizSessionService.retakeSession(sessionId, userId, userLocale);
    }

    @PostMapping("/{sessionId}/new-quiz")
    @Operation(summary = "Complete current session and start a new quiz session of the same type")
    @ApiResponse(responseCode = "200", description = "New quiz session started")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<StartOrResumeResponse> startNewQuizFromExistingSession(
            @PathVariable String slug,
            @PathVariable UUID sessionId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("X-User-Locale") String userLocale) {
        return quizSessionService.startNewQuizFromExistingSession(sessionId, userId, userLocale);
    }
}