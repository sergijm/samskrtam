package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.dto.SessionDataResponse;
import sm.selflearn.samskrtam.events.AnswerSubmitted;
import sm.selflearn.samskrtam.events.SessionCompleted;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.*;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final SessionCacheService sessionCacheService;
    private final ContentClient contentClient;
    private final DeclensionOptionGeneratorService declensionOptionGeneratorService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public Mono<StartSessionResponse> startSession(UUID quizId, UUID userId, String userLocale) {
        return contentClient.getSessionData(quizId)
                .flatMap(sessionData -> {
                    // UUID sessionId = UUID.randomUUID(); // Let DB generate ID
                    List<CachedQuestion> cachedQuestions = sessionData.getQuestions().stream()
                            .map(qr -> CachedQuestion.builder()
                                    .questionId(qr.getId())
                                    .text(qr.getText())
                                    .explanationRu(qr.getExplanationRu()) // Changed from explanation
                                    .explanationEn(qr.getExplanationEn()) // Added explanationEn
                                    .declensionStemId(qr.getDeclensionStemId())
                                    .targetCase(qr.getTargetCase())
                                    .targetNumber(qr.getTargetNumber())
                                    .correctFormIast(qr.getCorrectFormIast())
                                    .correctFormDevanagari(qr.getCorrectFormDevanagari())
                                    .build())
                            .collect(Collectors.toList());

                    // Shuffle questions
                    Collections.shuffle(cachedQuestions);

                    QuizSession newSession = QuizSession.builder()
                            .id(null) // Let DB generate ID
                            .userId(userId)
                            .quizId(quizId)
                            .quizType(sessionData.getQuizType())
                            .totalQuestions(sessionData.getQuestionsPerSession())
                            .answeredQuestions(0)
                            .score(0)
                            .status(SessionStatus.IN_PROGRESS)
                            .startedAt(Instant.now())
                            // .isNew(true) // Removed isNew
                            .build();

                    // SessionCache will use the ID returned by the DB after saving
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                SessionCache sessionCache = SessionCache.builder()
                                        .sessionId(savedSession.getId()) // Use DB generated ID
                                        .userId(userId)
                                        .quizId(quizId)
                                        .quizType(sessionData.getQuizType())
                                        .questions(cachedQuestions)
                                        .answeredQuestionIds(new java.util.HashSet<>())
                                        .score(0)
                                        .build();
                                return sessionCacheService.put(savedSession.getId(), sessionCache)
                                        .then(buildStartSessionResponse(sessionCache, userLocale));
                            });
                });
    }

    public Mono<ResumeSessionResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return sessionCacheService.get(sessionId)
                .filter(cache -> cache.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(cache -> buildResumeSessionResponse(cache, userLocale));
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return sessionCacheService.get(sessionId)
                .filter(cache -> cache.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .filter(cache -> !cache.getAnsweredQuestionIds().contains(request.getQuestionId()))
                .switchIfEmpty(Mono.error(new SamskrtamException("ALREADY_ANSWERED", "Question already answered: " + request.getQuestionId())))
                .flatMap(cache -> {
                    CachedQuestion cachedQuestion = cache.findQuestion(request.getQuestionId());
                    boolean isCorrect = cachedQuestion.getCorrectFormIast().equals(getOptionIast(request.getSelectedOptionId(), cachedQuestion, userLocale)); // Need to get IAST from selectedOptionId

                    QuizAnswer newAnswer = QuizAnswer.builder()
                            .id(null) // Let DB generate ID
                            .sessionId(sessionId)
                            .questionId(request.getQuestionId())
                            .selectedOptionId(request.getSelectedOptionId())
                            .correctFormIast(cachedQuestion.getCorrectFormIast())
                            .correct(isCorrect)
                            .responseTimeMs(request.getResponseTimeMs())
                            .answeredAt(Instant.now())
                            // .isNew(true) // Removed isNew
                            .build();

                    // Publish AnswerSubmitted event to outbox
                    AnswerSubmitted event = new AnswerSubmitted(
                            userId, cache.getQuizType(), cache.getQuizId(),
                            request.getQuestionId(), request.getSelectedOptionId(),
                            isCorrect, request.getResponseTimeMs()
                    );
                    Mono<Void> outboxMono = Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                            .flatMap(payload -> outboxEventRepository.save(OutboxEvent.builder()
                                    .id(null) // Let DB generate ID
                                    .aggregateId(userId.toString())
                                    .topic("quiz.answer.submitted")
                                    .payload(payload)
                                    .status(OutboxStatus.PENDING)
                                    .eventType(OutboxEventType.ANSWER_SUBMITTED)
                                    // .isNew(true) // Removed isNew
                                    .build()))
                            .then();

                    return quizAnswerRepository.save(newAnswer)
                            .then(sessionCacheService.put(sessionId, cache))
                            .then(outboxMono)
                            .thenReturn(AnswerResponse.builder()
                                    .isCorrect(isCorrect)
                                    .correctOptionId(request.getSelectedOptionId()) // This might need to be the actual correct option ID from the generated options
                                    .explanationRu(cachedQuestion.getExplanationRu()) // Changed from getExplanation
                                    .explanationEn(cachedQuestion.getExplanationEn()) // Added explanationEn
                                    .questionNumber(cache.getAnsweredQuestionIds().size())
                                    .totalQuestions(cache.getQuestions().size())
                                    .build());
                });
    }

    public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
        return sessionCacheService.get(sessionId)
                .filter(cache -> cache.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(cache -> quizSessionRepository.findById(sessionId)
                        .flatMap(session -> {
                            session.setStatus(SessionStatus.COMPLETED);
                            session.setCompletedAt(Instant.now());
                            session.setScore(cache.getScore());
                            session.setAnsweredQuestions(cache.getAnsweredQuestionIds().size());

                            // Publish SessionCompleted event to outbox
                            SessionCompleted event = new SessionCompleted(
                                    userId, cache.getQuizType(), cache.getQuizId(),
                                    cache.getScore(), cache.getQuestions().size(),
                                    Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis()
                            );
                            Mono<Void> outboxMono = Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                                    .flatMap(payload -> outboxEventRepository.save(OutboxEvent.builder()
                                            .id(null) // Let DB generate ID
                                            .aggregateId(userId.toString())
                                            .topic("quiz.session.completed")
                                            .payload(payload)
                                            .status(OutboxStatus.PENDING)
                                            .eventType(OutboxEventType.SESSION_COMPLETED)
                                            // .isNew(true) // Removed isNew
                                            .build()))
                                    .then();

                            return quizSessionRepository.save(session)
                                    .then(sessionCacheService.evict(sessionId))
                                    .then(outboxMono)
                                    .thenReturn(CompleteSessionResponse.builder()
                                            .sessionId(sessionId)
                                            .score(cache.getScore())
                                            .totalQuestions(cache.getQuestions().size())
                                            .durationMs(Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis())
                                            .build());
                        }));
    }

    private Mono<StartSessionResponse> buildStartSessionResponse(SessionCache cache, String userLocale) {
        return Flux.fromIterable(cache.getQuestions())
                .flatMap(cachedQuestion ->
                        declensionOptionGeneratorService.generateOptions(
                                cachedQuestion.getDeclensionStemId(),
                                cachedQuestion.getTargetCase(),
                                cachedQuestion.getTargetNumber(),
                                cachedQuestion.getCorrectFormIast()
                        ).map(options -> QuestionDto.builder()
                                .id(cachedQuestion.getQuestionId())
                                .text(cachedQuestion.getText())
                                .options(options)
                                .build())
                )
                .collectList()
                .map(questions -> StartSessionResponse.builder()
                        .sessionId(cache.getSessionId())
                        .quizId(cache.getQuizId())
                        .quizType(cache.getQuizType())
                        .questions(questions)
                        .totalQuestions(cache.getQuestions().size())
                        .build());
    }

    private Mono<ResumeSessionResponse> buildResumeSessionResponse(SessionCache cache, String userLocale) {
        return Flux.fromIterable(cache.getQuestions())
                .flatMap(cachedQuestion ->
                        declensionOptionGeneratorService.generateOptions(
                                cachedQuestion.getDeclensionStemId(),
                                cachedQuestion.getTargetCase(),
                                cachedQuestion.getTargetNumber(),
                                cachedQuestion.getCorrectFormIast()
                        ).map(options -> QuestionDto.builder()
                                .id(cachedQuestion.getQuestionId())
                                .text(cachedQuestion.getText())
                                .options(options)
                                .build())
                )
                .collectList()
                .map(questions -> ResumeSessionResponse.builder()
                        .sessionId(cache.getSessionId())
                        .quizId(cache.getQuizId())
                        .quizType(cache.getQuizType())
                        .questions(questions)
                        .totalQuestions(cache.getQuestions().size())
                        .answeredQuestions(cache.getAnsweredQuestionIds().size())
                        .score(cache.getScore())
                        .currentQuestionIndex(cache.getAnsweredQuestionIds().size()) // Assuming current question is the next unanswered one
                        .build());
    }

    // Helper to get IAST from selectedOptionId (this will require finding the option within the generated options)
    private String getOptionIast(UUID selectedOptionId, CachedQuestion cachedQuestion, String userLocale) {
        // This is a placeholder. In a real scenario, you'd need to regenerate options
        // or store them in the cache to retrieve the IAST for the selectedOptionId.
        // For now, we'll assume the selectedOptionId directly corresponds to the correctFormIast if it's the correct one.
        // This logic needs refinement based on how options are generated and tracked.
        return cachedQuestion.getCorrectFormIast(); // Simplified for now
    }
}
