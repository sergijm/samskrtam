package sm.selflearn.samskrtam.content.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.content.model.QuizType;
import sm.selflearn.samskrtam.content.service.QuizService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/content/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    @GetMapping
    public List<QuizSummaryDto> getQuizzes(@RequestParam(required = false) QuizType type) {
        return quizService.getQuizzes(type);
    }
}
