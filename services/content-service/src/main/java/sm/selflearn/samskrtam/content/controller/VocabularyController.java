package sm.selflearn.samskrtam.content.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.service.QuizService; // Import QuizService
import sm.selflearn.samskrtam.content.service.VocabularyService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;
    private final QuizService quizService; // Inject QuizService

    // Existing endpoints (as per documentation)
    // POST   /api/v1/content/quizzes/{id}/vocabulary
    // PUT    /api/v1/content/vocabulary/{wordId}
    // DELETE /api/v1/content/vocabulary/{wordId}

    // Новый эндпоинт для получения одного словарного слова по ID
    @GetMapping("/vocabulary/words/{wordId}")
    public VocabularyWordDto getVocabularyWordById(@PathVariable UUID wordId) {
        return vocabularyService.getVocabularyWordById(wordId);
    }

    // New endpoint for quiz-service to get vocabulary words
    @GetMapping("/quizzes/{quizId}/vocabulary-words")
    public List<VocabularyWordDto> getVocabularyWordsForQuiz(
            @PathVariable UUID quizId,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        // Get the quiz slug from the quizId
        String quizSlug = quizService.getQuizSummaryById(quizId).getSlug();
        return vocabularyService.getVocabularyWordsForQuiz(quizSlug, limit); // Pass quizSlug
    }
}
