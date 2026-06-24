package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "generated_quiz_data", schema = "content")
public class GeneratedQuizDataRecord {
    @Id
    private UUID id; // This will be our generatedQuizDataId

    @Column(nullable = false)
    private UUID quizId;


    @Column(nullable = false)
    private String userLocale;

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(columnDefinition = "TEXT")
    private String vocabularyWordsJson; // To store vocabulary words for VOCABULARY quizzes
}
