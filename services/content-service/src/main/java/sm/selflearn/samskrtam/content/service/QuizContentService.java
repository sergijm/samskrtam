package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Import Slf4j
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.model.Question;
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

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j // Add Slf4j annotation
public class QuizContentService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final DeclensionQuizGeneratorService declensionQuizGeneratorService; // Inject DeclensionQuizGeneratorService

    public Flux<QuizListItemResponse> getQuizList(String category) {
        log.debug("getQuizList called with category: {}", category); // Logging argument
        return Flux.fromIterable(quizRepository.findAll())
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
                .map(this::mapToQuizListItemResponse);
    }

    private QuizListItemResponse mapToQuizListItemResponse(Quiz quiz) {
        return QuizListItemResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitleEn())
                .description(quiz.getDescriptionEn())
                .quizType(quiz.getQuizType())
                .slug(quiz.getSlug())
                .totalQuestions(quiz.getQuestionsPerSession())
                .build();
    }

    public Mono<SessionDataResponse> getSessionData(UUID quizId, Locale locale) {
        log.debug("getSessionData called with quizId: {}, locale: {}", quizId, locale); // Logging arguments
        return Mono.fromCallable(() -> quizRepository.findById(quizId)
                        .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found with ID: " + quizId)))
                .flatMap(quiz -> {
                    Mono<List<QuestionResponse>> questionsMono;

                    // Determine how to get questions based on QuizType
                    switch (quiz.getQuizType()) {
                        case DECLENSIONS:
                        case A_STEM_DECLENSIONS:
                        case AA_STEM_DECLENSIONS:
                        case I_STEM_DECLENSIONS:
                        case II_STEM_DECLENSIONS:
                        case U_STEM_DECLENSIONS:
                        case UU_STEM_DECLENSIONS:
                        case R_STEM_DECLENSIONS:
                            questionsMono = declensionQuizGeneratorService.generateDeclensionQuestions(quiz, locale);
                            break;
                        case CONJUGATIONS:
                        case VOCABULARY:
                            // Existing logic for pre-defined questions
                            questionsMono = Mono.fromCallable(() -> {
                                List<Question> questions = questionRepository.findByQuizId(quizId);
                                if (questions.isEmpty()) {
                                    throw new SamskrtamException("NO_QUESTIONS", "No questions found for quiz ID: " + quizId);
                                }
                                return questions.stream()
                                        .map(question -> {
                                            QuestionOption correctOption = questionOptionRepository.findById(question.getCorrectOptionId())
                                                    .orElseThrow(() -> new SamskrtamException("OPTION_NOT_FOUND", "Correct option not found for question ID: " + question.getId()));

                                            return QuestionResponse.builder()
                                                    .id(question.getId())
                                                    .text(locale.getLanguage().equals("ru") ? question.getTextRu() : question.getTextEn())
                                                    .explanationRu(question.getExplanationRu()) // Added explanationRu
                                                    .explanationEn(question.getExplanationEn()) // Added explanationEn
                                                    .declensionStemId(question.getDeclensionStemId())
                                                    .targetCase(question.getTargetCase())
                                                    .targetNumber(question.getTargetNumber())
                                                    .correctFormIast(correctOption.getFormIast())
                                                    .correctFormDevanagari(correctOption.getFormDevanagari())
                                                    .build();
                                        })
                                        .collect(Collectors.toList());
                            });
                            break;
                        default:
                            questionsMono = Mono.error(new SamskrtamException("UNSUPPORTED_QUIZ_TYPE", "Unsupported quiz type: " + quiz.getQuizType()));
                    }

                    return questionsMono.map(questionResponses -> SessionDataResponse.builder()
                            .quizId(quiz.getId())
                            .quizType(quiz.getQuizType())
                            .questionsPerSession(quiz.getQuestionsPerSession())
                            .questions(questionResponses)
                            .build());
                });
    }
}
