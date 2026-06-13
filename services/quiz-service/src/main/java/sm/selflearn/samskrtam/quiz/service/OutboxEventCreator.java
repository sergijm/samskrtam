package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.event.QuizAnsweredEvent;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEvent;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEventType;
import sm.selflearn.samskrtam.quiz.outbox.OutboxStatus;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxEventCreator {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public Mono<Void> createAndSaveQuizAnsweredEvent(QuizAnsweredEvent event) {
        return createAndSave(event, OutboxEventType.QUIZ_ANSWERED, event.quizSessionId());
    }

    public Mono<Void> createAndSaveSessionStatusChangedEvent(QuizSessionStatusChangedEvent event) {
        return createAndSave(event, OutboxEventType.QUIZ_SESSION_STATUS_CHANGED, event.quizSessionId());
    }

    private Mono<Void> createAndSave(Object eventPayload, OutboxEventType eventType, UUID aggregateId) {
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(null)
                    .aggregateType("QuizSession")
                    .aggregateId(aggregateId.toString())
                    .eventType(eventType)
                    .payload(payload)
                    .createdAt(Instant.now())
                    .status(OutboxStatus.NEW)
                    .build();
            return outboxEventRepository.save(outboxEvent).then();
        } catch (JsonProcessingException e) {
            return Mono.error(new RuntimeException("Failed to serialize event payload", e));
        }
    }
}
