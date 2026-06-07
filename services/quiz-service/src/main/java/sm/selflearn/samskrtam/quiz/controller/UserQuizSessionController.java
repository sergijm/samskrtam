package sm.selflearn.samskrtam.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryDto; // Import AnswerHistoryDto
import sm.selflearn.samskrtam.quiz.dto.QuizSessionSummaryDto;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.service.UserSessionService;

import java.util.Locale; // Import Locale
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/quiz-sessions")
@Tag(name = "User Quiz Sessions", description = "APIs for retrieving user quiz session history")
@RequiredArgsConstructor
public class UserQuizSessionController {

    private final UserSessionService userSessionService;

    @GetMapping
    @Operation(summary = "Get a paginated list of user's quiz sessions")
    @ApiResponse(responseCode = "200", description = "List of quiz sessions retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public Mono<ResponseEntity<Page<QuizSessionSummaryDto>>> getUserQuizSessions(
            @RequestParam UUID userId, // Получаем userId как параметр запроса
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "startedAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) QuizType quizType,
            @RequestParam(required = false) SessionStatus status
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return userSessionService.getUserQuizSessions(userId, quizType, status, pageable) // Передаем userId из параметра запроса
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{sessionId}/answers")
    @Operation(summary = "Get a paginated list of answers for a specific quiz session")
    @ApiResponse(responseCode = "200", description = "List of answers retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "Session not found")
    public Mono<ResponseEntity<Page<AnswerHistoryDto>>> getSessionAnswerHistory(
            @PathVariable UUID sessionId,
            @RequestParam UUID userId, // Получаем userId как параметр запроса
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "answeredAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestHeader(value = "X-User-Locale", defaultValue = "en") Locale locale
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        return userSessionService.getSessionAnswerHistory(sessionId, userId, pageable, locale)
                .map(ResponseEntity::ok);
    }
}
