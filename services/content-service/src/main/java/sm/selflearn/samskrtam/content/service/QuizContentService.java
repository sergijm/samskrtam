package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.model.QuestionOption;
import sm.selflearn.samskrtam.content.model.Quiz;
import sm.selflearn.samskrtam.content.repository.QuestionOptionRepository;
import sm.selflearn.samskrtam.content.repository.QuestionRepository;
import sm.selflearn.samskrtam.content.repository.QuizRepository;


// Импорт DTO из shared:quiz-content-dtos
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.dto.QuizListItemResponse;
import sm.selflearn.samskrtam.content.dto.SessionDataResponse;
import sm.selflearn.samskrtam.content.dto.QuizType; // Import QuizType
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto; // Import VocabularyWordDto

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizContentService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final DeclensionQuizGeneratorService declensionQuizGeneratorService; // Inject DeclensionQuizGeneratorService
    private final VocabularyService vocabularyService; // Inject VocabularyService

    public List<QuizListItemResponse> getQuizList(String category) {
        log.debug("getQuizList called with category: {}", category);
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
                .title(quiz.getTitleEn()) // Default to English title
                .titleRu(quiz.getTitleRu()) // Set Russian title
                .titleEn(quiz.getTitleEn()) // Set English title
                .description(quiz.getDescriptionEn()) // Default to English description
                .descriptionRu(quiz.getDescriptionRu()) // Set Russian description
                .descriptionEn(quiz.getDescriptionEn()) // Set English description
                .quizType(quiz.getQuizType())
                .slug(quiz.getSlug())
                .totalQuestions(quiz.getQuestionsPerSession())
                .build();
    }

    public SessionDataResponse getSessionData(UUID quizId, Locale locale) {
        log.debug("getSessionData called with quizId: {}, locale: {}", quizId, locale);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found with ID: " + quizId));

        List<QuestionResponse> questions = null;
        List<VocabularyWordDto> vocabularyWords = null;

        switch (quiz.getQuizType()) {
            case DECLENSIONS:
            case A_STEM_DECLENSIONS:
            case AA_STEM_DECLENSIONS:
            case I_STEM_DECLENSIONS:
            case II_STEM_DECLENSIONS:
            case U_STEM_DECLENSIONS:
            case UU_STEM_DECLENSIONS:
            case R_STEM_DECLENSIONS:
                questions = declensionQuizGeneratorService.generateDeclensionQuestions(quiz, locale);
                break;
            case CONJUGATIONS:
                // Existing logic for pre-defined questions
                questions = questionRepository.findByQuizId(quizId).stream()
                        .map(question -> {
                            QuestionOption correctOption = questionOptionRepository.findById(question.getCorrectOptionId())
                                    .orElseThrow(() -> new SamskrtamException("OPTION_NOT_FOUND", "Correct option not found for question ID: " + question.getId()));

                            return QuestionResponse.builder()
                                    .id(question.getId())
                                    .text(locale.getLanguage().equals("ru") ? question.getTextRu() : question.getTextEn())
                                    .explanationRu(question.getExplanationRu())
                                    .explanationEn(question.getExplanationEn())
                                    .declensionStemId(question.getDeclensionStemId())
                                    .targetCase(question.getTargetCase())
                                    .targetNumber(question.getTargetNumber())
                                    .correctFormIast(correctOption.getFormIast())
                                    .correctFormDevanagari(correctOption.getFormDevanagari())
                                    .build();
                        })
                        .collect(Collectors.toList());
                if (questions.isEmpty()) {
                    throw new SamskrtamException("NO_QUESTIONS", "No questions found for quiz ID: " + quizId);
                }
                break;
            case VOCABULARY:
                vocabularyWords = vocabularyService.getVocabularyWordsForQuiz(quizId, quiz.getQuestionsPerSession() * 4); // Fetch more words for distractors
                if (vocabularyWords.isEmpty()) {
                    throw new SamskrtamException("NO_VOCABULARY_WORDS", "No vocabulary words found for quiz ID: " + quizId);
                }
                break;
            default:
                throw new SamskrtamException("UNSUPPORTED_QUIZ_TYPE", "Unsupported quiz type: " + quiz.getQuizType());
        }

        return SessionDataResponse.builder()
                .quizId(quiz.getId())
                .quizType(quiz.getQuizType())
                .questionsPerSession(quiz.getQuestionsPerSession())
                .questions(questions) // Will be null for VOCABULARY
                .vocabularyWords(vocabularyWords) // Will be null for DECLENSIONS/CONJUGATIONS
                .build();
    }
}
