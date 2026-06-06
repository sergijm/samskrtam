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
import sm.selflearn.samskrtam.content.dto.Gender; // Corrected import for Gender

import java.time.Instant;
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

    @Column(name = "dictionary_entry", columnDefinition = "TEXT") // Use TEXT for potentially long dictionary entries
    private String dictionaryEntry; // Словарная статья целиком

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
