package sm.selflearn.samskrtam.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import sm.selflearn.samskrtam.content.dto.QuizListItemResponse;
import sm.selflearn.samskrtam.content.service.QuizContentService;

@RestController
@RequestMapping("/api/v1/content/quizzes")
@Tag(name = "Quiz Content", description = "APIs for managing quiz content")
@RequiredArgsConstructor
public class QuizContentController {

    private final QuizContentService quizContentService;

    @GetMapping
    @Operation(summary = "Get a list of available quizzes")
    @ApiResponse(responseCode = "200", description = "List of quizzes retrieved successfully")
    public Flux<QuizListItemResponse> getQuizList() {
        return quizContentService.getQuizList();
    }
}
