package sm.selflearn.samskrtam.quiz.service;

import io.r2dbc.postgresql.codec.Json;
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
import sm.selflearn.samskrtam.quiz.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.quiz.mapper.UserSessionMapper;
import sm.selflearn.samskrtam.quiz.model.*;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionToDtoMapper;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;

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
        private final SessionQuestionRepository sessionQuestionRepository;
    private final SessionQuestionToDtoMapper sessionQuestionToDtoMapper;
    private final UserSessionMapper userSessionMapper;
    

        public Mono<Page<QuizSummaryDto>> getUserQuizSessions(
            UUID userId,
            LessonType lessonType,
            SessionStatus status,
            UUID quizId,
            Pageable pageable) {

        Flux<QuizSession> sessionsFlux;
        Mono<Long> totalElementsMono;

        if (quizId != null) {
            sessionsFlux = quizSessionRepository.findUserSessionsByQuizId(userId, quizId, status, pageable);
            totalElementsMono = quizSessionRepository.countUserSessionsByQuizId(userId, quizId, status);
        } else {
            sessionsFlux = quizSessionRepository.findUserSessions(userId, lessonType, status, pageable);
            totalElementsMono = quizSessionRepository.countUserSessions(userId, lessonType, status);
        }

                Mono<Map<UUID, LessonItemResponse>> lessonSummariesMapMono = sessionsFlux
                .map(QuizSession::getLessonId)
                .collect(Collectors.toSet())
                .flatMap(lessonIds -> {
                    if (lessonIds.isEmpty()) {
                        return Mono.just(Map.of());
                    }
                    return Flux.fromIterable(lessonIds)
                            .flatMap(contentClient::getLessonItem)
                            .collect(Collectors.toMap(LessonItemResponse::getId, Function.identity()));
                });

        return Mono.zip(sessionsFlux.collectList(), lessonSummariesMapMono, totalElementsMono)
                .flatMap(tuple -> {
                    List<QuizSession> sessions = tuple.getT1();
                    Map<UUID, LessonItemResponse> lessonSummariesMap = tuple.getT2();
                    Long totalElements = tuple.getT3();

                    // Fetch correct answer counts for all sessions in parallel
                    List<UUID> sessionIds = sessions.stream()
                            .map(QuizSession::getId)
                            .collect(Collectors.toList());

                    Mono<Map<UUID, Long>> correctAnswersMapMono;
                    if (sessionIds.isEmpty()) {
                        correctAnswersMapMono = Mono.just(Map.of());
                    } else {
                        correctAnswersMapMono = Flux.fromIterable(sessionIds)
                                .flatMap(sid -> quizAnswerRepository.countCorrectAnswersBySessionId(sid)
                                        .map(count -> Map.entry(sid, count)))
                                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
                    }

                    return correctAnswersMapMono.map(correctAnswersMap -> {
                        List<QuizSummaryDto> dtoList = sessions.stream()
                                .map(session -> {
                                    LessonItemResponse summary = lessonSummariesMap.get(session.getLessonId());
                                    Long durationMs = null;
                                    if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                                        durationMs = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis();
                                    }
                                    int combinationsCount = countFilterCombinations(session);
                                    long correctAnswers = correctAnswersMap.getOrDefault(session.getId(), 0L);

                                    return QuizSummaryDto.builder()
                                            .sessionId(session.getId())
                                            .lessonId(session.getLessonId())
                                            .lessonTitle(summary != null ? summary.getTitleEn() : "Unknown Lesson")
                                            .lessonTitleRu(summary != null ? summary.getTitleRu() : "Неизвестный урок")
                                            .lessonTitleEn(summary != null ? summary.getTitleEn() : "Unknown Lesson")
                                            .slug(summary != null ? summary.getSlug() : "")
                                            .lessonType(session.getLessonType())
                                            .score(session.getScore())
                                            .totalQuestions(session.getTotalQuestions())
                                            .answeredQuestions(session.getAnsweredQuestions())
                                            .correctAnswers((int) correctAnswers)
                                            .combinationsCount(combinationsCount)
                                            .status(session.getStatus())
                                            .startedAt(session.getStartedAt())
                                            .completedAt(session.getCompletedAt())
                                            .durationMs(durationMs)
                                            .build();
                                })
                                .collect(Collectors.toList());

                        return new PageImpl<>(dtoList, pageable, totalElements);
                    });
                });
    }

                public Mono<QuizSummaryDto> getQuizSessionSummary(UUID sessionId, UUID userId) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Quiz session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> Mono.zip(
                        contentClient.getLessonItem(session.getLessonId()),
                        quizAnswerRepository.countCorrectAnswersBySessionId(sessionId)
                )
                        .map(tuple -> {
                            LessonItemResponse lessonSummary = tuple.getT1();
                            long correctAnswers = tuple.getT2();
                            Long durationMs = null;
                            if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                                durationMs = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis();
                            }
                            return QuizSummaryDto.builder()
                                    .sessionId(session.getId())
                                    .lessonId(session.getLessonId())
                                    .lessonTitle(lessonSummary.getTitleEn())
                                    .lessonTitleRu(lessonSummary.getTitleRu())
                                    .lessonTitleEn(lessonSummary.getTitleEn())
                                    .slug(lessonSummary.getSlug())
                                    .lessonType(session.getLessonType())
                                    .score(session.getScore())
                                    .totalQuestions(session.getTotalQuestions())
                                    .answeredQuestions(session.getAnsweredQuestions())
                                    .correctAnswers((int) correctAnswers)
                                    .combinationsCount(countFilterCombinations(session))
                                    .status(session.getStatus())
                                    .startedAt(session.getStartedAt())
                                    .completedAt(session.getCompletedAt())
                                    .durationMs(durationMs)
                                    .build();
                        }));
    }

    public Mono<List<AnswerHistoryDto>> getSessionAnswerHistory(
            UUID sessionId,
            UUID userId,
            Locale locale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found or does not belong to user: " + sessionId)))
                                .flatMap(session -> Mono.zip(
                                        sessionQuestionRepository.findBySessionId(session.getId())
                                                .map(sessionQuestionToDtoMapper::toDto)
                                                .collectList(),
                                        quizAnswerRepository.findBySessionId(sessionId).collectList()
                                )
                                .flatMap(tuple -> {
                                    List<GeneratedQuizQuestionDto> generatedQuestions = tuple.getT1();
                                    List<QuizAnswer> quizAnswers = tuple.getT2();

                                    Map<UUID, QuizAnswer> answersMap = quizAnswers.stream()
                                            .collect(Collectors.toMap(QuizAnswer::getQuestionId, Function.identity()));

                                                                        List<AnswerHistoryDto> fullHistory = generatedQuestions.stream()
                                            .sorted(Comparator.comparing(GeneratedQuizQuestionDto::getId))
                                            .map(gq -> {
                                                QuizAnswer answer = answersMap.get(gq.getId());
                                                String explanationRu = gq.getExplanationRu();
                                                String explanationEn = gq.getExplanationEn();

                                                return AnswerHistoryDto.builder()
                                                        .questionId(gq.getId())
                                                        .questionNumber(gq.getQuestionNumber())
                                                        .questionText(gq.getText())
                                                        .selectedAnswerIast(answer != null ? answer.getSelectedFormIast() : null)
                                                        .correctOptionIast(gq.getCorrectFormIast())
                                                        .isCorrect(answer != null ? answer.getIsCorrect() : null)
                                                        .responseTimeMs(answer != null ? answer.getResponseTimeMs() : null)
                                                        .answeredAt(answer != null ? answer.getAnsweredAt() : null)
                                                        .explanationRu(explanationRu)
                                                        .explanationEn(explanationEn)
                                                        .build();
                                            })
                                            .collect(Collectors.toList());

                                    return Mono.just(fullHistory);
                                })

                );
    }

        public Mono<QuizProgressDto> getLatestUnfinishedQuizProgress(UUID userId, UUID lessonId) {
        return quizSessionRepository.findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(userId, lessonId, SessionStatus.IN_PROGRESS)
                .map(session -> new QuizProgressDto(session.getId(), session.getAnsweredQuestions(), session.getTotalQuestions(), true))
                .defaultIfEmpty(new QuizProgressDto(null, 0, 0, false));
    }

    public QuizProgressDto getUserQuizProgress(UUID userId, UUID id) {
        return null;
    }

        public Mono<List<QuizAnswer>> getWordAnswers(UUID userId, UUID wordId, UUID lessonId) {
        return quizAnswerRepository.findByWordIdAndUserIdAndLessonId(wordId, userId, lessonId)
                .collectList();
    }

        public Mono<Long> countWordAnswers(UUID userId, UUID wordId, UUID lessonId) {
        return quizAnswerRepository.countByWordIdAndUserIdAndLessonId(wordId, userId, lessonId);
    }

    /**
     * Counts the number of filter combinations stored in the session's JSONB fields.
     */
    private int countFilterCombinations(QuizSession session) {
        if (session.getFilterScope() == null) return 0;
        try {
            return switch (session.getFilterScope()) {
                case CASE_ONLY -> parseJsonArrayLength(session.getFilterCaseTypes());
                case NUMBER_ONLY -> parseJsonArrayLength(session.getFilterNumberTypes());
                case CASE_NUMBER_GENDER -> parseJsonArrayLength(session.getFilterCombinations());
            };
        } catch (Exception e) {
            log.warn("Failed to parse filter combinations for session {}", session.getId(), e);
            return 0;
        }
    }

        private int parseJsonArrayLength(Json json) {
        if (json == null) return 0;
        return parseJsonArrayLength(json.asString());
    }

    private int parseJsonArrayLength(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) return 0;
        // Simple count of "{" for objects or "," + 1 for flat arrays
        int openBraces = 0;
        for (char c : jsonArray.toCharArray()) {
            if (c == '{') openBraces++;
        }
        if (openBraces > 0) return openBraces;
        // Flat string array: count commas + 1, but only if non-empty
        String stripped = jsonArray.replaceAll("[\\[\\]\"]", "").trim();
        if (stripped.isEmpty()) return 0;
        return (int) stripped.chars().filter(c -> c == ',').count() + 1;
    }
}
