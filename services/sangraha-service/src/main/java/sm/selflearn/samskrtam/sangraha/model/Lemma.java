package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Словарь лексем по всему корпусу (lemma-classification.md §1.1). Одна строка
 * на {@code lemmaSlp1} (UNIQUE). Пер-lemma статистика (gender, occurrenceCount,
 * dominantPosCode) живёт в {@link LemmaStatistics}.
 */
@Entity
@Table(name = "lemma", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lemma {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lemma_slp1", nullable = false)
    private String lemmaSlp1;

    @Column(name = "lemma_iast", nullable = false)
    private String lemmaIast;

    @Column(name = "lemma_devanagari", nullable = false)
    private String lemmaDevanagari;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}