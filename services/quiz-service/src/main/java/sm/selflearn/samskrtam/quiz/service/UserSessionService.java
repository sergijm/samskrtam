package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryDto;
import sm.selflearn.samskrtam.quiz.dto.QuizProgressDto;
import sm.selflearn.samskrtam.quiz.dto.QuizSessionSummaryDto;
import sm.selflearn.samskrtam.quiz.model.*;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.dto.GeneratedQuizQuestionDto; // Added import

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ContentClient contentClient;
    private final ObjectMapper objectMapper;

    private static final Random random = new Random();

    public Mono<Page<QuizSessionSummaryDto>> getUserQuizSessions(
            UUID userId,
            QuizType quizType,
            SessionStatus status,
            Pageable pageable) {

        Mono<Long> totalElementsMono = quizSessionRepository.countUserSessions(userId, quizType, status);
        Flux<QuizSession> sessionsFlux = quizSessionRepository.findUserSessions(userId, quizType, status, pageable);

        Mono<Map<UUID, QuizSummaryDto>> quizSummariesMapMono = sessionsFlux
                .map(QuizSession::getQuizId)
                .collect(Collectors.toSet())
                .flatMap(quizIds -> {
                    if (quizIds.isEmpty()) {
                        return Mono.just(Map.of());
                    }
                    return Flux.fromIterable(quizIds)
                            .flatMap(contentClient::getQuizSummary)
                            .collect(Collectors.toMap(QuizSummaryDto::getId, Function.identity()));
                });

        return Mono.zip(sessionsFlux.collectList(), quizSummariesMapMono, totalElementsMono)
                .map(tuple -> {
                    List<QuizSession> sessions = tuple.getT1();
                    Map<UUID, QuizSummaryDto> quizSummariesMap = tuple.getT2();
                    Long totalElements = tuple.getT3();

                    List<QuizSessionSummaryDto> dtoList = sessions.stream()
                            .map(session -> {
                                QuizSummaryDto summary = quizSummariesMap.get(session.getQuizId());
                                String quizTitle = (summary != null) ? summary.getTitleEn() : "Unknown Quiz";
                                String quizTitleRu = (summary != null) ? summary.getTitleRu() : "Неизвестный квиз";
                                String quizTitleEn = (summary != null) ? summary.getTitleEn() : "Unknown Quiz";
                                String slug = (summary != null) ? summary.getSlug() : "";

                                Long durationMs = null;
                                if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                                    durationMs = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis();
                                }

                                return QuizSessionSummaryDto.builder()
                                        .sessionId(session.getId())
                                        .quizId(session.getQuizId())
                                        .quizTitle(quizTitle)
                                        .quizTitleRu(quizTitleRu)
                                        .quizTitleEn(quizTitleEn)
                                        .slug(slug)
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

    public Mono<QuizSessionSummaryDto> getQuizSessionSummary(UUID sessionId, UUID userId) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Quiz session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> contentClient.getQuizSummary(session.getQuizId())
                        .map(quizSummary -> {
                            Long durationMs = null;
                            if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                                durationMs = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis();
                            }
                            return QuizSessionSummaryDto.builder()
                                    .sessionId(session.getId())
                                    .quizId(session.getQuizId())
                                    .quizTitle(quizSummary.getTitleEn()) // Assuming English title for summary
                                    .quizTitleRu(quizSummary.getTitleRu())
                                    .quizTitleEn(quizSummary.getTitleEn())
                                    .slug(quizSummary.getSlug())
                                    .quizType(session.getQuizType())
                                    .score(session.getScore())
                                    .totalQuestions(session.getTotalQuestions())
                                    .status(session.getStatus())
                                    .startedAt(session.getStartedAt())
                                    .completedAt(session.getCompletedAt())
                                    .durationMs(durationMs)
                                    .build();
                        }));
    }

    public Mono<List<AnswerHistoryDto>> getSessionAnswerHistory( // Changed return type to List
            UUID sessionId,
            UUID userId,
            Locale locale) { // Removed Pageable
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> Mono.zip(
                                contentClient.getGeneratedQuizData(session.getGeneratedQuizDataId()), // Changed method call
                                quizAnswerRepository.findBySessionId(sessionId).collectList()
                        )
                        .flatMap(tuple -> {
                            GeneratedQuizData generatedQuizData = tuple.getT1(); // Changed type
                            List<GeneratedQuizQuestionDto> generatedQuestions = generatedQuizData.getGeneratedQuestions(); // Extract questions
                            List<sm.selflearn.samskrtam.quiz.model.QuizAnswer> quizAnswers = tuple.getT2();

                            Map<UUID, sm.selflearn.samskrtam.quiz.model.QuizAnswer> answersMap = quizAnswers.stream()
                                    .collect(Collectors.toMap(sm.selflearn.samskrtam.quiz.model.QuizAnswer::getQuestionId, Function.identity()));

                            List<AnswerHistoryDto> fullHistory = generatedQuestions.stream()
                                    .sorted(Comparator.comparing(GeneratedQuizQuestionDto::getId)) // Ensure consistent order
                                    .map(gq -> {
                                        QuizAnswer answer = answersMap.get(gq.getId());
                                        // Use explanationRu and explanationEn directly
                                        String explanationRu = gq.getExplanationRu();
                                        String explanationEn = gq.getExplanationEn();

                                        return AnswerHistoryDto.builder()
                                                .questionId(gq.getId())
                                                .questionText(gq.getText())
                                                .selectedAnswerIast(answer != null ? answer.getSelectedFormIast() : null)
                                                .correctOptionIast(gq.getCorrectFormIast()) // Always from GeneratedQuizQuestionDto
                                                .isCorrect(answer != null ? answer.getIsCorrect() : null) // Use null for unanswered
                                                .responseTimeMs(answer != null ? answer.getResponseTimeMs() : null)
                                                .answeredAt(answer != null ? answer.getAnsweredAt() : null)
                                                .explanationRu(explanationRu) // Use new field
                                                .explanationEn(explanationEn) // Use new field
                                                .build();
                                    })
                                    .collect(Collectors.toList());

                            return Mono.just(fullHistory); // Return the full list
                        })

                );
    }

    public Mono<QuizProgressDto> getLatestUnfinishedQuizProgress(UUID userId, UUID quizId) {
        return quizSessionRepository.findTopByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(userId, quizId, SessionStatus.IN_PROGRESS)
                .map(session -> new QuizProgressDto(session.getId(), session.getAnsweredQuestions(), session.getTotalQuestions(), true))
                .defaultIfEmpty(new QuizProgressDto(null, 0, 0, false));
    }
}
