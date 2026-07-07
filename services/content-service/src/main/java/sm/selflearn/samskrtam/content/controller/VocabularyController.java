package sm.selflearn.samskrtam.content.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.service.LessonContentService;
import sm.selflearn.samskrtam.content.service.VocabularyService;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;
    private final LessonContentService lessonContentService;

    // Новый эндпоинт для получения одного словарного слова по ID
    @GetMapping("/vocabulary/words/{wordId}")
    public VocabularyWordDto getVocabularyWordById(@PathVariable UUID wordId) {
        return vocabularyService.getVocabularyWordById(wordId);
    }

    // New endpoint for quiz-service to get vocabulary words
    @GetMapping("/lessons/{quizId}/vocabulary-words")
    public List<VocabularyWordDto> getVocabularyWordsForQuiz(
            @PathVariable UUID quizId,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        // Get the quiz slug from the quizId
        String quizSlug = lessonContentService.getLessonItemById(quizId).getSlug();
        return vocabularyService.getVocabularyWordsForQuiz(quizSlug, limit); // Pass quizSlug
    }

    /**
     * Возвращает плоский список word_id для урока/категории по slug.
     * Используется QuizGenerator'ом для получения externalRefId без полных DTO.
     */
    @GetMapping("/lessons/{slug}/vocabulary-word-ids")
    public Set<UUID> getVocabularyWordIdsForLesson(@PathVariable String slug) {
        return vocabularyService.getVocabularyWordIdsForQuiz(slug);
    }
}

