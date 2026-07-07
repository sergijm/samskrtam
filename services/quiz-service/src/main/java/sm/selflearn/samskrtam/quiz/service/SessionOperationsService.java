package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;
import sm.selflearn.samskrtam.quiz.dto.AnswerResponse;
import sm.selflearn.samskrtam.quiz.dto.CompleteSessionResponse;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.event.QuizAnsweredEvent;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.mapper.QuizAnswerMapper;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionMapper;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionToDtoMapper;
import sm.selflearn.samskrtam.quiz.service.strategy.ScoreUpdateStrategyRegistry;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Содержит логику управления сессией: resume, retake, complete, submitAnswer.
 * Выделена из QuizSessionService для уменьшения размера класса.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionOperationsService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final QuizDataAssembler quizDataAssembler;
    private final OutboxEventCreator outboxEventCreator;
    private final QuizAnswerMapper quizAnswerMapper;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final SessionQuestionToDtoMapper sessionQuestionToDtoMapper;
    private final ScoreUpdateStrategyRegistry scoreUpdateStrategyRegistry;
    private final VocabularyWordsHelper vocabularyWordsHelper;
    private final ContentClient contentClient;
    private final SessionFactory sessionFactory;
    private final SessionQuestionMapper sessionQuestionMapper;
    private final SessionPublisher sessionPublisher;

    // ================== Public Methods ==================

    public Mono<AnswerResponse> submitAnswer(QuizSession session, UUID userId, AnswerRequest request, String userLocale) {
        return quizAnswerRepository.existsBySessionIdAndQuestionId(session.getId(), request.getQuestionId())
                .flatMap(alreadyAnswered -> {
                    if (alreadyAnswered) {
                        return Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId()));
                    }
                    return sessionQuestionRepository.findByQuestionId(request.getQuestionId())
                            .map(sessionQuestionToDtoMapper::toDto)
                            .flatMap(generatedQuestion -> processAndSaveAnswer(session, userId, request, generatedQuestion, userLocale));
                });
    }

    public Mono<CompleteSessionResponse> completeSession(QuizSession session) {
        return completeAndPublishSessionStatus(session)
                .map(savedSession -> CompleteSessionResponse.builder()
                        .sessionId(savedSession.getId())
                        .score(savedSession.getScore())
                        .totalQuestions(savedSession.getTotalQuestions())
                        .durationMs(Duration.between(savedSession.getStartedAt(), savedSession.getCompletedAt()).toMillis())
                        .build());
    }

    public Mono<StartOrResumeResponse> retakeSession(QuizSession session, String userLocale) {
        return quizAnswerRepository.deleteBySessionId(session.getId()).thenReturn(session)
                .flatMap(s -> resetAndPublishSessionStatus(s, userLocale));
    }

    public Mono<QuizSession> startNewQuizFromExistingSession(QuizSession session) {
        return completeAndPublishSessionStatus(session);
    }

    public Mono<StartOrResumeResponse> resume(QuizSession session, String userLocale) {
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            return updateAndPublishSessionStatusToInProgress(session, userLocale);
        } else {
            return fetchQuestionsFromDbAndBuildResponse(session, userLocale);
        }
    }

    // ================== Private Methods ==================

    private Mono<AnswerResponse> processAndSaveAnswer(QuizSession session, UUID userId, AnswerRequest request,
                                                       GeneratedQuizQuestionDto generatedQuestion, String userLocale) {
        return vocabularyWordsHelper.getVocabularyWords(session)
                .flatMap(allVocabularyWords -> {
                    String selectedOptionIast = quizDataAssembler.determineSelectedOptionIast(request, generatedQuestion, allVocabularyWords);
                    boolean isCorrect = generatedQuestion.getCorrectFormIast().equals(selectedOptionIast);
                    UUID correctWordId = quizDataAssembler.findCorrectWordId(generatedQuestion, allVocabularyWords);
                    String correctAnswerText = quizDataAssembler.findCorrectAnswerText(generatedQuestion, allVocabularyWords, userLocale);

                    QuizAnswer newAnswer = QuizAnswer.builder()
                            .id(null)
                            .sessionId(session.getId())
                            .questionId(request.getQuestionId())
                            .selectedOptionId(request.getSelectedOptionId())
                            .selectedFormIast(selectedOptionIast)
                            .correctFormIast(generatedQuestion.getCorrectFormIast())
                            .isCorrect(isCorrect)
                            .responseTimeMs(request.getResponseTimeMs())
                            .answeredAt(Instant.now())
                            .build();

                    return quizAnswerRepository.save(newAnswer)
                            .then(quizSessionRepository.incrementAnsweredQuestionsAndScore(session.getId(), isCorrect))
                            .then(scoreUpdateStrategyRegistry.getStrategy(session.getLessonType())
                                    .updateScore(userId, session.getLessonId(), generatedQuestion, isCorrect))
                            .then(outboxEventCreator.createAndSaveQuizAnsweredEvent(
                                    new QuizAnsweredEvent(session.getId(), userId, session.getLessonId(),
                                            session.getLessonType(), request.getQuestionId(),
                                            selectedOptionIast, isCorrect, Instant.now())))
                            .thenReturn(quizAnswerMapper.toAnswerResponse(
                                    isCorrect, correctWordId, correctAnswerText, generatedQuestion, session));
                });
    }

    private Mono<QuizSession> completeAndPublishSessionStatus(QuizSession session) {
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        return quizSessionRepository.save(session)
                .flatMap(savedSession -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(
                                new QuizSessionStatusChangedEvent(savedSession.getId(), savedSession.getUserId(),
                                        savedSession.getLessonId(), savedSession.getLessonType(),
                                        oldStatus.name(), savedSession.getStatus().name(), Instant.now()))
                        .thenReturn(savedSession));
    }

    private Mono<StartOrResumeResponse> updateAndPublishSessionStatusToInProgress(QuizSession session, String userLocale) {
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCompletedAt(null);
        return quizSessionRepository.save(session)
                .flatMap(savedSession -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(
                                new QuizSessionStatusChangedEvent(savedSession.getId(), savedSession.getUserId(),
                                        savedSession.getLessonId(), savedSession.getLessonType(),
                                        oldStatus.name(), savedSession.getStatus().name(), Instant.now()))
                        .then(fetchQuestionsFromDbAndBuildResponse(savedSession, userLocale)));
    }

    private Mono<StartOrResumeResponse> fetchQuestionsFromDbAndBuildResponse(QuizSession session, String userLocale) {
        return sessionQuestionRepository.findBySessionId(session.getId())
                .map(sessionQuestionToDtoMapper::toDto)
                .collectList()
                .flatMap(generatedQuestions -> vocabularyWordsHelper.getVocabularyWords(session)
                        .flatMap(allVocabularyWords -> quizDataAssembler.assembleResponse(
                                session, generatedQuestions, allVocabularyWords, userLocale)));
    }

    private Mono<StartOrResumeResponse> resetAndPublishSessionStatus(QuizSession session, String userLocale) {
        SessionStatus oldStatus = session.getStatus();
        session.setScore(0);
        session.setAnsweredQuestions(0);
        session.setStartedAt(Instant.now());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCompletedAt(null);
        return quizSessionRepository.save(session)
                .flatMap(updatedSession -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(
                                new QuizSessionStatusChangedEvent(updatedSession.getId(), updatedSession.getUserId(),
                                        updatedSession.getLessonId(), updatedSession.getLessonType(),
                                        oldStatus.name(), updatedSession.getStatus().name(), Instant.now()))
                        .then(fetchQuestionsFromDbAndBuildResponse(updatedSession, userLocale)));
    }
}