package sm.selflearn.samskrtam.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.dto.LessonListResponse;
import sm.selflearn.samskrtam.quiz.dto.VocabularyLessonDto;
import sm.selflearn.samskrtam.quiz.dto.GrammarLesson;
import sm.selflearn.samskrtam.quiz.service.LessonService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lessons")
@Tag(name = "Lessons", description = "APIs for lesson-related endpoints")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/vocabulary/{slug}")
    @Operation(summary = "Get vocabulary lesson with user progress")
    @ApiResponse(responseCode = "200", description = "Lesson with user progress retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Lesson not found")
    public Mono<ResponseEntity<VocabularyLessonDto>> getVocabularyLesson(
            @PathVariable String slug,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId )
    {
        return lessonService.getVocabularyLesson(slug, userId)
                .map(ResponseEntity::ok);
    }

        @GetMapping("/{lessonType}")
    @Operation(summary = "Get lessons by type (e.g. VOCABULARY, DECLENSIONS)")
    @ApiResponse(responseCode = "200", description = "List of lessons retrieved successfully")
    public Mono<ResponseEntity<LessonListResponse>> getLessonsByType(
            @PathVariable String lessonType,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        return lessonService.getLessonsByType(lessonType, userId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/grammar/{slug}")
    @Operation(summary = "Get grammar lesson with user progress")
    @ApiResponse(responseCode = "200", description = "Lesson with user progress retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Lesson not found")
    public Mono<ResponseEntity<GrammarLesson>> getGrammarLesson(
            @PathVariable("slug") String slug,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId )
    {
        return lessonService.getGrammarLesson(slug, userId)
                .map(ResponseEntity::ok);
    }
}