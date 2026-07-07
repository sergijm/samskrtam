package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;
import sm.selflearn.samskrtam.quiz.dto.AnswerResponse;
import sm.selflearn.samskrtam.quiz.dto.CompleteSessionResponse;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис управления квиз-сессиями.
 * Содержит публичные API-методы; вся сложная логика делегирована в {@link SessionOperationsService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final ContentClient contentClient;
    private final QuizDataAssembler quizDataAssembler;
    private final SessionFactory sessionFactory;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final SessionQuestionMapper sessionQuestionMapper;
    private final SessionPublisher sessionPublisher;
    private final SessionOperationsService sessionOperationsService;

    public Mono<StartOrResumeResponse> startOrResumeSession(UUID lessonId, UUID userId, String userLocale) {
        return quizSessionRepository
                .findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(userId, lessonId, SessionStatus.IN_PROGRESS)
                .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                .switchIfEmpty(Mono.defer(() -> createNewSession(lessonId, userId, userLocale)));
    }

    public Mono<StartOrResumeResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionOperationsService.resume(session, userLocale));
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionOperationsService.submitAnswer(session, userId, request, userLocale));
    }

    public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(sessionOperationsService::completeSession);
    }

    public Mono<StartOrResumeResponse> retakeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionOperationsService.retakeSession(session, userLocale));
    }

    public Mono<StartOrResumeResponse> startNewQuizFromExistingSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionOperationsService.startNewQuizFromExistingSession(session)
                        .flatMap(completedSession ->
                                createNewSession(completedSession.getLessonId(), userId, userLocale)));
    }

    /**
     * Создаёт новую сессию: запрашивает данные квиза, сохраняет сессию и вопросы.
     */
    private Mono<StartOrResumeResponse> createNewSession(UUID lessonId, UUID userId, String userLocale) {
        return contentClient.generateQuizData(lessonId, userLocale)
                .flatMap(generatedQuizData -> {
                    QuizSession newSession = sessionFactory.createSession(lessonId, userId, generatedQuizData);
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                List<SessionQuestion> sessionQuestions = generatedQuizData.getGeneratedQuestions()
                                        .stream()
                                        .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                                        .collect(Collectors.toList());
                                return sessionQuestionRepository.saveAll(sessionQuestions)
                                        .then(sessionPublisher.publishStarted(savedSession))
                                        .then(quizDataAssembler.assembleResponse(
                                                savedSession,
                                                generatedQuizData.getGeneratedQuestions(),
                                                generatedQuizData.getVocabularyWords(),
                                                userLocale));
                            });
                });
    }
}
