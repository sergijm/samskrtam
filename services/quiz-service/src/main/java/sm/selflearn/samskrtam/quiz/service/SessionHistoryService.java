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

    public Flux<SessionHistoryResponse> getSessionHistory(UUID userId, UUID lessonId, String lessonType, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);

        Flux<SessionHistory> historyFlux;
        if (lessonId != null && lessonType != null) {
            historyFlux = sessionHistoryRepository.findByUserIdAndLessonIdAndLessonType(userId, lessonId, LessonType.valueOf(lessonType), pageRequest);
        } else if (lessonId != null) {
            historyFlux = sessionHistoryRepository.findByUserIdAndLessonId(userId, lessonId, pageRequest);
        } else if (lessonType != null) {
            historyFlux = sessionHistoryRepository.findByUserIdAndLessonType(userId, LessonType.valueOf(lessonType), pageRequest);
        } else {
            historyFlux = sessionHistoryRepository.findByUserId(userId, pageRequest);
        }

        return historyFlux.map(this::mapToSessionHistoryResponse);
    }


    private SessionHistoryResponse mapToSessionHistoryResponse(SessionHistory history) {
        int percentage = (history.getTotalQuestions() > 0) ? (history.getScore() * 100 / history.getTotalQuestions()) : 0;
        return SessionHistoryResponse.builder()
                .sessionId(history.getSessionId())
                .lessonId(history.getLessonId())
                .lessonType(history.getLessonType())
                .score(history.getScore())
                .totalQuestions(history.getTotalQuestions())
                .percentage(percentage)
                .durationMs(history.getDurationMs())
                .completedAt(history.getCompletedAt())
                .build();
    }
}

