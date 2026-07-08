package sm.selflearn.samskrtam.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("GET /vocabulary/{} — X-User-Id={}", slug, userId);
        return lessonService.getVocabularyLesson(slug, userId)
                .doOnNext(lesson -> {
                    int nonZeroScores = (int) lesson.getWords().stream()
                            .filter(w -> w.getScore() > 0).count();
                    log.info("GET /vocabulary/{} — response: totalWords={}, nonZeroScores={}",
                            slug, lesson.getTotalWords(), nonZeroScores);
                })
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
        log.info("GET /grammar/{} — X-User-Id={}", slug, userId);
        return lessonService.getGrammarLesson(slug, userId)
                .doOnNext(lesson -> {
                    int nonZeroScores = (int) lesson.getQuestions().stream()
                            .filter(q -> q.getScore() > 0).count();
                    log.info("GET /grammar/{} — response: totalQuestions={}, nonZeroScores={}, sampleScores={}",
                            slug, lesson.getTotalQuestions(), nonZeroScores,
                            lesson.getQuestions().stream().limit(5)
                                    .map(q -> String.format("(gender=%s,case=%s,num=%s,score=%d)",
                                            q.getGender(), q.getCaseType(), q.getNumberType(), q.getScore()))
                                    .toList());
                })
                .map(ResponseEntity::ok);
    }
}
