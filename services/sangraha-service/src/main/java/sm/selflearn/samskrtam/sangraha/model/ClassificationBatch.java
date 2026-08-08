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
 * Один LLM-вызов внутри прогона (lemma-classification.md §1.7). При неудаче
 * {@code status = FAILED}, остальные батчи обрабатываются независимо.
 */
@Entity
@Table(name = "classification_batch", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassificationBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scheme_code", nullable = false)
    private String schemeCode;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "batch_index", nullable = false)
    private int batchIndex;

    @Column(name = "lemma_count", nullable = false)
    private int lemmaCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClassificationBatchStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "llm_model", nullable = false)
    private String llmModel;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}