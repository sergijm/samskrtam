package sm.selflearn.samskrtam.quiz.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEvent;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEventType;
import sm.selflearn.samskrtam.quiz.outbox.OutboxStatus;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final ReactiveKafkaProducerTemplate<String, Object> reactiveKafkaProducerTemplate;
    private final ObjectMapper objectMapper; // To deserialize payload if needed, or just pass as String

    // Scheduled to run every 5 seconds
    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:5000}")
    public void publishOutboxEvents() {
        log.debug("Checking for new outbox events to publish...");
        outboxEventRepository.findByStatus(OutboxStatus.NEW)
                .flatMap(this::publishEvent)
                .subscribe(
                        null, // onNext - no-op, as publishEvent handles updates
                        error -> log.error("Error processing outbox events: {}", error.getMessage(), error),
                        () -> log.debug("Finished checking for outbox events.")
                );
    }

    private Mono<Void> publishEvent(OutboxEvent event) {
        log.info("Attempting to publish outbox event: {}", event.getId());
        String topic = getTopicForEventType(event.getEventType());

        return reactiveKafkaProducerTemplate.send(topic, event.getAggregateId(), event.getPayload())
                .flatMap(senderResult -> {
                    log.info("Successfully published event {} to Kafka. Topic: {}, Partition: {}, Offset: {}",
                            event.getId(),
                            senderResult.recordMetadata().topic(),
                            senderResult.recordMetadata().partition(),
                            senderResult.recordMetadata().offset());
                    // Attempt to mark as processed. If it fails (e.g., already processed by another instance),
                    // the Mono<Void> will complete without error, but no row will be updated.
                    return outboxEventRepository.markProcessed(event.getId())
                            .doOnSuccess(v -> log.debug("Marked event {} as PROCESSED.", event.getId()))
                            .doOnError(e -> log.warn("Failed to mark event {} as PROCESSED, possibly already processed: {}", event.getId(), e.getMessage()));
                })
                .onErrorResume(e -> {
                    log.error("Failed to publish event {} to Kafka: {}", event.getId(), e.getMessage(), e);
                    // Attempt to mark as failed. Similar to markProcessed, if it's already processed, it's fine.
                    return outboxEventRepository.markFailed(event.getId(), e.getMessage())
                            .doOnSuccess(v -> log.debug("Marked event {} as FAILED.", event.getId()))
                            .doOnError(e2 -> log.warn("Failed to mark event {} as FAILED, possibly already processed: {}", event.getId(), e2.getMessage()));
                })
                .then(); // Ensure the Mono completes
    }

    private String getTopicForEventType(OutboxEventType eventType) {
        return switch (eventType) {
            case QUIZ_ANSWERED -> "quiz-answered-events";
            case QUIZ_SESSION_STATUS_CHANGED -> "quiz-session-status-changed-events";
            // Add other event types as needed
        };
    }
}
