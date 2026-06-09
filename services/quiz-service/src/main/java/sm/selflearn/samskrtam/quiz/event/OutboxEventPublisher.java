package sm.selflearn.samskrtam.quiz.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import sm.selflearn.samskrtam.events.AbstractEvent;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxEventPublisher {

    private final ReactiveKafkaProducerTemplate<String, Object> kafkaTemplate;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper; // For serializing payload

    /**
     * Публикует события из таблицы outbox в Kafka.
     * Вызывается @Scheduled процессором каждые 5 секунд.
     */
    @Scheduled(fixedDelayString = "${outbox.processor.interval-ms:5000}")
    public void process() {
        publishPending().subscribe(); // Subscribe to trigger the Flux
    }

    public Flux<Void> publishPending() {
        return outboxRepository.findByStatus(OutboxStatus.PENDING)
                .flatMap(event -> {
                    try {
                        AbstractEvent payload = objectMapper.readValue(event.getPayload(), AbstractEvent.class); // Deserialize to Object
                        return kafkaTemplate
                                .send(event.getTopic(), event.getAggregateId(), payload)
                                .doOnSuccess(result -> log.debug(
                                        "Published outbox event: id={}, topic={}", event.getId(), event.getTopic()))
                                .then(outboxRepository.markProcessed(event.getId()))
                                .onErrorResume(e -> {
                                    log.error("Failed to publish outbox event: id={}", event.getId(), e);
                                    return outboxRepository.markFailed(event.getId(), e.getMessage());
                                });
                    } catch (JsonProcessingException e) {
                        log.error("Failed to deserialize outbox event payload: id={}", event.getId(), e);
                        return outboxRepository.markFailed(event.getId(), "Failed to deserialize payload: " + e.getMessage());
                    }
                });
    }
}
