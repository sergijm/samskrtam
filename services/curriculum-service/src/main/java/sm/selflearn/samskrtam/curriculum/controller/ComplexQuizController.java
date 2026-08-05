package sm.selflearn.samskrtam.curriculum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.dto.ComplexQuizDto;
import sm.selflearn.samskrtam.curriculum.dto.ComplexQuizSummaryDto;
import sm.selflearn.samskrtam.curriculum.dto.UpsertComplexQuizRequest;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizType;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.service.ComplexQuizService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/curriculum/complex-quizzes")
@RequiredArgsConstructor
public class ComplexQuizController {

    private final ComplexQuizService complexQuizService;

    @GetMapping
    public List<ComplexQuizSummaryDto> listComplexQuizzes(
            @RequestParam(required = false) LearningLevel level,
            @RequestParam(required = false) ComplexQuizType type) {
        return complexQuizService.listComplexQuizzes(level, type);
    }

    @GetMapping("/{id}")
    public ComplexQuizDto getComplexQuiz(@PathVariable UUID id) {
        return complexQuizService.getComplexQuiz(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplexQuizDto createComplexQuiz(@Valid @RequestBody UpsertComplexQuizRequest request) {
        return complexQuizService.createComplexQuiz(request);
    }

    @PutMapping("/{id}")
    public ComplexQuizDto updateComplexQuiz(
            @PathVariable UUID id,
            @Valid @RequestBody UpsertComplexQuizRequest request) {
        return complexQuizService.updateComplexQuiz(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComplexQuiz(@PathVariable UUID id) {
        complexQuizService.deleteComplexQuiz(id);
    }
}
