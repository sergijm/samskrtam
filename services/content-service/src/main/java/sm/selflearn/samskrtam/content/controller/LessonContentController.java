package sm.selflearn.samskrtam.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.dto.DeclensionStemDto;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.model.*;
import sm.selflearn.samskrtam.content.model.DeclensionForm;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.content.repository.DeclensionFormRepository;
import sm.selflearn.samskrtam.content.service.GenerateQuizService;
import sm.selflearn.samskrtam.content.service.GrammarContentService;
import sm.selflearn.samskrtam.content.service.LessonContentService;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/content")
@Tag(name = "Quiz Content", description = "APIs for managing quiz content")
@RequiredArgsConstructor
public class LessonContentController {

    private final LessonContentService lessonContentService;
    private final GenerateQuizService generateQuizService;
    private final GrammarContentService grammarContentService;
    private final DeclensionFormRepository declensionFormRepository;

    @GetMapping("/lessons")
    @Operation(summary = "Get a list of available quizzes")
    @ApiResponse(responseCode = "200", description = "List of quizzes retrieved successfully")
    public List<LessonItemResponse> getLessonsList(@RequestParam(required = false) String category) {
        return lessonContentService.getLessonsList(category);
    }

    @GetMapping("/lessons/{slug}")
    @Operation(summary = "Get quiz summary by slug")
    @ApiResponse(responseCode = "200", description = "Quiz summary retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public LessonItemResponse getLessonBySlug(@PathVariable String slug) {
        return lessonContentService.getLessonItemBySlug(slug);
    }

    @GetMapping(value = "/lessons", params = "id")
    @Operation(summary = "Get quiz summary by ID")
    @ApiResponse(responseCode = "200", description = "Quiz summary retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
    public LessonItemResponse getLessonSummaryById(@RequestParam UUID id) {
        return lessonContentService.getLessonItemById(id);
    }

    @GetMapping("/lessons/{slug}/declension-stems")
    @Operation(summary = "Get all declension stems for a lesson by slug")
    @ApiResponse(responseCode = "200", description = "List of declension stems retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Lesson not found")
    public List<DeclensionStemDto> getDeclensionStemsForLesson(@PathVariable String slug) {
        return grammarContentService.getDeclensionStemsForLesson(slug);
    }

    @GetMapping("/lessons/{slug}/case-endings")
    @Operation(summary = "Get case endings for a lesson with optional filters")
    @ApiResponse(responseCode = "200", description = "List of case endings retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Lesson not found")
    public List<CaseEndingDto> getCaseEndingsForLesson(
            @PathVariable String slug,
            @RequestParam(required = false) CaseType caseType,
            @RequestParam(required = false) NumberType numberType,
            @RequestParam(required = false) Gender gender) {
        return grammarContentService.getCaseEndingsForLesson(slug, caseType, numberType, gender);
    }

    @GetMapping("/case-endings")
    @Operation(summary = "Get all case endings for a vowel type")
    @ApiResponse(responseCode = "200", description = "List of case endings retrieved successfully")
    public List<CaseEndingDto> getCaseEndingsByVowelType(@RequestParam VowelType vowelType) {
        return grammarContentService.getCaseEndingsByVowelType(vowelType);
    }

    @PostMapping("/lessons/{quizId}/generate-quiz-data")
    @Operation(summary = "Generate quiz data for a specific quiz")
    @ApiResponse(responseCode = "200", description = "Quiz data generated successfully")
    @ApiResponse(responseCode = "404", description = "Quiz not found")
        public GeneratedQuizData generateQuizData(
            @PathVariable UUID quizId,
            @RequestHeader(value = "X-User-Locale", defaultValue = "en") Locale locale,
            @RequestParam(required = false) String filterScope,
            @RequestParam(required = false) String filterCaseTypes,
            @RequestParam(required = false) String filterNumberTypes,
            @RequestParam(required = false) String filterCombinations) {
        return generateQuizService.generateQuizData(quizId, locale,
                filterScope, filterCaseTypes, filterNumberTypes, filterCombinations);
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

        @GetMapping("/public/lessons/{slug}/declension-paradigms")
    @Operation(summary = "Get one declension paradigm by index (public carousel)")
    @ApiResponse(responseCode = "200", description = "Paradigm page retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Lesson not found, not DECLENSIONS, or index out of range")
    public DeclensionParadigmPageDto getDeclensionParadigm(
            @PathVariable String slug,
            @RequestParam(defaultValue = "0") int index) {
        return grammarContentService.getDeclensionParadigmForLesson(slug, index);
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