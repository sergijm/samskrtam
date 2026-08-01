package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Кэш verseId[] по группам (vowel_type, gender, case_type, number_type)
 * для вкладки «Примеры» на странице шага склонений (content-service.md §12).
 * Пустой verse_ids — валидный результат («искали, ничего не нашли»).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "declension_example_groups", schema = "content")
public class DeclensionExampleGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "vowel_type", nullable = false)
    private String vowelType;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "case_type", nullable = false)
    private String caseType;

    @Column(name = "number_type", nullable = false)
    private String numberType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "verse_ids", columnDefinition = "JSONB", nullable = false)
    private String verseIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}