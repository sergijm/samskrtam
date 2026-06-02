package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
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
import sm.selflearn.samskrtam.content.dto.QuestionOptionResponse;
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.dto.QuizListItemResponse;
import sm.selflearn.samskrtam.content.dto.SessionDataResponse;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizContentService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;

    public Flux<QuizListItemResponse> getQuizList() {
        return Flux.fromIterable(quizRepository.findAll())
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
        return Mono.fromCallable(() -> quizRepository.findById(quizId)
                        .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found with ID: " + quizId)))
                .flatMap(quiz -> {
                    List<Question> questions = questionRepository.findByQuizId(quizId);
                    if (questions.isEmpty()) {
                        return Mono.error(new SamskrtamException("NO_QUESTIONS", "No questions found for quiz ID: " + quizId));
                    }

                    List<QuestionResponse> questionResponses = questions.stream()
                            .map(question -> {
                                QuestionOption correctOption = questionOptionRepository.findById(question.getCorrectOptionId())
                                        .orElseThrow(() -> new SamskrtamException("OPTION_NOT_FOUND", "Correct option not found for question ID: " + question.getId()));

                                return QuestionResponse.builder()
                                        .id(question.getId())
                                        .text(locale.getLanguage().equals("ru") ? question.getTextRu() : question.getTextEn())
                                        .explanation(locale.getLanguage().equals("ru") ? question.getExplanationRu() : question.getExplanationEn())
                                        .declensionStemId(question.getDeclensionStemId())
                                        .targetCase(question.getTargetCase())
                                        .targetNumber(question.getTargetNumber())
                                        .correctOption(QuestionOptionResponse.builder()
                                                .id(correctOption.getId())
                                                .formIast(correctOption.getFormIast())
                                                .formDevanagari(correctOption.getFormDevanagari())
                                                .build())
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return Mono.just(SessionDataResponse.builder()
                            .quizId(quiz.getId())
                            .quizType(quiz.getQuizType())
                            .questionsPerSession(quiz.getQuestionsPerSession())
                            .questions(questionResponses)
                            .build());
                });
    }
}
