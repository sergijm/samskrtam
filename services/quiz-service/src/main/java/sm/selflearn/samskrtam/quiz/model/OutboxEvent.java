package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder // Moved @Builder back to class level
@Table(name = "outbox_events", schema = "quiz")
public class OutboxEvent {
    @Id
    private UUID id;
    private String aggregateId;   // userId — ключ партиции Kafka
    private String topic;         // quiz.answer.submitted / quiz.session.completed
    private String payload;       // JSON события
    private OutboxStatus status;        // PENDING / PROCESSED / FAILED
    private OutboxEventType eventType; // Added eventType field
    private Instant createdAt;
    private Instant processedAt;
    private int retryCount;
    private String errorMessage;
}
