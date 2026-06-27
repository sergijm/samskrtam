package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;
import sm.selflearn.samskrtam.quiz.dto.AnswerResponse;
import sm.selflearn.samskrtam.quiz.dto.CompleteSessionResponse;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.event.QuizAnsweredEvent;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.mapper.QuizAnswerMapper;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ContentClient contentClient;
    private final QuizDataAssembler quizDataAssembler;
    private final OutboxEventCreator outboxEventCreator;
    private final QuizAnswerMapper quizAnswerMapper;
        private final ObjectMapper objectMapper;

    public Mono<StartOrResumeResponse> startOrResumeSession(UUID lessonId, UUID userId, String userLocale) {
        return quizSessionRepository.findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(userId, lessonId, SessionStatus.IN_PROGRESS)
                .flatMap(session -> resume(session.getId(), userId, userLocale))
                .switchIfEmpty(Mono.defer(() -> createNewSessionAndBuildStartOrResumeResponse(lessonId, userId, userLocale)));
    }

    public Mono<StartOrResumeResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return resume(sessionId, userId, userLocale);
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> quizAnswerRepository.existsBySessionIdAndQuestionId(sessionId, request.getQuestionId())
                        .flatMap(alreadyAnswered -> {
                            if (alreadyAnswered) {
                                return Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId()));
                            } else {
                                return contentClient.getGeneratedQuestion(request.getQuestionId())
                                        .flatMap(generatedQuestion -> processAndSaveAnswer(session, userId, request, generatedQuestion, userLocale));
                            }
                        })
                );
    }

    public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(this::completeAndPublishSessionStatus)
                .map(savedSession -> CompleteSessionResponse.builder()
                        .sessionId(savedSession.getId())
                        .score(savedSession.getScore())
                        .totalQuestions(savedSession.getTotalQuestions())
                        .durationMs(Duration.between(savedSession.getStartedAt(), savedSession.getCompletedAt()).toMillis())
                        .build());
    }

    public Mono<StartOrResumeResponse> retakeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> quizAnswerRepository.deleteBySessionId(sessionId).thenReturn(session))
                .flatMap(session -> resetAndPublishSessionStatus(session, userLocale));
    }

    public Mono<StartOrResumeResponse> startNewQuizFromExistingSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(this::completeAndPublishSessionStatus)
                .flatMap(completedSession -> createNewSessionAndBuildStartOrResumeResponse(completedSession.getLessonId(), userId, userLocale));
    }

    private Mono<AnswerResponse> processAndSaveAnswer(QuizSession session, UUID userId, AnswerRequest request, GeneratedQuizQuestionDto generatedQuestion, String userLocale) {
        return getVocabularyWords(session)
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
                .flatMap(savedSession -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(new QuizSessionStatusChangedEvent(savedSession.getId(), savedSession.getUserId(), savedSession.getLessonId(), savedSession.getLessonType(), oldStatus.name(), savedSession.getStatus().name(), Instant.now()))
                        .thenReturn(savedSession));
    }

        private Mono<StartOrResumeResponse> createNewSessionAndBuildStartOrResumeResponse(UUID lessonId, UUID userId, String userLocale) {
        return contentClient.generateQuizData(lessonId, userLocale)
                .flatMap(generatedQuizData -> {
                    String vocabularyWordsJson = serializeVocabularyWords(generatedQuizData.getVocabularyWords());
                    QuizSession newSession = buildNewQuizSession(lessonId, userId, generatedQuizData, vocabularyWordsJson);
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(new QuizSessionStatusChangedEvent(savedSession.getId(), userId, lessonId, savedSession.getLessonType(), null, SessionStatus.IN_PROGRESS.name(), Instant.now()))
                                    .then(quizDataAssembler.assembleResponse(savedSession, generatedQuizData.getGeneratedQuestions(), generatedQuizData.getVocabularyWords(), userLocale)));
                });
    }

    private Mono<StartOrResumeResponse> resume(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> {
                    if (session.getStatus() != SessionStatus.IN_PROGRESS) {
                        return updateAndPublishSessionStatusToInProgress(session, userLocale);
                    } else {
                        return fetchGeneratedQuizDataAndBuildResponse(session, userLocale);
                    }
                });
    }

    private Mono<StartOrResumeResponse> updateAndPublishSessionStatusToInProgress(QuizSession session, String userLocale) {
        SessionStatus oldStatus = session.getStatus();
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCompletedAt(null);
        return quizSessionRepository.save(session)
                .flatMap(savedSession -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(new QuizSessionStatusChangedEvent(savedSession.getId(), savedSession.getUserId(), savedSession.getLessonId(), savedSession.getLessonType(), oldStatus.name(), savedSession.getStatus().name(), Instant.now()))
                        .then(fetchGeneratedQuizDataAndBuildResponse(savedSession, userLocale)));
    }

    private Mono<StartOrResumeResponse> fetchGeneratedQuizDataAndBuildResponse(QuizSession session, String userLocale) {
        return contentClient.getGeneratedQuizData(session.getGeneratedQuizDataId())
                .flatMap(generatedQuizData -> getVocabularyWords(session)
                        .flatMap(allVocabularyWords -> quizDataAssembler.assembleResponse(session, generatedQuizData.getGeneratedQuestions(), allVocabularyWords, userLocale)));
    }

    private Mono<StartOrResumeResponse> resetAndPublishSessionStatus(QuizSession session, String userLocale) {
        SessionStatus oldStatus = session.getStatus();
        session.setScore(0);
        session.setAnsweredQuestions(0);
        session.setStartedAt(Instant.now());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCompletedAt(null);
        return quizSessionRepository.save(session)
                .flatMap(updatedSession -> outboxEventCreator.createAndSaveSessionStatusChangedEvent(new QuizSessionStatusChangedEvent(updatedSession.getId(), updatedSession.getUserId(), updatedSession.getLessonId(), updatedSession.getLessonType(), oldStatus.name(), updatedSession.getStatus().name(), Instant.now()))
                        .then(fetchGeneratedQuizDataAndBuildResponse(updatedSession, userLocale)));
    }

    private String serializeVocabularyWords(List<VocabularyWordDto> allVocabularyWords) {
        if (allVocabularyWords == null || allVocabularyWords.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(allVocabularyWords);
        } catch (JsonProcessingException e) {
            throw new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to serialize vocabulary words", e);
        }
    }

        private QuizSession buildNewQuizSession(UUID lessonId, UUID userId, GeneratedQuizData generatedQuizData, String vocabularyWordsJson) {
        return QuizSession.builder()
                .id(null)
                .userId(userId)
                .lessonId(lessonId)
                .lessonType(generatedQuizData.getLessonType())
                .totalQuestions(generatedQuizData.getQuestionsPerSession())
                .answeredQuestions(0)
                .score(0)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .vocabularyWordsJson(vocabularyWordsJson)
                .generatedQuizDataId(generatedQuizData.getGeneratedQuizDataId())
                .build();
    }

    private Mono<List<VocabularyWordDto>> getVocabularyWords(QuizSession session) {
        if (LessonType.isVocabulary(session.getLessonType()) && session.getVocabularyWordsJson() != null) {
            try {
                return Mono.just(objectMapper.readValue(session.getVocabularyWordsJson(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, VocabularyWordDto.class)));
            } catch (JsonProcessingException e) {
                return Mono.error(new SamskrtamException("JSON_PROCESSING_ERROR", "Failed to deserialize vocabulary words", e));
            }
        }
        return Mono.just(Collections.emptyList());
    }
}
