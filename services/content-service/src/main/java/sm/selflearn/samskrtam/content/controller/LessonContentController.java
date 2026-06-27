package sm.selflearn.samskrtam.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.content.model.DeclensionForm;
import sm.selflearn.samskrtam.content.repository.DeclensionFormRepository;
import sm.selflearn.samskrtam.content.service.QuestionGenerationService;
import sm.selflearn.samskrtam.content.service.QuizContentService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/content")
@Tag(name = "Quiz Content", description = "APIs for managing quiz content")
@RequiredArgsConstructor
public class LessonContentController {

    private final QuizContentService quizContentService;
    private final DeclensionFormRepository declensionFormRepository;
    private final QuestionGenerationService questionGenerationService;

    @GetMapping("/quizzes")
    @Operation(summary = "Get a list of available quizzes")
    @ApiResponse(responseCode = "200", description = "List of quizzes retrieved successfully")
    public List<LessonItemResponse> getQuizList(@RequestParam(required = false) String category) {
        return quizContentService.getLessonsList(category);
    }

    @PostMapping("/lessons/{quizId}/generate-quiz-data")
    @Operation(summary = "Generate quiz data for a specific quiz")
    @ApiResponse(responseCode = "200", description = "Quiz data generated successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public GeneratedQuizData generateQuizData(
            @PathVariable UUID quizId,
            @RequestHeader(value = "X-User-Locale", defaultValue = "en") Locale locale) {
        return quizContentService.generateQuizData(quizId, locale);
    }

    @GetMapping("/generated-quiz-data/{id}")
    @Operation(summary = "Get generated quiz data by ID")
    @ApiResponse(responseCode = "200", description = "Generated quiz data retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Generated quiz data not found")
    public GeneratedQuizData getGeneratedQuizData(@PathVariable UUID id) {
        return quizContentService.getGeneratedQuizData(id);
    }

    @GetMapping("/lessons/by-slug/{slug}")
    @Operation(summary = "Get quiz summary by slug")
    @ApiResponse(responseCode = "200", description = "Quiz summary retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public LessonItemResponse getLessonBySlug(@PathVariable String slug) {
        return quizContentService.getLessonItemBySlug(slug);
    }

    @GetMapping("/lessons/{id}/summary")
    @Operation(summary = "Get quiz summary by ID")
    @ApiResponse(responseCode = "200", description = "Quiz summary retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public LessonItemResponse getLessonSummaryById(@PathVariable UUID id) {
        return quizContentService.getLessonItemById(id);
    }

    @GetMapping("/declension-stems/{stemId}/forms")
    @Operation(summary = "Get all declension forms for a specific stem")
    @ApiResponse(responseCode = "200", description = "List of declension forms retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Declension stem not found")
    public List<DeclensionFormDto> getDeclensionForms(@PathVariable UUID stemId) {
        List<DeclensionForm> forms = declensionFormRepository.findByDeclensionStemId(stemId);
        if (forms.isEmpty()) {
            throw new SamskrtamException("DECLENSION_STEM_NOT_FOUND", "Declension stem not found with ID: " + stemId);
        }
        return forms.stream()
                .map(this::mapToDeclensionFormDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/generated-questions/{questionId}")
    @Operation(summary = "Get a specific generated question by ID")
    @ApiResponse(responseCode = "200", description = "Generated question retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Generated question not found")
    public GeneratedQuizQuestionDto getGeneratedQuestion(@PathVariable UUID questionId) {
        return questionGenerationService.getGeneratedQuestionById(questionId);
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

