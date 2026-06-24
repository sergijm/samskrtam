package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.dto.SessionHistoryResponse;
import sm.selflearn.samskrtam.quiz.model.SessionHistory;
import sm.selflearn.samskrtam.quiz.repository.SessionHistoryRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionHistoryService {

    private final SessionHistoryRepository sessionHistoryRepository;

    public Flux<SessionHistoryResponse> getSessionHistory(UUID userId, UUID quizId, String lessonType, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Flux<SessionHistory> historyFlux;
        if (quizId != null && lessonType != null) {
            historyFlux = sessionHistoryRepository.findByUserIdAndQuizIdAndLessonType(userId, quizId, LessonType.valueOf(lessonType), pageRequest);
        } else if (quizId != null) {
            historyFlux = sessionHistoryRepository.findByUserIdAndQuizId(userId, quizId, pageRequest);
        } else if (lessonType != null) {
            historyFlux = sessionHistoryRepository.findByUserIdAndLessonType(userId, LessonType.valueOf(lessonType), pageRequest);
        } else {
            historyFlux = sessionHistoryRepository.findByUserId(userId, pageRequest);
        }

        return historyFlux.map(this::mapToSessionHistoryResponse);
    }

    public Mono<SessionHistoryResponse> getSessionHistoryDetails(UUID sessionId, UUID userId) {
        return sessionHistoryRepository.findBySessionIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_HISTORY_NOT_FOUND", "Session history not found for ID: " + sessionId)))
                .map(this::mapToSessionHistoryResponse);
    }

    private SessionHistoryResponse mapToSessionHistoryResponse(SessionHistory history) {
        int percentage = (history.getTotalQuestions() > 0) ? (history.getScore() * 100 / history.getTotalQuestions()) : 0;
        return SessionHistoryResponse.builder()
                .sessionId(history.getSessionId())
                .quizId(history.getQuizId())
                .lessonType(history.getLessonType())
                .score(history.getScore())
                .totalQuestions(history.getTotalQuestions())
                .percentage(percentage)
                .durationMs(history.getDurationMs())
                .completedAt(history.getCompletedAt())
                .build();
    }
}
