package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.QuestionLanguage;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.model.Lesson;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionGenerationService {

    private final DeclensionQuizGeneratorService declensionQuizGeneratorService;
    private final VocabularyService vocabularyService;

    public List<GeneratedQuizQuestionDto> generateQuestions(Lesson lesson, String userLocale) {
        log.debug("Generating new questions for quizId: {} and locale: {}", lesson.getId(), userLocale);

        var builders = new ArrayList<GeneratedQuizQuestionDto.GeneratedQuizQuestionDtoBuilder>();

        if (LessonType.isDeclensions(lesson.getLessonType())) {
            builders.addAll(declensionQuizGeneratorService.generateDeclensionQuestions(lesson, new Locale(userLocale)).stream()
                    .map(response -> GeneratedQuizQuestionDto.builder()
                            .id(UUID.randomUUID())
                            .quizId(lesson.getId())
                            .text(response.getText())
                            .explanationRu(response.getExplanationRu())
                            .explanationEn(response.getExplanationEn())
                            .declensionStemId(response.getDeclensionStemId())
                            .targetCase(response.getTargetCase())
                            .targetNumber(response.getTargetNumber())
                            .correctFormIast(response.getCorrectFormIast())
                            .correctFormDevanagari(response.getCorrectFormDevanagari())
                            .userLocale(userLocale)
                            .stem(response.getStem())
                            .caseType(response.getTargetCase() != null ? response.getTargetCase().getRuName() : null)
                            .numberType(response.getTargetNumber() != null ? response.getTargetNumber().getRuName() : null)
                            .stemDevanagari(response.getStemDevanagari())
                                                        .stemTranslationRu(response.getStemTranslationRu())
                                                        .stemTranslationEn(response.getStemTranslationEn())
                            .gender(response.getGender()))
                    .collect(Collectors.toList()));
        } else if (LessonType.isVocabulary(lesson.getLessonType())) {
            List<VocabularyWordDto> vocabularyWords = vocabularyService.getVocabularyWordsForQuiz(lesson.getSlug(), lesson.getQuestionsPerSession() * 4);

            for (VocabularyWordDto word : vocabularyWords) {
                // Sanskrit to Translation
                builders.add(GeneratedQuizQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .quizId(lesson.getId())
                        .text(String.format(
                                userLocale.equals("ru") ? "Как переводится слово '%s'?" : "How is the word '%s' translated?",
                                word.getWordIast()))
                        .explanationRu(word.getExplanationRu())
                        .explanationEn(word.getExplanationEn())
                        .vocabularyWordId(word.getId())
                        .questionSourceLanguage(QuestionLanguage.SANSKRIT)
                        .questionTargetLanguage(userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH)
                        .correctTranslationRu(word.getTranslationRu())
                        .correctTranslationEn(word.getTranslationEn())
                        .correctFormIast(userLocale.equals("ru") ? word.getTranslationRu() : word.getTranslationEn())
                        .userLocale(userLocale)
                        .stem(word.getWordIast()));

                // Translation to Sanskrit
                builders.add(GeneratedQuizQuestionDto.builder()
                        .id(UUID.randomUUID())
                        .quizId(lesson.getId())
                        .text(String.format(
                                userLocale.equals("ru") ? "Как будет '%s' на санскрите?" : "How is '%s' in Sanskrit?",
                                userLocale.equals("ru") ? word.getTranslationRu() : word.getTranslationEn()))
                        .explanationRu(word.getExplanationRu())
                        .explanationEn(word.getExplanationEn())
                        .vocabularyWordId(word.getId())
                        .questionSourceLanguage(userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH)
                        .questionTargetLanguage(QuestionLanguage.SANSKRIT)
                        .correctTranslationRu(word.getTranslationRu())
                        .correctTranslationEn(word.getTranslationEn())
                        .correctFormIast(word.getWordIast())
                        .userLocale(userLocale)
                        .stem(word.getWordIast()));
            }
        }

        // Shuffle and assign question numbers
        Collections.shuffle(builders);
        List<GeneratedQuizQuestionDto> questions = IntStream.range(0, Math.min(builders.size(), lesson.getQuestionsPerSession()))
                .mapToObj(i -> builders.get(i).questionNumber(i + 1).build())
                .collect(Collectors.toList());

        return questions;
    }
}

