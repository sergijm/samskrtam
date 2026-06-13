package sm.selflearn.samskrtam.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.model.GeneratedQuizDataRecord;
import sm.selflearn.samskrtam.content.model.GeneratedQuestion;
import sm.selflearn.samskrtam.content.model.Quiz;
import sm.selflearn.samskrtam.content.repository.*;
import sm.selflearn.samskrtam.quiz.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.QuizListItemResponse;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizContentService {

    private final QuizRepository quizRepository;
    private final QuestionGenerationService questionGenerationService;
    private final VocabularyService vocabularyService;
    private final GeneratedQuizDataRecordRepository generatedQuizDataRecordRepository;
    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final VocabularyCategoryRepository vocabularyCategoryRepository;
    private final VocabularyWordCategoryRepository vocabularyWordCategoryRepository;
    private final ObjectMapper objectMapper;

    public List<QuizListItemResponse> getQuizList(String category) {
        return quizRepository.findAll().stream()
                .filter(quiz -> {
                    if (category == null) {
                        return true;
                    }
                    switch (category.toLowerCase()) {
                        case "declensions":
                            return quiz.getQuizType().toString().contains("DECLENSIONS");
                        case "conjugations":
                            return quiz.getQuizType() == QuizType.CONJUGATIONS;
                        case "vocabulary":
                            return quiz.getQuizType() == QuizType.VOCABULARY;
                        case "grammar": // Fallback for general grammar, if needed
                            return quiz.getQuizType() != QuizType.VOCABULARY;
                        default:
                            return true;
                    }
                })
                .map(this::mapToQuizListItemResponse)
                .collect(Collectors.toList());
    }

    private QuizListItemResponse mapToQuizListItemResponse(Quiz quiz) {
        int wordCount = 0;
        if (quiz.getQuizType() == QuizType.VOCABULARY) {
            wordCount = vocabularyCategoryRepository.findByCodeIgnoreCase(quiz.getSlug())
                    .map(category -> {
                        List<UUID> allCategoryIds = vocabularyCategoryRepository.findAllChildrenIds(category.getId());
                        return vocabularyWordCategoryRepository.countByCategoryIdIn(allCategoryIds);
                    })
                    .orElse(0);
        }

        return QuizListItemResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitleEn())
                .titleRu(quiz.getTitleRu())
                .titleEn(quiz.getTitleEn())
                .description(quiz.getDescriptionEn())
                .descriptionRu(quiz.getDescriptionRu())
                .descriptionEn(quiz.getDescriptionEn())
                .quizType(quiz.getQuizType())
                .slug(quiz.getSlug())
                .totalQuestions(quiz.getQuestionsPerSession())
                .wordCount(wordCount)
                .build();
    }

    @Transactional
    public GeneratedQuizData generateQuizData(UUID quizId, Locale locale) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found with ID: " + quizId));

        UUID generatedQuizDataId = UUID.randomUUID();

        List<VocabularyWordDto> vocabularyWords = Collections.emptyList();
        String vocabularyWordsJson = null;
        if (quiz.getQuizType() == QuizType.VOCABULARY) {
            vocabularyWords = vocabularyService.getVocabularyWordsForQuiz(quiz.getSlug(), quiz.getQuestionsPerSession() * 4);
            try {
                vocabularyWordsJson = objectMapper.writeValueAsString(vocabularyWords);
            } catch (JsonProcessingException e) {
                log.error("Error serializing vocabulary words for quiz {}: {}", quizId, e.getMessage());
                throw new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize vocabulary words", e);
            }
        }

        GeneratedQuizDataRecord record = GeneratedQuizDataRecord.builder()
                .id(generatedQuizDataId)
                .quizId(quizId)
                .userLocale(locale.getLanguage())
                .generatedAt(Instant.now())
                .vocabularyWordsJson(vocabularyWordsJson)
                .build();
        generatedQuizDataRecordRepository.save(record);

        List<GeneratedQuizQuestionDto> questions = questionGenerationService.generateQuestions(
                generatedQuizDataId,
                quiz, locale.getLanguage());

        List<GeneratedQuestion> generatedQuestionEntities = questions.stream()
                .map(dto -> GeneratedQuestion.builder()
                        .id(dto.getId())
                        .generatedQuizDataId(dto.getGeneratedQuizDataId())
                        .quizId(dto.getQuizId())
                        .questionNumber(dto.getQuestionNumber())
                        .text(dto.getText())
                        .explanationRu(dto.getExplanationRu())
                        .explanationEn(dto.getExplanationEn())
                        .declensionStemId(dto.getDeclensionStemId())
                        .targetCase(dto.getTargetCase())
                        .targetNumber(dto.getTargetNumber())
                        .correctFormIast(dto.getCorrectFormIast())
                        .correctFormDevanagari(dto.getCorrectFormDevanagari())
                        .vocabularyWordId(dto.getVocabularyWordId())
                        .questionSourceLanguage(dto.getQuestionSourceLanguage())
                        .questionTargetLanguage(dto.getQuestionTargetLanguage())
                        .correctTranslationRu(dto.getCorrectTranslationRu())
                        .correctTranslationEn(dto.getCorrectTranslationEn())
                        .userLocale(dto.getUserLocale())
                        .stem(dto.getStem())
                        .caseType(dto.getTargetCase())
                        .numberType(dto.getTargetNumber())
                        .build())
                .collect(Collectors.toList());
        generatedQuestionRepository.saveAll(generatedQuestionEntities);

        List<GeneratedQuizQuestionDto> sortedQuestions = questions.stream().sorted(Comparator.comparingInt(GeneratedQuizQuestionDto::getQuestionNumber)).toList();

        return GeneratedQuizData.builder()
                .generatedQuizDataId(generatedQuizDataId)
                .quizId(quiz.getId())
                .quizType(quiz.getQuizType())
                .questionsPerSession(quiz.getQuestionsPerSession())
                .generatedQuestions(sortedQuestions)
                .vocabularyWords(vocabularyWords)
                .build();
    }

    public GeneratedQuizData getGeneratedQuizData(UUID generatedQuizDataId) {
        GeneratedQuizDataRecord record = generatedQuizDataRecordRepository.findById(generatedQuizDataId)
                .orElseThrow(() -> new SamskrtamException("GENERATED_QUIZ_DATA_NOT_FOUND", "Generated quiz data not found with ID: " + generatedQuizDataId));

        List<GeneratedQuestion> questionEntities = generatedQuestionRepository.findByGeneratedQuizDataIdOrderByQuestionNumberAsc(generatedQuizDataId);
        List<GeneratedQuizQuestionDto> questions = questionEntities.stream()
                .map(entity -> GeneratedQuizQuestionDto.builder()
                        .id(entity.getId())
                        .generatedQuizDataId(entity.getGeneratedQuizDataId())
                        .quizId(entity.getQuizId())
                        .questionNumber(entity.getQuestionNumber())
                        .text(entity.getText())
                        .explanationRu(entity.getExplanationRu())
                        .explanationEn(entity.getExplanationEn())
                        .declensionStemId(entity.getDeclensionStemId())
                        .targetCase(entity.getTargetCase())
                        .targetNumber(entity.getTargetNumber())
                        .correctFormIast(entity.getCorrectFormIast())
                        .correctFormDevanagari(entity.getCorrectFormDevanagari())
                        .vocabularyWordId(entity.getVocabularyWordId())
                        .questionSourceLanguage(entity.getQuestionSourceLanguage())
                        .questionTargetLanguage(entity.getQuestionTargetLanguage())
                        .correctTranslationRu(entity.getCorrectTranslationRu())
                        .correctTranslationEn(entity.getCorrectTranslationEn())
                        .userLocale(entity.getUserLocale())
                        .stem(entity.getStem())
                        .caseType(entity.getTargetCase() != null ? entity.getTargetCase().getRuName() : null)
                        .numberType(entity.getTargetNumber() != null ? entity.getTargetNumber().getRuName() : null)
                        .build())
                .collect(Collectors.toList());

        List<VocabularyWordDto> vocabularyWords = Collections.emptyList();
        if (record.getVocabularyWordsJson() != null) {
            try {
                vocabularyWords = objectMapper.readValue(record.getVocabularyWordsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class));
            } catch (JsonProcessingException e) {
                log.error("Error deserializing vocabulary words for generated quiz data {}: {}", generatedQuizDataId, e.getMessage());
                throw new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e);
            }
        }

        Quiz quiz = quizRepository.findById(record.getQuizId())
                .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found with ID: " + record.getQuizId()));

        List<GeneratedQuizQuestionDto> sortedQuestions = questions.stream().sorted(Comparator.comparingInt(GeneratedQuizQuestionDto::getQuestionNumber)).toList();

        return GeneratedQuizData.builder()
                .generatedQuizDataId(record.getId())
                .quizId(quiz.getId())
                .quizType(quiz.getQuizType())
                .questionsPerSession(quiz.getQuestionsPerSession())
                .generatedQuestions(sortedQuestions)
                .vocabularyWords(vocabularyWords)
                .build();
    }
}
