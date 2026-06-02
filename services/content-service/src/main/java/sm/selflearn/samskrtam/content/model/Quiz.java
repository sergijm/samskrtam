package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.Data;
import sm.selflearn.samskrtam.content.dto.Difficulty;
import sm.selflearn.samskrtam.content.dto.QuizType;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "quizzes", schema = "content")
public class Quiz {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(unique = true) private String slug;
    private String titleRu;
    private String titleEn;
    private String descriptionRu; // Added
    private String descriptionEn; // Added
    @Enumerated(EnumType.STRING) private QuizType quizType;
    @Enumerated(EnumType.STRING) private Difficulty difficulty;
    private int questionsPerSession;
    private Instant createdAt;
    private Instant deletedAt;
}
