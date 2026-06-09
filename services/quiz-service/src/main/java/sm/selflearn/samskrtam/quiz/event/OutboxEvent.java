package sm.selflearn.samskrtam.quiz.event;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Table(name = "outbox_events", schema = "quiz") // Note: Schema might need to be dynamic or removed if this DTO is truly shared
public class OutboxEvent {
    @Id
    private UUID id;
    private String aggregateId;
    private String topic;
    private String payload;
    private OutboxStatus status;
    private OutboxEventType eventType;
    private Instant createdAt;
    private Instant processedAt;
    private int retryCount;
    private String errorMessage;
}
