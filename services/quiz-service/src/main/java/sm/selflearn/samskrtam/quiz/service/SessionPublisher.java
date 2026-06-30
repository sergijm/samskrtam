package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SessionPublisher {

    private final OutboxEventCreator outboxEventCreator;

    public Mono<Void> publishStarted(QuizSession session) {
        return outboxEventCreator.createAndSaveSessionStatusChangedEvent(
                new QuizSessionStatusChangedEvent(
                        session.getId(),
                        session.getUserId(),
                        session.getLessonId(),
                        session.getLessonType(),
                        null,
                        SessionStatus.IN_PROGRESS.name(),
                        Instant.now()
                )
        );
    }
}