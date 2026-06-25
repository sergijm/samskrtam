package sm.selflearn.samskrtam.quiz.outbox;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Table(name = "outbox_events", schema = "quiz") // Specify the table name for R2DBC
public class OutboxEvent {
    @Id // Mark id as the primary key
    UUID id;
    String aggregateType;
    String aggregateId;
    OutboxEventType eventType;
    String payload;
    Instant createdAt;
    OutboxStatus status;
    String errorMessage; // Added for failed events
    Integer retryCount; // Added for retry mechanism
    Instant processedAt; // Added for tracking when event was processed
}

