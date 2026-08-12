package sm.selflearn.samskrtam.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.WordAnswerHistory;
import sm.selflearn.samskrtam.quiz.dto.QuestionAnswerHistory;
import sm.selflearn.samskrtam.quiz.service.LessonService;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lessons")
@Tag(name = "Lesson History", description = "APIs for lesson history endpoints")
@RequiredArgsConstructor
public class LessonHistoryController {

    private final LessonService lessonService;

    @GetMapping("/vocabulary/{slug}/words/{wordId}/history")
    @Operation(summary = "Get answer history for a specific word in a vocabulary lesson")
    @ApiResponse(responseCode = "200", description = "Word answer history retrieved successfully")
    @ApiResponse(responseCode = "404", description = "History not found")
    public Mono<ResponseEntity<WordAnswerHistory>> getWordAnswerHistory(
            @PathVariable String slug,
            @PathVariable UUID wordId,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "locale", defaultValue = "en") Locale locale) {
        
        Sort sort = Sort.by(Sort.Direction.DESC, "answeredAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return lessonService.getWordAnswerHistory(slug, wordId, userId, pageable, locale)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/grammar/{slug}/questions/history")
    @Operation(summary = "Get answer history for a specific grammar question by case, number and gender")
    @ApiResponse(responseCode = "200", description = "Question answer history retrieved successfully")
    @ApiResponse(responseCode = "404", description = "History not found")
    public Mono<ResponseEntity<QuestionAnswerHistory>> getQuestionAnswerHistory(
            @PathVariable String slug,
            @RequestParam String caseType,
            @RequestParam String numberType,
            @RequestParam String gender,
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "locale", defaultValue = "en") Locale locale) {
        
        Sort sort = Sort.by(Sort.Direction.DESC, "answeredAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        
        return lessonService.getQuestionAnswerHistory(slug, caseType, numberType, gender, userId, pageable, locale)
                .map(ResponseEntity::ok);
    }
}