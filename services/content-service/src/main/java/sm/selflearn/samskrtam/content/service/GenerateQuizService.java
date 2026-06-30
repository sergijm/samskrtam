package sm.selflearn.samskrtam.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.model.GeneratedQuestion;
import sm.selflearn.samskrtam.content.model.GeneratedQuizDataRecord;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.repository.GeneratedQuestionRepository;
import sm.selflearn.samskrtam.content.repository.GeneratedQuizDataRecordRepository;
import sm.selflearn.samskrtam.content.repository.LessonRepository;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateQuizService {

    private final LessonRepository lessonRepository;
    private final QuestionGenerationService questionGenerationService;
    private final VocabularyService vocabularyService;
    private final GeneratedQuizDataRecordRepository generatedQuizDataRecordRepository;
    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public GeneratedQuizData generateQuizData(UUID quizId, Locale locale) {
        Lesson lesson = lessonRepository.findById(quizId)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with ID: " + quizId));

        UUID generatedQuizDataId = UUID.randomUUID();

        List<VocabularyWordDto> vocabularyWords = Collections.emptyList();
        String vocabularyWordsJson = null;
        if (LessonType.isVocabulary(lesson.getLessonType())) {
            vocabularyWords = vocabularyService.getVocabularyWordsForQuiz(lesson.getSlug(), lesson.getQuestionsPerSession() * 4);
            try {
                vocabularyWordsJson = objectMapper.writeValueAsString(vocabularyWords);
            } catch (JsonProcessingException e) {
                log.error("Error serializing vocabulary words for lesson {}: {}", quizId, e.getMessage());
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
                generatedQuizDataId, lesson, locale.getLanguage());

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

        List<GeneratedQuizQuestionDto> sortedQuestions = questions.stream()
                .sorted(Comparator.comparingInt(GeneratedQuizQuestionDto::getQuestionNumber))
                .toList();

        return GeneratedQuizData.builder()
                .generatedQuizDataId(generatedQuizDataId)
                .lessonId(lesson.getId())
                .lessonType(lesson.getLessonType())
                .questionsPerSession(lesson.getQuestionsPerSession())
                .generatedQuestions(sortedQuestions)
                .vocabularyWords(vocabularyWords)
                .build();
    }

    public GeneratedQuizData getGeneratedQuizData(UUID generatedQuizDataId) {
        GeneratedQuizDataRecord record = generatedQuizDataRecordRepository.findById(generatedQuizDataId)
                .orElseThrow(() -> new SamskrtamException("GENERATED_QUIZ_DATA_NOT_FOUND", "Generated quiz data not found with ID: " + generatedQuizDataId));

        List<GeneratedQuestion> questionEntities = generatedQuestionRepository
                .findByGeneratedQuizDataIdOrderByQuestionNumberAsc(generatedQuizDataId);
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

        Lesson lesson = lessonRepository.findById(record.getQuizId())
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with ID: " + record.getQuizId()));

        List<GeneratedQuizQuestionDto> sortedQuestions = questions.stream()
                .sorted(Comparator.comparingInt(GeneratedQuizQuestionDto::getQuestionNumber))
                .toList();

        return GeneratedQuizData.builder()
                .generatedQuizDataId(record.getId())
                .lessonId(lesson.getId())
                .lessonType(lesson.getLessonType())
                .questionsPerSession(lesson.getQuestionsPerSession())
                .generatedQuestions(sortedQuestions)
                .vocabularyWords(vocabularyWords)
                .build();
    }
}