package sm.selflearn.samskrtam.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*; // Import RequestParam
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.dto.QuizListItemResponse;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto; // Import QuizSummaryDto
import sm.selflearn.samskrtam.content.dto.SessionDataResponse;
import sm.selflearn.samskrtam.content.model.DeclensionForm;
import sm.selflearn.samskrtam.content.repository.DeclensionFormRepository;
import sm.selflearn.samskrtam.content.service.QuizContentService;
import sm.selflearn.samskrtam.content.service.QuizService; // Import QuizService

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/content")
@Tag(name = "Quiz Content", description = "APIs for managing quiz content")
@RequiredArgsConstructor
public class QuizContentController {

    private final QuizContentService quizContentService;
    private final DeclensionFormRepository declensionFormRepository;
    private final QuizService quizService; // Inject QuizService

    @GetMapping("/quizzes")
    @Operation(summary = "Get a list of available quizzes")
    @ApiResponse(responseCode = "200", description = "List of quizzes retrieved successfully")
    public Flux<QuizListItemResponse> getQuizList(@RequestParam(required = false) String category) {
        return quizContentService.getQuizList(category);
    }

    @GetMapping("/quizzes/{quizId}/session-data")
    @Operation(summary = "Get session data for a specific quiz")
    @ApiResponse(responseCode = "200", description = "Session data retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public Mono<SessionDataResponse> getSessionData(
            @PathVariable UUID quizId,
            @RequestHeader(value = "X-User-Locale", defaultValue = "en") Locale locale) { // Added defaultValue
        return quizContentService.getSessionData(quizId, locale);
    }

    @GetMapping("/quizzes/by-slug/{slug}")
    @Operation(summary = "Get quiz summary by slug")
    @ApiResponse(responseCode = "200", description = "Quiz summary retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public Mono<QuizSummaryDto> getQuizBySlug(@PathVariable String slug) {
        return Mono.just(quizService.getQuizBySlug(slug));
    }

    @GetMapping("/declension-stems/{stemId}/forms")
    @Operation(summary = "Get all declension forms for a specific stem")
    @ApiResponse(responseCode = "200", description = "List of declension forms retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Declension stem not found")
    public Flux<DeclensionFormDto> getDeclensionForms(@PathVariable UUID stemId) {
        return Flux.fromIterable(declensionFormRepository.findByDeclensionStemId(stemId))
                .switchIfEmpty(Mono.error(new SamskrtamException("DECLENSION_STEM_NOT_FOUND", "Declension stem not found with ID: " + stemId)))
                .map(this::mapToDeclensionFormDto);
    }

    private DeclensionFormDto mapToDeclensionFormDto(DeclensionForm form) {
        return DeclensionFormDto.builder()
                .declensionStemId(form.getDeclensionStemId())
                .caseType(form.getCaseType())
                .numberType(form.getNumberType())
                .formIast(form.getFormIast())
                .formDevanagari(form.getFormDevanagari())
                .build();
    }
}
