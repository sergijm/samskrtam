package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "questions", schema = "content")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "quiz_id", nullable = false)
    private UUID quizId;

    @Column(name = "text_ru", nullable = false, columnDefinition = "TEXT")
    private String textRu;

    @Column(name = "text_en", nullable = false, columnDefinition = "TEXT")
    private String textEn;

    @Column(name = "explanation_ru", nullable = false, columnDefinition = "TEXT")
    private String explanationRu;

    @Column(name = "explanation_en", nullable = false, columnDefinition = "TEXT")
    private String explanationEn;

    @Column(name = "correct_option_id")
    private UUID correctOptionId; // This will be the ID of the correct QuestionOption

    @Column(name = "declension_stem_id") // New field for declension quizzes
    private UUID declensionStemId;

    @Enumerated(EnumType.STRING) // New field for declension quizzes
    @Column(name = "target_case")
    private Case targetCase;

    @Enumerated(EnumType.STRING) // New field for declension quizzes
    @Column(name = "target_number")
    private Number targetNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
