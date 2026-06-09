package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto; // Updated import
import sm.selflearn.samskrtam.content.model.VocabularyWord;
import sm.selflearn.samskrtam.content.repository.VocabularyWordRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VocabularyService {

    private final VocabularyWordRepository vocabularyWordRepository;

    public List<VocabularyWordDto> getVocabularyWordsForQuiz(UUID quizId, int limit) {
        // For now, we'll just return a limited number of all words.
        // In a real scenario, you'd filter by quizId if words were linked to quizzes.
        // Or, if quizId implies a specific set of words, you'd fetch those.
        // Assuming for now that quizId is not directly linked to VocabularyWord in DB.
        // We might need to add a quizId field to VocabularyWord later or fetch via Quiz entity.

        // For demonstration, fetch all and limit.
        // In a real application, you'd want to fetch efficiently from the DB.
        List<VocabularyWord> words = vocabularyWordRepository.findAll().stream()
                .limit(limit)
                .collect(Collectors.toList());

        return words.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
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
                .tags(word.getTags()) // Map new field
                .build();
    }
}
