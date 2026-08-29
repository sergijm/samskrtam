package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.QuestionLanguage;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionOptionDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LexicalOptionGeneratorService {

    private static final Random random = new Random();

    public Mono<List<QuestionOptionDto>> generateOptions(
            VocabularyWordDto correctWord,
            List<VocabularyWordDto> allWords,
            QuestionLanguage sourceLang,
            QuestionLanguage targetLang,
            String userLocale) {

        return Mono.fromCallable(() -> {
            if (targetLang == null) {
                log.error("targetLang is null for word: {}. sourceLang={}, userLocale={}", correctWord.getId(), sourceLang, userLocale);
                throw new IllegalArgumentException("targetLang must not be null for word: " + correctWord.getId());
            }

            List<QuestionOptionDto> options = new ArrayList<>();

            // 1. Add the correct option
            String correctOptionText = getTranslationForLanguage(correctWord, targetLang, userLocale);
            String correctOptionDevanagari = getDevanagariForLanguage(correctWord, targetLang, userLocale);

            options.add(QuestionOptionDto.builder()
                    .id(correctWord.getId())
                    .formIast(correctOptionText)
                    .formDevanagari(correctOptionDevanagari)
                    .build());

            // 2. Generate distractors
            List<VocabularyWordDto> potentialDistractors = allWords.stream()
                    .filter(word -> !word.getId().equals(correctWord.getId()))
                    .collect(Collectors.toList());

            List<VocabularyWordDto> filteredDistractors = potentialDistractors.stream()
                    .filter(word -> isSimilar(getTranslationForLanguage(word, targetLang, userLocale), correctOptionText))
                    .collect(Collectors.toList());

            if (filteredDistractors.size() < 3) {
                List<VocabularyWordDto> remainingDistractors = potentialDistractors.stream()
                        .filter(word -> !filteredDistractors.contains(word))
                        .collect(Collectors.toList());
                Collections.shuffle(remainingDistractors);
                filteredDistractors.addAll(remainingDistractors.subList(0, Math.min(3 - filteredDistractors.size(), remainingDistractors.size())));
            }

            Collections.shuffle(filteredDistractors);
            List<VocabularyWordDto> selectedDistractors = filteredDistractors.stream().limit(3).collect(Collectors.toList());

            for (VocabularyWordDto distractor : selectedDistractors) {
                options.add(QuestionOptionDto.builder()
                        .id(distractor.getId())
                        .formIast(getTranslationForLanguage(distractor, targetLang, userLocale))
                        .formDevanagari(getDevanagariForLanguage(distractor, targetLang, userLocale))
                        .build());
            }

            Collections.shuffle(options);
            return options;
        });
    }

    private String getTranslationForLanguage(VocabularyWordDto word, QuestionLanguage lang, String userLocale) {
        if (lang == null) {
            log.warn("getTranslationForLanguage called with null lang for word: {}. userLocale={}", word.getId(), userLocale);
            return null;
        }
        return switch (lang) {
            case SANSKRIT -> word.getWordIast();
            case ENGLISH -> word.getTranslationEn();
            case RUSSIAN -> word.getTranslationRu();
        };
    }

    private String getDevanagariForLanguage(VocabularyWordDto word, QuestionLanguage lang, String userLocale) {
        if (lang == null) {
            log.warn("getDevanagariForLanguage called with null lang for word: {}. userLocale={}", word.getId(), userLocale);
            return null;
        }
        return switch (lang) {
            case SANSKRIT -> word.getWordDevanagari();
            default -> null;
        };
    }

    private boolean isSimilar(String distractorText, String correctText) {
        int lengthDiff = Math.abs(distractorText.length() - correctText.length());
        if (lengthDiff > 3) return false;

        Set<Character> correctChars = correctText.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());
        long commonChars = distractorText.chars().mapToObj(c -> (char) c).filter(correctChars::contains).count();

        return commonChars >= 2;
    }
}

