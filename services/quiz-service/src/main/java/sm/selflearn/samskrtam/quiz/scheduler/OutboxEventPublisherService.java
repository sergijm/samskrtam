package sm.selflearn.samskrtam.quiz.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEvent;
import sm.selflearn.samskrtam.quiz.outbox.OutboxStatus;
import sm.selflearn.samskrtam.quiz.repository.OutboxEventRepository;

/**
 * Idle reader of the transactional outbox.
 *
 * <p>Kafka publishing was removed. The relay now only drains the outbox table:
 * it reads {@link OutboxStatus#NEW} events and marks them {@link OutboxStatus#PROCESSED}
 * so the table does not grow unbounded, but no event is forwarded anywhere.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisherService {

    private final OutboxEventRepository outboxEventRepository;

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:5000}")
    public void publishOutboxEvents() {
        log.debug("Checking for new outbox events to drain (idle mode, nothing is published)...");
        outboxEventRepository.findByStatus(OutboxStatus.NEW)
                .flatMap(this::markProcessed)
                .subscribe(
                        null,
                        error -> log.error("Error draining outbox events: {}", error.getMessage(), error),
                        () -> log.debug("Finished draining outbox events.")
                );
    }

    private Mono<Void> markProcessed(OutboxEvent event) {
        log.info("Draining outbox event {} (idle mode, not published): {}", event.getId(), event.getEventType());
        return outboxEventRepository.markProcessed(event.getId())
                .doOnSuccess(v -> log.debug("Marked event {} as PROCESSED.", event.getId()))
                .doOnError(e -> log.warn("Failed to mark event {} as PROCESSED, possibly already processed: {}", event.getId(), e.getMessage()))
                .then();
    }
}
