package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryDto;
import sm.selflearn.samskrtam.quiz.dto.QuizProgressDto;
import sm.selflearn.samskrtam.quiz.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.quiz.model.*;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionToDtoMapper;

import java.util.*;

/**
 * Сервис работы с историей и сводками сессий пользователя.
 * Пагинация делегирована в {@link SessionHistoryPaginationService},
 * сборка QuizSummaryDto — в {@link QuizSummaryAssembler},
 * сборка AnswerHistoryDto — в {@link AnswerHistoryAssembler}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ContentClient contentClient;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final SessionQuestionToDtoMapper sessionQuestionToDtoMapper;
    private final QuizSummaryAssembler quizSummaryAssembler;
    private final AnswerHistoryAssembler answerHistoryAssembler;
    private final SessionHistoryPaginationService sessionHistoryPaginationService;

    public Mono<Page<QuizSummaryDto>> getUserQuizSessions(
            UUID userId,
            LessonType lessonType,
            SessionStatus status,
            UUID quizId,
            Pageable pageable) {
        return sessionHistoryPaginationService.getUserQuizSessions(userId, lessonType, status, quizId, pageable);
    }

    public Mono<QuizSummaryDto> getQuizSessionSummary(UUID sessionId, UUID userId) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Quiz session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> Mono.zip(
                        resolveLessonItem(session.getLessonId()),
                        quizAnswerRepository.countCorrectAnswersBySessionId(sessionId)
                ).map(tuple -> quizSummaryAssembler.assemble(
                        session, tuple.getT1(), tuple.getT2(),
                        countFilterCombinations(session))));
    }

    public Mono<List<AnswerHistoryDto>> getSessionAnswerHistory(
            UUID sessionId, UUID userId, Locale locale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> Mono.zip(
                        sessionQuestionRepository.findBySessionId(session.getId())
                                .map(sessionQuestionToDtoMapper::toDto).collectList(),
                        quizAnswerRepository.findBySessionId(sessionId).collectList()
                ).map(tuple -> answerHistoryAssembler.assemble(tuple.getT1(), tuple.getT2())));
    }

    public Mono<QuizProgressDto> getLatestUnfinishedQuizProgress(UUID userId, UUID lessonId) {
        return quizSessionRepository
                .findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(userId, lessonId, SessionStatus.IN_PROGRESS)
                .map(session -> new QuizProgressDto(session.getId(), session.getAnsweredQuestions(),
                        session.getTotalQuestions(), true))
                .defaultIfEmpty(new QuizProgressDto(null, 0, 0, false));
    }

    public QuizProgressDto getUserQuizProgress(UUID userId, UUID id) {
        return null;
    }

    public Mono<List<QuizAnswer>> getWordAnswers(UUID userId, UUID wordId, UUID lessonId) {
        return quizAnswerRepository.findByWordIdAndUserIdAndLessonId(wordId, userId, lessonId).collectList();
    }

    public Mono<Long> countWordAnswers(UUID userId, UUID wordId, UUID lessonId) {
        return quizAnswerRepository.countByWordIdAndUserIdAndLessonId(wordId, userId, lessonId);
    }

    private Mono<LessonItemResponse> resolveLessonItem(UUID lessonId) {
        if (lessonId == null) {
            return Mono.empty();
        }
        return contentClient.getLessonItem(lessonId);
    }

    private int countFilterCombinations(QuizSession session) {
        if (session.getFilterScope() == null) return 0;
        try {
                        return switch (session.getFilterScope()) {
                case CASE_ONLY -> parseJsonArrayLength(session.getFilterCaseTypes());
                case NUMBER_ONLY -> parseJsonArrayLength(session.getFilterNumberTypes());
                case CASE_NUMBER_GENDER -> parseJsonArrayLength(session.getFilterCombinations());
                case ALL_STEMS -> parseJsonArrayLength(session.getFilterVowelTypes());
            };
        } catch (Exception e) {
            log.warn("Failed to parse filter combinations for session {}", session.getId(), e);
            return 0;
        }
    }

    private int parseJsonArrayLength(io.r2dbc.postgresql.codec.Json json) {
        if (json == null) return 0;
        return parseJsonStringLength(json.asString());
    }

    private int parseJsonStringLength(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) return 0;
        int openBraces = 0;
        for (char c : jsonArray.toCharArray()) {
            if (c == '{') openBraces++;
        }
        if (openBraces > 0) return openBraces;
        String stripped = jsonArray.replaceAll("[\\[\\]\"]", "").trim();
        if (stripped.isEmpty()) return 0;
        return (int) stripped.chars().filter(c -> c == ',').count() + 1;
    }
}