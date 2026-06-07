package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryDto; // Import AnswerHistoryDto
import sm.selflearn.samskrtam.quiz.dto.QuizSessionSummaryDto;
import sm.selflearn.samskrtam.quiz.model.CachedQuestion; // Import CachedQuestion
import sm.selflearn.samskrtam.quiz.model.QuizAnswer; // Import QuizAnswer
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionCache; // Import SessionCache
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository; // Inject QuizAnswerRepository
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.time.Duration;
import java.util.List;
import java.util.Locale; // Import Locale
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository; // Inject QuizAnswerRepository
    private final SessionCacheService sessionCacheService; // Inject SessionCacheService
    private final ContentClient contentClient;

    public Mono<Page<QuizSessionSummaryDto>> getUserQuizSessions(
            UUID userId,
            QuizType quizType,
            SessionStatus status,
            Pageable pageable) {

        // 1. Fetch sessions with pagination and sorting using new repository methods
        Mono<Long> totalElementsMono = quizSessionRepository.countUserSessions(userId, quizType, status);
        Flux<QuizSession> sessionsFlux = quizSessionRepository.findUserSessions(userId, quizType, status, pageable);

        // 2. Fetch quiz titles from content-service
        Mono<Map<UUID, QuizSummaryDto>> quizSummariesMapMono = sessionsFlux
                .map(QuizSession::getQuizId)
                .collect(Collectors.toSet()) // Collect unique quiz IDs
                .flatMap(quizIds -> {
                    if (quizIds.isEmpty()) {
                        return Mono.just(Map.of());
                    }
                    return Flux.fromIterable(quizIds)
                            .flatMap(contentClient::getQuizSummary)
                            .collect(Collectors.toMap(QuizSummaryDto::getId, Function.identity()));
                });

        // 3. Combine sessions with quiz titles and map to DTOs
        return Mono.zip(sessionsFlux.collectList(), quizSummariesMapMono, totalElementsMono)
                .map(tuple -> {
                    List<QuizSession> sessions = tuple.getT1();
                    Map<UUID, QuizSummaryDto> quizSummariesMap = tuple.getT2();
                    Long totalElements = tuple.getT3();

                    List<QuizSessionSummaryDto> dtoList = sessions.stream()
                            .map(session -> {
                                String quizTitle = quizSummariesMap.getOrDefault(session.getQuizId(), QuizSummaryDto.builder().titleEn("Unknown Quiz").build()).getTitleEn(); // Default to English title
                                Long durationMs = null;
                                if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                                    durationMs = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis();
                                }

                                return QuizSessionSummaryDto.builder()
                                        .sessionId(session.getId())
                                        .quizId(session.getQuizId())
                                        .quizTitle(quizTitle)
                                        .quizType(session.getQuizType())
                                        .score(session.getScore())
                                        .totalQuestions(session.getTotalQuestions())
                                        .status(session.getStatus())
                                        .startedAt(session.getStartedAt())
                                        .completedAt(session.getCompletedAt())
                                        .durationMs(durationMs)
                                        .build();
                            })
                            .collect(Collectors.toList());

                    return new PageImpl<>(dtoList, pageable, totalElements);
                });
    }

    public Mono<Page<AnswerHistoryDto>> getSessionAnswerHistory(
            UUID sessionId,
            UUID userId,
            Pageable pageable,
            Locale locale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionCacheService.get(sessionId)
                        .flatMap(sessionCache -> quizAnswerRepository.findSessionAnswers(sessionId, pageable) // Use new paginated method
                                .collectList()
                                .flatMap(answers -> {
                                    List<AnswerHistoryDto> history = answers.stream()
                                            .map(answer -> {
                                                CachedQuestion cachedQuestion = sessionCache.findQuestion(answer.getQuestionId());
                                                String explanation = locale.getLanguage().equals("ru") ? cachedQuestion.getExplanationRu() : cachedQuestion.getExplanationEn();

                                                return AnswerHistoryDto.builder()
                                                        .questionId(answer.getQuestionId())
                                                        .questionText(cachedQuestion.getText())
                                                        .selectedAnswerIast(answer.getSelectedFormIast()) // Use new field
                                                        .correctOptionIast(answer.getCorrectFormIast()) // Use existing field
                                                        .isCorrect(answer.isCorrect())
                                                        .responseTimeMs(answer.getResponseTimeMs())
                                                        .answeredAt(answer.getAnsweredAt())
                                                        .explanation(explanation)
                                                        .build();
                                            })
                                            .collect(Collectors.toList());

                                    return quizAnswerRepository.countBySessionId(sessionId) // Need a countBySessionId
                                            .map(total -> new PageImpl<>(history, pageable, total));
                                })
                        )
                );
    }
}
