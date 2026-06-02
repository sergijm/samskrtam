package sm.selflearn.samskrtam.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode; // Import for JdbcTypeCode
import org.hibernate.type.SqlTypes; // Import for SqlTypes

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", schema = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;             // userId

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private OutboxEventType eventType;    // USER_REGISTERED, PROFILE_UPDATED, USER_BLOCKED, USER_UNBLOCKED

    @JdbcTypeCode(SqlTypes.JSON) // Explicitly tell Hibernate to map String to JSONB
    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private String payload;               // JSON с данными для Keycloak

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;          // PENDING, PROCESSED, FAILED

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "error_message")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
        if (retryCount == 0) {
            retryCount = 0;
        }
    }
}
