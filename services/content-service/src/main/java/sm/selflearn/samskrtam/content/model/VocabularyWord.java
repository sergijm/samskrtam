package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode; // Import for JdbcTypeCode
import org.hibernate.type.SqlTypes; // Import for SqlTypes

import java.time.Instant;
import java.util.List; // Import List
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity // JPA Entity annotation
@Table(name = "vocabulary_words", schema = "content") // JPA Table annotation
public class VocabularyWord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID) // Assuming DB generates UUIDs
    private UUID id;

    @Column(name = "word_iast", nullable = false)
    private String wordIast;

    @Column(name = "word_devanagari", nullable = false)
    private String wordDevanagari;

    @Column(name = "translation_en", nullable = false)
    private String translationEn;

    @Column(name = "translation_ru", nullable = false)
    private String translationRu;

    @Enumerated(EnumType.STRING) // Store enum as String in DB
    @Column(nullable = false)
    private Gender gender; // Enum for gender

    @Column(nullable = false)
    private String stem; // Основа слова

    @Column
    private String root; // Корень слова

    @Column(name = "explanation_ru", columnDefinition = "TEXT", nullable = false) // New field
    private String explanationRu;

    @Column(name = "explanation_en", columnDefinition = "TEXT", nullable = false) // New field
    private String explanationEn;

    @JdbcTypeCode(SqlTypes.ARRAY) // Map PostgreSQL TEXT[] to Java List<String>
    @Column(name = "tags", columnDefinition = "TEXT[]", nullable = false)
    private List<String> tags; // New field for thematic tags

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
