package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionDto;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.mapper.QuizSessionMapper;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сборка {@link StartOrResumeResponse} для старта/возобновления сессии.
 * Делегирует построение опций вопросов в {@link QuestionOptionsBuilder}.
 */
@Service
@RequiredArgsConstructor
public class QuizDataAssembler {

    private final ContentClient contentClient;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizSessionMapper quizSessionMapper;
    private final QuestionOptionsBuilder questionOptionsBuilder;

    /**
     * Собрать полный ответ для start/resume сессии.
     */
    public Mono<StartOrResumeResponse> assembleResponse(QuizSession session, List<GeneratedQuizQuestionDto> generatedQuestions, List<VocabularyWordDto> allVocabularyWords, String userLocale) {
        return contentClient.getLessonItem(session.getLessonId())
                .flatMap(quizSummary -> quizAnswerRepository.findBySessionId(session.getId())
                        .collectList()
                        .flatMap(answeredQuestions -> {
                            Set<UUID> answeredQuestionIds = answeredQuestions.stream()
                                    .map(QuizAnswer::getQuestionId)
                                    .collect(Collectors.toSet());

                            List<GeneratedQuizQuestionDto> sortedQuestions = generatedQuestions.stream()
                                    .sorted(Comparator.comparing(GeneratedQuizQuestionDto::getQuestionNumber))
                                    .collect(Collectors.toList());

                                                                                    return Flux.fromIterable(sortedQuestions)
                                    .flatMap(gq -> questionOptionsBuilder.buildOptions(session, gq, allVocabularyWords, userLocale))
                                    .collectList()
                                    .map(questions -> {
                                        questions.sort(Comparator.comparing(QuestionDto::getQuestionNumber));
                                        return quizSessionMapper.toStartOrResumeResponse(session, questions, quizSummary, List.copyOf(answeredQuestionIds));
                                    });
                        }));
    }
}
