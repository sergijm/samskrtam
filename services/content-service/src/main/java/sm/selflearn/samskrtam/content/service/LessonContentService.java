package sm.selflearn.samskrtam.content.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionStemDto;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.content.model.DeclensionStem;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.content.model.GeneratedQuizDataRecord;
import sm.selflearn.samskrtam.content.model.GeneratedQuestion;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.repository.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonContentService {

    private final LessonRepository lessonRepository;
    private final QuestionGenerationService questionGenerationService;
    private final VocabularyService vocabularyService;
    private final GeneratedQuizDataRecordRepository generatedQuizDataRecordRepository;
    private final GeneratedQuestionRepository generatedQuestionRepository;
    private final VocabularyCategoryRepository vocabularyCategoryRepository;
    private final VocabularyWordCategoryRepository vocabularyWordCategoryRepository;
    private final DeclensionStemRepository declensionStemRepository;
    private final ObjectMapper objectMapper;

    public List<LessonItemResponse> getLessonsList(String category) {
        return lessonRepository.findAll().stream()
                .filter(lesson -> {
                    if (category == null) {
                        return true;
                    }
                    switch (category.toLowerCase()) {
                        case "declensions":
                            return LessonType.isDeclensions(lesson.getLessonType());
                        case "conjugations":
                            return lesson.getLessonType() == LessonType.CONJUGATIONS;
                        case "vocabulary":
                            return LessonType.isVocabulary(lesson.getLessonType());
                        case "vocabulary-basic":
                        case "vocabulary_basic":
                            return lesson.getLessonType() == LessonType.VOCABULARY_BASIC;
                        case "vocabulary-text":
                        case "vocabulary-texts":
                        case "vocabulary_texts":
                            return lesson.getLessonType() == LessonType.VOCABULARY_TEXTS;
                        case "grammar":
                            return !LessonType.isVocabulary(lesson.getLessonType());
                        default:
                            try {
                                return lesson.getLessonType() == LessonType.valueOf(category.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                return true;
                            }
                    }
                })
                .map(this::mapToLessonItemResponse)
                .collect(Collectors.toList());
    }

    public LessonItemResponse getLessonItemBySlug(String slug) {
        log.debug("getLessonBySlug called with slug: {}", slug);
        return lessonRepository.findBySlug(slug)
                .map(this::mapToLessonItemResponse)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with slug: " + slug));
    }

    public LessonItemResponse getLessonItemById(UUID lessonId) {
        log.debug("getLessonById called with lessonId: {}", lessonId);
        return lessonRepository.findById(lessonId)
                .map(this::mapToLessonItemResponse)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with ID: " + lessonId));
    }

    public List<DeclensionStemDto> getDeclensionStemsForLesson(String slug) {
        log.debug("getDeclensionStemsForLesson called with slug: {}", slug);
        lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with slug: " + slug));

        VowelType vowelType = mapSlugToVowelType(slug);
        List<DeclensionStem> stems;
        if (vowelType != null) {
            stems = declensionStemRepository.findByVowelType(vowelType);
        } else {
            stems = declensionStemRepository.findAll();
        }

        return stems.stream()
                .map(stem -> DeclensionStemDto.builder()
                        .id(stem.getId())
                        .build())
                .collect(Collectors.toList());
    }

    private VowelType mapSlugToVowelType(String slug) {
        if (slug == null) return null;
        if (slug.startsWith("declensions-a-"))  return VowelType.A_STEM;
        if (slug.startsWith("declensions-aa-")) return VowelType.AA_STEM;
        if (slug.startsWith("declensions-ii-") || slug.equals("declensions-ii")) return VowelType.II_STEM;
        if (slug.startsWith("declensions-i-")  || slug.equals("declensions-i"))  return VowelType.I_STEM;
        if (slug.startsWith("declensions-uu-") || slug.equals("declensions-uu")) return VowelType.UU_STEM;
        if (slug.startsWith("declensions-u-")  || slug.equals("declensions-u"))  return VowelType.U_STEM;
        if (slug.startsWith("declensions-r-")  || slug.equals("declensions-r"))  return VowelType.R_STEM;
        return null;
    }

    private LessonItemResponse mapToLessonItemResponse(Lesson lesson) {
        int wordCount = 0;
        if (LessonType.isVocabulary(lesson.getLessonType())) {
            wordCount = vocabularyCategoryRepository.findByCodeIgnoreCase(lesson.getSlug())
                    .map(category -> {
                        List<UUID> allCategoryIds = vocabularyCategoryRepository.findAllChildrenIds(category.getId());
                        return vocabularyWordCategoryRepository.countByCategoryIdIn(allCategoryIds);
                    })
                    .orElse(0);
        }

        return LessonItemResponse.builder()
                .id(lesson.getId())
                .title(lesson.getTitleEn())
                .titleRu(lesson.getTitleRu())
                .titleEn(lesson.getTitleEn())
                .description(lesson.getDescriptionEn())
                .descriptionRu(lesson.getDescriptionRu())
                .descriptionEn(lesson.getDescriptionEn())
                .lessonType(lesson.getLessonType())
                .difficulty(lesson.getDifficulty())
                .slug(lesson.getSlug())
                .totalQuestions(lesson.getQuestionsPerSession())
                .wordCount(wordCount)
                .build();
    }

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
                generatedQuizDataId,
                lesson, locale.getLanguage());

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

        Lesson lesson = lessonRepository.findById(record.getQuizId())
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND", "Lesson not found with ID: " + record.getQuizId()));

        List<GeneratedQuizQuestionDto> sortedQuestions = questions.stream().sorted(Comparator.comparingInt(GeneratedQuizQuestionDto::getQuestionNumber)).toList();

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
