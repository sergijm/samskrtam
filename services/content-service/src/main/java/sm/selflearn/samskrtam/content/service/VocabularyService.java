package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.model.VocabularyCategory;
import sm.selflearn.samskrtam.content.model.VocabularyWord;
import sm.selflearn.samskrtam.content.model.VocabularyWordCategory;
import sm.selflearn.samskrtam.content.repository.*;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyWordRepository vocabularyWordRepository;
    private final VocabularyCategoryRepository vocabularyCategoryRepository;
    private final VocabularyWordCategoryRepository vocabularyWordCategoryRepository;
    private final QuizRepository quizRepository;

    public List<VocabularyWordDto> getVocabularyWordsForQuiz(String quizSlug, int limit) {
        // 1. Find the category by code (which matches quizSlug)
        VocabularyCategory rootCategory = vocabularyCategoryRepository.findByCodeIgnoreCase(quizSlug)
                .orElseThrow(() -> new SamskrtamException("VOCABULARY_CATEGORY_NOT_FOUND", "Vocabulary category not found for slug: " + quizSlug));

        // 2. Get all category IDs including the root and its children
        List<UUID> allCategoryIds = vocabularyCategoryRepository.findAllChildrenIds(rootCategory.getId());

        // 3. Find all word-category links for these categories
        Set<UUID> vocabularyWordIds = new HashSet<>();
        for (UUID categoryId : allCategoryIds) {
            vocabularyWordCategoryRepository.findByCategoryId(categoryId).stream()
                    .map(vwc -> vwc.getId().getVocabularyWordId())
                    .forEach(vocabularyWordIds::add);
        }

        // 4. Fetch the actual vocabulary words by their IDs, limited
        List<VocabularyWord> words = vocabularyWordRepository.findAllById(vocabularyWordIds).stream()
                .limit(limit)
                .collect(Collectors.toList());

        return words.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<VocabularyWordDto> getVocabularyWordsForQuizById(UUID quizId, int limit) {
        String slug = quizRepository.findById(quizId)
                .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND",
                        "Quiz not found with ID: " + quizId))
                .getSlug();
        return getVocabularyWordsForQuiz(slug, limit);
    }

    public VocabularyWordDto getVocabularyWordById(UUID wordId) {
        VocabularyWord word = vocabularyWordRepository.findById(wordId)
                .orElseThrow(() -> new SamskrtamException("VOCABULARY_WORD_NOT_FOUND", "Vocabulary word not found with ID: " + wordId));
        return toDto(word);
    }

    private VocabularyWordDto toDto(VocabularyWord word) {
        return VocabularyWordDto.builder()
                .id(word.getId())
                .wordIast(word.getWordIast())
                .wordDevanagari(word.getWordDevanagari())
                .translationEn(word.getTranslationEn())
                .translationRu(word.getTranslationRu())
                .gender(word.getGender())
                .stem(word.getStem())
                .root(word.getRoot())
                .explanationRu(word.getExplanationRu())
                .explanationEn(word.getExplanationEn())
                .build();
    }
}
