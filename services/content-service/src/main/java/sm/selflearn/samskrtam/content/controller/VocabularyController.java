package sm.selflearn.samskrtam.content.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.service.VocabularyService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    // Existing endpoints (as per documentation)
    // POST   /api/v1/content/quizzes/{id}/vocabulary
    // PUT    /api/v1/content/vocabulary/{wordId}
    // DELETE /api/v1/content/vocabulary/{wordId}

    // New endpoint for quiz-service to get vocabulary words
    @GetMapping("/quizzes/{quizId}/vocabulary-words")
    public List<VocabularyWordDto> getVocabularyWordsForQuiz(
            @PathVariable UUID quizId,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        return vocabularyService.getVocabularyWordsForQuiz(quizId, limit);
    }
}
