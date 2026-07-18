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
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис пагинированного получения истории сессий пользователя.
 * Выделен из {@link UserSessionService} для соблюдения Single Responsibility.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionHistoryPaginationService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ContentClient contentClient;
    private final QuizSummaryAssembler quizSummaryAssembler;

    /**
     * Возвращает пагинированный список сводок сессий пользователя
     * с опциональной фильтрацией по типу урока, статусу и quizId.
     */
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
                                    long correctAnswers = correctAnswersMap.getOrDefault(session.getId(), 0L);
                                    int combinationsCount = countFilterCombinations(session);
                                    return quizSummaryAssembler.assemble(
                                            session, summary, correctAnswers, combinationsCount);
                                })
                                .collect(Collectors.toList());

                        return new PageImpl<>(dtoList, pageable, totalElements);
                    });
                });
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
