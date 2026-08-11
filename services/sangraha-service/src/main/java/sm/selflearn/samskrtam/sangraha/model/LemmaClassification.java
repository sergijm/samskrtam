package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Результат классификации лексемы (lemma-classification.md §1.7).
 * UNIQUE(lemma_id, gender, scheme_code) — один прогон по схеме для пары
 * (лемма, род) перезаписывает строку.
 */
@Entity
@Table(name = "lemma_classification", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LemmaClassification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "gender")
    private String gender;

    @Column(name = "scheme_code", nullable = false)
    private String schemeCode;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "gloss_ru")
    private String glossRu;

    @Column(name = "gloss_en")
    private String glossEn;

    @Column(name = "confidence")
    private Short confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ClassificationStatus status;

    @Column(name = "llm_model")
    private String llmModel;

    @Column(name = "reviewed_by")
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}