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
import sm.selflearn.samskrtam.quiz.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.quiz.mapper.UserSessionMapper;
import sm.selflearn.samskrtam.quiz.model.*;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
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
    private final ObjectMapper objectMapper;
    private final UserSessionMapper userSessionMapper;
    

    public Mono<Page<QuizSummaryDto>> getUserQuizSessions(
            UUID userId,
            LessonType lessonType,
            SessionStatus status,
            Pageable pageable) {

        Mono<Long> totalElementsMono = quizSessionRepository.countUserSessions(userId, lessonType, status);
        Flux<QuizSession> sessionsFlux = quizSessionRepository.findUserSessions(userId, lessonType, status, pageable);

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
                .map(tuple -> {
                    List<QuizSession> sessions = tuple.getT1();
                    Map<UUID, LessonItemResponse> lessonSummariesMap = tuple.getT2();
                    Long totalElements = tuple.getT3();

                    List<QuizSummaryDto> dtoList = sessions.stream()
                            .map(session -> {
                                LessonItemResponse summary = lessonSummariesMap.get(session.getLessonId());
                                Long durationMs = null;
                                if (session.getStartedAt() != null && session.getCompletedAt() != null) {
                                    durationMs = Duration.between(session.getStartedAt(), session.getCompletedAt()).toMillis();
                                }
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

        public Mono<QuizSummaryDto> getQuizSessionSummary(UUID sessionId, UUID userId) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Quiz session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> contentClient.getLessonItem(session.getLessonId())
                        .map(lessonSummary -> {
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
                                        contentClient.getGeneratedQuizData(session.getGeneratedQuizDataId()),
                                        quizAnswerRepository.findBySessionId(sessionId).collectList()
                                )
                                .flatMap(tuple -> {
                                    GeneratedQuizData generatedQuizData = tuple.getT1();
                                    List<GeneratedQuizQuestionDto> generatedQuestions = generatedQuizData.getGeneratedQuestions();
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
}
