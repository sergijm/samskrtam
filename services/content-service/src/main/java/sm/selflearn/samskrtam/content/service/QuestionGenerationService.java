package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuestionLanguage;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.model.GeneratedQuestion;
import sm.selflearn.samskrtam.content.model.Quiz;
import sm.selflearn.samskrtam.content.repository.GeneratedQuestionRepository;
import sm.selflearn.samskrtam.quiz.dto.GeneratedQuizQuestionDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionGenerationService {

    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final DeclensionQuizGeneratorService declensionQuizGeneratorService;
    private final VocabularyService vocabularyService;

    public List<GeneratedQuizQuestionDto> generateQuestions(UUID generatedQuizDataId, Quiz quiz, String userLocale) {

        log.debug("Generating new questions for quizId: {} and locale: {}", quiz.getId(), userLocale);
        List<GeneratedQuestion> newQuestions = generateAndSaveQuestions(generatedQuizDataId, quiz, userLocale);
        return newQuestions.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public GeneratedQuizQuestionDto getGeneratedQuestionById(UUID questionId) {

        GeneratedQuestion question = generatedQuestionRepository.findById(questionId)
                .orElseThrow(() -> new SamskrtamException("QUESTION_NOT_FOUND", "Generated question not found with ID: " + questionId));
        return mapToDto(question);
    }

    private List<GeneratedQuestion> generateAndSaveQuestions(UUID generatedQuizDataId, Quiz quiz, String userLocale) {
        List<GeneratedQuestion> questionsToSave = new ArrayList<>();

        if (quiz.getQuizType().toString().contains("DECLENSIONS")) {
            questionsToSave.addAll(declensionQuizGeneratorService.generateDeclensionQuestions(quiz, new Locale(userLocale)).stream()
                    .map(response -> GeneratedQuestion.builder()
                            .id(UUID.randomUUID())
                            .generatedQuizDataId(generatedQuizDataId)
                            .quizId(quiz.getId())
                            .text(response.getText())
                            .explanationRu(response.getExplanationRu())
                            .explanationEn(response.getExplanationEn())
                            .declensionStemId(response.getDeclensionStemId())
                            .targetCase(response.getTargetCase())
                            .targetNumber(response.getTargetNumber())
                            .correctFormIast(response.getCorrectFormIast())
                            .correctFormDevanagari(response.getCorrectFormDevanagari())
                            .userLocale(userLocale)
                            .build())
                    .collect(Collectors.<GeneratedQuestion>toList()));
        } else if (quiz.getQuizType() == QuizType.VOCABULARY) {
            List<VocabularyWordDto> vocabularyWords = vocabularyService.getVocabularyWordsForQuiz(quiz.getId(), quiz.getQuestionsPerSession() * 4); // Fetch more words than needed for options

            for (VocabularyWordDto word : vocabularyWords) {
                // Sanskrit to Translation
                String questionTextSanskritToTranslation = String.format(
                        userLocale.equals("ru") ? "Как переводится слово '%s'?" : "How is the word '%s' translated?",
                        userLocale.equals("ru") ? word.getWordDevanagari() : word.getWordIast()
                );
                questionsToSave.add(GeneratedQuestion.builder()
                        .id(UUID.randomUUID())
                        .generatedQuizDataId(generatedQuizDataId)
                        .quizId(quiz.getId())
                        .text(questionTextSanskritToTranslation)
                        .explanationRu(word.getExplanationRu())
                        .explanationEn(word.getExplanationEn())
                        .vocabularyWordId(word.getId())
                        .questionSourceLanguage(QuestionLanguage.SANSKRIT)
                        .questionTargetLanguage(userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH)
                        .correctTranslationRu(word.getTranslationRu())
                        .correctTranslationEn(word.getTranslationEn())
                        .correctFormIast(userLocale.equals("ru") ? word.getTranslationRu() : word.getTranslationEn())
                        .userLocale(userLocale)
                        .build());

                // Translation to Sanskrit
                String questionTextTranslationToSanskrit = String.format(
                        userLocale.equals("ru") ? "Как будет '%s' на санскрите?" : "How is '%s' in Sanskrit?",
                        userLocale.equals("ru") ? word.getTranslationRu() : word.getTranslationEn()
                );
                questionsToSave.add(GeneratedQuestion.builder()
                        .id(UUID.randomUUID())
                        .generatedQuizDataId(generatedQuizDataId)
                        .quizId(quiz.getId())
                        .text(questionTextTranslationToSanskrit)
                        .explanationRu(word.getExplanationRu())
                        .explanationEn(word.getExplanationEn())
                        .vocabularyWordId(word.getId())
                        .questionSourceLanguage(userLocale.equals("ru") ? QuestionLanguage.RUSSIAN : QuestionLanguage.ENGLISH)
                        .questionTargetLanguage(QuestionLanguage.SANSKRIT)
                        .correctTranslationRu(word.getTranslationRu())
                        .correctTranslationEn(word.getTranslationEn())
                        .correctFormIast(word.getWordIast())
                        .userLocale(userLocale)
                        .build());
            }
        }

        Collections.shuffle(questionsToSave);
        // Limit to questionsPerSession if more were generated
        List<GeneratedQuestion> finalQuestions = questionsToSave.stream()
                .limit(quiz.getQuestionsPerSession())
                .collect(Collectors.toList());

        return generatedQuestionRepository.saveAll(finalQuestions);
    }

    private GeneratedQuizQuestionDto mapToDto(GeneratedQuestion question) {
        return GeneratedQuizQuestionDto.builder()
                .id(question.getId())
                .quizId(question.getQuizId())
                .text(question.getText())
                .explanationRu(question.getExplanationRu())
                .explanationEn(question.getExplanationEn())
                .declensionStemId(question.getDeclensionStemId())
                .targetCase(question.getTargetCase())
                .targetNumber(question.getTargetNumber())
                .correctFormIast(question.getCorrectFormIast())
                .correctFormDevanagari(question.getCorrectFormDevanagari())
                .vocabularyWordId(question.getVocabularyWordId())
                .questionSourceLanguage(question.getQuestionSourceLanguage())
                .questionTargetLanguage(question.getQuestionTargetLanguage())
                .correctTranslationRu(question.getCorrectTranslationRu())
                .correctTranslationEn(question.getCorrectTranslationEn())
                .userLocale(question.getUserLocale())
                .build();
    }
}
