package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.model.OutboxEvent;
import sm.selflearn.samskrtam.sangraha.repository.OutboxEventRepository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAllPending();

        for (OutboxEvent event : pendingEvents) {
            try {
                Object payload = objectMapper.readValue(event.getPayload(), Object.class);
                kafkaTemplate.send(event.getEventType(), event.getAggregateId().toString(), payload);

                event.setStatus("PROCESSED");
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);

                log.info("Published outbox event: type={}, aggregateId={}", event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                event.setStatus("FAILED");
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                outboxEventRepository.save(event);
                log.error("Failed to publish outbox event: type={}, aggregateId={}", event.getEventType(), event.getAggregateId(), e);
            }
        }
    }
}