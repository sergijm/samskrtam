package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionOptionDto;
import sm.selflearn.samskrtam.quiz.model.QuestionLanguage;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LexicalOptionGeneratorService {

    private static final Random random = new Random();

    public Mono<List<QuestionOptionDto>> generateOptions(
            VocabularyWordDto correctWord,
            List<VocabularyWordDto> allWords,
            QuestionLanguage sourceLang,
            QuestionLanguage targetLang,
            String userLocale) {

        return Mono.fromCallable(() -> {
            List<QuestionOptionDto> options = new ArrayList<>();

            // 1. Add the correct option
            String correctOptionText = getTranslationForLanguage(correctWord, targetLang, userLocale);
            String correctOptionDevanagari = getDevanagariForLanguage(correctWord, targetLang, userLocale); // Get Devanagari if applicable

            options.add(QuestionOptionDto.builder()
                    .id(correctWord.getId()) // Use word ID as option ID
                    .formIast(correctOptionText)
                    .formDevanagari(correctOptionDevanagari)
                    .build());

            // 2. Generate distractors
            List<VocabularyWordDto> potentialDistractors = allWords.stream()
                    .filter(word -> !word.getId().equals(correctWord.getId())) // Exclude the correct word itself
                    .collect(Collectors.toList());

            // Filter for similar length and common letters
            List<VocabularyWordDto> filteredDistractors = potentialDistractors.stream()
                    .filter(word -> isSimilar(getTranslationForLanguage(word, targetLang, userLocale), correctOptionText))
                    .collect(Collectors.toList());

            // If not enough filtered distractors, take from general pool
            if (filteredDistractors.size() < 3) {
                List<VocabularyWordDto> remainingDistractors = potentialDistractors.stream()
                        .filter(word -> !filteredDistractors.contains(word))
                        .collect(Collectors.toList());
                Collections.shuffle(remainingDistractors);
                filteredDistractors.addAll(remainingDistractors.subList(0, Math.min(3 - filteredDistractors.size(), remainingDistractors.size())));
            }

            Collections.shuffle(filteredDistractors); // Shuffle filtered distractors
            List<VocabularyWordDto> selectedDistractors = filteredDistractors.stream().limit(3).collect(Collectors.toList());


            for (VocabularyWordDto distractor : selectedDistractors) {
                options.add(QuestionOptionDto.builder()
                        .id(distractor.getId())
                        .formIast(getTranslationForLanguage(distractor, targetLang, userLocale))
                        .formDevanagari(getDevanagariForLanguage(distractor, targetLang, userLocale))
                        .build());
            }

            Collections.shuffle(options); // Shuffle all options (correct + distractors)
            return options;
        });
    }

    private String getTranslationForLanguage(VocabularyWordDto word, QuestionLanguage lang, String userLocale) {
        return switch (lang) {
            case SANSKRIT -> word.getWordIast(); // Always return IAST for Sanskrit target language
            case ENGLISH -> word.getTranslationEn();
            case RUSSIAN -> word.getTranslationRu();
        };
    }

    private String getDevanagariForLanguage(VocabularyWordDto word, QuestionLanguage lang, String userLocale) {
        return switch (lang) {
            case SANSKRIT -> word.getWordDevanagari(); // Return Devanagari for Sanskrit target language
            default -> null; // Devanagari not applicable for English/Russian translations
        };
    }

    private boolean isSimilar(String distractorText, String correctText) {
        // Simple similarity check: similar length and some common characters
        int lengthDiff = Math.abs(distractorText.length() - correctText.length());
        if (lengthDiff > 3) return false; // Length difference threshold

        Set<Character> correctChars = correctText.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        long commonChars = distractorText.chars().mapToObj(c -> (char) c).filter(correctChars::contains).count();

        return commonChars >= 2; // At least 2 common characters
    }
}
