package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Статистика вхождения леммы по корпусу в разрезе рода (lemma-classification.md
 * §1.3). Одна строка на (lemma_id, gender) — UNIQUE. Пересчитывается процессом
 * refresh, стоит «между» словарём {@link Lemma} и классификацией.
 */
@Entity
@Table(name = "lemma_statistics", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LemmaStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lemma_id", nullable = false)
    private Lemma lemma;

    @Column(name = "gender")
    private String gender;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @Column(name = "dominant_pos_code")
    private String dominantPosCode;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}