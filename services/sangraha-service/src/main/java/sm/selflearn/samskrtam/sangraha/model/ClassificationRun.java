package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Прогон классификации по схеме (lemma-classification.md §1.6).
 */
@Entity
@Table(name = "classification_run", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassificationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scheme_code", nullable = false)
    private String schemeCode;

    @Column(name = "requested_batch_count", nullable = false)
    private int requestedBatchCount;

    @Column(name = "completed_batch_count", nullable = false)
    private int completedBatchCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClassificationRunStatus status;

    @Column(name = "requested_by")
    private String requestedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}