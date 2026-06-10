package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.outbox.OutboxEvent; // Updated import
import sm.selflearn.samskrtam.quiz.outbox.OutboxStatus; // Updated import

import java.util.UUID;

@Repository
public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    // Explicitly convert OutboxStatus enum to String for the query
    @Query("SELECT * FROM quiz.outbox_events WHERE status = :#{#status.name()}")
    Flux<OutboxEvent> findByStatus(OutboxStatus status);

    // Added WHERE status = 'NEW' to ensure we only update events that are still NEW
    @Query("UPDATE quiz.outbox_events SET status = 'PROCESSED', processed_at = NOW() WHERE id = :id AND status = 'NEW'")
    Mono<Void> markProcessed(UUID id);

    // Added WHERE status = 'NEW' to ensure we only update events that are still NEW
    @Query("UPDATE quiz.outbox_events SET status = 'FAILED', error_message = :errorMessage, retry_count = retry_count + 1 WHERE id = :id AND status = 'NEW'")
    Mono<Void> markFailed(UUID id, String errorMessage);
}
