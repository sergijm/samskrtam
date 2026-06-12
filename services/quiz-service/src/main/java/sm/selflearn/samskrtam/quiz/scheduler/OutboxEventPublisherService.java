package sm.selflearn.samskrtam.quiz.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.event.QuizAnsweredEvent;
import sm.selflearn.samskrtam.quiz.event.QuizSessionStatusChangedEvent;
import sm.selflearn.samskrtam.quiz.event.StatisticEvent;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEvent;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEventType;
import sm.selflearn.samskrtam.quiz.outbox.OutboxStatus;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    @Qualifier("outboxKafkaProducer")
    private final ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:5000}")
    public void publishOutboxEvents() {
        log.debug("Checking for new outbox events to publish...");
        outboxEventRepository.findByStatus(OutboxStatus.NEW)
                .flatMap(this::publishEvent)
                .subscribe(
                        null,
                        error -> log.error("Error processing outbox events: {}", error.getMessage(), error),
                        () -> log.debug("Finished checking for outbox events.")
                );
    }

    private Mono<Void> publishEvent(OutboxEvent event) {
        log.info("Attempting to publish outbox event: {}", event.getId());
        String topic = getTopicForEventType(event.getEventType());

        try {
            StatisticEvent statisticEvent = deserializePayload(event);
            return reactiveKafkaProducerTemplate.send(topic, event.getAggregateId(), statisticEvent)
                    .flatMap(senderResult -> {
                        log.info("Successfully published event {} to Kafka. Topic: {}, Partition: {}, Offset: {}",
                                event.getId(),
                                senderResult.recordMetadata().topic(),
                                senderResult.recordMetadata().partition(),
                                senderResult.recordMetadata().offset());
                        return outboxEventRepository.markProcessed(event.getId())
                                .doOnSuccess(v -> log.debug("Marked event {} as PROCESSED.", event.getId()))
                                .doOnError(e -> log.warn("Failed to mark event {} as PROCESSED, possibly already processed: {}", event.getId(), e.getMessage()));
                    })
                    .onErrorResume(e -> {
                        log.error("Failed to publish event {} to Kafka: {}", event.getId(), e.getMessage(), e);
                        return outboxEventRepository.markFailed(event.getId(), e.getMessage())
                                .doOnSuccess(v -> log.debug("Marked event {} as FAILED.", event.getId()))
                                .doOnError(e2 -> log.warn("Failed to mark event {} as FAILED, possibly already processed: {}", event.getId(), e2.getMessage()));
                    })
                    .then();
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize payload for event {}: {}", event.getId(), e.getMessage(), e);
            return outboxEventRepository.markFailed(event.getId(), "Failed to deserialize payload")
                    .then();
        }
    }

    private StatisticEvent deserializePayload(OutboxEvent event) throws JsonProcessingException {
        return switch (event.getEventType()) {
            case QUIZ_ANSWERED -> objectMapper.readValue(event.getPayload(), QuizAnsweredEvent.class);
            case QUIZ_SESSION_STATUS_CHANGED ->
                    objectMapper.readValue(event.getPayload(), QuizSessionStatusChangedEvent.class);
        };
    }

    private String getTopicForEventType(OutboxEventType eventType) {
        return switch (eventType) {
            case QUIZ_ANSWERED -> "quiz-answered-events";
            case QUIZ_SESSION_STATUS_CHANGED -> "quiz-session-status-changed-events";
        };
    }
}
