package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.OutboxEvent;
import sm.selflearn.samskrtam.quiz.model.OutboxStatus;

import java.util.UUID;

@Repository
public interface OutboxEventRepository extends ReactiveCrudRepository<OutboxEvent, UUID> {
    // Explicitly convert OutboxStatus enum to String for the query
    @Query("SELECT * FROM quiz.outbox_events WHERE status = :#{#status.name()}")
    Flux<OutboxEvent> findByStatus(OutboxStatus status);

    @Query("UPDATE quiz.outbox_events SET status = 'PROCESSED', processed_at = NOW() WHERE id = :id")
    Mono<Void> markProcessed(UUID id);

    @Query("UPDATE quiz.outbox_events SET status = 'FAILED', error_message = :errorMessage, retry_count = retry_count + 1 WHERE id = :id")
    Mono<Void> markFailed(UUID id, String errorMessage);
}
