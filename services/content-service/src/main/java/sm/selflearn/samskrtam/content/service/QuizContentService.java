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
import sm.selflearn.samskrtam.content.repository.GeneratedQuizDataRecordRepository;
import sm.selflearn.samskrtam.content.repository.GeneratedQuestionRepository;
import sm.selflearn.samskrtam.content.repository.QuizRepository;
import sm.selflearn.samskrtam.quiz.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.QuizListItemResponse;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizContentService {

    private final QuizRepository quizRepository;
    private final QuestionGenerationService questionGenerationService;
    private final VocabularyService vocabularyService;
    private final GeneratedQuizDataRecordRepository generatedQuizDataRecordRepository; // Inject new repository
    private final GeneratedQuestionRepository generatedQuestionRepository; // Inject new repository
    private final ObjectMapper objectMapper; // Inject ObjectMapper for JSON serialization

    public List<QuizListItemResponse> getQuizList(String category) {
        return quizRepository.findAll().stream()
                .filter(quiz -> {
                    if (category == null) {
                        return true;
                    }
                    if ("grammar".equalsIgnoreCase(category)) {
                        return quiz.getQuizType() != QuizType.VOCABULARY;
                    }
                    if ("vocabulary".equalsIgnoreCase(category)) {
                        return quiz.getQuizType() == QuizType.VOCABULARY;
                    }
                    return true;
                })
                .map(this::mapToQuizListItemResponse)
                .collect(Collectors.toList());
    }

    private QuizListItemResponse mapToQuizListItemResponse(Quiz quiz) {
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
            vocabularyWords = vocabularyService.getVocabularyWordsForQuiz(quizId, quiz.getQuestionsPerSession() * 4);
            try {
                vocabularyWordsJson = objectMapper.writeValueAsString(vocabularyWords);
            } catch (JsonProcessingException e) {
                log.error("Error serializing vocabulary words for quiz {}: {}", quizId, e.getMessage());
                throw new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize vocabulary words", e);
            }
        }

        // Save GeneratedQuizDataRecord
        GeneratedQuizDataRecord record = GeneratedQuizDataRecord.builder()
                .id(generatedQuizDataId)
                .quizId(quizId)
                // Removed quizType from here as it's no longer in GeneratedQuizDataRecord
                .userLocale(locale.getLanguage())
                .generatedAt(Instant.now())
                .vocabularyWordsJson(vocabularyWordsJson)
                .build();
        generatedQuizDataRecordRepository.save(record);

        List<GeneratedQuizQuestionDto> questions = questionGenerationService.generateQuestions(
                generatedQuizDataId,
                quiz, locale.getLanguage());


        return GeneratedQuizData.builder()
                .generatedQuizDataId(generatedQuizDataId)
                .quizId(quiz.getId())
                .quizType(quiz.getQuizType()) // Get quizType from Quiz entity
                .questionsPerSession(quiz.getQuestionsPerSession())
                .generatedQuestions(questions)
                .vocabularyWords(vocabularyWords)
                .build();
    }

    public GeneratedQuizData getGeneratedQuizData(UUID generatedQuizDataId) {
        GeneratedQuizDataRecord record = generatedQuizDataRecordRepository.findById(generatedQuizDataId)
                .orElseThrow(() -> new SamskrtamException("GENERATED_QUIZ_DATA_NOT_FOUND", "Generated quiz data not found with ID: " + generatedQuizDataId));

        List<GeneratedQuestion> questionEntities = generatedQuestionRepository.findByGeneratedQuizDataId(generatedQuizDataId);
        List<GeneratedQuizQuestionDto> questions = questionEntities.stream()
                .map(entity -> GeneratedQuizQuestionDto.builder()
                        .id(entity.getId())
                        .quizId(entity.getQuizId())
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
                        .build())
                .collect(Collectors.toList());

        List<VocabularyWordDto> vocabularyWords = Collections.emptyList();
        if (record.getVocabularyWordsJson() != null) { // Check if vocabularyWordsJson exists
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


        return GeneratedQuizData.builder()
                .generatedQuizDataId(record.getId())
                .quizId(record.getQuizId())
                .quizType(quiz.getQuizType()) // Get quizType from Quiz entity
                .questionsPerSession(quiz.getQuestionsPerSession()) // Get from Quiz entity
                .generatedQuestions(questions)
                .vocabularyWords(vocabularyWords)
                .build();
    }
}
