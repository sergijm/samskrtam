package sm.selflearn.samskrtam.sangraha.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "verse_analyses", schema = "sangraha")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerseAnalysis {

    @Id
    @Column(name = "verse_id")
    private UUID verseId;

    @Column(name = "translation_ru", columnDefinition = "TEXT", nullable = false)
    private String translationRu;

    @Column(name = "translation_en", columnDefinition = "TEXT", nullable = false)
    private String translationEn;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sandhi_splits", columnDefinition = "JSONB", nullable = false)
    private String sandhiSplits;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_model_response", columnDefinition = "JSONB")
    private String rawModelResponse;

    @Column(name = "raw_prompt", columnDefinition = "TEXT")
    private String rawPrompt;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "analyzer_name", nullable = false)
    private String analyzerName;

    @Column(name = "analyzed_at", nullable = false)
    private Instant analyzedAt;

    @PrePersist
    public void prePersist() {
        if (analyzedAt == null) {
            analyzedAt = Instant.now();
        }
    }
}